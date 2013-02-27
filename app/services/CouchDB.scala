package services

import play.api.Play
import org.ektorp.http.StdHttpClient
import org.ektorp.impl.StdCouchDbInstance
import models.{Entry, User}
import collection.JavaConversions
import org.ektorp.ViewQuery
import play.api.Play.current

/**
 * Created with IntelliJ IDEA.
 * User: markbe
 * Date: 2/6/13
 * Time: 3:50 PM
 */

/*

{
  "_id": "twitterName|itemId",
  "entryid": "entryid",
  "timestamp": 1234567890,
  "posted": false
}

{
  "_id": "(..)",
  "twitterName": "m242dev",
  "twitterToken": "(...)",
  "twitterSecret": "(...)",
  "stellarName": "m242"
}

{
  "_id": "_design/users",
  "language": "javascript",
  "views": {
    "all": {
      "map": "function(doc) {\n  if(doc.twitterName) emit(doc.twitterName, doc);\n}"
    }
  }
}

*/

object CouchDB {

  val db = new StdCouchDbInstance(
    new StdHttpClient.Builder().url(
      "http://" + Play.configuration.getString("couchdb.hostname").getOrElse("localhost") + ":" +
        Play.configuration.getInt("couchdb.port").getOrElse(5984) + "/").socketTimeout(30000).build()
  ).createConnector(Play.configuration.getString("couchdb.database").getOrElse("stellarbot"), true)

  def getUsers: List[User] = {
    JavaConversions.asScalaBuffer[User](
      db.queryView(new ViewQuery()
        .designDocId("_design/users")
        .viewName("all")
      , classOf[User])
    ).toList
  }

  def getUser(twitterName: String): Option[User] = {
    JavaConversions.asScalaBuffer[User](
      db.queryView(new ViewQuery()
        .designDocId("_design/users")
        .viewName("all")
        .key(twitterName)
      , classOf[User])
    ).headOption
  }

  def getEntry(id: String): Option[Entry] = {
    try {
      Option(db.get[Entry](classOf[Entry], id))
    } catch {
      case e: Exception => None
    }
  }
}

