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
 * Text overlay configuration for the hardware text rendering engine.
 *
 * @param textId     Persistent layer slot identifier.
 * @param x          X coordinate (0-63).
 * @param y          Y coordinate (0-63).
 * @param dir        Scroll direction: 0 = static, 1 = scroll left, 2 = scroll right.
 * @param font       Bitmap font ID from device ROM (e.g. 0-7, etc.).
 * @param textWidth  Width allocated for text container/alignment.
 * @param speed      Scroll delay in milliseconds per step (e.g. 50-100ms).
 * @param textString Text content to render.
 * @param color      Color string in hexadecimal format (e.g. "#FF0000").
 * @param align      Alignment: 1 = left, 2 = center, 3 = right.
 */
public record PixooText(
        int textId,
        int x,
        int y,
        int dir,
        int font,
        int textWidth,
        int speed,
        String textString,
        String color,
        int align
) {
    /**
     * Creates a new Builder for constructing a PixooText object.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for {@link PixooText}.
     */
    public static class Builder {
        private int textId = 1;
        private int x = 0;
        private int y = 0;
        private int dir = 0;
        private int font = 0;
        private int textWidth = 64;
        private int speed = 80;
        private String textString = "";
        private String color = "#FFFFFF";
        private int align = 1;

        /** Creates a new Builder instance. */
        private Builder() {}

        /**
         * Sets the text identifier slot.
         *
         * @param textId the text slot (e.g., 0-7)
         * @return this Builder
         */
        public Builder textId(int textId) {
            this.textId = textId;
            return this;
        }

        /**
         * Sets the position of the text on the screen.
         *
         * @param x the x coordinate
         * @param y the y coordinate
         * @return this Builder
         */
        public Builder position(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }

        /**
         * Configures the text to scroll left.
         *
         * @return this Builder
         */
        public Builder scrollLeft() {
            this.dir = 1;
            return this;
        }

        /**
         * Configures the text to scroll right.
         *
         * @return this Builder
         */
        public Builder scrollRight() {
            this.dir = 2;
            return this;
        }

        /**
         * Configures the text to remain static (no scrolling).
         *
         * @return this Builder
         */
        public Builder staticText() {
            this.dir = 0;
            return this;
        }

        /**
         * Sets the font identifier.
         *
         * @param font the font ID
         * @return this Builder
         */
        public Builder font(int font) {
            this.font = font;
            return this;
        }

        /**
         * Sets the maximum width of the text container.
         *
         * @param textWidth the text width
         * @return this Builder
         */
        public Builder textWidth(int textWidth) {
            this.textWidth = textWidth;
            return this;
        }

        /**
         * Sets the scroll speed.
         *
         * @param speed the delay in milliseconds per step
         * @return this Builder
         */
        public Builder speed(int speed) {
            this.speed = speed;
            return this;
        }

        /**
         * Sets the text content.
         *
         * @param text the text to render
         * @return this Builder
         */
        public Builder text(String text) {
            this.textString = text;
            return this;
        }

        /**
         * Sets the text color.
         *
         * @param colorHex the hexadecimal color string (e.g. "#FF0000")
         * @return this Builder
         */
        public Builder color(String colorHex) {
            this.color = colorHex.startsWith("#") ? colorHex : "#" + colorHex;
            return this;
        }

        /**
         * Aligns the text to the left.
         *
         * @return this Builder
         */
        public Builder alignLeft() {
            this.align = 1;
            return this;
        }

        /**
         * Aligns the text to the center.
         *
         * @return this Builder
         */
        public Builder alignCenter() {
            this.align = 2;
            return this;
        }

        /**
         * Aligns the text to the right.
         *
         * @return this Builder
         */
        public Builder alignRight() {
            this.align = 3;
            return this;
        }

        /**
         * Builds the {@link PixooText} object.
         *
         * @return a new PixooText instance
         */
        public PixooText build() {
            return new PixooText(textId, x, y, dir, font, textWidth, speed, textString, color, align);
        }
    }
}
