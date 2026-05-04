package dev.cheleb.mythreeapp

import THREE.*
import org.scalajs.dom
import org.scalajs.dom.window
import org.scalajs.dom.html
import scala.scalajs.js

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
  // SELECTION STATE
  // =====================================================
  var rootKey: String = "A"
  // Default: dominant-7 arpeggio (R, 3, 5, b7).
  var selectedIntervals: Set[Int] = Set(0, 4, 7, 10)

  // Track meshes we add so we can clear them on re-render.
  val noteMeshes: js.Array[Mesh] = js.Array()

  // =====================================================
  // UI PANEL SKELETON
  // =====================================================
  val panel = dom.document.createElement("div").asInstanceOf[html.Div]
  panel.setAttribute("id", "control-panel")
  panel.setAttribute(
    "style",
    "position: fixed; top: 20px; left: 20px; color: #ffffff; font-family: monospace; font-size: 14px; background: rgba(0,0,0,0.75); padding: 14px 18px; border-radius: 6px; border: 1px solid #444; z-index: 100; min-width: 220px; max-width: 280px;"
  )
  dom.document.body.appendChild(panel)

  // Title
  val titleHeader = dom.document.createElement("div").asInstanceOf[html.Div]
  titleHeader.setAttribute("style", "font-size: 16px; font-weight: bold; margin-bottom: 10px;")
  titleHeader.innerText = "Bass Neck Explorer"
  panel.appendChild(titleHeader)

  // Root note dropdown label + select
  val rootLabel = dom.document.createElement("div").asInstanceOf[html.Div]
  rootLabel.setAttribute("style", "margin-bottom: 4px; color: #ccc;")
  rootLabel.innerText = "Root note:"
  panel.appendChild(rootLabel)

  val rootSelect = dom.document.createElement("select").asInstanceOf[html.Select]
  rootSelect.setAttribute(
    "style",
    "width: 100%; padding: 4px 6px; background: #222; color: #fff; border: 1px solid #555; border-radius: 4px; font-family: monospace; font-size: 14px; margin-bottom: 12px;"
  )
  Scale.chromaticNotes.foreach { note =>
    val opt = dom.document.createElement("option").asInstanceOf[html.Option]
    opt.value = note
    opt.text = note
    if note == rootKey then opt.selected = true
    rootSelect.appendChild(opt)
  }
  panel.appendChild(rootSelect)

  // Intervals section label + container
  val intervalsLabel = dom.document.createElement("div").asInstanceOf[html.Div]
  intervalsLabel.setAttribute("style", "margin-bottom: 6px; color: #ccc;")
  intervalsLabel.innerText = "Intervals:"
  panel.appendChild(intervalsLabel)

  val intervalsBox = dom.document.createElement("div").asInstanceOf[html.Div]
  intervalsBox.setAttribute(
    "style",
    "display: grid; grid-template-columns: 1fr 1fr; gap: 4px 10px; margin-bottom: 12px;"
  )
  panel.appendChild(intervalsBox)

  // Legend
  val legendTitle = dom.document.createElement("div").asInstanceOf[html.Div]
  legendTitle.setAttribute("style", "margin-bottom: 4px; color: #ccc;")
  legendTitle.innerText = "Selected:"
  panel.appendChild(legendTitle)

  val legend = dom.document.createElement("div").asInstanceOf[html.Div]
  legend.setAttribute("id", "legend")
  legend.setAttribute("style", "display: flex; flex-direction: column; gap: 3px; font-size: 13px;")
  panel.appendChild(legend)

  // =====================================================
  // LEGEND & RENDER FUNCTIONS
  // =====================================================
  def updateLegend(): Unit =
    legend.innerHTML = ""
    val rootIndex = Scale.noteIndexMap(rootKey)
    Scale.commonIntervals
      .filter { case (s, _, _) => selectedIntervals.contains(s) }
      .foreach { case (semitone, shortLabel, longLabel) =>
        val note = Scale.chromaticNotes((rootIndex + semitone) % 12)
        val colorHex = f"#${Scale.intervalColors(semitone)}%06x"
        val row = dom.document.createElement("div").asInstanceOf[html.Div]
        row.setAttribute(
          "style",
          "display: flex; align-items: center; gap: 8px;"
        )
        row.innerHTML =
          s"""<span style="display:inline-block;width:10px;height:10px;background:$colorHex;border:1px solid #666;border-radius:2px;"></span>
             |<span style="min-width: 24px; color:#fff;">$shortLabel</span>
             |<span style="color:#aaa;">$longLabel</span>
             |<span style="margin-left:auto; color:#ffd;">$note</span>""".stripMargin
        legend.appendChild(row)
      }

  def renderSelection(): Unit =
    // Remove previous meshes from the neck group and dispose resources.
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

      // Outline sphere for root notes: a slightly larger, translucent wireframe sphere.
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

    updateLegend()

  // =====================================================
  // WIRE UP UI EVENTS (must happen after renderSelection is defined)
  // =====================================================
  rootSelect.addEventListener(
    "change",
    (_: dom.Event) =>
      rootKey = rootSelect.value
      renderSelection()
  )

  Scale.commonIntervals.foreach { case (semitone, shortLabel, longLabel) =>
    val wrapper = dom.document.createElement("label").asInstanceOf[html.Label]
    wrapper.setAttribute(
      "style",
      "display: flex; align-items: center; gap: 6px; cursor: pointer; user-select: none;"
    )
    wrapper.title = longLabel

    val cb = dom.document.createElement("input").asInstanceOf[html.Input]
    cb.`type` = "checkbox"
    cb.checked = selectedIntervals.contains(semitone)
    cb.addEventListener(
      "change",
      (_: dom.Event) =>
        if cb.checked then selectedIntervals = selectedIntervals + semitone
        else selectedIntervals = selectedIntervals - semitone
        renderSelection()
    )
    wrapper.appendChild(cb)

    val swatch = dom.document.createElement("span").asInstanceOf[html.Span]
    val colorHex = f"#${Scale.intervalColors(semitone)}%06x"
    swatch.setAttribute(
      "style",
      s"display: inline-block; width: 10px; height: 10px; background: $colorHex; border: 1px solid #666; border-radius: 2px;"
    )
    wrapper.appendChild(swatch)

    val lbl = dom.document.createElement("span").asInstanceOf[html.Span]
    lbl.innerText = shortLabel
    wrapper.appendChild(lbl)

    intervalsBox.appendChild(wrapper)
  }

  // =====================================================
  // BOTTOM HINT
  // =====================================================
  val titleEl = dom.document.createElement("div").asInstanceOf[html.Div]
  titleEl.setAttribute(
    "style",
    "position: fixed; bottom: 20px; left: 50%; transform: translateX(-50%); color: #888; font-family: monospace; font-size: 12px; background: rgba(0,0,0,0.5); padding: 6px 12px; border-radius: 4px; z-index: 100;"
  )
  titleEl.innerText = "Bass Neck • Drag to rotate • Scroll to zoom"
  dom.document.body.appendChild(titleEl)

  // =====================================================
  // INITIAL RENDER
  // =====================================================
  renderSelection()

  // =====================================================
  // RENDER LOOP (static notes; just rotate/zoom via controls)
  // =====================================================
  val animate: () => Unit = () =>
    controls.update()
    renderer.render(scene, camera)

  renderer.setAnimationLoop(animate)

  // =====================================================
  // EVENT HANDLERS
  // =====================================================
  window.addEventListener(
    "resize",
    (_: dom.Event) =>
      camera.aspect = window.innerWidth / window.innerHeight
      camera.updateProjectionMatrix()
      renderer.setSize(window.innerWidth, window.innerHeight)
  )
