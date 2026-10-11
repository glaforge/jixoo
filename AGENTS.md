# AGENTS.md — Coding Agent Guide for `jixoo64`

`jixoo64` is a modern **Java 21** library and native command-line tool (`pixoo-cli`) for controlling **Divoom Pixoo 64** RGB LED matrix displays over local HTTP (`http://<device-ip>:80/post`) and Divoom Cloud APIs (`https://app.divoom-gz.com`).

---

## 1. Build, Test & Run Commands

- **Java Version**: Java 21 (`<maven.compiler.release>21</maven.compiler.release>`).
- **Run unit & integration tests**:
  ```bash
  ./mvnw test
  ```
- **Run a single test class**:
  ```bash
  ./mvnw test -Dtest=LedPostProcessorTest
  ```
- **Package library + shaded CLI uber-JAR (`target/jixoo64-*-cli.jar`)**:
  ```bash
  ./mvnw clean package
  ```
- **Build GraalVM native executable (`target/pixoo-cli`)**:
  ```bash
  ./mvnw -Pnative native:compile
  ```

---

## 2. Critical Architectural Constraints

### 2.1 Zero AWT in the Core Image Pipeline (GraalVM Native-Image)
- **DO NOT** introduce `java.awt.*` or `javax.imageio.*` into the core image processing pipeline (`PixooImage`, `PngDecoder`, `BmpDecoder`, `GifDecoder`, `GifEncoder`, `DivoomAssetDecoder`, `LedPostProcessor`, `RawRgbBuffer`).
- **Why**: GraalVM `native-image` on macOS and headless environments fails or requires complex native AWT linkage when AWT classes are reachable from `pixoo-cli`.
- All image decoding, scaling (`SamplingMode.BILINEAR` / `SamplingMode.NEAREST_NEIGHBOR`), GIF LZW encoding/decoding, and HSV color math (`LedPostProcessor`) are implemented in pure Java (`ar.com.hjg:pngj` is used for PNG decoding).
- *Note*: `ImageProcessor` retains `BufferedImage` convenience methods strictly for JVM library callers, while `pixoo-cli` and all `Path`/`InputStream`/`URI` flows use `PixooImage` exclusively.

### 2.2 GraalVM Reflection Configuration
- Jackson-serialized/deserialized models (`PixooCommand` subtypes, Cloud API DTOs, Weather DTOs) and Picocli command classes must be registered in:
  `src/main/resources/META-INF/native-image/io.github.glaforge.jixoo/reflect-config.json`
- Picocli annotation processing (`picocli-codegen`) runs automatically during compilation, but any new Jackson record/DTO added to `model/`, `cloud/model/`, `weather/`, or `internal/HttpPixooClient` **must** also be added to `reflect-config.json` so native binary builds succeed without reflection errors at runtime.

### 2.3 Pixoo 64 Hardware & Protocol Quirks
See [`SPECIFICATION.md`](SPECIFICATION.md) for the full protocol reference. Key hardware constraints to keep in mind:
1. **Single-Threaded Device HTTP Server**: The Pixoo 64's internal ESP32 HTTP server cannot handle rapid back-to-back requests reliably. `HttpPixooClient` enforces a minimum delay between consecutive HTTP POSTs and serializes access.
2. **`Draw/ResetHttpGifId` Before `Draw/SendHttpGif`**: Always reset the HTTP GIF buffer (`resetGifId()`) before sending a new static image or multi-frame GIF, and increment `PicID` monotonically.
3. **True-Black (`#000000`) Powers Off Physical LEDs**: Non-zero dark values (e.g., `RGB(5, 5, 8)`) trigger the LED driver's minimum PWM duty cycle, causing visible gray haze across the diffuser. `LedPostProcessor` (`--led-optimize`) clamps pixels with `max(R, G, B) <= blackThreshold` (default `15`) to `#000000`.
4. **32-Frame Hardware Buffer Ceiling**: Custom HTTP GIFs reliably play up to ~30–32 frames before looping early or dropping frames.

---

## 3. Package Structure

```text
src/main/java/io/github/glaforge/jixoo/
├── api/                # Public interfaces (PixooClient, PixooResponse) & exceptions
├── cli/                # Picocli entry point (PixooCli)
│   └── command/        # Subcommands (ImageCommand, GifCommand, CloudCommand, ToolCommand, etc.)
├── cloud/              # DivoomCloudClient & binary .divoom encoder/decoder support
│   └── model/          # Divoom Cloud REST DTOs
├── discovery/          # PixooDiscovery (mDNS, cloud scan, ARP, subnet scan)
├── example/            # Standalone demo apps
├── image/              # Pure-Java image pipeline:
│                       #   PixooImage, ScaleMode, SamplingMode, LedPostProcessor,
│                       #   ImageProcessor, PngDecoder, BmpDecoder, GifDecoder,
│                       #   GifEncoder, DivoomAssetDecoder
├── internal/           # HttpPixooClient implementation (java.net.http.HttpClient + Jackson)
├── model/              # Core domain enums (Channel, ClockLocation, Rotation, TextDirection, etc.)
│   ├── command/        # Sealed PixooCommand hierarchy for /post JSON payloads
│   ├── sys/            # SysConfig & ChannelConfig records
│   └── tool/           # Stopwatch, Timer, Scoreboard, Noise, Alarm records
└── weather/            # Zero-auth DivoomWeatherClient & DTOs
```

---

## 4. Documentation & Agent Skill Sync Checklist

Whenever you add or modify a CLI subcommand, flag, or Java API method:
1. **Java API & Overloads**: Keep `PixooClient` and `HttpPixooClient` in sync. For image/GIF methods, maintain consistent overload combinations for `ScaleMode`, `SamplingMode`, and `LedPostProcessor`.
2. **CLI Documentation**: Update both:
   - [`README.md`](README.md) (CLI Commands Reference & Java Library Usage sections)
   - [`skills/pixoo64/references/pixoo-cli.md`](skills/pixoo64/references/pixoo-cli.md) (used by AI agents invoking the `pixoo64` skill)
3. **Agent Skill (`skills/pixoo64/`)**:
   - [`skills/pixoo64/SKILL.md`](skills/pixoo64/SKILL.md) — High-level skill entry point
   - [`skills/pixoo64/references/pixoo64-led-bible.md`](skills/pixoo64/references/pixoo64-led-bible.md) — Physical LED optical & temporal laws
   - [`skills/pixoo64/references/discovered-patterns.md`](skills/pixoo64/references/discovered-patterns.md) — Empirical animation archetypes
   - [`skills/pixoo64/references/ai-generation.md`](skills/pixoo64/references/ai-generation.md) & [`nano-banana-prompts.md`](skills/pixoo64/references/nano-banana-prompts.md) — Gemini image/video generation workflows
4. **Tests**: Add or update unit tests in `src/test/java/io/github/glaforge/jixoo/` (`PixooCliTest`, `HttpPixooClientIntegrationTest`, `PixooCommandTest`, or `image/*Test`) and verify `./mvnw test` passes before committing.
