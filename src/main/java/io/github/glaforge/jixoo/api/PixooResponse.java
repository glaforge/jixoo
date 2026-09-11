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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Standard response returned by the Pixoo64 REST API.
 * Handles both standard numeric status codes (0 = success) and string error messages
 * occasionally returned by device firmware when requests are invalid or unsupported.
 *
 * @param errorCode    Status code returned by the device (0 indicates success).
 * @param errorMessage Optional error description or error message string.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PixooResponse(
        @JsonProperty("error_code") int errorCode,
        @JsonProperty("error_message") String errorMessage
) {
    public PixooResponse(int errorCode) {
        this(errorCode, null);
    }

    @JsonCreator
    public static PixooResponse fromJson(
            @JsonProperty("error_code") Object errorCodeObj,
            @JsonProperty("error_message") String errorMsg
    ) {
        int code = 0;
        String msg = errorMsg;
        if (errorCodeObj instanceof Number n) {
            code = n.intValue();
        } else if (errorCodeObj != null) {
            String str = errorCodeObj.toString();
            try {
                code = Integer.parseInt(str);
            } catch (NumberFormatException e) {
                code = -1;
                if (msg == null) {
                    msg = str;
                }
            }
        }
        return new PixooResponse(code, msg);
    }

    /**
     * Checks if the response indicates success.
     *
     * @return true if the error code is 0 and no error message is set, false otherwise
     */
    public boolean isSuccess() {
        return errorCode == 0 && errorMessage == null;
    }
}
