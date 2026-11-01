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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.glaforge.jixoo.api.PixooModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Finds out which {@link PixooModel} answers at a given address, by sending a harmless
 * {@code Channel/GetIndex} read to each model's API endpoint and checking the response envelope.
 */
public final class PixooModelProbe {
    private static final Logger log = LoggerFactory.getLogger(PixooModelProbe.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String PROBE_BODY = "{\"Command\": \"Channel/GetIndex\"}";

    private PixooModelProbe() {}

    /**
     * A device that answered a probe.
     *
     * @param model the detected model
     * @param port  the port it answered on
     * @param reply the decoded {@code Channel/GetIndex} reply
     */
    public record Detection(PixooModel model, int port, JsonNode reply) {}

    /**
     * An API endpoint to probe.
     *
     * @param model the model served at this endpoint
     * @param port  the HTTP port
     */
    public record Endpoint(PixooModel model, int port) {}

    /**
     * Detects the model at the given address. Without a port, each model is tried on its default
     * port (Pixoo 64 first); with a port, each model's API path is tried on that port.
     *
     * @param host    the device IP address or host name
     * @param port    the HTTP port, or {@code null} to use each model's default port
     * @param timeout connection and request timeout for each probe
     * @return the detection, or empty if no endpoint answered like a Pixoo
     */
    public static Optional<Detection> detect(String host, Integer port, Duration timeout) {
        List<Endpoint> endpoints = port == null
                ? List.of(new Endpoint(PixooModel.PIXOO_64, PixooModel.PIXOO_64.defaultPort()),
                          new Endpoint(PixooModel.PIXOO_64_II, PixooModel.PIXOO_64_II.defaultPort()))
                : List.of(new Endpoint(PixooModel.PIXOO_64, port), new Endpoint(PixooModel.PIXOO_64_II, port));
        try (HttpClient client = newClient(timeout)) {
            return detect(client, host, endpoints, timeout);
        }
    }

    static Optional<Detection> detect(HttpClient client, String host, List<Endpoint> endpoints, Duration timeout) {
        for (Endpoint endpoint : endpoints) {
            Optional<Detection> detection = probe(client, host, endpoint, timeout);
            if (detection.isPresent()) {
                return detection;
            }
        }
        return Optional.empty();
    }

    static HttpClient newClient(Duration timeout) {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(timeout)
                .build();
    }

    private static Optional<Detection> probe(HttpClient client, String host, Endpoint endpoint, Duration timeout) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + host + ":" + endpoint.port() + endpoint.model().apiPath()))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(PROBE_BODY))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return Optional.empty();
            }
            JsonNode reply = MAPPER.readTree(response.body());
            // Each model is recognized by its response envelope
            String statusField = endpoint.model() == PixooModel.PIXOO_64_II ? "ReturnCode" : "error_code";
            if (reply != null && reply.isObject() && reply.has(statusField)) {
                return Optional.of(new Detection(endpoint.model(), endpoint.port(), reply));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.debug("Probe failed for {}:{}{}", host, endpoint.port(), endpoint.model().apiPath(), e);
        }
        return Optional.empty();
    }
}
