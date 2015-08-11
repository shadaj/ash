package me.shadaj.ash.mdl

import org.scalajs.dom

import scala.annotation.unchecked.uncheckedVariance
import scala.scalajs.js
import scalatags.JsDom._
import scalatags.{jsdom, generic}
import scalatags.generic.Namespace

case class MDLTypedTag[+Output <: dom.Element](tag: String, mdlClass: String,
                                               modifiers: List[Seq[Modifier]] = Nil)
  extends generic.TypedTag[dom.Element, Output, dom.Node]
  with jsdom.Frag {
  val namespace = Namespace.htmlNamespaceConfig
//  val void = false

  protected[this] type Self = MDLTypedTag[Output @uncheckedVariance]

  def render: Output = {
    val elem = dom.document.createElementNS(namespace.uri, tag)
    build(elem)
    val ret = elem.asInstanceOf[Output]
    js.Dynamic.global.componentHandler.upgradeElement(ret, mdlClass)
    ret
  }

  def apply(xs: Modifier*): MDLTypedTag[Output] = {
    this.copy(modifiers = xs :: modifiers)
  }

  override def toString = render.outerHTML
}