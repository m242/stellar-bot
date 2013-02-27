package models

import org.ektorp.support.CouchDbDocument
import org.codehaus.jackson.annotate.JsonProperty
import services.CouchDB

/**
 * Created with IntelliJ IDEA.
 * User: markbe
 * Date: 2/6/13
 * Time: 3:41 PM
 */

class Entry extends CouchDbDocument {
  @JsonProperty var entryId: String = null
  @JsonProperty var timestamp: Double = 0
  @JsonProperty var posted: Boolean = false
  @JsonProperty var _deleted_conflicts: java.util.List[String] = null
}

object Entry {
  def apply(id: String, twitterName: String, timestamp: Double) = {
    val e = new Entry
    e.setId(key(twitterName, id))
    e.entryId = id
    e.timestamp = timestamp
    e.posted = false
    e
  }

  def apply(id: String) = CouchDB.db.get[Entry](classOf[Entry], id)

  def key(twitterName: String, entryId: String) = twitterName + "|" + entryId
}
