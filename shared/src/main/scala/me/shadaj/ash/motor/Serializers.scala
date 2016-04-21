package me.shadaj.ash.motor

import boopickle.CompositePickler
import boopickle.Default._
import me.shadaj.ash.communication.Serializers

sealed trait MotorMessage
case class Connected() extends MotorMessage
case class ToggleRotate() extends MotorMessage

object Serializers extends Serializers {
  val picklerPair = CompositePickler[AnyRef].
    addConcreteType[Connected].
    addConcreteType[ToggleRotate].
    addConcreteType[String]
  override val pickler: Pickler[AnyRef] = picklerPair
}
