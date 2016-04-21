package me.shadaj.ash.communication

import boopickle.Default._
import japgolly.scalajs.react.{ReactComponentB, ReactDOM}
import japgolly.scalajs.react.vdom.{all => v}
import v._
import me.shadaj.appa.{Actor, ActorRef}
import me.shadaj.ash.spotify.SpotifyCardContainer
import org.scalajs.dom
import com.payalabs.scalajs.react.mdl._
import me.shadaj.ash.speech.SpeechDetectorContainer

import scala.scalajs.js.JSApp
import scala.scalajs.js.typedarray.{ArrayBuffer, TypedArrayBuffer}
import scala.scalajs.js.typedarray.TypedArrayBufferOps._

object WebSocketConnection extends JSApp {
  def main(): Unit = {
    connect(s"ws://${dom.window.location.origin.get.split("//")(1)}/socket")

    val tagMods: List[TagMod] = ServiceStore.cards.map(c => _react_fragReactNode(c))
    val component = ReactComponentB[Unit]("AshCards").render { _ =>
      div(className := "mdl-layout mdl-js-layout mdl-layout--fixed-header")(
        header(className := "mdl-layout__header")(
          div(className := "mdl-layout__header-row")(
            span(className := "mdl-layout-title")("ASH"),
            div(className := "mdl-layout-spacer"),
            SpeechDetectorContainer()
          )
        ),
        v.main(className := "mdl-layout__content")(
          div(className := "card-container")(
            tagMods: _*
          )
        )
      ).material
    }.build()

    ReactDOM.render(component, dom.document.getElementById("main-content"))
  }

  def connect(url: String) = {
    val socket = new dom.WebSocket(url)
    val messenger = new ServiceMessenger(new ActorRef {
      override def actor: Actor = new Actor {
        override def receive: Receive = {
          case m@PickledMessage(_, _, _, _) =>
            socket.send(Pickle.intoBytes(m).arrayBuffer())
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
