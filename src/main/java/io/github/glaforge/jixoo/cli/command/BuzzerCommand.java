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
import picocli.CommandLine.ParentCommand;

import java.util.concurrent.Callable;

/**
 * CLI Subcommand to trigger internal buzzer alarm/tone pattern.
 */
@Command(
        name = "buzzer",
        description = "Trigger internal piezoelectric buzzer tone sequence."
)
public class BuzzerCommand implements Callable<Integer> {

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.")
    private boolean helpRequested;

    @ParentCommand
    private PixooCli parent;

    @Option(
            names = {"-a", "--active-ms"},
            defaultValue = "500",
            description = "Active tone duration in milliseconds per cycle (default: 500)"
    )
    private int activeMs;

    @Option(
            names = {"-o", "--off-ms"},
            defaultValue = "500",
            description = "Silence duration in milliseconds per cycle (default: 500)"
    )
    private int offMs;

    @Option(
            names = {"-t", "--total-ms"},
            defaultValue = "3000",
            description = "Total alarm duration in milliseconds (default: 3000)"
    )
    private int totalMs;

    @Override
    public Integer call() {
        PixooClient client = parent.createClient();
        PixooResponse response = client.playBuzzer(activeMs, offMs, totalMs);
        if (response.isSuccess()) {
            System.out.printf("Successfully triggered buzzer tone sequence (active: %dms, off: %dms, total: %dms).%n",
                    activeMs, offMs, totalMs);
            return 0;
        } else {
            System.err.printf("Failed to trigger buzzer (Error code: %d).%n", response.errorCode());
            return 1;
        }
    }
}
