package dev.cheleb.bassfretz

import THREE.*
import org.scalajs.dom
import org.scalajs.dom.window

object Neck:
  // Neck dimensions
  val scaleLength = 20.0
  val neckWidth = 2.0
  val neckHeight = 0.3

  /** Create the complete bass neck geometry including fretboard, nut, frets, strings, and markers.
    * Returns a Group containing all neck elements.
    */
  def create(): Group =
    val neckGroup = Group()

    // Fretboard (rosewood colored box)
    val fretboard = Mesh(
      geometry = BoxGeometry(neckWidth, neckHeight, scaleLength, 1, 1, 1),
      material = MeshStandardMaterial(
        color = 0x3b1f0a,
        roughness = 0.8,
        metalness = 0.1
      )
    )
    neckGroup.add(fretboard)

    // Nut (bone-colored bar at the headstock end)
    val nut = Mesh(
      geometry = BoxGeometry(neckWidth + 0.1, 0.15, 0.1, 1, 1, 1),
      material = MeshStandardMaterial(color = 0xfffff0, roughness = 0.4, metalness = 0.0)
    )
    nut.position.set(0, neckHeight / 2 + 0.075, -scaleLength / 2)
    neckGroup.add(nut)

    // Frets positioned using 12th-root-of-2 formula for equal temperament
    val fretMat = MeshStandardMaterial(color = 0xcccccc, roughness = 0.2, metalness = 0.9)
    val fretPositions = (1 to 20).map(n => scaleLength * (1.0 - 1.0 / Math.pow(2.0, n.toDouble / 12.0)))
    fretPositions.foreach { zPos =>
      val fret = Mesh(BoxGeometry(neckWidth + 0.1, 0.06, 0.06), fretMat)
      fret.position.set(0, neckHeight / 2 + 0.03, -scaleLength / 2 + zPos)
      neckGroup.add(fret)
    }

    // Strings (E, A, D, G) - cylinders rotated to align with neck
    val stringRadii = List(0.04, 0.035, 0.028, 0.022) // Thickest to thinnest
    val stringXPositions = List(-1.35 / 2, -0.45 / 2, 0.45 / 2, 1.35 / 2)
    val stringMat = MeshStandardMaterial(color = 0xdddddd, roughness = 0.3, metalness = 0.9)
    stringRadii.zip(stringXPositions).foreach { (radius, xPos) =>
      val string = Mesh(CylinderGeometry(radius, radius, scaleLength, 8), stringMat)
      string.rotation.x = Math.PI / 2  // Align along Z axis
      string.position.set(xPos, neckHeight / 2 + 0.12, 0)
      neckGroup.add(string)
    }

    // Fret markers (dot inlays) at standard positions
    val markerFrets = List(3, 5, 7, 9, 12, 12, 15, 17, 19)
    val markerMat = MeshStandardMaterial(color = 0xf0f0e0, roughness = 0.5)
    markerFrets.zipWithIndex.foreach { (fretNum, idx) =>
      val prevFretZ = if fretNum == 1 then 0.0
        else scaleLength * (1.0 - 1.0 / Math.pow(2.0, (fretNum - 1).toDouble / 12.0))
      val currFretZ = scaleLength * (1.0 - 1.0 / Math.pow(2.0, fretNum.toDouble / 12.0))
      val zWorld = -scaleLength / 2 + (prevFretZ + currFretZ) / 2.0
      val xOffset = if fretNum == 12 then (if idx == 4 then -0.7 else 0.7) else 0.0
      val marker = Mesh(SphereGeometry(0.12, 16, 16, 0, Math.PI * 2, 0, Math.PI), markerMat)
      marker.position.set(xOffset, neckHeight / 2 + 0.01, zWorld)
      marker.scale.set(1, 0.3, 1)  // Flattened disc
      neckGroup.add(marker)
    }

    neckGroup