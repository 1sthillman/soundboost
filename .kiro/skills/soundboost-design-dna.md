---
name: soundboost-design-dna
description: Sound'ST Boost visual identity DNA - tokens, style essence, and interaction feel that make this audio app unmistakable
---

# Sound'ST Boost Design DNA

## DESIGN TOKENS (Measurable)

### Color System
```kotlin
// Premium Dark Foundation
DeepBlack = #0A0A12       // Primary background, infinite depth
PanelDark = #15151F        // Surface base
PanelElevated = #1C1C28    // Elevated cards
SurfaceDark = #1F1F35      // Interactive surfaces
SurfaceElevated = #252540   // Highest elevation

// Neon Accents (Theme-specific primaries)
NeonCyan = #00F0FF → Glow: #00FFFF
NeonPink = #FF2DAA → Glow: #FF4DC4  
NeonPurple = #9D4DFF → Glow: #B76DFF
NeonGreen = #39FF88 → Glow: #5AFFA5

// Text Hierarchy
TextPrimary = #F5F5FF      // Headlines, active states
TextSecondary = #A0A0C0    // Body, labels
TextTertiary = #707090     // Hints, disabled states
```

### Typography Scale
```
Display: 64sp / Black / -0.5sp tracking
Headline: 28sp / Black / +0.5sp tracking  
Title: 18sp / Bold / +0.15sp tracking
Body: 16sp / Normal / +0.5sp tracking
Label: 14sp / Bold / +0.8sp tracking (ALL CAPS for status)
Micro: 10sp / Medium / +0.5sp tracking
```

### Spacing Rhythm
```
Base: 4dp
Tight: 8dp (related elements)
Default: 16dp (component spacing)
Loose: 24dp (section breathing)
Section: 32dp (major divisions)
Hero: 48dp (feature spacing)

Rule: More space ABOVE headings than below
Rule: Fluid scaling - mobile gets 0.7x desktop air
```

### Shape Language
```
Subtle: 16dp (buttons, inputs)
Medium: 20-24dp (cards, surfaces)  
Prominent: 28-32dp (hero elements)
Full: CircleShape (theme pickers, indicators)
```

### Elevation & Depth
```
Level 0: background (0dp)
Level 1: surface (2dp) - resting cards
Level 2: surfaceElevated (4dp) - active cards
Level 3: floating (8dp) - dialogs, sheets
Level 4: overlay (16dp) - tooltips, snackbars

Shadow: Offset + blur, not pure drop
Edge: Subtle top-light on dark surfaces
Grain: 2% noise overlay for depth
```

### Motion Timing
```
Instant: 100ms (micro-feedback)
Fast: 250ms (transitions)
Medium: 400ms (reveals, entrances)
Slow: 600ms (hero animations)
Breathing: 2000ms (idle pulsing)

Easing: FastOutSlowIn (standard)
Bounce: Spring(0.8, 400) (playful)
Smooth: Tween.EaseInOut (visualizer)
```

## DESIGN STYLE (Qualitative)

### Mood
**Premium • Energetic • Technical • Confident**

Not: Corporate, playful, minimal, medical
Yes: Club lighting, pro audio gear, refined power

### Visual Language
- **Neon-on-black** as primary expression
- **Circular forms** dominate (volume dial is hero)
- **Glow effects** signal active state
- **Gradient backgrounds** add depth without noise
- **Sharp typography** in all caps for labels
- **Glassmorphic surfaces** float above background

### Composition
- **Center-weighted** - main control is focal point
- **Breathing room** - elements never crowd
- **Rhythm in spacing** - deliberate pause before major sections
- **Theme switcher as necklace** - horizontal scroll of circles at top
- **Preset buttons in a row** - equal weight, unified treatment

### Interaction Feel
- **Double-tap to activate** - intentional, not accidental
- **Drag to adjust** - continuous, fluid control
- **Haptic peaks** - physical feedback at key moments
- **Visualizer breathes** - idle animation, not static
- **Smooth state transitions** - no jarring cuts
- **Glow intensifies** when active

### Brand Voice
Confident without aggression. Technical precision meets visceral feeling. "Boost active" not "Your sound has been enhanced". Short labels, clear hierarchy, no filler.

## VISUAL EFFECTS (Experience)

### Audio Visualizer
```kotlin
// Reactive bars driven by real FFT data
- 32 bars, mirrored symmetry
- Height: 0-120dp, scales with audio intensity
- Color: accent1 → accent2 gradient
- Animation: 60fps, smooth interpolation
- Idle state: subtle breathing at 0.3 amplitude
- Peak hold: 150ms delay before decay
```

### Volume Dial Glow
```kotlin
// Radial gradient pulses when active
centerColor = accent1.copy(alpha = 0.4f)
edgeColor = accent1.copy(alpha = 0f)
radius = 150.dp to 200.dp (animated)
duration = 2000ms, infinite repeat
```

### Theme Transition
```kotlin
// Background gradient crossfades, not cuts
animateColorAsState(
    targetValue = newTheme.background,
    animationSpec = tween(600, easing = FastOutSlowIn)
)
```

### Preset Button Activation
```kotlin
// Scale + glow + shadow on press
scale: 1.0 → 0.95 → 1.08 → 1.0
glow: 0 → accent1 at 0.6 alpha
shadow: elevation 2dp → 8dp
duration: 400ms total
```

### Scroll-Driven Effects
```kotlin
// Top bar background opacity fades in on scroll
alpha = (scrollOffset / 100.dp).coerceIn(0f, 1f)
backgroundColor = themeColors.background.copy(alpha = alpha)
```

### Glassmorphism
```kotlin
// Cards have subtle blur + border
background = themeColors.surfaceElevated.copy(alpha = 0.9f)
border = 1.dp, color = Color.White.copy(alpha = 0.1f)
blur = 10.dp (via RenderEffect on Android 12+)
```

## FINGERPRINT

This app is unmistakable because:
1. **Circular volume dial** - not sliders, not cards
2. **Neon glow** that intensifies with activation
3. **Symmetrical visualizer** - mirrored, centered
4. **Theme picker as jewelry** - circular buttons in a row
5. **Dark-first** - light themes are alternate, not primary
6. **Technical precision** - exact percentages, dB values shown
7. **Double-tap activation** - deliberate engagement
8. **All-caps labels** - status text screams confidence

If two screens could swap and feel the same, one is wrong.
