package me.shadaj.ash.communication

import akka.actor.{Actor, ActorRef}

final class ClientActor(serviceID: String, messenger: ActorRef) extends Actor {
  override def receive: Receive = {
    case MessageToForward(to, msg) =>
      to ! msg
    case msg =>
      messenger ! MessageToSend(sender(), serviceID, msg)
  }
}
