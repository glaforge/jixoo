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
import io.github.glaforge.jixoo.model.tool.*;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

import java.util.concurrent.Callable;

/**
 * CLI Command group for interacting with Pixoo hardware tools.
 */
@Command(
        name = "tool",
        description = "Control hardware built-in tools (stopwatch, timer, scoreboard, noise meter).",
        subcommands = {
                ToolCommand.StopwatchSubcommand.class,
                ToolCommand.TimerSubcommand.class,
                ToolCommand.ScoreboardSubcommand.class,
                ToolCommand.NoiseSubcommand.class
        }
)
public class ToolCommand implements Callable<Integer> {

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.")
    private boolean helpRequested;

    @ParentCommand
    PixooCli parent;

    @Override
    public Integer call() {
        CommandLine.usage(this, System.out);
        return 0;
    }

    @Command(name = "stopwatch", description = "Control on-screen stopwatch (start, stop, reset, status).")
    public static class StopwatchSubcommand implements Callable<Integer> {
        @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.")
        private boolean helpRequested;

        @ParentCommand
        private ToolCommand parent;

        @Parameters(index = "0", defaultValue = "status", description = "Action: start, stop, reset, status")
        private String action;

        @Override
        public Integer call() {
            PixooClient client = parent.parent.createClient();
            String norm = action.trim().toLowerCase();
            return switch (norm) {
                case "start", "run", "resume" -> {
                    PixooResponse resp = client.setStopwatch(StopwatchAction.START);
                    System.out.printf("Stopwatch started: %s%n", resp.isSuccess() ? "OK" : "Failed");
                    yield resp.isSuccess() ? 0 : 1;
                }
                case "stop", "pause" -> {
                    PixooResponse resp = client.setStopwatch(StopwatchAction.STOP);
                    System.out.printf("Stopwatch stopped: %s%n", resp.isSuccess() ? "OK" : "Failed");
                    yield resp.isSuccess() ? 0 : 1;
                }
                case "reset", "clear" -> {
                    PixooResponse resp = client.setStopwatch(StopwatchAction.RESET);
                    System.out.printf("Stopwatch reset: %s%n", resp.isSuccess() ? "OK" : "Failed");
                    yield resp.isSuccess() ? 0 : 1;
                }
                case "status", "get" -> {
                    StopwatchStatus status = client.getStopwatch();
                    String stateDesc = status.isRunning() ? "Running" : (status.isStopped() ? "Stopped" : "Reset");
                    System.out.printf("Stopwatch status: %s (code %d)%n", stateDesc, status.status());
                    yield 0;
                }
                default -> {
                    System.err.println("Invalid stopwatch action: '" + action + "'. Valid: start, stop, reset, status.");
                    yield 1;
                }
            };
        }
    }

    @Command(name = "timer", description = "Control countdown timer (start, stop, status).")
    public static class TimerSubcommand implements Callable<Integer> {
        @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.")
        private boolean helpRequested;

        @ParentCommand
        private ToolCommand parent;

        @Option(names = {"-m", "--min", "--minutes"}, defaultValue = "0", description = "Timer duration minutes")
        private int minutes;

        @Option(names = {"-s", "--sec", "--seconds"}, defaultValue = "0", description = "Timer duration seconds")
        private int seconds;

        @Parameters(index = "0", defaultValue = "status", description = "Action: start, stop, status")
        private String action;

        @Override
        public Integer call() {
            PixooClient client = parent.parent.createClient();
            String norm = action.trim().toLowerCase();
            return switch (norm) {
                case "start", "run" -> {
                    PixooResponse resp = client.setTimer(minutes, seconds, true);
                    System.out.printf("Timer started (%02d:%02d): %s%n", minutes, seconds, resp.isSuccess() ? "OK" : "Failed");
                    yield resp.isSuccess() ? 0 : 1;
                }
                case "stop", "pause" -> {
                    PixooResponse resp = client.setTimer(minutes, seconds, false);
                    System.out.printf("Timer stopped: %s%n", resp.isSuccess() ? "OK" : "Failed");
                    yield resp.isSuccess() ? 0 : 1;
                }
                case "status", "get" -> {
                    TimerStatus status = client.getTimer();
                    System.out.printf("Timer status: %s (%02d:%02d remaining)%n",
                            status.isRunning() ? "Running" : "Stopped", status.minute(), status.second());
                    yield 0;
                }
                default -> {
                    System.err.println("Invalid timer action: '" + action + "'. Valid: start, stop, status.");
                    yield 1;
                }
            };
        }
    }

    @Command(name = "scoreboard", description = "Control dual-team scoreboard scores.")
    public static class ScoreboardSubcommand implements Callable<Integer> {
        @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.")
        private boolean helpRequested;

        @ParentCommand
        private ToolCommand parent;

        @Option(names = {"-b", "--blue"}, description = "Blue team score (0..999)")
        private Integer blueScore;

        @Option(names = {"-r", "--red"}, description = "Red team score (0..999)")
        private Integer redScore;

        @Parameters(index = "0", arity = "0..1", defaultValue = "get", description = "Action: set or get")
        private String action;

        @Override
        public Integer call() {
            PixooClient client = parent.parent.createClient();
            String norm = action.trim().toLowerCase();
            if (norm.equals("set") || blueScore != null || redScore != null) {
                int b = blueScore != null ? blueScore : 0;
                int r = redScore != null ? redScore : 0;
                PixooResponse resp = client.setScoreboard(b, r);
                if (resp.isSuccess()) {
                    System.out.printf("Successfully set scoreboard: Blue: %d | Red: %d%n", b, r);
                    return 0;
                } else {
                    System.err.printf("Failed to set scoreboard (Error code: %d)%n", resp.errorCode());
                    return 1;
                }
            } else {
                ScoreboardStatus status = client.getScoreboard();
                System.out.printf("Scoreboard scores: Blue: %d | Red: %d%n", status.blueScore(), status.redScore());
                return 0;
            }
        }
    }

    @Command(name = "noise", description = "Control ambient noise decibel meter (start, stop, status).")
    public static class NoiseSubcommand implements Callable<Integer> {
        @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.")
        private boolean helpRequested;

        @ParentCommand
        private ToolCommand parent;

        @Parameters(index = "0", defaultValue = "status", description = "Action: start, stop, status")
        private String action;

        @Override
        public Integer call() {
            PixooClient client = parent.parent.createClient();
            String norm = action.trim().toLowerCase();
            return switch (norm) {
                case "start", "on", "enable" -> {
                    PixooResponse resp = client.setNoiseStatus(true);
                    System.out.printf("Noise meter started: %s%n", resp.isSuccess() ? "OK" : "Failed");
                    yield resp.isSuccess() ? 0 : 1;
                }
                case "stop", "off", "disable" -> {
                    PixooResponse resp = client.setNoiseStatus(false);
                    System.out.printf("Noise meter stopped: %s%n", resp.isSuccess() ? "OK" : "Failed");
                    yield resp.isSuccess() ? 0 : 1;
                }
                case "status", "get" -> {
                    NoiseStatus status = client.getNoiseStatus();
                    System.out.printf("Noise meter status: %s (code %d)%n",
                            status.isEnabled() ? "Active" : "Stopped", status.noiseStatus());
                    yield 0;
                }
                default -> {
                    System.err.println("Invalid noise action: '" + action + "'. Valid: start, stop, status.");
                    yield 1;
                }
            };
        }
    }
}
