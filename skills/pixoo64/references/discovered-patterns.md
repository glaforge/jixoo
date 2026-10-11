# Pixoo-64 Animation & Design Patterns

Empirical analysis and hardware-tested guidelines for creating animations and artwork for the Divoom Pixoo-64.

---

## 1. Executive Summary & Hardware Findings

- **True-Black (#000000) Coverage**: Top-scoring animations on the Pixoo-64 average **20%–60% True-Black (`#000000`) pixels**. Because `#000000` physically turns the LED off, generous dark space prevents optical light bleed through the diffuser grid and blends the artwork seamlessly into the physical black bezel.
- **Palette Economy**: The most readable animations use **8 to 24 distinct colors** with high foreground HSV saturation (avg `> 0.60`).
- **Optimal Temporal Pacing**: Animated loops perform best at **6 to 12 FPS** (delays of **80ms – 160ms**) with **<= 32 frames** total to respect the ESP32 hardware buffer limit.
- **Static Background Anchoring**: High-quality loops keep **50%–90% of the canvas static across frames**. Animating 100% of pixels causes visual fatigue and background flicker; localized secondary motion (falling rain, rising steam, blinking eyes, subtle breathing) creates crisp, professional pixel art.

---

## 2. Core Physical & Temporal Laws

See [pixoo64-led-bible.md](pixoo64-led-bible.md) for the complete optical, color, and temporal hardware rules:
1. **True-Black LED-Off Law**: Clamp dark/background pixels (`max(R, G, B) <= 15`) to `#000000` (`--led-optimize`).
2. **Diffuser Saturation Boost**: Keep foreground accent HSV saturation `>= 0.65` and use hue-shifted shading instead of desaturated grays/browns.
3. **Nearest-Neighbor Sharpness**: Always downscale pixel art with nearest-neighbor sampling (`--nearest`).
4. **Controlled Neon Bloom**: Use a discrete 3-step ramp (`L > 220` core -> `L ~ 100–140` 1px halo -> `#000000` cutoff).
5. **The 32-Frame Ceiling & Keyframe Holds**: Keep custom HTTP GIFs between `4` and `28` frames (`8–12 FPS`, `80–125ms` per frame) with `180–350ms` holds on rest/impact keyframes.
6. **Seamless Loop Closure**: Ensure the final frame transitions smoothly back to Frame 0 using modular phase math, ping-pong oscillation, or looping particle resets.

---

## 3. Patterns by Visual Archetype

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
