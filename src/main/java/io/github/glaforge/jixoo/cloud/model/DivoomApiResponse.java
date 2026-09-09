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
 * Standard response returned by Divoom cloud API endpoints.
 *
 * @param returnCode    Status code returned by the Divoom server (0 indicates success).
 * @param returnMessage Status message or error description.
 * @param galleryId     Assigned gallery ID (e.g. from SaveCustomPicToGallery).
 * @param pixelFileId   Assigned CDN pixel file ID (e.g. from SaveCustomPicToGallery).
 * @param customId      Assigned custom ID (e.g. from Channel/SetCustom).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DivoomApiResponse(
        @JsonProperty("ReturnCode") int returnCode,
        @JsonProperty("ReturnMessage") String returnMessage,
        @JsonProperty("GalleryId") int galleryId,
        @JsonProperty("PixelFileId") String pixelFileId,
        @JsonProperty("CustomId") int customId
) implements DivoomResponse {

    /**
     * Default constructor representing a successful empty response.
     */
    public DivoomApiResponse() {
        this(0, "", 0, null, 0);
    }

    public DivoomApiResponse(int returnCode, String returnMessage) {
        this(returnCode, returnMessage, 0, null, 0);
    }
}
