package me.shadaj.ash.speech

import org.scalajs.dom
import org.scalajs.dom.html
import org.scalajs.dom.raw.MouseEvent

object SpeechActionHandler {
  private var handlers = List[(String, String => Unit)]()
  def onPrefix(prefixes: String*)(handler: String => Unit) = {
    prefixes.foreach(prefix => handlers = (prefix, handler) :: handlers)
  }

  private def onMessage(msg: String) = {
    handlers.find(h => msg.startsWith(h._1)).foreach { case (prefix, callback) =>
      callback(msg.drop(prefix.length).trim)
    }
  }

  private var listeningToIntent = false
  private val recognition = new WebkitSpeechRecognition

  recognition.continuous = true
  recognition.interimResults = true

  val micButton = dom.document.getElementById("ash-voice").asInstanceOf[html.Anchor]

  private def startListening() = {
    listeningToIntent = true
    micButton.style.backgroundColor = "red"
  }

  private def stopListening() = {
    listeningToIntent = false
    micButton.style.backgroundColor = "transparent"
    restart()
  }

  recognition.onend = () => {
    recognition.start()
  }

  private def restart() = {
    recognition.abort()
  }

  private val ashTriggers = Seq("ash", "osh", "ok ash", "ok i", "okay i", "okay ash", "hey ash")

  recognition.onresult = (e: SpeechEvent) => {
    println(e)
    e.results.drop(e.resultIndex).foreach { result =>
      val text = result(0).transcript.trim.toLowerCase()
      println(text)

      if (!listeningToIntent && ashTriggers.exists(w => text.sliding(w.size).contains(w))) {
        startListening()
        restart()
      }

      if (listeningToIntent && result.isFinal) {
        println(s"FINAL $text")
        onMessage(text.trim)
        stopListening()
      }
    }
  }

  recognition.start()
  micButton.onclick = (_: MouseEvent) => {
    if (listeningToIntent) {
      stopListening()
    } else {
      startListening()
    }
  }
}
