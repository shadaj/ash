package me.shadaj.ash.speech

import japgolly.scalajs.react.vdom.all._
import japgolly.scalajs.react._

import com.payalabs.scalajs.react.mdl._

object SpeechDetectorContainer {
  private var handlers = List[(String, String => Unit)]()
  def onText(prefixes: String*)(handler: String => Unit) = {
    prefixes.foreach(prefix => handlers = (prefix, handler) :: handlers)
  }

  private def onMessage(msg: String) = {
    handlers.find(h => msg.sliding(h._1.length).contains(h._1)).foreach { case (prefix, callback) =>
      callback(msg.drop(prefix.length).trim)
    }
  }

  class Backend($: BackendScope[Unit, Option[String]]) {
    private val recognition = new WebkitSpeechRecognition

    recognition.continuous = true
    recognition.interimResults = true

    private def startListening() = {
      println("Listening: started")
      $.modState(_ => Some("")).runNow()
      restart()
    }

    private def stopListening() = {
      println("Listening: ending")
      $.modState(_ => None).runNow()
      restart()
    }

    recognition.onend = () => {
      recognition.start()
    }

    private def restart() = {
      println("Recognition: restarting")
      recognition.abort()
    }

    private val ashTriggers = Seq("ash", "osh", "ok ash", "ok i", "okay i", "okay ash", "hey ash")

    recognition.onresult = (e: SpeechEvent) => {
      e.results.drop(e.resultIndex).foreach { result =>
        val text = result(0).transcript.trim.toLowerCase()

        if ($.state.runNow().isDefined) {
          $.modState(_ => Some(text)).runNow()
          if (result.isFinal) {
            onMessage(text)
            stopListening()
          }
        } else {
          if (ashTriggers.exists(w => text.sliding(w.length).contains(w))) {
            startListening()
          }
        }
      }
    }

    recognition.start()
    println("started recognition")

    def render(state: Option[String]) = {
      div(span(marginRight := "15px")(state.getOrElse("").toString), SpeechDetectorComponent(
        state,
        () => if (state.isDefined) stopListening() else startListening()
      ))
    }
  }

  val component = ReactComponentB[Unit](getClass.getSimpleName)
    .initialState(None.asInstanceOf[Option[String]])
    .renderBackend[Backend]
    .build

  def apply() = component()
}

object SpeechDetectorComponent {
  case class Props(listenedText: Option[String], toggleListening: () => Unit)

  class Backend($: BackendScope[Props, Unit]) {
    def render(props: Props) = {
      import props._

      a(className := "mdl-button mdl-js-button mdl-button--icon",
        backgroundColor := (if (listenedText.isDefined) "red" else "transparent"),
        onClick --> Callback(toggleListening()))(
        i(className := "material-icons")("mic")
      ).material
    }
  }

  val component = ReactComponentB[Props](getClass.getSimpleName)
    .stateless
    .renderBackend[Backend]
    .build

  def apply(listenedText: Option[String], toggleListening: () => Unit) =
    component(Props(listenedText, toggleListening))
}
