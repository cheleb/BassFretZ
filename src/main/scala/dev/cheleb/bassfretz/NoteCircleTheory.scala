package dev.cheleb.bassfretz

/** Music theory helpers for the note circle.
 *
 * Thin facade over [[Scale]] that adds French notation and index/name conversion helpers. All chromatic-note and
 * major-scale data is delegated to [[Scale]] to avoid duplication.
 */
object NoteCircleTheory:

  /** 12 chromatic note names (sharps), ordered C..B. */
  val chromaticNotes: Array[String] = Scale.chromaticNotes

  /** 12 chromatic note names in French solfège, ordered C..B (Do..Si). */
  val frenchNotation: Array[String] =
    Array("Do", "Do#", "Ré", "Ré#", "Mi", "Fa", "Fa#", "Sol", "Sol#", "La", "La#", "Si")

  /** Major scale interval pattern (in semitones from root). */
  def majorScaleIntervals: List[Int] = Scale.scaleIntervals("major")

  /** Major scale note indices (0..11) for a given root index, modulo 12. */
  def getMajorScale(rootIndex: Int): List[Int] =
    require(rootIndex >= 0 && rootIndex < 12, s"rootIndex must be 0..11, got $rootIndex")
    majorScaleIntervals.map(i => (rootIndex + i) % 12)

  /** Scale note indices (0..11) for a given root index and scale type, modulo 12. */
  def getScaleFromType(rootIndex: Int, scaleType: String): List[Int] =
    require(rootIndex >= 0 && rootIndex < 12, s"rootIndex must be 0..11, got $rootIndex")
    Scale.scaleIntervals.getOrElse(scaleType, Scale.scaleIntervals("major")).map(i => (rootIndex + i) % 12)

  /** Convert a note name (sharps or flats) to its 0..11 chromatic index. */
  def noteToIndex(note: String): Int =
    Scale.noteIndexMap.getOrElse(note, throw new IllegalArgumentException(s"Unknown note: $note"))

  /** Convert a chromatic index to a label, in either sharp-letter or French notation. */
  def indexToNote(index: Int, useFrench: Boolean): String =
    require(index >= 0 && index < 12, s"index must be 0..11, got $index")
    if useFrench then frenchNotation(index) else chromaticNotes(index)
