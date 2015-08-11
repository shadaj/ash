package me.shadaj.ash.motor.remote

import java.net.{NetworkInterface, InetAddress, DatagramPacket, DatagramSocket}

import akka.actor.{Props, ActorSystem}
import com.typesafe.config.ConfigFactory

object Main extends App {
  val address = {
    import collection.JavaConversions._
    NetworkInterface.getNetworkInterfaces.
      flatMap(_.getInetAddresses).find { address =>
        val host = address.getHostAddress
        host.contains(".") && !address.isLoopbackAddress
      }.getOrElse(InetAddress.getLocalHost).getHostAddress
  }

  val system = ActorSystem(
    "motor-remote",
    ConfigFactory.parseString(s"akka.remote.netty.tcp.hostname=$address").
      withFallback(ConfigFactory.load()))

  val remoteActor = system.actorOf(Props[RemoteMotor])
}
