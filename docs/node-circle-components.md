# Note Circle Feature - Component Analysis

## Overview
This document provides a detailed component breakdown for implementing the Note Circle feature as described in `docs/node-circle.md`. The analysis identifies self-contained, testable modules that can be composed to create the complete application.

## Architecture Context
The BassFretZ project already has a solid foundation with:
- **ThreeScalaJS** bindings for Three.js 3D rendering
- **Laminar** for reactive UI components
- Existing music theory utilities in `Scale.scala`
- 3D rendering infrastructure in `Main.scala`

## Component Breakdown

### 1. Music Theory Core (Priority: High - Foundation)
**Module:** `NoteCircleTheory.scala`

**Responsibilities:**
- Define chromatic scale notes and their relationships
- Provide French solfège notation mappings
- Calculate major scale intervals from any root note
- Handle note name conversions

**Dependencies:** None (pure Scala, no Three.js or DOM)

**API:**
```scala
object NoteCircleTheory:
  // Chromatic notes in order (C, C#, D, ...)
  val chromaticNotes: Array[String]
  
  // French notation equivalents
  val frenchNotation: Array[String] = Array("Do", "Do#", "Ré", "Ré#", "Mi", "Fa", "Fa#", "Sol", "Sol#", "La", "La#", "Si")
  
  // Major scale interval pattern (W-W-H-W-W-W-H)
  val majorScaleIntervals: List[Int] = List(0, 2, 4, 5, 7, 9, 11)
  
  // Get major scale notes for a given root
  def getMajorScale(rootIndex: Int): List[Int]
  
  // Convert between note names and indices
  def noteToIndex(note: String): Int
  def indexToNote(index: Int, useFrench: Boolean): String
```

**Test Cases:**
- Happy path: All 12 notes return correct indices
- Edge case: Invalid note names throw exceptions
- Happy path: Major scale calculation for all 12 roots
- Edge case: Wrapping around octave boundaries
- Happy path: French notation conversion for all notes

---

### 2. Circle Geometry (Priority: High - Foundation)
**Module:** `NoteCircleGeometry.scala`

**Responsibilities:**
- Create and manage the circular layout of 12 note positions
- Calculate 3D positions for notes on a circle
- Handle circle scaling and responsive sizing

**Dependencies:**
- ThreeScalaJS (for Vector3, Group)
- NoteCircleTheory (for note count)

**API:**
```scala
import THREE.*

object NoteCircleGeometry:
  val defaultRadius: Double = 5.0
  val noteSpacing: Double = Math.PI * 2 / 12
  
  // Create a group containing the circle guide
  def createCircleGuide(radius: Double = defaultRadius, color: Int = 0x333333): Group
  
  // Get position for note at given index
  def getNotePosition(index: Int, radius: Double = defaultRadius): Vector3
  
  // Create all 12 note positions as a group
  def createNotePositions(radius: Double): Array[Vector3]
```

**Test Cases:**
- Happy path: All 12 positions are correctly spaced on circle
- Edge case: Radius of 0 returns origin for all points
- Happy path: Positions form perfect circle when projected to XY plane
- Edge case: Negative radius mirrors positions correctly

---

### 3. Note Visualization (Priority: High - Foundation)
**Module:** `NoteVisual.scala`

**Responsibilities:**
- Create 3D mesh for individual note representation
- Handle note selection visual states (normal, selected, in-scale)
- Manage note labels (text sprites)
- Provide animation capabilities for note interactions

**Dependencies:**
- ThreeScalaJS (for Mesh, SphereGeometry, TextGeometry, etc.)
- NoteCircleGeometry (for positions)

**API:**
```scala
import THREE.*
import com.raquo.laminar.api.L.*

class NoteVisual(
    index: Int,
    position: Vector3,
    radius: Double = 0.3,
    onClick: () => Unit
):
  val mesh: Mesh
  val label: Sprite | Mesh  // Text label for note name
  
  // Visual states
  def setNormalState(): Unit
  def setSelectedState(): Unit
  def setInScaleState(): Unit
  def setRootInScaleState(): Unit
  
  // Update label text
  def updateLabel(text: String): Unit
  
  // Animation
  def pulseHighlight(): Unit
  def fadeIn(duration: Double): Unit
```

**Test Cases:**
- Happy path: Note mesh created with correct position
- Happy path: All visual states change appearance correctly
- Edge case: Empty label text doesn't break rendering
- Happy path: Click handler triggers correctly
- Edge case: Multiple rapid state changes handled gracefully

---

### 4. Note Circle Renderer (Priority: High - Core Feature)
**Module:** `NoteCircleRenderer.scala`

**Responsibilities:**
- Create and manage all 12 note visuals
- Handle circle layout and responsive resizing
- Manage selection state and scale highlighting
- Coordinate with Three.js scene

**Dependencies:**
- ThreeScalaJS (for Group, Scene)
- NoteCircleGeometry (for positions)
- NoteVisual (for individual notes)
- NoteCircleTheory (for scale calculations)

**API:**
```scala
import THREE.*
import com.raquo.laminar.api.L.*

class NoteCircleRenderer(
    scene: Scene,
    onNoteSelected: (Int) => Unit
):
  val group: Group  // Parent group for all circle elements
  private val notes: Array[NoteVisual]
  
  // Create the circle with all notes
  def create(radius: Double = NoteCircleGeometry.defaultRadius): Unit
  
  // Update selected note (index 0-11)
  def setSelectedNote(index: Int): Unit
  
  // Highlight major scale for given root
  def highlightMajorScale(rootIndex: Int): Unit
  
  // Update all note labels (letter or French)
  def updateNotation(useFrench: Boolean): Unit
  
  // Cleanup
  def dispose(): Unit
```

**Test Cases:**
- Happy path: All 12 notes created and visible
- Happy path: Selecting note triggers callback with correct index
- Happy path: Major scale highlights correct 7 notes
- Edge case: Selecting same note twice handled correctly
- Happy path: Notation toggle updates all labels
- Edge case: Dispose removes all meshes from scene

---

### 5. State Management (Priority: High - Core Feature)
**Module:** `NoteCircleState.scala`

**Responsibilities:**
- Manage application state (selected note, notation mode)
- Provide reactive signals for UI updates
- Handle state persistence if needed

**Dependencies:**
- Laminar (for Var, Signal)

**API:**
```scala
import com.raquo.laminar.api.L.*

class NoteCircleState:
  val selectedNoteIndex: Var[Int] = Var(0)  // Default to C
  val useFrenchNotation: Var[Boolean] = Var(false)
  
  // Derived signals
  val selectedNoteName: Signal[String]
  val currentScaleNotes: Signal[List[Int]]
  
  // State modifiers
  def selectNote(index: Int): Unit
  def toggleNotation(): Unit
  def setNotation(french: Boolean): Unit
```

**Test Cases:**
- Happy path: Initial state has C selected, letter notation
- Happy path: Selecting note updates selectedNoteIndex
- Happy path: Toggling notation flips useFrenchNotation
- Happy path: Derived signals update correctly on state changes
- Edge case: Invalid note index handled gracefully

---

### 6. Control Panel (Priority: Medium - UI)
**Module:** `NoteCircleControlPanel.scala`

**Responsibilities:**
- Create Laminar-based control UI
- Provide note selection controls
- Provide notation toggle
- Display current scale information

**Dependencies:**
- Laminar
- NoteCircleState (for state binding)
- NoteCircleTheory (for note names)

**API:**
```scala
import com.raquo.laminar.api.L.*

object NoteCircleControlPanel:
  def render(state: NoteCircleState): Element =
    div(
      // Note selection dropdown
      select(
        options for all 12 notes,
        onChange updates state.selectedNoteIndex
      ),
      
      // Notation toggle
      label(
        input(type := "checkbox"),
        "French Notation"
      ),
      
      // Current scale display
      div(
        "Current Scale: ",
        child.text <-- state.currentScaleNotes.map(notes => 
          notes.map(i => NoteCircleTheory.indexToNote(i, state.useFrenchNotation.now)).mkString(" ")
        )
      )
    )
```

**Test Cases:**
- Happy path: All 12 notes appear in dropdown
- Happy path: Selecting note updates state
- Happy path: Toggle switches notation mode
- Happy path: Scale display updates when note changes
- Edge case: Rapid interactions don't cause UI glitches

---

### 7. Interaction Handler (Priority: Medium - Core Feature)
**Module:** `NoteCircleInteraction.scala`

**Responsibilities:**
- Handle mouse/touch events on note circle
- Implement raycasting for note selection
- Manage hover states
- Provide click feedback

**Dependencies:**
- ThreeScalaJS (for Raycaster, Mouse)
- NoteCircleRenderer (for note meshes)
- NoteCircleState (for state updates)

**API:**
```scala
import THREE.*
import org.scalajs.dom

class NoteCircleInteraction(
    camera: Camera,
    renderer: WebGLRenderer,
    noteMeshes: Array[Mesh],
    state: NoteCircleState
):
  private val raycaster: Raycaster = Raycaster()
  private val mouse: Vector2 = Vector2()
  
  // Initialize event listeners
  def setup(): Unit
  
  // Handle window resize
  def onResize(width: Int, height: Int): Unit
  
  // Cleanup
  def dispose(): Unit
```

**Test Cases:**
- Happy path: Clicking note selects it
- Happy path: Hover highlights note temporarily
- Edge case: Clicking empty space doesn't change selection
- Happy path: Touch events work on mobile
- Edge case: Rapid clicks handled correctly
- Happy path: Resize updates raycasting correctly

---

### 8. Main Integration (Priority: Medium - Composition)
**Module:** Modifications to `Main.scala`

**Responsibilities:**
- Integrate all components into main application
- Set up Three.js scene with note circle
- Wire up state management
- Handle rendering loop

**Dependencies:**
- All above modules
- Existing BassFretZ infrastructure

**Integration Steps:**
1. Create `NoteCircleState` instance
2. Create `NoteCircleRenderer` with scene
3. Create `NoteCircleInteraction` with camera and renderer
4. Add `NoteCircleControlPanel` to DOM
5. Wire up state changes to trigger re-rendering
6. Add note circle group to scene

---

## Implementation Order

Based on dependencies, implement in this order:

1. **NoteCircleTheory** (no dependencies)
2. **NoteCircleGeometry** (depends on NoteCircleTheory)
3. **NoteVisual** (depends on NoteCircleGeometry)
4. **NoteCircleState** (no dependencies beyond Laminar)
5. **NoteCircleRenderer** (depends on NoteVisual, NoteCircleGeometry, NoteCircleTheory)
6. **NoteCircleInteraction** (depends on NoteCircleRenderer, NoteCircleState)
7. **NoteCircleControlPanel** (depends on NoteCircleState, NoteCircleTheory)
8. **Main Integration** (depends on all above)

---

## Reusable Components from Existing Codebase

The following existing components can be leveraged:

1. **Scale.scala**: 
   - `chromaticNotes` array
   - `noteIndexMap` for note-to-index conversion
   - `majorScaleIntervals` (already defined as "major" in scaleIntervals)

2. **Main.scala**:
   - Three.js scene setup (camera, renderer, lighting)
   - OrbitControls integration
   - Animation loop structure
   - Resize handling

3. **ControlPanel.scala**:
   - Laminar component patterns
   - Styling conventions
   - Signal binding patterns

---

## New Dependencies Required

No new dependencies needed. The existing stack provides:
- ThreeScalaJS for 3D rendering
- Laminar for reactive UI
- Scala.js DOM for browser interaction

---

## Testing Strategy

### Unit Tests
- Test pure functions in NoteCircleTheory
- Test geometry calculations in NoteCircleGeometry
- Test state management in NoteCircleState

### Integration Tests
- Test NoteVisual with mock positions
- Test NoteCircleRenderer with mock scene
- Test NoteCircleInteraction with mock events

### Manual Testing
- Visual verification of circle layout
- Interaction testing (click, hover, touch)
- Responsive design testing
- Performance testing (60fps with all notes)

---

## Performance Considerations

1. **Mesh Count**: 12 note meshes + 12 labels = 24 objects (very manageable)
2. **Geometry**: Use simple geometries (spheres, sprites) for notes
3. **Text Rendering**: Consider CSS2DRenderer for labels instead of 3D text
4. **Raycasting**: Only test against note meshes, not labels
5. **State Updates**: Batch DOM updates when possible

---

## Edge Cases to Handle

1. **Mobile Touch**: Ensure touch events work alongside mouse
2. **Small Screens**: Circle should scale down appropriately
3. **High DPI**: Renderer should handle pixel ratio correctly
4. **Note Name Conflicts**: Handle enharmonic equivalents (C#/Db)
5. **Animation Performance**: Avoid expensive operations in render loop
6. **Memory Leaks**: Properly dispose of meshes and event listeners

---

## API Research Notes

### ThreeScalaJS Relevant APIs

1. **Raycaster**:
   ```scala
   val raycaster = Raycaster()
   raycaster.setFromCamera(mouse, camera)
   val intersects = raycaster.intersectObjects(meshes)
   ```

2. **Sprite/SpriteMaterial**: For 2D labels that always face camera
   ```scala
   val spriteMap = TextureLoader().load("label.png")
   val spriteMat = SpriteMaterial(map = spriteMap)
   val sprite = Sprite(spriteMat)
   ```

3. **CSS2DRenderer**: For HTML-based labels
   ```scala
   val labelRenderer = CSS2DRenderer()
   labelRenderer.setSize(width, height)
   ```

4. **OrbitControls**: Already integrated in Main.scala

### Laminar Relevant APIs

1. **Event Handling**:
   ```scala
   onClick --> callback
   onChange.mapToValue --> var.writer
   ```

2. **Reactive Signals**:
   ```scala
   val signal = var.signal
   signal.map(transform).foreach(observer)
   ```

3. **DOM Integration**:
   ```scala
   render(domElement, element)
   ```
