package dev.cheleb.bassfretz

import THREE.*
import com.raquo.laminar.api.L.*
import org.scalajs.dom
import org.scalajs.dom.window
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportTopLevel

@main def main: Unit =

  // =====================================================
  // SCENE SETUP
  // =====================================================
  val scene = Scene()

  val camera = new PerspectiveCamera(
    fov = 60,
    aspect = window.innerWidth / window.innerHeight,
    near = 0.1,
    far = 100
  )
  camera.position.set(0, 8, 15)
  camera.lookAt(0, 0, 0)

  val renderer = WebGLRenderer(antialias = true)
  renderer.setSize(window.innerWidth, window.innerHeight)
  renderer.setPixelRatio(window.devicePixelRatio)
  renderer.setClearColor("#0a0a1a", 1)
  dom.document.getElementById("app").appendChild(renderer.domElement)

  // CSS2DRenderer overlay — used by the note circle for crisp HTML labels.
  val labelRenderer = new CSS2DRendererFacade()
  labelRenderer.setSize(window.innerWidth, window.innerHeight)
  val labelEl = labelRenderer.domElement
  labelEl.style.position = "absolute"
  labelEl.style.top = "0px"
  labelEl.style.left = "0px"
  // Critical: don't intercept canvas clicks (would break OrbitControls + raycaster).
  labelEl.style.pointerEvents = "none"
  dom.document.getElementById("app").appendChild(labelEl)

  // =====================================================
  // LIGHTING
  // =====================================================
  val dl1 = DirectionalLight(0xffffff, 1.5)
  dl1.position.set(0, 10, 5)
  scene.add(dl1)
  val dl2 = DirectionalLight(0x88aaff, 0.5)
  dl2.position.set(0, -3, 5)
  scene.add(dl2)
  scene.add(AmbientLight(0x404060, 1.0))

  // =====================================================
  // CONTROLS
  // =====================================================
  val controls = OrbitControls(camera, renderer.domElement)
  controls.enablePan = true
  controls.target.set(0, 0, 0)
  controls.update()

  // =====================================================
  // NECK GEOMETRY
  // =====================================================
  val neckGroup = Neck.create()
  scene.add(neckGroup)

  // =====================================================
  // REACTIVE STATE
  // =====================================================
  val rootKeyVar = Var("A")
  val scaleTypeVar = Var("major")
  val selectedIntervalsVar = Var(Set(0, 2, 4, 5, 7, 9, 11)) // Default to major scale
  val menuOpen = Var(false)

  // When scale type changes, update selected intervals to match the scale pattern
  scaleTypeVar.signal.foreach { scaleType =>
    selectedIntervalsVar.set(Scale.scaleIntervals(scaleType).toSet)
  }(using unsafeWindowOwner)

  // =====================================================
  // PRACTICE MODE
  // =====================================================
  val practiceMode = PracticeMode(rootKeyVar, scaleTypeVar)

  // Restart practice cycle when root or scale changes while practicing
  rootKeyVar.signal.foreach { _ =>
    practiceMode.restart()
  }(using unsafeWindowOwner)
  scaleTypeVar.signal.foreach { _ =>
    practiceMode.restart()
  }(using unsafeWindowOwner)

  // =====================================================
  // NOTE CIRCLE
  // =====================================================
  val noteCircleState = NoteCircleState(rootKeyVar, scaleTypeVar)
  val noteCircleRenderer = NoteCircleRenderer(scene, noteCircleState, camera)
  val noteCircleInteraction = NoteCircleInteraction(
    camera,
    renderer.domElement.asInstanceOf[dom.html.Canvas],
    noteCircleRenderer.noteMeshes,
    noteCircleState
  )
  noteCircleInteraction.setup()

  // Track note meshes so we can clear them on re-render
  val noteMeshes: js.Array[Mesh] = js.Array()

  // =====================================================
  // THREE.JS RENDER FUNCTION (called from signal)
  // =====================================================
  def renderSelection(rootKey: String, selectedIntervals: Set[Int]): Unit =
    for i <- 0 until noteMeshes.length do
      val m = noteMeshes(i)
      neckGroup.remove(m)
      m.geometry.asInstanceOf[js.Dynamic].dispose()
      m.material.asInstanceOf[js.Dynamic].dispose()
    noteMeshes.length = 0

    val positions = Scale.computeIntervalPositions(rootKey, selectedIntervals)

    positions.foreach { case (stringIdx, fret, semitone, _noteName) =>
      val isRoot = semitone == 0
      val color = Scale.intervalColors.getOrElse(semitone, 0xffffff)
      val radius = if isRoot then 0.14 else 0.08

      val sphere = Mesh(
        geometry = SphereGeometry(radius, 16, 16, 0, Math.PI * 2, 0, Math.PI),
        material = MeshStandardMaterial(
          color = color,
          roughness = 0.3,
          metalness = 0.2,
          emissive = color,
          emissiveIntensity = if isRoot then 1.2 else 0.7
        )
      )

      val xPos = Scale.stringXPositions(stringIdx)
      val zPos = Scale.getFretZPosition(fret, Neck.scaleLength)
      val yPos = Neck.neckHeight / 2 + 0.15

      sphere.position.set(xPos, yPos, zPos)
      neckGroup.add(sphere)
      noteMeshes.push(sphere)

      if isRoot then
        val outlineMat = MeshBasicMaterial(color = 0xffffff)
        outlineMat.asInstanceOf[js.Dynamic].transparent = true
        outlineMat.asInstanceOf[js.Dynamic].opacity = 0.35
        outlineMat.asInstanceOf[js.Dynamic].wireframe = true
        val outline = Mesh(
          geometry = SphereGeometry(radius * 1.4, 16, 16, 0, Math.PI * 2, 0, Math.PI),
          material = outlineMat
        )
        outline.position.set(xPos, yPos, zPos)
        neckGroup.add(outline)
        noteMeshes.push(outline)
    }

  /** Render for practice mode: shows roots + progressively revealed scale notes. */
  def renderPractice(rootKey: String): Unit =
    for i <- 0 until noteMeshes.length do
      val m = noteMeshes(i)
      neckGroup.remove(m)
      m.geometry.asInstanceOf[js.Dynamic].dispose()
      m.material.asInstanceOf[js.Dynamic].dispose()
    noteMeshes.length = 0

    val positions = practiceMode.computePracticePositions(rootKey)

    positions.foreach { case (stringIdx, fret, semitone, _noteName) =>
      val isRoot = semitone == 0
      val color = Scale.intervalColors.getOrElse(semitone, 0xffffff)
      val radius = if isRoot then 0.14 else 0.08

      val sphere = Mesh(
        geometry = SphereGeometry(radius, 16, 16, 0, Math.PI * 2, 0, Math.PI),
        material = MeshStandardMaterial(
          color = color,
          roughness = 0.3,
          metalness = 0.2,
          emissive = color,
          emissiveIntensity = if isRoot then 1.2 else 0.7
        )
      )

      val xPos = Scale.stringXPositions(stringIdx)
      val zPos = Scale.getFretZPosition(fret, Neck.scaleLength)
      val yPos = Neck.neckHeight / 2 + 0.15

      sphere.position.set(xPos, yPos, zPos)
      neckGroup.add(sphere)
      noteMeshes.push(sphere)

      if isRoot then
        val outlineMat = MeshBasicMaterial(color = 0xffffff)
        outlineMat.asInstanceOf[js.Dynamic].transparent = true
        outlineMat.asInstanceOf[js.Dynamic].opacity = 0.35
        outlineMat.asInstanceOf[js.Dynamic].wireframe = true
        val outline = Mesh(
          geometry = SphereGeometry(radius * 1.4, 16, 16, 0, Math.PI * 2, 0, Math.PI),
          material = outlineMat
        )
        outline.position.set(xPos, yPos, zPos)
        neckGroup.add(outline)
        noteMeshes.push(outline)
    }

  // =====================================================
  // WIRE STATE -> THREE.JS
  // =====================================================

  // Normal mode rendering (when practice is off)
  rootKeyVar.signal
    .combineWith(selectedIntervalsVar.signal)
    .combineWith(practiceMode.isActive.signal)
    .foreach { tuple =>
      val rootKey = tuple._1
      val intervals = tuple._2
      val practicing = tuple._3
      if !practicing then renderSelection(rootKey, intervals)
    }(using unsafeWindowOwner)

  // Practice mode rendering (when practice is on)
  rootKeyVar.signal
    .combineWith(practiceMode.isActive.signal)
    .combineWith(practiceMode.revealedCount.signal)
    .combineWith(practiceMode.currentSegmentIdx.signal)
    .foreach { tuple =>
      val rootKey = tuple._1
      val practicing = tuple._2
      // tuple._3 = revealedCount, tuple._4 = segmentIdx (both trigger re-render)
      if practicing then renderPractice(rootKey)
    }(using unsafeWindowOwner)

   // =====================================================
   // LAMINAR CONTROL PANEL  (visibility controlled by menu)
   // =====================================================
   val panelContainer = dom.document.createElement("div")
   dom.document.body.appendChild(panelContainer)
   render(panelContainer, ControlPanel.render(rootKeyVar, scaleTypeVar, selectedIntervalsVar, noteCircleState, practiceMode))

   // When-menu-open = panel VISIBLE; when menu closes = panel hidden
    menuOpen.signal.foreach { open =>
      val el = dom.document.getElementById("control-panel").asInstanceOf[dom.html.Element]
      if (el != null) el.style.display = if (open) "" else "none"
    }(using unsafeWindowOwner)

   // =====================================================
   // BOTTOM HINT
  // =====================================================
  val hintContainer = dom.document.createElement("div")
  dom.document.body.appendChild(hintContainer)
  render(
    hintContainer,
    div(
      styleAttr := "position: fixed; bottom: 20px; left: 50%; transform: translateX(-50%); color: #888; font-family: monospace; font-size: 12px; background: rgba(0,0,0,0.5); padding: 6px 12px; border-radius: 4px; z-index: 100;",
      "Bass Neck • Drag to rotate • Scroll to zoom"
    )
  )

    // =====================================================
    // TOP-LEFT MENU BUTTON  (toggles panel visibility)
    // =====================================================
    val menuContainer = dom.document.createElement("div")
    dom.document.body.appendChild(menuContainer)
    render(
      menuContainer,
      div(
        idAttr := "menu-wrapper",

        // Hamburger button — sits flush at the top-left corner
        button(
          idAttr := "menu-button",
          title := "Show/hide panel" ,
          child.text <-- menuOpen.signal.map(open => if (open) "▲" else "☰"),
          onClick --> { _ => menuOpen.update(!_) }
        ),

        // Tiny dropdown — "Hide control panel" / "Show control panel"
      )
    )

   // =====================================================
   // RENDER LOOP
  // =====================================================
  val animate: () => Unit = () =>
    controls.update()
    noteCircleRenderer.updateFaceCamera()
    renderer.render(scene, camera)
    labelRenderer.render(scene, camera)

  renderer.setAnimationLoop(animate)

  // =====================================================
  // RESIZE HANDLER
  // =====================================================
  window.addEventListener(
    "resize",
    (_: dom.Event) =>
      camera.aspect = window.innerWidth / window.innerHeight
      camera.updateProjectionMatrix()
      renderer.setSize(window.innerWidth, window.innerHeight)
      labelRenderer.setSize(window.innerWidth, window.innerHeight)
  )
