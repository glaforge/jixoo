# jixoo64 🖼️✨

Modern Java 21 client library and native CLI for the **Divoom Pixoo 64** 64x64 RGB LED matrix display.

---

## Features

- ☕ **Modern Java 21 API**: Built with Java 21 records, sealed interfaces, and native `HttpClient` enforcing HTTP/1.1 protocol rules required by the Pixoo 64 ESP32 web server.
- 🚀 **Picocli Command-Line Interface (`pixoo64`)**: Full-featured terminal application with fast command execution.
- ⚡ **GraalVM 25 Native Image Support**: Instantaneous (<5ms startup) native executable support (`mvn package -Pnative`).
- 🔍 **Subnet Device Discovery**: Auto-discover active Pixoo 64 devices on your local network using UDP broadcast.
- 🖼️ **Image & Animated GIF Processing**: Load PNG, JPG, BMP, or animated GIF files from disk or stream remote GIF URLs directly. Includes smart aspect-ratio preserving scaling and letterboxing for 64x64 matrix pixels.
- ✍️ **Hardware Text Layer Rendering**: Multi-slot text overlay engine supporting ROM fonts, scroll direction (static, left, right), speed, positioning, hexadecimal colors, and alignments.
- 🎛️ **Full Device Control**: Channel switching (Clock, Cloud Gallery, Visualizer, Custom, Black Screen), LED matrix brightness, screen power toggle, rotation angle, buzzer alarm tone patterns, and raw JSON command execution.

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
`./target/pixoo64`

### Releasing Native Binaries

This project uses a GitHub Actions workflow to automatically build and publish native binaries for Linux, macOS (Apple Silicon), and Windows. 

To trigger a new release, create a Git tag starting with `v` (e.g., `v1.0.0`) and push it to GitHub:

```bash
git tag v1.0.0
git push origin v1.0.0
```

The CI pipeline will automatically extract the version from the tag, compile the native executables via GraalVM, and attach them to a new GitHub Release on the repository.

---

## CLI Usage (`pixoo64`)

Set your target Pixoo 64 device IP address via command-line option `-H` / `--host` or environment variable `PIXOO_HOST`:

```bash
export PIXOO_HOST="192.168.1.100"
```

### Subcommands Reference

#### 1. Auto-Discover Devices
```bash
pixoo64 discover
```

#### 2. Channel Switching
```bash
pixoo64 -H 192.168.1.100 channel clock
pixoo64 channel cloud
pixoo64 channel visualizer
pixoo64 channel custom
pixoo64 channel black-screen
```

#### 3. Set LED Brightness (0-100%)
```bash
pixoo64 brightness 80
```

#### 4. Power Screen Display On/Off
```bash
pixoo64 screen off
pixoo64 screen on
```

#### 5. Screen Rotation
```bash
pixoo64 rotation 0      # Normal (0°)
pixoo64 rotation 90     # 90° Clockwise
pixoo64 rotation 180    # 180°
pixoo64 rotation 270    # 270° Clockwise
```

#### 6. Text Layer Rendering
```bash
# Render scrolling text
pixoo64 text send -t "Hello World!" -x 0 -y 24 -c "#00FFFF" --dir left -s 50 -a center

# Clear all text overlays
pixoo64 text clear
```

#### 7. Display Solid Plain Colors
```bash
pixoo64 color #23ED23
pixoo64 color 23ED23
```

#### 8. Display Static Images
```bash
pixoo64 image path/to/artwork.png
```

#### 9. Display Animated GIFs
```bash
# Display local GIF file
pixoo64 gif --file path/to/animation.gif

# Render remote HTTP GIF URL
pixoo64 gif --url "http://example.com/animation.gif"
```

#### 10. Trigger Piezoelectric Buzzer Sound Pattern
```bash
pixoo64 buzzer --active-ms 500 --off-ms 500 --total-ms 3000
```

#### 11. Execute Raw JSON Protocol Commands
```bash
pixoo64 raw --json '{"Command": "Channel/SetIndex", "SelectIndex": 0}'
```

---

## Java Library Usage

Include `jixoo64` in your project dependencies.

### Quickstart Example

```java
import io.github.glaforge.jixoo.api.*;

import java.nio.file.Path;

public class PixooExample {
    public static void main(String[] args) {
        // Create client
        PixooClient client = PixooClient.create("192.168.1.100");

        // 1. Switch to Clock channel
        client.selectChannel(PixooChannel.CLOCK);

        // 2. Adjust LED brightness
        client.setBrightness(75);

        // 3. Display an image file
        client.sendImage(Path.of("image.png"));

        // 4. Send hardware text overlay
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

## License

This project is licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for details.

---

## Disclaimer

This is not an official Google project.

