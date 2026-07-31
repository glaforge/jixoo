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
package com.divoom.pixoo64.internal;

import com.divoom.pixoo64.api.*;
import com.divoom.pixoo64.model.PixooAnimation;
import com.divoom.pixoo64.model.PixooFrame;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
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

class HttpPixooClientIntegrationTest {

    private HttpServer server;
    private int port;
    private final List<String> receivedRequests = new ArrayList<>();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = server.getAddress().getPort();

        server.createContext("/post", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                InputStream is = exchange.getRequestBody();
                String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                synchronized (receivedRequests) {
                    receivedRequests.add(body);
                }

                String response = "{\"error_code\": 0}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes(StandardCharsets.UTF_8));
                os.close();
            }
        });

        server.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void testChannelSelection() {
        PixooClient client = PixooClient.builder()
                .ipAddress("127.0.0.1")
                .port(port)
                .autoSwitchToCustomChannel(false)
                .build();

        PixooResponse response = client.selectChannel(PixooChannel.CUSTOM);
        assertTrue(response.isSuccess());

        synchronized (receivedRequests) {
            assertEquals(1, receivedRequests.size());
            assertTrue(receivedRequests.get(0).contains("\"Command\":\"Channel/SetIndex\""));
            assertTrue(receivedRequests.get(0).contains("\"SelectIndex\":3"));
        }
    }

    @Test
    void testSendAnimationTransactionSequence() {
        PixooClient client = PixooClient.builder()
                .ipAddress("127.0.0.1")
                .port(port)
                .autoSwitchToCustomChannel(true)
                .build();

        byte[] frameBuffer = new byte[12288];
        PixooFrame frame1 = new PixooFrame(frameBuffer, 100);
        PixooFrame frame2 = new PixooFrame(frameBuffer, 200);

        PixooAnimation animation = new PixooAnimation(List.of(frame1, frame2));

        PixooResponse response = client.sendAnimation(animation);
        assertTrue(response.isSuccess());

        synchronized (receivedRequests) {
            // Expected sequence:
            // 1. Channel/SetIndex (3)
            // 2. Draw/ResetHttpGifId
            // 3. Draw/SendHttpGif (frame 0)
            // 4. Draw/SendHttpGif (frame 1)
            assertEquals(4, receivedRequests.size());
            assertTrue(receivedRequests.get(0).contains("Channel/SetIndex"));
            assertTrue(receivedRequests.get(1).contains("Draw/ResetHttpGifId"));
            assertTrue(receivedRequests.get(2).contains("Draw/SendHttpGif"));
            assertTrue(receivedRequests.get(2).contains("\"PicNum\":2"));
            assertTrue(receivedRequests.get(2).contains("\"PicOffset\":0"));
            assertTrue(receivedRequests.get(3).contains("Draw/SendHttpGif"));
            assertTrue(receivedRequests.get(3).contains("\"PicNum\":2"));
            assertTrue(receivedRequests.get(3).contains("\"PicOffset\":1"));
        }
    }

    @Test
    void testHardwareTextOverlay() {
        PixooClient client = PixooClient.builder()
                .ipAddress("127.0.0.1")
                .port(port)
                .autoSwitchToCustomChannel(false)
                .build();

        PixooText text = PixooText.builder()
                .textId(1)
                .position(0, 10)
                .scrollLeft()
                .color("#FF0000")
                .text("Hello Pixoo")
                .build();

        PixooResponse response = client.sendText(text);
        assertTrue(response.isSuccess());

        synchronized (receivedRequests) {
            assertEquals(1, receivedRequests.size());
            assertTrue(receivedRequests.get(0).contains("Draw/SendHttpText"));
            assertTrue(receivedRequests.get(0).contains("\"TextString\":\"Hello Pixoo\""));
        }
    }

    @Test
    void testBuzzerAndBrightness() {
        PixooClient client = PixooClient.builder()
                .ipAddress("127.0.0.1")
                .port(port)
                .autoSwitchToCustomChannel(false)
                .build();

        client.setBrightness(85);
        client.playBuzzer(500, 500, 3000);

        synchronized (receivedRequests) {
            assertEquals(2, receivedRequests.size());
            assertTrue(receivedRequests.get(0).contains("Channel/SetBrightness"));
            assertTrue(receivedRequests.get(0).contains("\"Brightness\":85"));
            assertTrue(receivedRequests.get(1).contains("Device/PlayBuzzer"));
            assertTrue(receivedRequests.get(1).contains("\"PlayTotalTime\":3000"));
        }
    }
}
