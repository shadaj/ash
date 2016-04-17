package me.shadaj.ash.speech

import org.scalajs.dom
import org.scalajs.dom.html
import org.scalajs.dom.raw.MouseEvent
import rx.core.Var

import scalatags.JsDom.all._
import scalatags.rx.all._

object SpeechActionHandler {
  val currentText = Var("")
  val displayStyle = Var("none")
  val card = div(`class` := "mdl-card mdl-shadow--4dp", width := "100%", display := displayStyle)(
    div(`class` := "mdl-card__supporting-text", width := "100%")(
      h1(currentText)
    )
  ).render

  dom.document.getElementById("card-container").appendChild(
    div(`class` := "mdl-cell mdl-cell--4-col")(
      card
    ).render)

  private var handlers = List[(String, String => Unit)]()
  def onPrefix(prefixes: String*)(handler: String => Unit) = {
    prefixes.foreach(prefix => handlers = (prefix, handler) :: handlers)
  }

  private def onMessage(msg: String) = {
    handlers.find(h => msg.startsWith(h._1)).foreach { case (prefix, callback) =>
      callback(msg.drop(prefix.length).trim)
    }
  }

  private var listening = false
  private val recognition = new WebkitSpeechRecognition

  recognition.continuous = true
  recognition.interimResults = true

  val micButton = dom.document.getElementById("ash-voice").asInstanceOf[html.Anchor]

  private def startListening() = {
    displayStyle() = "block"
    listening = true
    micButton.style.backgroundColor = "red"
  }

  private def stopListening() = {
    displayStyle() = "none"
    listening = false
    micButton.style.backgroundColor = "transparent"
    restart()
  }

  private def restart(then: => Unit) = {
    recognition.onend = () => {
      recognition.onend = null
      recognition.start()
      then
    }

    recognition.abort()
  }

  private val ashTriggers = Seq("ash", "OSH", "ok ash", "okay ash", "hey ash")
  var lastText: String = ""

  recognition.onresult = (e: SpeechEvent) => {
    e.results.drop(e.resultIndex).foreach { result =>
      val text = result(0).transcript.trim
      println(text)

      currentText() = text
      lastText = text

      if (!listening && ashTriggers.exists(w => text.sliding(w.size).contains(w))) {
        restart {
          startListening()
          dom.setTimeout(() => {
            if (listening) {
              currentText() = s"FINAL: $lastText"
              println(s"FINAL $lastText")
              onMessage(lastText)
              stopListening()
              restart()
            }
          }, 5000)
        }
      }

      if (listening && result.isFinal) {
        currentText() = s"FINAL: $text"
        println(s"FINAL $text")
        onMessage(text.trim)
        stopListening()
        restart()
      }
    }
  }

  recognition.start()
  micButton.onclick = (_: MouseEvent) => {
    if (listening) {
      stopListening()
      restart()
    } else {
      startListening()
    }
  }
}
