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
package io.github.glaforge.jixoo.api;

import java.util.Map;
import java.util.Optional;

/**
 * Divoom device models supported by the local HTTP API, with their transport endpoint.
 * <p>
 * Both models share the same command vocabulary, but differ in where the API is served
 * and in the response envelope:
 * <ul>
 *     <li>{@link #PIXOO_64}: {@code POST http://<ip>:80/post}, replies with {@code error_code}.</li>
 *     <li>{@link #PIXOO_64_II}: {@code POST http://<ip>:9000/divoom_api} (port 80 is closed),
 *     replies with {@code ReturnCode} / {@code ReturnMessage}.</li>
 * </ul>
 * The model also carries the device's quirks: how long a still image must play, and which
 * commands it acknowledges without honoring them (see SPECIFICATION.md §10).
 */
public enum PixooModel {
    /** Original Divoom Pixoo 64. */
    PIXOO_64(80, "/post", 0, Map.of()),
    /**
     * Divoom Pixoo-64 II. A still image is played for its {@code PicSpeed} before the next upload
     * shows: a short duration lets each image replace the previous one at once (the last one stays).
     */
    PIXOO_64_II(9000, "/divoom_api", 1000, Map.of(
            "Channel/SetStartupChannel", "the device acknowledges it, but the startup channel reverts within seconds",
            "Memorial/Del", "the device stops answering after this command"));

    private final int defaultPort;
    private final String apiPath;
    private final int stillFrameDelayMs;
    private final Map<String, String> unsupportedCommands;

    PixooModel(int defaultPort, String apiPath, int stillFrameDelayMs, Map<String, String> unsupportedCommands) {
        this.defaultPort = defaultPort;
        this.apiPath = apiPath;
        this.stillFrameDelayMs = stillFrameDelayMs;
        this.unsupportedCommands = unsupportedCommands;
    }

    /**
     * Gets the HTTP port the device serves its local API on.
     *
     * @return the default HTTP port
     */
    public int defaultPort() {
        return defaultPort;
    }

    /**
     * Gets the HTTP path of the local API endpoint.
     *
     * @return the API path, starting with a slash
     */
    public String apiPath() {
        return apiPath;
    }

    /**
     * Gets the display duration to send for a still image (a one-frame animation).
     *
     * @param requestedMs the duration requested by the caller
     * @return the duration to send to this model
     */
    public int stillFrameDelayMs(int requestedMs) {
        return stillFrameDelayMs > 0 ? stillFrameDelayMs : requestedMs;
    }

    /**
     * Tells why this model cannot honor a command it would otherwise accept.
     *
     * @param command the command name (e.g. {@code Channel/SetStartupChannel})
     * @return the reason, or empty if the command is not known to be unsupported
     */
    public Optional<String> unsupportedReason(String command) {
        return Optional.ofNullable(unsupportedCommands.get(command));
    }
}
