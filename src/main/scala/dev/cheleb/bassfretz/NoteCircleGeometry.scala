package dev.cheleb.bassfretz

import THREE.*
import scala.scalajs.js

/** Pure geometric helpers for the 12-note circle (XY plane, normal along +Z).
  *
  * Index 0 sits at the top (12 o'clock) and indices increase clockwise when viewed from +Z.
  */
object NoteCircleGeometry:

  /** Default radius — tuned for the "above neck" placement (centered at y=6). */
  val defaultRadius: Double = 3.0

  private val noteCount: Int = 12

  /** Position for the i-th note (0..11) on a circle of given radius, on the XY plane.
    *
    * Index 0 sits at the top of the circle (y = +radius). Indices increase clockwise as viewed from +Z (the camera
    * side), so index 3 is at 3 o'clock, index 6 at the bottom, index 9 at 9 o'clock.
    */
  def getNotePosition(index: Int, radius: Double = defaultRadius): Vector3 =
    // Start at PI/2 (12 o'clock: cos=0, sin=+1) and decrement so increasing index rotates clockwise.
    val angle = Math.PI / 2 - index * (Math.PI * 2 / noteCount)
    Vector3(Math.cos(angle) * radius, Math.sin(angle) * radius, 0.0)

  /** All 12 positions in chromatic order (C, C#, ..., B). */
  def createNotePositions(radius: Double = defaultRadius): Array[Vector3] =
    Array.tabulate(noteCount)(i => getNotePosition(i, radius))

  /** A group containing a thin Line guide circle on the XY plane. */
  def createCircleGuide(radius: Double = defaultRadius, color: Int = 0x333333, segments: Int = 64): Group =
    val group = Group()
    val pts = js.Array[Vector3]()
    var i = 0
    while i <= segments do
      val a = i * (Math.PI * 2 / segments)
      pts.push(Vector3(Math.cos(a) * radius, Math.sin(a) * radius, 0.0))
      i += 1
    val geom = BufferGeometry().setFromPoints(pts)
    val mat = new LineBasicMaterial(js.Dynamic.literal(color = color).asInstanceOf[js.Object])
    group.add(Line(geom, mat))
    group
