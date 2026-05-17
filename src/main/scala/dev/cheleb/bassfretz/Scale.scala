package dev.cheleb.bassfretz

import scala.scalajs.js

object Scale:
  // Chromatic scale starting from C
  val chromaticNotes = Array("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

  // Map note names to semitone offsets from C
  val noteIndexMap = Map(
    "C" -> 0, "C#" -> 1, "Db" -> 1, "D" -> 2, "D#" -> 3, "Eb" -> 3,
    "E" -> 4, "F" -> 5, "F#" -> 6, "Gb" -> 6, "G" -> 7, "G#" -> 8, "Ab" -> 8,
    "A" -> 9, "A#" -> 10, "Bb" -> 10, "B" -> 11
  )

  // Scale interval patterns (semitones from root note)
  val scaleIntervals: Map[String, List[Int]] = Map(
    "major" -> List(0, 2, 4, 5, 7, 9, 11),
    "natural_minor" -> List(0, 2, 3, 5, 7, 8, 10),
    "harmonic_minor" -> List(0, 2, 3, 5, 7, 8, 11),
    "melodic_minor" -> List(0, 2, 3, 5, 7, 9, 11),
    "dorian" -> List(0, 2, 3, 5, 7, 9, 10),
    "mixolydian" -> List(0, 2, 4, 5, 7, 9, 10),
    "ionian" -> List(0, 2, 4, 5, 7, 9, 11),
    "aeolian" -> List(0, 2, 3, 5, 7, 8, 10)
  )

  // Open string tuning (E, A, D, G) as semitone offsets from C
  val openStringNotes = List(4, 9, 2, 7)

  // String X positions for note placement
  val stringXPositions = List(-1.35 / 2, -0.45 / 2, 0.45 / 2, 1.35 / 2)

  // Color palette for each note name
  val noteColors: Map[String, Int] = Map(
    "C" -> 0xff3333, "C#" -> 0xff6666, "Db" -> 0xff6666,
    "D" -> 0xff8800, "D#" -> 0xffaa44, "Eb" -> 0xffaa44,
    "E" -> 0xffff00, "F" -> 0x33cc33, "F#" -> 0x55dd55, "Gb" -> 0x55dd55,
    "G" -> 0x00cccc, "G#" -> 0x55ddcc, "Ab" -> 0x55ddcc,
    "A" -> 0x3366ff, "A#" -> 0x6699ff, "Bb" -> 0x6699ff, "B" -> 0x9933ff
  )

  /** Calculate which note names are in the specified scale. */
  def getScaleNotes(rootKey: String, scaleType: String): Set[String] =
    val rootIndex = noteIndexMap(rootKey)
    scaleIntervals(scaleType).map(off => chromaticNotes((rootIndex + off) % 12)).toSet

  /** Compute Z position for a note at the given fret.
    * Fret 0 = behind nut, fret N = midpoint between fret(N-1) and fret(N)
    */
  def getFretZPosition(fret: Int, scaleLength: Double): Double =
    if fret == 0 then -scaleLength / 2 - 0.3
    else
      val prevFretZ = if fret == 1 then 0.0
        else scaleLength * (1.0 - 1.0 / Math.pow(2.0, (fret - 1).toDouble / 12.0))
      val currFretZ = scaleLength * (1.0 - 1.0 / Math.pow(2.0, fret.toDouble / 12.0))
      -scaleLength / 2 + (prevFretZ + currFretZ) / 2.0

  /** Compute all positions (string, fret, noteName) where scale notes occur on the neck. */
  def computeScalePositions(
    scaleNotes: Set[String],
    maxFret: Int = 20
  ): List[(Int, Int, String)] =
    (for
      stringIdx <- 0 until 4
      fret <- 0 to maxFret
      noteIdx = (openStringNotes(stringIdx) + fret) % 12
      noteName = chromaticNotes(noteIdx)
      if scaleNotes.contains(noteName)
    yield (stringIdx, fret, noteName)).toList.sortBy(t => (t._1, t._2))

  // Common chord/arpeggio intervals: (semitones, shortLabel, longLabel)
  val commonIntervals: List[(Int, String, String)] = List(
    (0,  "R",  "Root"),
    (1,  "b2", "Minor 2nd"),
    (2,  "2",  "Major 2nd"),
    (3,  "b3", "Minor 3rd"),
    (4,  "3",  "Major 3rd"),
    (5,  "4",  "Perfect 4th"),
    (6,  "b5", "Tritone"),
    (7,  "5",  "Perfect 5th"),
    (8,  "b6", "Minor 6th"),
    (9,  "6",  "Major 6th"),
    (10, "b7", "Minor 7th"),
    (11, "7",  "Major 7th"),
    (12, "8",  "Octave")
  )

  // Distinct color per interval semitone (stable, independent of root).
  val intervalColors: Map[Int, Int] = Map(
    0  -> 0xff2222, // Root - bright red
    1  -> 0xff66aa, // b2
    2  -> 0xff8844, // 2
    3  -> 0xffaa44, // b3
    4  -> 0xffee33, // 3 - yellow
    5  -> 0x66dd33, // 4
    6  -> 0x33cc88, // b5
    7  -> 0x33aaff, // 5 - blue
    8  -> 0x6677ee, // b6
    9  -> 0x9955ee, // 6
    10 -> 0xcc55dd, // b7
    11 -> 0xee44aa, // 7
    12 -> 0xff6666  // octave - lighter red
  )

  /** Compute positions for the given root note and selected interval semitones.
    * Returns (stringIdx, fret, semitoneOffset, noteName) tuples.
    * The octave (12) is preserved as its own tag so the UI can color it separately from the root.
    */
  def computeIntervalPositions(
    rootKey: String,
    selectedSemitones: Set[Int],
    maxFret: Int = 20
  ): List[(Int, Int, Int, String)] =
    val rootIndex = noteIndexMap(rootKey)
    // Map each selected semitone to its resulting chromatic note name.
    val targets: Map[String, List[Int]] =
      selectedSemitones.toList
        .map(s => (s, chromaticNotes((rootIndex + s) % 12)))
        .groupBy(_._2)
        .view
        .mapValues(_.map(_._1).sorted)
        .toMap

    (for
      stringIdx <- 0 until 4
      fret <- 0 to maxFret
      noteIdx = (openStringNotes(stringIdx) + fret) % 12
      noteName = chromaticNotes(noteIdx)
      if targets.contains(noteName)
      // Pick the smallest semitone that produced this note (so root wins over octave when both selected).
      semitone = targets(noteName).head
    yield (stringIdx, fret, semitone, noteName)).toList.sortBy(t => (t._1, t._2))