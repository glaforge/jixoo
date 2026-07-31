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
package com.divoom.pixoo64.image;

import com.divoom.pixoo64.api.exception.PixooException;
import com.divoom.pixoo64.model.PixooAnimation;
import com.divoom.pixoo64.model.PixooFrame;
import com.divoom.pixoo64.model.RawRgbBuffer;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.InputStream;

/**
 * Image processing utilities for loading, scaling, cropping, and fitting images into 64x64.
 */
public class ImageProcessor {

    /**
     * Scale mode for fitting images into the 64x64 canvas.
     */
    public enum ScaleMode {
        FIT_CENTER,
        FILL_CROP,
        STRETCH
    }

    /**
     * Loads an image from the specified path.
     *
     * @param path the path to load the image from
     * @return the loaded BufferedImage
     * @throws PixooException if the image cannot be loaded
     */
    public static BufferedImage load(Path path) {
        try (InputStream is = Files.newInputStream(path)) {
            BufferedImage image = ImageIO.read(is);
            if (image == null) {
                throw new PixooException("Failed to decode image from path: " + path);
            }
            return image;
        } catch (Exception e) {
            throw new PixooException("Error reading image path: " + path, e);
        }
    }

    /**
     * Loads an image from the specified input stream.
     *
     * @param inputStream the stream to load the image from
     * @return the loaded BufferedImage
     * @throws PixooException if the image cannot be loaded
     */
    public static BufferedImage load(InputStream inputStream) {
        try {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                throw new PixooException("Failed to decode image from input stream");
            }
            return image;
        } catch (Exception e) {
            throw new PixooException("Error reading image from stream", e);
        }
    }

    /**
     * Resizes and fits an image into a 64x64 canvas using FIT_CENTER scale mode.
     *
     * @param input the input image
     * @return the 64x64 processed image
     */
    public static BufferedImage resizeAndFit(BufferedImage input) {
        return resizeAndFit(input, ScaleMode.FIT_CENTER);
    }

    /**
     * Resizes and fits an image into a 64x64 canvas using the specified scale mode.
     *
     * @param input     the input image
     * @param scaleMode the scaling strategy to use
     * @return the 64x64 processed image
     */
    public static BufferedImage resizeAndFit(BufferedImage input, ScaleMode scaleMode) {
        if (input.getWidth() == RawRgbBuffer.WIDTH && input.getHeight() == RawRgbBuffer.HEIGHT) {
            return ensureType(input);
        }

        BufferedImage target = new BufferedImage(RawRgbBuffer.WIDTH, RawRgbBuffer.HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = target.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        int srcW = input.getWidth();
        int srcH = input.getHeight();

        switch (scaleMode) {
            case STRETCH -> g2d.drawImage(input, 0, 0, RawRgbBuffer.WIDTH, RawRgbBuffer.HEIGHT, null);
            case FIT_CENTER -> {
                double scale = Math.min((double) RawRgbBuffer.WIDTH / srcW, (double) RawRgbBuffer.HEIGHT / srcH);
                int drawW = (int) Math.round(srcW * scale);
                int drawH = (int) Math.round(srcH * scale);
                int x = (RawRgbBuffer.WIDTH - drawW) / 2;
                int y = (RawRgbBuffer.HEIGHT - drawH) / 2;
                g2d.drawImage(input, x, y, drawW, drawH, null);
            }
            case FILL_CROP -> {
                double scale = Math.max((double) RawRgbBuffer.WIDTH / srcW, (double) RawRgbBuffer.HEIGHT / srcH);
                int drawW = (int) Math.round(srcW * scale);
                int drawH = (int) Math.round(srcH * scale);
                int x = (RawRgbBuffer.WIDTH - drawW) / 2;
                int y = (RawRgbBuffer.HEIGHT - drawH) / 2;
                g2d.drawImage(input, x, y, drawW, drawH, null);
            }
        }
        g2d.dispose();
        return target;
    }

    /**
     * Processes an image and wraps it in a single-frame PixooAnimation.
     *
     * @param input the input image
     * @return a single-frame animation containing the processed image
     */
    public static PixooAnimation processImage(BufferedImage input) {
        BufferedImage processed = resizeAndFit(input);
        return PixooAnimation.singleImage(processed);
    }

    private static BufferedImage ensureType(BufferedImage input) {
        if (input.getType() == BufferedImage.TYPE_INT_RGB) {
            return input;
        }
        BufferedImage copy = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = copy.createGraphics();
        g2d.drawImage(input, 0, 0, null);
        g2d.dispose();
        return copy;
    }
}
