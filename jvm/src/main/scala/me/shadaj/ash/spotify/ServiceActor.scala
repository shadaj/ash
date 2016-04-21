package me.shadaj.ash.spotify

import akka.actor.Actor
import akka.pattern.pipe
import dispatch._
import me.shadaj.ash.communication.{ServiceMessenger, Initialize}
import play.api.libs.json.{JsObject, Json}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.sys.process._

import me.shadaj.spotify._

case object AppOpened
case class NewSong(url: String, name: String, artist: String)

class ServiceActor extends Actor {
  SpotifyLocal.oauthToken.zip(SpotifyLocal.openApp).pipeTo(self)

  override def receive: Receive = {
    case Initialize =>
    case (t: TokenResponse, ConnectResponse(_, _, _, _)) => context.become(appOpened(t))
    case e => println(e)
  }

  def appOpened(oauth: TokenResponse): Receive = {
    SpotifyLocal.csrfToken.pipeTo(self)

    {
      case csrf@CSRFResponse(_) =>
        context.become(ready(oauth, csrf))
      case e => println(e)
    }
  }

  def ready(oauth: TokenResponse, csrf: CSRFResponse): Receive = {
    SpotifyLocal.statusChange(oauth, csrf, 5).pipeTo(self)

    {
      case Right(_) =>
        SpotifyLocal.statusChange(oauth, csrf, 5).pipeTo(self)
      case Left(s: Status) =>
        SpotifyLocal.statusChange(oauth, csrf, 5).pipeTo(self)
        val message = if (s.track.album_resource.isDefined) {
          val albumURI = s.track.album_resource.get.uri.get
          Http(url(s"https://api.spotify.com/v1/albums/${albumURI.split(':')(2)}")).map { r =>
            val responseJSON = Json.parse(r.getResponseBody)
            val imageUrl = ((responseJSON \ "images").as[Array[JsObject]].head \ "url").as[String]
            val artist = ((responseJSON \ "artists").as[Array[JsObject]].head \ "name").as[String]
            NewStatus(
              s.playing,
              SongData(
                s.track.track_resource.get.uri.getOrElse(""),
                imageUrl,
                s.track.track_resource.get.name.getOrElse(""),
                artist
              )
            )
          }
        } else {
          Future.successful(NewStatus(
            s.playing,
            SongData(
              "ad",
              "",
              "Advertisement",
              "Spotify"
            )
          ))
        }
        message.foreach(ServiceMessenger.broadcast("me.shadaj.ash.spotify", _))

      case NextSong() =>
        Seq("osascript", "-e", """tell application "Spotify" to next track""").!!
      case PreviousSong() =>
        Seq("osascript", "-e", """tell application "Spotify" to previous track""").!!
      case Play(uri) =>
        SpotifyLocal.play(oauth, csrf, uri)
      case Pause() =>
        SpotifyLocal.pause(oauth, csrf)
      case Continue() =>
        SpotifyLocal.unpause(oauth, csrf)
      case e => println(e)
    }
  }
}
