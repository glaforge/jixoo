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

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Callable;

/**
 * CLI Command group for device real-time clock synchronization.
 */
@Command(
        name = "time",
        description = "Manage and synchronize device real-time clock (RTC).",
        subcommands = {
                TimeCommand.SyncSubcommand.class
        }
)
public class TimeCommand implements Callable<Integer> {

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.")
    private boolean helpRequested;

    @ParentCommand
    PixooCli parent;

    @Override
    public Integer call() {
        return new SyncSubcommand(this).call();
    }

    @Command(name = "sync", description = "Synchronize device RTC clock to current host local time.")
    public static class SyncSubcommand implements Callable<Integer> {
        @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.")
        private boolean helpRequested;

        @ParentCommand
        private TimeCommand parent;

        public SyncSubcommand() {}

        public SyncSubcommand(TimeCommand parent) {
            this.parent = parent;
        }

        @Override
        public Integer call() {
            PixooClient client = parent.parent.createClient();
            Instant now = Instant.now();
            String formatted = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault())
                    .format(now);
            PixooResponse resp = client.syncTime(now);
            if (resp.isSuccess()) {
                System.out.printf("Successfully synchronized device RTC to %s.%n", formatted);
                return 0;
            } else {
                System.err.printf("Failed to sync device time (Error code: %d)%n", resp.errorCode());
                return 1;
            }
        }
    }
}
