package me.shadaj.ash.motor.remote

import java.net._

import akka.actor._
import akka.io.{Udp, IO}
import akka.util.ByteString
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import akka.pattern.pipe
import scala.collection.JavaConversions._
import scala.sys.process.Process

case object BroadcastSelf
case class Hello(main: ActorRef)
case object ToggleRotate
case object StopRotating

class RemoteMotor extends Actor {
  val socket = new DatagramSocket()
  socket.setBroadcast(true)

  val broadcastAddress = NetworkInterface.getNetworkInterfaces.filterNot(_.isLoopback).
    flatMap(_.getInterfaceAddresses.flatMap(i =>
      Option(i.getBroadcast)
    )).next()

  val selfAddress = context.system.asInstanceOf[ExtendedActorSystem].provider.getDefaultAddress

  context.system.scheduler.scheduleOnce(1 second, self, BroadcastSelf)

  override def receive: Receive = {
    case BroadcastSelf =>
      val toSend = s"ASH-REMOTE;me.shadaj.me.shadaj.ash.motor;${self.path.toStringWithAddress(selfAddress)}"

      println(s"BROADCAST: $toSend")

      val toSendBytes = toSend.getBytes
      val packet = new DatagramPacket(toSendBytes, toSendBytes.length, broadcastAddress, 8888)
      socket.send(packet)

      context.system.scheduler.scheduleOnce(1 second, self, BroadcastSelf)

    case Hello(main) =>
      println(s"Connected to $main")
      context.watch(main)
      context.become(connected(main))
  }

  def connected(main: ActorRef): Receive = {
    case Terminated(`main`) =>
      context.become(receive)
      self ! BroadcastSelf
    case ToggleRotate =>
      context.become(rotating(Process(
        "/usr/bin/python /home/pi/Adafruit-Motor-HAT-Python-Library/examples/StepperTest.py"
      ).run(), main))
  }

  def rotating(process: Process, main: ActorRef): Receive = {
    case ToggleRotate =>
      process.destroy()
      context.become(connected(main))
    case Terminated(`main`) =>
      process.destroy()
      context.become(receive)
      self ! BroadcastSelf
  }
}
