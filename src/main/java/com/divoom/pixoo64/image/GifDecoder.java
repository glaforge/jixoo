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
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Animated GIF decoder extracting frames and metadata delay times.
 */
public class GifDecoder {

    /**
     * Decodes a GIF file or stream into a PixooAnimation with exact per-frame delays.
     *
     * @param inputStream the stream containing the GIF data
     * @return a PixooAnimation containing the GIF frames
     * @throws PixooException if decoding fails
     */
    public static PixooAnimation decode(InputStream inputStream) {
        try {
            byte[] bytes = inputStream.readAllBytes();
            try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
                if (!readers.hasNext()) {
                    throw new PixooException("No GIF reader available in javax.imageio");
                }
                ImageReader reader = readers.next();
                reader.setInput(iis);

                int numFrames = reader.getNumImages(true);
                List<PixooFrame> frames = new ArrayList<>();

                for (int i = 0; i < numFrames; i++) {
                    BufferedImage rawFrame = reader.read(i);
                    int delayMs = getFrameDelayMs(reader, i);
                    if (delayMs <= 0) {
                        delayMs = 100; // Default to 100ms if not specified or 0
                    }

                    BufferedImage processed = ImageProcessor.resizeAndFit(rawFrame);
                    frames.add(PixooFrame.fromImage(processed, delayMs));
                }

                reader.dispose();
                return new PixooAnimation(frames);
            }
        } catch (PixooException e) {
            throw e;
        } catch (Exception e) {
            throw new PixooException("Failed to decode GIF image", e);
        }
    }

    /**
     * Decodes a GIF file from the specified path into a PixooAnimation.
     *
     * @param path the path to the GIF file
     * @return a PixooAnimation containing the GIF frames
     * @throws PixooException if reading or decoding fails
     */
    public static PixooAnimation decode(Path path) {
        try (InputStream is = Files.newInputStream(path)) {
            return decode(is);
        } catch (Exception e) {
            throw new PixooException("Failed to read GIF file: " + path, e);
        }
    }

    private static int getFrameDelayMs(ImageReader reader, int frameIndex) {
        try {
            IIOMetadata metadata = reader.getImageMetadata(frameIndex);
            String metaFormat = metadata.getNativeMetadataFormatName();
            Node root = metadata.getAsTree(metaFormat);
            NodeList children = root.getChildNodes();

            for (int i = 0; i < children.getLength(); i++) {
                Node node = children.item(i);
                if ("GraphicControlExtension".equalsIgnoreCase(node.getNodeName())) {
                    NamedNodeMap attr = node.getAttributes();
                    Node delayNode = attr.getNamedItem("delayTime");
                    if (delayNode != null) {
                        int delayHundredths = Integer.parseInt(delayNode.getNodeValue());
                        return delayHundredths * 10; // Convert 1/100ths of a second to ms
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return 100;
    }
}
