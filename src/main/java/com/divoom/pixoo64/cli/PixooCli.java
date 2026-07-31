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
package com.divoom.pixoo64.cli;

import com.divoom.pixoo64.api.PixooClient;
import com.divoom.pixoo64.cli.command.*;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.time.Duration;
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
                ResetAnimationCommand.class,
                RawCommand.class
        }
)
public class PixooCli implements Callable<Integer> {

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.")
    private boolean helpRequested;

    @Option(names = {"-V", "--version"}, versionHelp = true, description = "Print version information and exit.")
    private boolean versionRequested;

    @Option(
            names = {"-H", "--host", "--ip"},
            description = "IP address of the Pixoo 64 device (or set PIXOO_HOST / PIXOO_IP env var)"
    )
    private String host;

    @Option(
            names = {"-p", "--port"},
            defaultValue = "80",
            description = "Device HTTP port (default: 80)"
    )
    private int port;

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

    public int getPort() {
        return port;
    }

    /**
     * Creates a {@link PixooClient} configured with the specified target host, port, and timeouts.
     *
     * @return a configured PixooClient instance
     * @throws IllegalArgumentException if host is not specified
     */
    public PixooClient createClient() {
        String targetHost = getHost();
        if (targetHost == null || targetHost.isBlank()) {
            throw new IllegalArgumentException("Device IP address must be specified using --host option or PIXOO_HOST / PIXOO_IP environment variable.");
        }
        return PixooClient.builder()
                .ipAddress(targetHost)
                .port(port)
                .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
                .requestTimeout(Duration.ofSeconds(requestTimeoutSeconds))
                .build();
    }

    @Override
    public Integer call() {
        CommandLine.usage(this, System.out);
        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new PixooCli()).execute(args);
        System.exit(exitCode);
    }
}
