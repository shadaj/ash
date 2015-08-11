package me.shadaj.ash.spotify

import boopickle.{CompositePickler, Pickler, Unpickler}
import me.shadaj.ash.communication.Serializers

case class SongData(uri: String, imageUrl: String, songName: String, artistName: String)
case class NewStatus(playing: Boolean, songData: SongData)
case class NextSong()
case class PreviousSong()
case class Play(uri: String)
case class Pause()
case class Continue()

object Serializers extends Serializers {
  val picklerPair = CompositePickler[AnyRef].
    addConcreteType[NewStatus].
    addConcreteType[NextSong].
    addConcreteType[PreviousSong].
    addConcreteType[Play].
    addConcreteType[Pause].
    addConcreteType[Continue]
  override val pickler: Pickler[AnyRef] = picklerPair.pickler
  override val unpickler: Unpickler[AnyRef] = picklerPair.unpickler
}
