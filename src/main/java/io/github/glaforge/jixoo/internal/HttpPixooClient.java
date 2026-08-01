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

import io.github.glaforge.jixoo.api.*;
import io.github.glaforge.jixoo.api.exception.PixooException;
import io.github.glaforge.jixoo.model.PixooAnimation;
import io.github.glaforge.jixoo.model.PixooFrame;
import io.github.glaforge.jixoo.model.command.PixooCommand;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
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

            PixooResponse pixooResponse = MAPPER.readValue(response.body(), PixooResponse.class);
            log.debug("Received response: {}", pixooResponse);
            return pixooResponse;

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

    private int getLightSwitch() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUri))
                    .timeout(requestTimeout)
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"Command\":\"Channel/GetAllConf\"}"))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String body = response.body();
                int idx = body.indexOf("\"LightSwitch\"");
                if (idx > 0) {
                    int colonIdx = body.indexOf(":", idx);
                    int commaIdx = body.indexOf(",", colonIdx);
                    if (commaIdx == -1) commaIdx = body.indexOf("}", colonIdx);
                    if (colonIdx > 0 && commaIdx > colonIdx) {
                        return Integer.parseInt(body.substring(colonIdx + 1, commaIdx).trim());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to get light switch status", e);
        }
        return -1;
    }

    private int prepareCustomChannel() {
        if (!autoSwitchToCustomChannel) return -1;
        
        int lightSwitch = getLightSwitch();
        if (lightSwitch == 1) {
            executeCommand(new PixooCommand.ScreenStateCommand(0));
            try { Thread.sleep(250); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        selectChannel(PixooChannel.CUSTOM);
        return lightSwitch;
    }

    private void restoreScreen(int lightSwitch) {
        if (lightSwitch == 1) {
            executeCommand(new PixooCommand.ScreenStateCommand(1));
        }
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
        return executeCommand(new PixooCommand.RotationCommand(rotation.angle()));
    }

    @Override
    public PixooResponse playBuzzer(int activeMs, int offMs, int totalMs) {
        return executeCommand(new PixooCommand.PlayBuzzerCommand(activeMs, offMs, totalMs));
    }
}
