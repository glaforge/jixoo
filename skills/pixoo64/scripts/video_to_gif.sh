#!/bin/bash
# Crop a video to 1:1, scale it down to 64x64, and convert to an optimized GIF
# Usage: bash video_to_gif.sh input.mp4 output.gif

if [ "$#" -ne 2 ]; then
    echo "Usage: $0 <input.mp4> <output.gif>"
    exit 1
fi

INPUT_VIDEO="$1"
OUTPUT_GIF="$2"

echo "Cropping and scaling $INPUT_VIDEO to $OUTPUT_GIF..."
# Using crop=ih:ih (makes it square), scale=64:64, framerate=3 (for simple animations)
ffmpeg -i "$INPUT_VIDEO" \
  -vf "crop=ih:ih,scale=64:64:flags=neighbor,split[s0][s1];[s0]palettegen[p];[s1][p]paletteuse" \
  -r 3 -loop 0 \
  "$OUTPUT_GIF"
echo "Done!"
