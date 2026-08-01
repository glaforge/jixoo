#!/bin/bash
# Convert a sequence of images (like image-0.png, image-1.png, etc) into a GIF animation
# Usage: bash images_to_gif.sh "image-%d.png" output.gif

if [ "$#" -ne 2 ]; then
    echo "Usage: $0 <input_pattern> <output.gif>"
    echo "Example: $0 'image-%d.png' output.gif"
    exit 1
fi

INPUT_PATTERN="$1"
OUTPUT_GIF="$2"

echo "Converting $INPUT_PATTERN to $OUTPUT_GIF..."
ffmpeg -framerate 10 -i "$INPUT_PATTERN" -vf "scale=64:64" -sws_dither none "$OUTPUT_GIF"
echo "Done!"
