package me.shadaj.ash

import scalatags.JsDom.all._
import org.scalajs.dom.html

package object mdl {
  val MaterialButton = (classes: String) => MDLTypedTag[html.Button]("button", "MaterialButton")(
    `class` := s"mdl-button mdl-js-button $classes"
  )
}
