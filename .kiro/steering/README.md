---
inclusion: manual
description: Quick reference guide for STHILLMAN steering files and design skills - workflow documentation, skill activation commands, and integration status. Use this for understanding how design-dna, taste-skill, and frontend-design work together.
---

# STHILLMAN Steering & Skills Overview

## 📁 Struktur

```
.kiro/steering/
├── design-conventions.md    # Skill aktivasyon kuralları (auto-include)
├── design-system.md         # Design DNA JSON & tokens (auto-include)
└── README.md                # Bu dosya (manual include)

.agents/skills/
├── design-dna/              # 3-phase design extraction & generation
├── design-taste-frontend/   # Anti-slop frontend rules (taste-skill)
└── frontend-design/         # Anthropic's official anti-slop skill
```

## 🎯 Quick Reference

### Her UI Task İçin Otomatik Aktif
- ✅ **design-taste-frontend** - Generic pattern'leri engeller
- ✅ **frontend-design** - Klişe tasarım kararlarını önler
- ✅ **design-conventions.md** - STHILLMAN özel kuralları uygular
- ✅ **design-system.md** - Token'ları sağlar

### İhtiyaç Halinde Çağır
- 🔧 **design-dna Phase 2** - Referans tasarımlardan DNA çıkar
- 🔧 **design-dna Phase 3** - DNA JSON'dan implementation üret
- 🔧 **design-dna verify** - Color fidelity doğrula

## 🎨 STHILLMAN Design Signature

### Dials
```
DESIGN_VARIANCE: 8    (asymmetric, bold layouts)
MOTION_INTENSITY: 7   (cinematic, high-energy)
VISUAL_DENSITY: 4     (standard web density)
```

### Core Rules
- ❌ **NO** em-dash (`—`) anywhere
- ❌ **NO** AI-purple gradients
- ❌ **NO** centered hero
- ❌ **NO** three equal feature cards
- ❌ **NO** Inter font default
- ✅ **YES** Geist/Cabinet Grotesk
- ✅ **YES** Dark mode first
- ✅ **YES** CS2-inspired orange/blue accents
- ✅ **YES** Asymmetric bento layouts
- ✅ **YES** Motivated motion only

## 📱 Mobile-First Checklist
- [ ] `min-h-[100dvh]` (not `h-screen`)
- [ ] Explicit collapse rules `< 768px`
- [ ] Touch targets min 44x44px
- [ ] Hero top padding max `pt-24`
- [ ] Navigation hamburger transition
- [ ] Asymmetric → `w-full px-4` mobile

## 🚀 Workflow

### 1. New Feature/Page
```bash
# Otomatik aktif: design-conventions.md + design-system.md
# Skill'ler zaten devrede: taste-skill + frontend-design
# Sadece code yaz, kurallar uygulanır
```

### 2. Design System Güncelleme
```bash
# 1. Referans görselleri topla (pear.no, CS2 UI)
# 2. design-dna skill'ini activate et
# 3. Phase 2 (analyze) çalıştır → DNA JSON
# 4. .kiro/steering/design-system.md güncelle
# 5. verify.mjs ile doğrula
```

### 3. Scroll Effect Ekleme
```bash
# design-taste-frontend Section 5 referansları:
# - Sticky-Stack (5.A)
# - Horizontal-Pan (5.B)
# - Scroll-Reveal Stagger (5.C)
# GSAP + Motion hybrid approach
```

### 4. Pre-Flight Check
```bash
# design-taste-frontend Section 14 checklist:
# - Zero em-dash ✓
# - Page Theme Lock ✓
# - Color Consistency ✓
# - Button Contrast ✓
# - Hero Viewport Fit ✓
# - Eyebrow Count ✓
# - Copy Self-Audit ✓
# - Motion Motivated ✓
# - 60+ checkboxes total
```

## 🛠 Skill Activation Commands

### Manual Activation (when needed)
```typescript
// Phase 2: Analyze reference design
await activateSkill('design-dna', {
  phase: 'analyze',
  references: ['pear-no-homepage.png', 'cs2-ui-screenshot.png']
});

// Phase 3: Generate from DNA
await activateSkill('design-dna', {
  phase: 'generate',
  dnaJson: './design-system.json',
  content: 'Hero section with CS2 weapon showcase'
});

// Verify color fidelity
await runVerification('implementation-screenshot.png', 'design-system.json');
```

## 📖 Skill Documentation

### design-dna
- **Phase 1:** Show schema (3 dimensions: system, style, effects)
- **Phase 2:** Analyze references → JSON (color measurement, typography, effects)
- **Phase 3:** Generate implementation from JSON
- **Verify:** ΔE color scoring, coverage drift

### design-taste-frontend (1207 lines)
- **Section 0:** Brief inference (read the room first)
- **Section 1:** Three Dials (variance, motion, density)
- **Section 2:** Design system mapping (official packages)
- **Section 4:** Design engineering directives (anti-defaults)
- **Section 5:** Context-aware proactivity (motion patterns)
- **Section 9:** AI Tells (forbidden patterns)
- **Section 14:** Pre-Flight Check (60+ checkboxes)

### frontend-design
- Ground designs in subject matter
- Typography carries personality
- Motion sparingly, deliberately
- Self-critique before shipping
- Restraint: one bold element, rest quiet

## 🎯 STHILLMAN-Specific Overrides

### Allowed Deviations from Defaults
```typescript
// Taste-skill default: serif discouraged
// STHILLMAN: Sans-serif mandatory (gaming domain)

// Taste-skill default: MOTION_INTENSITY baseline 6
// STHILLMAN: 7 (cinematic CS2 theme justified)

// Taste-skill: Premium-consumer palette ban applies
// STHILLMAN: N/A (gaming domain, not cookware)

// Frontend-design: "creative brief ≠ serif"
// STHILLMAN: ✓ Confirmed, bold sans-serif display
```

### Banned Overrides
```typescript
// These remain HARD BANS even for STHILLMAN:
- Em-dash (`—`)
- window.addEventListener('scroll')
- Div-based fake screenshots
- Hand-rolled SVG icons
- "Jane Doe" / "Acme" placeholders
- White-on-white buttons (contrast fail)
```

## 🔗 Quick Links

- **Taste-skill full doc:** `.agents/skills/design-taste-frontend/SKILL.md`
- **Frontend-design doc:** `.agents/skills/frontend-design/SKILL.md`
- **Design-DNA doc:** `.agents/skills/design-dna/SKILL.md`
- **Convention rules:** `.kiro/steering/design-conventions.md`
- **Token system:** `.kiro/steering/design-system.md`

## 📝 Status

| Component | Status | Last Updated |
|-----------|--------|--------------|
| design-conventions.md | ✅ Active | 2026-09-07 |
| design-system.md | 🟡 Draft (awaiting Phase 2) | 2026-09-07 |
| design-taste-frontend | ✅ Installed | - |
| frontend-design | ✅ Installed | - |
| design-dna | ✅ Installed | - |
| scrollcraft | ❌ Not installed yet | - |

---

**Maintainer:** STHILLMAN project  
**Contact:** #context team  
**Version:** 1.0.0
