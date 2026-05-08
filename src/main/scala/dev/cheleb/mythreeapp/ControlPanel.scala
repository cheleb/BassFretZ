package dev.cheleb.mythreeapp

import com.raquo.laminar.api.L.*
import org.scalajs.dom

object ControlPanel:

  def render(
    rootKeyVar: Var[String],
    selectedIntervalsVar: Var[Set[Int]]
  ): Element =

    div(
      idAttr := 