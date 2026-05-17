package dev.cheleb.bassfretz

import THREE.*
import org.scalajs.dom
import scala.scalajs.js

/** Pointer interaction for the note circle: clicks on the canvas are projected via a `Raycaster` against the 12 note
  * meshes and dispatched to [[NoteCircleState.selectNoteByIndex]].
  *
  * A small drag-distance gate prevents OrbitControls drags from accidentally selecting a note when the user releases
  * over a sphere.
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

  /** Maximum pointer travel (px, squared) between pointerdown and click that still counts as a click. */
  private val DragThresholdSq: Double = 6.0 * 6.0

  private val raycaster = Raycaster()
  private val mouse = Vector2()

  private var downX: Double = 0.0
  private var downY: Double = 0.0

  private val onPointerDown: js.Function1[dom.PointerEvent, Unit] = (event: dom.PointerEvent) =>
    downX = event.clientX
    downY = event.clientY

  private val onClick: js.Function1[dom.MouseEvent, Unit] = (event: dom.MouseEvent) =>
    val dx = event.clientX - downX
    val dy = event.clientY - downY
    if dx * dx + dy * dy <= DragThresholdSq then
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

  /** Attach pointer handlers to the canvas. */
  def setup(): Unit =
    rendererDom.addEventListener("pointerdown", onPointerDown)
    rendererDom.addEventListener("click", onClick)

  /** Detach pointer handlers. */
  def dispose(): Unit =
    rendererDom.removeEventListener("pointerdown", onPointerDown)
    rendererDom.removeEventListener("click", onClick)
