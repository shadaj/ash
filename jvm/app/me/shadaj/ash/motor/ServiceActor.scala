package me.shadaj.ash.motor

import java.net.{DatagramPacket, InetAddress, DatagramSocket}

import akka.actor._
import akka.pattern.pipe

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import me.shadaj.ash.communication.{ServiceMessenger, Initialize}
import me.shadaj.ash.motor

import scala.language.postfixOps

case object CheckForBroadcast

class ServiceActor extends Actor {
  val socket = new DatagramSocket(8888, InetAddress.getByName("0.0.0.0"))
  socket.setBroadcast(true)

  self ! CheckForBroadcast
  def receive: Receive = {
    case Initialize =>
    case CheckForBroadcast =>
      val buffer = new Array[Byte](15000)
      val packet = new DatagramPacket(buffer, buffer.length)
      socket.receive(packet)

      val message = new String(packet.getData)
      if (message.startsWith("ASH-REMOTE")) {
        val messageSplit = message.split(';')
        if (messageSplit(1) == "me.shadaj.ash.motor") {
          val path = akka.actor.ActorPath.fromString(messageSplit.last.trim)
          println(s"Connecting to remote: $path")
          context.actorSelection(path).resolveOne(5 seconds).pipeTo(self)
        } else {
          self ! CheckForBroadcast
        }
      } else {
        self ! CheckForBroadcast
      }

    case remote: ActorRef =>
      println(s"Connected to remote: $remote")
      context.watch(remote)
      remote ! motor.remote.Hello(self)
      ServiceMessenger.broadcast(Connected())
      context.become(connected(remote))
  }

  def connected(remote: ActorRef): Receive = {
    case Initialize =>
      sender() ! Connected()
    case ToggleRotate() =>
      remote ! motor.remote.ToggleRotate
    case Terminated(`remote`) =>
      context.become(receive)
      self ! CheckForBroadcast
    case _ =>
  }
}
