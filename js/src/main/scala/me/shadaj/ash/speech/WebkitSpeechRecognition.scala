package me.shadaj.ash.speech

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSBracketAccess, JSName}

trait SpeechResultData extends js.Object {
  val transcript: String = js.native
}

trait SpeechResult extends js.Object {
  val isFinal: Boolean = js.native

  @JSBracketAccess
  def apply(idx: Int): SpeechResultData = js.native
}

trait SpeechEvent extends js.Object {
  val resultIndex: Int = js.native
  val results: js.Array[SpeechResult] = js.native
}

@JSName("webkitSpeechRecognition")
class WebkitSpeechRecognition extends js.Object {
  var continuous: Boolean = js.native
  var interimResults: Boolean = js.native

  var onend: js.Function0[Unit] = js.native
  var onresult: js.Function1[SpeechEvent, Unit] = js.native

  def start(): Unit = js.native
  def abort(): Unit = js.native
}
