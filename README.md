stellar-bot
===========

Want to host your own site where people can create Stellar bots? This is written in Scala, using the Play! framework.

Prerequisites:

* [Play! framework version 2.10](http://playframework.org/) to build.
* [Apache CouchDB](http://couchdb.apache.org/) for document storage
* Recommended, but not required, is a web front-end. I use nginx.


You also need a design doc in your database, "_design/users":

```
  {
   "_id": "_design/users",
   "_rev": "1-42618de062b776f560e2e866003472ac",
   "language": "javascript",
   "views": {
       "all": {
           "map": "function(doc) {\n  if(doc.twitterName) emit(doc.twitterName, doc);\n}"
       }
    }
  }
```

