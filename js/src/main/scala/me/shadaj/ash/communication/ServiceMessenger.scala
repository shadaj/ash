package me.shadaj.ash.communication

import boopickle.Default._
import japgolly.scalajs.react._
import me.shadaj.appa.{Actor, ActorRef}

import scala.collection.mutable
import scala.scalajs.js.Dynamic
import me.shadaj.ash.Resources

case object Initialize

case class MessageToSend(from: ActorRef, toService: String, msg: Any)
case class MessageToForward(to: ActorRef, msg: Any)

final class ServiceMessenger(up: ActorRef) extends Actor {
  ServiceStore.actors.values.foreach(t => t._2 ! Initialize)

  ServiceMessenger._serverActors =
    ServiceStore.actors.keys.map(service => service -> new ServerActor(service, self).self).toMap

  override def receive: Receive = {
    case p@PickledMessage(from, to, protocol, data) =>
      val serverActor: ActorRef = ServiceMessenger.serverActor(from)

      val serializers = ServiceStore.actors(protocol)._1
      val (_, toActor) = ServiceStore.actors(to)

      serverActor ! MessageToForward(toActor, Unpickle[AnyRef](serializers.pickler).fromBytes(data))
    case MessageToSend(from, toService, msg) =>
      val fromService = ServiceStore.serviceForRef(from)

      val fromSerializers = ServiceStore.actors(fromService)._1
      val (toSerializers, _) = ServiceStore.actors(toService)

      val fromCan = fromSerializers.pickler.picklers.exists(_._1 == msg.getClass.getName)
      val toCan = toSerializers.pickler.picklers.exists(_._1 == msg.getClass.getName)

      if (fromCan) {
        println(s"Sending $msg with protocol of $fromService")
        up ! PickledMessage(fromService, toService, fromService, Pickle.intoBytes(msg.asInstanceOf[AnyRef])(implicitly[PickleState], fromSerializers.pickler))
      } else if (toCan) {
        println(s"Sending $msg with protocol of $toService")
        up ! PickledMessage(fromService, toService, toService, Pickle.intoBytes(msg.asInstanceOf[AnyRef])(implicitly[PickleState], toSerializers.pickler))
      } else {
        println(s"Unknown serializer for message $msg from $fromService to $toService")
      }
  }
}

object ServiceMessenger {
  private[ServiceMessenger] var _serverActors: Map[String, ActorRef] = null
  def serverActor(id: String): ActorRef = _serverActors(id)
}

private[communication] object ServiceStore {
  val lines = Resources.services
  private val actorClasses = lines.map { l =>
    val basePackage = Dynamic.global.eval(l)
    l -> (basePackage.Serializers().asInstanceOf[Serializers],
                 basePackage.ServiceActor().asInstanceOf[Actor])
  }

  val cards: List[ReactComponentU[_, _, _, TopNode]] = actorClasses.map(c => c._2._2.asInstanceOf[Dynamic].card.asInstanceOf[ReactComponentU[_, _, _, TopNode]])
  val actors: Map[String, (Serializers, ActorRef)] = actorClasses.map(c => c._1 -> (c._2._1, c._2._2.ref)).toMap
  val serviceForRef: Map[ActorRef, String] = actors.map(t => t._2._2 -> t._1)
}
