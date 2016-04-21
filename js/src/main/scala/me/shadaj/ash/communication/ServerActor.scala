package me.shadaj.ash.communication

import me.shadaj.appa.{Actor, ActorRef}

final class ServerActor(serviceID: String, messenger: ActorRef) extends Actor {
  override def receive: Receive = {
    case MessageToForward(to, msg) =>
      to ! msg
    case msg =>
      messenger ! MessageToSend(sender(), serviceID, msg)
  }
}
