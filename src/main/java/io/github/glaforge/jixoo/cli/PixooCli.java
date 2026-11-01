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
package io.github.glaforge.jixoo.cli;

import io.github.glaforge.jixoo.api.PixooClient;
import io.github.glaforge.jixoo.api.PixooModel;
import io.github.glaforge.jixoo.api.exception.PixooException;
import io.github.glaforge.jixoo.cli.command.*;
import io.github.glaforge.jixoo.discovery.PixooModelProbe;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.Callable;

/**
 * Main entry point for the Pixoo 64 command-line interface.
 */
@Command(
        name = "pixoo64",
        version = "jixoo64 1.0.0",
        description = "Command-line interface to interact with Divoom Pixoo 64 LED matrix displays.",
        subcommands = {
                DiscoverCommand.class,
                ChannelCommand.class,
                BrightnessCommand.class,
                ScreenCommand.class,
                RotationCommand.class,
                BuzzerCommand.class,
                TextCommand.class,
                ImageCommand.class,
                GifCommand.class,
                ColorCommand.class,
                ResetAnimationCommand.class,
                RawCommand.class,
                CloudCommand.class,
                ToolCommand.class,
                TimeCommand.class,
                ConfigCommand.class,
                WeatherCommand.class
        }
)
public class PixooCli implements Callable<Integer> {

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.")
    private boolean helpRequested;

    @Option(names = {"-V", "--version"}, versionHelp = true, description = "Print version information and exit.")
    private boolean versionRequested;

    @Option(
            names = {"-H", "--host", "--ip"},
            description = "IP address of the Pixoo 64 / Pixoo-64 II device (or set PIXOO_HOST / PIXOO_IP env var)"
    )
    private String host;

    @Option(
            names = {"--model"},
            defaultValue = "${env:PIXOO_MODEL:-auto}",
            description = "Device model: auto, pixoo64 or pixoo64-ii (default: auto-detect, or PIXOO_MODEL env var)"
    )
    private String model;

    @Option(
            names = {"-p", "--port"},
            description = "Device HTTP port (default: 80 for pixoo64, 9000 for pixoo64-ii)"
    )
    private Integer port;

    @Option(
            names = {"--connect-timeout"},
            defaultValue = "5",
            description = "Connection timeout in seconds (default: 5)"
    )
    private int connectTimeoutSeconds;

    @Option(
            names = {"--request-timeout"},
            defaultValue = "5",
            description = "Request timeout in seconds (default: 5)"
    )
    private int requestTimeoutSeconds;

    public String getHost() {
        if (host != null && !host.isBlank()) {
            return host;
        }
        String envHost = System.getenv("PIXOO_HOST");
        if (envHost != null && !envHost.isBlank()) {
            return envHost;
        }
        String envIp = System.getenv("PIXOO_IP");
        if (envIp != null && !envIp.isBlank()) {
            return envIp;
        }
        return null;
    }

    public Integer getPort() {
        return port;
    }

    /**
     * Gets the device model chosen with --model or PIXOO_MODEL.
     *
     * @return the model, or {@code null} to detect it
     */
    public PixooModel getModel() {
        return parseModel(model);
    }

    static PixooModel parseModel(String name) {
        String normalized = name == null ? "auto" : name.trim().toLowerCase(Locale.ROOT).replace("_", "").replace("-", "");
        return switch (normalized) {
            case "", "auto" -> null;
            case "pixoo64" -> PixooModel.PIXOO_64;
            case "pixoo64ii" -> PixooModel.PIXOO_64_II;
            default -> throw new IllegalArgumentException("Unknown device model: " + name + " (expected auto, pixoo64 or pixoo64-ii)");
        };
    }

    /**
     * Resolves the URL of the device's local API, detecting the model if needed.
     *
     * @return the API endpoint URL
     */
    public String apiEndpoint() {
        String targetHost = requireHost();
        PixooModel targetModel = getModel();
        if (targetModel == null) {
            PixooModelProbe.Detection detection = PixooModelProbe.detect(targetHost, port, Duration.ofSeconds(connectTimeoutSeconds))
                    .orElseThrow(() -> new IllegalStateException("No Pixoo device answered at " + targetHost
                            + (port != null ? ":" + port : " (tried ports 80 and 9000)") + "; use --model to set it explicitly."));
            return "http://" + targetHost + ":" + detection.port() + detection.model().apiPath();
        }
        return "http://" + targetHost + ":" + (port != null ? port : targetModel.defaultPort()) + targetModel.apiPath();
    }

    private String requireHost() {
        String targetHost = getHost();
        if (targetHost == null || targetHost.isBlank()) {
            throw new IllegalArgumentException("Device IP address must be specified using --host option or PIXOO_HOST / PIXOO_IP environment variable.");
        }
        return targetHost;
    }

    /**
     * Creates a {@link PixooClient} configured with the specified target host, port, and timeouts.
     *
     * @return a configured PixooClient instance
     * @throws IllegalArgumentException if host is not specified
     */
    public PixooClient createClient() {
        String targetHost = requireHost();
        PixooClient.Builder builder = PixooClient.builder()
                .ipAddress(targetHost)
                .model(getModel());
        if (port != null) {
            builder.port(port);
        }
        return builder
                .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
                .requestTimeout(Duration.ofSeconds(requestTimeoutSeconds))
                .build();
    }

    @Override
    public Integer call() {
        CommandLine.usage(this, System.out);
        return 0;
    }

    /**
     * Creates the command line, reporting device errors (such as a command the device does not
     * support) as a one-line message instead of a stack trace.
     *
     * @return the configured command line
     */
    public static CommandLine commandLine() {
        return new CommandLine(new PixooCli())
                .setExecutionExceptionHandler((ex, commandLine, parseResult) -> {
                    if (ex instanceof PixooException || ex instanceof IllegalArgumentException || ex instanceof IllegalStateException) {
                        commandLine.getErr().println("Error: " + ex.getMessage());
                        return 1;
                    }
                    throw ex;
                });
    }

    public static void main(String[] args) {
        int exitCode = commandLine().execute(args);
        System.exit(exitCode);
    }
}
