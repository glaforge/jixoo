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
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;

import java.util.concurrent.Callable;

/**
 * CLI Subcommand to fill the screen display with a solid hexadecimal CSS color.
 */
@Command(
        name = "color",
        description = "Fill the display screen with a solid hexadecimal CSS color (e.g. #23ED23 or 23ED23)."
)
public class ColorCommand implements Callable<Integer> {

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.")
    private boolean helpRequested;

    @Spec
    private CommandSpec spec;

    @ParentCommand
    private PixooCli parent;

    @Parameters(
            index = "0",
            description = "Hexadecimal CSS color string (e.g. '#23ED23' or '23ED23')"
    )
    private String hexColor;

    @Override
    public Integer call() {
        try {
            PixooClient client = parent.createClient();
            spec.commandLine().getOut().printf("Setting display screen color to %s...%n", hexColor);
            PixooResponse response = client.sendColor(hexColor);
            if (response.isSuccess()) {
                spec.commandLine().getOut().printf("Successfully updated screen color to %s.%n", hexColor);
                return 0;
            } else {
                spec.commandLine().getErr().printf("Failed to set screen color (Error code: %d).%n", response.errorCode());
                return 1;
            }
        } catch (Exception e) {
            spec.commandLine().getErr().printf("Error setting color: %s%n", e.getMessage());
            return 1;
        }
    }
}
