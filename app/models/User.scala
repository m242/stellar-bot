package models

import org.ektorp.support.CouchDbDocument
import org.codehaus.jackson.annotate.JsonProperty
import services.CouchDB

/**
 * Created with IntelliJ IDEA.
 * User: markbe
 * Date: 2/6/13
 * Time: 3:53 PM
 */

class User extends CouchDbDocument {
  @JsonProperty var twitterName: String = null
  @JsonProperty var twitterToken: String = null
  @JsonProperty var twitterSecret: String = null
  @JsonProperty var stellarName: String = null
  @JsonProperty var _deleted_conflicts: java.util.List[String] = null
}

object User {
  def apply(twitterName: String, twitterToken: String, twitterSecret: String, stellarName: String = null) = {
    val u = new User()
    u setId twitterName
    u.stellarName = stellarName
    u.twitterName = twitterName
    u.twitterSecret = twitterSecret
    u.twitterToken = twitterToken
    u
  }

  def apply(twitterName: String) = CouchDB getUser twitterName
}