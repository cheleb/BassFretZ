package dev.cheleb.mythreeapp

import com.raquo.laminar.api.L.*

object ControlPanel:

  def render(
    rootKeyVar: Var[String],
    selectedIntervalsVar: Var[Set[Int]]
  ): Element =

    div(
      idAttr := "control-panel",
      styleAttr := "position: fixed; top: 20px; left: 20px; color: #ffffff; font-family: monospace; font-size: 14px; background: rgba(0,0,0,0.75); padding: 14px 18px; border-radius: 6px; border: 1px solid #444; z-index: 100; min-width: 220px; max-width: 280px;",

      div(
        styleAttr := "font-size: 16px; font-weight: bold; margin-bottom: 10px;",
        "Bass Neck Explorer"
      ),

      div(styleAttr := "margin-bottom: 4px; color: #ccc;", "Root note:"),

      select(
        styleAttr := "width: 100%; padding: 4px 6px; background: #222; color: #fff; border: 1px solid #555; border-radius: 4px; font-family: monospace; font-size: 14px; margin-bottom: 12px;",
        Scale.chromaticNotes.map { note =>
          option(
            value := note,
            selected <-- rootKeyVar.signal.map(_ == note),
            note
          )
        }.toSeq,
        onChange.mapToValue --> rootKeyVar.writer
      ),

      div(styleAttr := "margin-bottom: 6px; color: #ccc;", "Intervals:"),

      div(
        styleAttr := "display: grid; grid-template-columns: 1fr 1fr; gap: 4px 10px; margin-bottom: 12px;",
        Scale.commonIntervals.map { case (semitone, shortLabel, longLabel) =>
          val colorHex = f"#${Scale.intervalColors(semitone)}%06x"
          label(
            styleAttr := "display: flex; align-items: center; gap: 6px; cursor: pointer; user-select: none;",
            title := longLabel,
            input(
              typ := "checkbox",
              checked <-- selectedIntervalsVar.signal.map(_.contains(semitone)),
              onClick --> { _ =>
                selectedIntervalsVar.update { intervals =>
                  if intervals.contains(semitone) then intervals - semitone
                  else intervals + semitone
                }
              }
            ),
            span(
              styleAttr := s"display: inline-block; width: 10px; height: 10px; background: $colorHex; border: 1px solid #666; border-radius: 2px;"
            ),
            span(shortLabel)
          )
        }
      ),

      div(styleAttr := "margin-bottom: 4px; color: #ccc;", "Selected:"),

      div(
        idAttr := "legend",
        styleAttr := "display: flex; flex-direction: column; gap: 3px; font-size: 13px;",
        children <-- rootKeyVar.signal.combineWith(selectedIntervalsVar.signal).map {
          case (rootKey, selectedIntervals) =>
            val rootIndex = Scale.noteIndexMap(rootKey)
            Scale.commonIntervals
              .filter { case (s, _, _) => selectedIntervals.contains(s) }
              .map { case (semitone, shortLabel, longLabel) =>
                val note     = Scale.chromaticNotes((rootIndex + semitone) % 12)
                val colorHex = f"#${Scale.intervalColors(semitone)}%06x"
                div(
                  styleAttr := "display: flex; align-items: center; gap: 8px;",
                  span(styleAttr := s"display:inline-block;width:10px;height:10px;background:$colorHex;border:1px solid #666;border-radius:2px;"),
                  span(styleAttr := "min-width: 24px; color:#fff;", shortLabel),
                  span(styleAttr := "color:#aaa;", longLabel),
                  span(styleAttr := "margin-left:auto; color:#ffd;", note)
                )
              }
        }
      )
    )
