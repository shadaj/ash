package me.shadaj.ash.spotify

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
  var inSpeech = false

  override def receive: Receive = {
    case Initialize =>
      val image = img(`class` := "img-responsive").render

      val songName = Var("")
      val artistName = Var("")
      var textBackground = Var("transparent")
      var textColor = Var("black")
      var buttonBackground = Var("transparent")
      var sideButtonColor = Var("black")
      var mainButtonColor = Var("black")

      val previous = MaterialButton("mdl-button--fab mdl-button--mini-fab mdl-js-ripple-effect transparent")(
        color := sideButtonColor
      )(
        i(`class` := "material-icons")("skip_previous")
      ).render
      val playPauseIcon = i(`class` := "material-icons")("play_arrow").render
      val playPause = MaterialButton("mdl-button--fab mdl-js-ripple-effect")(
        color := mainButtonColor
      )(playPauseIcon).render
      val next = MaterialButton("mdl-button--fab mdl-button--mini-fab mdl-js-ripple-effect transparent")(
        color := sideButtonColor
      )(
          i(`class` := "material-icons")("skip_next")
      ).render

      next.onclick = (e: MouseEvent) => {
        ServiceMessenger.current ! NextSong()
      }

      SpeechActionHandler.onPrefix("play next song") { _ =>
        ServiceMessenger.current ! NextSong()
        inSpeech = true
      }

      previous.onclick = (e: MouseEvent) => {
        ServiceMessenger.current ! PreviousSong()
      }

      SpeechActionHandler.onPrefix("play previous song") { _ =>
        ServiceMessenger.current ! PreviousSong()
        inSpeech = true
      }

      SpeechActionHandler.onPrefix("play the song", "play the current song") { _ =>
        ServiceMessenger.current ! Continue()
        inSpeech = true
      }

      SpeechActionHandler.onPrefix("pause the song", "pause the music", "pause the current song") { _ =>
        ServiceMessenger.current ! Pause()
        inSpeech = true
      }

      val card = div(`class` := "mdl-card mdl-shadow--4dp", width := "100%")(
        image,
        div(`class` := "mdl-card__supporting-text", width := "100%", backgroundColor := textBackground, color := textColor)(
          h3(marginBottom := "5px")(songName),
          h4(marginTop := "5px")(artistName)
        ),
        div(`class` := "mdl-card__actions mdl-card--border text-center", backgroundColor := buttonBackground)(
          previous,
          playPause,
          next
        )
      ).render
      dom.document.getElementById("card-container").appendChild(
        div(`class` := "mdl-cell mdl-cell--4-col")(
          card
        ).render)
      context.become(withCard(image, songName, artistName,
                              textBackground, textColor,
                              buttonBackground, sideButtonColor,
                              mainButtonColor,
                              playPauseIcon, playPause))
  }

  def withCard(image: html.Image, songName: Var[String], artistName: Var[String],
               textBackground: Var[String], textColor: Var[String],
               buttonBackground: Var[String], sideButtonColor: Var[String],
               mainButtonColor: Var[String],
               playPauseIcon: html.Element, playPause: html.Button): Receive = {
    var lastUri = ""

    {
      case NewStatus(playing, SongData(uri, img, song, artist)) =>
        if (lastUri != uri) {
          if (inSpeech) {
            SpeechUtils.say(s"Now playing $song by $artist")
            inSpeech = false
          }

          lastUri = uri
          image.src = s"/img?url=${js.Dynamic.global.encodeURIComponent(img)}"
          songName() = song
          artistName() = artist

          image.addEventListener("load", (e: Event) => {
            try {
              val vibrant = js.Dynamic.newInstance(
                js.Dynamic.global.Vibrant)(image)
              val swatches = vibrant.swatches()
              playPause.style.backgroundColor = swatches.Vibrant.getHex().toString
              textBackground() = swatches.DarkVibrant.getHex().toString
              textColor() = swatches.DarkVibrant.getTitleTextColor().toString
              buttonBackground() = swatches.DarkMuted.getHex().toString
              sideButtonColor() = swatches.DarkMuted.getTitleTextColor().toString
              mainButtonColor() = swatches.Vibrant.getTitleTextColor().toString
            } catch {
              case _: Throwable =>
            }
          })
        }

        val curSender = sender()
        if (playing) {
          playPauseIcon.innerHTML = "pause"
          playPause.onclick = (e: MouseEvent) => {
            curSender ! Pause()
          }
        } else {
          playPauseIcon.innerHTML = "play_arrow"
          playPause.onclick = (e: MouseEvent) => {
            curSender ! Continue()
          }
        }

      case _ =>
    }
  }
}
