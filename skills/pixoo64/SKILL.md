---
name: pixoo64
description: Interact with Divoom Pixoo 64 devices to display images, videos, text, colors, change channels, and ring the buzzer. Includes AI generation tools for Gemini.
license: Apache-2.0
compatibility: Requires pixoo-cli to be installed and available in the system PATH.
---

# Pixoo 64 Agent Skill

This skill allows you to control and display content on Divoom Pixoo 64 LED matrix devices.
It relies on the `pixoo-cli` command-line tool, which must be installed on the user's system.

## 1. Controlling the Device

You can use the `pixoo-cli` tool to directly control the Pixoo 64 device. The CLI provides a wide variety of commands to change channels, set brightness, turn the screen on/off, draw text, display solid colors, play custom animations (GIFs), and ring the buzzer.

When you need to execute a command, first ensure you know the device IP address, which the user can provide or which can be discovered using the `discover` subcommand if on the same local network.

For full details on the available commands and how to use them, refer to the documentation:
[pixoo-cli.md](references/pixoo-cli.md)

*Tip: If you are unsure of the exact syntax for a command, you can always run `pixoo-cli --help` or `pixoo-cli <subcommand> --help` to see the built-in help.*

## 2. Generating Images and Videos with Gemini

If the user requests to generate an image or video to display on the Pixoo 64, you can leverage Google's Gemini models.
The Pixoo 64 requires a 1:1 aspect ratio (square), and animations must be carefully cropped, scaled, and converted to optimized GIFs.

For detailed instructions on generating content with Gemini Nano Banana (images) or Gemini Omni (video) and preparing it for the device, refer to the AI Generation guide:
[ai-generation.md](references/ai-generation.md)

## Available Helper Scripts

This skill bundles scripts to assist with processing media for the Pixoo 64:

- **`scripts/images_to_gif.sh`** — Stitches a sequence of images into a 64x64 GIF at 10 FPS without dithering.
- **`scripts/video_to_gif.sh`** — Crops a 16:9 MP4 video to 1:1, scales it to 64x64, and converts it to a 3 FPS GIF optimized for the Pixoo display.
