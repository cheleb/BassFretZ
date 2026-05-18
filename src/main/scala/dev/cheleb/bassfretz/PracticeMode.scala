package dev.cheleb.bassfretz

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import scala.scalajs.js

/** Manages the "Practice" mode state.
  *
  * When active:
  *  1. Only root note positions are shown on the fretboard.
  *  2. Root positions are ordered by ascending fret number.
  *  3. Consecutive root pairs where the end root has a strictly higher true pitch
  *     than the start root form a "segment".
  *  4. For each segment, the ascending scale notes (by true pitch) between the two
  *     root pitches are revealed one at a time on a timer.
  *  5. Once all notes in the segment are shown, they are cleared (back to roots only)
  *     and the next segment begins.
  *
  * True pitch uses the open-string offsets from C in absolute semitones
  * (E=4, A=9, D=14, G=19) rather than the mod-12 values stored in Scale.
  */
class PracticeMode(
    rootKeyVar: Var[String],
    scaleTypeVar: Var[String]
):

  /** Whether practice mode is currently active. */
  val isActive: Var[Boolean] = Var(false)

  /** Index of the current segment being practiced. */
  val currentSegmentIdx: Var[Int] = Var(0)

  /** Number of notes revealed so far in the current segment (0 = only roots shown). */
  val revealedCount: Var[Int] = Var(0)

  /** Interval ID for the reveal timer. */
  private var timerId: Option[Int] = None

  /** Delay between revealing each note (ms). */
  val revealDelayMs: Int = 1200

  /** Pause between segments (ms). */
  val segmentPauseMs: Int = 1500

  // True ascending open-string pitches in semitones from C2:
  //   E2 = 4, A2 = 9, D3 = 14, G3 = 19
  private val trueOpenPitch = List(4, 9, 14, 19)

  /** Toggle practice mode on/off. */
  def toggle(): Unit =
    if isActive.now() then stop() else start()

  /** Start practice mode: reset state and begin timer. */
  def start(): Unit =
    isActive.set(true)
    currentSegmentIdx.set(0)
    revealedCount.set(0)
    startTimer()

  /** Stop practice mode. */
  def stop(): Unit =
    isActive.set(false)
    currentSegmentIdx.set(0)
    revealedCount.set(0)
    stopTimer()

  /** Restart the reveal sequence (e.g. when root/scale changes while practicing). */
  def restart(): Unit =
    if isActive.now() then
      currentSegmentIdx.set(0)
      revealedCount.set(0)
      stopTimer()
      startTimer()

  private def startTimer(): Unit =
    stopTimer()
    val id = dom.window.setInterval(() => advanceNote(), revealDelayMs.toDouble)
    timerId = Some(id)

  private def stopTimer(): Unit =
    timerId.foreach(dom.window.clearInterval(_))
    timerId = None

  /** Advance to reveal the next note in the current segment. */
  private def advanceNote(): Unit =
    val rootKey = rootKeyVar.now()
    val segments = computeSegments(rootKey)
    if segments.isEmpty then return

    val segIdx  = currentSegmentIdx.now() % segments.size
    val segment = segments(segIdx)
    val current = revealedCount.now()

    if current < segment.size then
      revealedCount.set(current + 1)
    else
      // Segment complete — pause, clear, move to next segment
      stopTimer()
      dom.window.setTimeout(
        () => {
          if isActive.now() then
            val nextSeg = (segIdx + 1) % segments.size
            currentSegmentIdx.set(nextSeg)
            revealedCount.set(0)
            startTimer()
        },
        segmentPauseMs.toDouble
      )

  // ===========================================================================
  // Path computation
  // ===========================================================================

  /** True pitch (semitones from C2) for a given string index and fret. */
  private def truePitch(stringIdx: Int, fret: Int): Int =
    trueOpenPitch(stringIdx) + fret

  /** Find all root note positions on the fretboard.
    * Sorted by fret ascending; ties broken by string index ascending.
    * Returns (stringIdx, fret) pairs.
    */
  private def findRootPositions(rootKey: String): List[(Int, Int)] =
    val rootIndex = Scale.noteIndexMap(rootKey)
    (for
      stringIdx <- 0 until 4
      fret      <- 0 to 20
      // use mod-12 to identify the note name
      noteIdx    = (Scale.openStringNotes(stringIdx) + fret) % 12
      if noteIdx == rootIndex
    yield (stringIdx, fret)).toList.sortBy { case (s, f) => (f, s) }

  /** DP state used during path search. */
  private case class St(cost: Int, maxF: Int, minF: Int, prev: Int):
    def span: Int = maxF - minF
    def betterThan(o: St): Boolean =
      cost < o.cost || (cost == o.cost && span < o.span)

  /** Run the DP shortest-path search over a sequence of pitch groups.
    *
    * @param groups    Ordered candidate sets (one per scale degree).  The order
    *                  determines the traversal direction (ascending or descending).
    * @param startFret Fret of the root we depart from.
    * @param endFret   Fret of the root we arrive at.
    * @return          Optimal (stringIdx, fret, semitone) sequence.
    */
  private def dpPath(
      groups:    Vector[Vector[(Int, Int, Int, Int)]],
      startFret: Int,
      endFret:   Int
  ): List[(Int, Int, Int)] =
    if groups.isEmpty then return Nil

    // Layer 0: cost from startFret to each candidate in groups(0)
    var layers = Vector(
      groups(0).map { case (_, f, _, _) =>
        St(Math.abs(f - startFret), Math.max(f, startFret), Math.min(f, startFret), -1)
      }
    )

    // Remaining layers
    for gi <- 1 until groups.size do
      val prev  = layers(gi - 1)
      val layer = groups(gi).map { case (_, f, _, _) =>
        var best = St(Int.MaxValue, 0, 0, -1)
        for k <- prev.indices do
          val p     = prev(k)
          val prevF = groups(gi - 1)(k)._2
          val c     = St(p.cost + Math.abs(f - prevF), Math.max(p.maxF, f), Math.min(p.minF, f), k)
          if c.betterThan(best) then best = c
        best
      }
      layers = layers :+ layer

    // Choose best final state, accounting for distance to endFret
    val last = layers.last
    var bestSt  = St(Int.MaxValue, 0, 0, -1)
    var bestIdx = -1
    for j <- last.indices do
      val p     = last(j)
      val lastF = groups.last(j)._2
      val total = St(p.cost + Math.abs(lastF - endFret),
                     Math.max(p.maxF, endFret), Math.min(p.minF, endFret), j)
      if total.betterThan(bestSt) then { bestSt = total; bestIdx = j }

    // Backtrack to recover positions
    val pathIndices = Array.fill(groups.size)(-1)
    pathIndices(groups.size - 1) = bestIdx
    for gi <- (groups.size - 2) to 0 by -1 do
      pathIndices(gi) = layers(gi + 1)(pathIndices(gi + 1)).prev

    pathIndices.toList.zipWithIndex.map { case (ci, gi) =>
      val (s, f, semitone, _) = groups(gi)(ci)
      (s, f, semitone)
    }

  /** Compute the scale-note path for one segment.
    *
    * @param ascending  If true, walk from lowerRoot up to higherRoot;
    *                   if false, walk from higherRoot down to lowerRoot.
    *
    * Requires truePitch(higherRoot) > truePitch(lowerRoot).
    * Returns None when the pitch condition is not met.
    * The returned list is always ordered in the direction of travel.
    */
  private def computePathBetweenRoots(
      rootKey:    String,
      lowerRoot:  (Int, Int),   // root with the lower true pitch
      higherRoot: (Int, Int),   // root with the higher true pitch
      ascending:  Boolean
  ): Option[List[(Int, Int, Int)]] =
    val loPitch = truePitch(lowerRoot._1,  lowerRoot._2)
    val hiPitch = truePitch(higherRoot._1, higherRoot._2)
    if hiPitch <= loPitch then return None

    val rootIndex = Scale.noteIndexMap(rootKey)
    val scaleType = scaleTypeVar.now()
    val intervals = Scale.scaleIntervals.getOrElse(scaleType, Scale.scaleIntervals("major"))

    // All non-root scale notes strictly between the two root pitches
    val candidates: List[(Int, Int, Int, Int)] =
      (for
        stringIdx <- 0 until 4
        fret      <- 0 to 24
        p          = truePitch(stringIdx, fret)
        if p > loPitch && p < hiPitch
        noteIdx    = (Scale.openStringNotes(stringIdx) + fret) % 12
        semitone   = (noteIdx - rootIndex + 12) % 12
        if intervals.contains(semitone) && semitone != 0
      yield (stringIdx, fret, semitone, p)).toList

    // Group by pitch; order ascending or descending depending on direction
    val groups: Vector[Vector[(Int, Int, Int, Int)]] =
      candidates.groupBy(_._4).toList
        .sortBy { case (p, _) => if ascending then p else -p }
        .map(_._2.toVector)
        .toVector

    val (startFret, endFret) =
      if ascending then (lowerRoot._2,  higherRoot._2)
      else              (higherRoot._2, lowerRoot._2)

    Some(dpPath(groups, startFret, endFret))

  /** Compute all segments for the current root and scale, alternating
    * ascending (even index) and descending (odd index) direction.
    *
    * Roots are sorted by fret; only consecutive pairs with strictly
    * different true pitches form valid segments.
    */
  def computeSegments(rootKey: String): List[List[(Int, Int, Int)]] =
    val roots = findRootPositions(rootKey)
    if roots.size < 2 then return Nil

    // Build adjacent pairs, keeping only those where pitches differ
    val pairs: List[((Int, Int), (Int, Int))] =
      roots.zip(roots.tail).filter { case (a, b) =>
        truePitch(a._1, a._2) != truePitch(b._1, b._2)
      }

    pairs.zipWithIndex.flatMap { case ((r1, r2), idx) =>
      val ascending = idx % 2 == 0
      // lowerRoot / higherRoot by true pitch
      val (lo, hi) = if truePitch(r1._1, r1._2) < truePitch(r2._1, r2._2) then (r1, r2) else (r2, r1)
      computePathBetweenRoots(rootKey, lo, hi, ascending)
    }

  /** Compute positions to display in practice mode.
    * Returns (stringIdx, fret, semitone, noteName) tuples.
    *
    * Always shows all root positions; additionally shows the revealed notes
    * of the current segment.
    */
  def computePracticePositions(rootKey: String): List[(Int, Int, Int, String)] =
    val rootIndex = Scale.noteIndexMap(rootKey)

    // All root positions (always visible)
    val rootPositions: List[(Int, Int, Int, String)] =
      findRootPositions(rootKey).map { case (s, f) =>
        (s, f, 0, Scale.chromaticNotes(rootIndex))
      }

    // Currently revealed path notes for the active segment
    val segments = computeSegments(rootKey)
    val pathPositions: List[(Int, Int, Int, String)] =
      if segments.isEmpty then Nil
      else
        val segIdx   = currentSegmentIdx.now() % segments.size
        val segment  = segments(segIdx)
        val revealed = revealedCount.now()
        segment.take(revealed).map { case (s, f, semitone) =>
          val noteName = Scale.chromaticNotes((rootIndex + semitone) % 12)
          (s, f, semitone, noteName)
        }

    rootPositions ++ pathPositions
