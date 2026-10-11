# Pixoo-64 Animation & Design Patterns

Empirical analysis and hardware-tested guidelines for creating animations and artwork for the Divoom Pixoo-64.

---

## 1. Executive Summary & Hardware Findings

- **True-Black (#000000) Coverage**: Top-scoring animations on the Pixoo-64 average **20%–60% True-Black (`#000000`) pixels**. Because `#000000` physically turns the LED off, generous dark space prevents optical light bleed through the diffuser grid and blends the artwork seamlessly into the physical black bezel.
- **Palette Economy**: The most readable animations use **8 to 24 distinct colors** with high foreground HSV saturation (avg `> 0.60`).
- **Optimal Temporal Pacing**: Animated loops perform best at **6 to 12 FPS** (delays of **80ms – 160ms**) with **<= 32 frames** total to respect the ESP32 hardware buffer limit.
- **Static Background Anchoring**: High-quality loops keep **50%–90% of the canvas static across frames**. Animating 100% of pixels causes visual fatigue and background flicker; localized secondary motion (falling rain, rising steam, blinking eyes, subtle breathing) creates crisp, professional pixel art.

---

## 2. Core Physical LED Laws

1. **TRUE-BLACK LED-OFF LAW**: Always clamp dark/background pixels (`max(R, G, B) <= 15`) to exact `#000000`. On the Pixoo-64, `#000000` physically powers off the LED; `RGB(8, 8, 12)` turns the LED on at minimum PWM duty cycle, producing a cloudy gray haze and potential coil whine.
2. **DIFFUSER SATURATION BOOST**: Physical LED cells sit behind a matte plastic grid diffuser. Keep foreground accent saturation `>= 0.65` (HSV) and avoid desaturated mid-tone grays or muddy browns.
3. **NEAREST-NEIGHBOR SHARPNESS**: Never downscale pixel art to 64×64 with bilinear interpolation without crisp nearest-neighbor sampling. Blended boundary pixels create dim, muddy halos on a physical matrix.
4. **CONTROLLED NEON BLOOM**: When simulating glowing neon or fire, use a 3-step ramp: bright core (`L > 220`) -> 1px saturated halo (`L ~ 110`) -> hard drop to `#000000`.

---

## 3. Temporal Pacing & Motion Laws

1. **THE 32-FRAME HARDWARE CEILING**: The Pixoo-64 HTTP GIF buffer reliably plays up to 30–32 frames. Keep custom animations between 4 and 28 frames (`8–16` frames is the sweet spot).
2. **KEYFRAME RHYTHM & HOLDS**: 8–12 FPS (80–125ms per frame) is the sweet spot for pixel motion, with 200–400ms holds on rest/impact keyframes.
3. **SEAMLESS LOOP CLOSURE**: Ensure the final frame transitions smoothly back to Frame 0 using modular phase math, ping-pong oscillation, or looping particle resets.

---

## 4. Patterns by Visual Archetype

### Archetype 1: `scenic-loop` (Environments, Cityscapes, Nature)
- **Ideal Frame Count**: `8 – 24` frames
- **Ideal Frame Delay**: `90 – 160 ms` (~`6 – 11 FPS`)
- **Effective Palette Size**: `10 – 32` colors
- **True-Black (`#000000`) Coverage**: `25% – 50%`
- **Static Background Lock**: `65% – 90%` invariant pixels
- **Top Techniques**: Static background scenery with localized particle effects (falling snow, raindrops, neon glow pulse, chimney smoke).

### Archetype 2: `character-sprite` (Characters, Mascots, Creatures)
- **Ideal Frame Count**: `4 – 16` frames (e.g. 2×2 or 3×3 sprite sheet)
- **Ideal Frame Delay**: `75 – 125 ms` (~`8 – 12.5 FPS`)
- **Effective Palette Size**: `4 – 16` colors
- **True-Black (`#000000`) Coverage**: `60% – 90%`
- **Static Background Lock**: `80% – 95%`
- **Top Techniques**: Centered character in the inner 48×48 zone, high-contrast dark background, subtle 1px vertical idle breathing bob, walk cycle, or facial expression change.

### Archetype 3: `abstract-shader` (Retro Grids, Hypnotic Patterns, Visualizers)
- **Ideal Frame Count**: `8 – 24` frames
- **Ideal Frame Delay**: `60 – 120 ms` (~`8 – 16 FPS`)
- **Effective Palette Size**: `6 – 16` vibrant colors (Synthwave, Cyberpunk, Arcade)
- **Top Techniques**: Symmetrical geometric movement, perspective grid scrolling, pulsating neon rings, clean cycling color ramps.

### Archetype 4: `widget-hud` (Clocks, Status Monitors, Trackers)
- **Ideal Frame Count**: `1` (static) or `4 – 12` frames (subtle ambient pulse)
- **Ideal Frame Delay**: `100 – 250 ms`
- **Effective Palette Size**: `4 – 12` colors
- **True-Black (`#000000`) Coverage**: `30% – 70%`
- **Top Techniques**: High readability, bold contrast, minimal distraction, clear data compartmentalization.

### Archetype 5: `typography-marquee` (Text, Quotes, Greetings)
- **Ideal Frame Count**: `4 – 16` frames
- **Ideal Frame Delay**: `80 – 150 ms`
- **Effective Palette Size**: `2 – 6` high-contrast colors
- **True-Black (`#000000`) Coverage**: `70% – 90%`
- **Top Techniques**: 1px crisp font glyphs, smooth horizontal scroll or gentle color breathing, zero blurry drop shadows.
