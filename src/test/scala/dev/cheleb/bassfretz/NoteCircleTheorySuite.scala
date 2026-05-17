package dev.cheleb.bassfretz

class NoteCircleTheorySuite extends munit.FunSuite:

  test("all letter notes round-trip via noteToIndex / indexToNote(false)"):
    for i <- 0 until 12 do
      val name = NoteCircleTheory.chromaticNotes(i)
      assertEquals(NoteCircleTheory.noteToIndex(name), i)
      assertEquals(NoteCircleTheory.indexToNote(i, useFrench = false), name)

  test("all french notes round-trip via frenchNotation / indexToNote(true)"):
    for i <- 0 until 12 do
      val french = NoteCircleTheory.frenchNotation(i)
      assertEquals(NoteCircleTheory.indexToNote(i, useFrench = true), french)

  test("getMajorScale(0) is C major (0,2,4,5,7,9,11)"):
    assertEquals(NoteCircleTheory.getMajorScale(0), List(0, 2, 4, 5, 7, 9, 11))

  test("getMajorScale(9) is A major and wraps around the octave"):
    assertEquals(NoteCircleTheory.getMajorScale(9), List(9, 11, 1, 2, 4, 6, 8))

  test("getMajorScale(7) (G major) contains 6 (F#) and not 5 (F)"):
    val g = NoteCircleTheory.getMajorScale(7)
    assert(g.contains(6))
    assert(!g.contains(5))

  test("noteToIndex accepts flat enharmonics (Db -> 1)"):
    assertEquals(NoteCircleTheory.noteToIndex("Db"), 1)
    assertEquals(NoteCircleTheory.noteToIndex("Eb"), 3)
    assertEquals(NoteCircleTheory.noteToIndex("Bb"), 10)

  test("noteToIndex throws on unknown note"):
    intercept[IllegalArgumentException]:
      NoteCircleTheory.noteToIndex("invalid")

  test("getMajorScale rejects out-of-range root indices"):
    intercept[IllegalArgumentException](NoteCircleTheory.getMajorScale(-1))
    intercept[IllegalArgumentException](NoteCircleTheory.getMajorScale(12))

  test("indexToNote rejects out-of-range index"):
    intercept[IllegalArgumentException](NoteCircleTheory.indexToNote(-1, useFrench = false))
    intercept[IllegalArgumentException](NoteCircleTheory.indexToNote(12, useFrench = true))
