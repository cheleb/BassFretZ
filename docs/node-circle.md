# BassFretZ Note Circle

## Overview
BassFretZ Note Circle is an interactive music theory visualization tool designed to help musicians understand the relationship between chromatic notes and major scales. The application displays the 12 notes of the chromatic scale arranged in a circular format, allowing users to select any root note and instantly visualize its corresponding major scale through highlighted notes in the circle. Notes can be displayed using either standard letter notation (C, C#, D, etc.) or French solfège notation (Do, Ré, Mi, etc.), catering to different musical educational backgrounds.

## User Flow and Behavior

### Initial State
Upon launching the application, users are presented with a clean, dark-background interface featuring:
- A central circle containing 12 equally spaced points representing the chromatic scale notes
- Each point initially displays its note name in letter notation (C, C#, D, D#, E, F, F#, G, G#, A, A#, B) in a legible white font
- A subtle highlighting or pulsating effect on the C note to indicate it as the default selected root
- A control panel positioned conveniently (typically bottom or side) containing:
  * A toggle switch to alternate between letter notation and French solfège notation
  * Brief instructions for interaction

### Note Selection
Users interact with the note circle through:
1. **Click/Tap**: Selecting any note point by clicking or tapping on it
2. **Visual Feedback**: 
   - The selected root note point enlarges slightly and changes color (typically to a bright accent color like cyan or yellow)
   - A subtle glow or outline appears around the selected point to emphasize selection
   - All other notes return to their default appearance

### Major Scale Display
Upon note selection:
1. The application calculates the major scale intervals (whole, whole, half, whole, whole, whole, half) from the selected root note
2. The corresponding notes in the circle that belong to the major scale are highlighted:
   - These points change to a distinct highlight color (typically green or blue)
   - Non-scale notes remain in their default state or become slightly dimmed
   - The root note maintains its special selection styling while also showing as part of the scale
3. Optional: A text label may appear near the circle displaying the scale name (e.g., "C Major Scale") and its note sequence

### Notation Toggle
Users can switch between notation systems at any time:
- Activating the toggle instantly changes all note labels in the circle:
  * Letter notation: C, C#, D, D#, E, F, F#, G, G#, A, A#, B
  * French notation: Do, Do#, Ré, Ré#, Mi, Fa, Fa#, Sol, Sol#, La, La#, Si
- The transition is smooth and instantaneous without losing the current selection state
- The selected root note and scale highlights remain consistent regardless of notation choice

### Interaction Details
- **Responsive Design**: The circle scales appropriately to different screen sizes while maintaining proportional spacing
- **Feedback**: Subtle audio feedback (optional click sound) and haptic feedback (on supported devices) confirm selections
- **Reset**: Clicking on the currently selected root note may reset to default state or allow reselecting the same note
- **Performance**: The visualization maintains smooth 60fps rendering with efficient Three.js rendering loops

## Usability Summary

The BassFretZ Note Circle excels in transforming abstract music theory concepts into an intuitive, visual interaction. By leveraging spatial arrangement and color coding, users can quickly grasp scale patterns that traditionally require memorization. The dual notation support accommodates diverse educational backgrounds, making the tool accessible to both English-speaking musicians and those trained in fixed-do solfège systems.

Key usability strengths include:
- **Immediate Feedback**: Selection and scale visualization occur without delay, reinforcing learning through instant cause-and-effect observation
- **Reduced Cognitive Load**: The circular format naturally represents the octave cycle, eliminating the need for mental transposition
- **Error Prevention**: Clear visual distinction between root note, scale notes, and chromatic notes minimizes confusion
- **Flexibility**: Users can explore any root note and instantly compare different major scales
- **Accessibility**: Sufficient color contrast and scalable text ensure readability for users with mild visual impairments

The tool serves effectively as both a learning aid for beginners studying scale construction and a quick-reference tool for experienced musicians exploring harmonic relationships. Its focus on a single, well-executed interaction pattern—selecting a root to view its major scale—creates a powerful yet simple educational experience without unnecessary complexity.
