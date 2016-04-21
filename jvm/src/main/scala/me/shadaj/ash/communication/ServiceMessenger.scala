package me.shadaj.ash.communication

import akka.actor.{Actor, ActorRef, ActorSystem, PoisonPill, Props}
import boopickle.Default.{Pickle, PickleState, Unpickle}
import me.shadaj.ash.Resources

import scala.collection.mutable
import scala.reflect.runtime.currentMirror

case object Initialize
case class WithUpstream(up: ActorRef)

case class MessageToSend(from: ActorRef, toService: String, msg: Any)
case class MessageToForward(to: ActorRef, msg: Any)

class ServiceMessenger extends Actor {
  ServiceStore.actors.values.foreach(t => t._2 ! Initialize)

  val clientActors = ServiceStore.actors.keys.map(service => service ->
    ServiceMessenger.system.actorOf(Props(new ClientActor(service, self)))).toMap

  clientActors.foreach { case (service, ref) =>
    ServiceMessenger.all(service) = ref :: ServiceMessenger.all.getOrElse(service, List.empty)
  }

  override def postStop() = {
    clientActors.foreach { case (service, ref) =>
      ServiceMessenger.all(service) = ServiceMessenger.all.getOrElse(service, List.empty).filterNot(_ == ref)
    }

    clientActors.values.foreach(_ ! PoisonPill)
  }

  override def receive: Receive = {
    case p@PickledMessage(from, to, protocol, data) =>
      val clientActor: ActorRef = clientActors(from)

      val serializers = ServiceStore.actors(protocol)._1
      val (_, toActor) = ServiceStore.actors(to)

      clientActor ! MessageToForward(toActor, Unpickle[AnyRef](serializers.pickler).fromBytes(data))
    case WithUpstream(up) =>
      context.become(withUpstream(up))
  }

  def withUpstream(up: ActorRef): Receive = {
    case p@PickledMessage(from, to, protocol, data) =>
      val clientActor: ActorRef = clientActors(from)

      val serializers = ServiceStore.actors(protocol)._1
      val (_, toActor) = ServiceStore.actors(to)

      clientActor ! MessageToForward(toActor, Unpickle[AnyRef](serializers.pickler).fromBytes(data))
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
        println(s"Unknown serializer for message from $fromService to $toService")
      }
  }
}

object ServiceMessenger {
  private[ServiceMessenger] val all = mutable.Map[String, List[ActorRef]]()
  private[ServiceMessenger] val system = ActorSystem("ash-client-interfaces")

  def broadcast(serviceID: String, msg: Any)(implicit from: ActorRef) = {
    all(serviceID).foreach(_.tell(msg, from))
  }
}

object ServiceStore {
  def getSerializers(servicePackage: String) = {
    val reflectModule = currentMirror.reflectModule(
      currentMirror.staticModule(servicePackage + ".Serializers")
    )
    reflectModule.instance.asInstanceOf[Serializers]

  }
  private val actorClasses = Resources.services.map { l =>
    l -> (
      getSerializers(l),
     Class.forName(l + ".ServiceActor")
    )
  }

  private val system = ActorSystem("ash-services")
  val actors = actorClasses.map(c => c._1 -> (c._2._1, system.actorOf(Props(c._2._2)))).toMap
  val serviceForRef = actors.map(t => t._2._2 -> t._1)
}
