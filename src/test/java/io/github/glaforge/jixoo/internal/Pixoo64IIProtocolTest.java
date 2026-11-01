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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import io.github.glaforge.jixoo.api.PixooChannel;
import io.github.glaforge.jixoo.api.PixooClient;
import io.github.glaforge.jixoo.api.PixooModel;
import io.github.glaforge.jixoo.api.PixooResponse;
import io.github.glaforge.jixoo.api.exception.PixooException;
import io.github.glaforge.jixoo.api.exception.UnsupportedCommandException;
import io.github.glaforge.jixoo.image.GifEncoder;
import io.github.glaforge.jixoo.model.PixooAnimation;
import io.github.glaforge.jixoo.model.PixooFrame;
import io.github.glaforge.jixoo.model.RawRgbBuffer;
import io.github.glaforge.jixoo.model.tool.TimerStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Talks to a fake Pixoo-64 II that replays replies recorded on a real device
 * ({@code src/test/resources/pixoo64-ii/replies.json}). Commands missing from the
 * recording get the device's unknown-command reply.
 */
class Pixoo64IIProtocolTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String UNKNOWN_COMMAND = "{\"ReturnCode\": 1, \"ReturnMessage\": \"Only accept JSON parameters\"}";

    private HttpServer server;
    private JsonNode replies;
    private PixooClient client;
    private final List<String> receivedCommands = new ArrayList<>();
    private final List<JsonNode> receivedBodies = new ArrayList<>();
    private final List<Long> arrivalNanos = new ArrayList<>();

    @BeforeEach
    void setUp() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/pixoo64-ii/replies.json")) {
            replies = MAPPER.readTree(in);
        }
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/divoom_api", exchange -> {
            long arrival = System.nanoTime();
            JsonNode request = MAPPER.readTree(exchange.getRequestBody());
            String command = request.path("Command").asText();
            synchronized (receivedCommands) {
                receivedCommands.add(command);
                receivedBodies.add(request);
                arrivalNanos.add(arrival);
            }
            JsonNode reply = replies.get(command);
            byte[] body = (reply != null ? reply.toString() : UNKNOWN_COMMAND).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        // A small animated GIF for the remote-URL fallback
        byte[] gif = GifEncoder.encode(new PixooAnimation(List.of(
                new PixooFrame(RawRgbBuffer.fromRgb(255, 0, 0), 200),
                new PixooFrame(RawRgbBuffer.fromRgb(0, 0, 255), 200))));
        server.createContext("/anim.gif", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "image/gif");
            exchange.sendResponseHeaders(200, gif.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(gif);
            }
        });
        server.start();
        client = PixooClient.builder()
                .model(PixooModel.PIXOO_64_II)
                .ipAddress("127.0.0.1")
                .port(server.getAddress().getPort())
                .build();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void writesSucceedWithReturnCodeEnvelope() {
        assertEquals(PixooModel.PIXOO_64_II, client.model());

        PixooResponse response = client.setBrightness(50);

        assertTrue(response.isSuccess());
        assertEquals(0, response.errorCode());
        assertNull(response.errorMessage(), "an empty ReturnMessage is not an error");
    }

    @Test
    void typedReadsIgnoreEnvelopeFields() {
        assertEquals(10, client.getClockId());
        assertTrue(client.isScreenOn());
        assertEquals(0, client.getCustomPageIndex());

        TimerStatus timer = client.getTimer();
        assertEquals(1, timer.minute());
        assertEquals(0, timer.second());
    }

    @Test
    void unknownCommandsThrowUnsupportedCommandException() {
        UnsupportedCommandException e = assertThrows(UnsupportedCommandException.class, () -> client.setSleepTimer(10));
        assertEquals("Device/SetDelayPowerOff", e.command());

        assertThrows(UnsupportedCommandException.class, () -> client.getSleepTimer());
        assertThrows(UnsupportedCommandException.class, () -> client.isStorageFull());
        assertThrows(UnsupportedCommandException.class, () -> client.setPomodoro(25, 5, 15));
        assertThrows(UnsupportedCommandException.class, () -> client.setMemorial(1, "Birthday", 10, 25));
    }

    @Test
    void readsWithoutDataThrowUnsupportedCommandException() {
        UnsupportedCommandException e = assertThrows(UnsupportedCommandException.class, () -> client.getSystemConfig());
        assertEquals("Sys/GetConf", e.command());

        assertThrows(UnsupportedCommandException.class, () -> client.getStartupChannel());
        assertThrows(UnsupportedCommandException.class, () -> client.getAlarms());
    }

    @Test
    void sendsFramesToDivoomApiEndpoint() {
        PixooResponse response = client.sendFrame(new PixooFrame(new byte[64 * 64 * 3], 1000));

        assertTrue(response.isSuccess());
        synchronized (receivedCommands) {
            assertEquals(List.of("Draw/ResetHttpGifId", "Draw/SendHttpGif"), receivedCommands);
        }
    }

    @Test
    void channelSelectionSucceeds() {
        assertTrue(client.selectChannel(PixooChannel.VISUALIZER).isSuccess());
    }

    @Test
    void clientWithoutModelDetectsPixoo64II() {
        PixooClient detecting = PixooClient.builder()
                .ipAddress("127.0.0.1")
                .port(server.getAddress().getPort())
                .build();

        assertEquals(10, detecting.getClockId());
        assertEquals(PixooModel.PIXOO_64_II, detecting.model());
        synchronized (receivedCommands) {
            // One probe on /divoom_api (the /post probe gets a 404), then the command itself
            assertEquals(List.of("Channel/GetIndex", "Channel/GetClockInfo"), receivedCommands);
        }
    }

    @Test
    void clientWithoutModelFailsClearlyWhenNothingAnswers() {
        int closedPort = server.getAddress().getPort();
        server.stop(0);
        PixooClient detecting = PixooClient.builder()
                .ipAddress("127.0.0.1")
                .port(closedPort)
                .build();

        PixooException e = assertThrows(PixooException.class, () -> detecting.setBrightness(50));
        assertTrue(e.getMessage().contains("No Pixoo device answered"), e.getMessage());
    }

    @Test
    void originalModelClientFailsOnPixoo64II() {
        // The original Pixoo 64 path (/post) does not exist on the Pixoo-64 II
        PixooClient original = PixooClient.builder()
                .model(PixooModel.PIXOO_64)
                .ipAddress("127.0.0.1")
                .port(server.getAddress().getPort())
                .build();

        PixooException e = assertThrows(PixooException.class, () -> original.setBrightness(50));
        assertTrue(e.getMessage().contains("404"), e.getMessage());
    }

    @Test
    void stillImagesUseShortDurationSoTheyDoNotQueue() {
        client.sendColor(255, 0, 0); // requests 60 s, the Pixoo 64 default for solid colors

        synchronized (receivedCommands) {
            JsonNode frame = receivedBodies.get(receivedCommands.indexOf("Draw/SendHttpGif"));
            assertEquals(1000, frame.path("PicSpeed").asInt());
        }
    }

    @Test
    void animationsKeepTheirFrameDurations() {
        client.sendAnimation(new PixooAnimation(List.of(
                new PixooFrame(new byte[64 * 64 * 3], 120),
                new PixooFrame(new byte[64 * 64 * 3], 250))));

        synchronized (receivedCommands) {
            List<Integer> speeds = receivedBodies.stream()
                    .filter(body -> body.path("Command").asText().equals("Draw/SendHttpGif"))
                    .map(body -> body.path("PicSpeed").asInt())
                    .toList();
            assertEquals(List.of(120, 250), speeds);
        }
    }

    @Test
    void commandsTheDeviceDoesNotHonorAreRejectedWithoutBeingSent() {
        UnsupportedCommandException e = assertThrows(UnsupportedCommandException.class,
                () -> client.setStartupChannel(PixooChannel.CLOCK));
        assertEquals("Channel/SetStartupChannel", e.command());
        assertTrue(e.getMessage().contains("reverts"), e.getMessage());

        assertThrows(UnsupportedCommandException.class, () -> client.deleteMemorial(1));

        synchronized (receivedCommands) {
            assertTrue(receivedCommands.isEmpty(), receivedCommands.toString());
        }
    }

    @Test
    void remoteGifUrlIsStreamedByTheClient() {
        String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/anim.gif";

        PixooResponse response = client.sendRemoteGifUrl(url);

        assertTrue(response.isSuccess());
        synchronized (receivedCommands) {
            assertFalse(receivedCommands.contains("Device/PlayTFGif"));
            assertEquals(List.of("Draw/ResetHttpGifId", "Draw/SendHttpGif", "Draw/SendHttpGif"), receivedCommands);
        }
    }

    @Test
    void requestsAreSpacedOut() {
        for (int i = 0; i < 4; i++) {
            client.setBrightness(50);
        }

        synchronized (receivedCommands) {
            for (int i = 1; i < arrivalNanos.size(); i++) {
                long gapMs = (arrivalNanos.get(i) - arrivalNanos.get(i - 1)) / 1_000_000;
                assertTrue(gapMs >= 30, "requests " + (i - 1) + " and " + i + " were " + gapMs + " ms apart");
            }
        }
    }

    @Test
    void screenStateFailsWhenTheDeviceIsUnreachable() {
        server.stop(0);

        assertThrows(PixooException.class, () -> client.isScreenOn());
    }
}
