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
import io.github.glaforge.jixoo.model.sys.SysConfig;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

import java.util.concurrent.Callable;

/**
 * CLI Command group for managing Pixoo device system configuration.
 */
@Command(
        name = "config",
        description = "Manage device system configuration settings (12/24h, temp unit, date format, mirror, auto-off).",
        subcommands = {
                ConfigCommand.GetSubcommand.class,
                ConfigCommand.SetSubcommand.class
        }
)
public class ConfigCommand implements Callable<Integer> {

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.")
    private boolean helpRequested;

    @ParentCommand
    PixooCli parent;

    @Override
    public Integer call() {
        return new GetSubcommand(this).call();
    }

    @Command(name = "get", description = "Query device system configuration.")
    public static class GetSubcommand implements Callable<Integer> {
        @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.")
        private boolean helpRequested;

        @ParentCommand
        private ConfigCommand parent;

        public GetSubcommand() {}

        public GetSubcommand(ConfigCommand parent) {
            this.parent = parent;
        }

        @Override
        public Integer call() {
            PixooClient client = parent.parent.createClient();
            SysConfig config = client.getSystemConfig();
            System.out.println("Pixoo-64 System Configuration:");
            if (config.time24Flag() != null) {
                System.out.printf(" - Time Format: %s (%d)%n", config.time24Flag() == 1 ? "24-Hour" : "12-Hour", config.time24Flag());
            }
            if (config.temperatureMode() != null) {
                System.out.printf(" - Temperature Unit: %s (%d)%n", config.temperatureMode() == 1 ? "Fahrenheit (°F)" : "Celsius (°C)", config.temperatureMode());
            }
            if (config.dateFormat() != null) {
                System.out.printf(" - Date Format: %d%n", config.dateFormat());
            }
            if (config.mirrorFlag() != null) {
                System.out.printf(" - Mirror Mode: %s (%d)%n", config.mirrorFlag() == 1 ? "Enabled" : "Disabled", config.mirrorFlag());
            }
            if (config.autoPowerOff() != null) {
                System.out.printf(" - Auto Power Off: %s%n", config.autoPowerOff() == 0 ? "Disabled" : config.autoPowerOff() + " minutes");
            }
            if (config.gyrateAngle() != null) {
                System.out.printf(" - Rotation Angle: %d°%n", config.gyrateAngle());
            }
            if (config.highLight() != null) {
                System.out.printf(" - Highlight Mode: %s%n", config.highLight() == 1 ? "Enabled" : "Disabled");
            }
            if (config.whiteBalanceR() != null && config.whiteBalanceG() != null && config.whiteBalanceB() != null) {
                System.out.printf(" - White Balance RGB: (%d, %d, %d)%n", config.whiteBalanceR(), config.whiteBalanceG(), config.whiteBalanceB());
            }
            if (config.bluetoothAutoConnect() != null) {
                System.out.printf(" - Bluetooth Auto-Connect: %s%n", config.bluetoothAutoConnect() == 1 ? "Enabled" : "Disabled");
            }
            return 0;
        }
    }

    @Command(name = "set", description = "Update device system configuration.")
    public static class SetSubcommand implements Callable<Integer> {
        @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.")
        private boolean helpRequested;

        @ParentCommand
        private ConfigCommand parent;

        @Option(names = {"--time-format"}, description = "Time format: 12 or 24")
        private Integer timeFormat;

        @Option(names = {"--temp-unit"}, description = "Temperature unit: 'c' (Celsius) or 'f' (Fahrenheit)")
        private String tempUnit;

        @Option(names = {"--date-format"}, description = "Date format index: 0..8")
        private Integer dateFormat;

        @Option(names = {"--mirror"}, description = "Mirror display: 'on' or 'off'")
        private String mirror;

        @Option(names = {"--auto-off"}, description = "Auto power off timer in minutes (0 to disable)")
        private Integer autoPowerOff;

        @Override
        public Integer call() {
            Integer time24 = null;
            if (timeFormat != null) {
                if (timeFormat == 24) time24 = 1;
                else if (timeFormat == 12) time24 = 0;
                else {
                    System.err.println("Invalid time format: " + timeFormat + ". Use 12 or 24.");
                    return 1;
                }
            }

            Integer tempMode = null;
            if (tempUnit != null) {
                String t = tempUnit.trim().toLowerCase();
                if (t.equals("c") || t.equals("celsius") || t.equals("0")) tempMode = 0;
                else if (t.equals("f") || t.equals("fahrenheit") || t.equals("1")) tempMode = 1;
                else {
                    System.err.println("Invalid temperature unit: '" + tempUnit + "'. Use 'c' or 'f'.");
                    return 1;
                }
            }

            Integer mirrorFlag = null;
            if (mirror != null) {
                String m = mirror.trim().toLowerCase();
                if (m.equals("on") || m.equals("true") || m.equals("1")) mirrorFlag = 1;
                else if (m.equals("off") || m.equals("false") || m.equals("0")) mirrorFlag = 0;
                else {
                    System.err.println("Invalid mirror option: '" + mirror + "'. Use 'on' or 'off'.");
                    return 1;
                }
            }

            if (time24 == null && tempMode == null && dateFormat == null && mirrorFlag == null && autoPowerOff == null) {
                System.err.println("Specify at least one setting to update. Run 'pixoo-cli config set --help' for options.");
                return 1;
            }

            PixooClient client = parent.parent.createClient();
            SysConfig newConfig = new SysConfig(
                    time24,
                    tempMode,
                    dateFormat,
                    mirrorFlag,
                    autoPowerOff,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            PixooResponse resp = client.setSystemConfig(newConfig);
            if (resp.isSuccess()) {
                System.out.println("Successfully updated device system configuration.");
                return 0;
            } else {
                System.err.printf("Failed to update system configuration (Error code: %d)%n", resp.errorCode());
                return 1;
            }
        }
    }
}
