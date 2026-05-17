package dev.cheleb.bassfretz

import com.raquo.laminar.api.L.*

/** Reactive state for the note-circle feature.
  *
  * Bridges between the existing `rootKeyVar: Var[String]` (note **names**) and the index-based circle (0..11). The
  * notation-mode toggle is local to this feature.
  */
class NoteCircleState(rootKeyVar: Var[String]):

  /** Toggle between letter notation (`C, C#, D, ...`) and French solfège (`Do, Do#, Ré, ...`). */
  val useFrenchNotation: Var[Boolean] = Var(false)

  /** Root note as a chromatic index (0..11), derived from the shared `rootKeyVar`. */
  val selectedNoteIndex: Signal[Int] =
    rootKeyVar.signal.map(name => NoteCircleTheory.noteToIndex(name))

  /** Display label for the selected root, in the current notation. */
  val selectedNoteName: Signal[String] =
    selectedNoteIndex.combineWith(useFrenchNotation.signal).map { case (idx, fr) =>
      NoteCircleTheory.indexToNote(idx, fr)
    }

  /** The 7 chromatic indices that make up the major scale of the current root. */
  val currentScaleNotes: Signal[List[Int]] =
    selectedNoteIndex.map(NoteCircleTheory.getMajorScale)

  /** The 7 scale notes as labels, in the current notation. */
  val currentScaleLabels: Signal[List[String]] =
    currentScaleNotes.combineWith(useFrenchNotation.signal).map { case (notes, fr) =>
      notes.map(i => NoteCircleTheory.indexToNote(i, fr))
    }

  // ---------------------------------------------------------------------------
  // Mutators
  // ---------------------------------------------------------------------------

  /** Set the root by chromatic index (0..11). Throws on out-of-range input. */
  def selectNoteByIndex(index: Int): Unit =
    require(index >= 0 && index < 12, s"index must be 0..11, got $index")
    rootKeyVar.set(NoteCircleTheory.chromaticNotes(index))

  /** Flip the notation mode. */
  def toggleNotation(): Unit =
    useFrenchNotation.update(!_)

  /** Force the notation mode. */
  def setNotation(french: Boolean): Unit =
    useFrenchNotation.set(french)
