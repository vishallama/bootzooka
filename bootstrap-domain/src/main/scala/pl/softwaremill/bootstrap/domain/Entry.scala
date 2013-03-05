package pl.softwaremill.bootstrap.domain

import org.joda.time.{DateTimeZone, DateTime}
import org.bson.types.ObjectId

case class Entry(id: ObjectId, text: String, authorId: ObjectId, entered: DateTime)

object Entry {
  def apply(text: String, authorId: ObjectId) = {
    new Entry(new ObjectId, text, authorId, new DateTime(DateTimeZone.UTC))
  }
}
