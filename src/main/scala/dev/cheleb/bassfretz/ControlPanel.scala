package dev.cheleb.bassfretz

import com.raquo.laminar.api.L.*

object ControlPanel:

  def render(
      rootKeyVar: Var[String],
      scaleTypeVar: Var[String],
      selectedIntervalsVar: Var[Set[Int]],
      noteCircleState: NoteCircleState,
      practiceMode: PracticeMode
  ): Element =

  div(
    idAttr := "control-panel",

    div(cls := "panel-title", "BassFretZ"),

    div(cls := "panel-label", "Root note:"),

    select(
      cls := "panel-select",
      Scale.chromaticNotes.map { note =>
        option(
          value := note,
          selected <-- rootKeyVar.signal.map(_ == note),
          note
        )
      }.toSeq,
      onChange.mapToValue --> rootKeyVar.writer
    ),

    div(cls := "panel-label", "Scale:"),

    select(
      cls := "panel-select",
      Scale.scaleIntervals.keys.toSeq.sorted.map { scaleName =>
        option(
          value := scaleName,
          selected <-- scaleTypeVar.signal.map(_ == scaleName),
          scaleName.replace("_", " ").capitalize
        )
      }.toSeq,
      onChange.mapToValue --> scaleTypeVar.writer
    ),

    div(cls := "panel-label", "Intervals:"),

    div(
      cls := "intervals-grid",
      Scale.commonIntervals.map { case (semitone, shortLabel, longLabel) =>
        val colorHex = f"#${Scale.intervalColors(semitone)}%06x"
        label(
          cls := "interval-label",
          title := longLabel,
          input(
            typ := "checkbox",
            checked <-- selectedIntervalsVar.signal.map(_.contains(semitone)),
            onChange --> { _ =>
              selectedIntervalsVar.update { intervals =>
                if intervals.contains(semitone) then intervals - semitone
                else intervals + semitone
              }
            }
          ),
          span(
            cls := "color-swatch",
            styleAttr := s"background: $colorHex;"
          ),
          span(shortLabel)
        )
      }
    ),

    div(cls := "panel-label", "Selected:"),

    div(
      idAttr := "legend",
      children <-- rootKeyVar.signal.combineWith(selectedIntervalsVar.signal).map {
        case (rootKey, selectedIntervals) =>
          val rootIndex = Scale.noteIndexMap(rootKey)
          Scale.commonIntervals
            .filter { case (s, _, _) => selectedIntervals.contains(s) }
            .map { case (semitone, shortLabel, longLabel) =>
              val note = Scale.chromaticNotes((rootIndex + semitone) % 12)
              val colorHex = f"#${Scale.intervalColors(semitone)}%06x"
              div(
                cls := "legend-row",
                span(
                  cls := "color-swatch",
                  styleAttr := s"background: $colorHex;"
                ),
                span(cls := "legend-short", shortLabel),
                span(cls := "legend-long", longLabel),
                span(cls := "legend-note", note)
              )
            }
      }
    ),

    // ----------------------------------------------------------------------
    // Practice Mode section
    // ----------------------------------------------------------------------
    div(
      cls := "panel-section",
      div(cls := "panel-label", "Practice:"),
      button(
        cls := "panel-button",
        child.text <-- practiceMode.isActive.signal.map(active =>
          if active then "⏹ Stop Practice" else "▶ Practice"
        ),
        styleAttr <-- practiceMode.isActive.signal.map(active =>
          if active then
            "background: #cc3333; color: white; border: none; padding: 6px 14px; border-radius: 4px; cursor: pointer; font-size: 13px; width: 100%;"
          else
            "background: #33aa55; color: white; border: none; padding: 6px 14px; border-radius: 4px; cursor: pointer; font-size: 13px; width: 100%;"
        ),
        onClick --> { _ => practiceMode.toggle() }
      ),
      child <-- practiceMode.isActive.signal.map { active =>
        if active then
          div(
            cls := "panel-label",
            styleAttr := "margin-top: 6px; font-size: 11px; color: #aaa;",
            child.text <-- practiceMode.revealedCount.signal.map { count =>
              val scaleType = scaleTypeVar.now()
              val intervals = Scale.scaleIntervals.getOrElse(scaleType, Scale.scaleIntervals("major"))
              val total = intervals.size - 1
              s"Revealing note $count / $total"
            }
          )
        else
          div(
            cls := "panel-label",
            styleAttr := "margin-top: 6px; font-size: 11px; color: #888;",
            "Shows only roots, then reveals scale notes one by one"
          )
      }
    ),

    // ----------------------------------------------------------------------
    // Note Circle section
    // ----------------------------------------------------------------------
    div(
      cls := "panel-section",
      div(cls := "panel-title", "Note Circle"),
      label(
        cls := "panel-label",
        input(
          typ := "checkbox",
          checked <-- noteCircleState.useFrenchNotation.signal,
          onChange.mapToChecked --> noteCircleState.useFrenchNotation.writer
        ),
        " French Notation"
      ),
      div(
        cls := "panel-label",
        "Selected: ",
        child.text <-- noteCircleState.selectedNoteName
      ),
      div(
        cls := "panel-label",
        "Current Scale: ",
        child.text <-- noteCircleState.currentScaleLabels.map(_.mkString(" "))
      )
    )
  )
