package me.shadaj.ash.communication

import akka.actor.{ActorRef, Props, ActorSystem, Actor}
import boopickle.Default.{PickleState, Pickle, Unpickle}
import me.shadaj.ash.Resources

import scala.collection.mutable
import scala.reflect.runtime.currentMirror

case object Initialize

class ServiceMessenger(up: ActorRef) extends Actor {
  ServiceStore.actors.values.foreach(t => t._2 ! Initialize)
  ServiceMessenger.all.add(self)

  override def postStop() = {
    ServiceMessenger.all.remove(self)
  }

  override def receive: Receive = {
    case p@PickledMessage(service, data) =>
      val (serializers, actor) = ServiceStore.actors(service)
      actor ! Unpickle[AnyRef](serializers.pickler).fromBytes(data)
    case data =>
      val service = ServiceStore.serviceForRef(sender())
      val (serializers, _) = ServiceStore.actors(service)
      up ! PickledMessage(service, Pickle.intoBytes(data.asInstanceOf[AnyRef])(implicitly[PickleState], serializers.pickler))
  }
}

object ServiceMessenger {
  val all = mutable.Set[ActorRef]()

  def broadcast(msg: Any)(implicit from: ActorRef) = {
    all.foreach(_.tell(msg, from))
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
