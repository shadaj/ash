package me.shadaj.ash.motor

import me.shadaj.appa.Actor
import me.shadaj.ash.communication.Subscribers
import scala.scalajs.js.annotation.JSExport

@JSExport
object ServiceActor extends Actor with Subscribers[MotorMessage] {
  @JSExport
  val card = MotorCardContainer()

  override def messageHandler: Receive = {
    case _ =>
  }
}
