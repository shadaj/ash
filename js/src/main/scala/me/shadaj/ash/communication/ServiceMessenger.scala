package me.shadaj.ash.communication

import boopickle.{PickleState, Pickle, Unpickle}
import me.shadaj.appa.{Actor, ActorRef}

import scala.collection.mutable
import scala.scalajs.js.Dynamic
import me.shadaj.ash.Resources

case object Initialize

final class ServiceMessenger(up: ActorRef) extends Actor {
  ServiceStore.actors.values.foreach(t => t._2 ! Initialize)
  ServiceMessenger._cur = self

  override def receive: Receive = {
    case PickledMessage(service, data) =>
      val (serializers, actor) = ServiceStore.actors(service)
      actor ! Unpickle[AnyRef](serializers.unpickler).fromBytes(data)
    case data =>
      val service = ServiceStore.serviceForRef(sender())
      val (serializers, _) = ServiceStore.actors(service)
      up ! PickledMessage(service, Pickle.intoBytes(data.asInstanceOf[AnyRef])(implicitly[PickleState], serializers.pickler))
  }
}

object ServiceMessenger {
  private[ServiceMessenger] var _cur: ActorRef = null
  def current: ActorRef = _cur
}

object ServiceStore {
  val lines = Resources.services
  private val actorClasses = lines.map { l =>
    val basePackage = Dynamic.global.eval(l)
    l -> (basePackage.Serializers().asInstanceOf[Serializers],
                 basePackage.ServiceActor().asInstanceOf[Actor])
  }

  val actors: Map[String, (Serializers, ActorRef)] = actorClasses.map(c => c._1 -> (c._2._1, c._2._2.ref)).toMap
  val serviceForRef: Map[ActorRef, String] = actors.map(t => t._2._2 -> t._1)
}
