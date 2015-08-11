package me.shadaj.ash.communication

import boopickle.{Unpickle, Pickle}
import me.shadaj.appa.{ActorRef, Actor}
import org.scalajs.dom

import scala.scalajs.js.JSApp

import scala.scalajs.js.typedarray.{ArrayBuffer, TypedArrayBuffer}
import scala.scalajs.js.typedarray.TypedArrayBufferOps._

object WebSocketConnection extends JSApp {
  def main(): Unit = {
    connect(s"ws://${dom.window.location.origin.split("//")(1)}/socket")
  }

  def connect(url: String) = {
    val socket = new dom.WebSocket(url)
    val messenger = new ServiceMessenger(new ActorRef {
      override def actor: Actor = new Actor {
        override def receive: Receive = {
          case m@PickledMessage(_, _) =>
            socket.send(Pickle.intoBytes(m).typedArray())
        }
      }
    }).ref

    socket.binaryType = "arraybuffer"

    socket.onmessage = (e: dom.MessageEvent) => {
      messenger.!(Unpickle[PickledMessage].fromBytes(TypedArrayBuffer.wrap(e.data.asInstanceOf[ArrayBuffer])))(null)
    }

    socket.onopen = (_: dom.Event) => {
      println("opened ws connection")
    }

    socket.onclose = (_: dom.Event) => {
      main()
    }
  }
}
