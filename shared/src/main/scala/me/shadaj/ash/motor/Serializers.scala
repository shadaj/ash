package me.shadaj.ash.motor

import boopickle.{CompositePickler, Pickler, Unpickler}
import me.shadaj.ash.communication.Serializers

case class Connected()
case class ToggleRotate()

object Serializers extends Serializers {
  val picklerPair = CompositePickler[AnyRef].
    addConcreteType[Connected].
    addConcreteType[ToggleRotate]
  override val pickler: Pickler[AnyRef] = picklerPair.pickler
  override val unpickler: Unpickler[AnyRef] = picklerPair.unpickler
}
