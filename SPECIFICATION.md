# Divoom Pixoo 64 HTTP Protocol Specification

This document provides a comprehensive, exhaustive specification of the Divoom Pixoo 64 local network HTTP API protocol.

---

## 1. Overview & Transport Layer

The Divoom Pixoo 64 communicates over local Wi-Fi via an embedded HTTP server (typically running on an ESP32 microcontroller).

### Key Protocol Attributes

* **Transport Protocol:** HTTP / 1.1 (**Strict requirement**)
* **HTTP Method:** `POST`
* **API Endpoint:** `http://<DEVICE_IP>:<PORT>/post`
* **Default Port:** `80` (or `9000` on select firmware versions)
* **Content-Type:** `application/json; charset=utf-8`

> [!IMPORTANT]
> **HTTP/1.1 Enforcement Constraint:**
> The ESP32 embedded web server on the Pixoo 64 does not support HTTP/2 upgrades or ALPN negotiations. Modern HTTP clients (such as Java 21 `HttpClient`) that default to HTTP/2 will receive an **`HTTP 400 Bad Request`** error from the device. All client implementations **must explicitly force HTTP/1.1**.

---

## 2. Response Format

All commands sent to `/post` return a standard JSON object indicating the execution status.

### Success Response
```json
{
  "error_code": 0
}
```

### Error Response
If `error_code` is non-zero, the command failed or contained invalid parameters:
```json
{
  "error_code": 1
}
```

---

## 3. Frame Buffer & Pixel Format

The Pixoo 64 has a 64x64 RGB LED matrix (4,096 total pixels).

### Binary Buffer Layout

* **Dimensions:** 64 columns × 64 rows
* **Color Depth:** 24-bit TrueColor (8 bits Red, 8 bits Green, 8 bits Blue)
* **Bytes per Pixel:** 3 bytes (`[R, G, B]`)
* **Total Buffer Size:** 64 × 64 × 3 = **12,288 bytes**
* **Scanline Order:** Top-to-bottom, left-to-right (Row 0 Col 0..63, Row 1 Col 0..63, ..., Row 63 Col 0..63)

### Base64 Encoding
For commands expecting frame buffer data (such as `Draw/SendHttpGif`), the raw 12,288-byte buffer must be Base64-encoded into a standard ASCII string.

---

## 4. Command Reference

All commands are submitted as JSON objects containing a `"Command"` field.

---

### 4.1. `Channel/SetIndex`
Switches the display to a predefined device channel.

#### Request Payload
```json
{
  "Command": "Channel/SetIndex",
  "SelectIndex": 0
}
```

#### Channel Index Values
| `SelectIndex` | Channel Name | Description |
| :--- | :--- | :--- |
| `0` | Clock | Displays firmware clock faces / dials |
| `1` | Cloud | Displays cloud/community gallery animations |
| `2` | Visualizer | Audio spectrum / equalizer modes |
| `3` | Custom | Displays user-defined gallery or local HTTP drawing buffer |
| `4` | Black Screen | Turns screen display matrix off |

---

### 4.2. `Draw/ResetHttpGifId`
Resets the internal HTTP GIF animation buffer state machine on the device. Must be called before sending a new multi-frame GIF animation.

#### Request Payload
```json
{
  "Command": "Draw/ResetHttpGifId"
}
```

---

### 4.3. `Draw/SendHttpGif`
Sends a single frame of a GIF animation to the device's HTTP display buffer.

#### Request Payload
```json
{
  "Command": "Draw/SendHttpGif",
  "PicNum": 1,
  "PicWidth": 64,
  "PicOffset": 0,
  "PicID": 1001,
  "PicSpeed": 1000,
  "PicData": "<BASE64_ENCODED_12288_BYTE_RGB_BUFFER>"
}
```

#### Field Specifications
* `PicNum`: Total number of frames in the animation sequence.
* `PicWidth`: Canvas width (must be `64`).
* `PicOffset`: 0-indexed frame index in the sequence (`0` to `PicNum - 1`).
* `PicID`: Unique identifier for the animation sequence (incremented for each new animation).
* `PicSpeed`: Frame display duration in milliseconds (e.g. `1000` = 1 second).
* `PicData`: Base64 string of the raw 12,288-byte RGB pixel matrix.

---

### 4.4. `Draw/SendHttpText`
Renders hardware-accelerated text on the device's overlay layer.

#### Request Payload
```json
{
  "Command": "Draw/SendHttpText",
  "TextId": 1,
  "x": 0,
  "y": 24,
  "dir": 0,
  "font": 2,
  "TextWidth": 64,
  "speed": 80,
  "TextString": "HELLO",
  "color": "#00FFFF",
  "align": 2
}
```

#### Field Specifications
* `TextId`: Text layer identifier slot (`0` to `19`).
* `x`: X coordinate of the text container (`0` to `63`).
* `y`: Y coordinate of the text container (`0` to `63`).
* `dir`: Scroll direction:
  * `0`: Static (no scroll)
  * `1`: Scroll Left
  * `2`: Scroll Right
* `font`: ROM font index (`0` to `7`).
* `TextWidth`: Container width for alignment calculations (typically `64`).
* `speed`: Scroll speed in milliseconds per step (e.g. `50` to `100`).
* `TextString`: UTF-8 string content to render.
* `color`: Hexadecimal color string formatted as `"#RRGGBB"`.
* `align`: Alignment within `TextWidth`:
  * `1`: Left
  * `2`: Center
  * `3`: Right

---

### 4.5. `Draw/ClearHttpText`
Clears all active hardware text layers from the screen.

#### Request Payload
```json
{
  "Command": "Draw/ClearHttpText"
}
```

---

### 4.6. `Channel/SetBrightness`
Sets the matrix LED brightness.

#### Request Payload
```json
{
  "Command": "Channel/SetBrightness",
  "Brightness": 100
}
```

#### Field Specifications
* `Brightness`: Integer between `0` (darkest) and `100` (full brightness).

---

### 4.7. `Channel/OnOffScreen`
Toggles the screen backlight/LED display state.

#### Request Payload
```json
{
  "Command": "Channel/OnOffScreen",
  "OnOff": 1
}
```

#### Field Specifications
* `OnOff`: `1` for ON, `0` for OFF.

---

### 4.8. `Device/SetScreenRotationAngle`
Sets the physical screen rotation angle.

#### Request Payload
```json
{
  "Command": "Device/SetScreenRotationAngle",
  "Mode": 0
}
```

#### Field Specifications
* `Mode`:
  * `0`: Normal (0°)
  * `1`: 90° Clockwise
  * `2`: 180°
  * `3`: 270° Clockwise

---

### 4.9. `Device/PlayBuzzer`
Triggers a beep/tone sequence on the internal piezoelectric buzzer.

#### Request Payload
```json
{
  "Command": "Device/PlayBuzzer",
  "ActiveTimeInCycle": 500,
  "OffTimeInCycle": 500,
  "PlayTotalTime": 3000
}
```

#### Field Specifications
* `ActiveTimeInCycle`: Beep active duration in milliseconds per cycle.
* `OffTimeInCycle`: Silence duration in milliseconds per cycle.
* `PlayTotalTime`: Total alarm duration in milliseconds.

---

### 4.10. `Device/PlayTFGif`
Directs the device to download and play an animation from a remote HTTP URL.

#### Request Payload
```json
{
  "Command": "Device/PlayTFGif",
  "FileType": 2,
  "FileName": "http://example.com/animation.gif"
}
```

#### Field Specifications
* `FileType`: Set to `2` for remote URL fetching.
* `FileName`: Direct URL to a static or animated GIF file.

---

### 4.11. `Channel/GetAllConf`
Fetches the device's complete hardware and user configuration state.

#### Request Payload
```json
{
  "Command": "Channel/GetAllConf"
}
```

#### Response Fields (Partial List)
* `Brightness`: Current LED brightness (`0` to `100`).
* `LightSwitch`: Screen power state (`1` for ON, `0` for OFF).
* `RotationFlag`: Current screen rotation setting.
* `Mac`: Device MAC address.
* `CurClockId`: The `ClockId` of the currently configured Faces clock.

---

## 5. Device Discovery Protocol

The Pixoo 64 can be discovered on a local area network using two primary methods:

### 1. UDP Multicast / Broadcast Probing
* **Target Ports:** `5000`, `7000`, `3333`
* **Mechanism:** Send UDP discovery Datagrams across local subnet broadcast address (`255.255.255.255`).
* *Note:* Some routers or AP isolation settings block UDP broadcast packets.

### 2. ARP Table Scanning + HTTP API Probing
* **Mechanism:** Read the system ARP table (e.g. `arp -a`), extract active IP addresses on the local subnet, and issue a lightweight HTTP POST (`Channel/SetIndex`) probe to port `80` with a short connection timeout (~400ms).
* *Result:* Validated Pixoo 64 devices will respond with `{"error_code": 0}`.

---

## 6. Device Quirks & Execution Behaviors

1. **HTTP GIF Expiry & State Reversion:**
   When an HTTP animation is uploaded via `Draw/SendHttpGif`, the display switches to the HTTP buffer. Once the total duration of the animation frames expires (e.g., a single frame with a `PicSpeed` of 1000ms), the device **automatically reverts** to its previously active channel (such as Cloud Gallery or Clock). To keep an HTTP display static, send frames with long frame delays (e.g., `60000` ms).

2. **Text Overlay Interaction with Channel Switches:**
   Sending `Channel/SetIndex` cancels the active HTTP GIF buffer and wipes active native HTTP text layers. Furthermore, client libraries that automatically issue `Channel/SetIndex` when sending text will interrupt any currently playing HTTP background animation.

3. **HTTP Local Graphics Alternative:**
   Because firmware text overlays can interact unpredictably with active cloud channels, clients can render text locally onto a 64x64 bitmap in memory (e.g., via `java.awt.Graphics2D`) and send the resulting 12,288-byte RGB frame using `Draw/SendHttpGif`.

4. **Custom Channel Screen Flicker (The "Screen State Hack"):**
   The device actively ignores incoming `Draw/SendHttpGif` frames unless it is already switched to the Custom channel (`Channel/SetIndex: 3`). However, switching to the Custom channel *before* uploading the frames causes the screen to briefly flash whatever old frames were previously in the custom buffer.
   - **The Fix:** You must wrap the channel switch and frame upload in a screen toggle. First, read `LightSwitch` from `Channel/GetAllConf`. If the screen is ON, send `Channel/OnOffScreen: 0` to turn it off instantly (bypassing the slow hardware fade of `SetBrightness`). Wait ~250ms for the matrix to deactivate. Switch to the Custom channel (`Channel/SetIndex: 3`). Send your GIF frames. Wait another ~250ms for the device's internal display loop to register the new frames, and finally turn the screen back ON (`Channel/OnOffScreen: 1`).
