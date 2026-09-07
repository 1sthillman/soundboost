---
inclusion: auto
description: STHILLMAN Design DNA JSON schema and tokens - defines 3-dimension design structure (system, style, effects) extracted using design-dna skill. Contains color measurement schema, typography tokens, spacing system, and CS2-themed visual effects configuration.
---

# STHILLMAN Design System (Design DNA)

Bu dosya STHILLMAN projesinin resmi tasarım token'ı ve DNA'sını içerir. **design-dna** skill'i kullanılarak pear.no ve CS2 ekran görüntülerinden çıkarılacak.

## Mevcut Durum
Bu design system henüz tamamlanmamıştır. Aşağıdaki adımları takip ederek doldurulmalıdır:

### Phase 1: Measurement (Öncelik)
1. **Referans Görselleri Topla:**
   - pear.no ana sayfa screenshot'ı
   - CS2 UI screenshot'ları (HUD, menü, weapon inspect)
   - STHILLMAN için var olan brand asset'ler

2. **Color Measurement Çalıştır:**
   ```bash
   # design-dna skill'inden measure-colors.mjs kullan
   SKILL_ROOT=$(dirname $(find .agents/skills -name "measure-colors.mjs" -type f))
   node "$SKILL_ROOT/scripts/measure-colors.mjs" <reference-image.png> > measured-colors.json
   ```

3. **Ölçüm Sonuçları Buraya Entegre Et:**
   - Measured palette → `design_system.color`
   - Typography inference → `design_system.typography`
   - Layout patterns → `design_system.layout`

### Phase 2: Design DNA JSON (Şema)
```json
{
  "project": "STHILLMAN",
  "version": "1.0.0",
  "extracted_from": [
    "pear.no homepage",
    "Counter-Strike 2 UI screenshots",
    "STHILLMAN brand guidelines"
  ],
  
  "design_system": {
    "color": {
      "measured_palette": {
        "background": null,
        "text": null,
        "accent": null,
        "palette": [],
        "coverage": []
      },
      "measurement": {
        "k_clusters": 8,
        "min_coverage_threshold": 0.01
      },
      "primary": {
        "hex": null,
        "role": "CS2 orange/gold brand color"
      },
      "secondary": {
        "hex": null,
        "role": "Electric blue accent for competitive feel"
      },
      "accent": {
        "hex": null,
        "role": "CTA / highlight"
      },
      "neutral": {
        "scale": [],
        "usage": "zinc-950 dark base → zinc-100 light text"
      },
      "semantic": {
        "success": null,
        "warning": null,
        "error": null,
        "info": null
      },
      "dark_mode_default": true
    },
    
    "typography": {
      "sans": {
        "family": "Geist, Cabinet Grotesk, system-ui",
        "weights": [400, 500, 600, 700, 800],
        "usage": "Headlines, body, UI"
      },
      "mono": {
        "family": "Geist Mono, JetBrains Mono, monospace",
        "weights": [400, 500, 600],
        "usage": "Code blocks, weapon stats, config displays"
      },
      "display": {
        "family": "Geist Display, Cabinet Grotesk Display",
        "weights": [700, 800, 900],
        "usage": "Hero headlines only"
      },
      "scale": {
        "xs": "0.75rem",
        "sm": "0.875rem",
        "base": "1rem",
        "lg": "1.125rem",
        "xl": "1.25rem",
        "2xl": "1.5rem",
        "3xl": "1.875rem",
        "4xl": "2.25rem",
        "5xl": "3rem",
        "6xl": "3.75rem",
        "7xl": "4.5rem"
      },
      "line_height": {
        "tight": "1.1",
        "snug": "1.25",
        "normal": "1.5",
        "relaxed": "1.625"
      },
      "tracking": {
        "tighter": "-0.05em",
        "tight": "-0.025em",
        "normal": "0em",
        "wide": "0.05em",
        "wider": "0.1em"
      }
    },
    
    "spacing": {
      "unit": "0.25rem",
      "scale": [0, 1, 2, 3, 4, 5, 6, 8, 10, 12, 16, 20, 24, 32, 40, 48, 64],
      "section_gaps": {
        "tight": "py-8",
        "default": "py-16",
        "relaxed": "py-24",
        "hero": "py-32"
      },
      "density": "standard (VISUAL_DENSITY: 4)"
    },
    
    "layout": {
      "max_width": "1400px",
      "breakpoints": {
        "sm": "640px",
        "md": "768px",
        "lg": "1024px",
        "xl": "1280px",
        "2xl": "1536px"
      },
      "grid": {
        "columns": 12,
        "gap": "1.5rem",
        "asymmetric": true
      },
      "container_padding": {
        "mobile": "1rem",
        "tablet": "2rem",
        "desktop": "3rem"
      }
    },
    
    "shape": {
      "border_radius": {
        "none": "0px",
        "sm": "4px",
        "default": "8px",
        "md": "12px",
        "lg": "16px",
        "xl": "24px",
        "full": "9999px"
      },
      "primary_radius": "12px",
      "button_radius": "8px",
      "card_radius": "16px",
      "borders": "1px solid, tinted to section background"
    },
    
    "elevation": {
      "shadows": [
        "none",
        "0 1px 2px rgba(0,0,0,0.05)",
        "0 4px 6px rgba(0,0,0,0.1)",
        "0 10px 15px rgba(0,0,0,0.1)",
        "0 20px 25px rgba(0,0,0,0.15)"
      ],
      "approach": "Minimal, tinted shadows, no pure black"
    },
    
    "motion": {
      "intensity": 7,
      "easing": {
        "standard": "cubic-bezier(0.16, 1, 0.3, 1)",
        "spring": "type: spring, stiffness: 100, damping: 20",
        "emphasized": "cubic-bezier(0.4, 0, 0.2, 1)"
      },
      "duration": {
        "fast": "150ms",
        "base": "300ms",
        "slow": "500ms"
      },
      "reduced_motion_support": true
    }
  },
  
  "design_style": {
    "mood": "Competitive, cinematic, high-energy, precision-focused",
    "personality": "Bold, technical, gaming-native, premium esports",
    "visual_language": "Dark tactical UI meets gaming luxury",
    "composition_strategy": "Asymmetric, high variance, cockpit density for data, art gallery for showcase",
    "whitespace_philosophy": "Strategic negative space, not minimalist emptiness",
    "ornamentation": "Functional graphics only, no decoration for decoration's sake",
    "genre": "Gaming/Esports landing page + SaaS dashboard hybrid",
    "tone": "Professional competitive gaming, not casual streamer aesthetic",
    "hierarchy_approach": "Weight + color + motion, not just scale",
    "contrast_preference": "High contrast for competitive UI, softer for marketing sections"
  },
  
  "visual_effects": {
    "enabled": true,
    "overview": {
      "effect_intensity": "medium-high (7/10)",
      "performance_tier": "medium (target 60fps desktop, 30fps mobile)",
      "accessibility_fallbacks": "Reduced motion mandatory, solid backgrounds for transparency fallback"
    },
    "scroll_effects": {
      "enabled": true,
      "parallax": true,
      "sticky_sections": true,
      "horizontal_scroll": false,
      "scroll_linked_animations": true,
      "implementation": "GSAP ScrollTrigger + Motion useScroll"
    },
    "canvas_2d": {
      "enabled": false,
      "use_cases": []
    },
    "webgl_three": {
      "enabled": false,
      "scenes": []
    },
    "particles": {
      "enabled": false
    },
    "shaders": {
      "enabled": false
    },
    "svg_animation": {
      "enabled": true,
      "use_cases": ["Logo animation on load", "Icon micro-interactions"]
    },
    "css_advanced": {
      "backdrop_filter": true,
      "gradient_animation": true,
      "clip_path": true,
      "mix_blend_mode": true,
      "filter_effects": true
    },
    "glassmorphism": {
      "enabled": true,
      "approach": "Web approximation, not Apple Liquid Glass",
      "fallback": "Solid fill for prefers-reduced-transparency"
    },
    "cursor_effects": {
      "enabled": false,
      "note": "Custom cursors banned per taste-skill"
    },
    "composite_notes": "Glowing weapon highlights on hover, CS2-inspired tactical overlays, scroll-reveal for weapon showcase cards"
  }
}
```

### Phase 3: Usage Notes

#### Renk Kullanımı
- **Dark mode default:** zinc-950 background, zinc-100/white text
- **Accent:** CS2 orange/gold primary CTA'larda, electric blue secondary vurgularda
- **AI-purple yasak:** Generic gradient'ler yerine brand-specific CS2 palette
- **Tinted shadows:** Saf siyah yerine background'a uyumlu tint'li shadow'lar

#### Typography Hierarchy
```css
/* Hero Headline */
.hero-headline {
  font-family: var(--font-display);
  font-size: clamp(3rem, 8vw, 4.5rem);
  font-weight: 800;
  line-height: 1.1;
  letter-spacing: -0.025em;
  max-width: 20ch; /* 2 satır max */
}

/* Section Heading */
.section-heading {
  font-family: var(--font-sans);
  font-size: clamp(1.875rem, 4vw, 3rem);
  font-weight: 700;
  line-height: 1.25;
  letter-spacing: -0.025em;
}

/* Body Copy */
.body-copy {
  font-family: var(--font-sans);
  font-size: 1rem;
  font-weight: 400;
  line-height: 1.625;
  max-width: 65ch;
}

/* Mono Stats */
.weapon-stats {
  font-family: var(--font-mono);
  font-size: 0.875rem;
  font-weight: 500;
  line-height: 1.5;
  letter-spacing: 0.05em;
}
```

#### Motion Patterns
```typescript
// Hero entrance (Motion)
const heroVariants = {
  hidden: { opacity: 0, y: 24 },
  visible: { 
    opacity: 1, 
    y: 0,
    transition: {
      duration: 0.6,
      ease: [0.16, 1, 0.3, 1]
    }
  }
};

// Scroll reveal stagger
const staggerChildren = {
  visible: {
    transition: {
      staggerChildren: 0.06
    }
  }
};

// GSAP sticky stack (scrollcraft pattern)
ScrollTrigger.create({
  trigger: card,
  start: "top top",
  pin: true,
  scrub: true
});
```

#### Layout Grid
```css
/* Main container */
.container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 1rem;
}

@media (min-width: 768px) {
  .container { padding: 0 2rem; }
}

@media (min-width: 1024px) {
  .container { padding: 0 3rem; }
}

/* Asymmetric bento */
.bento-grid {
  display: grid;
  grid-template-columns: repeat(12, 1fr);
  gap: 1.5rem;
}

.bento-hero {
  grid-column: span 8;
}

.bento-aside {
  grid-column: span 4;
}
```

## Verification Checklist
- [ ] Measured colors match ΔE < 10 (design-dna verify.mjs)
- [ ] Typography scale tested at all breakpoints
- [ ] Dark mode contrast passes WCAG AA (4.5:1 body, 3:1 large text)
- [ ] Motion respects prefers-reduced-motion
- [ ] Mobile collapse explicit for all asymmetric layouts
- [ ] No em-dash (`—`) anywhere in the system
- [ ] Glassmorphism has solid fallback
- [ ] Hero fits viewport (2 lines headline, 20 words subtext max)

## Next Steps
1. **Collect reference images** (pear.no, CS2 UI)
2. **Run design-dna Phase 2** (analyze + measure)
3. **Populate this JSON** with measured values
4. **Generate implementations** with design-dna Phase 3
5. **Verify fidelity** with verify.mjs
6. **Lock tokens** for production use

---

**Status:** DRAFT - Awaiting reference analysis
**Owner:** STHILLMAN project
**Last Updated:** 2026-09-07
