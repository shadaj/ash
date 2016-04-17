package me.shadaj.ash.communication

import me.shadaj.appa.Actor

trait Subscribers[Message] extends Actor {
  private var subscribers: List[PartialFunction[Message, Unit]] = List.empty

  def messageHandler: Receive
  def receive = {
    case m: Message =>
      publish(m)
      messageHandler(m)
    case o =>
      messageHandler(o)
  }

  def subscribe(pf: PartialFunction[Message, Unit]): Unit = {
    subscribers = pf :: subscribers
  }

  private def publish(message: Message): Unit = {
    subscribers.foreach { s =>
      if (s.isDefinedAt(message)) {
        s(message)
      }
    }
  }
}
