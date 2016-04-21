package me.shadaj.ash.communication

import java.nio.ByteBuffer

case class PickledMessage(serviceFrom: String, serviceTo: String, data: ByteBuffer)