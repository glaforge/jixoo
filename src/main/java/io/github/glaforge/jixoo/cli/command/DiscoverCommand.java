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

import io.github.glaforge.jixoo.discovery.PixooDevice;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * CLI Subcommand to discover Pixoo 64 devices on the local network.
 */
@Command(
        name = "discover",
        description = "Discover Pixoo 64 devices on the local network using UDP broadcast."
)
public class DiscoverCommand implements Callable<Integer> {

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.")
    private boolean helpRequested;

    @picocli.CommandLine.Spec
    private picocli.CommandLine.Model.CommandSpec spec;

    @Option(
            names = {"-t", "--timeout"},
            defaultValue = "3",
            description = "Discovery listener timeout in seconds (default: 3)"
    )
    private int timeoutSeconds;

    @Override
    public Integer call() {
        spec.commandLine().getOut().printf("Searching for Pixoo 64 devices on local network (%d seconds)...%n", timeoutSeconds);
        List<PixooDevice> devices = PixooDevice.discoverDevices(Duration.ofSeconds(timeoutSeconds));

        if (devices.isEmpty()) {
            spec.commandLine().getOut().println("No Pixoo devices discovered. Ensure the device is powered on and connected to the same Wi-Fi subnet.");
            return 0;
        }

        spec.commandLine().getOut().printf("Found %d device(s):%n%n", devices.size());
        spec.commandLine().getOut().printf("%-18s %-20s %-16s %s%n", "IP ADDRESS", "MAC ADDRESS", "NAME", "DEVICE ID");
        spec.commandLine().getOut().println("------------------------------------------------------------------");
        for (PixooDevice device : devices) {
            spec.commandLine().getOut().printf("%-18s %-20s %-16s %d%n",
                    device.ipAddress(),
                    device.macAddress().isEmpty() ? "N/A" : device.macAddress(),
                    device.deviceName(),
                    device.deviceId());
        }
        return 0;
    }
}
