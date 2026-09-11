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

import java.util.concurrent.Callable;

/**
 * CLI Subcommand to turn the screen display on or off.
 */
@Command(
        name = "screen",
        description = "Turn screen display on or off, or manage auto sleep timer.",
        subcommands = {
                ScreenCommand.SleepSubcommand.class
        }
)
public class ScreenCommand implements Callable<Integer> {

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.")
    private boolean helpRequested;

    @ParentCommand
    private PixooCli parent;

    @Parameters(
            index = "0",
            arity = "0..1",
            description = "Action: 'on' (or 'true', '1'), 'off' (or 'false', '0'), or 'status'"
    )
    private String stateInput;

    @Override
    public Integer call() {
        if (stateInput == null || stateInput.isBlank()) {
            picocli.CommandLine.usage(this, System.out);
            return 0;
        }

        PixooClient client = parent.createClient();
        String normalized = stateInput.trim().toLowerCase();
        if (normalized.equals("status") || normalized.equals("get") || normalized.equals("query")) {
            boolean on = client.isScreenOn();
            System.out.printf("Screen is currently %s.%n", on ? "ON" : "OFF");
            return 0;
        }

        boolean state = parseState(stateInput);
        PixooResponse response = client.setScreenState(state);
        if (response.isSuccess()) {
            System.out.printf("Successfully turned screen %s.%n", state ? "ON" : "OFF");
            return 0;
        } else {
            System.err.printf("Failed to set screen state (Error code: %d).%n", response.errorCode());
            return 1;
        }
    }

    private boolean parseState(String input) {
        String normalized = input.trim().toLowerCase();
        return switch (normalized) {
            case "on", "true", "1", "enable" -> true;
            case "off", "false", "0", "disable" -> false;
            default -> throw new IllegalArgumentException("Invalid screen state: '" + input + "'. Use 'on' or 'off'.");
        };
    }

    @Command(name = "sleep", description = "Get, set, or cancel the auto sleep timer (turn off screen after N minutes).")
    public static class SleepSubcommand implements Callable<Integer> {
        @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.")
        private boolean helpRequested;

        @ParentCommand
        private ScreenCommand parent;

        @Parameters(
                index = "0",
                arity = "0..1",
                description = "Sleep timer in minutes (0 or 'cancel' to disable). If omitted or 'status', queries current timer."
        )
        private String minutesInput;

        @Override
        public Integer call() {
            PixooClient client = parent.parent.createClient();
            if (minutesInput == null || minutesInput.isBlank() || minutesInput.equalsIgnoreCase("status") || minutesInput.equalsIgnoreCase("get")) {
                int minutes = client.getSleepTimer();
                if (minutes > 0) {
                    System.out.printf("Auto sleep timer active: screen will turn off in %d minute(s).%n", minutes);
                } else {
                    System.out.println("Auto sleep timer is disabled.");
                }
                return 0;
            }

            int minutes;
            if (minutesInput.equalsIgnoreCase("cancel") || minutesInput.equalsIgnoreCase("off") || minutesInput.equalsIgnoreCase("disable")) {
                minutes = 0;
            } else {
                try {
                    minutes = Integer.parseInt(minutesInput);
                } catch (NumberFormatException e) {
                    System.err.printf("Invalid minutes value: '%s'. Enter a number of minutes or 'cancel'.%n", minutesInput);
                    return 1;
                }
            }

            PixooResponse resp = client.setSleepTimer(minutes);
            if (resp.isSuccess()) {
                if (minutes > 0) {
                    System.out.printf("Successfully set auto sleep timer to %d minute(s).%n", minutes);
                } else {
                    System.out.println("Successfully cancelled auto sleep timer.");
                }
                return 0;
            } else {
                System.err.printf("Failed to set sleep timer (Error code: %d)%n", resp.errorCode());
                return 1;
            }
        }
    }
}
