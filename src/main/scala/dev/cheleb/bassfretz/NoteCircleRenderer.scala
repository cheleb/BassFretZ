package dev.cheleb.bassfretz

import THREE.*
import com.raquo.airstream.ownership.Subscription
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
    camera: Camera,
    radius: Double = NoteCircleGeometry.defaultRadius,
    centerOffset: Vector3 = Vector3(5, 4, 0)
):

  /** Update the group's rotation so it always faces the camera. */
  def updateFaceCamera(): Unit =
    // Make the group look at the camera position. Since the group's guide circle is in the XY plane,
    // and we want the circle's normal (Z axis) to face the camera, we lookAt a point in the opposite
    // direction from the group to the camera.
    val target = new Vector3(camera.position.x, camera.position.y, camera.position.z)
    group.lookAt(target)

  /** Master group; parent of guide circle, sphere meshes, and label objects. */
  val group: Group = Group()
  group.position.copy(centerOffset)

  // Guide circle (thin line on the XY plane).
  private val guide: Group = NoteCircleGeometry.createCircleGuide(radius)
  group.add(guide)

  // 12 note visuals, in chromatic order (C..B). Labels are tracked separately so dispose() can
  // remove them from the scene graph (and let CSS2DRenderer drop their DOM nodes on the next render).
  private val notes: Array[NoteVisual] =
    NoteCircleGeometry.createNotePositions(radius).zipWithIndex.map { case (pos, idx) =>
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
   // Reactive wiring: redraw notes whenever (root, scale type, notation) changes.
   //
   // Combine all three signals for coherent updates.
   // ---------------------------------------------------------------------------
   private val subscription: Subscription =
     state.selectedNoteIndex
       .combineWith(state.currentScaleNotes)
       .combineWith(state.useFrenchNotation.signal)
       .foreach { tuple =>
         val selectedIdx = tuple._1
         val scaleNotes = tuple._2
         val useFrench = tuple._3
         val scaleSet = scaleNotes.toSet
         var i = 0
         while i < notes.length do
           val n = notes(i)
           n.updateLabel(NoteCircleTheory.indexToNote(i, useFrench))
           if selectedIdx == i then n.setSelectedState()
           else if scaleSet.contains(i) then n.setInScaleState()
           else n.setNormalState()
           i += 1
       }(using unsafeWindowOwner)

  /** Free GPU resources (sphere geometry/material for every note + the guide line) and detach scene/state. */
  def dispose(): Unit =
    // Stop reacting to state changes.
    subscription.kill()
    // Dispose every note's GPU resources.
    notes.foreach(_.dispose())
    // Dispose the guide line's geometry and material (they are not owned by NoteVisual).
    guide.children.foreach { obj =>
      val dyn = obj.asInstanceOf[js.Dynamic]
      if !js.isUndefined(dyn.geometry) then dyn.geometry.dispose()
      if !js.isUndefined(dyn.material) then dyn.material.dispose()
    }
    // Removing the group from the scene also detaches every CSS2DObject; CSS2DRenderer will
    // drop the orphan label <div>s on its next render call.
    scene.remove(group)
