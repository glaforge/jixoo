# AI Generation Guide for Pixoo 64

You can generate images and videos using Google's Gemini AI models and display them on the Pixoo 64.
Generating content requires the `GEMINI_API_KEY` environment variable.

---

## 1. Generating Static Images & Sprites (Gemini Nano Banana)

The Pixoo 64 requires a 1:1 (square) aspect ratio. You can use the Gemini Image API (`models/gemini-3.1-flash-image`, also known as Nano Banana 2) to generate these images.

### Prompt Best Practices for Physical LED Matrices

When prompting Gemini for Pixoo 64 artwork, always enforce these hardware rules:
- **Pixel Art Discipline**: Specify *"Strict authentic 64x64 pixel art style, crisp 1-pixel edges, zero anti-aliasing, zero soft gradients"*.
- **True Black Background**: Mandate *"Pure solid flat black (#000000) background"*. On the physical Pixoo 64, `#000000` physically turns the LEDs off, ensuring infinite contrast without diffuser bleed.
- **Vibrant Saturation**: The matte plastic diffuser washes out muted colors. Demand *"Hyper-saturated arcade/neon colors, high contrast"*.
- **No Micro-Details**: Details smaller than 2×2 pixels turn into indistinct noise on a 64×64 display.

### Example Request:
```bash
curl -s -X POST "https://generativelanguage.googleapis.com/v1beta/interactions?key=$GEMINI_API_KEY" \
-H 'Content-Type: application/json' \
-d '{
  "model": "models/gemini-3.1-flash-image",
  "input": "Strict 64x64 pixel art of a cyberpunk ramen shop at night. Pure solid #000000 black background and deep shadows, crisp 1-pixel edges, no blurry anti-aliasing, vibrant neon pink and cyan signs, bold silhouettes for an LED matrix.",
  "response_format": {
    "type": "image",
    "aspect_ratio": "1:1"
  }
}'
```
*Note: Extract the Base64 output from the response and save it as `image.png`.*

### Display it:
```bash
pixoo-cli image image.png
```

For advanced prompt templates including **2×2 sprite sheets**, **multi-turn animations**, and **archetype prompts**, see:
[nano-banana-prompts.md](nano-banana-prompts.md)

---

## 2. Generating Video Animations (Gemini Omni)

Gemini Omni (`models/gemini-omni-flash-preview`) can generate full videos based on a text prompt. 
Since Omni outputs standard `16:9` or `9:16` aspect ratios, center-crop the video to 1:1 and then scale it down to 64x64 to avoid letterboxing on the Pixoo display.

### Example Request:
```bash
curl -s -X POST "https://generativelanguage.googleapis.com/v1beta/interactions?key=$GEMINI_API_KEY" \
-H "Content-Type: application/json" \
-d '{
  "model": "models/gemini-omni-flash-preview",
  "input": "A head close-up of a cute comic dragon character throwing flames at us, clean high contrast pixel-friendly outlines, dark background.",
  "response_format": {
    "type": "video",
    "aspect_ratio": "16:9",
    "delivery": "uri"
  }
}' > response.json
```

Extract the URI and download `output.mp4`:
```bash
URI=$(python3 -c "import sys, json; print(json.load(sys.stdin)['steps'][1]['content'][0]['uri'])" < response.json)
curl -s -L -H "x-goog-api-key: $GEMINI_API_KEY" "$URI" -o output.mp4
```

### Process & Display:
Use the bundled script `scripts/video_to_gif.sh` to crop, scale, and convert the video into an optimized GIF:
```bash
bash scripts/video_to_gif.sh output.mp4 output_hq.gif
pixoo-cli gif -f output_hq.gif
```

---

## 3. Deep Reference Guides

For comprehensive hardware rules, visual archetypes, and palette specifications:
- [pixoo64-led-bible.md](pixoo64-led-bible.md) — Physical optics, PWM behavior, true-black clamping, diffuser physics, and the 32-frame buffer limit.
- [discovered-patterns.md](discovered-patterns.md) — Empirical design guidelines, frame rates (6–12 FPS), static background locking, and archetype specs.
- [palettes.json](palettes.json) — Curated and extracted color palettes optimized for the Pixoo-64 display.
