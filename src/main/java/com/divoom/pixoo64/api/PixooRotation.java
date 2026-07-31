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
 * Screen rotation angles supported by Pixoo64.
 */
public enum PixooRotation {
    /** Normal rotation (0 degrees). */
    NORMAL(0),
    /** Rotated 90 degrees clockwise. */
    ROTATE_90(90),
    /** Rotated 180 degrees. */
    ROTATE_180(180),
    /** Rotated 270 degrees clockwise. */
    ROTATE_270(270);

    private final int angle;

    PixooRotation(int angle) {
        this.angle = angle;
    }

    /**
     * Gets the angle of rotation in degrees.
     *
     * @return the rotation angle
     */
    public int angle() {
        return angle;
    }
}
