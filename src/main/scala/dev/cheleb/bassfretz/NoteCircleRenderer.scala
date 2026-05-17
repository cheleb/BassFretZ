package dev.cheleb.bassfretz

import THREE.*
import com.raquo.laminar.api.L.*
import scala.scalajs.js

/** Builds the master `Group` for the 12-note circle and wires it to reactive state.
  *
  * Owns: a parent `Group` (translated by `centerOffset`), a guide circle, 12 [[NoteVisual]]s, and 12 `CSS2DObject`
  * label objects. Subscribes to [[NoteCircleState]] and updates each note's color/emissive/label when the root, scale,
  * or notation changes.
  *
  * @param scene
  *   the master Three.js scene (the renderer's own group is added to this scene).
  * @param state
  *   reactive state shared with the rest of the app.
  * @param radius
  *   circle radius in world units.
  * @param centerOffset
  *   where to place the circle's centre (defaults to `(0, 6, 0)`, above the bass neck).
  */
class NoteCircleRenderer(
    scene: Scene,
    state: NoteCircleState,
    radius: Double = NoteCircleGeometry.defaultRadius,
    centerOffset: Vector3 = Vector3(0, 6, 0)
):

  /** Master group; parent of guide circle, sphere meshes, and label objects. */
  val group: Group = Group()
  group.position.copy(centerOffset)

  // Guide circle (thin line on the XY plane).
  private val guide: Group = NoteCircleGeometry.createCircleGuide(radius)
  group.add(guide)

  // 12 note visuals, in chromatic order (C..B).
  private val notes: Array[NoteVisual] =
    NoteCircleGeometry
      .createNotePositions(radius)
      .zipWithIndex
      .map { case (pos, idx) =>
        val nv = NoteVisual(idx, pos)
        group.add(nv.mesh)

        // Wrap the label div as a CSS2DObject and place it slightly outside the sphere
        // along the radial direction (radius * 1.25).
        val labelObj = new CSS2DObjectFacade(nv.labelDiv)
        val labelX = pos.x.getOrElse(0.0) * 1.25
        val labelY = pos.y.getOrElse(0.0) * 1.25
        val labelZ = pos.z.getOrElse(0.0) * 1.25
        labelObj.position.set(labelX, labelY, labelZ)
        group.add(labelObj)

        nv
      }

  /** Mesh array for raycasting (in chromatic order). */
  val noteMeshes: js.Array[Mesh] = js.Array(notes.map(_.mesh)*)

  scene.add(group)

  // ---------------------------------------------------------------------------
  // Reactive wiring: redraw notes whenever (root, scale, notation) changes
  // ---------------------------------------------------------------------------
  state.selectedNoteIndex
    .combineWith(state.currentScaleNotes)
    .combineWith(state.useFrenchNotation.signal)
    .foreach { case (rootIdx, scaleNotes, useFrench) =>
      val scaleSet = scaleNotes.toSet
      var i = 0
      while i < notes.length do
        val n = notes(i)
        n.updateLabel(NoteCircleTheory.indexToNote(i, useFrench))
        if i == rootIdx then n.setSelectedState()
        else if scaleSet.contains(i) then n.setInScaleState()
        else n.setNormalState()
        i += 1
    }(using unsafeWindowOwner)

  /** Free GPU resources for every note and remove the group from the scene. */
  def dispose(): Unit =
    notes.foreach(_.dispose())
    scene.remove(group)
