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

import io.github.glaforge.jixoo.api.exception.PixooException;
import io.github.glaforge.jixoo.model.PixooAnimation;
import io.github.glaforge.jixoo.model.PixooFrame;
import io.github.glaforge.jixoo.model.RawRgbBuffer;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Pure-Java decoder for proprietary Divoom Cloud binary animations (formats 26 / 0x1A and 18 / 0x12).
 * <p>
 * Decodes quantized palette index bitstreams and untangles $16 \times 16$ tile layouts
 * into standard row-major RGB pixel buffers (12,288 bytes per 64x64 frame),
 * ready for direct display playback on the Pixoo 64 without any third-party dependencies.
 */
public final class DivoomAssetDecoder {

    private DivoomAssetDecoder() {}

    /**
     * Decodes a binary Divoom animation from an {@link InputStream}.
     *
     * @param stream the input stream
     * @return decoded {@link PixooAnimation}
     */
    public static PixooAnimation decode(InputStream stream) {
        try {
            return decode(stream.readAllBytes());
        } catch (PixooException e) {
            throw e;
        } catch (Exception e) {
            throw new PixooException("Failed to read Divoom binary stream: " + e.getMessage(), e);
        }
    }

    /**
     * Decodes a binary Divoom animation from a file path.
     *
     * @param path path to the .bin / asset file
     * @return decoded {@link PixooAnimation}
     */
    public static PixooAnimation decode(Path path) {
        try {
            return decode(Files.readAllBytes(path));
        } catch (IOException e) {
            throw new PixooException("Failed to read file " + path + ": " + e.getMessage(), e);
        }
    }

    /**
     * Decodes raw binary Divoom container bytes into a {@link PixooAnimation}.
     *
     * @param data raw asset bytes
     * @return decoded {@link PixooAnimation}
     */
    public static PixooAnimation decode(byte[] data) {
        if (data == null || data.length < 6) {
            throw new PixooException("Invalid Divoom binary asset: data too short (" + (data != null ? data.length : 0) + " bytes)");
        }

        int format = data[0] & 0xFF;
        // Format 26 (0x1A) and format 18 (0x12) are standard 64x64 animations
        if (format != 26 && format != 18 && format != 35) {
            throw new PixooException("Unsupported Divoom asset format: " + format + " (expected 26, 18, or 35)");
        }

        int totalFrames = data[1] & 0xFF;
        if (totalFrames <= 0) {
            throw new PixooException("Invalid Divoom asset: frame count is 0");
        }

        int delayMs = ((data[2] & 0xFF) << 8) | (data[3] & 0xFF);
        if (delayMs <= 0) {
            delayMs = 100; // default 100 ms per frame
        }

        int rows = data[4] & 0xFF;
        int cols = data[5] & 0xFF;
        if (rows <= 0) rows = 4;
        if (cols <= 0) cols = 4;

        int width = cols * 16;
        int height = rows * 16;

        List<PixooFrame> frames = new ArrayList<>(totalFrames);
        int pos = 6;

        for (int frameIdx = 0; frameIdx < totalFrames; frameIdx++) {
            if (pos + 4 > data.length) {
                break;
            }

            int frameLen = ((data[pos] & 0xFF) << 24) |
                           ((data[pos + 1] & 0xFF) << 16) |
                           ((data[pos + 2] & 0xFF) << 8) |
                           (data[pos + 3] & 0xFF);

            int frameStart = pos + 4;
            if (frameStart + frameLen > data.length) {
                frameLen = data.length - frameStart;
            }

            byte[] frameRaw = new byte[frameLen];
            System.arraycopy(data, frameStart, frameRaw, 0, frameLen);

            byte[] decodedTiledRgb = decodeFrame(frameRaw, width * height);
            byte[] rowMajorRgb = untangleTiles(decodedTiledRgb, width, height, rows, cols);

            frames.add(new PixooFrame(rowMajorRgb, delayMs));
            pos = frameStart + frameLen;
        }

        if (frames.isEmpty()) {
            throw new PixooException("No valid frames could be decoded from Divoom asset");
        }

        return new PixooAnimation(frames);
    }

    /**
     * Decodes an individual 0x0C frame payload into tiled RGB bytes.
     */
    private static byte[] decodeFrame(byte[] frameData, int numPixels) {
        if (frameData.length < 8) {
            return new byte[numPixels * 3];
        }

        // Fast path for solid-color frames: AA 0B 00 F4 01 0C 01 00 R G B
        if (frameData.length >= 11 &&
                (frameData[0] & 0xFF) == 0xAA &&
                (frameData[5] & 0xFF) == 0x0C &&
                (frameData[6] & 0xFF) == 0x01 &&
                frameData[7] == 0x00) {
            byte r = frameData[8];
            byte g = frameData[9];
            byte b = frameData[10];
            byte[] solid = new byte[numPixels * 3];
            for (int p = 0; p < numPixels; p++) {
                solid[p * 3] = r;
                solid[p * 3 + 1] = g;
                solid[p * 3 + 2] = b;
            }
            return solid;
        }

        int uVar13 = frameData[6] & 0xFF;
        int iVar11 = uVar13 * 3;
        int bVar9;

        if (uVar13 == 0) {
            bVar9 = 8;
            iVar11 = 768;
        } else {
            bVar9 = 0xFF;
            int bVar15 = 1;
            while (true) {
                if ((uVar13 & 1) != 0) {
                    boolean bVar18 = (bVar9 == 0xFF);
                    bVar9 = bVar15;
                    if (bVar18) {
                        bVar9 = bVar15 - 1;
                    }
                }
                int uVar14 = uVar13 & 0xFFFE;
                bVar15++;
                uVar13 = uVar14 >> 1;
                if (uVar14 == 0) {
                    break;
                }
            }
        }

        int pos = (iVar11 + 8) & 0xFFFF;
        byte[] output = new byte[numPixels * 3];

        for (int pixelIdx = 0; pixelIdx < numPixels; pixelIdx++) {
            int colorIdx = getDotInfo(frameData, pos, pixelIdx, bVar9);
            int tPos = pixelIdx * 3;
            if (colorIdx == -1) {
                output[tPos] = output[tPos + 1] = output[tPos + 2] = 0;
            } else {
                int colorPos = 8 + colorIdx * 3;
                if (colorPos + 2 < frameData.length) {
                    output[tPos] = frameData[colorPos];
                    output[tPos + 1] = frameData[colorPos + 1];
                    output[tPos + 2] = frameData[colorPos + 2];
                } else {
                    output[tPos] = output[tPos + 1] = output[tPos + 2] = 0;
                }
            }
        }
        return output;
    }

    /**
     * Extracts a palette index from the bit-packed pixel stream.
     */
    private static int getDotInfo(byte[] data, int pos, int pixelIdx, int bits) {
        if (pos >= data.length) return -1;
        int uVar2 = (bits * pixelIdx) & 7;
        int uVar4 = (int) (((long) bits * pixelIdx * 65536L) >> 19);

        if (bits < 9) {
            int uVar3 = bits + uVar2;
            if (uVar3 < 9) {
                int idx = pos + uVar4;
                if (idx >= data.length) return -1;
                int uVar6 = ((data[idx] & 0xFF) << (8 - uVar3)) & 0xFF;
                uVar6 >>= (uVar2 + (8 - uVar3));
                return uVar6;
            } else {
                int idx0 = pos + uVar4;
                int idx1 = idx0 + 1;
                if (idx1 >= data.length || idx0 >= data.length) return -1;
                int uVar6 = ((data[idx1] & 0xFF) << (16 - uVar3)) & 0xFF;
                uVar6 >>= (16 - uVar3);
                uVar6 &= 0xFFFF;
                uVar6 <<= (8 - uVar2);
                uVar6 |= ((data[idx0] & 0xFF) >> uVar2);
                return uVar6;
            }
        }
        return -1;
    }

    /**
     * Untangles the $16 \times 16$ tile order into standard row-major RGB scanlines.
     */
    private static byte[] untangleTiles(byte[] frameData, int width, int height, int rowCount, int colCount) {
        int frameSize = rowCount * colCount * 16 * 16 * 3;
        byte[] out = new byte[width * height * 3];

        int pos = 0;
        int x = 0;
        int y = 0;
        int gridX = 0;
        int gridY = 0;

        while (pos < frameSize && pos + 2 < frameData.length) {
            byte r = frameData[pos];
            byte g = frameData[pos + 1];
            byte b = frameData[pos + 2];

            int realX = x + (gridX * 16);
            int realY = y + (gridY * 16);

            if (realX < width && realY < height) {
                int targetIdx = (realY * width + realX) * 3;
                out[targetIdx] = r;
                out[targetIdx + 1] = g;
                out[targetIdx + 2] = b;
            }

            x++;
            pos += 3;
            if ((pos / 3) % 16 == 0) {
                x = 0;
                y++;
            }
            if ((pos / 3) % 256 == 0) {
                x = 0;
                y = 0;
                gridX++;
                if (gridX == rowCount) {
                    gridX = 0;
                    gridY++;
                }
            }
        }
        return out;
    }

    /**
     * Converts a single {@link PixooFrame} to a {@link BufferedImage}.
     *
     * @param frame the frame to convert
     * @return a 64x64 RGB BufferedImage
     */
    public static BufferedImage toBufferedImage(PixooFrame frame) {
        BufferedImage img = new BufferedImage(RawRgbBuffer.WIDTH, RawRgbBuffer.HEIGHT, BufferedImage.TYPE_INT_RGB);
        byte[] rgb = frame.rgbData();
        for (int y = 0; y < RawRgbBuffer.HEIGHT; y++) {
            for (int x = 0; x < RawRgbBuffer.WIDTH; x++) {
                int idx = (y * RawRgbBuffer.WIDTH + x) * 3;
                int r = rgb[idx] & 0xFF;
                int g = rgb[idx + 1] & 0xFF;
                int b = rgb[idx + 2] & 0xFF;
                img.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
        return img;
    }

    /**
     * Converts all frames in a {@link PixooAnimation} into {@link BufferedImage} instances.
     *
     * @param animation the animation
     * @return list of BufferedImages
     */
    public static List<BufferedImage> toBufferedImages(PixooAnimation animation) {
        List<BufferedImage> images = new ArrayList<>(animation.frames().size());
        for (PixooFrame frame : animation.frames()) {
            images.add(toBufferedImage(frame));
        }
        return images;
    }

    /**
     * Converts all frames in a {@link PixooAnimation} into {@link PixooImage} instances.
     *
     * @param animation the animation
     * @return list of PixooImages
     */
    public static List<PixooImage> toPixooImages(PixooAnimation animation) {
        List<PixooImage> images = new ArrayList<>(animation.frames().size());
        for (PixooFrame frame : animation.frames()) {
            byte[] rgb = frame.rgbData();
            int[] argb = new int[RawRgbBuffer.WIDTH * RawRgbBuffer.HEIGHT];
            for (int i = 0; i < argb.length; i++) {
                int r = rgb[i * 3] & 0xFF;
                int g = rgb[i * 3 + 1] & 0xFF;
                int b = rgb[i * 3 + 2] & 0xFF;
                argb[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
            }
            images.add(new PixooImage(RawRgbBuffer.WIDTH, RawRgbBuffer.HEIGHT, argb));
        }
        return images;
    }
}
