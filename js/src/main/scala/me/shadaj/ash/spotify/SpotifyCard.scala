package me.shadaj.ash.spotify

import japgolly.scalajs.react.vdom.all._
import japgolly.scalajs.react._
import org.scalajs.dom.html.{Element, Image}
import org.scalajs.dom.NodeList

import scala.scalajs.js
import com.payalabs.scalajs.react.mdl._
import me.shadaj.ash.communication.ServiceMessenger
import me.shadaj.ash.speech.{SpeechActionHandler, SpeechComponent}

object SpotifyCardContainer {
  case class State(image: String, songName: String, artistName: String, playing: Boolean)
  case class Props()


  class Backend($: BackendScope[Props, State]) {
    def componentDidMount: Callback = Callback {
      ServiceActor.subscribe {
        case NewStatus(playing, songData) =>
          $.modState(s => s.copy(image = songData.imageUrl, songName = songData.songName, artistName = songData.artistName, playing = playing)).runNow()
      }
    }

    def render(state: State) = {
      implicit val sender = ServiceActor.self
      div(SpotifyCard(
        state.image, state.songName, state.artistName, state.playing,
        () => ServiceMessenger.current ! PreviousSong(),
        () => if (state.playing) {
          ServiceMessenger.current ! Pause()
        } else ServiceMessenger.current ! Continue(),
        () => ServiceMessenger.current ! NextSong()
      ))
    }
  }

  val component = ReactComponentB[Props](getClass.getSimpleName)
    .initialState(State("", "", "", false))
    .renderBackend[Backend]
    .componentDidMount(_.backend.componentDidMount)
    .build

  def apply() = component(Props())
}

object SpotifyCard {
  case class Props(image: String, songName: String, artistName: String, playing: Boolean,
                   onPrevious: () => Unit, onPlayPause: () => Unit, onNext: () => Unit)

  sealed trait SpeechIntent
  case object Empty extends SpeechIntent
  case object PlayPrevious extends SpeechIntent
  case object Play extends SpeechIntent
  case object Pause extends SpeechIntent
  case object PlayNext extends SpeechIntent
  case object WhatSong extends SpeechIntent

  class Backend($: BackendScope[Props, SpeechIntent]) {
    implicit class QuerySeq(val nodeList: NodeList) extends Seq[Element] {
      override def length: Int = nodeList.length
      override def apply(idx: Int): Element = nodeList(idx).asInstanceOf[Element]
      override def iterator: Iterator[Element] = (0 until length).map(apply).toIterator
    }

    def componentDidUpdate: Callback = Callback {
      val imageUrl = ReactDOM.findDOMNode($).querySelector("img").asInstanceOf[Image].attributes.getNamedItem("src").value

      if (imageUrl.nonEmpty) {
        val vibrant = js.Dynamic.newInstance(js.Dynamic.global.Vibrant)(imageUrl)
        vibrant.getPalette { (err: js.UndefOr[js.Object], pallete: js.UndefOr[js.Dynamic]) =>
          pallete.foreach { vibrantSwatch =>
            val rootElement = ReactDOM.findDOMNode($)

            val supportingText = rootElement.querySelector(".mdl-card__supporting-text").asInstanceOf[Element]
            val bodyBackground = vibrantSwatch.DarkVibrant.getHex().toString
            val bodyText = vibrantSwatch.DarkVibrant.getTitleTextColor().toString
            supportingText.style.backgroundColor = bodyBackground
            supportingText.style.color = bodyText

            val actions = rootElement.querySelector(".mdl-card__actions").asInstanceOf[Element]
            val buttonBackground = vibrantSwatch.DarkMuted.getHex().toString
            actions.style.backgroundColor = buttonBackground

            val miniButtons = rootElement.querySelectorAll(".mdl-button--mini-fab")
            val buttonText = vibrantSwatch.DarkMuted.getTitleTextColor().toString
            miniButtons.foreach { e =>
              e.style.color = buttonText
            }

            val playPauseBackground = vibrantSwatch.Vibrant.getHex().toString
            val playPauseText = vibrantSwatch.Vibrant.getTitleTextColor().toString
            val playPause = rootElement.querySelector("#play-pause").asInstanceOf[Element]
            playPause.style.backgroundColor = playPauseBackground
            playPause.style.color = playPauseText
          }
        }
      }
    }

    def render(props: Props, state: SpeechIntent) = {
      import props._

      div(className := "mdl-cell mdl-cell--4-col")(
        div(className := "mdl-card mdl-shadow--4dp", width := "100%")(
          img(className := "vibrant img-responsive", src := s"/img?url=${js.Dynamic.global.encodeURIComponent(image)}"),
          div(className := "mdl-card__supporting-text", width := "100%")(
            h3(marginBottom := "5px")(songName),
            h4(marginTop := "5px")(artistName)
          ),
          div(className := "mdl-card__actions mdl-card--border text-center")(
            button(className := "mdl-button mdl-js-button mdl-button--fab mdl-button--mini-fab mdl-js-ripple-effect transparent", onClick --> Callback(onPrevious()))(
              i(className := "material-icons")("skip_previous")
            ).material,
            button(id := "play-pause", className := "mdl-button mdl-js-button mdl-button--fab mdl-js-ripple-effect", onClick --> Callback(onPlayPause()))(
              if (playing) i(className := "material-icons")("pause") else i(className := "material-icons")("play_arrow")
            ).material,
            button(className := "mdl-button mdl-js-button mdl-button--fab mdl-button--mini-fab mdl-js-ripple-effect transparent", onClick --> Callback(onNext()))(
              i(className := "material-icons")("skip_next")
            ).material
          ),
          SpeechComponent(s"I skipped to $songName by $artistName", (songName, artistName), state == PlayNext, () => $.modState(_ => Empty).runNow()),
          SpeechComponent(s"I went back to $songName by $artistName", (songName, artistName), state == PlayPrevious, () => $.modState(_ => Empty).runNow()),
          SpeechComponent(s"I paused the music", playing, state == Pause, () => $.modState(_ => Empty).runNow()),
          SpeechComponent(s"I unpaused the music", playing, state == Play, () => $.modState(_ => Empty).runNow()),
          SpeechComponent(s"This is $songName by $artistName", state, state == WhatSong, () => $.modState(_ => Empty).runNow())
        )
      )
    }

    SpeechActionHandler.onPrefix("play next song", "play the next song") { _ =>
      $.props.map(_.onNext()).runNow()
      $.modState(_ => PlayNext).runNow()
    }

    SpeechActionHandler.onPrefix("play previous song", "play the previous song", "play the last song") { _ =>
      $.props.map(_.onPrevious()).runNow()
      $.props.map(_.onPrevious()).runNow()
      $.modState(_ => PlayPrevious).runNow()
    }

    SpeechActionHandler.onPrefix("what song is this", "what's this song", "what's playing") { _ =>
      $.modState(_ => WhatSong).runNow()
    }

    SpeechActionHandler.onPrefix("continue the song", "unpause the song") { _ =>
      if (!$.props.runNow().playing) {
        $.props.map(_.onPlayPause()).runNow()
        $.modState(_ => Play).runNow()
      }
    }

    SpeechActionHandler.onPrefix("pause the song", "stop the song") { _ =>
      if ($.props.runNow().playing) {
        $.props.map(_.onPlayPause()).runNow()
        $.modState(_ => Pause).runNow()
      }
    }
  }

  val component = ReactComponentB[Props](getClass.getSimpleName)
    .initialState(Empty.asInstanceOf[SpeechIntent])
    .renderBackend[Backend]
    .componentDidUpdate(_.$.backend.componentDidUpdate)
    .build

  def apply(image: String, songName: String, artistName: String, playing: Boolean,
            onPrevious: () => Unit, onPlayPause: () => Unit, onNext: () => Unit) =
    component(Props(image, songName, artistName, playing, onPrevious, onPlayPause, onNext))
}
