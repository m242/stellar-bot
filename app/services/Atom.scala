package services

import concurrent.{Await, Future}
import xml.{Node, Elem}
import java.net.URLEncoder
import collection.mutable.ArrayBuffer
import models.{User, Entry}
import java.util.Date
import play.api.libs.concurrent.Execution.Implicits._
import play.api.Logger
import language.postfixOps
import concurrent.duration._
import controllers.Twitter

/**
 * Created with IntelliJ IDEA.
 * User: markbe
 * Date: 2/6/13
 * Time: 4:00 PM
 */

object Atom {

  val logger = Logger("services.Atom")
  val statusR = """(?i)twitter.com/[A-Z0-9_]+/status/([0-9]+)""".r

  def apply(url: String): Future[Option[Elem]] = {
    Future {
      try {
        Some(xml.XML.load(url))
      } catch {
        case e: Exception => {
          Logger error "Error loading " + url + " : " + e.toString
          None
        }
      }
    }
  }

  def schedule() {
    // Get a list of users from CouchDB
    val users = CouchDB.getUsers
    val futures = new ArrayBuffer[Future[Any]]()
    Logger debug "Checking " + users.length + " users"
    users.foreach(user => futures.append(schedule(user)))
    futures.foreach(f => Await.result(f, 2 minutes))
  }

  def schedule(user: User): Future[Option[Elem]] = {
    Logger debug "Checking twitter: " + user.twitterName + " stellar: " + user.stellarName
    // Load the XML feed
    val future = apply("http://stellar.io/" + URLEncoder.encode(user.stellarName, "UTF-8") + "/flow/feed")
    future onSuccess {
      case Some(feed) =>
        // For each entry,
        (feed \\ "entry").seq.reverse.foreach(node => {
          val entryId = (node \ "id").text
          // Get Entry(key(twitterName, entryId))
          CouchDB.getEntry(Entry.key(user.twitterName, entryId)) match {
            case Some(entry) if(entry.posted == false) => {
              Logger info  "Posting " + entry.entryId
              // Post entry
              try {
                val f = post(node, user)
                f onSuccess { case b =>
                  // Update entry with posted = true
                  entry.posted = b
                  CouchDB.db.update(entry)
                }
                Await.result(f, 2 minutes)
              } catch {
                case e: Exception => Logger error e.toString
              }
            }
            case None => {
              // Create entry
              val e = Entry(entryId, user.twitterName, new Date().getTime.toDouble)
              Logger info "Posting " + e.getId
              try {
                // Post entry
                val f = post(node, user)
                f onSuccess { case b =>
                  e.posted = b
                  CouchDB.db.create(e)
                }
                Await.result(f, 2 minutes)
              } catch {
                case ex: Exception => {
                  e.posted = false
                  CouchDB.db.create(e)
                  Logger error ex.toString
                }
              }
            }
            case _ => { }
          }
        })
    }
    future onFailure { case e => Logger error "Error in checking " + user.stellarName + ": " + e.toString }
    future
  }

  def post(node: Node, user: User): Future[Boolean] = {
    val title = (node \ "title").text
    val link = (node \ "link" \ "@href").text
    Option(link).getOrElse("").contains("twitter.com") match {
      case true => {
        // Re-tweet
        Option(statusR.findFirstMatchIn(link).map(m => m.group(1)).getOrElse("0").toLong).filter(_ > 0).map(id => {
          Twitter.retweet(id, user.twitterToken, user.twitterSecret)
        }).getOrElse(Future { false })
      }
      case false => {
        // Post new tweet
        val linkLength = link.length + 1
        val newTitle = {
          title.length > 140-linkLength match {
            case true => title.substring(0, 140-linkLength)
            case false => title
          }
        }
        Twitter.tweet(newTitle + " " + link, user.twitterToken, user.twitterSecret)
      }
    }
  }
}
