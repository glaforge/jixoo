# Nano Banana (`gemini-3.1-flash-image`) Prompt Engineering for Pixoo-64

When generating assets for a 64×64 LED matrix using **Nano Banana 2 (`models/gemini-3.1-flash-image`)** via the Gemini API, raw prompts like *"a pixel art cat"* often produce high-resolution pseudo-pixel illustrations with soft gradients, noisy backgrounds, and inconsistent pixel sizes.

Use the prompt blueprints below to obtain authentic, LED-ready pixel art.

---

## Key Principles for LED Pixel Art Prompts

1. **Explicit 64×64 Pixel Art Aesthetic**: Always specify *"Strict authentic 64x64 pixel art style, crisp 1-pixel edges, zero anti-aliasing, zero soft gradients"*.
2. **True Black Background**: Mandate *"Pure solid flat black (#000000) background"*. On the physical Pixoo-64, `#000000` powers the LED completely off, giving infinite contrast and preventing backlight glow.
3. **High Foreground Saturation**: LEDs lose vibrancy through the diffuser if colors are desaturated. Specify *"Vibrant, high-saturation arcade/neon palette with bold outlines"*.
4. **Avoid Tiny Micro-Details**: Details smaller than 2×2 pixels turn into unreadable visual noise at 64×64. Instruct the model: *"Bold readable silhouettes, no micro-details, strong focal subject"*.

---

## Prompt Blueprints by Archetype

### 1. Sprite Grid (2×2 or 3×3 Animation Sprite Sheet)

**Best for**: Mascot idle loops, walking/running cycles, spinning objects, character emotes.

```text
A 2x2 sprite sheet grid showing 4 sequential animation frames of [SUBJECT_AND_ACTION], arranged in reading order (top-left frame 1, top-right frame 2, bottom-left frame 3, bottom-right frame 4).
Strict authentic 16-bit 64x64 pixel art style.
The background of the entire image and every cell MUST be pure solid flat black (#000000) with NO grid lines, NO borders, NO text labels, and NO drop shadows.
Keep the character/object exactly the same size, palette, and centered position in each of the 4 quadrants so the frames form a smooth seamless animation loop.
Use high-contrast, hyper-saturated LED colors ([PALETTE_HINTS]), crisp 1-pixel edges, zero anti-aliasing, and zero soft gradients.
```

*Why it works*: Generating all 4 frames in a single square image forces Gemini to preserve identical character proportions, lighting, and palette across frames.

---

### 2. Scenic Lo-Fi / Cyberpunk Scene

**Best for**: Desk clock backdrops, atmospheric environments, cozy interiors, space vistas.

```text
A strict 64x64 pixel art scene of [SCENE_DESCRIPTION].
Designed specifically for a 64x64 physical RGB LED matrix: use deep pure #000000 black for the sky/shadows/background (at least 35% of canvas), bold readable silhouettes, crisp 1px edges, zero blur, and punchy high-saturation neon/arcade accents ([PALETTE_HINTS]).
Do not include tiny unreadable details.
```

---

### 3. Widget & HUD Icons

**Best for**: Weather widgets, crypto/stock trackers, game stats, alerts.

```text
A bold 64x64 pixel art icon representing [METRIC_OR_EVENT].
Minimalist high-contrast design: centered iconography within the inner 48x48 zone, surrounded by pure solid #000000 black.
Crisp 1px borders, high-saturation LED colors (cyan, magenta, yellow, or bright green), zero anti-aliased smoothing, sharp geometry.
```

---

### 4. Multi-Turn Sequential Keyframes

**Best for**: Multi-step animations with camera persistence.

**Turn 1 Prompt (Base Keyframe)**:
```text
Strict 64x64 pixel art of [SUBJECT_FRAME_1].
Pure solid #000000 black background, crisp 1px pixel clusters, limited 16-color vibrant arcade palette, high contrast for an LED matrix display, no anti-aliasing, no soft gradients.
```

**Turn 2..N Prompt (Continuing Interaction)**:
```text
Generate the next sequential animation frame (Frame [K] of [N]): [DESCRIBE_SMALL_INCREMENTAL_MOTION].
CRITICAL: Keep the background, lighting, pixel scale, and all static elements 100% identical to the previous frame. Only move the active animated elements by a small step. Keep pure #000000 black background.
```
