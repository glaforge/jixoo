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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Pure-Java animated GIF decoder extracting frames and metadata delay times with zero AWT/native runtime dependencies.
 */
public final class GifDecoder {

    private GifDecoder() {}

    /**
     * Decodes a GIF file or stream into a {@link PixooAnimation} with exact per-frame delays.
     *
     * @param inputStream the stream containing the GIF data
     * @return a PixooAnimation containing the GIF frames
     * @throws PixooException if decoding fails
     */
    public static PixooAnimation decode(InputStream inputStream) {
        try {
            byte[] bytes = inputStream.readAllBytes();
            return decode(bytes);
        } catch (PixooException e) {
            throw e;
        } catch (Exception e) {
            throw new PixooException("Failed to decode GIF image", e);
        }
    }

    /**
     * Decodes a GIF file from the specified path into a {@link PixooAnimation}.
     *
     * @param path the path to the GIF file
     * @return a PixooAnimation containing the GIF frames
     * @throws PixooException if reading or decoding fails
     */
    public static PixooAnimation decode(Path path) {
        try (InputStream is = Files.newInputStream(path)) {
            return decode(is);
        } catch (PixooException e) {
            throw e;
        } catch (Exception e) {
            throw new PixooException("Failed to read GIF file: " + path, e);
        }
    }

    /**
     * Decodes GIF bytes directly into a PixooAnimation.
     */
    public static PixooAnimation decode(byte[] data) {
        if (data == null || data.length < 13) {
            throw new PixooException("Invalid GIF: Data stream too short");
        }

        // 1. Header: 6 bytes ("GIF87a" or "GIF89a")
        String header = new String(data, 0, 6);
        if (!header.startsWith("GIF")) {
            throw new PixooException("Invalid GIF: Header missing 'GIF' signature");
        }

        int pos = 6;
        int logicalWidth = readInt16LE(data, pos);
        int logicalHeight = readInt16LE(data, pos + 2);
        int packed = data[pos + 4] & 0xFF;
        int bgColorIndex = data[pos + 5] & 0xFF;
        pos += 7;

        boolean hasGlobalColorTable = (packed & 0x80) != 0;
        int globalColorTableSize = 2 << (packed & 0x07);
        int[] globalColorTable = null;

        if (hasGlobalColorTable) {
            globalColorTable = readColorTable(data, pos, globalColorTableSize);
            pos += globalColorTableSize * 3;
        }

        int canvasWidth = logicalWidth;
        int canvasHeight = logicalHeight;
        if (canvasWidth <= 0 || canvasHeight <= 0) {
            throw new PixooException("Invalid GIF logical dimensions: " + canvasWidth + "x" + canvasHeight);
        }

        int[] masterCanvas = new int[canvasWidth * canvasHeight];
        int[] previousCanvas = new int[canvasWidth * canvasHeight];

        // Fill master canvas with background color if defined
        int defaultBg = 0xFF000000;
        if (hasGlobalColorTable && bgColorIndex < globalColorTable.length) {
            defaultBg = globalColorTable[bgColorIndex];
        }
        Arrays.fill(masterCanvas, defaultBg);
        System.arraycopy(masterCanvas, 0, previousCanvas, 0, masterCanvas.length);

        List<PixooFrame> frames = new ArrayList<>();

        // Graphics Control Extension state
        int delayMs = 100;
        int disposalMethod = 0; // 0=unspecified, 1=doNotDispose, 2=restoreToBackground, 3=restoreToPrevious
        boolean hasTransparency = false;
        int transparentColorIndex = -1;

        while (pos < data.length) {
            int blockType = data[pos++] & 0xFF;
            if (blockType == 0x3B) {
                // Trailer - End of GIF
                break;
            }

            if (blockType == 0x21) {
                // Extension block
                if (pos >= data.length) break;
                int extType = data[pos++] & 0xFF;
                if (extType == 0xF9) {
                    // Graphic Control Extension
                    int blockSize = data[pos++] & 0xFF;
                    if (blockSize >= 4 && pos + blockSize <= data.length) {
                        int gcePacked = data[pos] & 0xFF;
                        disposalMethod = (gcePacked >> 2) & 0x07;
                        hasTransparency = (gcePacked & 0x01) != 0;
                        int rawDelay = readInt16LE(data, pos + 1);
                        delayMs = (rawDelay > 0) ? rawDelay * 10 : 100;
                        transparentColorIndex = data[pos + 3] & 0xFF;
                    }
                    pos += blockSize;
                    // Skip block terminator
                    while (pos < data.length && (data[pos] & 0xFF) != 0) {
                        int skip = data[pos] & 0xFF;
                        pos += 1 + skip;
                    }
                    if (pos < data.length) pos++; // Skip trailing 0x00
                } else {
                    // Other extension (comment, app extension, etc.) - skip all sub-blocks
                    while (pos < data.length && (data[pos] & 0xFF) != 0) {
                        int skip = data[pos] & 0xFF;
                        pos += 1 + skip;
                    }
                    if (pos < data.length) pos++;
                }
            } else if (blockType == 0x2C) {
                // Image Descriptor
                if (pos + 9 > data.length) break;
                int imageLeft = readInt16LE(data, pos);
                int imageTop = readInt16LE(data, pos + 2);
                int imageWidth = readInt16LE(data, pos + 4);
                int imageHeight = readInt16LE(data, pos + 6);
                int imgPacked = data[pos + 8] & 0xFF;
                pos += 9;

                boolean hasLocalColorTable = (imgPacked & 0x80) != 0;
                boolean interlace = (imgPacked & 0x40) != 0;
                int[] activeColorTable = globalColorTable;

                if (hasLocalColorTable) {
                    int localColorTableSize = 2 << (imgPacked & 0x07);
                    activeColorTable = readColorTable(data, pos, localColorTableSize);
                    pos += localColorTableSize * 3;
                }

                if (activeColorTable == null) {
                    activeColorTable = new int[256];
                }

                if (pos >= data.length) break;
                int lzwMinCodeSize = data[pos++] & 0xFF;

                // Read all data sub-blocks
                byte[] lzwData = readSubBlocks(data, pos);
                pos += lzwData.length + countSubBlockOverhead(data, pos);

                // Decompress LZW pixels
                byte[] pixelIndices = lzwDecompress(lzwData, lzwMinCodeSize, imageWidth * imageHeight);

                // Create a working canvas initialized with the master canvas
                int[] currentCanvas = Arrays.copyOf(masterCanvas, masterCanvas.length);

                // Render current frame pixels onto currentCanvas
                renderFramePixels(
                        pixelIndices, imageWidth, imageHeight,
                        imageLeft, imageTop, canvasWidth, canvasHeight,
                        interlace, activeColorTable, hasTransparency, transparentColorIndex,
                        currentCanvas
                );

                // Resize current canvas to 64x64 PixooImage
                PixooImage rawImg = new PixooImage(canvasWidth, canvasHeight, currentCanvas);
                PixooImage fitted = rawImg.resizeAndFit(64, 64, ImageProcessor.ScaleMode.FIT_CENTER);
                frames.add(new PixooFrame(fitted.toRawRgb(), delayMs));

                // Update master canvas for next frame according to disposal method
                if (disposalMethod == 3) {
                    // Restore to previous
                    masterCanvas = Arrays.copyOf(previousCanvas, previousCanvas.length);
                } else if (disposalMethod == 2) {
                    // Restore to background color in the frame's bounding box
                    previousCanvas = Arrays.copyOf(masterCanvas, masterCanvas.length);
                    masterCanvas = Arrays.copyOf(currentCanvas, currentCanvas.length);
                    clearBoundingBox(masterCanvas, canvasWidth, canvasHeight, imageLeft, imageTop, imageWidth, imageHeight, defaultBg);
                } else {
                    // 0 (unspecified) or 1 (do not dispose)
                    previousCanvas = Arrays.copyOf(masterCanvas, masterCanvas.length);
                    masterCanvas = Arrays.copyOf(currentCanvas, currentCanvas.length);
                }

                // Reset GCE state
                delayMs = 100;
                disposalMethod = 0;
                hasTransparency = false;
                transparentColorIndex = -1;
            }
        }

        if (frames.isEmpty()) {
            throw new PixooException("No valid image frames found in GIF data");
        }

        return new PixooAnimation(frames);
    }

    private static void renderFramePixels(
            byte[] pixelIndices, int imgW, int imgH,
            int left, int top, int canvasW, int canvasH,
            boolean interlace, int[] colorTable,
            boolean hasTransparency, int transparentIdx,
            int[] canvas
    ) {
        int[] passStart = {0, 4, 2, 1};
        int[] passStep = {8, 8, 4, 2};

        int srcIdx = 0;
        int totalPixels = imgW * imgH;

        if (!interlace) {
            for (int y = 0; y < imgH && srcIdx < totalPixels; y++) {
                int destY = top + y;
                for (int x = 0; x < imgW && srcIdx < totalPixels; x++) {
                    int destX = left + x;
                    int colorIdx = pixelIndices[srcIdx++] & 0xFF;
                    if (hasTransparency && colorIdx == transparentIdx) {
                        continue;
                    }
                    if (destX >= 0 && destX < canvasW && destY >= 0 && destY < canvasH) {
                        int color = (colorIdx < colorTable.length) ? colorTable[colorIdx] : 0xFF000000;
                        canvas[destY * canvasW + destX] = color;
                    }
                }
            }
        } else {
            // 4 interlaced passes
            for (int pass = 0; pass < 4; pass++) {
                int start = passStart[pass];
                int step = passStep[pass];
                for (int y = start; y < imgH && srcIdx < totalPixels; y += step) {
                    int destY = top + y;
                    for (int x = 0; x < imgW && srcIdx < totalPixels; x++) {
                        int destX = left + x;
                        int colorIdx = pixelIndices[srcIdx++] & 0xFF;
                        if (hasTransparency && colorIdx == transparentIdx) {
                            continue;
                        }
                        if (destX >= 0 && destX < canvasW && destY >= 0 && destY < canvasH) {
                            int color = (colorIdx < colorTable.length) ? colorTable[colorIdx] : 0xFF000000;
                            canvas[destY * canvasW + destX] = color;
                        }
                    }
                }
            }
        }
    }

    private static void clearBoundingBox(
            int[] canvas, int canvasW, int canvasH,
            int left, int top, int width, int height,
            int clearColor
    ) {
        int endX = Math.min(canvasW, left + width);
        int endY = Math.min(canvasH, top + height);
        for (int y = Math.max(0, top); y < endY; y++) {
            for (int x = Math.max(0, left); x < endX; x++) {
                canvas[y * canvasW + x] = clearColor;
            }
        }
    }

    private static int[] readColorTable(byte[] data, int offset, int count) {
        int[] table = new int[count];
        for (int i = 0; i < count; i++) {
            int p = offset + i * 3;
            if (p + 2 >= data.length) break;
            int r = data[p] & 0xFF;
            int g = data[p + 1] & 0xFF;
            int b = data[p + 2] & 0xFF;
            table[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
        }
        return table;
    }

    private static byte[] readSubBlocks(byte[] data, int offset) {
        int pos = offset;
        int totalLen = 0;
        while (pos < data.length && (data[pos] & 0xFF) != 0) {
            int len = data[pos] & 0xFF;
            totalLen += len;
            pos += 1 + len;
        }

        byte[] result = new byte[totalLen];
        pos = offset;
        int destPos = 0;
        while (pos < data.length && (data[pos] & 0xFF) != 0) {
            int len = data[pos] & 0xFF;
            pos++;
            int copyLen = Math.min(len, data.length - pos);
            System.arraycopy(data, pos, result, destPos, copyLen);
            destPos += copyLen;
            pos += len;
        }
        return result;
    }

    private static int countSubBlockOverhead(byte[] data, int offset) {
        int pos = offset;
        while (pos < data.length && (data[pos] & 0xFF) != 0) {
            int len = data[pos] & 0xFF;
            pos += 1 + len;
        }
        if (pos < data.length) pos++; // account for trailing 0x00
        return pos - offset - (offset < data.length ? 0 : 0);
    }

    /**
     * Decompresses LZW-compressed GIF image stream into pixel color indices.
     */
    private static byte[] lzwDecompress(byte[] lzwData, int minCodeSize, int expectedLength) {
        byte[] output = new byte[expectedLength];
        int clearCode = 1 << minCodeSize;
        int endCode = clearCode + 1;

        int codeSize = minCodeSize + 1;
        int maxCode = 1 << codeSize;
        int available = clearCode + 2;

        int[] prefix = new int[4096];
        byte[] suffix = new byte[4096];
        byte[] pixelStack = new byte[4097];

        for (int i = 0; i < clearCode; i++) {
            prefix[i] = -1;
            suffix[i] = (byte) i;
        }

        int bitBuffer = 0;
        int bitCount = 0;
        int dataPos = 0;
        int outPos = 0;
        int top = 0;
        int oldCode = -1;

        while (outPos < expectedLength) {
            if (top == 0) {
                while (bitCount < codeSize) {
                    if (dataPos >= lzwData.length) break;
                    bitBuffer |= (lzwData[dataPos++] & 0xFF) << bitCount;
                    bitCount += 8;
                }
                if (bitCount < codeSize) break;

                int code = bitBuffer & ((1 << codeSize) - 1);
                bitBuffer >>= codeSize;
                bitCount -= codeSize;

                if (code == clearCode) {
                    codeSize = minCodeSize + 1;
                    maxCode = 1 << codeSize;
                    available = clearCode + 2;
                    oldCode = -1;
                    continue;
                }
                if (code == endCode) {
                    break;
                }

                if (oldCode == -1) {
                    if (code >= available) code = 0;
                    output[outPos++] = suffix[code];
                    oldCode = code;
                    continue;
                }

                int inCode = code;
                if (code >= available) {
                    pixelStack[top++] = (byte) suffix[oldCode];
                    code = oldCode;
                }

                while (code >= clearCode) {
                    pixelStack[top++] = suffix[code];
                    code = prefix[code];
                }
                pixelStack[top++] = suffix[code];

                if (available < 4096) {
                    prefix[available] = oldCode;
                    suffix[available] = suffix[code];
                    available++;
                    if (available >= maxCode && codeSize < 12) {
                        codeSize++;
                        maxCode = 1 << codeSize;
                    }
                }
                oldCode = inCode;
            }

            output[outPos++] = pixelStack[--top];
        }

        return output;
    }

    private static int readInt16LE(byte[] data, int offset) {
        return (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8);
    }
}
