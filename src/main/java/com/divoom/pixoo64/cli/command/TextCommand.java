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
package com.divoom.pixoo64.cli.command;

import com.divoom.pixoo64.api.PixooClient;
import com.divoom.pixoo64.api.PixooResponse;
import com.divoom.pixoo64.api.PixooText;
import com.divoom.pixoo64.cli.PixooCli;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

import java.util.concurrent.Callable;

/**
 * CLI Subcommand for managing hardware text rendering on Pixoo 64.
 */
@Command(
        name = "text",
        description = "Manage screen hardware text overlays (send text or clear text layers).",
        subcommands = {
                TextCommand.SendTextCommand.class,
                TextCommand.ClearTextCommand.class
        }
)
public class TextCommand implements Callable<Integer> {

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.")
    private boolean helpRequested;

    @ParentCommand
    private PixooCli parent;

    public PixooCli getParent() {
        return parent;
    }

    @Override
    public Integer call() {
        CommandLine.usage(this, System.out);
        return 0;
    }

    /**
     * Subcommand to render text on screen hardware text layer.
     */
    @Command(
            name = "send",
            description = "Render text on screen layer."
    )
    public static class SendTextCommand implements Callable<Integer> {

        @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.")
        private boolean helpRequested;

        @ParentCommand
        private TextCommand parentCommand;

        @Option(
                names = {"-t", "--text"},
                required = true,
                description = "Text string content to display"
        )
        private String text;

        @Option(
                names = {"--id"},
                defaultValue = "1",
                description = "Text slot index (0-19, default: 1)"
        )
        private int textId;

        @Option(
                names = {"-x"},
                defaultValue = "0",
                description = "X coordinate (0-63, default: 0)"
        )
        private int x;

        @Option(
                names = {"-y"},
                defaultValue = "0",
                description = "Y coordinate (0-63, default: 0)"
        )
        private int y;

        @Option(
                names = {"-c", "--color"},
                defaultValue = "#FFFFFF",
                description = "Hexadecimal color string (e.g. #FF0000, default: #FFFFFF)"
        )
        private String color;

        @Option(
                names = {"-f", "--font"},
                defaultValue = "0",
                description = "ROM font ID (0-7, default: 0)"
        )
        private int font;

        @Option(
                names = {"-s", "--speed"},
                defaultValue = "80",
                description = "Scroll delay in ms per step (default: 80)"
        )
        private int speed;

        @Option(
                names = {"-d", "--dir"},
                defaultValue = "static",
                description = "Scroll direction: static, left, right (default: static)"
        )
        private String direction;

        @Option(
                names = {"-a", "--align"},
                defaultValue = "left",
                description = "Text alignment: left, center, right (default: left)"
        )
        private String alignment;

        @Option(
                names = {"-w", "--width"},
                defaultValue = "64",
                description = "Container text width (default: 64)"
        )
        private int textWidth;

        @Override
        public Integer call() {
            PixooCli cli = parentCommand != null ? parentCommand.getParent() : null;
            if (cli == null) {
                throw new IllegalStateException("CLI parent context not available.");
            }
            PixooClient client = cli.createClient();

            PixooText.Builder builder = PixooText.builder()
                    .textId(textId)
                    .position(x, y)
                    .text(text)
                    .color(color)
                    .font(font)
                    .speed(speed)
                    .textWidth(textWidth);

            switch (direction.toLowerCase().trim()) {
                case "left", "1" -> builder.scrollLeft();
                case "right", "2" -> builder.scrollRight();
                case "static", "0" -> builder.staticText();
                default -> throw new IllegalArgumentException("Invalid scroll direction: '" + direction + "'. Options: static, left, right.");
            }

            switch (alignment.toLowerCase().trim()) {
                case "left", "1" -> builder.alignLeft();
                case "center", "2" -> builder.alignCenter();
                case "right", "3" -> builder.alignRight();
                default -> throw new IllegalArgumentException("Invalid alignment: '" + alignment + "'. Options: left, center, right.");
            }

            PixooResponse response = client.sendText(builder.build());
            if (response.isSuccess()) {
                System.out.printf("Successfully sent text layer (ID: %d) \"%s\".%n", textId, text);
                return 0;
            } else {
                System.err.printf("Failed to send text layer (Error code: %d).%n", response.errorCode());
                return 1;
            }
        }
    }

    /**
     * Subcommand to clear all hardware text layers.
     */
    @Command(
            name = "clear",
            description = "Clear all active hardware text layers from screen."
    )
    public static class ClearTextCommand implements Callable<Integer> {

        @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.")
        private boolean helpRequested;

        @ParentCommand
        private TextCommand parentCommand;

        @Override
        public Integer call() {
            PixooCli cli = parentCommand != null ? parentCommand.getParent() : null;
            if (cli == null) {
                throw new IllegalStateException("CLI parent context not available.");
            }
            PixooClient client = cli.createClient();
            PixooResponse response = client.clearText();
            if (response.isSuccess()) {
                System.out.println("Successfully cleared all text layers.");
                return 0;
            } else {
                System.err.printf("Failed to clear text layers (Error code: %d).%n", response.errorCode());
                return 1;
            }
        }
    }
}
