# AI Generation Guide for Pixoo 64

You can generate images and videos using Google's Gemini AI models and display them on the Pixoo 64.
Generating content requires the `GEMINI_API_KEY` environment variable.

## 1. Generating Static Images (Gemini Nano Banana)

The Pixoo 64 requires a 1:1 (square) aspect ratio. You can use the Gemini Image API (`models/gemini-3.1-flash-image` also known as Nano Banana 2) to generate these images.

**Example Request:**
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
*Note: You will need to extract the Base64 output from the response and save it as `image.png`.*

**Display it:**
```bash
pixoo-cli image -f image.png
```

## 2. Generating Video Animations (Gemini Omni)

Gemini Omni (`models/gemini-omni-flash-preview`) can generate full videos based on a text prompt. 
Since Omni outputs standard `16:9` or `9:16` aspect ratios, you must center-crop the video to 1:1 and then scale it down to 64x64 to avoid ugly letterboxing on the Pixoo display.

**Example Request:**
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

To extract the URI and download the `output.mp4`:
```bash
URI=$(python3 -c "import sys, json; print(json.load(sys.stdin)['steps'][1]['content'][0]['uri'])" < response.json)
curl -s -L -H "x-goog-api-key: $GEMINI_API_KEY" "$URI" -o output.mp4
```

**Process & Display:**
You can use the bundled script `scripts/video_to_gif.sh` to process this video into an optimized GIF and display it!
```bash
bash scripts/video_to_gif.sh output.mp4 output_hq.gif
pixoo-cli gif -f output_hq.gif
```
