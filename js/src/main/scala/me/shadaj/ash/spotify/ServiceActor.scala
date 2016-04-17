package me.shadaj.ash.spotify

import me.shadaj.appa.Actor
import me.shadaj.ash.communication.Subscribers

import scala.scalajs.js.annotation.JSExport

@JSExport
object ServiceActor extends Actor with Subscribers[SpotifyMessage] {
  @JSExport
  val card = SpotifyCardContainer()

  override def messageHandler = {
    case _ =>
  }
}
