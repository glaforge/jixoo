# jixoo64 🖼️✨

Modern Java 21 client library and native CLI for the **Divoom Pixoo 64** 64x64 RGB LED matrix display.

---

## Features

- ☕ **Modern Java 21 API**: Built with Java 21 records, sealed interfaces, and native `HttpClient` enforcing HTTP/1.1 protocol rules required by the Pixoo 64 ESP32 web server.
- 🚀 **Picocli Command-Line Interface (`pixoo-cli`)**: Full-featured terminal application with fast command execution.
- ⚡ **GraalVM 25 Native Image Support**: Instantaneous (<5ms startup) native executable support (`mvn package -Pnative`).
- 🔍 **Subnet Device Discovery**: Auto-discover active Pixoo 64 devices on your local network using UDP broadcast and ARP scanning.
- 🖼️ **Image & Animated GIF Processing**: Load PNG, JPG, BMP, or animated GIF files from disk or stream remote GIF URLs directly. Includes smart aspect-ratio preserving scaling and letterboxing for 64x64 matrix pixels.
- ✍️ **Hardware Text Layer Rendering**: Multi-slot text overlay engine supporting ROM fonts, scroll direction (static, left, right), speed, positioning, hexadecimal colors, and alignments.
- ⏱️ **Interactive Hardware Tools Engine**: Control on-screen Stopwatch, Countdown Timer, Scoreboard (Blue vs Red), and real-time Ambient Noise Decibel Meter.
- 🕒 **Direct RTC Clock Synchronization**: Synchronize device hardware real-time clock without relying on external cloud NTP.
- ⚙️ **System Configuration Management**: Configure 12/24-hour time format, Celsius/Fahrenheit units, date formatting, display mirroring, auto-sleep timers, and startup channels.
- ☁️ **Divoom Cloud & Persistent Custom Channels**: Direct integration with Divoom Cloud API for persistent flash playlist storage and gallery publishing.
- 🎛️ **Full Device Control**: Channel switching, LED matrix brightness, screen power toggle and status query, rotation angle modes, buzzer alarm tone patterns, and raw JSON execution.

---

## Requirements

- **Java 21** or higher
- **Maven 3.8+**
- *(Optional)* **GraalVM 25** for compiling standalone native binaries

---

## Installation & Build

Clone the repository and build with Maven:

```bash
git clone https://github.com/divoom/jixoo64.git
cd jixoo64

# Build library and executable fat JAR
mvn package
```

The executable JAR will be located at:
`target/jixoo64-1.0.0-SNAPSHOT-cli.jar`

### GraalVM Native Executable Build

If you have GraalVM 25 installed, compile a native binary:

```bash
mvn package -Pnative -DskipTests
```

The standalone native binary will be generated at:
`./target/pixoo-cli`

### Download Pre-compiled Binaries

You can download ready-to-use native binaries for Linux, macOS (Apple Silicon), and Windows from the [GitHub Releases page](https://github.com/divoom/jixoo64/releases).

---

## CLI Usage (`pixoo-cli`)

Set your target Pixoo 64 device IP address via command-line option `-H` / `--host` or environment variable `PIXOO_HOST`:

```bash
export PIXOO_HOST="192.168.1.100"
```

### Subcommands Reference

#### 1. Auto-Discover Devices
```bash
pixoo-cli discover
```

#### 2. Channel Switching & Management
```bash
# Switch active channel
pixoo-cli channel clock
pixoo-cli channel cloud
pixoo-cli channel visualizer
pixoo-cli channel custom
pixoo-cli channel black-screen

# Manage default boot channel
pixoo-cli channel startup               # Query startup channel
pixoo-cli channel startup custom        # Set startup channel to Custom

# Select clock face by ID
pixoo-cli channel clock-face            # Query active clock ID
pixoo-cli channel clock-face 42         # Select clock face 42

# Switch custom gallery page slot (0, 1, 2)
pixoo-cli channel page                  # Query active page slot
pixoo-cli channel page 1                # Switch to page slot 1

# Query channel timing configuration
pixoo-cli channel config
```

#### 3. Set LED Brightness (0-100%)
```bash
pixoo-cli brightness 80
```

#### 4. Power Screen Display On/Off & Status
```bash
pixoo-cli screen status   # Query if screen is ON or OFF
pixoo-cli screen on       # Turn screen ON
pixoo-cli screen off      # Turn screen OFF / Standby
```

#### 5. Screen Rotation
```bash
pixoo-cli rotation 0      # Normal (0°, mode 0)
pixoo-cli rotation 90     # 90° Clockwise (mode 1)
pixoo-cli rotation 180    # 180° (mode 2)
pixoo-cli rotation 270    # 270° Clockwise (mode 3)
```

#### 6. Built-in Hardware Tools (`tool`)
```bash
# Stopwatch
pixoo-cli tool stopwatch start
pixoo-cli tool stopwatch stop
pixoo-cli tool stopwatch reset
pixoo-cli tool stopwatch status

# Countdown Timer
pixoo-cli tool timer --min 5 --sec 30 start
pixoo-cli tool timer stop
pixoo-cli tool timer status

# Scoreboard (Dual team: Blue vs Red)
pixoo-cli tool scoreboard --blue 21 --red 18 set
pixoo-cli tool scoreboard get

# Ambient Noise Decibel Meter
pixoo-cli tool noise start
pixoo-cli tool noise stop
pixoo-cli tool noise status
```

#### 7. Clock Synchronization (`time`)
```bash
# Synchronize device real-time clock to host local time
pixoo-cli time sync
```

#### 8. System Configuration (`config`)
```bash
# Query system configuration
pixoo-cli config get

# Update settings
pixoo-cli config set --time-format 24 --temp-unit c --auto-off 0
pixoo-cli config set --mirror off --date-format 1
```

#### 9. Text Layer Rendering
```bash
# Render scrolling text
pixoo-cli text send -t "Hello World!" -x 0 -y 24 -c "#00FFFF" --dir left -s 50 -a center

# Clear all text overlays
pixoo-cli text clear
```

#### 10. Display Solid Plain Colors
```bash
pixoo-cli color #23ED23
pixoo-cli color 23ED23
```

#### 11. Display Static Images & Animated GIFs
```bash
# Display image file (auto-resized and centered)
pixoo-cli image path/to/artwork.png

# Display local animated GIF
pixoo-cli gif --file path/to/animation.gif

# Stream remote HTTP GIF
pixoo-cli gif --url "http://example.com/animation.gif"
```

#### 12. Divoom Cloud & Custom Channels (`cloud`)
```bash
# Log in to Divoom Cloud account
pixoo-cli cloud login -e user@example.com -p mypassword

# List bound hardware devices
pixoo-cli cloud devices

# Upload animation to persistent Custom Channel slot (slot 0..2)
pixoo-cli cloud channel --file animation.gif --slot 0

# Publish artwork to Divoom Cloud Gallery
pixoo-cli cloud gallery --file artwork.gif --name "Pixel Art" --desc "Created with jixoo64"
```

#### 13. Trigger Buzzer Sound Pattern
```bash
pixoo-cli buzzer --active-ms 500 --off-ms 500 --total-ms 3000
```

#### 14. Execute Raw JSON Protocol Commands
```bash
pixoo-cli raw --json '{"Command": "Channel/SetIndex", "SelectIndex": 0}'
```

---

## Java Library Usage

Include `jixoo64` in your project dependencies.

### Quickstart Example

```java
import io.github.glaforge.jixoo.api.*;
import io.github.glaforge.jixoo.model.tool.StopwatchAction;

import java.nio.file.Path;

public class PixooExample {
    public static void main(String[] args) {
        // Connect to Pixoo 64 device
        try (PixooClient client = PixooClient.create("192.168.1.100")) {
            
            // 1. Sync real-time clock
            client.syncTime();

            // 2. Switch to Clock channel
            client.selectChannel(PixooChannel.CLOCK);

            // 3. Adjust LED brightness
            client.setBrightness(75);

            // 4. Display an image file
            client.sendImage(Path.of("artwork.png"));

            // 5. Send hardware text overlay
            PixooText text = PixooText.builder()
                    .textId(1)
                    .position(0, 24)
                    .text("ALERT!")
                    .color("#FF0000")
                    .scrollLeft()
                    .speed(50)
                    .alignCenter()
                    .build();
            client.sendText(text);

            // 6. Control hardware stopwatch
            client.setStopwatch(StopwatchAction.START);
        }
    }
}
```

### Auto-Discovering Devices in Java

```java
import io.github.glaforge.jixoo.discovery.PixooDevice;

import java.time.Duration;
import java.util.List;

List<PixooDevice> devices = PixooDevice.discoverDevices(Duration.ofSeconds(3));
for (PixooDevice device : devices) {
    System.out.println("Discovered: " + device.deviceName() + " at " + device.ipAddress());
}
```

---

## Protocol Specification

For full technical protocol documentation of the Pixoo 64 HTTP API, see [SPECIFICATION.md](SPECIFICATION.md).

---

## AI Agent Skills

This repository provides an official Agent Skill for AI agents to automatically interact with your Pixoo 64 device. You can instruct your agents to install and use this skill via the AgentSkills protocol.

To install this skill for your AI agent:

```bash
npx skills add github.com/glaforge/jixoo/skills/pixoo64
```
or 
```bash
gh skills add github.com/glaforge/jixoo/skills/pixoo64
```

---

## License

This project is licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for details.

---

## Disclaimer

This is not an official Google project.
