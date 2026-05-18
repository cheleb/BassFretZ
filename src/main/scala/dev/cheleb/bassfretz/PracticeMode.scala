package dev.cheleb.bassfretz

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import scala.scalajs.js

/** Manages the "Practice" mode state.
  *
  * When active, only root notes are displayed on the fretboard. Notes of the scale path between two
  * consecutive root notes are revealed one at a time on a timer.
  */
class PracticeMode(
    rootKeyVar: Var[String],
    scaleTypeVar: Var[String]
):

  /** Whether practice mode is currently active. */
  val isActive: Var[Boolean] = Var(false)

  /** Index of the next note to reveal in the current scale path (0 = first non-root note). */
  val revealedCount: Var[Int] = Var(0)

  /** Interval ID for the reveal timer. */
  private var timerId: Option[Int] = None

  /** Delay between revealing each note (ms). */
  val revealDelayMs: Int = 1200

  /** Toggle practice mode on/off. */
  def toggle(): Unit =
    if isActive.now() then stop()
    else start()

  /** Start practice mode: reset revealed count and begin timer. */
  def start(): Unit =
    isActive.set(true)
    revealedCount.set(0)
    startTimer()

  /** Stop practice mode. */
  def stop(): Unit =
    isActive.set(false)
    revealedCount.set(0)
    stopTimer()

  /** Restart the reveal sequence (e.g. when root/scale changes while practicing). */
  def restart(): Unit =
    if isActive.now() then
      revealedCount.set(0)
      stopTimer()
      startTimer()

  private def startTimer(): Unit =
    stopTimer()
    val id = dom.window.setInterval(
      () => advanceNote(),
      revealDelayMs.toDouble
    )
    timerId = Some(id)

  private def stopTimer(): Unit =
    timerId.foreach(dom.window.clearInterval(_))
    timerId = None

  /** Advance to reveal the next note. Stops when all scale notes are revealed. */
  private def advanceNote(): Unit =
    val scaleSize = currentScaleSize
    // Scale has N notes; roots (index 0) are always shown, so we reveal indices 1..(N-1)
    val maxReveal = scaleSize - 1
    val current = revealedCount.now()
    if current < maxReveal then revealedCount.set(current + 1)
    else
      // All notes revealed - pause briefly, then restart the cycle
      stopTimer()
      dom.window.setTimeout(
        () => {
          if isActive.now() then
            revealedCount.set(0)
            startTimer()
        },
        2000.0 // 2 second pause before restarting
      )

  /** Number of notes in the current scale (typically 7). */
  private def currentScaleSize: Int =
    Scale.scaleIntervals.getOrElse(scaleTypeVar.now(), Scale.scaleIntervals("major")).size

  /** Compute positions to display in practice mode.
    * Returns (stringIdx, fret, semitone, noteName) tuples - same shape as computeIntervalPositions.
    *
    * Shows:
    *  - All root note positions (semitone == 0) always
    *  - Scale notes revealed up to `revealedCount` (in scale order from root)
    */
  def computePracticePositions(rootKey: String): List[(Int, Int, Int, String)] =
    val scaleType = scaleTypeVar.now()
    val intervals = Scale.scaleIntervals.getOrElse(scaleType, Scale.scaleIntervals("major"))
    val revealed = revealedCount.now()

    // Always show roots + revealed intervals
    val visibleIntervals: Set[Int] =
      Set(0) ++ intervals.slice(1, 1 + revealed).toSet

    Scale.computeIntervalPositions(rootKey, visibleIntervals)
