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
package io.github.glaforge.jixoo.model.tool;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Representation of a scheduled alarm on the Pixoo hardware display.
 *
 * @param alarmId     Alarm index (1..N)
 * @param alarmName   Display label/name
 * @param alarmTime   Unix timestamp or daily second offset
 * @param enableFlag  1 if enabled, 0 if disabled
 * @param repeatArray Days of week to repeat (e.g. 0=Sun, 1=Mon, ..., 6=Sat)
 * @param volume      Alarm speaker buzzer volume (0..100)
 * @param soundType   Buzzer rhythm / tone preset
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PixooAlarm(
        @JsonProperty("AlarmId") int alarmId,
        @JsonProperty("AlarmName") String alarmName,
        @JsonProperty("AlarmTime") long alarmTime,
        @JsonProperty("EnableFlag") int enableFlag,
        @JsonProperty("RepeatArray") List<Integer> repeatArray,
        @JsonProperty("Volume") int volume,
        @JsonProperty("SoundType") int soundType
) {
    public boolean isEnabled() {
        return enableFlag == 1;
    }

    public static PixooAlarm of(int alarmId, String name, int hour, int minute, List<Integer> repeatDays) {
        long timeOffsetSeconds = hour * 3600L + minute * 60L;
        return new PixooAlarm(alarmId, name, timeOffsetSeconds, 1, repeatDays != null ? repeatDays : List.of(), 80, 1);
    }
}
