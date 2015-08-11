package me.shadaj.ash.communication

import boopickle.{Pickler, Unpickler}

trait Serializers {
   val pickler: Pickler[AnyRef]
   val unpickler: Unpickler[AnyRef]
 }
