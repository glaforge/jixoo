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
package io.github.glaforge.jixoo.model.sys;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * System configuration settings for the Pixoo-64 device.
 *
 * @param time24Flag           0 = 12-hour format, 1 = 24-hour format
 * @param temperatureMode      0 = Celsius, 1 = Fahrenheit
 * @param dateFormat           date display format index (0..8)
 * @param mirrorFlag           0 = normal, 1 = mirror display
 * @param autoPowerOff         idle sleep timer in minutes (0 = disabled)
 * @param gyrateAngle          rotation angle (0, 90, 180, 270)
 * @param highLight            highlight boost mode
 * @param whiteBalanceR        white balance red adjustment (0..100)
 * @param whiteBalanceG        white balance green adjustment (0..100)
 * @param whiteBalanceB        white balance blue adjustment (0..100)
 * @param language             system language code
 * @param notificationSound    sound notification flag
 * @param onOffVolume          power on/off audio volume
 * @param bluetoothAutoConnect bluetooth auto-connect flag
 * @param deviceAutoUpdate     firmware auto-update flag
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SysConfig(
        @JsonProperty("Time24Flag") Integer time24Flag,
        @JsonProperty("TemperatureMode") Integer temperatureMode,
        @JsonProperty("DateFormat") Integer dateFormat,
        @JsonProperty("MirrorFlag") Integer mirrorFlag,
        @JsonProperty("AutoPowerOff") Integer autoPowerOff,
        @JsonProperty("GyrateAngle") Integer gyrateAngle,
        @JsonProperty("HighLight") Integer highLight,
        @JsonProperty("WhiteBalanceR") Integer whiteBalanceR,
        @JsonProperty("WhiteBalanceG") Integer whiteBalanceG,
        @JsonProperty("WhiteBalanceB") Integer whiteBalanceB,
        @JsonProperty("Language") Integer language,
        @JsonProperty("NotificationSound") Integer notificationSound,
        @JsonProperty("OnOffVolume") Integer onOffVolume,
        @JsonProperty("BluetoothAutoConnect") Integer bluetoothAutoConnect,
        @JsonProperty("DeviceAutoUpdate") Integer deviceAutoUpdate
) {
    public boolean is24HourMode() {
        return time24Flag != null && time24Flag == 1;
    }

    public boolean isFahrenheit() {
        return temperatureMode != null && temperatureMode == 1;
    }

    public boolean isMirror() {
        return mirrorFlag != null && mirrorFlag == 1;
    }
}
