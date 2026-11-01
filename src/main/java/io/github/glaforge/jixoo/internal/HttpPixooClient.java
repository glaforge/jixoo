/*
 * Copyright 2026 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.glaforge.jixoo.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.glaforge.jixoo.api.*;
import io.github.glaforge.jixoo.api.exception.PixooException;
import io.github.glaforge.jixoo.api.exception.UnsupportedCommandException;
import io.github.glaforge.jixoo.discovery.PixooModelProbe;
import io.github.glaforge.jixoo.model.PixooAnimation;
import io.github.glaforge.jixoo.model.PixooFrame;
import io.github.glaforge.jixoo.model.command.PixooCommand;
import io.github.glaforge.jixoo.model.sys.ChannelConfig;
import io.github.glaforge.jixoo.model.sys.SysConfig;
import io.github.glaforge.jixoo.model.tool.NoiseStatus;
import io.github.glaforge.jixoo.model.tool.PixooAlarm;
import io.github.glaforge.jixoo.model.tool.ScoreboardStatus;
import io.github.glaforge.jixoo.model.tool.StopwatchAction;
import io.github.glaforge.jixoo.model.tool.StopwatchStatus;
import io.github.glaforge.jixoo.model.tool.TimerStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * HTTP-based implementation of {@link PixooClient}.
 */
public class HttpPixooClient implements PixooClient {
    private static final Logger log = LoggerFactory.getLogger(HttpPixooClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    private static final AtomicInteger PIC_ID_GENERATOR = new AtomicInteger(new Random().nextInt(10000) + 1);
    private static final DateTimeFormatter UTC_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Reply of the Pixoo-64 II to a command it does not know
    private static final String UNKNOWN_COMMAND_MESSAGE = "Only accept JSON parameters";
    // Fields every reply may carry, whatever the command: a reply with nothing else holds no data
    private static final Set<String> ENVELOPE_FIELDS = Set.of(
            "error_code", "error_message",
            "Command", "DeviceId", "PacketFlag", "DeviceType", "ReturnCode", "ReturnMessage", "WifiSingal");

    private final String ipAddress;
    private final Integer port;
    private final Duration connectTimeout;
    // Resolved on first use when the model is detected
    private volatile PixooModel model;
    private volatile String targetUri;
    private final HttpClient httpClient;
    // The device serves one request at a time and garbles back-to-back requests: send them one by one, spaced out
    private static final long MIN_REQUEST_INTERVAL_NANOS = Duration.ofMillis(30).toNanos();
    private final ReentrantLock requestLock = new ReentrantLock(true);
    private long lastRequestEndNanos;
    private final Duration requestTimeout;
    private final boolean autoSwitchToCustomChannel;

    /**
     * Constructs a new HttpPixooClient for an original Pixoo 64.
     *
     * @param ipAddress                 the IP address of the device
     * @param port                      the HTTP port to connect to
     * @param connectTimeout            the connection timeout duration
     * @param requestTimeout            the request timeout duration
     * @param autoSwitchToCustomChannel whether to automatically switch to the custom channel when sending content
     */
    public HttpPixooClient(String ipAddress, int port, Duration connectTimeout, Duration requestTimeout, boolean autoSwitchToCustomChannel) {
        this(PixooModel.PIXOO_64, ipAddress, port, connectTimeout, requestTimeout, autoSwitchToCustomChannel);
    }

    /**
     * Constructs a new HttpPixooClient.
     *
     * @param model                     the device model, which sets the API path and response envelope
     * @param ipAddress                 the IP address of the device
     * @param port                      the HTTP port to connect to
     * @param connectTimeout            the connection timeout duration
     * @param requestTimeout            the request timeout duration
     * @param autoSwitchToCustomChannel whether to automatically switch to the custom channel when sending content
     */
    public HttpPixooClient(PixooModel model, String ipAddress, int port, Duration connectTimeout, Duration requestTimeout, boolean autoSwitchToCustomChannel) {
        this(Objects.requireNonNull(model, "model"), ipAddress, (Integer) port, connectTimeout, requestTimeout, autoSwitchToCustomChannel);
    }

    /**
     * Creates a client that detects the device model on first use (see {@link PixooModelProbe}).
     *
     * @param ipAddress                 the IP address of the device
     * @param port                      the HTTP port, or {@code null} to try each model's default port
     * @param connectTimeout            the connection timeout duration
     * @param requestTimeout            the request timeout duration
     * @param autoSwitchToCustomChannel whether to automatically switch to the custom channel when sending content
     * @return a client whose model is detected on the first command
     */
    public static HttpPixooClient detecting(String ipAddress, Integer port, Duration connectTimeout, Duration requestTimeout, boolean autoSwitchToCustomChannel) {
        return new HttpPixooClient(null, ipAddress, port, connectTimeout, requestTimeout, autoSwitchToCustomChannel);
    }

    private HttpPixooClient(PixooModel model, String ipAddress, Integer port, Duration connectTimeout, Duration requestTimeout, boolean autoSwitchToCustomChannel) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.connectTimeout = connectTimeout;
        if (model != null) {
            useModel(model, port != null ? port : model.defaultPort());
        }
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(connectTimeout)
                .build();
        this.requestTimeout = requestTimeout;
        this.autoSwitchToCustomChannel = autoSwitchToCustomChannel;
    }

    @Override
    public PixooResponse executeCommand(PixooCommand command) {
        return executeCommand(command, PixooResponse.class);
    }

    /**
     * Executes a command against the device and maps the response to the specified class.
     *
     * @param command      the command to execute
     * @param responseType the response model class
     * @param <T>          the response type
     * @return deserialized response
     */
    public <T> T executeCommand(PixooCommand command, Class<T> responseType) {
        try {
            String uri = endpointUri();
            Optional<String> unsupported = model().unsupportedReason(command.command());
            if (unsupported.isPresent()) {
                throw new UnsupportedCommandException(command.command(),
                        "Command " + command.command() + " is not supported by this device (" + model() + "): " + unsupported.get());
            }
            String jsonBody = MAPPER.writeValueAsString(command);
            log.debug("Sending JSON command to {}: {}", uri, jsonBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .timeout(requestTimeout)
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = send(request);

            if (response.statusCode() != 200) {
                throw new PixooException("Pixoo API HTTP Error: Status " + response.statusCode() + ", Body: " + response.body());
            }

            JsonNode reply = MAPPER.readTree(response.body());
            PixooResponse status = MAPPER.treeToValue(reply, PixooResponse.class);
            if (reply.has("ReturnCode") && UNKNOWN_COMMAND_MESSAGE.equals(status.errorMessage())) {
                throw new UnsupportedCommandException(command.command(),
                        "Command " + command.command() + " is not supported by this device (" + model() + ")");
            }
            if (responseType == PixooResponse.class) {
                return responseType.cast(status);
            }
            if (!status.isSuccess()) {
                throw new PixooException("Command " + command.command() + " failed with error code "
                        + status.errorCode() + (status.errorMessage() != null ? ": " + status.errorMessage() : ""));
            }
            if (reply.has("ReturnCode") && !hasPayload(reply)) {
                throw new UnsupportedCommandException(command.command(),
                        "Command " + command.command() + " returned no data on this device (" + model() + ")");
            }
            return MAPPER.treeToValue(reply, responseType);

        } catch (PixooException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PixooException("Command execution interrupted: " + command.command(), e);
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            throw new PixooException("Failed to execute command " + command.command() + ": " + msg, e);
        }
    }

    private HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
        requestLock.lock();
        try {
            long wait = lastRequestEndNanos + MIN_REQUEST_INTERVAL_NANOS - System.nanoTime();
            if (lastRequestEndNanos != 0 && wait > 0) {
                TimeUnit.NANOSECONDS.sleep(wait);
            }
            try {
                return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } finally {
                lastRequestEndNanos = System.nanoTime();
            }
        } finally {
            requestLock.unlock();
        }
    }

    private static boolean hasPayload(JsonNode reply) {
        for (Iterator<String> names = reply.fieldNames(); names.hasNext(); ) {
            if (!ENVELOPE_FIELDS.contains(names.next())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public PixooModel model() {
        endpointUri();
        return model;
    }

    private String endpointUri() {
        if (targetUri == null) {
            synchronized (this) {
                if (targetUri == null) {
                    PixooModelProbe.Detection detection = PixooModelProbe.detect(ipAddress, port, connectTimeout)
                            .orElseThrow(() -> new PixooException("No Pixoo device answered at " + ipAddress
                                    + (port != null ? ":" + port : " (tried ports 80 and 9000)")
                                    + "; check the address, or set the model explicitly"));
                    log.debug("Detected {} at {}:{}", detection.model(), ipAddress, detection.port());
                    useModel(detection.model(), detection.port());
                }
            }
        }
        return targetUri;
    }

    private void useModel(PixooModel model, int port) {
        this.model = model;
        this.targetUri = "http://" + ipAddress + ":" + port + model.apiPath();
    }

    @Override
    public void close() {
        if (httpClient != null) {
            httpClient.close();
        }
    }

    @Override
    public PixooResponse selectChannel(PixooChannel channel) {
        return executeCommand(new PixooCommand.ChannelIndexCommand(channel.index()));
    }

    @Override
    public PixooResponse resetAnimationBuffer() {
        return executeCommand(new PixooCommand.ResetGifCommand());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ScreenStateResponse(@JsonProperty("OnOff") Integer onOff) {}

    @Override
    public boolean isScreenOn() {
        ScreenStateResponse resp = executeCommand(new PixooCommand.GetScreenStateCommand(), ScreenStateResponse.class);
        return resp.onOff() == null || resp.onOff() == 1;
    }

    private int prepareCustomChannel() {
        if (!autoSwitchToCustomChannel) return -1;

        boolean wasScreenOn;
        try {
            wasScreenOn = isScreenOn();
        } catch (PixooException e) {
            log.warn("Failed to get screen state, assuming it is on: {}", e.getMessage());
            wasScreenOn = true;
        }
        if (!wasScreenOn) {
            setScreenState(true);
            try { Thread.sleep(250); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        selectChannel(PixooChannel.CUSTOM);
        return wasScreenOn ? 1 : 0;
    }

    private void restoreScreen(int originalState) {
        // Do not issue Channel/OnOffScreen after uploading, as it wipes the HTTP buffer.
    }

    @Override
    public PixooResponse sendAnimation(PixooAnimation animation) {
        int originalState = prepareCustomChannel();
        resetAnimationBuffer();

        int totalFrames = animation.frameCount();
        int picId = PIC_ID_GENERATOR.incrementAndGet();
        PixooResponse lastResponse = new PixooResponse(0);
        if (totalFrames > 1 && model() == PixooModel.PIXOO_64_II) {
            // The Pixoo-64 II takes ~1.5 s to acknowledge each frame
            log.info("Uploading {} frames to the Pixoo-64 II, about {} s...", totalFrames, Math.round(totalFrames * 1.5));
        }

        for (int i = 0; i < totalFrames; i++) {
            PixooFrame frame = animation.frames().get(i);
            String base64Data = frame.toBase64();

            PixooCommand.SendGifCommand command = new PixooCommand.SendGifCommand(
                    totalFrames,
                    i,
                    picId,
                    totalFrames == 1 ? model().stillFrameDelayMs(frame.delayMs()) : frame.delayMs(),
                    base64Data
            );

            lastResponse = executeCommand(command);
            if (!lastResponse.isSuccess()) {
                log.warn("Frame {} of {} failed with error code {}", i, totalFrames, lastResponse.errorCode());
                break;
            }
            log.debug("Frame {} of {} sent", i + 1, totalFrames);
        }

        try { Thread.sleep(250); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        restoreScreen(originalState);
        return lastResponse;
    }

    @Override
    public PixooResponse sendRemoteGifUrl(String gifUrl) {
        if (model() == PixooModel.PIXOO_64_II) {
            // The Pixoo-64 II downloads the URL but never displays it: decode it here and stream the frames instead
            log.info("The Pixoo-64 II cannot play remote GIF URLs itself; downloading and streaming {} instead", gifUrl);
            return sendGifUrl(gifUrl);
        }
        int originalState = prepareCustomChannel();
        PixooResponse response = executeCommand(new PixooCommand.RemoteGifCommand(gifUrl));
        try { Thread.sleep(250); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        restoreScreen(originalState);
        return response;
    }

    @Override
    public PixooResponse sendText(PixooText text) {
        int originalState = prepareCustomChannel();
        PixooResponse response = executeCommand(new PixooCommand.SendTextCommand(
                text.textId(),
                text.x(),
                text.y(),
                text.dir(),
                text.font(),
                text.textWidth(),
                text.speed(),
                text.textString(),
                text.color(),
                text.align()
        ));
        try { Thread.sleep(250); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        restoreScreen(originalState);
        return response;
    }

    @Override
    public PixooResponse clearText() {
        return executeCommand(new PixooCommand.ClearTextCommand());
    }

    @Override
    public PixooResponse setBrightness(int brightness) {
        int clamped = Math.max(0, Math.min(100, brightness));
        return executeCommand(new PixooCommand.BrightnessCommand(clamped));
    }

    @Override
    public PixooResponse setScreenState(boolean on) {
        return executeCommand(new PixooCommand.ScreenStateCommand(on ? 1 : 0));
    }

    @Override
    public PixooResponse setRotation(PixooRotation rotation) {
        return executeCommand(new PixooCommand.RotationCommand(rotation.mode()));
    }

    @Override
    public PixooResponse playBuzzer(int activeMs, int offMs, int totalMs) {
        return executeCommand(new PixooCommand.PlayBuzzerCommand(activeMs, offMs, totalMs));
    }

    @Override
    public PixooResponse syncTime(Instant instant) {
        long epochSecond = instant.getEpochSecond();
        String formatted = UTC_DATE_FORMATTER
                .withZone(ZoneId.systemDefault())
                .format(instant);
        return executeCommand(new PixooCommand.SetUtcCommand(epochSecond, formatted));
    }

    @Override
    public SysConfig getSystemConfig() {
        return executeCommand(new PixooCommand.GetSysConfigCommand(), SysConfig.class);
    }

    @Override
    public PixooResponse setSystemConfig(SysConfig config) {
        return executeCommand(new PixooCommand.SetSysConfigCommand(
                config.time24Flag(),
                config.temperatureMode(),
                config.dateFormat(),
                config.mirrorFlag(),
                config.autoPowerOff(),
                config.gyrateAngle(),
                config.highLight(),
                config.language(),
                config.notificationSound(),
                config.onOffVolume(),
                config.bluetoothAutoConnect(),
                null,
                null
        ));
    }

    @Override
    public ChannelConfig getChannelConfig() {
        return executeCommand(new PixooCommand.GetDeviceConfigCommand(), ChannelConfig.class);
    }

    @Override
    public PixooResponse setStartupChannel(PixooChannel channel) {
        return executeCommand(new PixooCommand.SetStartupChannelCommand(channel.index()));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record StartupChannelResponse(@JsonProperty("ChannelIndex") Integer channelIndex) {}

    @Override
    public PixooChannel getStartupChannel() {
        StartupChannelResponse resp = executeCommand(new PixooCommand.GetStartupChannelCommand(), StartupChannelResponse.class);
        int idx = resp.channelIndex() != null ? resp.channelIndex() : 0;
        return PixooChannel.fromIndex(idx);
    }

    @Override
    public PixooResponse setClockId(int clockId) {
        return executeCommand(new PixooCommand.SetClockSelectIdCommand(clockId));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ClockInfoResponse(@JsonProperty("ClockId") Integer clockId) {}

    @Override
    public int getClockId() {
        ClockInfoResponse resp = executeCommand(new PixooCommand.GetClockInfoCommand(), ClockInfoResponse.class);
        return resp.clockId() != null ? resp.clockId() : 0;
    }

    @Override
    public PixooResponse setCustomPageIndex(int pageIndex) {
        if (pageIndex < 0 || pageIndex > 2) {
            throw new IllegalArgumentException("Custom page index must be 0, 1, or 2");
        }
        return executeCommand(new PixooCommand.SetCustomPageIndexCommand(pageIndex));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CustomPageIndexResponse(@JsonProperty("CustomPageIndex") Integer customPageIndex) {}

    @Override
    public int getCustomPageIndex() {
        CustomPageIndexResponse resp = executeCommand(new PixooCommand.GetCustomPageIndexCommand(), CustomPageIndexResponse.class);
        return resp.customPageIndex() != null ? resp.customPageIndex() : 0;
    }

    @Override
    public PixooResponse setStopwatch(StopwatchAction action) {
        return executeCommand(new PixooCommand.SetStopWatchCommand(action.value()));
    }

    @Override
    public StopwatchStatus getStopwatch() {
        return executeCommand(new PixooCommand.GetStopWatchCommand(), StopwatchStatus.class);
    }

    @Override
    public PixooResponse setTimer(int minute, int second, boolean start) {
        return executeCommand(new PixooCommand.SetTimerCommand(minute, second, start ? 1 : 0));
    }

    @Override
    public TimerStatus getTimer() {
        return executeCommand(new PixooCommand.GetTimerCommand(), TimerStatus.class);
    }

    @Override
    public PixooResponse setScoreboard(int blueScore, int redScore) {
        return executeCommand(new PixooCommand.SetScoreBoardCommand(blueScore, redScore));
    }

    @Override
    public ScoreboardStatus getScoreboard() {
        return executeCommand(new PixooCommand.GetScoreBoardCommand(), ScoreboardStatus.class);
    }

    @Override
    public PixooResponse setNoiseStatus(boolean start) {
        return executeCommand(new PixooCommand.SetNoiseStatusCommand(start ? 1 : 0));
    }

    @Override
    public NoiseStatus getNoiseStatus() {
        return executeCommand(new PixooCommand.GetNoiseStatusCommand(), NoiseStatus.class);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record StorageStatusResponse(@JsonProperty("Full") Integer full) {}

    @Override
    public boolean isStorageFull() {
        StorageStatusResponse resp = executeCommand(new PixooCommand.GetStorageStatusCommand(), StorageStatusResponse.class);
        return resp.full() != null && resp.full() == 1;
    }

    @Override
    public PixooResponse setSleepTimer(int minutes) {
        return executeCommand(new PixooCommand.SetDelayPowerOffCommand(minutes));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record DelayPowerOffResponse(@JsonProperty("DelayPowerOff") Integer delayPowerOff) {}

    @Override
    public int getSleepTimer() {
        DelayPowerOffResponse resp = executeCommand(new PixooCommand.GetDelayPowerOffCommand(), DelayPowerOffResponse.class);
        return resp.delayPowerOff() != null ? resp.delayPowerOff() : 0;
    }

    @Override
    public PixooResponse setPomodoro(int id, String name, int workMinutes, int shortRestMinutes, int longRestMinutes) {
        return executeCommand(new PixooCommand.TomatoSetCommand(id, name, workMinutes, shortRestMinutes, longRestMinutes));
    }

    @Override
    public PixooResponse startPomodoro(int id) {
        return executeCommand(new PixooCommand.TomatoStartCommand(id));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AlarmListResponse(@JsonProperty("AlarmList") List<PixooAlarm> alarmList) {}

    @Override
    public List<PixooAlarm> getAlarms() {
        AlarmListResponse resp = executeCommand(new PixooCommand.AlarmGetCommand(1), AlarmListResponse.class);
        return resp.alarmList() != null ? resp.alarmList() : List.of();
    }

    @Override
    public PixooResponse setAlarm(PixooAlarm alarm) {
        return executeCommand(new PixooCommand.AlarmSetCommand(
                alarm.alarmId(),
                alarm.alarmName(),
                alarm.alarmTime(),
                alarm.enableFlag(),
                alarm.repeatArray(),
                alarm.volume(),
                alarm.soundType()
        ));
    }

    @Override
    public PixooResponse deleteAlarm(int alarmId) {
        return executeCommand(new PixooCommand.AlarmDelCommand(alarmId));
    }

    @Override
    public PixooResponse setMemorial(int id, String name, int month, int day, int hour, int minute) {
        return executeCommand(new PixooCommand.MemorialSetCommand(
                id,
                name,
                month,
                day,
                hour * 60 + minute
        ));
    }

    @Override
    public PixooResponse deleteMemorial(int memorialId) {
        return executeCommand(new PixooCommand.MemorialDelCommand(memorialId));
    }
}
