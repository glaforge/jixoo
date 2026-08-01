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
                try {
                    reader.setInput(iis);

                    int numFrames = reader.getNumImages(true);
                    List<PixooFrame> frames = new ArrayList<>();

                    int canvasWidth = 0;
                    int canvasHeight = 0;

                    // Read stream metadata for logical screen dimensions
                    try {
                        IIOMetadata streamMeta = reader.getStreamMetadata();
                        if (streamMeta != null) {
                            Node root = streamMeta.getAsTree(streamMeta.getNativeMetadataFormatName());
                            NodeList children = root.getChildNodes();
                            for (int j = 0; j < children.getLength(); j++) {
                                Node node = children.item(j);
                                if ("LogicalScreenDescriptor".equalsIgnoreCase(node.getNodeName())) {
                                    NamedNodeMap attr = node.getAttributes();
                                    Node wNode = attr.getNamedItem("logicalScreenWidth");
                                    Node hNode = attr.getNamedItem("logicalScreenHeight");
                                    if (wNode != null) canvasWidth = Integer.parseInt(wNode.getNodeValue());
                                    if (hNode != null) canvasHeight = Integer.parseInt(hNode.getNodeValue());
                                }
                            }
                        }
                    } catch (Exception ignored) {
                    }

                    if (canvasWidth <= 0 || canvasHeight <= 0) {
                        BufferedImage firstFrame = reader.read(0);
                        canvasWidth = firstFrame.getWidth();
                        canvasHeight = firstFrame.getHeight();
                    }

                    BufferedImage masterCanvas = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_ARGB);
                    BufferedImage previousCanvas = null;

                    for (int i = 0; i < numFrames; i++) {
                        BufferedImage rawFrame = reader.read(i);
                        int delayMs = getFrameDelayMs(reader, i);
                        if (delayMs <= 0) {
                            delayMs = 100;
                        }

                        FrameMetadata meta = getFrameMetadata(reader, i);

                        // Handle disposal method of previous frame if needed
                        BufferedImage currentCanvas = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_ARGB);
                        java.awt.Graphics2D g = currentCanvas.createGraphics();
                        g.drawImage(masterCanvas, 0, 0, null);

                        // Draw raw frame at its offset position
                        g.drawImage(rawFrame, meta.left, meta.top, null);
                        g.dispose();

                        // Update master canvas for next frame based on disposal method
                        if ("restoreToPrevious".equalsIgnoreCase(meta.disposalMethod) && previousCanvas != null) {
                            masterCanvas = copyImage(previousCanvas);
                        } else if ("restoreToBackgroundColor".equalsIgnoreCase(meta.disposalMethod)) {
                            masterCanvas = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_ARGB);
                        } else {
                            previousCanvas = copyImage(masterCanvas);
                            masterCanvas = copyImage(currentCanvas);
                        }

                        BufferedImage processed = ImageProcessor.resizeAndFit(currentCanvas);
                        frames.add(PixooFrame.fromImage(processed, delayMs));
                    }

                    return new PixooAnimation(frames);
                } finally {
                    reader.dispose();
                }
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

    private static record FrameMetadata(int delayMs, int left, int top, String disposalMethod) {}

    private static FrameMetadata getFrameMetadata(ImageReader reader, int frameIndex) {
        int delayMs = 100;
        int left = 0;
        int top = 0;
        String disposalMethod = "none";

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
                        delayMs = Integer.parseInt(delayNode.getNodeValue()) * 10;
                    }
                    Node disposalNode = attr.getNamedItem("disposalMethod");
                    if (disposalNode != null) {
                        disposalMethod = disposalNode.getNodeValue();
                    }
                } else if ("ImageDescriptor".equalsIgnoreCase(node.getNodeName())) {
                    NamedNodeMap attr = node.getAttributes();
                    Node leftNode = attr.getNamedItem("imageLeftPosition");
                    if (leftNode != null) {
                        left = Integer.parseInt(leftNode.getNodeValue());
                    }
                    Node topNode = attr.getNamedItem("imageTopPosition");
                    if (topNode != null) {
                        top = Integer.parseInt(topNode.getNodeValue());
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return new FrameMetadata(delayMs, left, top, disposalMethod);
    }

    private static int getFrameDelayMs(ImageReader reader, int frameIndex) {
        return getFrameMetadata(reader, frameIndex).delayMs();
    }

    private static BufferedImage copyImage(BufferedImage src) {
        BufferedImage copy = new BufferedImage(src.getWidth(), src.getHeight(), src.getType());
        java.awt.Graphics2D g = copy.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return copy;
    }
}
