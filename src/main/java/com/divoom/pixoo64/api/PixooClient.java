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

import com.divoom.pixoo64.image.GifDecoder;
import com.divoom.pixoo64.image.ImageProcessor;
import com.divoom.pixoo64.internal.HttpPixooClient;
import com.divoom.pixoo64.model.PixooAnimation;
import com.divoom.pixoo64.model.PixooFrame;
import com.divoom.pixoo64.model.command.PixooCommand;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.io.InputStream;
import java.time.Duration;

/**
 * Main interface for interacting with the Divoom Pixoo64 display.
 */
public interface PixooClient {

    /**
     * Creates a client connected to the given IP address with default settings.
     *
     * @param ipAddress the IP address of the Pixoo64 device
     * @return a new instance of PixooClient
     */
    static PixooClient create(String ipAddress) {
        return builder().ipAddress(ipAddress).build();
    }

    /**
     * Returns a builder for configuring a PixooClient instance.
     *
     * @return a new Builder
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Executes a raw command against the device.
     *
     * @param command the PixooCommand to execute
     * @return the device's response
     */
    PixooResponse executeCommand(PixooCommand command);

    /**
     * Switches the device to the specified channel (Clock, Cloud, Visualizer, Custom, Black Screen).
     *
     * @param channel the target channel to switch to
     * @return the device's response
     */
    PixooResponse selectChannel(PixooChannel channel);

    /**
     * Resets the internal HTTP GIF animation state machine buffer.
     *
     * @return the device's response
     */
    PixooResponse resetAnimationBuffer();

    /**
     * Transmits a full animation sequence (static or multi-frame) to the display.
     * Automatically resets the animation buffer before transmission.
     *
     * @param animation the animation to send
     * @return the device's response to the final frame sent
     */
    PixooResponse sendAnimation(PixooAnimation animation);

    /**
     * Sends a single frame to the display.
     *
     * @param frame the frame to send
     * @return the device's response
     */
    default PixooResponse sendFrame(PixooFrame frame) {
        return sendAnimation(PixooAnimation.singleFrame(frame));
    }

    /**
     * Resizes and sends a static BufferedImage to the display.
     *
     * @param image the image to process and send
     * @return the device's response
     */
    default PixooResponse sendImage(BufferedImage image) {
        return sendAnimation(ImageProcessor.processImage(image));
    }

    /**
     * Loads, resizes, and sends an image file from a Path to the display.
     *
     * @param path the path to the image file
     * @return the device's response
     */
    default PixooResponse sendImage(Path path) {
        return sendImage(ImageProcessor.load(path));
    }

    /**
     * Decodes and displays an animated or static GIF from an InputStream.
     *
     * @param gifStream the input stream containing the GIF data
     * @return the device's response to the final frame sent
     */
    default PixooResponse sendGif(InputStream gifStream) {
        return sendAnimation(GifDecoder.decode(gifStream));
    }

    /**
     * Decodes and displays an animated or static GIF from a Path.
     *
     * @param gifPath the path to the GIF file
     * @return the device's response to the final frame sent
     */
    default PixooResponse sendGif(Path gifPath) {
        return sendAnimation(GifDecoder.decode(gifPath));
    }

    /**
     * Instructs the Pixoo64 to download and render a GIF from a remote URL.
     *
     * @param gifUrl the HTTP/HTTPS URL of the GIF
     * @return the device's response
     */
    PixooResponse sendRemoteGifUrl(String gifUrl);

    /**
     * Renders text on the hardware text engine.
     *
     * @param text the text configuration to render
     * @return the device's response
     */
    PixooResponse sendText(PixooText text);

    /**
     * Clears all hardware text elements from the screen.
     *
     * @return the device's response
     */
    PixooResponse clearText();

    /**
     * Sets the LED matrix brightness (0-100).
     *
     * @param brightness the brightness level (0-100)
     * @return the device's response
     */
    PixooResponse setBrightness(int brightness);

    /**
     * Turns the screen display on or off.
     *
     * @param on true to turn on, false to turn off
     * @return the device's response
     */
    PixooResponse setScreenState(boolean on);

    /**
     * Sets the physical screen rotation angle.
     *
     * @param rotation the rotation angle to set
     * @return the device's response
     */
    PixooResponse setRotation(PixooRotation rotation);

    /**
     * Triggers the internal piezoelectric buzzer sound rhythm.
     *
     * @param activeMs Duration of the beep sound in milliseconds
     * @param offMs    Duration of silence between beeps in milliseconds
     * @param totalMs  Total duration of the alarm pattern in milliseconds
     * @return the device's response
     */
    PixooResponse playBuzzer(int activeMs, int offMs, int totalMs);

    /**
     * Builder class for constructing {@link PixooClient} instances.
     */
    class Builder {
        private String ipAddress;
        private int port = 80;
        private Duration connectTimeout = Duration.ofSeconds(5);
        private Duration requestTimeout = Duration.ofSeconds(5);
        private boolean autoSwitchToCustomChannel = true;

        /** Creates a new Builder instance. */
        Builder() {}

        /**
         * Sets the target device IP address.
         *
         * @param ipAddress the IP address
         * @return this Builder
         */
        public Builder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        /**
         * Sets the target device port (default is 80).
         *
         * @param port the HTTP port
         * @return this Builder
         */
        public Builder port(int port) {
            this.port = port;
            return this;
        }

        /**
         * Sets the connection timeout.
         *
         * @param timeout the connection timeout
         * @return this Builder
         */
        public Builder connectTimeout(Duration timeout) {
            this.connectTimeout = timeout;
            return this;
        }

        /**
         * Sets the request timeout.
         *
         * @param timeout the request timeout
         * @return this Builder
         */
        public Builder requestTimeout(Duration timeout) {
            this.requestTimeout = timeout;
            return this;
        }

        /**
         * Sets whether to automatically switch to the custom channel when sending content.
         *
         * @param autoSwitch true to auto switch, false otherwise
         * @return this Builder
         */
        public Builder autoSwitchToCustomChannel(boolean autoSwitch) {
            this.autoSwitchToCustomChannel = autoSwitch;
            return this;
        }

        /**
         * Builds the PixooClient instance.
         *
         * @return a new PixooClient
         */
        public PixooClient build() {
            if (ipAddress == null || ipAddress.isBlank()) {
                throw new IllegalArgumentException("IP address must be specified");
            }
            return new HttpPixooClient(ipAddress, port, connectTimeout, requestTimeout, autoSwitchToCustomChannel);
        }
    }
}
