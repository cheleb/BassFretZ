package dev.cheleb.mythreeapp

import THREE.*
import com.raquo.laminar.api.L.*
import org.scalajs.dom
import org.scalajs.dom.window
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
  renderer.setClearColor(