# AGENTS.md — Coding Agent Guide for `jixoo64`

`jixoo64` is a modern **Java 21** library and native command-line tool (`pixoo-cli`) for controlling **Divoom Pixoo 64** and **Pixoo-64 II** RGB LED matrix displays over local HTTP (`http://<device-ip>:80/post` on the Pixoo 64, `http://<device-ip>:9000/divoom_api` on the Pixoo-64 II) and Divoom Cloud APIs (`https://app.divoom-gz.com`).

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
1. **Single-Threaded Device HTTP Server**: The Pixoo 64's internal ESP32 HTTP server cannot handle rapid back-to-back requests reliably. `HttpPixooClient` serializes requests (fair lock) and spaces them at least 30 ms apart.
2. **`Draw/ResetHttpGifId` Before `Draw/SendHttpGif`**: Always reset the HTTP GIF buffer (`resetGifId()`) before sending a new static image or multi-frame GIF, and increment `PicID` monotonically.
3. **True-Black (`#000000`) Powers Off Physical LEDs**: Non-zero dark values (e.g., `RGB(5, 5, 8)`) trigger the LED driver's minimum PWM duty cycle, causing visible gray haze across the diffuser. `LedPostProcessor` (`--led-optimize`) clamps pixels with `max(R, G, B) <= blackThreshold` (default `15`) to `#000000`.
4. **32-Frame Hardware Buffer Ceiling**: Custom HTTP GIFs reliably play up to ~30–32 frames before looping early or dropping frames.

### 2.4 Pixoo-64 II Support (`PixooModel`)
The Pixoo-64 II speaks the same commands with a different transport; see [`SPECIFICATION.md` §10](SPECIFICATION.md) for the compatibility matrix observed on a real device.
1. **Model profile**: `api/PixooModel` holds each model's port, API path, still-image duration and the commands the device acknowledges without honoring. Put per-model behavior there rather than in `if` branches spread across the code.
2. **Response envelope**: the Pixoo-64 II replies with `ReturnCode` / `ReturnMessage` (empty on success) instead of `error_code`. `PixooResponse` reads both. Never treat a missing `error_code` as success without checking `ReturnCode`.
3. **Unsupported commands**: unknown commands (`"Only accept JSON parameters"`), reads that return no data, and commands listed in `PixooModel` throw `UnsupportedCommandException`. Do not silently return defaults.
4. **Detection**: a client built without a model detects it lazily on its first command (`discovery/PixooModelProbe`); building a client must never touch the network.
5. **Still images and animations**: on the Pixoo-64 II, a long `PicSpeed` queues successive still images, so one-frame animations are sent at 1000 ms. Each frame takes ~1.5 s to be acknowledged (a 32-frame animation takes ~50 s).

---

## 3. Package Structure

```text
src/main/java/io/github/glaforge/jixoo/
├── api/                # Public interfaces (PixooClient, PixooResponse) & exceptions
├── cli/                # Picocli entry point (PixooCli)
│   └── command/        # Subcommands (ImageCommand, GifCommand, CloudCommand, ToolCommand, etc.)
├── cloud/              # DivoomCloudClient & binary .divoom encoder/decoder support
│   └── model/          # Divoom Cloud REST DTOs
├── discovery/          # PixooDevice (UDP, ARP, subnet scan), PixooModelProbe (model detection)
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
   - `HttpPixooClientIntegrationTest` runs against both models (`@EnumSource(PixooModel.class)`); `Pixoo64IIProtocolTest` replays replies recorded on a real Pixoo-64 II (`src/test/resources/pixoo64-ii/replies.json`, with an anonymized `DeviceId`). Commands missing from that file get the device's unknown-command reply.
5. **Device compatibility**: When a command is added or changed, check it on both models when possible and update the matrix in `SPECIFICATION.md` §10.
