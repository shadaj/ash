package me.shadaj.ash.speech

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

trait SpeechSynthesisVoice extends js.Object {
  val voiceURI: String = js.native
}

@JSName("SpeechSynthesisUtterance")
class SpeechSynthesisUtterance(var text: String) extends js.Object {
  var voice: SpeechSynthesisVoice = js.native
}

@JSName("speechSynthesis")
object SpeechSynthesis extends js.Object {
  def speak(msg: SpeechSynthesisUtterance): Unit = js.native
  def getVoices(): js.Array[SpeechSynthesisVoice] = js.native
}