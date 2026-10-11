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
package io.github.glaforge.jixoo.image;

import io.github.glaforge.jixoo.model.PixooAnimation;
import io.github.glaforge.jixoo.model.PixooFrame;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Post-processing filter tailored for the physical optical properties of the Pixoo-64 LED matrix.
 * <p>
 * Key operations:
 * <ul>
 *   <li><b>True-Black Clamping</b>: Clamps near-black background pixels below a given threshold
 *       to true {@code #000000} (off). On the Pixoo-64, {@code #000000} powers off the physical LED,
 *       preventing minimum-PWM duty-cycle glow, optical diffuser bleed, and coil whine.</li>
 *   <li><b>Peak Luminance Soft-Limiting</b>: Caps max RGB channel values (e.g. 245) to prevent
 *       optical halo blowout and glare through the silicone grid baffles.</li>
 *   <li><b>Saturation Boost</b>: Enhances HSV saturation of mid-tones to overcome
 *       the desaturating effect of the matte diffuser cover.</li>
 * </ul>
 *
 * @param blackThreshold  Threshold (0-255) below which dark pixels are clamped to #000000 (default: 15)
 * @param maxLuminance    Maximum value (1-255) for any color channel (default: 245)
 * @param saturationBoost Multiplier (0.1-3.0) applied to color saturation (default: 1.0)
 */
public record LedPostProcessor(int blackThreshold, int maxLuminance, double saturationBoost) {

    public static final int DEFAULT_BLACK_THRESHOLD = 15;
    public static final int DEFAULT_MAX_LUMINANCE = 245;
    public static final double DEFAULT_SATURATION_BOOST = 1.0;

    public LedPostProcessor {
        if (blackThreshold < 0 || blackThreshold > 255) {
            throw new IllegalArgumentException("blackThreshold must be between 0 and 255: " + blackThreshold);
        }
        if (maxLuminance < 1 || maxLuminance > 255) {
            throw new IllegalArgumentException("maxLuminance must be between 1 and 255: " + maxLuminance);
        }
        if (saturationBoost < 0.1 || saturationBoost > 3.0) {
            throw new IllegalArgumentException("saturationBoost must be between 0.1 and 3.0: " + saturationBoost);
        }
    }

    /**
     * Default standard optimization profile for Pixoo-64 displays.
     */
    public static LedPostProcessor standard() {
        return new LedPostProcessor(DEFAULT_BLACK_THRESHOLD, DEFAULT_MAX_LUMINANCE, DEFAULT_SATURATION_BOOST);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private int blackThreshold = DEFAULT_BLACK_THRESHOLD;
        private int maxLuminance = DEFAULT_MAX_LUMINANCE;
        private double saturationBoost = DEFAULT_SATURATION_BOOST;

        public Builder blackThreshold(int threshold) {
            this.blackThreshold = threshold;
            return this;
        }

        public Builder maxLuminance(int maxLuminance) {
            this.maxLuminance = maxLuminance;
            return this;
        }

        public Builder saturationBoost(double saturationBoost) {
            this.saturationBoost = saturationBoost;
            return this;
        }

        public LedPostProcessor build() {
            return new LedPostProcessor(blackThreshold, maxLuminance, saturationBoost);
        }
    }

    /**
     * Optimizes a single PixooImage using default standard settings.
     */
    public static PixooImage optimize(PixooImage image) {
        return standard().process(image);
    }

    /**
     * Optimizes an entire animation using default standard settings.
     */
    public static PixooAnimation optimize(PixooAnimation animation) {
        return standard().process(animation);
    }

    /**
     * Processes a PixooImage and returns a new optimized PixooImage.
     */
    public PixooImage process(PixooImage image) {
        int w = image.width();
        int h = image.height();
        int[] src = image.argbPixels();
        int[] dest = new int[w * h];

        for (int i = 0; i < src.length; i++) {
            dest[i] = processPixel(src[i]);
        }

        return new PixooImage(w, h, dest);
    }

    /**
     * Processes a PixooFrame and returns a new optimized PixooFrame.
     */
    public PixooFrame process(PixooFrame frame) {
        byte[] srcRgb = frame.rgbData();
        byte[] destRgb = new byte[srcRgb.length];

        for (int i = 0; i < srcRgb.length; i += 3) {
            int r = srcRgb[i] & 0xFF;
            int g = srcRgb[i + 1] & 0xFF;
            int b = srcRgb[i + 2] & 0xFF;

            int argb = (0xFF << 24) | (r << 16) | (g << 8) | b;
            int processed = processPixel(argb);

            destRgb[i] = (byte) ((processed >> 16) & 0xFF);
            destRgb[i + 1] = (byte) ((processed >> 8) & 0xFF);
            destRgb[i + 2] = (byte) (processed & 0xFF);
        }

        return new PixooFrame(destRgb, frame.delayMs());
    }

    /**
     * Processes all frames of a PixooAnimation.
     */
    public PixooAnimation process(PixooAnimation animation) {
        List<PixooFrame> processedFrames = new ArrayList<>(animation.frames().size());
        for (PixooFrame f : animation.frames()) {
            processedFrames.add(process(f));
        }
        return new PixooAnimation(processedFrames);
    }

    private int processPixel(int argb) {
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;

        // 1. True-Black Clamping (powers LED completely off)
        if (r <= blackThreshold && g <= blackThreshold && b <= blackThreshold) {
            return (a << 24); // pure black with original alpha
        }

        // 2. Saturation Boost (optional)
        if (saturationBoost != 1.0) {
            float[] hsv = new float[3];
            Color.RGBtoHSB(r, g, b, hsv);
            hsv[1] = (float) Math.min(1.0, hsv[1] * saturationBoost);
            int rgb = Color.HSBtoRGB(hsv[0], hsv[1], hsv[2]);
            r = (rgb >> 16) & 0xFF;
            g = (rgb >> 8) & 0xFF;
            b = rgb & 0xFF;
        }

        // 3. Peak Luminance Soft-Limiting
        if (maxLuminance < 255) {
            if (r > maxLuminance) r = maxLuminance;
            if (g > maxLuminance) g = maxLuminance;
            if (b > maxLuminance) b = maxLuminance;
        }

        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
