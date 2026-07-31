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
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

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

    @picocli.CommandLine.Spec
    private picocli.CommandLine.Model.CommandSpec spec;

    @ParentCommand
    private PixooCli parent;

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

        PixooClient client = parent.createClient();
        spec.commandLine().getOut().printf("Processing and sending image: %s...%n", path.getFileName());
        PixooResponse response = client.sendImage(path);
        if (response.isSuccess()) {
            spec.commandLine().getOut().printf("Successfully displayed image: %s.%n", path.getFileName());
            return 0;
        } else {
            spec.commandLine().getErr().printf("Failed to send image (Error code: %d).%n", response.errorCode());
            return 1;
        }
    }
}
