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
package io.github.glaforge.jixoo.model;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Objects;

/**
 * Representation of a static image or multi-frame animation sequence to be sent to Pixoo64.
 *
 * @param frames Sequenced list of frames making up the animation
 */
public record PixooAnimation(List<PixooFrame> frames) {

    public PixooAnimation {
        Objects.requireNonNull(frames, "frames list cannot be null");
        if (frames.isEmpty()) {
            throw new IllegalArgumentException("PixooAnimation must contain at least one frame");
        }
    }

    /**
     * Creates a PixooAnimation containing a single frame.
     *
     * @param frame the frame to include
     * @return a new PixooAnimation
     */
    public static PixooAnimation singleFrame(PixooFrame frame) {
        return new PixooAnimation(List.of(frame));
    }

    /**
     * Creates a static PixooAnimation from a single image.
     *
     * @param image the image to convert to a frame
     * @return a new PixooAnimation
     */
    public static PixooAnimation singleImage(BufferedImage image) {
        return singleFrame(PixooFrame.fromImage(image));
    }

    /**
     * Gets the total number of frames in this animation.
     *
     * @return the frame count
     */
    public int frameCount() {
        return frames.size();
    }
}
