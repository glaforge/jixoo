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
package io.github.glaforge.jixoo.cli.command;

import io.github.glaforge.jixoo.api.PixooClient;
import io.github.glaforge.jixoo.api.PixooResponse;
import io.github.glaforge.jixoo.cli.PixooCli;
import io.github.glaforge.jixoo.image.ImageProcessor.SamplingMode;
import io.github.glaforge.jixoo.image.ImageProcessor.ScaleMode;
import io.github.glaforge.jixoo.image.LedPostProcessor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

/**
 * CLI Subcommand to load, process, and send a static image file to display.
 */
@Command(
        name = "image",
        description = "Process and send a static image file (PNG, JPG, BMP, etc.) to the display."
)
public class ImageCommand implements Callable<Integer> {

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.")
    private boolean helpRequested;

    @Spec
    private CommandSpec spec;

    @ParentCommand
    private PixooCli parent;

    @Option(
            names = {"-c", "--crop"},
            description = "Crop rectangular images to fill the 64x64 screen without letterbox black bars"
    )
    private boolean crop;

    @Option(
            names = {"-s", "--scale-mode"},
            description = "Scale mode for fitting onto 64x64 canvas: FIT_CENTER, FILL_CROP, STRETCH"
    )
    private ScaleMode scaleMode;

    @Option(
            names = {"-n", "--nearest"},
            description = "Use nearest-neighbor sampling instead of bilinear interpolation (ideal for pixel art)"
    )
    private boolean nearest;

    @Option(
            names = {"-m", "--sampling-mode"},
            description = "Sampling mode for scaling: BILINEAR (default), NEAREST_NEIGHBOR"
    )
    private SamplingMode samplingMode;

    @Option(
            names = {"-o", "--led-optimize"},
            description = "Apply physical LED matrix optimization (true-black clamping and glare reduction)"
    )
    private boolean ledOptimize;

    @Option(
            names = {"--black-threshold"},
            description = "Threshold (0-255) below which dark pixels are clamped to true #000000 (default: 15)"
    )
    private Integer blackThreshold;

    @Parameters(
            index = "0",
            description = "Path to the image file (e.g., image.png, photo.jpg)"
    )
    private String imageFilePath;

    @Override
    public Integer call() {
        Path path = Paths.get(imageFilePath);
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            spec.commandLine().getErr().printf("Image file does not exist or is not a regular file: %s%n", imageFilePath);
            return 1;
        }

        ScaleMode mode = scaleMode != null
                ? scaleMode
                : (crop ? ScaleMode.FILL_CROP : ScaleMode.FIT_CENTER);

        SamplingMode sampleMode = samplingMode != null
                ? samplingMode
                : (nearest ? SamplingMode.NEAREST_NEIGHBOR : SamplingMode.BILINEAR);

        boolean applyLedOptimize = ledOptimize || blackThreshold != null;
        int threshold = blackThreshold != null ? blackThreshold : LedPostProcessor.DEFAULT_BLACK_THRESHOLD;
        LedPostProcessor postProcessor = applyLedOptimize
                ? LedPostProcessor.builder().blackThreshold(threshold).build()
                : null;

        PixooClient client = parent.createClient();
        if (applyLedOptimize) {
            spec.commandLine().getOut().printf("Processing and sending image with LED optimization: %s (%s, %s, blackThreshold=%d)...%n",
                    path.getFileName(), mode, sampleMode, threshold);
        } else {
            spec.commandLine().getOut().printf("Processing and sending image: %s (%s, %s)...%n",
                    path.getFileName(), mode, sampleMode);
        }
        PixooResponse response = client.sendImage(path, mode, sampleMode, postProcessor);
        if (response.isSuccess()) {
            spec.commandLine().getOut().printf("Successfully displayed image: %s.%n", path.getFileName());
            return 0;
        } else {
            spec.commandLine().getErr().printf("Failed to send image (Error code: %d).%n", response.errorCode());
            return 1;
        }
    }
}
