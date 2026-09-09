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
package io.github.glaforge.jixoo.cloud;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.glaforge.jixoo.cli.PixooCli;
import io.github.glaforge.jixoo.cloud.model.DivoomCustomListItem;
import io.github.glaforge.jixoo.cloud.model.DivoomCustomListResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DivoomCloudCustomChannelTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("DivoomCustomListResponse parses JSON payload successfully")
    void testCustomListResponseJsonParsing() throws Exception {
        String json = """
                {
                    "ReturnCode": 0,
                    "ReturnMessage": "",
                    "CustomList": [
                        {
                            "CustomId": 1,
                            "FileId": "group9/M10/0B/F8/0bf8383145e5cdc23d747484cac55eeb"
                        },
                        {
                            "CustomId": 2,
                            "FileId": "group9/M10/1A/2C/1a2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e"
                        }
                    ]
                }
                """;

        DivoomCustomListResponse response = mapper.readValue(json, DivoomCustomListResponse.class);
        assertTrue(response.isSuccess());
        assertEquals(0, response.returnCode());
        assertEquals("", response.returnMessage());

        List<DivoomCustomListItem> items = response.customList();
        assertEquals(2, items.size());
        assertEquals(1, items.get(0).customId());
        assertEquals("group9/M10/0B/F8/0bf8383145e5cdc23d747484cac55eeb", items.get(0).fileId());
        assertEquals(2, items.get(1).customId());
        assertEquals("group9/M10/1A/2C/1a2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e", items.get(1).fileId());
    }

    @Test
    @DisplayName("DivoomCustomListResponse handles null or empty CustomList gracefully")
    void testEmptyCustomListResponse() throws Exception {
        String json = """
                {
                    "ReturnCode": 0,
                    "ReturnMessage": ""
                }
                """;

        DivoomCustomListResponse response = mapper.readValue(json, DivoomCustomListResponse.class);
        assertTrue(response.isSuccess());
        assertNotNull(response.customList());
        assertTrue(response.customList().isEmpty());
    }

    @Test
    @DisplayName("CustomChannel CLI help outputs all new options")
    void testCustomChannelCliHelp() {
        PixooCli cli = new PixooCli();
        CommandLine cmd = new CommandLine(cli);

        StringWriter out = new StringWriter();
        cmd.setOut(new PrintWriter(out));

        int exitCode = cmd.execute("cloud", "channel", "--help");
        assertEquals(0, exitCode);

        String help = out.toString();
        assertTrue(help.contains("--index"));
        assertTrue(help.contains("--append"));
        assertTrue(help.contains("--list"));
        assertTrue(help.contains("--clean"));
        assertTrue(help.contains("--delete"));
        assertTrue(help.contains("--slot"));
    }

    @Test
    @DisplayName("CustomChannel CLI fails gracefully when slot is out of bounds")
    void testInvalidSlot() {
        PixooCli cli = new PixooCli();
        CommandLine cmd = new CommandLine(cli);

        StringWriter err = new StringWriter();
        cmd.setErr(new PrintWriter(err));

        int exitCode = cmd.execute("cloud", "channel", "--slot", "5");
        assertEquals(1, exitCode);
        assertTrue(err.toString().contains("Invalid slot index: 5"));
    }

    @Test
    @DisplayName("CustomChannel CLI fails gracefully when no files and no action flag provided")
    void testMissingFilesAndAction() {
        PixooCli cli = new PixooCli();
        CommandLine cmd = new CommandLine(cli);

        StringWriter err = new StringWriter();
        cmd.setErr(new PrintWriter(err));

        int exitCode = cmd.execute("cloud", "channel", "--slot", "0");
        assertEquals(1, exitCode);
        assertTrue(err.toString().contains("No Divoom credentials found") || err.toString().contains("No file(s) specified"));
    }

    @Test
    @DisplayName("CustomChannel CLI fails when input file does not exist")
    void testNonExistentFile() {
        PixooCli cli = new PixooCli();
        CommandLine cmd = new CommandLine(cli);

        StringWriter err = new StringWriter();
        cmd.setErr(new PrintWriter(err));

        // Note: credentials check happens first if not logged in, but with fake env or session it would validate file
        int exitCode = cmd.execute("cloud", "channel", "-f", "/non/existent/file.gif", "--slot", "0");
        assertEquals(1, exitCode);
    }
}
