package me.shadaj.ash.speech

import scala.scalajs.js

object SpeechUtils {
  def speechVoices = Seq(SpeechSynthesis.getVoices()).find(_.nonEmpty)

  def googleUSEnglish = speechVoices.flatMap(_.find(_.voiceURI == "Google US English"))

  def say(msg: String) = {
    val speechSynthesis = new SpeechSynthesisUtterance(msg)
    googleUSEnglish.foreach(speechSynthesis.voice = _)
    SpeechSynthesis.speak(speechSynthesis)
  }

  say("")
}
