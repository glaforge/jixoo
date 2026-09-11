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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Pure-Java animated GIF89a encoder producing standards-compliant multi-frame GIF files
 * from {@link PixooAnimation} with zero AWT or native runtime dependencies.
 */
public final class GifEncoder {

    private GifEncoder() {}

    /**
     * Encodes a {@link PixooAnimation} into a GIF byte array.
     *
     * @param animation the animation to encode
     * @return GIF-encoded byte array
     */
    public static byte[] encode(PixooAnimation animation) {
        if (animation == null || animation.frames().isEmpty()) {
            throw new PixooException("Cannot encode empty animation to GIF");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            encode(animation, baos);
        } catch (IOException e) {
            throw new PixooException("Failed to encode GIF: " + e.getMessage(), e);
        }
        return baos.toByteArray();
    }

    /**
     * Encodes a {@link PixooAnimation} to an {@link OutputStream}.
     *
     * @param animation the animation to encode
     * @param out       the output stream
     * @throws IOException if writing fails
     */
    public static void encode(PixooAnimation animation, OutputStream out) throws IOException {
        if (animation == null || animation.frames().isEmpty()) {
            throw new PixooException("Cannot encode empty animation to GIF");
        }

        List<PixooFrame> frames = animation.frames();
        int width = 64;
        int height = 64;

        // 1. GIF Header: "GIF89a"
        out.write("GIF89a".getBytes(StandardCharsets.US_ASCII));

        // 2. Logical Screen Descriptor (7 bytes)
        writeUInt16LE(out, width);
        writeUInt16LE(out, height);

        // Compute global color table from first frame for maximum compatibility
        FramePalette firstPalette = FramePalette.fromFrame(frames.get(0));
        int gctSizeBits = firstPalette.bits; // 2..8
        int gctSize = 1 << gctSizeBits;
        int packedFields = 0x80 | 0x70 | (gctSizeBits - 1); // GCT Flag=1, Color Res=8 (0x70), GCT size=gctSizeBits-1
        out.write(packedFields);
        out.write(0); // Background Color Index
        out.write(0); // Pixel Aspect Ratio

        // Write Global Color Table
        firstPalette.writeColorTable(out, gctSize);

        // 3. Netscape 2.0 Application Extension (Infinite loop)
        out.write(0x21); // Extension Introducer
        out.write(0xFF); // Application Extension Label
        out.write(11);   // Block Size
        out.write("NETSCAPE2.0".getBytes(StandardCharsets.US_ASCII));
        out.write(3);    // Sub-block Length
        out.write(1);    // Sub-block ID (Loop count)
        writeUInt16LE(out, 0); // 0 = Loop infinitely
        out.write(0);    // Block Terminator

        // 4. Encode each frame
        for (PixooFrame frame : frames) {
            encodeFrame(frame, width, height, out);
        }

        // 5. GIF Trailer
        out.write(0x3B);
        out.flush();
    }

    /**
     * Encodes a {@link PixooAnimation} directly to a file on disk.
     *
     * @param animation the animation to encode
     * @param targetPath the destination path
     */
    public static void encode(PixooAnimation animation, Path targetPath) {
        try {
            if (targetPath.getParent() != null) {
                Files.createDirectories(targetPath.getParent());
            }
            byte[] data = encode(animation);
            Files.write(targetPath, data);
        } catch (IOException e) {
            throw new PixooException("Failed to write GIF to " + targetPath + ": " + e.getMessage(), e);
        }
    }

    private static void encodeFrame(PixooFrame frame, int width, int height, OutputStream out) throws IOException {
        FramePalette palette = FramePalette.fromFrame(frame);
        int paletteBits = palette.bits; // 2..8
        int colorCount = 1 << paletteBits;

        // Graphic Control Extension (GCE)
        out.write(0x21); // Extension Introducer
        out.write(0xF9); // Graphic Control Label
        out.write(4);    // Byte Count
        out.write(0x04); // Packed: disposal method 1 (do not dispose, keep current image)
        int delayCentiseconds = Math.max(1, frame.delayMs() / 10);
        writeUInt16LE(out, delayCentiseconds);
        out.write(0);    // Transparent Color Index
        out.write(0);    // Block Terminator

        // Image Descriptor
        out.write(0x2C); // Image Separator ','
        writeUInt16LE(out, 0); // Left
        writeUInt16LE(out, 0); // Top
        writeUInt16LE(out, width);
        writeUInt16LE(out, height);
        int imgPacked = 0x80 | (paletteBits - 1); // Local Color Table flag=1, size
        out.write(imgPacked);

        // Local Color Table
        palette.writeColorTable(out, colorCount);

        // Map frame pixels to palette indices
        byte[] pixelIndices = palette.mapPixels(frame.rgbData());

        // LZW Compression
        int minCodeSize = Math.max(2, paletteBits);
        out.write(minCodeSize);
        writeLzwData(pixelIndices, minCodeSize, out);
    }

    private static void writeUInt16LE(OutputStream out, int value) throws IOException {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
    }

    /**
     * Compresses pixel indices using the GIF LZW algorithm and writes them in sub-blocks of <= 255 bytes.
     */
    private static void writeLzwData(byte[] pixels, int minCodeSize, OutputStream out) throws IOException {
        int clearCode = 1 << minCodeSize;
        int endCode = clearCode + 1;

        BitOutputStream bitOut = new BitOutputStream(out);

        int codeSize = minCodeSize + 1;
        int maxCode = 1 << codeSize;
        int available = clearCode + 2;

        // Code dictionary mapping (prefix << 8 | suffix) -> code
        Map<Integer, Integer> table = new HashMap<>();

        // Start with Clear Code
        bitOut.writeBits(clearCode, codeSize);

        if (pixels.length == 0) {
            bitOut.writeBits(endCode, codeSize);
            bitOut.flush();
            return;
        }

        int prefix = pixels[0] & 0xFF;

        for (int i = 1; i < pixels.length; i++) {
            int c = pixels[i] & 0xFF;
            int key = (prefix << 8) | c;

            Integer found = table.get(key);
            if (found != null) {
                prefix = found;
            } else {
                bitOut.writeBits(prefix, codeSize);

                if (available < 4096) {
                    table.put(key, available++);
                    if (available > maxCode && codeSize < 12) {
                        codeSize++;
                        maxCode = 1 << codeSize;
                    }
                } else {
                    // Dictionary full: emit clear code and reset
                    bitOut.writeBits(clearCode, codeSize);
                    table.clear();
                    codeSize = minCodeSize + 1;
                    maxCode = 1 << codeSize;
                    available = clearCode + 2;
                }

                prefix = c;
            }
        }

        // Emit final prefix and end code
        bitOut.writeBits(prefix, codeSize);
        bitOut.writeBits(endCode, codeSize);
        bitOut.flush();
    }

    /**
     * Helper bitstream packer that outputs bytes in GIF sub-blocks of up to 255 bytes.
     */
    private static final class BitOutputStream {
        private final OutputStream out;
        private int bitBuffer = 0;
        private int bitCount = 0;
        private final byte[] block = new byte[255];
        private int blockIndex = 0;

        BitOutputStream(OutputStream out) {
            this.out = out;
        }

        void writeBits(int code, int numBits) throws IOException {
            bitBuffer |= (code << bitCount);
            bitCount += numBits;

            while (bitCount >= 8) {
                writeBlockByte((byte) (bitBuffer & 0xFF));
                bitBuffer >>>= 8;
                bitCount -= 8;
            }
        }

        void flush() throws IOException {
            if (bitCount > 0) {
                writeBlockByte((byte) (bitBuffer & 0xFF));
                bitBuffer = 0;
                bitCount = 0;
            }
            flushBlock();
            // GIF sub-block terminator
            out.write(0);
        }

        private void writeBlockByte(byte b) throws IOException {
            block[blockIndex++] = b;
            if (blockIndex == 255) {
                flushBlock();
            }
        }

        private void flushBlock() throws IOException {
            if (blockIndex > 0) {
                out.write(blockIndex);
                out.write(block, 0, blockIndex);
                blockIndex = 0;
            }
        }
    }

    /**
     * Palette builder supporting exact palettes up to 256 colors or adaptive color quantization.
     */
    private static final class FramePalette {
        final List<Integer> colors; // 24-bit 0xRRGGBB
        final int bits; // 2..8

        FramePalette(List<Integer> colors, int bits) {
            this.colors = colors;
            this.bits = bits;
        }

        static FramePalette fromFrame(PixooFrame frame) {
            byte[] rgb = frame.rgbData();
            int pixelCount = rgb.length / 3;

            Map<Integer, Integer> uniqueColors = new LinkedHashMap<>();
            for (int i = 0; i < pixelCount; i++) {
                int r = rgb[i * 3] & 0xFF;
                int g = rgb[i * 3 + 1] & 0xFF;
                int b = rgb[i * 3 + 2] & 0xFF;
                int color = (r << 16) | (g << 8) | b;
                uniqueColors.putIfAbsent(color, uniqueColors.size());
            }

            List<Integer> palette;
            if (uniqueColors.size() <= 256) {
                palette = new ArrayList<>(uniqueColors.keySet());
            } else {
                // Quantize to at most 256 colors using median cut or popularity
                palette = quantizeColors(uniqueColors.keySet(), 256);
            }

            if (palette.isEmpty()) {
                palette.add(0);
            }

            // GIF requires at least 4 colors (min 2 bits)
            int bits = 2;
            while ((1 << bits) < palette.size() && bits < 8) {
                bits++;
            }

            return new FramePalette(palette, bits);
        }

        byte[] mapPixels(byte[] rgb) {
            int pixelCount = rgb.length / 3;
            byte[] indices = new byte[pixelCount];

            Map<Integer, Integer> exactMap = new HashMap<>();
            for (int i = 0; i < colors.size(); i++) {
                exactMap.put(colors.get(i), i);
            }

            for (int i = 0; i < pixelCount; i++) {
                int r = rgb[i * 3] & 0xFF;
                int g = rgb[i * 3 + 1] & 0xFF;
                int b = rgb[i * 3 + 2] & 0xFF;
                int color = (r << 16) | (g << 8) | b;

                Integer idx = exactMap.get(color);
                if (idx != null) {
                    indices[i] = (byte) idx.intValue();
                } else {
                    // Find nearest color in palette
                    indices[i] = (byte) findNearestColorIndex(r, g, b);
                }
            }

            return indices;
        }

        private int findNearestColorIndex(int r, int g, int b) {
            int bestIdx = 0;
            int bestDist = Integer.MAX_VALUE;

            for (int i = 0; i < colors.size(); i++) {
                int c = colors.get(i);
                int cr = (c >> 16) & 0xFF;
                int cg = (c >> 8) & 0xFF;
                int cb = c & 0xFF;

                int dr = r - cr;
                int dg = g - cg;
                int db = b - cb;
                int dist = dr * dr + dg * dg + db * db;

                if (dist < bestDist) {
                    bestDist = dist;
                    bestIdx = i;
                    if (dist == 0) break;
                }
            }
            return bestIdx;
        }

        void writeColorTable(OutputStream out, int capacity) throws IOException {
            for (int color : colors) {
                out.write((color >> 16) & 0xFF);
                out.write((color >> 8) & 0xFF);
                out.write(color & 0xFF);
            }
            for (int i = colors.size(); i < capacity; i++) {
                out.write(0);
                out.write(0);
                out.write(0);
            }
        }

        private static List<Integer> quantizeColors(Set<Integer> uniqueColors, int maxColors) {
            // High-quality median-cut quantization
            List<Integer> initial = new ArrayList<>(uniqueColors);
            List<List<Integer>> boxes = new ArrayList<>();
            boxes.add(initial);

            while (boxes.size() < maxColors) {
                int splitIndex = -1;
                int maxRange = -1;

                for (int i = 0; i < boxes.size(); i++) {
                    List<Integer> box = boxes.get(i);
                    if (box.size() <= 1) continue;

                    int rMin = 255, rMax = 0;
                    int gMin = 255, gMax = 0;
                    int bMin = 255, bMax = 0;
                    for (int c : box) {
                        int r = (c >> 16) & 0xFF;
                        int g = (c >> 8) & 0xFF;
                        int b = c & 0xFF;
                        if (r < rMin) rMin = r;
                        if (r > rMax) rMax = r;
                        if (g < gMin) gMin = g;
                        if (g > gMax) gMax = g;
                        if (b < bMin) bMin = b;
                        if (b > bMax) bMax = b;
                    }
                    int range = Math.max(rMax - rMin, Math.max(gMax - gMin, bMax - bMin));
                    if (range > maxRange) {
                        maxRange = range;
                        splitIndex = i;
                    }
                }

                if (splitIndex == -1 || maxRange <= 0) break;

                List<Integer> boxToSplit = boxes.remove(splitIndex);
                int rMin = 255, rMax = 0;
                int gMin = 255, gMax = 0;
                int bMin = 255, bMax = 0;
                for (int c : boxToSplit) {
                    int r = (c >> 16) & 0xFF;
                    int g = (c >> 8) & 0xFF;
                    int b = c & 0xFF;
                    if (r < rMin) rMin = r;
                    if (r > rMax) rMax = r;
                    if (g < gMin) gMin = g;
                    if (g > gMax) gMax = g;
                    if (b < bMin) bMin = b;
                    if (b > bMax) bMax = b;
                }
                int rRange = rMax - rMin;
                int gRange = gMax - gMin;
                int bRange = bMax - bMin;

                Comparator<Integer> comp;
                if (rRange >= gRange && rRange >= bRange) {
                    comp = Comparator.comparingInt(c -> (c >> 16) & 0xFF);
                } else if (gRange >= rRange && gRange >= bRange) {
                    comp = Comparator.comparingInt(c -> (c >> 8) & 0xFF);
                } else {
                    comp = Comparator.comparingInt(c -> c & 0xFF);
                }
                boxToSplit.sort(comp);

                int mid = boxToSplit.size() / 2;
                boxes.add(new ArrayList<>(boxToSplit.subList(0, mid)));
                boxes.add(new ArrayList<>(boxToSplit.subList(mid, boxToSplit.size())));
            }

            List<Integer> result = new ArrayList<>();
            for (List<Integer> box : boxes) {
                long rSum = 0, gSum = 0, bSum = 0;
                for (int c : box) {
                    rSum += (c >> 16) & 0xFF;
                    gSum += (c >> 8) & 0xFF;
                    bSum += c & 0xFF;
                }
                int r = (int) (rSum / box.size());
                int g = (int) (gSum / box.size());
                int b = (int) (bSum / box.size());
                result.add((r << 16) | (g << 8) | b);
            }
            return result;
        }
    }
}
