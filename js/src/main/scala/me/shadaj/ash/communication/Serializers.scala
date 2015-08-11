package me.shadaj.ash.communication

import boopickle.{Pickler, Unpickler}

import scala.scalajs.js.annotation.JSExportDescendentObjects

@JSExportDescendentObjects
trait Serializers {
  val pickler: Pickler[AnyRef]
  val unpickler: Unpickler[AnyRef]
}
