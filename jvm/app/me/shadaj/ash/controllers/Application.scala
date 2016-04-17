package me.shadaj.ash.controllers

import java.nio.ByteBuffer

import akka.actor.{Actor, Props}
import me.shadaj.ash.communication.{ServiceMessenger, PickledMessage}
import play.api.libs.ws.WS
import play.api.mvc.WebSocket.FrameFormatter
import play.api.mvc._
import boopickle.Default._
import play.api.Play.current
import scala.concurrent.ExecutionContext.Implicits.global

object Application extends Controller {
  def index = Action {
    Ok(views.html.index("opt"))
  }

  def indexFast = Action {
    Ok(views.html.index("fastopt"))
  }

  def imageProxy(url: String) = Action.async {
    WS.url(url).get().map(r => Ok(r.bodyAsBytes).as(s"image/${url.split('.').last}"))
  }

  implicit val byteBufferFormatter = FrameFormatter.byteArrayFrame.transform[PickledMessage](Pickle.intoBytes(_).array(),
                                                                                             b => Unpickle[PickledMessage].fromBytes(ByteBuffer.wrap(b)))

  def socket = WebSocket.acceptWithActor[PickledMessage, PickledMessage](request => up => Props(new ServiceMessenger(up)))
}