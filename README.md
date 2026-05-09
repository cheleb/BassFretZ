# BassFretZ 🎸

A 3D bass guitar fretboard visualizer built with [Scala.js](https://www.scala-js.org/), [Laminar](https://laminar.dev/) and [Three.js](https://threejs.org/).

## Demo

👉 **[Live demo on GitHub Pages](https://cheleb.github.io/BassFretZ/)**

## Tech Stack

- **Scala.js** — Scala compiled to JavaScript
- **Laminar** — reactive UI framework for Scala.js
- **Three.js** — 3D rendering in the browser
- **Vite** — fast frontend build tool

## Development

```bash
Install JS dependencies
bun install
```
Start dev server (with Scala.js watch)

```bash
sbt ~fastLinkJS
bun run dev
```


Incremental build:

```bash
sbt ~fullLinkJS
```

