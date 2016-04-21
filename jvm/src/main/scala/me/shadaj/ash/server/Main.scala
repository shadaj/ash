package me.shadaj.ash.server

import java.nio.ByteBuffer

import akka.actor.{ActorSystem, PoisonPill, Props}

import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Framing, Sink, Source}
import akka.util.ByteString

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.MediaType.Binary
import akka.http.scaladsl.model.{HttpResponse, MediaTypes}
import akka.http.scaladsl.model.ws.{BinaryMessage, Message}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.RouteResult

import boopickle.Default._
import dispatch._

import me.shadaj.ash.communication.{PickledMessage, ServiceMessenger, WithUpstream}

object Main extends App {
  implicit val system = ActorSystem()

  def websocketFlow: Flow[Message, Message, Any] = {
    val messenger = system.actorOf(Props[ServiceMessenger])

    val toMessenger =
      Flow[Message].filter(_.isInstanceOf[BinaryMessage.Strict]).map { msg =>
        Unpickle[PickledMessage].fromBytes(ByteBuffer.wrap(
          msg.asInstanceOf[BinaryMessage.Strict].data.toArray[Byte]
        ))
      }.to(Sink.actorRef(messenger, PoisonPill))

    val fromMessenger =
      Source.actorRef[PickledMessage](1, OverflowStrategy.fail)
        .map(m => BinaryMessage(ByteString.fromArray(Pickle.intoBytes(m).array())))
        .mapMaterializedValue(messenger ! WithUpstream(_))

    Flow.fromSinkAndSource(toMessenger, fromMessenger)
  }

  implicit val materializer = ActorMaterializer()

  implicit val executionContext = system.dispatcher

  val route =
    path("socket") {
      get {
        handleWebSocketMessages(websocketFlow)
      }
    } ~ pathSingleSlash {
      getFromResource("META-INF/resources/index-opt.html")
    } ~ path("fast") {
      getFromResource("META-INF/resources/index-fast.html")
    } ~ pathPrefix("proxy") {
      get { ctx =>
        dispatch.Http(url(ctx.unmatchedPath.toString().tail)).map { r =>
          val key = r.getContentType.split('/')
          RouteResult.Complete(HttpResponse().withEntity(
            MediaTypes.getForKey(key.head, key.last).get.asInstanceOf[Binary].toContentType,
            r.getResponseBodyAsBytes))
        }
      }
    } ~ pathPrefix("") {
      encodeResponse(getFromResourceDirectory("META-INF/resources"))
    }

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)
}
