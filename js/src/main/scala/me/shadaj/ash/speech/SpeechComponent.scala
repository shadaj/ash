package me.shadaj.ash.speech

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.all._

import scala.scalajs.js

object SpeechComponent {
  val rc = ReactComponentB[(String, Any, Boolean, () => Unit)]("SpeechComponent")
    .render($ => null)
    .shouldComponentUpdate(u => u.$.props._2 != u.nextProps._2)
    .componentDidMount($ => maybeSpeak($.props))
    .componentDidUpdate(u => maybeSpeak(u.$.props))
    .build

  def apply(text: String, definingValues: Any, on: Boolean, onComplete: () => Unit): ReactComponentU[(String, Any, Boolean, () => Unit), Unit, Unit, TopNode] = {
    rc((text, definingValues, on, onComplete))
  }

  private def maybeSpeak(props: (String, Any, Boolean, () => Unit)) = Callback {
    if (props._3) {
      SpeechUtils.say(props._1)
      props._4()
    }
  }
}
