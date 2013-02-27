package controllers

import play.api.mvc._
import concurrent.Await
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.iteratee.Enumerator
import org.apache.commons.lang3.StringEscapeUtils
import play.api.libs.ws.WS
import play.api.libs.oauth.{RequestToken, OAuthCalculator}
import services.{Atom, CouchDB}
import play.api.libs.json.Json._
import play.api.data.Form
import play.api.data.Forms._
import java.net.URLEncoder
import language.postfixOps
import concurrent.duration._
import models.User
import play.api.Logger


object Application extends Controller with TwitterSecurity {

  def index = Action { implicit request =>
    Twitter.sessionTokenPair match {
      case Some(credentials) => request.session.get("screen_name") match {
        case Some(screen_name) => {
          // If this user already exists, display the unlink page
          CouchDB.getUser(screen_name) match {
            case Some(user) => {
              Ok(views.html.loggedin(user.twitterName, Some(user.stellarName)))
            }
            case None => {
              Ok(views.html.loggedin(StringEscapeUtils.escapeHtml4(screen_name), None))
            }
          }
        }
        case None => Async {
          WS.url("https://api.twitter.com/1.1/account/settings.json")
            .sign(OAuthCalculator(Twitter.KEY, RequestToken(credentials.token, credentials.secret)))
            .get()
            .map(result => {
            result.body.contains("\"errors\":") match {
              case true => Redirect(routes.Application.index()).withNewSession
              case false => {
                val screenName = (result.json \ "screen_name").as[String]
                CouchDB.getUser(screenName) match {
                  case Some(user) => Ok(views.html.loggedin(user.twitterName, Some(user.stellarName))).withSession(
                    "token" -> credentials.token, "secret" -> credentials.secret, "screen_name" -> screenName)
                  case None => Ok(views.html.loggedin(StringEscapeUtils.escapeHtml4(screenName), None)).withSession(
                    "token" -> credentials.token, "secret" -> credentials.secret, "screen_name" -> screenName)
                }
              }
            }
          })
        }
      }
      case None => Ok(views.html.index())
    }
  }

  def indexHead = Action { implicit request =>
    val body = views.html.index()
    SimpleResult(
      ResponseHeader(200, Map(CONTENT_TYPE -> "text/html; charset=utf-8", CONTENT_LENGTH -> ("" + body.body.length))),
      Enumerator("")
    )
  }

  def signout = Action { implicit request => Ok(views.html.signout()).withNewSession }

  def postStellar = UsernameAction { name => implicit request =>
    Twitter.sessionTokenPair match {
      case Some(token) => {
        val stellarForm = Form(single(
          "stellarName" -> text(minLength = 1)
        ) verifying("That isn't a valid Stellar username", fields => fields match {
          case (s) => {
            try {
              val future = Atom("http://stellar.io/" + URLEncoder.encode(s, "UTF-8") + "/flow/feed")
              Await.result(future, 30 seconds).isDefined
            } catch {
              case e: Exception => false
            }
          }
        }))
        stellarForm.bindFromRequest.fold(
          formWithErrors => BadRequest(toJson(formWithErrors.errorsAsJson)),
          validForm => {
            // Create user
            val user = User(name, token.token, token.secret, validForm)
            CouchDB.db.create(user)
            Logger info "Added user twitter: " + user.twitterName + " stellar: " + user.stellarName
            Atom.schedule(user) match {
              case _ => Ok(toJson(Map("stellarName" -> validForm)))
            }
          }
        )
      }
      case None => BadRequest(toJson(Map("error" -> "no token")))
    }
  }

  def deleteStellar() = UsernameAction { name => implicit request =>
    CouchDB.getUser(name) match {
      case None => BadRequest(toJson(Map("error" -> "no user")))
      case Some(u) => {
        CouchDB.db.delete(u)
        Logger info "Deleted user twitter: " + u.twitterName + " stellar: " + u.stellarName
        Ok(toJson(Map("deleted" -> u.twitterName)))
      }
    }
  }

  def exception = Action {
    throw new Exception("test")
    Ok("test")
  }
}

trait TwitterSecurity extends Controller {

  def UsernameAction(f: => String => Request[AnyContent] => Result) = {
    Action { implicit request =>
      request.session.get("screen_name") match {
        case Some(screen_name) => f(screen_name)(request)
        case None => BadRequest(toJson(Map("error" -> "no screen_name")))
      }
    }
  }
}
