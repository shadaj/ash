package me.shadaj.ash.communication

import boopickle.Default._

import scala.scalajs.js.annotation.JSExportDescendentObjects

@JSExportDescendentObjects
trait Serializers {
  val pickler: Pickler[AnyRef]
}
