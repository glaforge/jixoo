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
package io.github.glaforge.jixoo.cloud.model;

/**
 * Base sealed interface for all responses returned by the Divoom Cloud REST API.
 */
public sealed interface DivoomResponse permits
        DivoomApiResponse,
        DivoomLoginResponse,
        DivoomDeviceListResponse,
        DivoomCustomListResponse,
        DivoomFileUploadResponse {

    /**
     * The numeric status code returned by the Divoom server (0 indicates success).
     *
     * @return the status return code
     */
    int returnCode();

    /**
     * The message or error detail returned by the Divoom server.
     *
     * @return the status return message
     */
    String returnMessage();

    /**
     * Checks if the response represents a successful request (ReturnCode == 0).
     *
     * @return true if returnCode is 0, false otherwise
     */
    default boolean isSuccess() {
        return returnCode() == 0;
    }
}
