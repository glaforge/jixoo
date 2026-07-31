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
import io.github.glaforge.jixoo.api.PixooRotation;
import io.github.glaforge.jixoo.cli.PixooCli;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

import java.util.concurrent.Callable;

/**
 * CLI Subcommand to set physical screen rotation angle.
 */
@Command(
        name = "rotation",
        description = "Set physical screen rotation angle (0, 90, 180, 270 degrees)."
)
public class RotationCommand implements Callable<Integer> {

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.")
    private boolean helpRequested;

    @ParentCommand
    private PixooCli parent;

    @Parameters(
            index = "0",
            description = "Rotation angle: 0 (normal), 90, 180, 270"
    )
    private String rotationInput;

    @Override
    public Integer call() {
        PixooRotation rotation = parseRotation(rotationInput);
        PixooClient client = parent.createClient();
        PixooResponse response = client.setRotation(rotation);
        if (response.isSuccess()) {
            System.out.printf("Successfully set screen rotation to %d° (%s).%n", rotation.angle(), rotation.name());
            return 0;
        } else {
            System.err.printf("Failed to set screen rotation (Error code: %d).%n", response.errorCode());
            return 1;
        }
    }

    private PixooRotation parseRotation(String input) {
        String normalized = input.trim().toUpperCase().replace("-", "_");
        return switch (normalized) {
            case "0", "NORMAL", "0_DEG" -> PixooRotation.NORMAL;
            case "90", "90_CW", "ROTATE_90" -> PixooRotation.ROTATE_90;
            case "180", "ROTATE_180" -> PixooRotation.ROTATE_180;
            case "270", "270_CW", "ROTATE_270" -> PixooRotation.ROTATE_270;
            default -> throw new IllegalArgumentException("Invalid rotation: '" + input +
                    "'. Valid options: 0 (normal), 90, 180, 270.");
        };
    }
}
