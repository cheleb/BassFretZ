package dev.cheleb.bassfretz

import THREE.*
import org.scalajs.dom
import scala.scalajs.js

/** Pointer interaction for the note circle: clicks on the canvas are projected via a `Raycaster` against the 12 note
  * meshes and dispatched to [[NoteCircleState.selectNoteByIndex]].
  *
  * @param camera
  *   the active scene camera (must match the one used by `WebGLRenderer.render`).
  * @param rendererDom
  *   the WebGL canvas (its bounding rect drives the NDC mapping).
  * @param noteMeshes
  *   meshes in chromatic order, each carrying a `noteIndex` field on `userData`.
  * @param state
  *   reactive state to mutate on click.
  */
class NoteCircleInteraction(
    camera: Camera,
    rendererDom: dom.html.Canvas,
    noteMeshes: js.Array[Mesh],
    state: NoteCircleState
):

  private val raycaster = Raycaster()
  private val mouse = Vector2()

  private val onClick: js.Function1[dom.MouseEvent, Unit] = (event: dom.MouseEvent) =>
    val rect = rendererDom.getBoundingClientRect()
    // Normalised device coordinates in [-1, 1].
    mouse.x = ((event.clientX - rect.left) / rect.width) * 2.0 - 1.0
    mouse.y = -((event.clientY - rect.top) / rect.height) * 2.0 + 1.0

    raycaster.setFromCamera(mouse, camera)
    val intersects = raycaster.intersectObjects(noteMeshes.asInstanceOf[js.Array[Object3D]], false)
    if intersects.length > 0 then
      val first = intersects(0).`object`
      val idxAny = first.userData.asInstanceOf[js.Dynamic].noteIndex
      if !js.isUndefined(idxAny) then state.selectNoteByIndex(idxAny.asInstanceOf[Int])

  /** Attach the click handler to the canvas. */
  def setup(): Unit =
    rendererDom.addEventListener("click", onClick)

  /** Detach the click handler. */
  def dispose(): Unit =
    rendererDom.removeEventListener("click", onClick)
