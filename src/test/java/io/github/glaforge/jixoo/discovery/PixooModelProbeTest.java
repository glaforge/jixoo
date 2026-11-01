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
package io.github.glaforge.jixoo.discovery;

import com.sun.net.httpserver.HttpServer;
import io.github.glaforge.jixoo.api.PixooModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PixooModelProbeTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(2);
    private static final String PIXOO_64_REPLY = "{\"error_code\": 0, \"SelectIndex\": 3}";
    private static final String PIXOO_64_II_REPLY = "{\"Command\": \"Channel/GetIndex\", \"DeviceId\": 123456789, \"PacketFlag\": 1,"
            + " \"DeviceType\": \"Divoom\", \"ReturnCode\": 0, \"ReturnMessage\": \"\", \"WifiSingal\": 0, \"SelectIndex\": 2}";

    private HttpServer server;

    private int serve(String path, String reply) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(path, exchange -> {
            exchange.getRequestBody().readAllBytes();
            byte[] body = reply.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();
        return server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void detectsPixoo64OnPostEndpoint() throws IOException {
        int port = serve("/post", PIXOO_64_REPLY);

        Optional<PixooModelProbe.Detection> detection = PixooModelProbe.detect("127.0.0.1", port, TIMEOUT);

        assertTrue(detection.isPresent());
        assertEquals(PixooModel.PIXOO_64, detection.get().model());
        assertEquals(port, detection.get().port());
    }

    @Test
    void detectsPixoo64IIOnDivoomApiEndpoint() throws IOException {
        int port = serve("/divoom_api", PIXOO_64_II_REPLY);

        Optional<PixooModelProbe.Detection> detection = PixooModelProbe.detect("127.0.0.1", port, TIMEOUT);

        assertTrue(detection.isPresent());
        assertEquals(PixooModel.PIXOO_64_II, detection.get().model());
        assertEquals(123456789, detection.get().reply().path("DeviceId").asInt());
    }

    @Test
    void ignoresServersThatDoNotAnswerLikeAPixoo() throws IOException {
        // Right path, wrong envelope: some other HTTP service
        int port = serve("/divoom_api", "{\"status\": \"ok\"}");

        assertTrue(PixooModelProbe.detect("127.0.0.1", port, TIMEOUT).isEmpty());
    }

    @Test
    void findsNothingWhenNoServerListens() throws IOException {
        int port = serve("/post", PIXOO_64_REPLY);
        server.stop(0);
        server = null;

        assertTrue(PixooModelProbe.detect("127.0.0.1", port, TIMEOUT).isEmpty());
    }

    @Test
    void discoveryProbeReportsModelNameAndDeviceId() throws IOException {
        int port = serve("/divoom_api", PIXOO_64_II_REPLY);
        List<PixooModelProbe.Endpoint> endpoints = List.of(
                new PixooModelProbe.Endpoint(PixooModel.PIXOO_64, port),
                new PixooModelProbe.Endpoint(PixooModel.PIXOO_64_II, port));

        try (HttpClient client = PixooModelProbe.newClient(TIMEOUT)) {
            PixooDevice device = PixooDevice.probeDevice(client, "127.0.0.1", "f4:e2:5d:00:00:00", endpoints);

            assertNotNull(device);
            assertEquals(PixooModel.PIXOO_64_II, device.model());
            assertEquals("Pixoo-64 II", device.deviceName());
            assertEquals(123456789, device.deviceId());
            assertEquals("f4:e2:5d:00:00:00", device.macAddress());
        }
    }

    @Test
    void legacyConstructorDefaultsToPixoo64() {
        assertEquals(PixooModel.PIXOO_64, new PixooDevice("192.168.1.10", "", "Pixoo64", 0).model());
    }
}
