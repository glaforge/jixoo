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

/**
 * Screen rotation angles supported by Pixoo64.
 */
public enum PixooRotation {
    /** Normal rotation (0 degrees). */
    NORMAL(0, 0),
    /** Rotated 90 degrees clockwise. */
    ROTATE_90(1, 90),
    /** Rotated 180 degrees. */
    ROTATE_180(2, 180),
    /** Rotated 270 degrees clockwise. */
    ROTATE_270(3, 270);

    private final int mode;
    private final int angle;

    PixooRotation(int mode, int angle) {
        this.mode = mode;
        this.angle = angle;
    }

    /**
     * Gets the mode index (0..3) expected by the firmware.
     *
     * @return the mode index
     */
    public int mode() {
        return mode;
    }

    /**
     * Gets the angle of rotation in degrees (0, 90, 180, 270).
     *
     * @return the rotation angle
     */
    public int angle() {
        return angle;
    }

    /**
     * Resolves a PixooRotation from a firmware mode index (0..3).
     *
     * @param mode the mode index
     * @return the corresponding PixooRotation
     * @throws IllegalArgumentException if mode is unrecognized
     */
    public static PixooRotation fromMode(int mode) {
        for (PixooRotation r : values()) {
            if (r.mode == mode) {
                return r;
            }
        }
        throw new IllegalArgumentException("Unknown rotation mode: " + mode + ". Valid modes are 0, 1, 2, 3.");
    }

    /**
     * Resolves a PixooRotation from an angle in degrees.
     *
     * @param angle the rotation angle in degrees
     * @return the corresponding PixooRotation
     * @throws IllegalArgumentException if angle is unrecognized
     */
    public static PixooRotation fromAngle(int angle) {
        for (PixooRotation r : values()) {
            if (r.angle == angle) {
                return r;
            }
        }
        throw new IllegalArgumentException("Unknown rotation angle: " + angle + "°. Valid angles are 0, 90, 180, 270.");
    }
}
