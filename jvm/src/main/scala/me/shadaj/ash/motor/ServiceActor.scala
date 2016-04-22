package me.shadaj.ash.motor

import akka.actor._
import akka.pattern.pipe
import me.shadaj.ash.communication.remote.RemoteResolver

import scala.concurrent.ExecutionContext.Implicits.global
import me.shadaj.ash.communication.{Initialize, ServiceMessenger}
import me.shadaj.ash.motor

import scala.language.postfixOps

class ServiceActor extends Actor {
  def receive: Receive = {
    case Initialize =>
      RemoteResolver.remote("me.shadaj.ash.motor", context).pipeTo(self)
    case remote: ActorRef =>
      println(s"Connected to remote: $remote")
      context.watch(remote)
      remote ! motor.remote.Hello(self)
      ServiceMessenger.broadcast("me.shadaj.ash.motor", Connected())
      context.become(connected(remote))
  }

  def connected(remote: ActorRef): Receive = {
    case Initialize =>
      sender() ! Connected()
    case ToggleRotate() =>
      remote ! motor.remote.ToggleRotate
    case Terminated(`remote`) =>
      context.become(receive)
      RemoteResolver.remote("me.shadaj.ash.motor", context).pipeTo(self)
    case _ =>
  }
}
