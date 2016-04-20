package me.shadaj.ash.communication

import boopickle.Default._

trait Serializers {
   val pickler: Pickler[AnyRef]
 }
