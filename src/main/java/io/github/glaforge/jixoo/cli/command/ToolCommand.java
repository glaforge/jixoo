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

import java.util.List;
import java.util.concurrent.Callable;

/**
 * CLI Command group for interacting with Pixoo hardware tools.
 */
@Command(
        name = "tool",
        description = "Control hardware built-in tools (stopwatch, timer, scoreboard, noise meter, pomodoro, alarm, countdown).",
        subcommands = {
                ToolCommand.StopwatchSubcommand.class,
                ToolCommand.TimerSubcommand.class,
                ToolCommand.ScoreboardSubcommand.class,
                ToolCommand.NoiseSubcommand.class,
                ToolCommand.PomodoroSubcommand.class,
                ToolCommand.AlarmSubcommand.class,
                ToolCommand.CountdownSubcommand.class
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

    @Command(name = "pomodoro", aliases = {"tomato"}, description = "Configure and start Pomodoro focus timer.")
    public static class PomodoroSubcommand implements Callable<Integer> {
        @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.")
        private boolean helpRequested;

        @ParentCommand
        private ToolCommand parent;

        @Parameters(index = "0", defaultValue = "start", description = "Action: 'start' or 'set'")
        private String action;

        @Option(names = {"--work"}, defaultValue = "25", description = "Work session duration in minutes (default: 25)")
        private int workMinutes;

        @Option(names = {"--short-rest"}, defaultValue = "5", description = "Short rest duration in minutes (default: 5)")
        private int shortRestMinutes;

        @Option(names = {"--long-rest"}, defaultValue = "15", description = "Long rest duration in minutes (default: 15)")
        private int longRestMinutes;

        @Option(names = {"--name"}, defaultValue = "Pomodoro", description = "Pomodoro preset name")
        private String name;

        @Option(names = {"--id"}, defaultValue = "0", description = "Pomodoro preset ID (default: 0)")
        private int id;

        @Override
        public Integer call() {
            PixooClient client = parent.parent.createClient();
            String norm = action.trim().toLowerCase();
            if (norm.equals("set") || norm.equals("config")) {
                PixooResponse resp = client.setPomodoro(id, name, workMinutes, shortRestMinutes, longRestMinutes);
                if (resp.isSuccess()) {
                    System.out.printf("Successfully configured Pomodoro [%d] '%s': %d min work, %d min short rest, %d min long rest.%n",
                            id, name, workMinutes, shortRestMinutes, longRestMinutes);
                    return 0;
                } else {
                    System.err.printf("Failed to configure Pomodoro (Error code: %d)%n", resp.errorCode());
                    return 1;
                }
            } else if (norm.equals("start")) {
                if (workMinutes != 25 || shortRestMinutes != 5 || longRestMinutes != 15) {
                    client.setPomodoro(id, name, workMinutes, shortRestMinutes, longRestMinutes);
                }
                PixooResponse resp = client.startPomodoro(id);
                if (resp.isSuccess()) {
                    System.out.printf("Successfully started Pomodoro focus timer [%d] '%s'!%n", id, name);
                    return 0;
                } else {
                    System.err.printf("Failed to start Pomodoro timer (Error code: %d)%n", resp.errorCode());
                    return 1;
                }
            } else {
                System.err.println("Invalid Pomodoro action: '" + action + "'. Valid: start, set.");
                return 1;
            }
        }
    }

    @Command(name = "alarm", aliases = {"alarms"}, description = "Manage scheduled alarms on the device.")
    public static class AlarmSubcommand implements Callable<Integer> {
        @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.")
        private boolean helpRequested;

        @ParentCommand
        private ToolCommand parent;

        @Parameters(index = "0", defaultValue = "list", description = "Action: 'list', 'set', or 'delete'")
        private String action;

        @Option(names = {"--id"}, defaultValue = "1", description = "Alarm ID (1..N, default: 1)")
        private int id;

        @Option(names = {"--name"}, defaultValue = "Alarm", description = "Alarm label/name")
        private String name;

        @Option(names = {"--time"}, description = "Alarm time in HH:mm format (e.g. 07:30)")
        private String time;

        @Option(names = {"--repeat"}, description = "Repeat days comma-separated (0=Sun, 1=Mon...6=Sat, or 'weekdays', 'daily')")
        private String repeat;

        @Option(names = {"--volume"}, defaultValue = "80", description = "Alarm buzzer volume (0-100, default: 80)")
        private int volume;

        @Option(names = {"--sound"}, defaultValue = "1", description = "Alarm sound tone type (default: 1)")
        private int soundType;

        @Override
        public Integer call() {
            PixooClient client = parent.parent.createClient();
            String norm = action.trim().toLowerCase();
            if (norm.equals("list") || norm.equals("get")) {
                List<PixooAlarm> alarms = client.getAlarms();
                if (alarms.isEmpty()) {
                    System.out.println("No alarms configured on device.");
                    return 0;
                }
                System.out.println("Configured Alarms:");
                System.out.println("ID | Name            | Time  | Enabled | Repeat Days          | Volume");
                System.out.println("---+-----------------+-------+---------+----------------------+-------");
                for (PixooAlarm a : alarms) {
                    long totalSeconds = a.alarmTime();
                    long hours = (totalSeconds / 3600) % 24;
                    long minutes = (totalSeconds / 60) % 60;
                    String timeStr = String.format("%02d:%02d", hours, minutes);
                    String days = formatRepeatDays(a.repeatArray());
                    System.out.printf("%-2d | %-15s | %s | %-7s | %-20s | %3d%%%n",
                            a.alarmId(),
                            a.alarmName() != null ? a.alarmName() : "",
                            timeStr,
                            a.isEnabled() ? "Yes" : "No",
                            days,
                            a.volume());
                }
                return 0;
            } else if (norm.equals("set") || norm.equals("add")) {
                int hour = 8;
                int minute = 0;
                if (time != null && !time.isBlank()) {
                    String[] parts = time.split(":");
                    if (parts.length == 2) {
                        try {
                            hour = Integer.parseInt(parts[0].trim());
                            minute = Integer.parseInt(parts[1].trim());
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid time format: '" + time + "'. Expected HH:mm.");
                            return 1;
                        }
                    } else {
                        System.err.println("Invalid time format: '" + time + "'. Expected HH:mm.");
                        return 1;
                    }
                }
                List<Integer> repeatDays = parseRepeatDays(repeat);
                PixooAlarm alarm = new PixooAlarm(id, name, hour * 3600L + minute * 60L, 1, repeatDays, volume, soundType);
                PixooResponse resp = client.setAlarm(alarm);
                if (resp.isSuccess()) {
                    System.out.printf("Successfully set alarm #%d '%s' for %02d:%02d.%n", id, name, hour, minute);
                    return 0;
                } else {
                    System.err.printf("Failed to set alarm (Error code: %d)%n", resp.errorCode());
                    return 1;
                }
            } else if (norm.equals("del") || norm.equals("delete") || norm.equals("remove")) {
                PixooResponse resp = client.deleteAlarm(id);
                if (resp.isSuccess()) {
                    System.out.printf("Successfully deleted alarm #%d.%n", id);
                    return 0;
                } else {
                    System.err.printf("Failed to delete alarm (Error code: %d)%n", resp.errorCode());
                    return 1;
                }
            } else {
                System.err.println("Invalid alarm action: '" + action + "'. Valid: list, set, delete.");
                return 1;
            }
        }

        private static List<Integer> parseRepeatDays(String str) {
            if (str == null || str.isBlank()) return List.of();
            String norm = str.trim().toLowerCase();
            if (norm.equals("daily") || norm.equals("everyday") || norm.equals("all")) {
                return List.of(0, 1, 2, 3, 4, 5, 6);
            }
            if (norm.equals("weekdays")) {
                return List.of(1, 2, 3, 4, 5);
            }
            if (norm.equals("weekends")) {
                return List.of(0, 6);
            }
            List<Integer> result = new java.util.ArrayList<>();
            for (String part : str.split(",")) {
                try {
                    result.add(Integer.parseInt(part.trim()));
                } catch (NumberFormatException ignored) {}
            }
            return result;
        }

        private static String formatRepeatDays(List<Integer> days) {
            if (days == null || days.isEmpty()) return "Once";
            if (days.size() == 7) return "Daily";
            if (days.size() == 5 && days.containsAll(List.of(1, 2, 3, 4, 5))) return "Weekdays";
            if (days.size() == 2 && days.containsAll(List.of(0, 6))) return "Weekends";
            String[] names = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
            StringBuilder sb = new StringBuilder();
            for (int d : days) {
                if (d >= 0 && d < names.length) {
                    if (!sb.isEmpty()) sb.append(",");
                    sb.append(names[d]);
                }
            }
            return sb.toString();
        }
    }

    @Command(name = "countdown", aliases = {"memorial"}, description = "Configure or delete event countdowns / memorial days.")
    public static class CountdownSubcommand implements Callable<Integer> {
        @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.")
        private boolean helpRequested;

        @ParentCommand
        private ToolCommand parent;

        @Parameters(index = "0", defaultValue = "set", description = "Action: 'set' or 'delete'")
        private String action;

        @Option(names = {"--id"}, defaultValue = "1", description = "Countdown ID (1..N, default: 1)")
        private int id;

        @Option(names = {"--name"}, defaultValue = "Event", description = "Event title / name")
        private String name;

        @Option(names = {"--month"}, description = "Target month (1..12)")
        private Integer month;

        @Option(names = {"--day"}, description = "Target day of month (1..31)")
        private Integer day;

        @Option(names = {"--time"}, defaultValue = "00:00", description = "Target time HH:mm (default: 00:00)")
        private String time;

        @Override
        public Integer call() {
            PixooClient client = parent.parent.createClient();
            String norm = action.trim().toLowerCase();
            if (norm.equals("set") || norm.equals("add")) {
                if (month == null || day == null) {
                    System.err.println("Error: --month and --day are required when configuring a countdown.");
                    return 1;
                }
                int hour = 0;
                int min = 0;
                if (time != null && !time.isBlank()) {
                    String[] parts = time.split(":");
                    if (parts.length == 2) {
                        try {
                            hour = Integer.parseInt(parts[0].trim());
                            min = Integer.parseInt(parts[1].trim());
                        } catch (NumberFormatException ignored) {}
                    }
                }
                PixooResponse resp = client.setMemorial(id, name, month, day, hour, min);
                if (resp.isSuccess()) {
                    System.out.printf("Successfully configured countdown #%d '%s' for month %d, day %d (%02d:%02d).%n",
                            id, name, month, day, hour, min);
                    return 0;
                } else {
                    System.err.printf("Failed to set countdown (Error code: %d)%n", resp.errorCode());
                    return 1;
                }
            } else if (norm.equals("del") || norm.equals("delete") || norm.equals("remove")) {
                PixooResponse resp = client.deleteMemorial(id);
                if (resp.isSuccess()) {
                    System.out.printf("Successfully deleted countdown #%d.%n", id);
                    return 0;
                } else {
                    System.err.printf("Failed to delete countdown (Error code: %d)%n", resp.errorCode());
                    return 1;
                }
            } else {
                System.err.println("Invalid countdown action: '" + action + "'. Valid: set, delete.");
                return 1;
            }
        }
    }
}
