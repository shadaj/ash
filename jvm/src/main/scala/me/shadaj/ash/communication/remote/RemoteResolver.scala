package me.shadaj.ash.communication.remote

import java.net.{DatagramPacket, DatagramSocket, InetAddress}

import akka.actor.{ActorContext, ActorRef}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

object RemoteResolver {
  private val socket = new DatagramSocket(8888, InetAddress.getByName("0.0.0.0"))
  socket.setBroadcast(true)

  private def resolve(id: String, context: ActorContext): Future[ActorRef] = {
    val buffer = new Array[Byte](15000)
    val packet = new DatagramPacket(buffer, buffer.length)
    socket.receive(packet)

    val message = new String(packet.getData)
    if (message.startsWith("ASH-REMOTE")) {
      val messageSplit = message.split(';')
      if (messageSplit(1) == id) {
        val path = akka.actor.ActorPath.fromString(messageSplit.last.trim)
        println(s"Connecting to remote: $path")
        context.actorSelection(path).resolveOne(5 seconds)
      } else {
        resolve(id, context)
      }
    } else {
      resolve(id, context)
    }
  }

  def remote(id: String, context: ActorContext): Future[ActorRef] = {
    Future(resolve(id, context)).flatMap(identity)
  }
}
