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

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Standard response returned by the Pixoo64 REST API.
 *
 * @param errorCode Status code returned by the device (0 indicates success).
 */
public record PixooResponse(
        @JsonProperty("error_code") int errorCode
) {
    /**
     * Checks if the response indicates success.
     *
     * @return true if the error code is 0, false otherwise
     */
    public boolean isSuccess() {
        return errorCode == 0;
    }
}
