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
 * Metadata for an individual pixel art artwork in the Divoom Cloud Gallery.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CloudGalleryItem(
        @JsonProperty("GalleryId") long galleryId,
        @JsonProperty("FileId") String fileId,
        @JsonProperty("FileName") String fileName,
        @JsonProperty("UserId") long userId,
        @JsonProperty("UserName") String userName,
        @JsonProperty("FileSize") int fileSize,
        @JsonProperty("FileType") int fileType,
        @JsonProperty("LikeCnt") int likeCount,
        @JsonProperty("WatchCnt") int watchCount,
        @JsonProperty("CommentCnt") int commentCount,
        @JsonProperty("Date") long dateTimestamp
) {
    /**
     * Checks if this artwork is 64x64 resolution (FileSize == 4).
     */
    public boolean is64x64() {
        return fileSize == 4;
    }
}
