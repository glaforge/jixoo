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
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

/**
 * CLI Subcommand to send an animated GIF from a local file or remote HTTP URL.
 */
@Command(
        name = "gif",
        description = "Display an animated GIF from a local file path or remote HTTP URL."
)
public class GifCommand implements Callable<Integer> {

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.")
    private boolean helpRequested;

    @Spec
    private CommandSpec spec;

    @ParentCommand
    private PixooCli parent;

    @Option(
            names = {"-f", "--file"},
            description = "Path to local GIF file"
    )
    private String gifFilePath;

    @Option(
            names = {"-u", "--url"},
            description = "Remote HTTP/HTTPS URL of a GIF file"
    )
    private String gifUrl;

    @Option(
            names = {"-c", "--crop"},
            description = "Crop rectangular GIFs to fill the 64x64 screen without letterbox black bars"
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

    @Option(
            names = {"--direct"},
            description = "Direct device firmware to fetch URL directly via Device/PlayTFGif (WARNING: can crash device on high-res GIFs or complex HTTPS)"
    )
    private boolean direct;

    @Override
    public Integer call() {
        if ((gifFilePath == null || gifFilePath.isBlank()) && (gifUrl == null || gifUrl.isBlank())) {
            spec.commandLine().getErr().println("Error: Specify either --file <path> or --url <http_url>.");
            return 1;
        }

        if (gifFilePath != null && !gifFilePath.isBlank() && gifUrl != null && !gifUrl.isBlank()) {
            spec.commandLine().getErr().println("Error: Cannot specify both --file and --url at the same time.");
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
        PixooResponse response;

        if (gifFilePath != null && !gifFilePath.isBlank()) {
            Path path = Paths.get(gifFilePath);
            if (!Files.exists(path) || !Files.isRegularFile(path)) {
                spec.commandLine().getErr().printf("GIF file does not exist or is not a regular file: %s%n", gifFilePath);
                return 1;
            }
            if (applyLedOptimize) {
                spec.commandLine().getOut().printf("Decoding and uploading GIF with LED optimization: %s (%s, %s, blackThreshold=%d)...%n",
                        path.getFileName(), mode, sampleMode, threshold);
            } else {
                spec.commandLine().getOut().printf("Decoding and uploading GIF file: %s (%s, %s)...%n",
                        path.getFileName(), mode, sampleMode);
            }
            response = client.sendGif(path, mode, sampleMode, postProcessor);
        } else {
            if (direct) {
                spec.commandLine().getOut().printf("Directing Pixoo 64 firmware to download and render remote GIF from URL: %s...%n", gifUrl);
                response = client.sendRemoteGifUrl(gifUrl);
            } else {
                if (applyLedOptimize) {
                    spec.commandLine().getOut().printf("Downloading, scaling to 64x64 (%s, %s, blackThreshold=%d), and streaming remote GIF from URL: %s...%n",
                            mode, sampleMode, threshold, gifUrl);
                } else {
                    spec.commandLine().getOut().printf("Downloading, scaling to 64x64 (%s, %s), and streaming remote GIF from URL: %s...%n",
                            mode, sampleMode, gifUrl);
                }
                try {
                    response = client.sendGifUrl(gifUrl, mode, sampleMode, postProcessor);
                } catch (Exception e) {
                    spec.commandLine().getErr().printf("Failed to download or stream GIF: %s%n", e.getMessage());
                    return 1;
                }
            }
        }

        if (response.isSuccess()) {
            spec.commandLine().getOut().println("Successfully displayed GIF on Pixoo 64.");
            return 0;
        } else {
            spec.commandLine().getErr().printf("Failed to display GIF (Error code: %d).%n", response.errorCode());
            return 1;
        }
    }
}
