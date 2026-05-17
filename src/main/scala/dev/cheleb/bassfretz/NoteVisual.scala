package dev.cheleb.bassfretz

import THREE.*
import org.scalajs.dom
import scala.scalajs.js

/** A single note in the note circle: a small sphere mesh + a CSS2D HTML label.
  *
  * Wrapping the label in a `CSS2DObject` is delegated to [[NoteCircleRenderer]], which owns the renderer-specific
  * imports.
  *
  * @param index
  *   chromatic index 0..11
  * @param position
  *   centre of the sphere on the parent group's local coordinate system
  * @param radius
  *   sphere radius
  */
class NoteVisual(
    val index: Int,
    val position: Vector3,
    radius: Double = 0.25
):
  // ---------------------------------------------------------------------------
  // 3D mesh
  // ---------------------------------------------------------------------------
  private val geom = SphereGeometry(radius, 24, 16)
  private val mat: MeshStandardMaterial = MeshStandardMaterial(
    color = 0x666666,
    roughness = 0.4,
    metalness = 0.2,
    emissive = 0x000000,
    emissiveIntensity = 0.0
  )

  val mesh: Mesh = Mesh(geom, mat)
  mesh.position.copy(position)
  // Stash the chromatic index for raycaster-based lookup in NoteCircleInteraction.
  // Assigning a fresh literal rather than mutating `userData` in place avoids relying
  // on Three.js's default `{}` initialization (the facade types it as `js.Object`,
  // which could be `null`/`undefined` in some configurations).
  mesh.userData = js.Dynamic.literal(noteIndex = index).asInstanceOf[js.Object]

  // ---------------------------------------------------------------------------
  // HTML label (wrapped in CSS2DObject by the renderer)
  // ---------------------------------------------------------------------------
  val labelDiv: dom.html.Div =
    val d = dom.document.createElement("div").asInstanceOf[dom.html.Div]
    d.className = "note-circle-label"
    d

  def updateLabel(text: String): Unit =
    labelDiv.textContent = text

  // ---------------------------------------------------------------------------
  // Visual states
  // ---------------------------------------------------------------------------
  def setNormalState(): Unit =
    mat.color.setHex(0x666666)
    mat.asInstanceOf[js.Dynamic].emissive.setHex(0x000000)
    mat.asInstanceOf[js.Dynamic].emissiveIntensity = 0.0
    labelDiv.classList.remove("in-scale")
    labelDiv.classList.remove("selected")

  def setInScaleState(): Unit =
    mat.color.setHex(0x33dd55)
    mat.asInstanceOf[js.Dynamic].emissive.setHex(0x115522)
    mat.asInstanceOf[js.Dynamic].emissiveIntensity = 0.5
    labelDiv.classList.remove("selected")
    labelDiv.classList.add("in-scale")

  def setSelectedState(): Unit =
    mat.color.setHex(0xffaa00)
    mat.asInstanceOf[js.Dynamic].emissive.setHex(0xffaa00)
    mat.asInstanceOf[js.Dynamic].emissiveIntensity = 0.9
    labelDiv.classList.remove("in-scale")
    labelDiv.classList.add("selected")

  /** Root note that also lies in the displayed scale: render as Selected (root color wins). */
  def setRootInScaleState(): Unit = setSelectedState()

  /** Free GPU resources. Called by [[NoteCircleRenderer.dispose]]. */
  def dispose(): Unit =
    geom.asInstanceOf[js.Dynamic].dispose()
    mat.asInstanceOf[js.Dynamic].dispose()
