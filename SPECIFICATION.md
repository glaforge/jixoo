# Divoom Pixoo 64 HTTP Protocol Specification

This document provides a comprehensive, exhaustive specification of the Divoom Pixoo 64 local network HTTP API protocol, derived from network captures and reverse engineering of the official Divoom Android client application (`com.divoom.Divoom`).

---

## 1. Overview & Transport Layer

The Divoom Pixoo 64 communicates over local Wi-Fi via an embedded HTTP server running on an ESP32 microcontroller.

### Key Protocol Attributes

* **Transport Protocol:** HTTP / 1.1 (**Strict requirement**)
* **HTTP Method:** `POST`
* **API Endpoints:**
  * Standard REST control: `http://<DEVICE_IP>:80/post`
  * Divoom Mobile App Local API: `http://<DEVICE_IP>:9000/divoom_api`
  * Multipart binary upload endpoint: `http://<DEVICE_IP>:9000/upload`
* **Content-Type:** `application/json; charset=utf-8` (or `multipart/form-data` for port 9000 uploads)

> [!IMPORTANT]
> **HTTP/1.1 Enforcement & JSON Key Order Constraints:**
> 1. The ESP32 embedded web server on the Pixoo 64 does not support HTTP/2 upgrades or ALPN negotiations. Modern HTTP clients (such as Java 21 `HttpClient`) that default to HTTP/2 will receive an **`HTTP 400 Bad Request`** error from the device. All client implementations **must explicitly force HTTP/1.1**.
> 2. The Pixoo 64 firmware uses a lightweight C JSON parser (such as cJSON) that expects the `"Command"` key to be the **very first field** in the JSON request payload. Placing `"Command"` after large payload fields (e.g. after a 16KB Base64 `PicData` string) will cause parsing failures or silent command drops. All commands must serialize `@JsonPropertyOrder({"Command", ...})` first.

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

## 4. Local Command Reference

### 4.1. `Channel/SetIndex`
Switches the active display channel.

#### Request Payload
```json
{
  "Command": "Channel/SetIndex",
  "SelectIndex": 0
}
```

#### Field Specifications
* `SelectIndex`:
  * `0`: Clock Faces
  * `1`: Cloud Channel (user subscription galleries)
  * `2`: Visualizer / Equalizer (built-in audio animations)
  * `3`: Custom Channel (persistent playlist stored in flash)
  * `4`: Black Screen (turns off display output while keeping Wi-Fi active)

---

### 4.2. `Draw/ResetHttpGifId`
Resets the device's internal HTTP GIF animation receiver buffer and sequence state. Must be called before transmitting a new animation sequence.

#### Request Payload
```json
{
  "Command": "Draw/ResetHttpGifId"
}
```

---

### 4.3. `Draw/SendHttpGif`
Pushes a single frame of a 64x64 animation into the device's HTTP playback buffer.

#### Request Payload
```json
{
  "Command": "Draw/SendHttpGif",
  "PicNum": 1,
  "PicWidth": 64,
  "PicOffset": 0,
  "PicID": 1001,
  "PicSpeed": 1000,
  "PicData": "<BASE64_ENCODED_12288_BYTES>"
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
* `TextString`: UTF-8 text string to render.
* `color`: 6-digit hex color code (`#RRGGBB`).
* `align`: Text alignment (`1`: Left, `2`: Center, `3`: Right).

---

### 4.5. `Draw/ClearHttpText`
Clears all active native hardware text overlays.

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
Controls the physical screen LED power state (standby vs active).

#### Request Payload
```json
{
  "Command": "Channel/OnOffScreen",
  "OnOff": 1
}
```

#### Field Specifications
* `OnOff`:
  * `1`: Turn Screen **ON** (active matrix display)
  * `0`: Turn Screen **OFF** (standby / display asleep)

> [!NOTE]
> In official Divoom Android APK sources (`WifiChannelModel.java`, `DiscoverMainFragment.java`), `1` activates the screen and `0` puts it in standby. Some legacy community reverse-engineering notes inverted this; `jixoo64` implements the official firmware convention.

---

### 4.8. `Channel/GetOnOffScreen`
Queries the current screen LED power state directly without requiring a full configuration dump.

#### Request Payload
```json
{
  "Command": "Channel/GetOnOffScreen"
}
```

#### Response Payload
```json
{
  "error_code": 0,
  "OnOff": 1
}
```
* `OnOff`: `1` if screen is active/ON, `0` if screen is asleep/OFF.

---

### 4.9. `Device/SetScreenRotationAngle`
Sets the physical screen rotation orientation mode.

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
  * `2`: 180° (Inverted)
  * `3`: 270° Clockwise

> [!IMPORTANT]
> The field name is `"Mode"` and accepts values `0..3`. Passing degree values (`90`, `180`, `270`) directly to `Device/SetScreenRotationAngle` causes firmware parse rejection. Degrees (`0, 90, 180, 270`) are used in `Sys/DevUpdateConf` under the field `"GyrateAngle"`.

---

### 4.10. `Device/SetUTC`
Synchronizes the device's onboard real-time clock (RTC) directly with host time, bypassing cloud NTP synchronization.

#### Request Payload
```json
{
  "Command": "Device/SetUTC",
  "Utc": 1700000000,
  "Time": "2023-11-14 22:13:20"
}
```

#### Field Specifications
* `Utc`: Unix timestamp in epoch seconds (`long`).
* `Time`: Local host timestamp string in `"yyyy-MM-dd HH:mm:ss"` format.

---

### 4.11. Hardware Tools Engine (`Tools/...`)

The Pixoo 64 has a built-in suite of interactive utility tools.

#### 4.11.1. Stopwatch (`Tools/SetStopWatch`, `Tools/GetStopWatch`)
Controls the on-screen digital stopwatch display.

##### Start / Stop / Reset Request
```json
{
  "Command": "Tools/SetStopWatch",
  "Status": 1
}
```
* `Status`:
  * `0`: Stop / pause
  * `1`: Start / resume
  * `2`: Reset to 00:00

##### Query Status Request
```json
{
  "Command": "Tools/GetStopWatch"
}
```
##### Response
```json
{
  "error_code": 0,
  "Status": 1
}
```

---

#### 4.11.2. Countdown Timer (`Tools/SetTimer`, `Tools/GetTimer`)
Controls the on-screen countdown timer display.

##### Set & Run Timer Request
```json
{
  "Command": "Tools/SetTimer",
  "Minute": 5,
  "Second": 30,
  "Status": 1
}
```
* `Minute`: Initial timer minutes (`0..99`).
* `Second`: Initial timer seconds (`0..59`).
* `Status`: `1` to start countdown, `0` to stop/pause.

##### Query Timer Status Request
```json
{
  "Command": "Tools/GetTimer"
}
```
##### Response
```json
{
  "error_code": 0,
  "Minute": 4,
  "Second": 12,
  "Status": 1
}
```

---

#### 4.11.3. Scoreboard (`Tools/SetScoreBoard`, `Tools/GetScoreBoard`)
Displays a full-screen dual-team score tracker (Blue vs Red).

##### Set Scores Request
```json
{
  "Command": "Tools/SetScoreBoard",
  "BlueScore": 21,
  "RedScore": 17
}
```
* `BlueScore`: Score for the blue team (`0..999`).
* `RedScore`: Score for the red team (`0..999`).

##### Query Scores Request
```json
{
  "Command": "Tools/GetScoreBoard"
}
```
##### Response
```json
{
  "error_code": 0,
  "BlueScore": 21,
  "RedScore": 17
}
```

---

#### 4.11.4. Ambient Noise Decibel Meter (`Tools/SetNoiseStatus`, `Tools/GetNoiseStatus`)
Activates the device's internal microphone and displays real-time ambient noise decibel graphs.

##### Start / Stop Request
```json
{
  "Command": "Tools/SetNoiseStatus",
  "NoiseStatus": 1
}
```
* `NoiseStatus`: `1` to activate, `0` to deactivate.

##### Query Status Request
```json
{
  "Command": "Tools/GetNoiseStatus"
}
```
##### Response
```json
{
  "error_code": 0,
  "NoiseStatus": 1
}
```

---

#### 4.11.5. Pomodoro Timer (`Tools/SetTomato`, `Tools/SetTomatoStatus`)
Controls the built-in productivity Pomodoro timer on the LED matrix.

##### Configure Pomodoro
```json
{
  "Command": "Tools/SetTomato",
  "WorkTime": 25,
  "RestTime": 5
}
```
* `WorkTime`: Focus duration in minutes (e.g. `25`).
* `RestTime`: Rest/break duration in minutes (e.g. `5`).

##### Start / Stop Pomodoro
```json
{
  "Command": "Tools/SetTomatoStatus",
  "Status": 1
}
```
* `Status`: `1` to start the Pomodoro session, `0` to stop/pause.

---

#### 4.11.6. Alarm Management (`Tools/SetAlarm`, `Tools/GetAlarm`, `Tools/DelAlarm`)
Manages the hardware alarms stored on the device.

##### Query Alarms
```json
{
  "Command": "Tools/GetAlarm"
}
```
Response:
```json
{
  "error_code": 0,
  "AlarmList": [
    {
      "AlarmId": 1,
      "Hour": 7,
      "Minute": 30,
      "Days": 31,
      "Status": 1
    }
  ]
}
```

##### Set Alarm
```json
{
  "Command": "Tools/SetAlarm",
  "AlarmId": 1,
  "Hour": 7,
  "Minute": 30,
  "Days": 31,
  "Status": 1
}
```
* `AlarmId`: Slot ID (`1..N`).
* `Hour`: Hour (`0..23`).
* `Minute`: Minute (`0..59`).
* `Days`: Bitmask representing repeating weekdays (bit 0 = Mon, ..., bit 6 = Sun; `127` = all days, `31` = weekdays).
* `Status`: `1` for enabled, `0` for disabled.

##### Delete Alarm
```json
{
  "Command": "Tools/DelAlarm",
  "AlarmId": 1
}
```

---

#### 4.11.7. Target Date / Memorial Countdown (`Tools/SetMemorialDay`, `Tools/DelMemorialDay`)
Displays a visual countdown to a specified target annual date or event.

##### Set Memorial Countdown
```json
{
  "Command": "Tools/SetMemorialDay",
  "Id": 1,
  "MemorialMoon": 12,
  "MemorialDay": 25,
  "MemorialTime": 0,
  "MemorialName": "Christmas"
}
```
* `Id`: Slot ID (`1..N`).
* `MemorialMoon`: Target month (`1..12`).
* `MemorialDay`: Target day of month (`1..31`).
* `MemorialTime`: Time of day in elapsed minutes (`hour * 60 + minute`).
* `MemorialName`: Label displayed on the matrix.

##### Delete Memorial Countdown
```json
{
  "Command": "Tools/DelMemorialDay",
  "Id": 1
}
```

---


### 4.12. System Configuration (`Sys/...`)

#### 4.12.1. `Sys/GetConf`
Retrieves device-wide hardware, display, and regional settings.

##### Request
```json
{
  "Command": "Sys/GetConf"
}
```

##### Response Fields
```json
{
  "error_code": 0,
  "Time24Flag": 1,
  "TemperatureMode": 0,
  "DateFormat": 1,
  "MirrorFlag": 0,
  "AutoPowerOff": 30,
  "GyrateAngle": 0,
  "HighLight": 1,
  "WhiteBalanceR": 100,
  "WhiteBalanceG": 100,
  "WhiteBalanceB": 100,
  "Language": 0,
  "NotificationSound": 1,
  "OnOffVolume": 10,
  "BluetoothAutoConnect": 1,
  "DeviceAutoUpdate": 1
}
```
* `Time24Flag`: `0` for 12-hour AM/PM clock, `1` for 24-hour clock.
* `TemperatureMode`: `0` for Celsius (°C), `1` for Fahrenheit (°F).
* `DateFormat`: Date formatting mode index (`0`: `yyyy-MM-dd`, `1`: `dd-MM-yyyy`, `2`: `MM-dd-yyyy`, etc.).
* `MirrorFlag`: `0` for normal display, `1` for horizontally mirrored display.
* `AutoPowerOff`: Idle sleep timer in minutes (`0` = disabled).
* `GyrateAngle`: Physical rotation angle (`0`, `90`, `180`, `270`).
* `HighLight`: Highlight display mode flag.
* `WhiteBalanceR`, `WhiteBalanceG`, `WhiteBalanceB`: RGB channel color balance calibration (`0..100`).
* `Language`: Firmware language index.

---

#### 4.12.2. `Sys/DevUpdateConf` / `Sys/SetConf`
Updates device system configuration parameters.

##### Request Payload
```json
{
  "Command": "Sys/DevUpdateConf",
  "Time24Flag": 1,
  "TemperatureMode": 0,
  "DateFormat": 1,
  "MirrorFlag": 0,
  "AutoPowerOff": 0
}
```

---

### 4.13. Channel Configuration & Management

#### 4.13.1. `Channel/GetConfig`
Returns timing intervals and auto-rotation settings for channels.

##### Request
```json
{
  "Command": "Channel/GetConfig"
}
```

##### Response Fields
```json
{
  "error_code": 0,
  "ChannelIndex": 0,
  "ClockTime": 15,
  "GalleryTime": 60,
  "SingleGalleyTime": 10,
  "RotationFlag": 0,
  "StartUpClockId": 1
}
```
* `ChannelIndex`: Active channel index.
* `ClockTime`: Display duration for clock faces in seconds.
* `GalleryTime`: Slideshow rotation interval for gallery animations in seconds.
* `RotationFlag`: Auto-rotation between channels (`0` = off, `1` = on).
* `StartUpClockId`: Clock face ID loaded on boot.

---

#### 4.13.2. `Channel/SetStartupChannel` & `Channel/GetStartupChannel`
Configures or reads the default channel displayed when the device powers on.

##### Set Startup Channel
```json
{
  "Command": "Channel/SetStartupChannel",
  "ChannelIndex": 0
}
```

##### Get Startup Channel
```json
{
  "Command": "Channel/GetStartupChannel"
}
```
Response: `{"error_code": 0, "ChannelIndex": 0}`

---

#### 4.13.3. `Channel/SetClockSelectId` & `Channel/GetClockInfo`
Selects an active clock face by its catalog ID or inspects the active clock.

##### Select Clock Face
```json
{
  "Command": "Channel/SetClockSelectId",
  "ClockId": 42
}
```

##### Get Clock Info
```json
{
  "Command": "Channel/GetClockInfo"
}
```
Response: `{"error_code": 0, "ClockId": 42, "Brightness": 100, "LcdIndex": 0, "ProduceTime": 0}`

---

#### 4.13.4. `Channel/SetCustomPageIndex` & `Channel/GetCustomPageIndex`
Selects which custom channel playlist slot (`0`, `1`, or `2`) is active.

##### Set Page Index
```json
{
  "Command": "Channel/SetCustomPageIndex",
  "CustomPageIndex": 1
}
```

##### Get Page Index
```json
{
  "Command": "Channel/GetCustomPageIndex"
}
```
Response: `{"error_code": 0, "CustomPageIndex": 1}`

---

### 4.14. `Device/GetStorageStatus`
Checks whether the onboard SPI flash memory partition for storing user pixel art and animations is full.

#### Request
```json
{
  "Command": "Device/GetStorageStatus"
}
```

#### Response
```json
{
  "error_code": 0,
  "Full": 0
}
```
* `Full`: `1` if onboard storage is full, `0` if storage space is available.

---

### 4.15. `Device/PlayBuzzer`
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

### 4.16. `Device/PlayTFGif`
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

> [!WARNING]
> **Hardware OOM & Firmware Crash Hazard:**
> The ESP32 microcontroller in the Pixoo 64 has limited SRAM (~320KB allocatable) and cannot handle on-chip decoding of high-resolution GIF files (e.g. 1400x896) or complex TLS certificate chains over HTTPS. Attempting to point `Device/PlayTFGif` to large internet GIFs causes memory allocation panics and reboots the device.
> 
> **Best Practice:** Client libraries should download remote GIF URLs on the host computer, decode and downscale each frame to 64x64, and stream raw 12,288-byte RGB frames using `Draw/SendHttpGif`. The `jixoo64` client and CLI implement this safe client-side pipeline by default.

---

### 4.17. Sleep Timer / Delayed Power Off (`Device/SetDelayPowerOff`, `Device/GetDelayPowerOff`)
Configures or inspects a countdown timer after which the display automatically enters sleep/standby mode.

#### Set Sleep Timer
```json
{
  "Command": "Device/SetDelayPowerOff",
  "DelayTime": 30
}
```
* `DelayTime`: Countdown duration in minutes (`0` cancels/disables the sleep timer).

#### Get Sleep Timer Status
```json
{
  "Command": "Device/GetDelayPowerOff"
}
```
Response:
```json
{
  "error_code": 0,
  "DelayTime": 30
}
```
* `DelayTime`: Remaining minutes until automatic power-off (`0` indicates timer is inactive).

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

4. **Custom Channel vs HTTP Buffer (`Channel/SetIndex: 3`):**
   Calling `Channel/SetIndex: 3` switches the device to its internal **Divoom App Custom Gallery preset** stored in flash memory. If no custom gallery is configured via the Divoom mobile app, switching to index `3` results in a **black screen**. 
   - Client applications drawing via `Draw/SendHttpGif` or `Draw/SendHttpText` do not require switching to channel `3`, as HTTP drawing commands render directly on top of the active display buffer.

5. **Frame Pacing & Rate Limiting for `Draw/SendHttpGif`:**
   Because each 64x64 RGB frame is ~16KB in Base64 JSON, bursting dozens of frame payloads as fast as possible will overwhelm the embedded ESP32 web server buffer. This leads to TCP packet truncation and the device returning `"Request data illegal json"`. Client implementations **must introduce a short delay (e.g. 20ms to 30ms)** between successive HTTP POST frame uploads.

6. **Maximum HTTP GIF Frame Capacity:**
   The internal memory buffer for HTTP GIF animations on the Pixoo 64 is capped at **~60 frames**. Uploading animations with more than 60 frames can result in memory overruns and command rejections (`"Request data illegal json"`). Longer animations should be downsampled (e.g. to 5-10 fps) or split into segments before transmission.

7. **String Error Responses vs Numeric Error Codes:**
   When receiving invalid JSON or unknown commands, the Pixoo 64 firmware does not always return an integer error code like `{"error_code": 1}`. Instead, it frequently returns string error descriptions in the error field:
   ```json
   {
     "error_code": "Request data illegal json"
   }
   ```
   Client JSON parsers must implement lenient deserialization for `error_code` that accepts both numeric integers and string messages to prevent unhandled deserialization crashes.

8. **Hardware Alarm Endpoint Naming:**
   While tools endpoints follow the `Tools/...` namespace, hardware alarms use a mixed naming convention on Pixoo 64 firmware:
   * Setting an alarm: `Device/SetAlarm` (calling `Alarm/Set` returns `"Request data illegal json"`).
   * Querying alarms: `Device/GetAlarm` (calling `Alarm/Get` returns `"Request data illegal json"`).
   * Deleting an alarm: `Alarm/Del` (under the `Alarm/` namespace).

9. **Direct Remote GIF Playback Crash Risk:**
   Directing the ESP32 to fetch remote GIF URLs via `Device/PlayTFGif` frequently crashes the firmware due to limited RAM when decoding large frames (e.g. 1400x896) or handling HTTPS certificate validation. Client-side downloading, scaling to 64x64, and streaming via `Draw/SendHttpGif` avoids these firmware panics completely.

---

## 7. Divoom Cloud & Social API Protocol

The Divoom mobile ecosystem connects to central cloud REST endpoints hosted at `https://app.divoom-gz.com/Device_Info/`.

### 7.1. Pagination Rules & Conventions
* **1-Based Indexing:** Endpoints such as `GetCategoryFileListV2`, `SearchGalleryV3`, and `GetSomeoneListV3` use `StartNum` and `EndNum`.
  > [!IMPORTANT]
  > `StartNum` **must be $\ge 1$**. Passing `StartNum: 0` causes the cloud server to return an empty item array (`{"ReturnCode": 0, "FileList": []}`).
* **File Dimension Bitmask:** For Pixoo 64 (64x64 matrix), the query parameter `FileSize` should be set to `127` (all sizes) or `64`.

---

### 7.2. Public Gallery Discovery (`GetCategoryFileListV2`)
Fetches public community pixel art categorized by popularity, recency, or total likes.

* **Endpoint:** `POST https://app.divoom-gz.com/Device_Info/Channel/GetCategoryFileListV2`
* **Request Body:**
```json
{
  "StartNum": 1,
  "EndNum": 20,
  "SortType": 1,
  "Classify": 0,
  "FileSize": 127,
  "FileType": 5,
  "FileSort": 0,
  "RefreshIndex": 0,
  "Version": 19
}
```
* `SortType`: `0` for Newest, `1` for Hot / Most Popular, `2` for Most Liked.
* `Classify`: Category filter (`0` for All, `1` Characters, `2` Animals, etc.).

---

### 7.3. Gallery Search (`SearchGalleryV3`)
Searches community pixel art by keyword or tag.

* **Endpoint:** `POST https://app.divoom-gz.com/Device_Info/Channel/SearchGalleryV3`
* **Request Body:**
```json
{
  "StartNum": 1,
  "EndNum": 20,
  "SearchKey": "cyberpunk",
  "FileSize": 127,
  "FileType": 5,
  "FileSort": 0,
  "RefreshIndex": 0,
  "Version": 19
}
```

---

### 7.4. Artist Portfolio & Profile (`GetSomeoneListV3`, `GetSomeoneInfoV2`)
Discovers artworks published by specific community artists, user uploads, or user favorites.

#### Fetch Artist Artworks
* **Endpoint:** `POST https://app.divoom-gz.com/Device_Info/Channel/GetSomeoneListV3`
* **Request Body:**
```json
{
  "StartNum": 1,
  "EndNum": 20,
  "SomeOneUserId": 12345678,
  "ShowAllFlag": 1,
  "Classify": 0,
  "FileSize": 127,
  "FileType": 5,
  "FileSort": 0,
  "RefreshIndex": 0,
  "Version": 19
}
```

#### Fetch Artist Profile
* **Endpoint:** `POST https://app.divoom-gz.com/Device_Info/Channel/GetSomeoneInfoV2`
* **Request Body:** `{"UserId": 12345678}`
* **Response Keys:** Returns `NickName`, `UserNewSign` (bio), `FansCnt`, `FollowCnt`, `LikeCnt`, `HeadUrl` (avatar), and `WorkCnt`.

---

### 7.5. Clock Face Store (`Channel/StoreTop20`, `Channel/StoreGetClassifyList`)
Discovers curated and trending clock dial faces created for the Pixoo 64.

* **Endpoints:**
  * `POST https://app.divoom-gz.com/Device_Info/Channel/StoreTop20`: Retrieves top 20 downloaded clock faces.
  * `POST https://app.divoom-gz.com/Device_Info/Channel/StoreGetClassifyList`: Retrieves clock faces filtered by category.

---

## 8. Divoom Zero-Authentication Weather Proxy

Divoom devices fetch weather and multi-day forecasts via a public cache proxy without requiring an OpenWeatherMap API key.

* **Endpoint:** `GET https://wea.divoom-gz.com/OpenWeatherMapCache.php`
* **Query Parameters:**
  * `action`: `current_weather` or `forecast_weather`
  * `longitude`: Decimal longitude (e.g. `2.3522`)
  * `latitude`: Decimal latitude (e.g. `48.8566`)
  * `mode`: `base64` (the server returns the JSON payload wrapped in a Base64 string)
  * `version`: `1`

---

## 9. Divoom Native Binary Asset Format & Codec

Artworks stored on Divoom Cloud servers are often serialized as custom binary `.bin` files rather than standard GIF or PNG files.

### 9.1. Format Structure (Format 26 / Format 18)
* **Magic Header:** Format identifier byte (typically `0x1A` = 26 or `0x12` = 18).
* **Palette:**
  * The header defines the number of color entries in the indexed palette (e.g., 256 colors).
  * Each palette entry consists of 3 RGB bytes (`[R, G, B]`).
* **Frame Speed:** 2 bytes indicating frame duration in milliseconds.

### 9.2. 16x16 Tiled Macroblock Layout
Unlike conventional image formats that store pixels in consecutive scanlines from row 0 to 63:
* Divoom 64x64 binary assets divide the matrix into **16 macroblock tiles of 16x16 pixels**, arranged in a 4x4 grid.
* Within each 16x16 tile, pixels are indexed row-major from `0..15` and `0..15`.
* To convert the decoded palette indices into a standard 12,288-byte RGB frame for `Draw/SendHttpGif`:
  $$\text{Global } X = (\text{tileIndex} \pmod 4) \times 16 + \text{tile } x$$
  $$\text{Global } Y = (\lfloor\text{tileIndex} / 4\rfloor) \times 16 + \text{tile } y$$
  $$\text{Buffer Offset} = (\text{Global } Y \times 64 + \text{Global } X) \times 3$$

`jixoo64` implements a pure Java 21 decoder (`DivoomAssetDecoder`) requiring zero external dependencies to unpack and untangle these binary assets directly into displayable RGB buffers.

### 9.3. Pure Java Animated GIF89a Encoder (`GifEncoder`)
To convert decoded Divoom assets or arbitrary `PixooAnimation` objects into universally compatible animated GIF files, `jixoo64` implements a standards-compliant GIF89a encoder:
* **Zero Native/C Dependencies:** Fully implemented in standard Java without external LZO or native image libraries.
* **Pure Java LZW Compression:** Implements variable-length code LZW stream compression (12-bit max code table, clear code and end-of-information code handling).
* **Median-Cut Color Quantization:** Frames containing more than 256 colors are automatically quantized down to 256 indexed palette entries using a recursive median-cut partitioning algorithm.
* **Per-Frame Timing & Netscape Looping:** Encodes exact per-frame millisecond delays into Graphic Control Extensions and includes the Netscape 2.0 application block for continuous looping.

### 9.4. Aspect Ratio Scaling Modes (`ScaleMode`)
Because the Pixoo 64 physical display is strictly a $64 \times 64$ square matrix ($1:1$ aspect ratio), rectangular images and videos must be mapped onto the canvas:
* `FIT_CENTER` (default): Scales the image by $\min(64 / W, 64 / H)$ to fit entirely within the canvas, padding empty borders with black pixels ($0, 0, 0$).
* `FILL_CROP` (`--crop`): Scales the image by $\max(64 / W, 64 / H)$ so that the shorter dimension fills 64 pixels, centering the excess and cropping it away with zero letterbox bars.
* `STRETCH`: Stretches the image directly to $64 \times 64$ without preserving the original aspect ratio.


