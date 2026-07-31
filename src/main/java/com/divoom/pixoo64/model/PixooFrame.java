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
package com.divoom.pixoo64.model;

import java.awt.image.BufferedImage;
import java.util.Base64;
import java.util.Objects;

/**
 * Single frame representation containing raw 12,288-byte RGB pixel buffer and display duration in milliseconds.
 *
 * @param rgbData  12,288 byte array (64x64x3 RGB scan order)
 * @param delayMs  Duration in milliseconds to display this frame
 */
public record PixooFrame(byte[] rgbData, int delayMs) {

    /**
     * Constructs and validates a new PixooFrame.
     */
    public PixooFrame {
        Objects.requireNonNull(rgbData, "rgbData cannot be null");
        if (rgbData.length != RawRgbBuffer.TOTAL_BYTES) {
            throw new IllegalArgumentException(
                    "RGB buffer must be exactly " + RawRgbBuffer.TOTAL_BYTES + " bytes, but was " + rgbData.length
            );
        }
        if (delayMs < 0) {
            throw new IllegalArgumentException("Frame delay cannot be negative");
        }
    }

    /**
     * Creates a PixooFrame from a BufferedImage with a specified delay.
     *
     * @param image   the source image (must be 64x64)
     * @param delayMs the duration to display this frame in milliseconds
     * @return a new PixooFrame instance
     */
    public static PixooFrame fromImage(BufferedImage image, int delayMs) {
        return new PixooFrame(RawRgbBuffer.fromImage(image), delayMs);
    }

    /**
     * Creates a PixooFrame from a BufferedImage with a default delay of 100ms.
     *
     * @param image the source image (must be 64x64)
     * @return a new PixooFrame instance
     */
    public static PixooFrame fromImage(BufferedImage image) {
        return fromImage(image, 100);
    }

    /**
     * Converts the raw RGB pixel data to a Base64 encoded string.
     *
     * @return the Base64 representation of the frame data
     */
    public String toBase64() {
        return Base64.getEncoder().encodeToString(rgbData);
    }
}
