---
name: pixoo64
description: Interact with Divoom Pixoo 64 devices to display images, videos, text, colors, change channels, run hardware tools (stopwatch, timer, scoreboard, noise meter, pomodoro, alarms, countdowns), sync time, query live weather, search and stream cloud pixel art, and configure device settings. Includes AI generation tools for Gemini and physical LED matrix design guidelines.
license: Apache-2.0
compatibility: Requires pixoo-cli to be installed and available in the system PATH.
---

# Pixoo 64 Agent Skill

This skill allows you to control and display content on Divoom Pixoo 64 LED matrix devices.
It relies on the `pixoo-cli` command-line tool, which must be installed on the user's system.
If the user does not have it installed, you can download the pre-compiled binary for their OS from the [GitHub Releases page](https://github.com/glaforge/jixoo/releases), or advise them to install it.

## 1. Controlling the Device

You can use the `pixoo-cli` tool to directly control the Pixoo 64 device. The CLI provides a wide variety of commands to:
- Change channels, set startup boot channel, select or browse clock faces (`channel clock-face top`), or switch custom gallery page slots
- Set brightness, query screen state (`screen status`), toggle screen on/off, and configure delayed sleep timers (`screen sleep`)
- Control hardware tools: digital stopwatch, countdown timer, dual scoreboard (Blue vs Red), ambient noise decibel meter, Pomodoro focus timer (`tool pomodoro`), hardware alarms (`tool alarm`), and countdowns (`tool countdown`)
- Synchronize hardware real-time clock (`time sync`)
- Query live weather and 5-day forecasts via zero-auth weather proxy (`weather current`, `weather forecast`)
- Discover, search, download, and directly stream/play community pixel art from Divoom Cloud (`cloud browse`, `cloud search`, `cloud artist`, `cloud play`)
- Inspect and configure system settings (`config get` / `config set`: 12/24h, Celsius/Fahrenheit, date format, mirror mode, auto-off)
- Draw text overlays, display solid colors, render static images, and play animated GIFs
- Trigger piezoelectric buzzer sound rhythms
- Manage Divoom Cloud integration, persistent flash custom channel playlists, and gallery uploads

When you need to execute a command, first ensure you know the device IP address, which the user can provide or which can be discovered using the `discover` subcommand if on the same local network.

For full details on the available commands and how to use them, refer to:
[pixoo-cli.md](references/pixoo-cli.md)

*Tip: If you are unsure of the exact syntax for a command, you can always run `pixoo-cli --help` or `pixoo-cli <subcommand> --help` to see the built-in help.*

## 2. Generating Images and Videos with Gemini

If the user requests to generate an image or video to display on the Pixoo 64, leverage Google's Gemini models:
- **Images & Pixel Art Sprites**: Use Gemini Nano Banana 2 (`models/gemini-3.1-flash-image`) with strict pixel art prompts.
- **Videos & Animations**: Use Gemini Omni (`models/gemini-omni-flash-preview`) cropped to 1:1 and downscaled to 64×64.

Always enforce authentic pixel art styling (no anti-aliasing blur, solid `#000000` black background, high color saturation).

For step-by-step instructions and curl examples, see:
[ai-generation.md](references/ai-generation.md)

For prompt templates (sprite sheets, scenic loops, HUD widgets), see:
[nano-banana-prompts.md](references/nano-banana-prompts.md)

## 3. Physical LED Hardware Rules

A 64×64 RGB LED matrix has distinct physical characteristics compared to computer monitors:
- **True-Black (`#000000`)**: `#000000` powers off the physical LED. Aim for 25%–60% black background so unlit LEDs seamlessly blend into the physical bezel without light bleed.
- **Diffuser Saturation**: Colors must be saturated (`HSV Saturation >= 0.60`) to punch through the matte diffuser.
- **Hardware Frame Limit**: Custom HTTP GIF animations must stay between **4 and 28 frames** (maximum 32 frames) to fit within the ESP32 hardware memory buffer.
- **Frame Rate**: Optimal pacing is **8 to 12 FPS** (delays of **80ms – 125ms**). Keep 60%–85% of background pixels static to prevent distracting whole-screen flicker.

For deep optical laws and animation principles, see:
- [pixoo64-led-bible.md](references/pixoo64-led-bible.md) — Physical optics, PWM duty cycles, and hardware limits.
- [discovered-patterns.md](references/discovered-patterns.md) — Empirical archetypes, pacing, and design patterns.
- [palettes.json](references/palettes.json) — Curated and extracted color palettes for LED matrices.

## Available Helper Scripts

This skill bundles scripts to assist with processing media for the Pixoo 64:

- **`scripts/images_to_gif.sh`** — Stitches a sequence of images into a 64x64 GIF at 10 FPS without dithering.
- **`scripts/video_to_gif.sh`** — Crops a 16:9 MP4 video to 1:1, scales it to 64x64, and converts it to a 3 FPS GIF optimized for the Pixoo display.
