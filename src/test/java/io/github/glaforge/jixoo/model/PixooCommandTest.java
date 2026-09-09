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
package io.github.glaforge.jixoo.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.glaforge.jixoo.api.PixooRotation;
import io.github.glaforge.jixoo.model.command.PixooCommand;
import io.github.glaforge.jixoo.model.sys.ChannelConfig;
import io.github.glaforge.jixoo.model.sys.SysConfig;
import io.github.glaforge.jixoo.model.tool.NoiseStatus;
import io.github.glaforge.jixoo.model.tool.ScoreboardStatus;
import io.github.glaforge.jixoo.model.tool.StopwatchStatus;
import io.github.glaforge.jixoo.model.tool.TimerStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PixooCommandTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    @DisplayName("RotationCommand serializes Mode correctly")
    void testRotationCommand() throws Exception {
        PixooCommand.RotationCommand cmd = new PixooCommand.RotationCommand(PixooRotation.ROTATE_90.mode());
        String json = MAPPER.writeValueAsString(cmd);
        assertTrue(json.contains("\"Command\":\"Device/SetScreenRotationAngle\""));
        assertTrue(json.contains("\"Mode\":1"));
    }

    @Test
    @DisplayName("SetUtcCommand serializes Command, Utc, and Time")
    void testSetUtcCommand() throws Exception {
        PixooCommand.SetUtcCommand cmd = new PixooCommand.SetUtcCommand(1700000000L, "2023-11-14 22:13:20");
        String json = MAPPER.writeValueAsString(cmd);
        assertTrue(json.contains("\"Command\":\"Device/SetUTC\""));
        assertTrue(json.contains("\"Utc\":1700000000"));
        assertTrue(json.contains("\"Time\":\"2023-11-14 22:13:20\""));
    }

    @Test
    @DisplayName("GetScreenStateCommand serializes to Channel/GetOnOffScreen")
    void testGetScreenStateCommand() throws Exception {
        PixooCommand.GetScreenStateCommand cmd = new PixooCommand.GetScreenStateCommand();
        String json = MAPPER.writeValueAsString(cmd);
        assertTrue(json.contains("\"Command\":\"Channel/GetOnOffScreen\""));
    }

    @Test
    @DisplayName("Stopwatch commands serialize properly")
    void testStopwatchCommands() throws Exception {
        PixooCommand.SetStopWatchCommand setCmd = new PixooCommand.SetStopWatchCommand(1);
        String setJson = MAPPER.writeValueAsString(setCmd);
        assertTrue(setJson.contains("\"Command\":\"Tools/SetStopWatch\""));
        assertTrue(setJson.contains("\"Status\":1"));

        PixooCommand.GetStopWatchCommand getCmd = new PixooCommand.GetStopWatchCommand();
        String getJson = MAPPER.writeValueAsString(getCmd);
        assertTrue(getJson.contains("\"Command\":\"Tools/GetStopWatch\""));
    }

    @Test
    @DisplayName("Timer commands serialize properly")
    void testTimerCommands() throws Exception {
        PixooCommand.SetTimerCommand setCmd = new PixooCommand.SetTimerCommand(5, 30, 1);
        String setJson = MAPPER.writeValueAsString(setCmd);
        assertTrue(setJson.contains("\"Command\":\"Tools/SetTimer\""));
        assertTrue(setJson.contains("\"Minute\":5"));
        assertTrue(setJson.contains("\"Second\":30"));
        assertTrue(setJson.contains("\"Status\":1"));

        PixooCommand.GetTimerCommand getCmd = new PixooCommand.GetTimerCommand();
        String getJson = MAPPER.writeValueAsString(getCmd);
        assertTrue(getJson.contains("\"Command\":\"Tools/GetTimer\""));
    }

    @Test
    @DisplayName("Scoreboard commands serialize properly")
    void testScoreboardCommands() throws Exception {
        PixooCommand.SetScoreBoardCommand setCmd = new PixooCommand.SetScoreBoardCommand(21, 19);
        String setJson = MAPPER.writeValueAsString(setCmd);
        assertTrue(setJson.contains("\"Command\":\"Tools/SetScoreBoard\""));
        assertTrue(setJson.contains("\"BlueScore\":21"));
        assertTrue(setJson.contains("\"RedScore\":19"));

        PixooCommand.GetScoreBoardCommand getCmd = new PixooCommand.GetScoreBoardCommand();
        String getJson = MAPPER.writeValueAsString(getCmd);
        assertTrue(getJson.contains("\"Command\":\"Tools/GetScoreBoard\""));
    }

    @Test
    @DisplayName("Noise meter commands serialize properly")
    void testNoiseCommands() throws Exception {
        PixooCommand.SetNoiseStatusCommand setCmd = new PixooCommand.SetNoiseStatusCommand(1);
        String setJson = MAPPER.writeValueAsString(setCmd);
        assertTrue(setJson.contains("\"Command\":\"Tools/SetNoiseStatus\""));
        assertTrue(setJson.contains("\"NoiseStatus\":1"));

        PixooCommand.GetNoiseStatusCommand getCmd = new PixooCommand.GetNoiseStatusCommand();
        String getJson = MAPPER.writeValueAsString(getCmd);
        assertTrue(getJson.contains("\"Command\":\"Tools/GetNoiseStatus\""));
    }

    @Test
    @DisplayName("StartupChannel and ClockSelect commands serialize properly")
    void testChannelCommands() throws Exception {
        PixooCommand.SetStartupChannelCommand startupCmd = new PixooCommand.SetStartupChannelCommand(2);
        String startupJson = MAPPER.writeValueAsString(startupCmd);
        assertTrue(startupJson.contains("\"Command\":\"Channel/SetStartupChannel\""));
        assertTrue(startupJson.contains("\"ChannelIndex\":2"));

        PixooCommand.SetClockSelectIdCommand clockCmd = new PixooCommand.SetClockSelectIdCommand(42);
        String clockJson = MAPPER.writeValueAsString(clockCmd);
        assertTrue(clockJson.contains("\"Command\":\"Channel/SetClockSelectId\""));
        assertTrue(clockJson.contains("\"ClockId\":42"));

        PixooCommand.SetCustomPageIndexCommand pageCmd = new PixooCommand.SetCustomPageIndexCommand(1);
        String pageJson = MAPPER.writeValueAsString(pageCmd);
        assertTrue(pageJson.contains("\"Command\":\"Channel/SetCustomPageIndex\""));
        assertTrue(pageJson.contains("\"CustomPageIndex\":1"));
    }

    @Test
    @DisplayName("SysConfig and ChannelConfig deserialization")
    void testSysAndChannelConfigDeserialization() throws Exception {
        String sysJson = """
                {
                    "Time24Flag": 1,
                    "TemperatureMode": 0,
                    "DateFormat": 1,
                    "MirrorFlag": 0,
                    "AutoPowerOff": 30,
                    "GyrateAngle": 90,
                    "HighLight": 1
                }
                """;
        SysConfig sysConfig = MAPPER.readValue(sysJson, SysConfig.class);
        assertTrue(sysConfig.is24HourMode());
        assertFalse(sysConfig.isFahrenheit());
        assertFalse(sysConfig.isMirror());
        assertEquals(30, sysConfig.autoPowerOff());
        assertEquals(90, sysConfig.gyrateAngle());

        String channelJson = """
                {
                    "ChannelIndex": 3,
                    "ClockTime": 15,
                    "GalleryTime": 60,
                    "RotationFlag": 1
                }
                """;
        ChannelConfig channelConfig = MAPPER.readValue(channelJson, ChannelConfig.class);
        assertEquals(3, channelConfig.channelIndex());
        assertEquals(15, channelConfig.clockTime());
        assertEquals(60, channelConfig.galleryTime());
        assertEquals(1, channelConfig.rotationFlag());
    }

    @Test
    @DisplayName("ToolStatus records deserialization")
    void testToolStatusDeserialization() throws Exception {
        StopwatchStatus sw = MAPPER.readValue("{\"Status\": 1}", StopwatchStatus.class);
        assertTrue(sw.isRunning());
        assertFalse(sw.isStopped());

        TimerStatus tm = MAPPER.readValue("{\"Minute\": 2, \"Second\": 45, \"Status\": 1}", TimerStatus.class);
        assertEquals(2, tm.minute());
        assertEquals(45, tm.second());
        assertTrue(tm.isRunning());

        ScoreboardStatus sb = MAPPER.readValue("{\"BlueScore\": 10, \"RedScore\": 7}", ScoreboardStatus.class);
        assertEquals(10, sb.blueScore());
        assertEquals(7, sb.redScore());

        NoiseStatus ns = MAPPER.readValue("{\"NoiseStatus\": 1}", NoiseStatus.class);
        assertTrue(ns.isEnabled());
    }

    @Test
    @DisplayName("PixooRotation mode and angle conversions")
    void testPixooRotationConversions() {
        assertEquals(0, PixooRotation.NORMAL.mode());
        assertEquals(0, PixooRotation.NORMAL.angle());

        assertEquals(1, PixooRotation.ROTATE_90.mode());
        assertEquals(90, PixooRotation.ROTATE_90.angle());

        assertEquals(2, PixooRotation.ROTATE_180.mode());
        assertEquals(180, PixooRotation.ROTATE_180.angle());

        assertEquals(3, PixooRotation.ROTATE_270.mode());
        assertEquals(270, PixooRotation.ROTATE_270.angle());

        assertEquals(PixooRotation.NORMAL, PixooRotation.fromMode(0));
        assertEquals(PixooRotation.ROTATE_90, PixooRotation.fromMode(1));
        assertEquals(PixooRotation.ROTATE_180, PixooRotation.fromMode(2));
        assertEquals(PixooRotation.ROTATE_270, PixooRotation.fromMode(3));

        assertEquals(PixooRotation.NORMAL, PixooRotation.fromAngle(0));
        assertEquals(PixooRotation.ROTATE_90, PixooRotation.fromAngle(90));
        assertEquals(PixooRotation.ROTATE_180, PixooRotation.fromAngle(180));
        assertEquals(PixooRotation.ROTATE_270, PixooRotation.fromAngle(270));

        assertThrows(IllegalArgumentException.class, () -> PixooRotation.fromMode(4));
        assertThrows(IllegalArgumentException.class, () -> PixooRotation.fromAngle(45));
    }
}
