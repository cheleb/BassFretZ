package dev.cheleb.bassfretz

import THREE.*
import scala.scalajs.js

class Animation(
    noteMeshes: js.Array[Mesh],
    noteNames: js.Array[String],
    revealInterval: Double = 0.15,
    holdDuration: Double = 2.0,
    fadeOutDuration: Double = 0.5,
    popDuration: Double = 0.2
)(updateHUD: String => Unit):

  private val totalNotes = noteMeshes.length

  // Animation state
  private var animPhase: Int = 0
  private var currentNoteIndex: Int = 0
  private var phaseTimer: Double = 0.0

  // Track when each note started its pop-in animation (-1 = not triggered)
  private val noteTriggeredAt: js.Array[Double] = js.Array()
  for _ <- 0 until totalNotes do noteTriggeredAt.push(-1.0)

  /** Update animation state for one frame.
    * @param delta time since last frame in seconds
    * @param elapsed total elapsed time in seconds
    */
  def update(delta: Double, elapsed: Double): Unit =
    animPhase match
      case 0 => // Reveal phase - trigger notes at intervals
        phaseTimer += delta
        while phaseTimer >= revealInterval && currentNoteIndex < totalNotes do
          noteTriggeredAt(currentNoteIndex) = elapsed
          updateHUD(noteNames(currentNoteIndex))
          currentNoteIndex += 1
          phaseTimer -= revealInterval
        end while
        if currentNoteIndex >= totalNotes then
          animPhase = 1
          phaseTimer = 0.0

      case 1 => // Hold phase - gentle pulsing animation
        phaseTimer += delta
        val pulse = 1.0 + 0.05 * Math.sin(phaseTimer * Math.PI * 2)
        for i <- 0 until totalNotes do noteMeshes(i).scale.set(pulse, pulse, pulse)
        if phaseTimer >= holdDuration then
          animPhase = 2
          phaseTimer = 0.0

      case 2 => // Fade phase - shrink all notes to nothing
        phaseTimer += delta
        val progress = Math.min(1.0, phaseTimer / fadeOutDuration)
        val s = 1.0 - progress
        for i <- 0 until totalNotes do noteMeshes(i).scale.set(s, s, s)
        if progress >= 1.0 then
          resetCycle()

      case _ => ()

    // Pop-in effect for revealed notes
    if animPhase == 0 then
      for i <- 0 until currentNoteIndex do
        val triggeredAt = noteTriggeredAt(i)
        if triggeredAt >= 0 then
          val timeSinceReveal = elapsed - triggeredAt
          if timeSinceReveal < popDuration then
            val t = timeSinceReveal / popDuration
            val s = 1.0 + 0.2 * Math.sin(t * Math.PI)
            noteMeshes(i).scale.set(s, s, s)
          else noteMeshes(i).scale.set(1, 1, 1)

  /** Reset to initial state for the next cycle. */
  private def resetCycle(): Unit =
    for i <- 0 until totalNotes do
      noteMeshes(i).scale.set(0, 0, 0)
      noteTriggeredAt(i) = -1.0
    animPhase = 0
    currentNoteIndex = 0
    phaseTimer = 0.0
    updateHUD("...")

  /** Get the total number of notes being animated. */
  def noteCount: Int = totalNotes