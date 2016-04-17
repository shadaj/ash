package me.shadaj.ash.spotify

import boopickle.CompositePickler
import boopickle.Default._
import me.shadaj.ash.communication.Serializers

sealed trait SpotifyMessage
case class SongData(uri: String, imageUrl: String, songName: String, artistName: String) extends SpotifyMessage
case class NewStatus(playing: Boolean, songData: SongData) extends SpotifyMessage
case class NextSong() extends SpotifyMessage
case class PreviousSong() extends SpotifyMessage
case class Play(uri: String) extends SpotifyMessage
case class Pause() extends SpotifyMessage
case class Continue() extends SpotifyMessage

object Serializers extends Serializers {
  val picklerPair = CompositePickler[AnyRef].
    addConcreteType[NewStatus].
    addConcreteType[NextSong].
    addConcreteType[PreviousSong].
    addConcreteType[Play].
    addConcreteType[Pause].
    addConcreteType[Continue]
  override val pickler: Pickler[AnyRef] = picklerPair
}
