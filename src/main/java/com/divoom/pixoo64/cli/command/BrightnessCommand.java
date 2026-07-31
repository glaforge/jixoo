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
 * CLI Subcommand to set LED matrix brightness.
 */
@Command(
        name = "brightness",
        description = "Set LED matrix brightness (0-100)."
)
public class BrightnessCommand implements Callable<Integer> {

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.")
    private boolean helpRequested;

    @ParentCommand
    private PixooCli parent;

    @Parameters(
            index = "0",
            description = "Brightness percentage between 0 (darkest) and 100 (full brightness)"
    )
    private int brightness;

    @Override
    public Integer call() {
        if (brightness < 0 || brightness > 100) {
            throw new IllegalArgumentException("Brightness value must be between 0 and 100 (got " + brightness + ").");
        }
        PixooClient client = parent.createClient();
        PixooResponse response = client.setBrightness(brightness);
        if (response.isSuccess()) {
            System.out.printf("Successfully set brightness to %d%%.%n", brightness);
            return 0;
        } else {
            System.err.printf("Failed to set brightness (Error code: %d).%n", response.errorCode());
            return 1;
        }
    }
}
