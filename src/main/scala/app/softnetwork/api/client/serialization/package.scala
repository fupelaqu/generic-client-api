package app.softnetwork.api.client

import org.json4s.ext.{JavaTypesSerializers, JodaTimeSerializers}
import org.json4s.jackson.Serialization
import org.json4s._

import app.softnetwork.serialization.JavaTimeSerializers

import scala.language.implicitConversions

/** Created by smanciot on 01/04/2021.
  */
package object serialization {

  val defaultFormats: Formats =
    Serialization.formats(NoTypeHints) ++
    JodaTimeSerializers.all ++
    JavaTypesSerializers.all ++
    JavaTimeSerializers.all

}
