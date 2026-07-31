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
import com.divoom.pixoo64.cli.PixooCli;
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
        description = "Turn screen display on or off."
)
public class ScreenCommand implements Callable<Integer> {

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.")
    private boolean helpRequested;

    @ParentCommand
    private PixooCli parent;

    @Parameters(
            index = "0",
            description = "State: 'on' (or 'true', '1') / 'off' (or 'false', '0')"
    )
    private String stateInput;

    @Override
    public Integer call() {
        boolean state = parseState(stateInput);
        PixooClient client = parent.createClient();
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
}
