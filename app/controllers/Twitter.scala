package controllers

import play.api.mvc.{RequestHeader, Action, Controller}
import play.api.libs.oauth._
import play.api.Play
import twitter4j.conf.ConfigurationBuilder
import twitter4j.TwitterFactory
import play.api.libs.ws.WS
import play.api.libs.oauth.ServiceInfo
import play.api.libs.oauth.OAuth
import play.api.libs.oauth.OAuthCalculator
import play.api.libs.oauth.ConsumerKey
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import twitter4j.auth.AccessToken
import concurrent.Future

/**
 * Created with IntelliJ IDEA.
 * User: mark
 * Date: 2/6/13
 * Time: 11:09 PM
 */

object Twitter extends Controller {

  val KEY = ConsumerKey(Play.configuration.getString("twitter.key").getOrElse("x"),
    Play.configuration.getString("twitter.secret").getOrElse("y"))

  val TwitterFactory = {
    val c = new ConfigurationBuilder()
    c.setOAuthConsumerKey(Play.configuration.getString("twitter.key").getOrElse("x"))
    c.setOAuthConsumerSecret(Play.configuration.getString("twitter.secret").getOrElse("y"))
    c.setDebugEnabled(true)
    new TwitterFactory(c.build())
  }

  val TWITTER = OAuth(ServiceInfo(
    "https://api.twitter.com/oauth/request_token",
    "https://api.twitter.com/oauth/access_token",
    "https://api.twitter.com/oauth/authenticate",
    KEY
  ), use10a = true)

  def callback = Action { request =>
    request.queryString("oauth_verifier").map { verifier =>
      val tokenPair = sessionTokenPair(request).get
      TWITTER.retrieveAccessToken(tokenPair, verifier) match {
        case Right(t) => {
          Async {
            WS.url("https://api.twitter.com/1.1/account/settings.json")
              .sign(OAuthCalculator(Twitter.KEY, RequestToken(t.token, t.secret)))
              .get()
              .map(result => {
                Redirect(routes.Application.index()).withSession("token" -> t.token,
                  "secret" -> t.secret, "screen_name" -> (result.json \ "screen_name").as[String])
              })
          }
        }
        case Left(e) => throw e
      }
    }.head
  }

  def auth = Action { request =>
    TWITTER.retrieveRequestToken("http://" + request.host + routes.Twitter.callback()) match {
      case Right(t) => {
        Redirect(TWITTER.redirectUrl(t.token)).withSession("token" -> t.token, "secret" -> t.secret)
      }
      case Left(e) => throw e
    }
  }

  def sessionTokenPair(implicit request: RequestHeader): Option[RequestToken] = {
    for {
      token <- request.session.get("token")
      secret <- request.session.get("secret")
    } yield {
      RequestToken(token, secret)
    }
  }

  /*def sendTweet(tweet: Tweet): Future[Boolean] = {
    Future[Boolean] {
      TwitterFactory.getInstance(new AccessToken(tweet.token, tweet.secret)).updateStatus(tweet.tweet).getText.length > 0
    }
  }*/

  def tweet(post: String, token: String, secret: String): Future[Boolean] = {
    Future {
      TwitterFactory.getInstance(new AccessToken(token, secret)).updateStatus(post).getId > 0
    }
  }

  def retweet(id: Long, token: String, secret: String): Future[Boolean] = {
    Future {
      TwitterFactory.getInstance(new AccessToken(token, secret)).retweetStatus(id).getId > 0
    }
  }

}
