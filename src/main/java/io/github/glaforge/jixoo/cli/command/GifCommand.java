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
import picocli.CommandLine.ParentCommand;

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

    @picocli.CommandLine.Spec
    private picocli.CommandLine.Model.CommandSpec spec;

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

        PixooClient client = parent.createClient();
        PixooResponse response;

        if (gifFilePath != null && !gifFilePath.isBlank()) {
            Path path = Paths.get(gifFilePath);
            if (!Files.exists(path) || !Files.isRegularFile(path)) {
                spec.commandLine().getErr().printf("GIF file does not exist or is not a regular file: %s%n", gifFilePath);
                return 1;
            }
            spec.commandLine().getOut().printf("Decoding and uploading GIF file: %s...%n", path.getFileName());
            response = client.sendGif(path);
        } else {
            spec.commandLine().getOut().printf("Directing Pixoo 64 to download and render remote GIF from URL: %s...%n", gifUrl);
            response = client.sendRemoteGifUrl(gifUrl);
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
