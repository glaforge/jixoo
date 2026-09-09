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
package io.github.glaforge.jixoo.cloud;

import io.github.glaforge.jixoo.model.PixooAnimation;
import io.github.glaforge.jixoo.model.PixooFrame;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * Encodes animations and images into Divoom's native 64x64 binary animation format (type 0x1A).
 * <p>
 * This format is accepted by {@code Cloud/UploadPicture} and parsed by Divoom firmware
 * when setting persistent custom channels or uploading to the cloud gallery.
 */
public class DivoomAnimationEncoder {

    private static final byte MAGIC_ANIMATION_64 = 0x1A;
    private static final byte MAGIC_FRAME_HEADER = (byte) 0xAA;
    private static final byte ENCRYPT_TYPE = 0x0C;
    private static final int WIDTH = 64;
    private static final int HEIGHT = 64;
    private static final int PIXEL_COUNT = WIDTH * HEIGHT; // 4096

    // Precomputed bits-per-pixel table matching Divoom libtimebox.so (gdivoom_image_bits_table)
    private static final int[] BITS_TABLE = new int[257];

    static {
        BITS_TABLE[0] = 0;
        BITS_TABLE[1] = 0;
        BITS_TABLE[2] = 1;
        for (int i = 3; i <= 4; i++) BITS_TABLE[i] = 2;
        for (int i = 5; i <= 8; i++) BITS_TABLE[i] = 3;
        for (int i = 9; i <= 16; i++) BITS_TABLE[i] = 4;
        for (int i = 17; i <= 32; i++) BITS_TABLE[i] = 5;
        for (int i = 33; i <= 64; i++) BITS_TABLE[i] = 6;
        for (int i = 65; i <= 128; i++) BITS_TABLE[i] = 7;
        for (int i = 129; i <= 256; i++) BITS_TABLE[i] = 8;
    }

    /**
     * Encodes a {@link PixooAnimation} into a Divoom 64x64 binary format byte array.
     *
     * @param animation the animation to encode
     * @return the raw binary payload ready for Divoom Cloud upload
     */
    public static byte[] encode(PixooAnimation animation) {
        if (animation == null || animation.frames().isEmpty()) {
            throw new IllegalArgumentException("Animation must contain at least one frame");
        }

        int frameCount = animation.frameCount();
        int speed = animation.frames().get(0).delayMs();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream dos = new DataOutputStream(baos)) {
            // Overall header:
            // 0x1A (magic), total_frames, speed (big-endian short), row_count (4), col_count (4)
            dos.writeByte(MAGIC_ANIMATION_64);
            dos.writeByte(frameCount);
            dos.writeShort(speed);
            dos.writeByte(4); // row count (4 blocks of 16 = 64)
            dos.writeByte(4); // column count (4 blocks of 16 = 64)

            for (PixooFrame frame : animation.frames()) {
                byte[] framePayload = encodeFrame(frame);
                // 4-byte big-endian frame length
                dos.writeInt(framePayload.length);
                dos.write(framePayload);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to encode Divoom animation", e);
        }

        return baos.toByteArray();
    }

    /**
     * Reorders 64x64 raw RGB bytes from raster order (row-by-row) to 4x4 blocks of 16x16 tiles,
     * matching Divoom firmware requirement.
     */
    static byte[] tileReorder64(byte[] rawRgb) {
        byte[] tiled = new byte[PIXEL_COUNT * 3];
        int outIdx = 0;
        for (int blockY = 0; blockY < 4; blockY++) {
            for (int blockX = 0; blockX < 4; blockX++) {
                for (int yInBlock = 0; yInBlock < 16; yInBlock++) {
                    int y = blockY * 16 + yInBlock;
                    for (int xInBlock = 0; xInBlock < 16; xInBlock++) {
                        int x = blockX * 16 + xInBlock;
                        int inIdx = (y * 64 + x) * 3;
                        tiled[outIdx] = rawRgb[inIdx];
                        tiled[outIdx + 1] = rawRgb[inIdx + 1];
                        tiled[outIdx + 2] = rawRgb[inIdx + 2];
                        outIdx += 3;
                    }
                }
            }
        }
        return tiled;
    }

    private static byte[] encodeFrame(PixooFrame frame) throws IOException {
        byte[] rawRgb = frame.rgbData();
        if (rawRgb.length != PIXEL_COUNT * 3) {
            throw new IllegalArgumentException("Frame RGB buffer must be exactly 12288 bytes (64x64x3)");
        }

        // 1. Reorder pixels into 16x16 tiles
        byte[] tiledRgb = tileReorder64(rawRgb);

        // 2. Build dynamic palette (up to 256 colors)
        Map<Integer, Integer> colorToPaletteIndex = new LinkedHashMap<>();
        List<int[]> paletteColors = new ArrayList<>();
        int[] pixelPaletteIndices = new int[PIXEL_COUNT];

        for (int i = 0; i < PIXEL_COUNT; i++) {
            int r = tiledRgb[i * 3] & 0xFF;
            int g = tiledRgb[i * 3 + 1] & 0xFF;
            int b = tiledRgb[i * 3 + 2] & 0xFF;
            int rgbKey = (r << 16) | (g << 8) | b;

            Integer paletteIdx = colorToPaletteIndex.get(rgbKey);
            if (paletteIdx == null) {
                if (paletteColors.size() < 256) {
                    paletteIdx = paletteColors.size();
                    colorToPaletteIndex.put(rgbKey, paletteIdx);
                    paletteColors.add(new int[]{r, g, b});
                } else {
                    paletteIdx = findClosestColor(r, g, b, paletteColors);
                }
            }
            pixelPaletteIndices[i] = paletteIdx;
        }

        int colorCount = paletteColors.size();
        if (colorCount == 0) {
            colorCount = 1;
            paletteColors.add(new int[]{0, 0, 0});
        }

        int bpp = BITS_TABLE[colorCount];
        int packedPixelBytes = 512 * bpp; // (4096 * bpp) / 8
        int frameDataLength = 8 + (colorCount * 3) + packedPixelBytes;

        int delay = frame.delayMs();

        // 3. Assemble Frame Header (8 bytes)
        // byte 0: 0xAA
        // byte 1-2: uint16_le frameDataLength
        // byte 3-4: uint16_le delay
        // byte 5: 0x0C
        // byte 6-7: uint16_le colorCount
        byte[] header = new byte[]{
                MAGIC_FRAME_HEADER,
                (byte) (frameDataLength & 0xFF),
                (byte) ((frameDataLength >> 8) & 0xFF),
                (byte) (delay & 0xFF),
                (byte) ((delay >> 8) & 0xFF),
                ENCRYPT_TYPE,
                (byte) (colorCount & 0xFF),
                (byte) ((colorCount >> 8) & 0xFF)
        };

        // 4. Assemble Palette (colorCount * 3 bytes)
        byte[] paletteBytes = new byte[colorCount * 3];
        for (int i = 0; i < colorCount; i++) {
            int[] color = paletteColors.get(i);
            paletteBytes[i * 3] = (byte) color[0];
            paletteBytes[i * 3 + 1] = (byte) color[1];
            paletteBytes[i * 3 + 2] = (byte) color[2];
        }

        // 5. Pack Pixel Indices with variable bit-width (bpp bits per pixel, little-endian bit order)
        byte[] packedBytes = new byte[packedPixelBytes];
        if (bpp > 0) {
            for (int p = 0; p < PIXEL_COUNT; p++) {
                int idx = pixelPaletteIndices[p];
                for (int b = 0; b < bpp; b++) {
                    int bit = (idx >> b) & 1;
                    int bitPos = p * bpp + b;
                    int bytePos = bitPos >> 3;
                    int bitInByte = bitPos & 7;
                    packedBytes[bytePos] |= (byte) (bit << bitInByte);
                }
            }
        }

        ByteArrayOutputStream frameBaos = new ByteArrayOutputStream(frameDataLength);
        frameBaos.write(header);
        frameBaos.write(paletteBytes);
        frameBaos.write(packedBytes);

        return frameBaos.toByteArray();
    }

    private static int findClosestColor(int r, int g, int b, List<int[]> palette) {
        int bestIdx = 0;
        int minDistance = Integer.MAX_VALUE;
        for (int i = 0; i < palette.size(); i++) {
            int[] c = palette.get(i);
            int dr = r - c[0];
            int dg = g - c[1];
            int db = b - c[2];
            int dist = dr * dr + dg * dg + db * db;
            if (dist < minDistance) {
                minDistance = dist;
                bestIdx = i;
            }
        }
        return bestIdx;
    }
}
