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

import io.github.glaforge.jixoo.image.GifDecoder;
import io.github.glaforge.jixoo.image.ImageProcessor;
import io.github.glaforge.jixoo.image.PixooImage;
import io.github.glaforge.jixoo.internal.HttpPixooClient;
import io.github.glaforge.jixoo.model.PixooAnimation;
import io.github.glaforge.jixoo.model.PixooFrame;
import io.github.glaforge.jixoo.model.RawRgbBuffer;
import io.github.glaforge.jixoo.model.command.PixooCommand;

import io.github.glaforge.jixoo.model.sys.ChannelConfig;
import io.github.glaforge.jixoo.model.sys.SysConfig;
import io.github.glaforge.jixoo.model.tool.NoiseStatus;
import io.github.glaforge.jixoo.model.tool.ScoreboardStatus;
import io.github.glaforge.jixoo.model.tool.StopwatchAction;
import io.github.glaforge.jixoo.model.tool.StopwatchStatus;
import io.github.glaforge.jixoo.model.tool.TimerStatus;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;

/**
 * Main interface for interacting with the Divoom Pixoo64 display.
 */
public interface PixooClient extends AutoCloseable {

    /**
     * Closes underlying network client resources.
     */
    @Override
    default void close() {
        // Default no-op
    }

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
     * Fills the display screen with a solid RGB color.
     *
     * @param r red component (0-255)
     * @param g green component (0-255)
     * @param b blue component (0-255)
     * @return the device's response
     */
    default PixooResponse sendColor(int r, int g, int b) {
        byte[] rgbData = RawRgbBuffer.fromRgb(r, g, b);
        return sendFrame(new PixooFrame(rgbData, 60000));
    }

    /**
     * Fills the display screen with a solid Color.
     *
     * @param color the Color to display
     * @return the device's response
     */
    default PixooResponse sendColor(Color color) {
        if (color == null) {
            throw new IllegalArgumentException("Color cannot be null");
        }
        return sendColor(color.getRed(), color.getGreen(), color.getBlue());
    }

    /**
     * Fills the display screen with a solid hexadecimal CSS color string (e.g. "#23ED23" or "23ED23").
     *
     * @param hexColor the hex color string
     * @return the device's response
     */
    default PixooResponse sendColor(String hexColor) {
        int[] rgb = parseHexRgb(hexColor);
        return sendColor(rgb[0], rgb[1], rgb[2]);
    }

    /**
     * Parses a CSS hexadecimal color string (e.g. "#23ED23", "23ED23", "#F00", "F00") into RGB components.
     *
     * @param colorStr the color string to parse
     * @return an int array containing [red, green, blue] in the range 0-255
     * @throws IllegalArgumentException if the string format is invalid
     */
    static int[] parseHexRgb(String colorStr) {
        if (colorStr == null || colorStr.isBlank()) {
            throw new IllegalArgumentException("Color string cannot be null or empty");
        }
        String hex = colorStr.trim();
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        if (hex.length() == 3) {
            char r = hex.charAt(0);
            char g = hex.charAt(1);
            char b = hex.charAt(2);
            hex = "" + r + r + g + g + b + b;
        }
        if (hex.length() != 6) {
            throw new IllegalArgumentException("Invalid color format: '" + colorStr + "'. Expected hex format like '#23ED23' or '23ED23'.");
        }
        try {
            int rgb = Integer.parseInt(hex, 16);
            return new int[] {
                    (rgb >> 16) & 0xFF,
                    (rgb >> 8) & 0xFF,
                    rgb & 0xFF
            };
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid hexadecimal color: '" + colorStr + "'.", e);
        }
    }

    /**
     * Parses a CSS hexadecimal color string (e.g. "#23ED23", "23ED23", "#F00", "F00") into a {@link Color}.
     *
     * @param colorStr the color string to parse
     * @return the parsed Color
     * @throws IllegalArgumentException if the string format is invalid
     */
    static Color parseColor(String colorStr) {
        int[] rgb = parseHexRgb(colorStr);
        return new Color(rgb[0], rgb[1], rgb[2]);
    }

    /**
     * Resizes and sends a PixooImage to the display.
     *
     * @param image the image to process and send
     * @return the device's response
     */
    default PixooResponse sendImage(PixooImage image) {
        return sendAnimation(ImageProcessor.processImage(image));
    }

    /**
     * Resizes and sends a static BufferedImage to the display.
     *
     * @param image the image to process and send
     * @return the device's response
     */
    default PixooResponse sendImage(BufferedImage image) {
        return sendImage(PixooImage.fromBufferedImage(image));
    }

    /**
     * Loads, resizes, and sends an image file from a Path to the display using pure-Java decoders.
     *
     * @param path the path to the image file
     * @return the device's response
     */
    default PixooResponse sendImage(Path path) {
        return sendImage(ImageProcessor.loadImage(path));
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
     * Checks if the screen matrix is currently turned on.
     *
     * @return true if the screen is on, false if off / in standby
     */
    boolean isScreenOn();

    /**
     * Synchronizes the device's real-time clock to the current system time.
     *
     * @return the device's response
     */
    default PixooResponse syncTime() {
        return syncTime(Instant.now());
    }

    /**
     * Synchronizes the device's real-time clock to the specified instant.
     *
     * @param instant the timestamp to set
     * @return the device's response
     */
    PixooResponse syncTime(Instant instant);

    /**
     * Retrieves the device's system configuration settings.
     *
     * @return the system configuration
     */
    SysConfig getSystemConfig();

    /**
     * Updates the device's system configuration settings.
     *
     * @param config the configuration settings to update
     * @return the device's response
     */
    PixooResponse setSystemConfig(SysConfig config);

    /**
     * Retrieves the device's channel configuration settings.
     *
     * @return the channel configuration
     */
    ChannelConfig getChannelConfig();

    /**
     * Sets the default channel displayed when the device boots up.
     *
     * @param channel the startup channel
     * @return the device's response
     */
    PixooResponse setStartupChannel(PixooChannel channel);

    /**
     * Retrieves the default channel displayed when the device boots up.
     *
     * @return the startup channel
     */
    PixooChannel getStartupChannel();

    /**
     * Selects an active clock face by its clock ID.
     *
     * @param clockId the clock face identifier
     * @return the device's response
     */
    PixooResponse setClockId(int clockId);

    /**
     * Retrieves the active clock face ID.
     *
     * @return the clock face ID
     */
    int getClockId();

    /**
     * Sets the active custom channel gallery page index (0, 1, or 2).
     *
     * @param pageIndex the page index (0..2)
     * @return the device's response
     */
    PixooResponse setCustomPageIndex(int pageIndex);

    /**
     * Retrieves the active custom channel gallery page index.
     *
     * @return the custom page index (0..2)
     */
    int getCustomPageIndex();

    /**
     * Controls the built-in stopwatch hardware tool.
     *
     * @param action the action to perform (START, STOP, RESET)
     * @return the device's response
     */
    PixooResponse setStopwatch(StopwatchAction action);

    /**
     * Retrieves the current stopwatch state.
     *
     * @return stopwatch status
     */
    StopwatchStatus getStopwatch();

    /**
     * Configures and starts or stops the countdown timer tool.
     *
     * @param minute countdown minutes
     * @param second countdown seconds
     * @param start  true to start the countdown, false to stop/pause
     * @return the device's response
     */
    PixooResponse setTimer(int minute, int second, boolean start);

    /**
     * Retrieves the countdown timer tool status.
     *
     * @return timer status
     */
    TimerStatus getTimer();

    /**
     * Sets the scores displayed on the dual-team scoreboard tool.
     *
     * @param blueScore blue team score (0..999)
     * @param redScore  red team score (0..999)
     * @return the device's response
     */
    PixooResponse setScoreboard(int blueScore, int redScore);

    /**
     * Retrieves the scores from the scoreboard tool.
     *
     * @return scoreboard status
     */
    ScoreboardStatus getScoreboard();

    /**
     * Starts or stops the ambient noise decibel meter tool.
     *
     * @param start true to start the noise meter, false to stop
     * @return the device's response
     */
    PixooResponse setNoiseStatus(boolean start);

    /**
     * Retrieves the ambient noise decibel meter status.
     *
     * @return noise meter status
     */
    NoiseStatus getNoiseStatus();

    /**
     * Checks if the device's onboard flash storage is full.
     *
     * @return true if full, false otherwise
     */
    boolean isStorageFull();

    /**
     * Builder class for constructing {@link PixooClient} instances.
     */
    class Builder {
        private String ipAddress;
        private int port = 80;
        private Duration connectTimeout = Duration.ofSeconds(5);
        private Duration requestTimeout = Duration.ofSeconds(5);
        private boolean autoSwitchToCustomChannel = false;

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
