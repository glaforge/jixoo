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
package io.github.glaforge.jixoo.api.exception;

/**
 * Thrown when the device does not support a command: it rejects it as unknown,
 * or acknowledges a read without returning any data (as the Pixoo-64 II does for
 * {@code Sys/GetConf}, for instance).
 */
public class UnsupportedCommandException extends PixooException {

    private final String command;

    /**
     * Constructs a new UnsupportedCommandException.
     *
     * @param command the rejected command name (e.g. {@code Sys/DevUpdateConf})
     * @param message the detail message
     */
    public UnsupportedCommandException(String command, String message) {
        super(message);
        this.command = command;
    }

    /**
     * Gets the name of the command the device does not support.
     *
     * @return the command name
     */
    public String command() {
        return command;
    }
}
