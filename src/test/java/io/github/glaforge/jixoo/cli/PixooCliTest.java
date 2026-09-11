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
package io.github.glaforge.jixoo.cli;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;

class PixooCliTest {

    static {
        System.setProperty("picocli.ansi", "false");
    }

    @Test
    @DisplayName("Root CLI should print help output when run without arguments or with --help")
    void testRootHelp() {
        PixooCli cli = new PixooCli();
        CommandLine cmd = new CommandLine(cli);

        StringWriter out = new StringWriter();
        cmd.setOut(new PrintWriter(out));

        int exitCode = cmd.execute("--help");
        assertEquals(0, exitCode);
        String output = out.toString();
        assertTrue(output.contains("Usage: pixoo64"));
        assertTrue(output.contains("discover"));
        assertTrue(output.contains("channel"));
        assertTrue(output.contains("brightness"));
        assertTrue(output.contains("text"));
        assertTrue(output.contains("image"));
        assertTrue(output.contains("gif"));
    }

    @Test
    @DisplayName("Discover subcommand help should output usage information")
    void testDiscoverHelp() {
        PixooCli cli = new PixooCli();
        CommandLine cmd = new CommandLine(cli);

        StringWriter out = new StringWriter();
        cmd.setOut(new PrintWriter(out));

        int exitCode = cmd.execute("discover", "--help");
        assertEquals(0, exitCode);
        String output = out.toString();
        assertTrue(output.contains("Discover Pixoo 64 devices"));
        assertTrue(output.contains("--timeout"));
    }

    @Test
    @DisplayName("Channel subcommand help should output available channels")
    void testChannelHelp() {
        PixooCli cli = new PixooCli();
        CommandLine cmd = new CommandLine(cli);

        StringWriter out = new StringWriter();
        cmd.setOut(new PrintWriter(out));

        int exitCode = cmd.execute("channel", "--help");
        assertEquals(0, exitCode);
        String output = out.toString();
        assertTrue(output.contains("clock, cloud, visualizer, custom, black-screen"));
    }

    @Test
    @DisplayName("Text subcommand help should list send and clear subcommands")
    void testTextHelp() {
        PixooCli cli = new PixooCli();
        CommandLine cmd = new CommandLine(cli);

        StringWriter out = new StringWriter();
        cmd.setOut(new PrintWriter(out));

        int exitCode = cmd.execute("text", "--help");
        assertEquals(0, exitCode);
        String output = out.toString();
        assertTrue(output.contains("send"));
        assertTrue(output.contains("clear"));
    }

    @Test
    @DisplayName("Text send subcommand help should show text options")
    void testTextSendHelp() {
        PixooCli cli = new PixooCli();
        CommandLine cmd = new CommandLine(cli);

        StringWriter out = new StringWriter();
        cmd.setOut(new PrintWriter(out));

        int exitCode = cmd.execute("text", "send", "--help");
        assertEquals(0, exitCode);
        String output = out.toString();
        assertTrue(output.contains("--text"));
        assertTrue(output.contains("--id"));
        assertTrue(output.contains("--color"));
        assertTrue(output.contains("--font"));
        assertTrue(output.contains("--dir"));
        assertTrue(output.contains("--align"));
    }

    @Test
    @DisplayName("Subcommand execution without host option should fail with clear error message")
    void testMissingHostFailure() {
        PixooCli cli = new PixooCli();
        CommandLine cmd = new CommandLine(cli);

        StringWriter err = new StringWriter();
        cmd.setErr(new PrintWriter(err));

        int exitCode = cmd.execute("brightness", "50");
        assertNotEquals(0, exitCode);
    }

    @Test
    @DisplayName("Image subcommand should fail if target file does not exist")
    void testImageNonExistentFile() {
        PixooCli cli = new PixooCli();
        CommandLine cmd = new CommandLine(cli);

        StringWriter err = new StringWriter();
        cmd.setErr(new PrintWriter(err));

        int exitCode = cmd.execute("--host", "192.168.1.100", "image", "/non/existent/path/photo.png");
        assertEquals(1, exitCode);
        assertTrue(err.toString().contains("does not exist"));
    }

    @Test
    @DisplayName("Gif subcommand should fail if neither --file nor --url is provided")
    void testGifMissingParams() {
        PixooCli cli = new PixooCli();
        CommandLine cmd = new CommandLine(cli);

        StringWriter err = new StringWriter();
        cmd.setErr(new PrintWriter(err));

        int exitCode = cmd.execute("--host", "192.168.1.100", "gif");
        assertEquals(1, exitCode);
        assertTrue(err.toString().contains("Specify either --file"));
    }

    @Test
    @DisplayName("Gif subcommand should fail if both --file and --url are provided")
    void testGifConflictingParams() {
        PixooCli cli = new PixooCli();
        CommandLine cmd = new CommandLine(cli);

        StringWriter err = new StringWriter();
        cmd.setErr(new PrintWriter(err));

        int exitCode = cmd.execute("--host", "192.168.1.100", "gif", "--file", "anim.gif", "--url", "http://example.com/anim.gif");
        assertEquals(1, exitCode);
        assertTrue(err.toString().contains("Cannot specify both"));
    }

    @Test
    @DisplayName("Color subcommand help should output usage information")
    void testColorHelp() {
        PixooCli cli = new PixooCli();
        CommandLine cmd = new CommandLine(cli);

        StringWriter out = new StringWriter();
        cmd.setOut(new PrintWriter(out));

        int exitCode = cmd.execute("color", "--help");
        assertEquals(0, exitCode);
        String output = out.toString();
        assertTrue(output.contains("Fill the display screen with a solid hexadecimal CSS color"));
    }

    @Test
    @DisplayName("Color subcommand should validate hex color format")
    void testColorInvalidFormat() {
        PixooCli cli = new PixooCli();
        CommandLine cmd = new CommandLine(cli);

        StringWriter err = new StringWriter();
        cmd.setErr(new PrintWriter(err));

        int exitCode = cmd.execute("--host", "192.168.1.100", "color", "INVALID_COLOR");
        assertEquals(1, exitCode);
        assertTrue(err.toString().contains("Invalid color format"));
    }

    @Test
    @DisplayName("Cloud subcommand help should list cloud subcommands")
    void testCloudHelp() {
        PixooCli cli = new PixooCli();
        CommandLine cmd = new CommandLine(cli);

        StringWriter out = new StringWriter();
        cmd.setOut(new PrintWriter(out));

        int exitCode = cmd.execute("cloud", "--help");
        assertEquals(0, exitCode);
        String output = out.toString();
        assertTrue(output.contains("login"));
        assertTrue(output.contains("logout"));
        assertTrue(output.contains("devices"));
        assertTrue(output.contains("channel"));
        assertTrue(output.contains("gallery"));
        assertTrue(output.contains("browse"));
        assertTrue(output.contains("search"));
        assertTrue(output.contains("artist"));
        assertTrue(output.contains("uploads"));
        assertTrue(output.contains("likes"));
        assertTrue(output.contains("download"));
        assertTrue(output.contains("play"));
    }


    @Test
    @DisplayName("Cloud logout subcommand help should show options")
    void testCloudLogoutHelp() {
        PixooCli cli = new PixooCli();
        CommandLine cmd = new CommandLine(cli);

        StringWriter out = new StringWriter();
        cmd.setOut(new PrintWriter(out));

        int exitCode = cmd.execute("cloud", "logout", "--help");
        assertEquals(0, exitCode);
        String output = out.toString();
        assertTrue(output.contains("Log out from Divoom Cloud"));
    }

    @Test
    @DisplayName("Cloud channel subcommand help should show options")
    void testCloudChannelHelp() {
        PixooCli cli = new PixooCli();
        CommandLine cmd = new CommandLine(cli);

        StringWriter out = new StringWriter();
        cmd.setOut(new PrintWriter(out));

        int exitCode = cmd.execute("cloud", "channel", "--help");
        assertEquals(0, exitCode);
        String output = out.toString();
        assertTrue(output.contains("--file"));
        assertTrue(output.contains("--slot"));
        assertTrue(output.contains("--logout"));
    }

    @Test
    @DisplayName("Cloud download subcommand help should show options including --raw")
    void testCloudDownloadHelp() {
        PixooCli cli = new PixooCli();
        CommandLine cmd = new CommandLine(cli);

        StringWriter out = new StringWriter();
        cmd.setOut(new PrintWriter(out));

        int exitCode = cmd.execute("cloud", "download", "--help");
        assertEquals(0, exitCode);
        String output = out.toString();
        assertTrue(output.contains("--output"));
        assertTrue(output.contains("--raw"));
    }


    @Test
    @DisplayName("Tool subcommand help should list all hardware tools")
    void testToolHelp() {
        PixooCli cli = new PixooCli();
        CommandLine cmd = new CommandLine(cli);

        StringWriter out = new StringWriter();
        cmd.setOut(new PrintWriter(out));

        int exitCode = cmd.execute("tool", "--help");
        assertEquals(0, exitCode);
        String output = out.toString();
        assertTrue(output.contains("stopwatch"));
        assertTrue(output.contains("timer"));
        assertTrue(output.contains("scoreboard"));
        assertTrue(output.contains("noise"));
    }

    @Test
    @DisplayName("Tool subcommands individual help")
    void testToolSubcommandsHelp() {
        PixooCli cli = new PixooCli();
        CommandLine cmd = new CommandLine(cli);

        StringWriter out = new StringWriter();
        cmd.setOut(new PrintWriter(out));

        assertEquals(0, cmd.execute("tool", "stopwatch", "--help"));
        assertTrue(out.toString().contains("stopwatch"));

        out.getBuffer().setLength(0);
        assertEquals(0, cmd.execute("tool", "timer", "--help"));
        assertTrue(out.toString().contains("--min"));
        assertTrue(out.toString().contains("--sec"));

        out.getBuffer().setLength(0);
        assertEquals(0, cmd.execute("tool", "scoreboard", "--help"));
        assertTrue(out.toString().contains("--blue"));
        assertTrue(out.toString().contains("--red"));

        out.getBuffer().setLength(0);
        assertEquals(0, cmd.execute("tool", "noise", "--help"));
        assertTrue(out.toString().contains("noise"));
    }

    @Test
    @DisplayName("Time subcommand help should show sync")
    void testTimeHelp() {
        PixooCli cli = new PixooCli();
        CommandLine cmd = new CommandLine(cli);

        StringWriter out = new StringWriter();
        cmd.setOut(new PrintWriter(out));

        int exitCode = cmd.execute("time", "--help");
        assertEquals(0, exitCode);
        String output = out.toString();
        assertTrue(output.contains("sync"));
    }

    @Test
    @DisplayName("Config subcommand help should show get and set")
    void testConfigHelp() {
        PixooCli cli = new PixooCli();
        CommandLine cmd = new CommandLine(cli);

        StringWriter out = new StringWriter();
        cmd.setOut(new PrintWriter(out));

        int exitCode = cmd.execute("config", "--help");
        assertEquals(0, exitCode);
        String output = out.toString();
        assertTrue(output.contains("get"));
        assertTrue(output.contains("set"));

        out.getBuffer().setLength(0);
        assertEquals(0, cmd.execute("config", "set", "--help"));
        String setOutput = out.toString();
        assertTrue(setOutput.contains("--time-format"));
        assertTrue(setOutput.contains("--temp-unit"));
        assertTrue(setOutput.contains("--date-format"));
        assertTrue(setOutput.contains("--mirror"));
        assertTrue(setOutput.contains("--auto-off"));
    }

    @Test
    @DisplayName("Channel subcommands help for startup, clock-face, and page")
    void testChannelSubcommandsHelp() {
        PixooCli cli = new PixooCli();
        CommandLine cmd = new CommandLine(cli);

        StringWriter out = new StringWriter();
        cmd.setOut(new PrintWriter(out));

        assertEquals(0, cmd.execute("channel", "startup", "--help"));
        assertTrue(out.toString().contains("startup"));

        out.getBuffer().setLength(0);
        assertEquals(0, cmd.execute("channel", "clock-face", "--help"));
        assertTrue(out.toString().contains("clock"));

        out.getBuffer().setLength(0);
        assertEquals(0, cmd.execute("channel", "page", "--help"));
        assertTrue(out.toString().contains("page"));
    }

    @Test
    @DisplayName("Screen sleep subcommand help should show sleep duration options")
    void testScreenSleepHelp() {
        PixooCli cli = new PixooCli();
        CommandLine cmd = new CommandLine(cli);

        StringWriter out = new StringWriter();
        cmd.setOut(new PrintWriter(out));

        assertEquals(0, cmd.execute("screen", "sleep", "--help"));
        String output = out.toString();
        assertTrue(output.contains("sleep"));
        assertTrue(output.contains("status"));
        assertTrue(output.contains("cancel"));
    }

    @Test
    @DisplayName("Tool pomodoro, alarm, and countdown subcommands help")
    void testToolNewSubcommandsHelp() {
        PixooCli cli = new PixooCli();
        CommandLine cmd = new CommandLine(cli);

        StringWriter out = new StringWriter();
        cmd.setOut(new PrintWriter(out));

        assertEquals(0, cmd.execute("tool", "pomodoro", "--help"));
        assertTrue(out.toString().contains("start"));
        assertTrue(out.toString().contains("set"));

        out.getBuffer().setLength(0);
        assertEquals(0, cmd.execute("tool", "alarm", "--help"));
        assertTrue(out.toString().contains("list"));
        assertTrue(out.toString().contains("set"));
        assertTrue(out.toString().contains("delete"));

        out.getBuffer().setLength(0);
        assertEquals(0, cmd.execute("tool", "countdown", "--help"));
        assertTrue(out.toString().contains("set"));
        assertTrue(out.toString().contains("delete"));
    }

    @Test
    @DisplayName("Weather command help should show current and forecast")
    void testWeatherHelp() {
        PixooCli cli = new PixooCli();
        CommandLine cmd = new CommandLine(cli);

        StringWriter out = new StringWriter();
        cmd.setOut(new PrintWriter(out));

        assertEquals(0, cmd.execute("weather", "--help"));
        String output = out.toString();
        assertTrue(output.contains("current"));
        assertTrue(output.contains("forecast"));
    }
}


