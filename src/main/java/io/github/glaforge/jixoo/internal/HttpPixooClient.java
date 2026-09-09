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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.glaforge.jixoo.api.*;
import io.github.glaforge.jixoo.api.exception.PixooException;
import io.github.glaforge.jixoo.model.PixooAnimation;
import io.github.glaforge.jixoo.model.PixooFrame;
import io.github.glaforge.jixoo.model.command.PixooCommand;
import io.github.glaforge.jixoo.model.sys.ChannelConfig;
import io.github.glaforge.jixoo.model.sys.SysConfig;
import io.github.glaforge.jixoo.model.tool.NoiseStatus;
import io.github.glaforge.jixoo.model.tool.ScoreboardStatus;
import io.github.glaforge.jixoo.model.tool.StopwatchAction;
import io.github.glaforge.jixoo.model.tool.StopwatchStatus;
import io.github.glaforge.jixoo.model.tool.TimerStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * HTTP-based implementation of {@link PixooClient}.
 */
public class HttpPixooClient implements PixooClient {
    private static final Logger log = LoggerFactory.getLogger(HttpPixooClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    private static final AtomicInteger PIC_ID_GENERATOR = new AtomicInteger(new Random().nextInt(10000) + 1);
    private static final DateTimeFormatter UTC_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String targetUri;
    private final HttpClient httpClient;
    private final Duration requestTimeout;
    private final boolean autoSwitchToCustomChannel;

    /**
     * Constructs a new HttpPixooClient.
     *
     * @param ipAddress                 the IP address of the device
     * @param port                      the HTTP port to connect to
     * @param connectTimeout            the connection timeout duration
     * @param requestTimeout            the request timeout duration
     * @param autoSwitchToCustomChannel whether to automatically switch to the custom channel when sending content
     */
    public HttpPixooClient(String ipAddress, int port, Duration connectTimeout, Duration requestTimeout, boolean autoSwitchToCustomChannel) {
        this.targetUri = "http://" + ipAddress + ":" + port + "/post";
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
            String jsonBody = MAPPER.writeValueAsString(command);
            log.debug("Sending JSON command to {}: {}", targetUri, jsonBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUri))
                    .timeout(requestTimeout)
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new PixooException("Pixoo API HTTP Error: Status " + response.statusCode() + ", Body: " + response.body());
            }

            return MAPPER.readValue(response.body(), responseType);

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
        try {
            ScreenStateResponse resp = executeCommand(new PixooCommand.GetScreenStateCommand(), ScreenStateResponse.class);
            return resp.onOff() == null || resp.onOff() == 1;
        } catch (Exception e) {
            log.warn("Failed to get screen state: {}", e.getMessage());
            return true;
        }
    }

    private int prepareCustomChannel() {
        if (!autoSwitchToCustomChannel) return -1;

        boolean wasScreenOn = isScreenOn();
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

        for (int i = 0; i < totalFrames; i++) {
            PixooFrame frame = animation.frames().get(i);
            String base64Data = frame.toBase64();

            PixooCommand.SendGifCommand command = new PixooCommand.SendGifCommand(
                    totalFrames,
                    i,
                    picId,
                    frame.delayMs(),
                    base64Data
            );

            lastResponse = executeCommand(command);
            if (!lastResponse.isSuccess()) {
                log.warn("Frame {} of {} failed with error code {}", i, totalFrames, lastResponse.errorCode());
                break;
            }
            try { Thread.sleep(30); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        try { Thread.sleep(250); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        restoreScreen(originalState);
        return lastResponse;
    }

    @Override
    public PixooResponse sendRemoteGifUrl(String gifUrl) {
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
}
