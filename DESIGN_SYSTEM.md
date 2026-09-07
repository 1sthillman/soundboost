# STHILLMAN - CS2 Gaming Design System

## 🎨 Design Philosophy

**Competitive, Cinematic, Precision-Focused**
- Bold typography with gaming aesthetics
- High-energy cinematic animations
- CS2-inspired tactical UI elements
- 60+ FPS performance guarantee
- Accessible and competitive-ready

**Design DNA Integration:**
- Built with `.agents/skills/design-dna` methodology
- Follows `.agents/skills/design-taste-frontend` anti-slop rules
- Adheres to `.agents/skills/frontend-design` distinctive principles
- Reference: `.kiro/steering/design-system.md` for full Design DNA JSON

---

## 📐 Spacing System

**Base: 4px rhythm**

```kotlin
object Spacing {
    val xs = 8.dp      // 2x
    val sm = 12.dp     // 3x
    val md = 16.dp     // 4x - Default
    val lg = 24.dp     // 6x
    val xl = 32.dp     // 8x
    val xxl = 48.dp    // 12x
    val xxxl = 64.dp   // 16x
}
```

**Usage Rules:**
- More space **above** headings than below
- Consistent vertical rhythm
- Section spacing: `lg` or `xl`
- Item spacing: `sm` or `md`

---

## 🔤 Typography

**Text Styles:**

```kotlin
object TextStyles {
    // Display (Hero)
    val displayLarge = 64.sp
    val displayMedium = 48.sp
    
    // Titles
    val titleLarge = 32.sp
    val titleMedium = 24.sp
    val titleSmall = 20.sp
    
    // Body
    val bodyLarge = 16.sp
    val bodyMedium = 14.sp
    val bodySmall = 12.sp
    
    // Caption
    val caption = 10.sp
    
    // Letter Spacing
    val spacingTight = (-0.5).sp
    val spacingNormal = 0.sp
    val spacingWide = 1.sp
    val spacingExtraWide = 2.sp
}
```

**Font Weights:**
- `FontWeight.Black (900)` - Primary emphasis
- `FontWeight.Bold (700)` - Secondary emphasis
- `FontWeight.SemiBold (600)` - Subtle emphasis
- `FontWeight.Medium (500)` - Body text
- `FontWeight.Normal (400)` - Tertiary text

---

## 🎨 Color System

**Theme Structure:**

Each theme provides:
- `primary` - Main brand color
- `onPrimary` - Text on primary
- `secondary` - Supporting color
- `onSecondary` - Text on secondary
- `background` - Background color
- `onBackground` - Text on background
- `surface` - Card/elevated surface
- `onSurface` - Text on surface
- `surfaceElevated` - Higher elevation surfaces
- `onSurfaceVariant` - Secondary text
- `accent1` - Primary accent
- `accent2` - Secondary accent
- `accentGlow` - Glow effects
- `outline` - Borders

**Opacity Levels:**
- Disabled: 38%
- Subtle: 60%
- Secondary: 70-80%
- Primary: 90-100%

---

## 📦 Corner Radius

```kotlin
object Corners {
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val xxl = 32.dp
}
```

**Usage:**
- Small buttons/chips: `sm` (12dp)
- Cards: `md` to `lg` (16-20dp)
- Large elements: `xl` to `xxl` (24-32dp)
- Full round: `50.dp` or `CircleShape`

---

## 🌊 Elevation System

```kotlin
object Elevation {
    val level0 = 0.dp      // Flat
    val level1 = 2.dp      // Subtle lift
    val level2 = 4.dp      // Cards
    val level3 = 8.dp      // Floating elements
    val level4 = 12.dp     // Modals
    val level5 = 16.dp     // Maximum depth
}
```

**Shadows:**
- Use natural shadows with proper blur
- Increase elevation on interaction
- Audio-reactive elevation boost

---

## 🎭 Animation System

**Spring Animation:**

```kotlin
object SpringParams {
    val Default = spring<Float>(
        dampingRatio = 0.8f,
        stiffness = 400f
    )
    
    val Bouncy = spring<Float>(
        dampingRatio = 0.6f,
        stiffness = 500f
    )
    
    val Stiff = spring<Float>(
        dampingRatio = 0.9f,
        stiffness = 600f
    )
}
```

**Motion Duration:**

```kotlin
object MotionDuration {
    val instant = 0
    val fast = 150
    val normal = 250
    val slow = 350
    val verySlow = 500
}
```

**Easing:**
- `FastOutSlowInEasing` - Natural deceleration
- `LinearOutSlowInEasing` - Smooth entry
- `LinearEasing` - Constant motion

---

## 🎯 Component Patterns

### Cards

```kotlin
Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(Corners.md),
    color = themeColors.surfaceElevated,
    shadowElevation = Elevation.level2
) {
    // Content
}
```

### Buttons

**Primary:**
```kotlin
Button(
    onClick = { },
    modifier = Modifier
        .fillMaxWidth()
        .height(54.dp),
    shape = RoundedCornerShape(Corners.md),
    colors = ButtonDefaults.buttonColors(
        containerColor = themeColors.accent1
    )
)
```

**Scale Feedback:**
```kotlin
val scale by animateFloatAsState(
    targetValue = if (pressed) 0.95f else 1f,
    animationSpec = SpringParams.Default
)

Box(modifier = Modifier.scale(scale)) {
    // Content
}
```

### Interactive Elements

**Touch Feedback:**
- Scale: 0.92 - 1.0
- Alpha: 0.6 - 1.0
- Elevation: +4dp on press

**Audio-Reactive:**
- Pulse with audio intensity
- Dynamic elevation boost
- Glow effects on peaks

---

## 📱 Screen Layouts

### Standard Screen Structure

```kotlin
Scaffold(
    topBar = { /* AppBar */ },
    containerColor = Color.Transparent
) { padding ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = Spacing.md)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(Spacing.md))
        
        // Section Header
        Text(
            text = "Section Title",
            fontSize = TextStyles.titleMedium,
            fontWeight = FontWeight.Black
        )
        
        Spacer(Modifier.height(Spacing.lg))
        
        // Content
        
        Spacer(Modifier.height(Spacing.xl))
    }
}
```

---

## 🎨 Visual Effects

### Glow Effect

```kotlin
Box(
    modifier = Modifier
        .background(
            Brush.radialGradient(
                colors = listOf(
                    color.copy(alpha = 0.3f),
                    Color.Transparent
                )
            )
        )
)
```

### Gradient Overlay

```kotlin
Box(
    modifier = Modifier
        .fillMaxSize()
        .background(
            Brush.verticalGradient(
                colors = listOf(
                    startColor,
                    endColor
                )
            )
        )
)
```

### Blur Effect (Ambient)

```kotlin
Box(
    modifier = Modifier
        .blur(25.dp)
        .alpha(0.5f)
)
```

---

## 🎵 Audio-Reactive Design

### Principles

1. **Visualizer Integration**
   - Show real audio data
   - Smooth 60 FPS rendering
   - Theme-specific styles

2. **Ambient Effects**
   - Background glow based on audio
   - Particle systems on peaks
   - Dynamic elevation

3. **UI Feedback**
   - Status indicators pulse with audio
   - Theme pills animate with intensity
   - Preset buttons glow on high energy

### Audio Intensity Mapping

```kotlin
val audioIntensity = remember { Animatable(0f) }

LaunchedEffect(audioLevels, isActive) {
    if (isActive && audioLevels != null) {
        val intensity = audioLevels.average().toFloat()
        audioIntensity.animateTo(
            targetValue = intensity.coerceIn(0f, 1f),
            animationSpec = tween(50, easing = FastOutSlowInEasing)
        )
    }
}
```

**Usage:**
- Elevation: `baseElevation + (intensity * 4.dp)`
- Alpha: `0.6f + (intensity * 0.4f)`
- Scale: `1f + (intensity * 0.05f)`
- Glow: `intensity * 0.3f`

---

## ♿ Accessibility

### Touch Targets

- Minimum: 48dp x 48dp
- Recommended: 54dp height for primary actions
- Spacing between targets: 8dp minimum

### Contrast Ratios

- Normal text (14sp+): 4.5:1 minimum
- Large text (18sp+): 3:1 minimum
- Interactive elements: 3:1 minimum

### States

All interactive elements must have:
- Default state
- Hover/Focus state (desktop)
- Pressed state
- Disabled state (38% opacity)

---

## 🎯 Performance Guidelines

### 60 FPS Rules

1. **Reduce Overdraw**
   - Minimize overlapping layers
   - Use `clipToBounds` when needed
   - Avoid unnecessary backgrounds

2. **Optimize Animations**
   - Use `remember` for animated values
   - Batch state updates
   - Avoid heavy calculations in composition

3. **Efficient Rendering**
   - Reduce sample counts in visualizers
   - Simplify path calculations
   - Use hardware acceleration

### Memory

- Avoid creating new objects in draw loops
- Cache gradients and brushes
- Release resources when not needed

---

## 🚀 Implementation Checklist

### Per Screen

- [ ] Apply DesignTokens spacing
- [ ] Use proper TextStyles
- [ ] Implement spring animations
- [ ] Add scale feedback on interactions
- [ ] Consistent corner radius
- [ ] Proper elevation levels
- [ ] Audio-reactive elements (where applicable)
- [ ] Accessibility touch targets
- [ ] 60 FPS performance verified

### Per Component

- [ ] Smooth animations (250ms default)
- [ ] Natural spring physics
- [ ] Proper color opacity
- [ ] Consistent padding/margin
- [ ] Visual feedback on interaction
- [ ] Error states handled
- [ ] Loading states implemented
- [ ] Empty states designed

---

## 📚 References

### Design Skills (Active)
- **design-taste-frontend** (`.agents/skills/`) - Anti-slop frontend rules
- **frontend-design** (`.agents/skills/`) - Anthropic distinctive design skill
- **design-dna** (`.agents/skills/`) - 3-phase design extraction & generation

### Steering Files (Auto-Loaded)
- **design-conventions.md** (`.kiro/steering/`) - Skill activation rules
- **design-system.md** (`.kiro/steering/`) - Design DNA JSON tokens

### External Inspiration
- **pear.no** - Clean minimalist aesthetic (to be analyzed)
- **Counter-Strike 2 UI** - Tactical gaming patterns (to be analyzed)
- **Material Design 3** - Base Android guidelines
- **iOS Human Interface** - Animation principles
- **GSAP ScrollTrigger** - Cinematic scroll patterns

## 🚀 Implementation Status

| Component | Status | Notes |
|-----------|--------|-------|
| Spacing System | ✅ Complete | 4dp rhythm, consistent |
| Typography | ✅ Complete | Geist/Cabinet planned for web |
| Color System | 🟡 Partial | Awaiting design-dna Phase 2 analysis |
| Animation System | ✅ Complete | Spring physics, 60 FPS |
| Audio-Reactive | ✅ Complete | Visualizer integration |
| Scroll Effects | 🔜 Planned | GSAP sticky-stack, horizontal-pan |
| Design DNA JSON | 🟡 Draft | `.kiro/steering/design-system.md` |

## 🎯 Next Steps

1. **Collect reference images** (pear.no, CS2 UI screenshots)
2. **Run design-dna Phase 2** (analyze + measure colors)
3. **Populate Design DNA JSON** in `.kiro/steering/design-system.md`
4. **Generate web components** with design-dna Phase 3
5. **Verify color fidelity** with verify.mjs (ΔE < 10)
6. **Implement scroll effects** using taste-skill Section 5 patterns

---

**Version**: 3.0  
**Last Updated**: 2026-09-07  
**Status**: 🟡 Active (Android) | 🔜 Pending (Web Design DNA)
