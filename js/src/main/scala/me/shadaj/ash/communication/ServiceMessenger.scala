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
    case p@PickledMessage(from, to, data) =>
      val clientActor: ActorRef = ServiceMessenger.serverActor(from)
      val serializers = ServiceStore.actors(from)._1 // server sends in server language
      val (_, toActor) = ServiceStore.actors(to)
      clientActor ! MessageToForward(toActor, Unpickle[AnyRef](serializers.pickler).fromBytes(data))
    case MessageToSend(from, toService, msg) =>
      val fromService = ServiceStore.serviceForRef(from)
      val (serializers, _) = ServiceStore.actors(toService) // server receives in server language
      up ! PickledMessage(fromService, toService, Pickle.intoBytes(msg.asInstanceOf[AnyRef])(implicitly[PickleState], serializers.pickler))
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
