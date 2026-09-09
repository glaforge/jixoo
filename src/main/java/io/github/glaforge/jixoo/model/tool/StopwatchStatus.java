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

/**
 * Status response for the device stopwatch tool.
 *
 * @param status the status integer (0 = stopped/paused, 1 = running, 2 = reset)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record StopwatchStatus(
        @JsonProperty("Status") int status
) {
    public boolean isRunning() {
        return status == 1;
    }

    public boolean isStopped() {
        return status == 0;
    }
}
