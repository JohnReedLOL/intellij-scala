package org.jetbrains.plugins.scala.codeInsight

import com.intellij.openapi.actionSystem.{KeyboardShortcut, Shortcut}
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.editor.{EditorCustomElementRenderer, InlayModel}
import com.intellij.openapi.keymap.KeymapManager
import com.intellij.openapi.util.{Key, TextRange}
import javax.swing.KeyStroke
import org.jetbrains.plugins.scala.codeInsight.implicits.presentation.Presentation

import scala.collection.JavaConverters._

package object implicits {
  private val ScalaImplicitHintKey = Key.create[Boolean]("SCALA_IMPLICIT_HINT")

  private[implicits] type Inlay = com.intellij.openapi.editor.Inlay[_ <: EditorCustomElementRenderer]

  implicit class Model(val model: InlayModel) extends AnyVal {
    def inlaysIn(range: TextRange): Seq[Inlay] =
      model.getInlineElementsInRange(range.getStartOffset, range.getEndOffset)
        .asScala
        .filter(ScalaImplicitHintKey.isIn)

    def add(hint: Hint): Option[Inlay] = {
      val inlay = hint.addTo(model)
      inlay.foreach(_.putUserData(ScalaImplicitHintKey, true))
      inlay
    }
  }

  // TODO
  implicit class InlayExt(val inlay: Inlay) extends AnyVal {
    def presentation: Presentation = inlay.getRenderer.asInstanceOf[PresentationRenderer].presentation
  }

  val EnableShortcuts = Seq(
    new KeyboardShortcut(KeyStroke.getKeyStroke("control alt shift EQUALS"), null),
    new KeyboardShortcut(KeyStroke.getKeyStroke("control alt shift ADD"), null))

  val DisableShortcuts = Seq(
    new KeyboardShortcut(KeyStroke.getKeyStroke("control alt shift MINUS"), null),
    new KeyboardShortcut(KeyStroke.getKeyStroke("control alt shift SUBTRACT"), null))

  def setShortcuts(id: String, shortcuts: Seq[Shortcut]): Unit = {
    val keymap = KeymapManager.getInstance().getActiveKeymap
    keymap.removeAllActionShortcuts(id)
    shortcuts.foreach(keymap.addShortcut(id, _))
  }

  def removeAllShortcuts(id: String): Unit = {
    val keymap = KeymapManager.getInstance().getActiveKeymap
    keymap.removeAllActionShortcuts(id)
  }

  def pairFor[T](element: T, elements: Seq[T], isOpening: T => Boolean, isClosing: T => Boolean): Option[T] = {
    def pairIn(elements: Seq[T]) = {
      var balance = 0
      val remainder = elements.dropWhile(_ != element).dropWhile { it =>
        if (isOpening(it)) balance += 1
        if (isClosing(it)) balance -= 1
        balance != 0
      }
      remainder.headOption
    }

    if (isOpening(element)) pairIn(elements)
    else if (isClosing(element)) pairIn(elements.reverse)
    else None
  }

  implicit class TextAttributesExt(val v: TextAttributes) extends AnyVal {
    def + (attributes: TextAttributes): TextAttributes = {
      val result = v.clone()
      Option(attributes.getForegroundColor).foreach(result.setForegroundColor)
      Option(attributes.getBackgroundColor).foreach(result.setBackgroundColor)
      Option(attributes.getFontType).foreach(result.setFontType)
      Option(attributes.getEffectType).foreach(result.setEffectType)
      Option(attributes.getEffectColor).foreach(result.setEffectColor)
      result
    }

    def ++ (attributes: Iterable[TextAttributes]): TextAttributes =
      attributes.foldLeft(v)(_ + _)
  }
}
