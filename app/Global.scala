import java.util.Date
import play.api.cache.Cache
import play.api.mvc.{RequestHeader, Result}
import play.api.{Logger, GlobalSettings}
import play.api.libs.concurrent.Akka
import play.api.Play.current
import concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits._
import language.postfixOps
import services.Atom
import play.api.mvc.Results._

/**
 * Created with IntelliJ IDEA.
 * User: markbe
 * Date: 2/6/13
 * Time: 4:28 PM
 */

object Global extends GlobalSettings {

  val KEY = "stellar"

  override def onStart(app: play.api.Application) {
    Akka.system.scheduler.schedule(5 seconds, 5 minutes) {
      Cache.get(KEY)(app).asInstanceOf[Option[Boolean]].getOrElse(false) match {
        case true => {}
        case false => {
          Cache.set(KEY, true)(app)
          try {
            Logger debug "Checking Stellar feeds: " + new Date().getTime
            Atom.schedule()
          } catch {
            case e: Exception => Logger error "Exception in Stellar job: " + e.toString
          } finally {
            Cache.set(KEY, false)(app)
          }
        }
      }
    }
  }

  override def onHandlerNotFound(request: RequestHeader): Result = {
    NotFound(views.html.notfound()(request))
  }

  override def onError(request: RequestHeader, e: Throwable): Result = {
    InternalServerError(views.html.error(e))
  }

}
