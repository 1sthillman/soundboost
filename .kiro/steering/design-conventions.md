---
inclusion: auto
description: STHILLMAN design skill activation rules - defines when and how design-dna, design-taste-frontend, frontend-design, and scrollcraft skills are applied. Sets Three Dials (VARIANCE=8, MOTION=7, DENSITY=4) and enforces mobile-first, anti-slop rules for all UI/UX work.
---

# STHILLMAN Design Conventions

Bu steering dosyası STHILLMAN projesi için `.agents/skills/` altındaki skill'lerin ne zaman ve nasıl devreye gireceğini tanımlar.

## Skill Locations (Context Reference)
```
.agents/skills/design-dna/SKILL.md
.agents/skills/design-taste-frontend/SKILL.md
.agents/skills/frontend-design/SKILL.md
```

**Not:** Bu skill'ler Kiro'nun built-in skill registry'sinde mevcuttur. Her UI/UX task'inde otomatik olarak aktivedir.

## Skill Hierarchy ve Aktivasyon Sırası

### 1. **design-taste-frontend** (taste-skill) - HER ZAMAN AKTİF
- **Ne zaman:** Tüm UI/UX üretimi sırasında
- **Kullanım:** STHILLMAN'ın tüm arayüzleri bu skill'in kurallarına uyar
- **Temel Kurallar:**
  - DESIGN_VARIANCE: 8 (oyun temalı cesur layout'lar)
  - MOTION_INTENSITY: 7 (sinematik CS2 teması için yüksek)
  - VISUAL_DENSITY: 4 (standart web yoğunluğu)
  - Anti-slop disiplini: Generic gradient'ler, feature-card grid'ler, AI-purple yasak
  - Eyebrow restraint: Her 3 section'da max 1 eyebrow
  - Em-dash BAN: Hiçbir yerde `—` kullanılmaz
  - Premium-consumer palette BAN uygulanmaz (bu gaming domain)
  - Hero discipline: 2 satır max headline, 20 kelime max subtext

### 2. **frontend-design** (Anthropic anti-slop) - HER ZAMAN AKTİF
- **Ne zaman:** Tüm tasarım kararları sırasında
- **Kullanım:** Distinctive, intentional visual design için
- **Temel Kurallar:**
  - Klişe tipografik muameleler yasak (tek kelime vurgusu, gereksiz ALL CAPS)
  - Motion sadece dikkat çekmek için, her card'da infinite loop değil
  - Broadsheet layout / newspaper-column / SaaS-card kit varsayılan olarak kullanılmaz
  - Copy minimalist ve işlevsel: Her kelime bir iş yapar
  - Restraint prensibini uygula: Boldness bir yerde, geri kalanı disiplinli

### 3. **design-dna** - İHTİYAÇ HALİNDE AKTİF
- **Ne zaman:**
  - Yeni tasarım sistemi oluşturulurken (Phase 2: Analyze)
  - Mevcut tasarım sisteminden yeni varyasyon üretilirken (Phase 3: Generate)
  - Tasarım tutarlılığı doğrulaması için (verify.mjs)
- **Kullanım:**
  - `DESIGN_SYSTEM.md` güncellenirken
  - Referans tasarımlardan (pear.no, CS2 screenshots) DNA çıkarılırken
  - Color measurement ile kesin hex değerleri alınırken
- **Process:**
  1. Referans görseller sağla → measure-colors.mjs ile renkleri ölç
  2. Design DNA JSON oluştur (üç boyut: system, style, effects)
  3. Generate ile implementation üret
  4. verify.mjs ile doğrula (ΔE scoring)

### 4. **scrollcraft** - SCROLL DENEYIMI İÇİN (henüz eklenmedi)
- **Ne zaman:** Ana sayfa hero ve hikaye anlatımı için scroll-driven animasyonlar
- **Kullanım:** CS2 temalı sinematik scroll deneyimi
- **Planlanan Özellikler:**
  - Scroll-timeline based animations
  - GSAP ScrollTrigger entegrasyonu
  - Sticky-stack card effects
  - Horizontal pan scroll hijacks
  - Reduced-motion fallback'ler

## Mobil Uyumluluk - BİRİNCİ ÖNCELİK
- Tüm layout'lar `< 768px`'de explicit collapse
- `min-h-[100dvh]` kullan (iOS Safari address bar için)
- Hero top padding max `pt-24`
- Navigation tek satırda (desktop), hamburger'a geçiş net
- Asymmetric layout'lar mobile'da `w-full px-4` olmalı
- Touch target'lar min 44x44px
- Form input'ları mobile keyboard ile test edilmeli

## STHILLMAN Özel Tasarım Signature
- **Tema:** Counter-Strike 2 sinematik, competitive gaming
- **Palette:** Koyu tema + neon accent (orange/electric blue), AI-purple yasak
- **Typography:** Bold sans-serif (Geist Display / Cabinet Grotesk), monospace code için Geist Mono
- **Motion:** Yoğun ama motivated: her animasyonun hikaye anlatımında yeri var
- **Layout:** Asymmetric, cesur whitespace, centered hero yasak
- **Visual Hierarchy:** Silah config'i cockpit yoğunluğu, showcase'ler art gallery airy

## Activation Workflow
1. **Her UI task'inde:** taste-skill + frontend-design otomatik aktif
2. **Tasarım sistemi değişikliğinde:** design-dna Phase 2 (analyze) çalıştır
3. **Scroll effect eklerken:** scrollcraft patterns kullan (mevcut olduğunda)
4. **Pre-flight check:** taste-skill'in 14. bölümündeki checklist'i çalıştır
5. **Verification:** design-dna verify.mjs ile color fidelity kontrol et

## Yasak Defaults (STHILLMAN için özelleşmiş)
- Inter font (Geist/Cabinet tercih)
- AI-purple gradients (CS2 için orange/blue)
- Three equal feature cards (asymmetric bento)
- Centered hero (split-screen veya left-aligned)
- Em-dash anywhere (hard ban)
- Generic "Acme" names (CS2'den gerçek silah isimleri)
- Jane Doe placeholder'lar (CS2 pro oyuncu isimleri)
- Div-based fake screenshots (gerçek CS2 screenshot'lar kullan)

## Build Discipline
- Server Components default, client island'lar sadece motion için
- Motion library: `motion/react` (formerly Framer Motion)
- GSAP sadece scroll-hijack ve pinned sections için
- No `window.addEventListener('scroll')` - Motion `useScroll()` kullan
- Dark mode default, light mode optional toggle
- Reduced-motion mandatory wrap (`MOTION_INTENSITY > 3`)
