# Pixoo-64 LED Matrix Optical, Color & Temporal Bible

The Divoom Pixoo-64 is a physical **64×64 RGB LED matrix** (4,096 individual SMD LEDs separated by plastic grid baffles behind a matte diffuser). What looks good on a 4K IPS/OLED monitor often looks muddy, washed out, or flickery on a physical LED matrix. Follow these hardware-grounded laws for every image or animation.

---

## 1. Physical LED Optics & Color Laws

### 1.1 The True-Black (`#000000`) LED-Off Law
- On the Pixoo-64, **`#000000` (`RGB(0, 0, 0)`) physically powers the LED off**.
- Even a tiny non-zero value like `RGB(5, 5, 8)` or `#08080C` forces the LED driver to fire at its minimum PWM duty cycle, creating a visible gray glow inside that cell and bleeding light into adjacent cells.
- **Rule**: Always clamp dark/shadow background pixels (`max(R, G, B) <= 18`) to exact `#000000`. Aim for **25%–65% True-Black (`#000000`) coverage** so the subject floats seamlessly inside the physical black bezel.

### 1.2 Diffuser Saturation Boost & Anti-Muddiness
- Because light passes through a matte diffuser, low-saturation colors (`HSV Saturation < 0.30`) and mid-tone grays/browns lose their hue identity and appear chalky.
- **Rule**: Keep foreground and accent colors at **HSV Saturation `>= 0.65`**. Replace desaturated shading with **hue-shifted shading** (e.g., shade yellow with warm orange/magenta, shade cyan with deep royal blue/indigo).

### 1.3 Cohesive Pixel-Art Palette Size
- The best-looking 64×64 animations use **8 to 32 effective colors** total.
- When downscaling AI-generated images (from Gemini Nano Banana), quantize to a locked palette (`12–24` colors) and lock the palette across all frames of the GIF so colors never shimmer between frames.

### 1.4 Controlled Neon Bloom (3-Step Ramp)
- Do not use smooth 10-pixel Gaussian blurs to simulate glow—on a 64×64 matrix, wide blurs turn into dim muddy smudges.
- Instead, use a **discrete 1–2px pixel-art halo**:
  1. **Core**: High luminance (`L > 220`, often pure white `#FFFFFF` or bright yellow/cyan).
  2. **1px Halo**: Saturated mid-luminance (`L ~ 100–140`, `S > 0.85`).
  3. **Outer Cutoff**: Hard step directly to `#000000` (or deep dark background).

---

## 2. Spatial Composition & 1px Crispness Laws

### 2.1 Zero Anti-Aliasing Blur
- Standard bilinear or bicubic downscaling to 64×64 averages boundary pixels, producing blurry intermediate colors around every silhouette.
- **Rule**: Downscale using **nearest-neighbor sampling** after palette quantization, and strip isolated 1px compression noise.

### 2.2 Silhouette & Bezel Framing
- At 64×64 resolution, details smaller than 2×2 pixels are read as texture/particles rather than shapes.
- Keep the primary subject bold, centered in the inner `48×48` zone, and keep the outer `2px` border predominantly dark (`#000000`) unless the scene is an intentional full-bleed horizon/landscape.

---

## 3. Temporal Pacing, Motion & Hardware Buffer Laws

### 3.1 The 32-Frame Pixoo-64 Hardware Ceiling
- The Pixoo-64 `Draw/SendHttpGif` buffer reliably plays **30–32 frames maximum** before looping early or dropping frames.
- **Rule**: Keep all custom animated GIFs between **4 and 28 frames** (`8–16` frames is the sweet spot).

### 3.2 Frame Delay Rhythm (8–12 FPS + Keyframe Holds)
- Ultra-high frame rates (`> 20 FPS`) on a 64×64 grid cause sub-pixel crawl and require too many frames.
- **Sweet Spot**: `80ms – 125ms` per frame (`8–12 FPS`).
- **Variable Holds**: Hold rest poses, apex poses, or impact frames for `180ms – 350ms` to give the animation weight and personality.

### 3.3 Static Background Locking (Anti-Flicker)
- If 100% of the 4,096 pixels change every frame (common in raw AI video-to-GIF conversions), the LED matrix exhibits distracting background boil.
- **Rule**: Keep **60%–85% of the canvas 100% invariant** across frames. Restrict motion to the character sprite or localized secondary motion (`3%–18%` active changed pixels per frame, such as falling rain, rising steam, flickering neon, or bobbing idle motion).
