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
package io.github.glaforge.jixoo.cloud.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

/**
 * Response returned by {@code Device/GetListV2}.
 *
 * @param returnCode    Status code returned by the Divoom server (0 indicates success).
 * @param returnMessage Status message or error description.
 * @param deviceList    List of devices bound to the user's account.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DivoomDeviceListResponse(
        @JsonProperty("ReturnCode") int returnCode,
        @JsonProperty("ReturnMessage") String returnMessage,
        @JsonProperty("DeviceList") List<DeviceItem> deviceList
) implements DivoomResponse {

    @Override
    public List<DeviceItem> deviceList() {
        return deviceList != null ? deviceList : Collections.emptyList();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DeviceItem(
            @JsonProperty("DeviceId") int deviceId,
            @JsonProperty("DeviceName") String deviceName,
            @JsonProperty("DevicePrivateIP") String devicePrivateIP,
            @JsonProperty("DeviceType") int deviceType,
            @JsonProperty("DeviceSSID") String deviceSSID,
            @JsonProperty("DeviceVersion") String deviceVersion
    ) {
    }
}
