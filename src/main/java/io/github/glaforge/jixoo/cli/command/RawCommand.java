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

import io.github.glaforge.jixoo.cli.PixooCli;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.Callable;

/**
 * CLI Subcommand to execute arbitrary raw JSON command payloads against the Pixoo 64 device.
 */
@Command(
        name = "raw",
        description = "Execute an arbitrary raw JSON command payload against the Pixoo 64 device."
)
public class RawCommand implements Callable<Integer> {

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.")
    private boolean helpRequested;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @picocli.CommandLine.Spec
    private picocli.CommandLine.Model.CommandSpec spec;

    @ParentCommand
    private PixooCli parent;

    @Option(
            names = {"-j", "--json"},
            description = "Inline JSON payload string (e.g. '{\"Command\": \"Channel/SetIndex\", \"SelectIndex\": 0}')"
    )
    private String jsonString;

    @Option(
            names = {"-f", "--file"},
            description = "Path to JSON payload file"
    )
    private String jsonFilePath;

    @Override
    public Integer call() {
        if ((jsonString == null || jsonString.isBlank()) && (jsonFilePath == null || jsonFilePath.isBlank())) {
            spec.commandLine().getErr().println("Error: Specify either --json <raw_json> or --file <json_file_path>.");
            return 1;
        }

        if (jsonString != null && !jsonString.isBlank() && jsonFilePath != null && !jsonFilePath.isBlank()) {
            spec.commandLine().getErr().println("Error: Cannot specify both --json and --file at the same time.");
            return 1;
        }

        String rawJsonPayload;
        try {
            if (jsonFilePath != null && !jsonFilePath.isBlank()) {
                Path path = Paths.get(jsonFilePath);
                if (!Files.exists(path) || !Files.isRegularFile(path)) {
                    spec.commandLine().getErr().printf("JSON payload file does not exist: %s%n", jsonFilePath);
                    return 1;
                }
                rawJsonPayload = Files.readString(path);
            } else {
                rawJsonPayload = jsonString;
            }

            // Validate JSON syntax
            JsonNode jsonNode = MAPPER.readTree(rawJsonPayload);
            String formattedJson = MAPPER.writeValueAsString(jsonNode);

            String host = parent.getHost();
            if (host == null || host.isBlank()) {
                spec.commandLine().getErr().println("Error: Device IP address must be specified using --host option or PIXOO_HOST / PIXOO_IP environment variable.");
                return 1;
            }

            int port = parent.getPort();
            String endpoint = "http://" + host + ":" + port + "/post";

            spec.commandLine().getOut().printf("Sending raw JSON payload to %s...%n%s%n", endpoint, formattedJson);

            HttpClient httpClient = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(formattedJson))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            spec.commandLine().getOut().printf("HTTP Status: %d%n", response.statusCode());
            spec.commandLine().getOut().printf("Response Body: %s%n", response.body());

            return response.statusCode() == 200 ? 0 : 1;

        } catch (Exception e) {
            spec.commandLine().getErr().printf("Error executing raw JSON command: %s%n", e.getMessage());
            return 1;
        }
    }
}
