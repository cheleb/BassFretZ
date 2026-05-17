package dev.cheleb.bassfretz

import com.raquo.laminar.api.L.*

class NoteCircleStateSuite extends munit.FunSuite:

  /** Subscribe `s` and capture the latest emitted value via a mutable cell. */
  private def latest[A](s: Signal[A]): () => A =
    var cur: Option[A] = None
    s.foreach(v => cur = Some(v))(using unsafeWindowOwner)
    () => cur.getOrElse(throw new IllegalStateException("signal has not emitted yet"))

  test("initial state: rootKeyVar=C maps to selectedNoteIndex=0"):
    val rootVar = Var("C")
    val state = NoteCircleState(rootVar)
    val idx = latest(state.selectedNoteIndex)
    assertEquals(idx(), 0)

  test("selectNoteByIndex(7) sets rootKeyVar to G"):
    val rootVar = Var("C")
    val state = NoteCircleState(rootVar)
    val idx = latest(state.selectedNoteIndex)
    state.selectNoteByIndex(7)
    assertEquals(rootVar.now(), "G")
    assertEquals(idx(), 7)

  test("currentScaleNotes follows the selected root (A major)"):
    val rootVar = Var("C")
    val state = NoteCircleState(rootVar)
    val notes = latest(state.currentScaleNotes)
    state.selectNoteByIndex(9)
    assertEquals(notes(), NoteCircleTheory.getMajorScale(9))

  test("toggleNotation flips useFrenchNotation"):
    val state = NoteCircleState(Var("C"))
    val french = latest(state.useFrenchNotation.signal)
    assertEquals(french(), false)
    state.toggleNotation()
    assertEquals(french(), true)
    state.toggleNotation()
    assertEquals(french(), false)

  test("setNotation(true) + selectNoteByIndex(0) yields selectedNoteName=Do"):
    val rootVar = Var("A")
    val state = NoteCircleState(rootVar)
    val name = latest(state.selectedNoteName)
    state.setNotation(true)
    state.selectNoteByIndex(0)
    assertEquals(name(), "Do")

  test("currentScaleLabels reflects the active notation"):
    val rootVar = Var("C")
    val state = NoteCircleState(rootVar)
    val labels = latest(state.currentScaleLabels)
    // C major in letters
    assertEquals(labels(), List("C", "D", "E", "F", "G", "A", "B"))
    state.setNotation(true)
    // C major in French
    assertEquals(labels(), List("Do", "Ré", "Mi", "Fa", "Sol", "La", "Si"))

  test("selectNoteByIndex rejects out-of-range index"):
    val state = NoteCircleState(Var("C"))
    intercept[IllegalArgumentException](state.selectNoteByIndex(-1))
    intercept[IllegalArgumentException](state.selectNoteByIndex(12))
