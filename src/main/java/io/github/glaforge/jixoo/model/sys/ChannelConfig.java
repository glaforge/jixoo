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
package io.github.glaforge.jixoo.model.sys;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Channel configuration settings for the Pixoo-64 device.
 *
 * @param channelIndex           active or startup channel index
 * @param clockTime              clock face display duration (seconds)
 * @param galleryTime            gallery slideshow interval (seconds)
 * @param singleGalleryTime      single gallery animation time
 * @param galleryShowTimeFlag    flag indicating if time overlay is shown in gallery
 * @param rotationFlag           channel auto-rotation flag (0 = off, 1 = on)
 * @param startUpClockId         clock id used on startup
 * @param startUpClockOnOff      startup clock state
 * @param startUpClockImageFileId startup clock image file id
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChannelConfig(
        @JsonProperty("ChannelIndex") Integer channelIndex,
        @JsonProperty("ClockTime") Integer clockTime,
        @JsonProperty("GalleryTime") Integer galleryTime,
        @JsonProperty("SingleGalleyTime") Integer singleGalleryTime,
        @JsonProperty("GalleryShowTimeFlag") Integer galleryShowTimeFlag,
        @JsonProperty("RotationFlag") Integer rotationFlag,
        @JsonProperty("StartUpClockId") Integer startUpClockId,
        @JsonProperty("StartUpClockOnOff") Integer startUpClockOnOff,
        @JsonProperty("StartUpClockImageFileId") String startUpClockImageFileId
) {
}
