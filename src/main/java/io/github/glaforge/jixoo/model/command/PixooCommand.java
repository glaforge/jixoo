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
package io.github.glaforge.jixoo.model.command;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Sealed interface representing all supported JSON control commands sent to Pixoo64.
 */
public sealed interface PixooCommand permits
        PixooCommand.ChannelIndexCommand,
        PixooCommand.ResetGifCommand,
        PixooCommand.SendGifCommand,
        PixooCommand.SendTextCommand,
        PixooCommand.ClearTextCommand,
        PixooCommand.BrightnessCommand,
        PixooCommand.ScreenStateCommand,
        PixooCommand.GetScreenStateCommand,
        PixooCommand.RotationCommand,
        PixooCommand.PlayBuzzerCommand,
        PixooCommand.RemoteGifCommand,
        PixooCommand.SetUtcCommand,
        PixooCommand.GetDeviceConfigCommand,
        PixooCommand.GetSysConfigCommand,
        PixooCommand.SetSysConfigCommand,
        PixooCommand.SetStopWatchCommand,
        PixooCommand.GetStopWatchCommand,
        PixooCommand.SetTimerCommand,
        PixooCommand.GetTimerCommand,
        PixooCommand.SetScoreBoardCommand,
        PixooCommand.GetScoreBoardCommand,
        PixooCommand.SetNoiseStatusCommand,
        PixooCommand.GetNoiseStatusCommand,
        PixooCommand.SetStartupChannelCommand,
        PixooCommand.GetStartupChannelCommand,
        PixooCommand.SetClockSelectIdCommand,
        PixooCommand.GetClockInfoCommand,
        PixooCommand.SetCustomPageIndexCommand,
        PixooCommand.GetCustomPageIndexCommand,
        PixooCommand.GetStorageStatusCommand,
        PixooCommand.SetDelayPowerOffCommand,
        PixooCommand.GetDelayPowerOffCommand,
        PixooCommand.TomatoSetCommand,
        PixooCommand.TomatoStartCommand,
        PixooCommand.AlarmGetCommand,
        PixooCommand.AlarmSetCommand,
        PixooCommand.AlarmDelCommand,
        PixooCommand.MemorialSetCommand,
        PixooCommand.MemorialDelCommand {

    /**
     * Gets the command string identifier used by the Pixoo64 API.
     *
     * @return the command string
     */
    @JsonProperty("Command")
    String command();

    /** Command to set the active display channel. */
    record ChannelIndexCommand(
            @JsonProperty("SelectIndex") int selectIndex
    ) implements PixooCommand {
        @Override
        @JsonProperty("Command")
        public String command() {
            return "Channel/SetIndex";
        }
    }

    /** Command to reset the HTTP GIF buffer on the device. */
    record ResetGifCommand() implements PixooCommand {
        @Override
        @JsonProperty("Command")
        public String command() {
            return "Draw/ResetHttpGifId";
        }

        @JsonProperty("Command")
        public String getCommand() {
            return command();
        }
    }

    /** Command to send a frame of a GIF animation. */
    @JsonPropertyOrder({"Command", "PicNum", "PicWidth", "PicOffset", "PicID", "PicSpeed", "PicData"})
    record SendGifCommand(
            @JsonProperty("PicNum") int picNum,
            @JsonProperty("PicWidth") int picWidth,
            @JsonProperty("PicOffset") int picOffset,
            @JsonProperty("PicID") int picID,
            @JsonProperty("PicSpeed") int picSpeed,
            @JsonProperty("PicData") String picData
    ) implements PixooCommand {
        public SendGifCommand(int picNum, int picOffset, int picID, int picSpeed, String picData) {
            this(picNum, 64, picOffset, picID, picSpeed, picData);
        }

        @Override
        @JsonProperty("Command")
        public String command() {
            return "Draw/SendHttpGif";
        }
    }

    /** Command to send custom text to the display. */
    record SendTextCommand(
            @JsonProperty("TextId") int textId,
            @JsonProperty("x") int x,
            @JsonProperty("y") int y,
            @JsonProperty("dir") int dir,
            @JsonProperty("font") int font,
            @JsonProperty("TextWidth") int textWidth,
            @JsonProperty("speed") int speed,
            @JsonProperty("TextString") String textString,
            @JsonProperty("color") String color,
            @JsonProperty("align") int align
    ) implements PixooCommand {
        @Override
        @JsonProperty("Command")
        public String command() {
            return "Draw/SendHttpText";
        }
    }

    /** Command to clear the hardware text engine. */
    record ClearTextCommand() implements PixooCommand {
        @Override
        @JsonProperty("Command")
        public String command() {
            return "Draw/ClearHttpText";
        }

        @JsonProperty("Command")
        public String getCommand() {
            return command();
        }
    }

    /** Command to set the screen brightness. */
    record BrightnessCommand(
            @JsonProperty("Brightness") int brightness
    ) implements PixooCommand {
        @Override
        @JsonProperty("Command")
        public String command() {
            return "Channel/SetBrightness";
        }
    }

    /** Command to toggle the screen on and off. */
    record ScreenStateCommand(
            @JsonProperty("OnOff") int onOff
    ) implements PixooCommand {
        @Override
        @JsonProperty("Command")
        public String command() {
            return "Channel/OnOffScreen";
        }
    }

    /** Command to query screen power state. */
    record GetScreenStateCommand() implements PixooCommand {
        @Override
        @JsonProperty("Command")
        public String command() {
            return "Channel/GetOnOffScreen";
        }

        @JsonProperty("Command")
        public String getCommand() {
            return command();
        }
    }

    /** Command to set the screen rotation angle mode (0 = 0°, 1 = 90°, 2 = 180°, 3 = 270°). */
    record RotationCommand(
            @JsonProperty("Mode") int mode
    ) implements PixooCommand {
        @Override
        @JsonProperty("Command")
        public String command() {
            return "Device/SetScreenRotationAngle";
        }
    }

    /** Command to play a buzzer pattern. */
    record PlayBuzzerCommand(
            @JsonProperty("ActiveTimeInCycle") int activeTimeInCycle,
            @JsonProperty("OffTimeInCycle") int offTimeInCycle,
            @JsonProperty("PlayTotalTime") int playTotalTime
    ) implements PixooCommand {
        @Override
        @JsonProperty("Command")
        public String command() {
            return "Device/PlayBuzzer";
        }
    }

    /** Command to instruct the device to download and play a remote GIF. */
    record RemoteGifCommand(
            @JsonProperty("FileType") int fileType,
            @JsonProperty("FileName") String fileName
    ) implements PixooCommand {
        public RemoteGifCommand(String url) {
            this(2, url);
        }

        @Override
        @JsonProperty("Command")
        public String command() {
            return "Device/PlayTFGif";
        }
    }

    /** Command to synchronize the device's real-time clock. */
    @JsonPropertyOrder({"Command", "Utc", "Time"})
    record SetUtcCommand(
            @JsonProperty("Utc") long utc,
            @JsonProperty("Time") String time
    ) implements PixooCommand {
        @Override
        @JsonProperty("Command")
        public String command() {
            return "Device/SetUTC";
        }
    }

    /** Command to get channel configuration. */
    record GetDeviceConfigCommand() implements PixooCommand {
        @Override
        @JsonProperty("Command")
        public String command() {
            return "Channel/GetConfig";
        }

        @JsonProperty("Command")
        public String getCommand() {
            return command();
        }
    }

    /** Command to get device system configuration. */
    record GetSysConfigCommand() implements PixooCommand {
        @Override
        @JsonProperty("Command")
        public String command() {
            return "Sys/GetConf";
        }

        @JsonProperty("Command")
        public String getCommand() {
            return command();
        }
    }

    /** Command to update device system configuration. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record SetSysConfigCommand(
            @JsonProperty("Time24Flag") Integer time24Flag,
            @JsonProperty("TemperatureMode") Integer temperatureMode,
            @JsonProperty("DateFormat") Integer dateFormat,
            @JsonProperty("MirrorFlag") Integer mirrorFlag,
            @JsonProperty("AutoPowerOff") Integer autoPowerOff,
            @JsonProperty("GyrateAngle") Integer gyrateAngle,
            @JsonProperty("HighLight") Integer highLight,
            @JsonProperty("Language") Integer language,
            @JsonProperty("NotificationSound") Integer notificationSound,
            @JsonProperty("OnOffVolume") Integer onOffVolume,
            @JsonProperty("BluetoothAutoConnect") Integer bluetoothAutoConnect,
            @JsonProperty("LTime") Integer lockScreenTime,
            @JsonProperty("SProt") Integer screenProtection
    ) implements PixooCommand {
        @Override
        @JsonProperty("Command")
        public String command() {
            return "Sys/DevUpdateConf";
        }
    }

    /** Command to set stopwatch state (0 = stop/pause, 1 = start/resume, 2 = reset). */
    record SetStopWatchCommand(
            @JsonProperty("Status") int status
    ) implements PixooCommand {
        @Override
        @JsonProperty("Command")
        public String command() {
            return "Tools/SetStopWatch";
        }
    }

    /** Command to get stopwatch state. */
    record GetStopWatchCommand() implements PixooCommand {
        @Override
        @JsonProperty("Command")
        public String command() {
            return "Tools/GetStopWatch";
        }

        @JsonProperty("Command")
        public String getCommand() {
            return command();
        }
    }

    /** Command to configure and control the countdown timer. */
    @JsonPropertyOrder({"Command", "Minute", "Second", "Status"})
    record SetTimerCommand(
            @JsonProperty("Minute") int minute,
            @JsonProperty("Second") int second,
            @JsonProperty("Status") int status
    ) implements PixooCommand {
        @Override
        @JsonProperty("Command")
        public String command() {
            return "Tools/SetTimer";
        }
    }

    /** Command to get timer state. */
    record GetTimerCommand() implements PixooCommand {
        @Override
        @JsonProperty("Command")
        public String command() {
            return "Tools/GetTimer";
        }

        @JsonProperty("Command")
        public String getCommand() {
            return command();
        }
    }

    /** Command to configure scoreboard scores. */
    @JsonPropertyOrder({"Command", "BlueScore", "RedScore"})
    record SetScoreBoardCommand(
            @JsonProperty("BlueScore") int blueScore,
            @JsonProperty("RedScore") int redScore
    ) implements PixooCommand {
        @Override
        @JsonProperty("Command")
        public String command() {
            return "Tools/SetScoreBoard";
        }
    }

    /** Command to get scoreboard scores. */
    record GetScoreBoardCommand() implements PixooCommand {
        @Override
        @JsonProperty("Command")
        public String command() {
            return "Tools/GetScoreBoard";
        }

        @JsonProperty("Command")
        public String getCommand() {
            return command();
        }
    }

    /** Command to control the ambient noise meter (0 = stop, 1 = start). */
    record SetNoiseStatusCommand(
            @JsonProperty("NoiseStatus") int noiseStatus
    ) implements PixooCommand {
        @Override
        @JsonProperty("Command")
        public String command() {
            return "Tools/SetNoiseStatus";
        }
    }

    /** Command to get noise meter status. */
    record GetNoiseStatusCommand() implements PixooCommand {
        @Override
        @JsonProperty("Command")
        public String command() {
            return "Tools/GetNoiseStatus";
        }

        @JsonProperty("Command")
        public String getCommand() {
            return command();
        }
    }

    /** Command to set default channel on device boot. */
    record SetStartupChannelCommand(
            @JsonProperty("ChannelIndex") int channelIndex
    ) implements PixooCommand {
        @Override
        @JsonProperty("Command")
        public String command() {
            return "Channel/SetStartupChannel";
        }
    }

    /** Command to get default channel on device boot. */
    record GetStartupChannelCommand() implements PixooCommand {
        @Override
        @JsonProperty("Command")
        public String command() {
            return "Channel/GetStartupChannel";
        }

        @JsonProperty("Command")
        public String getCommand() {
            return command();
        }
    }

    /** Command to select active clock face by ID. */
    record SetClockSelectIdCommand(
            @JsonProperty("ClockId") int clockId
    ) implements PixooCommand {
        @Override
        @JsonProperty("Command")
        public String command() {
            return "Channel/SetClockSelectId";
        }
    }

    /** Command to get clock face information. */
    record GetClockInfoCommand() implements PixooCommand {
        @Override
        @JsonProperty("Command")
        public String command() {
            return "Channel/GetClockInfo";
        }

        @JsonProperty("Command")
        public String getCommand() {
            return command();
        }
    }

    /** Command to set custom gallery page index (0..2). */
    record SetCustomPageIndexCommand(
            @JsonProperty("CustomPageIndex") int customPageIndex
    ) implements PixooCommand {
        @Override
        @JsonProperty("Command")
        public String command() {
            return "Channel/SetCustomPageIndex";
        }
    }

    /** Command to get custom gallery page index. */
    record GetCustomPageIndexCommand() implements PixooCommand {
        @Override
        @JsonProperty("Command")
        public String command() {
            return "Channel/GetCustomPageIndex";
        }

        @JsonProperty("Command")
        public String getCommand() {
            return command();
        }
    }

    /** Command to check if flash storage is full. */
    record GetStorageStatusCommand() implements PixooCommand {
        @Override
        @JsonProperty("Command")
        public String command() {
            return "Device/GetStorageStatus";
        }

        @JsonProperty("Command")
        public String getCommand() {
            return command();
        }
    }

    /** Command to set delay power off timer in minutes (0 to cancel). */
    record SetDelayPowerOffCommand(
            @JsonProperty("DelayPowerOff") int delayPowerOff
    ) implements PixooCommand {
        @Override
        @JsonProperty("Command")
        public String command() {
            return "Device/SetDelayPowerOff";
        }
    }

    /** Command to get delay power off timer. */
    record GetDelayPowerOffCommand() implements PixooCommand {
        @Override
        @JsonProperty("Command")
        public String command() {
            return "Device/GetDelayPowerOff";
        }

        @JsonProperty("Command")
        public String getCommand() {
            return command();
        }
    }

    /** Command to configure pomodoro focus timer. */
    record TomatoSetCommand(
            @JsonProperty("TomatoId") int tomatoId,
            @JsonProperty("TomatoName") String tomatoName,
            @JsonProperty("WorkTime") int workTime,
            @JsonProperty("ShortRestTime") int shortRestTime,
            @JsonProperty("LongRestTime") int longRestTime
    ) implements PixooCommand {
        @Override
        @JsonProperty("Command")
        public String command() {
            return "Tomato/Set";
        }
    }

    /** Command to start a pomodoro focus timer. */
    record TomatoStartCommand(
            @JsonProperty("TomatoId") int tomatoId
    ) implements PixooCommand {
        @Override
        @JsonProperty("Command")
        public String command() {
            return "Tomato/Start";
        }
    }

    /** Command to retrieve configured alarms. */
    record AlarmGetCommand(
            @JsonProperty("IsGetAll") int isGetAll
    ) implements PixooCommand {
        public AlarmGetCommand() {
            this(1);
        }

        @Override
        @JsonProperty("Command")
        public String command() {
            return "Alarm/Get";
        }
    }

    /** Command to configure an alarm. */
    record AlarmSetCommand(
            @JsonProperty("AlarmId") int alarmId,
            @JsonProperty("AlarmName") String alarmName,
            @JsonProperty("AlarmTime") long alarmTime,
            @JsonProperty("EnableFlag") int enableFlag,
            @JsonProperty("RepeatArray") java.util.List<Integer> repeatArray,
            @JsonProperty("Volume") int volume,
            @JsonProperty("SoundType") int soundType
    ) implements PixooCommand {
        @Override
        @JsonProperty("Command")
        public String command() {
            return "Alarm/Set";
        }
    }

    /** Command to delete an alarm. */
    record AlarmDelCommand(
            @JsonProperty("AlarmId") int alarmId
    ) implements PixooCommand {
        @Override
        @JsonProperty("Command")
        public String command() {
            return "Alarm/Del";
        }
    }

    /** Command to configure a memorial day countdown. */
    record MemorialSetCommand(
            @JsonProperty("MemorialId") int memorialId,
            @JsonProperty("MemorialName") String memorialName,
            @JsonProperty("MemorialMoon") int memorialMoon,
            @JsonProperty("MemorialDay") int memorialDay,
            @JsonProperty("MemorialTime") int memorialTime
    ) implements PixooCommand {
        @Override
        @JsonProperty("Command")
        public String command() {
            return "Memorial/Set";
        }
    }

    /** Command to delete a memorial day countdown. */
    record MemorialDelCommand(
            @JsonProperty("MemorialId") int memorialId
    ) implements PixooCommand {
        @Override
        @JsonProperty("Command")
        public String command() {
            return "Memorial/Del";
        }
    }
}
