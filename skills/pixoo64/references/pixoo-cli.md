# pixoo-cli Reference

The `pixoo-cli` (or `pixoo-cli.exe` on Windows) is the native CLI to control the Divoom Pixoo 64.

## Auto-Discover Devices
Discover Pixoo 64 devices on the local network (useful to find the IP address).
```bash
pixoo-cli discover
```

## Global Options
Set the target Pixoo 64 device IP address via `-H` / `--host` or environment variable `PIXOO_HOST`:
```bash
pixoo-cli -H 192.168.1.100 <subcommand>
# OR
export PIXOO_HOST="192.168.1.100"
pixoo-cli <subcommand>
```

Common flags:
- `-H`, `--host=<host>`: Pixoo 64 IP address or hostname.
- `-v`, `--verbose`: Enable verbose debug logging output.
- `-h`, `--help`: Show help and usage instructions.

---

## Subcommands

### `screen`
Control screen display power or query the current screen power state.
```bash
# Power screen ON
pixoo-cli screen on

# Power screen OFF (standby)
pixoo-cli screen off

# Query screen state (returns ON or OFF)
pixoo-cli screen status
```

### `brightness`
Set the LED matrix brightness (0–100%).
```bash
pixoo-cli brightness 80
```

### `rotation`
Rotate the screen contents. Accepts rotation degrees (`0`, `90`, `180`, `270`) or rotation mode indices (`0`, `1`, `2`, `3`).
- `0`: Normal (0°)
- `1` / `90`: 90° Clockwise
- `2` / `180`: 180° Inverted
- `3` / `270`: 270° Clockwise
```bash
pixoo-cli rotation 90
pixoo-cli rotation 1
```

---

### `channel`
Switch channels, manage boot/startup channel, configure clock dial faces, and switch custom gallery page slots.

#### Switch Channel
```bash
# Switch to standard channels
pixoo-cli channel clock
pixoo-cli channel cloud
pixoo-cli channel visualizer
pixoo-cli channel custom
pixoo-cli channel black-screen

# Or explicitly using switch subcommand
pixoo-cli channel switch clock
```

#### Startup Boot Channel
Configure which channel loads automatically when the device powers on.
```bash
# Set startup channel
pixoo-cli channel startup set clock
pixoo-cli channel startup set custom

# Query startup channel
pixoo-cli channel startup get
```

#### Clock Dial Face
Change or query the active clock dial face ID.
```bash
# Set clock face by ID
pixoo-cli channel clock-face set 12345

# Query current clock face ID
pixoo-cli channel clock-face get
```

#### Custom Page Slot
Switch or query the active custom channel page slot (0–2).
```bash
# Set custom page slot
pixoo-cli channel page set 1

# Query active custom page slot
pixoo-cli channel page get
```

#### Channel Configuration
Inspect channel configurations and custom page slot items.
```bash
pixoo-cli channel config
```

---

### `tool`
Control interactive hardware tools on the Pixoo 64 screen.

#### Stopwatch
```bash
# Start or resume the stopwatch
pixoo-cli tool stopwatch start

# Stop / pause the stopwatch
pixoo-cli tool stopwatch stop

# Reset the stopwatch to zero
pixoo-cli tool stopwatch reset

# Query stopwatch status (running, stopped, reset)
pixoo-cli tool stopwatch status
```

#### Countdown Timer
```bash
# Start a countdown timer (e.g. 5 minutes 30 seconds)
pixoo-cli tool timer --min 5 --sec 30 start

# Stop the active timer
pixoo-cli tool timer stop

# Query countdown timer status and remaining seconds
pixoo-cli tool timer status
```

#### Scoreboard (Blue vs Red)
```bash
# Set scoreboard scores
pixoo-cli tool scoreboard --blue 21 --red 18 set

# Query current scores
pixoo-cli tool scoreboard get
```

#### Ambient Noise Decibel Meter
```bash
# Start real-time noise decibel monitoring on screen
pixoo-cli tool noise start

# Stop noise monitoring
pixoo-cli tool noise stop

# Query noise meter status (active/inactive)
pixoo-cli tool noise status
```

---

### `time`
Hardware Real-Time Clock (RTC) synchronization without relying on cloud NTP servers.
```bash
# Synchronize device RTC to the host computer's current local time
pixoo-cli time sync

# Synchronize device RTC to a specific UTC epoch timestamp (in seconds)
pixoo-cli time sync --utc 1718000000
```

---

### `config`
Inspect and update device system configuration.

```bash
# Query system configuration
pixoo-cli config get

# Update settings
# --time-format: 12 or 24
# --temp-unit: c or f
# --auto-off: auto-sleep timer in minutes (0 to disable)
# --date-format: 0 (MM/DD), 1 (DD/MM), 2 (YYYY/MM/DD)
# --mirror: on or off
pixoo-cli config set --time-format 24 --temp-unit c --auto-off 0
pixoo-cli config set --mirror off --date-format 1
```

---

### `text`
Draw text overlays on the display or clear text layers.

```bash
# Send scrolling text overlay
pixoo-cli text send -t "Hello World!" -x 0 -y 24 -c "#00FFFF" --dir left -s 50 -a center

# Send static text at specific coordinates
pixoo-cli text send -t "ALARM" -x 10 -y 10 -c "#FF0000" --dir none

# Clear all active text overlays
pixoo-cli text clear
```

Text Options:
- `-i`, `--id=<id>`: Text slot ID (default: `1`).
- `-x`, `--x=<x>`: X coordinate (`0` to `63`).
- `-y`, `--y=<y>`: Y coordinate (`0` to `63`).
- `-t`, `--text=<text>`: Text message string to render.
- `-c`, `--color=<hex>`: Text color in `#RRGGBB` format.
- `-s`, `--speed=<ms>`: Scroll speed in milliseconds (default: `50`).
- `--dir=<dir>`: Scroll direction (`left`, `right`, `none`).
- `-a`, `--align=<align>`: Alignment (`left`, `center`, `right`).
- `--font=<font>`: ROM font index (`0`–`7`).

---

### `color`
Fill the entire 64x64 matrix with a solid HTML/CSS hex color.
```bash
pixoo-cli color #23ED23
pixoo-cli color FF5500
```

---

### `image`
Display a static image (PNG, JPG, BMP). Automatically scales and centers images to fit 64x64.
```bash
pixoo-cli image path/to/artwork.png
```

---

### `gif`
Play an animated GIF file from the local filesystem or stream from an HTTP URL.
```bash
# Local GIF file
pixoo-cli gif --file path/to/animation.gif

# Remote HTTP GIF URL
pixoo-cli gif --url "http://example.com/animation.gif"
```

---

### `buzzer`
Trigger the internal piezoelectric buzzer with a defined frequency cycle.
```bash
pixoo-cli buzzer --active-ms 500 --off-ms 500 --total-ms 3000
```

Options:
- `--active-ms=<ms>`: Tone duration in milliseconds.
- `--off-ms=<ms>`: Silence duration in milliseconds.
- `--total-ms=<ms>`: Total duration in milliseconds.

---

### `cloud`
Manage Divoom Cloud authentication, list bound devices, upload animations to persistent custom channel slots, and publish to the public gallery.

```bash
# Log in to Divoom Cloud account
pixoo-cli cloud login -e user@example.com -p mypassword

# Log out of Divoom Cloud account
pixoo-cli cloud logout

# List all devices bound to your account (shows Device ID, MAC, IP, SSID, etc.)
pixoo-cli cloud devices

# Upload animation to persistent Custom Channel slot (slot 0, 1, or 2)
pixoo-cli cloud channel --file animation.gif --slot 0

# Publish artwork to Divoom Cloud Gallery
pixoo-cli cloud gallery --file artwork.gif --name "Pixel Art" --desc "Created with jixoo64"
```

---

### `raw`
Send a raw JSON payload directly to the device `/post` endpoint.
```bash
pixoo-cli raw --json '{"Command": "Channel/SetIndex", "SelectIndex": 0}'
```
*Note: Ensure the `"Command"` key is first in the JSON object for ESP32 cJSON compatibility. Full command schema details are in [SPECIFICATION.md](https://raw.githubusercontent.com/glaforge/jixoo/main/SPECIFICATION.md).*

---

**Tip**: Run `pixoo-cli --help` or `pixoo-cli <subcommand> --help` to inspect all options, defaults, and requirements for any command!
