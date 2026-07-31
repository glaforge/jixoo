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
    @DisplayName("Raw subcommand should fail if neither --json nor --file is provided")
    void testRawMissingParams() {
        PixooCli cli = new PixooCli();
        CommandLine cmd = new CommandLine(cli);

        StringWriter err = new StringWriter();
        cmd.setErr(new PrintWriter(err));

        int exitCode = cmd.execute("--host", "192.168.1.100", "raw");
        assertEquals(1, exitCode);
        assertTrue(err.toString().contains("Specify either --json"));
    }
}
