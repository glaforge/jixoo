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

import io.github.glaforge.jixoo.api.PixooChannel;
import io.github.glaforge.jixoo.api.PixooClient;
import io.github.glaforge.jixoo.api.PixooResponse;
import io.github.glaforge.jixoo.cli.PixooCli;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

import java.util.concurrent.Callable;

/**
 * CLI Subcommand to switch Pixoo 64 channel.
 */
@Command(
        name = "channel",
        description = "Switch screen channel (clock, cloud, visualizer, custom, black-screen)."
)
public class ChannelCommand implements Callable<Integer> {

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.")
    private boolean helpRequested;

    @ParentCommand
    private PixooCli parent;

    @Parameters(
            index = "0",
            description = "Target channel: clock, cloud, visualizer, custom, black-screen (or index 0-4)"
    )
    private String channelInput;

    @Override
    public Integer call() {
        PixooChannel channel;
        try {
            channel = parseChannel(channelInput);
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }

        PixooClient client = parent.createClient();
        PixooResponse response = client.selectChannel(channel);
        if (response.isSuccess()) {
            System.out.printf("Successfully switched channel to %s.%n", channel);
            return 0;
        } else {
            System.err.printf("Failed to switch channel (Error code: %d).%n", response.errorCode());
            return 1;
        }
    }

    private PixooChannel parseChannel(String input) {
        String normalized = input.trim().toUpperCase().replace("-", "_");
        return switch (normalized) {
            case "0", "CLOCK" -> PixooChannel.CLOCK;
            case "1", "CLOUD" -> PixooChannel.CLOUD;
            case "2", "VISUALIZER", "EQUALIZER" -> PixooChannel.VISUALIZER;
            case "3", "CUSTOM" -> PixooChannel.CUSTOM;
            case "4", "BLACK", "BLACK_SCREEN", "OFF" -> PixooChannel.BLACK_SCREEN;
            default -> throw new IllegalArgumentException("Invalid channel: '" + input +
                    "'. Valid options: clock (0), cloud (1), visualizer (2), custom (3), black-screen (4).");
        };
    }
}
