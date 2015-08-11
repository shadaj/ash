package me.shadaj.ash.communication

import java.nio.ByteBuffer

case class PickledMessage(service: String, data: ByteBuffer)