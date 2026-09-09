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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response returned by {@code /UserLogin}.
 *
 * @param returnCode    Status code returned by the Divoom server (0 indicates success).
 * @param returnMessage Status message or error description.
 * @param userId        The authenticated user ID.
 * @param token         The session token.
 * @param userToken     The user token.
 * @param nickname      The user nickname.
 * @param email         The user email address.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DivoomLoginResponse(
        @JsonProperty("ReturnCode") int returnCode,
        @JsonProperty("ReturnMessage") String returnMessage,
        @JsonProperty("UserId") int userId,
        @JsonProperty("Token") String token,
        @JsonProperty("UserToken") String userToken,
        @JsonProperty("Nickname") String nickname,
        @JsonProperty("Email") String email
) implements DivoomResponse {
}
