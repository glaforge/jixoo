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
package com.divoom.pixoo64.api;

/**
 * High-level display channels supported by the Pixoo64 device.
 */
public enum PixooChannel {
    /** Channel 0 (Clock) */
    CLOCK(0),
    /** Channel 1 (Cloud) */
    CLOUD(1),
    /** Channel 2 (Visualizer) */
    VISUALIZER(2),
    /** Channel 3 (Custom) */
    CUSTOM(3),
    /** Channel 4 (Black Screen) */
    BLACK_SCREEN(4);

    private final int index;

    PixooChannel(int index) {
        this.index = index;
    }

    /**
     * Gets the integer index associated with this channel.
     *
     * @return the channel index (0-4)
     */
    public int index() {
        return index;
    }
}
