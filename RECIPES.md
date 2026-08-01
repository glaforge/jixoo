# AI Generation Recipes for Pixoo 64

This guide covers how to use Google's Gemini AI models to generate content and display it on your Divoom Pixoo 64 device using the `pixoo-cli` command-line tool.

---

## Prerequisites

Before running the examples in this guide, ensure you have the following installed and configured on your system:

1. **`pixoo-cli`**: The native executable binary (`pixoo-cli` on macOS/Linux or `pixoo-cli.exe` on Windows). You can download the pre-compiled releases for your operating system from the [GitHub Releases page](https://github.com/glaforge/jixoo/releases).
2. **`curl`**: Used to make HTTP requests to the Gemini API.
3. **`ffmpeg`**: Required to stitch images into GIFs or convert video files to high-quality pixel art animations.
4. **`python3`**: Used in some examples to parse JSON responses from the Gemini API.
5. **Gemini API Key**: You must have a valid Gemini API key exported as an environment variable (e.g., `export GEMINI_API_KEY="your_api_key_here"`).

---

## 1. Static Images (Nano Banana / Gemini Flash Image)

You can generate 1:1 aspect ratio images using the Gemini Image generation models and display them directly on your Pixoo. 

### Step 1: Generate the Image
Use the Gemini Image API to generate a square image. We use `gemini-3.1-flash-image` (also known as Nano Banana 2).

```bash
curl -s -X POST "https://generativelanguage.googleapis.com/v1beta/interactions?key=$GEMINI_API_KEY" \
-H 'Content-Type: application/json' \
-d '{
  "model": "models/gemini-3.1-flash-image",
  "input": "A pixel art 64x64 style image of a cyberpunk city at night.",
  "response_format": {
    "type": "image",
    "aspect_ratio": "1:1"
  }
}'
```
*(Note: Extract the Base64 output from the response and save it as `image.png`)*

### Step 2: Display on Pixoo
The `jixoo64` CLI automatically handles resizing the image to the 64x64 canvas.

```bash
pixoo-cli -H 192.168.86.161 image -f image.png
```

---

## 2. Animations (From a Series of Images)

You can create an animation by generating a sequence of images (e.g. frames of a character walking) and stitching them together into a GIF.

### Step 1: Generate Frame Sequence
Generate multiple images using the API (or extract them). Save them sequentially, e.g., `frame_01.png`, `frame_02.png`, etc.

### Step 2: Stitch into a GIF with FFmpeg
Use `ffmpeg` to stitch the images into a high-quality, pixel-perfect 64x64 GIF at 10 frames per second. We use nearest-neighbor scaling and disable dithering to keep the colors crisp for the Pixoo.

```bash
ffmpeg -framerate 10 -i frame_%02d.png \
  -vf "scale=64:64:flags=neighbor,split[s0][s1];[s0]palettegen=stats_mode=diff[p];[s1][p]paletteuse=dither=none" \
  -loop 0 animation.gif
```

### Step 3: Display on Pixoo
Upload the custom GIF animation to the device:

```bash
pixoo-cli -H 192.168.86.161 gif -f animation.gif
```

---

## 3. Video (Gemini Omni)

Gemini Omni can generate full videos based on a text prompt. Since Omni outputs standard 16:9 or 9:16 aspect ratios, we must center-crop the video to 1:1 and then scale it down to 64x64 to avoid ugly black letterboxing bars.

### Step 1: Generate Video with Gemini Omni
Request a video generation by sending a prompt to `gemini-omni-flash-preview` via the `interactions` API endpoint.

#### Text Prompt Only:
```bash
curl -s -X POST "https://generativelanguage.googleapis.com/v1beta/interactions?key=$GEMINI_API_KEY" \
-H "Content-Type: application/json" \
-d '{
  "model": "models/gemini-omni-flash-preview",
  "input": "A head close-up of a cute comic dragon character throwing flames at us.",
  "response_format": {
    "type": "video",
    "aspect_ratio": "16:9",
    "delivery": "uri"
  }
}' > response.json
```

#### Multimodal (Text + Reference Image):
To reference an image, pass an array of typed parts for `input` containing the base64-encoded image:

```json
{
  "model": "models/gemini-omni-flash-preview",
  "input": [
    { "type": "text", "text": "A video over a black background that morphs a vibrant glowing rainbow into the provided logo." },
    { "type": "image", "mime_type": "image/png", "data": "<BASE64_IMAGE_DATA>" }
  ],
  "response_format": {
    "type": "video",
    "aspect_ratio": "16:9",
    "delivery": "uri"
  }
}
```

Extract the download URL from the JSON response using Python:
```bash
URI=$(python3 -c "import sys, json; print(json.load(sys.stdin)['steps'][1]['content'][0]['uri'])" < response.json)
curl -s -L -H "x-goog-api-key: $GEMINI_API_KEY" "$URI" -o output.mp4
```

### Step 2: Process to High-Quality Pixel Art GIF
Use `ffmpeg` to scale it to 64x64, generate a custom palette, and apply it with `dither=none` to preserve solid pixel colors.

*Note: The Pixoo 64 hardware animation player will only display the first 30–32 frames of a custom HTTP GIF before looping. To ensure a full 10-second video plays from start to finish without getting cut in half, set the framerate to 3 FPS (`-r 3`), which keeps the total frame count around 30 frames.*

```bash
ffmpeg -i output.mp4 -vf "crop=in_h:in_h,scale=64:64,split[s0][s1];[s0]palettegen=stats_mode=diff[p];[s1][p]paletteuse=dither=none" -r 3 -loop 0 output_hq.gif
```

### Step 3: Display on Pixoo
Send the converted video animation to the device:

```bash
pixoo-cli -H 192.168.86.161 gif -f output_hq.gif
```
