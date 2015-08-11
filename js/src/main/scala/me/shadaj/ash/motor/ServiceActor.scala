package me.shadaj.ash.motor

import me.shadaj.appa.Actor
import me.shadaj.ash.communication.{ServiceMessenger, Initialize}
import me.shadaj.ash.speech.{SpeechActionHandler, SpeechUtils}
import org.scalajs.dom
import org.scalajs.dom.{Event, html}
import org.scalajs.dom.raw.{MouseEvent, HTMLElement}
import rx.core._
import scalatags.rx.all._

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport

import scalatags.JsDom.all._

import me.shadaj.ash.mdl._

@JSExport
object ServiceActor extends Actor {
  override def receive: Receive = {
    case Initialize =>
      val connectionStatus = Var("Connecting...")

      val toggle = MaterialButton("mdl-button--fab mdl-js-ripple-effect mdl-button--colored")(
        i(`class` := "material-icons")("loop")
      ).render

      toggle.onclick = (e: MouseEvent) => {
        ServiceMessenger.current ! ToggleRotate()
      }

      SpeechActionHandler.onPrefix("toggle rotation", "toggle motor rotation") { _ =>
        ServiceMessenger.current ! ToggleRotate()
      }

      val card = div(`class` := "mdl-card mdl-shadow--4dp", width := "100%")(
        div(`class` := "mdl-card__supporting-text", width := "100%")(
          h1(connectionStatus)
        ),
        div(`class` := "mdl-card__actions mdl-card--border text-center")(
          toggle
        )
      ).render
      dom.document.getElementById("card-container").appendChild(
        div(`class` := "mdl-cell mdl-cell--4-col")(
          card
        ).render)
      context.become(withCard(connectionStatus))
  }

  def withCard(connectionStatus: Var[String]): Receive = {
    case Connected() =>
      connectionStatus() = "Connected!"

    case _ =>
  }
}
