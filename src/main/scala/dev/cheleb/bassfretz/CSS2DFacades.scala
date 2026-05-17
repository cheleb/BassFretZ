package dev.cheleb.bassfretz

import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** `CSS2DObject` and `CSS2DRenderer` live in `three/examples/jsm/...` and are NOT part of the `threesjs` 0.1.0 facades.
  * We declare minimal `@JSImport` facades here so the rest of the app can use them with a Scala-friendly API.
  */

/** Wraps an HTML element as an `Object3D` so it can be added to a Three.js scene and rendered by `CSS2DRenderer`. */
@js.native
@JSImport("three/examples/jsm/renderers/CSS2DRenderer.js", "CSS2DObject")
class CSS2DObjectFacade(element: dom.Element) extends THREE.Object3D

/** Renders `CSS2DObject` instances as a transparent HTML overlay positioned to track 3D points. */
@js.native
@JSImport("three/examples/jsm/renderers/CSS2DRenderer.js", "CSS2DRenderer")
class CSS2DRendererFacade() extends js.Object:
  def setSize(width: Double, height: Double): Unit = js.native
  def render(scene: THREE.Scene, camera: THREE.Camera): Unit = js.native
  val domElement: dom.html.Element = js.native
