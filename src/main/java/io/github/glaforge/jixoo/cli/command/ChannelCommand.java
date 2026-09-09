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
import io.github.glaforge.jixoo.model.sys.ChannelConfig;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

import java.util.concurrent.Callable;

/**
 * CLI Subcommand to switch Pixoo 64 channel and manage channel settings.
 */
@Command(
        name = "channel",
        description = "Switch screen channel (clock, cloud, visualizer, custom, black-screen) or configure channels.",
        subcommands = {
                ChannelCommand.StartupSubcommand.class,
                ChannelCommand.ClockFaceSubcommand.class,
                ChannelCommand.PageSubcommand.class,
                ChannelCommand.ConfigSubcommand.class
        }
)
public class ChannelCommand implements Callable<Integer> {

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.")
    private boolean helpRequested;

    @ParentCommand
    private PixooCli parent;

    @Parameters(
            index = "0",
            arity = "0..1",
            description = "Target channel: clock, cloud, visualizer, custom, black-screen (or index 0-4)"
    )
    private String channelInput;

    @Override
    public Integer call() {
        if (channelInput == null || channelInput.isBlank()) {
            CommandLine.usage(this, System.out);
            return 0;
        }

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

    public static PixooChannel parseChannel(String input) {
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

    @Command(name = "startup", description = "Get or set the default startup channel upon device power-on.")
    public static class StartupSubcommand implements Callable<Integer> {
        @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.")
        private boolean helpRequested;

        @ParentCommand
        private ChannelCommand parent;

        @Parameters(
                index = "0",
                arity = "0..1",
                description = "Optional startup channel to set (clock, cloud, visualizer, custom). If omitted, queries current startup channel."
        )
        private String targetChannel;

        @Override
        public Integer call() {
            PixooClient client = parent.parent.createClient();
            if (targetChannel == null || targetChannel.isBlank()) {
                PixooChannel current = client.getStartupChannel();
                System.out.printf("Device startup channel is currently: %s (index %d)%n", current, current.index());
                return 0;
            }

            PixooChannel ch = parseChannel(targetChannel);
            PixooResponse resp = client.setStartupChannel(ch);
            if (resp.isSuccess()) {
                System.out.printf("Successfully set startup channel to: %s%n", ch);
                return 0;
            } else {
                System.err.printf("Failed to set startup channel (Error code: %d)%n", resp.errorCode());
                return 1;
            }
        }
    }

    @Command(name = "clock-face", aliases = {"dial"}, description = "Select active clock face by ClockId or query current ClockId.")
    public static class ClockFaceSubcommand implements Callable<Integer> {
        @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.")
        private boolean helpRequested;

        @ParentCommand
        private ChannelCommand parent;

        @Parameters(
                index = "0",
                arity = "0..1",
                description = "Clock face ID to display. If omitted, queries active clock face ID."
        )
        private Integer clockId;

        @Override
        public Integer call() {
            PixooClient client = parent.parent.createClient();
            if (clockId == null) {
                int currentId = client.getClockId();
                System.out.printf("Active clock face ID: %d%n", currentId);
                return 0;
            }

            PixooResponse resp = client.setClockId(clockId);
            if (resp.isSuccess()) {
                System.out.printf("Successfully selected clock face ID: %d%n", clockId);
                return 0;
            } else {
                System.err.printf("Failed to select clock face ID (Error code: %d)%n", resp.errorCode());
                return 1;
            }
        }
    }

    @Command(name = "page", aliases = {"custom-page"}, description = "Switch custom gallery page index (0, 1, or 2) or query current page.")
    public static class PageSubcommand implements Callable<Integer> {
        @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.")
        private boolean helpRequested;

        @ParentCommand
        private ChannelCommand parent;

        @Parameters(
                index = "0",
                arity = "0..1",
                description = "Custom page index (0..2). If omitted, queries active page index."
        )
        private Integer pageIndex;

        @Override
        public Integer call() {
            PixooClient client = parent.parent.createClient();
            if (pageIndex == null) {
                int current = client.getCustomPageIndex();
                System.out.printf("Current custom page index: %d%n", current);
                return 0;
            }

            if (pageIndex < 0 || pageIndex > 2) {
                System.err.println("Page index must be 0, 1, or 2.");
                return 1;
            }

            PixooResponse resp = client.setCustomPageIndex(pageIndex);
            if (resp.isSuccess()) {
                System.out.printf("Successfully switched to custom page %d%n", pageIndex);
                return 0;
            } else {
                System.err.printf("Failed to switch custom page (Error code: %d)%n", resp.errorCode());
                return 1;
            }
        }
    }

    @Command(name = "config", description = "Query channel slideshow intervals and auto-rotation configuration.")
    public static class ConfigSubcommand implements Callable<Integer> {
        @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.")
        private boolean helpRequested;

        @ParentCommand
        private ChannelCommand parent;

        @Override
        public Integer call() {
            PixooClient client = parent.parent.createClient();
            ChannelConfig config = client.getChannelConfig();
            System.out.println("Pixoo-64 Channel Configuration:");
            if (config.channelIndex() != null) System.out.printf(" - Active Channel Index: %d%n", config.channelIndex());
            if (config.rotationFlag() != null) System.out.printf(" - Auto Rotation Flag: %s%n", config.rotationFlag() == 1 ? "Enabled" : "Disabled");
            if (config.clockTime() != null) System.out.printf(" - Clock Display Duration: %d seconds%n", config.clockTime());
            if (config.galleryTime() != null) System.out.printf(" - Gallery Cycle Time: %d seconds%n", config.galleryTime());
            if (config.singleGalleryTime() != null) System.out.printf(" - Single Gallery Time: %d seconds%n", config.singleGalleryTime());
            if (config.startUpClockId() != null) System.out.printf(" - Startup Clock ID: %d%n", config.startUpClockId());
            return 0;
        }
    }
}
