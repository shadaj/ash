package me.shadaj.ash.motor

import japgolly.scalajs.react.vdom.all._
import japgolly.scalajs.react._

import com.payalabs.scalajs.react.mdl._

import me.shadaj.ash.communication.ServiceMessenger
import me.shadaj.ash.speech.{SpeechComponent, SpeechDetectorContainer}

object MotorCardContainer {
  class Backend($: BackendScope[Unit, Boolean]) {
    def componentDidMount: Callback = Callback {
      ServiceActor.subscribe {
        case Connected() =>
          $.modState(s => true).runNow()
      }
    }

    def render(state: Boolean) = {
      implicit val sender = ServiceActor.self
      div(MotorCard(
        state,
        () => ServiceMessenger.current ! ToggleRotate()
      ))
    }
  }

  val component = ReactComponentB[Unit](getClass.getSimpleName)
    .initialState(false)
    .renderBackend[Backend]
    .componentDidMount(_.backend.componentDidMount)
    .build

  def apply() = component()
}

object MotorCard {
  case class Props(connected: Boolean, toggleRotate: () => Unit)

  sealed trait SpeechIntent
  case object Empty extends SpeechIntent
  case object ToggleRotate extends SpeechIntent

  class Backend($: BackendScope[Props, SpeechIntent]) {
    def render(props: Props, state: SpeechIntent) = {
      import props._

      div(className := "mdl-cell mdl-cell--4-col")(
        div(className := "mdl-card mdl-shadow--4dp", width := "100%")(
          div(className := "mdl-card__supporting-text", width := "100%")(
            h1(textAlign := "center")(if (connected) "Connected!" else "Waiting for Connection")
          ),
          div(className := "mdl-card__actions mdl-card--border text-center")(
            button(className := "mdl-button mdl-js-button mdl-button--fab mdl-js-ripple-effect mdl-button--colored", onClick --> Callback(toggleRotate()))(
              i(`class` := "material-icons")("loop")
            ).material
          ),
          SpeechComponent(s"Toggled rotation", state, state == ToggleRotate, () => $.modState(_ => Empty).runNow())
        )
      )
    }

    SpeechDetectorContainer.onText("toggle rotation", "toggle motor rotation") { _ =>
      $.props.map(_.toggleRotate()).runNow()
      $.modState(_ => ToggleRotate).runNow()
    }
  }

  val component = ReactComponentB[Props](getClass.getSimpleName)
    .initialState(Empty.asInstanceOf[SpeechIntent])
    .renderBackend[Backend]
    .build

  def apply(connected: Boolean, toggleRotate: () => Unit) =
    component(Props(connected, toggleRotate))
}
