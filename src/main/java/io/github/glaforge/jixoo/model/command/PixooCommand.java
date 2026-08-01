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
        PixooCommand.RotationCommand,
        PixooCommand.PlayBuzzerCommand,
        PixooCommand.RemoteGifCommand {

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

    /** Command to set the screen rotation angle. */
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
}
