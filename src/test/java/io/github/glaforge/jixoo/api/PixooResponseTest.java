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
package io.github.glaforge.jixoo.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PixooResponseTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static PixooResponse parse(String json) throws Exception {
        return MAPPER.readValue(json, PixooResponse.class);
    }

    @Test
    void parsesPixoo64Success() throws Exception {
        PixooResponse response = parse("{\"error_code\": 0}");
        assertTrue(response.isSuccess());
    }

    @Test
    void parsesPixoo64StringError() throws Exception {
        PixooResponse response = parse("{\"error_code\": \"Request data illegal json\"}");
        assertFalse(response.isSuccess());
        assertEquals(-1, response.errorCode());
        assertEquals("Request data illegal json", response.errorMessage());
    }

    @Test
    void parsesPixoo64IISuccessWithEmptyMessage() throws Exception {
        PixooResponse response = parse("{\"Command\": \"Channel/GetIndex\", \"DeviceId\": 123456789, \"PacketFlag\": 1792850393,"
                + " \"DeviceType\": \"Divoom\", \"ReturnCode\": 0, \"ReturnMessage\": \"\", \"WifiSingal\": 0, \"SelectIndex\": 2}");
        assertTrue(response.isSuccess());
        assertNull(response.errorMessage());
    }

    @Test
    void parsesPixoo64IIUnknownCommand() throws Exception {
        PixooResponse response = parse("{\"ReturnCode\": 1, \"ReturnMessage\": \"Only accept JSON parameters\"}");
        assertFalse(response.isSuccess());
        assertEquals(1, response.errorCode());
        assertEquals("Only accept JSON parameters", response.errorMessage());
    }

    @Test
    void modelsExposeTheirEndpoint() {
        assertEquals(80, PixooModel.PIXOO_64.defaultPort());
        assertEquals("/post", PixooModel.PIXOO_64.apiPath());
        assertEquals(9000, PixooModel.PIXOO_64_II.defaultPort());
        assertEquals("/divoom_api", PixooModel.PIXOO_64_II.apiPath());
    }

    @Test
    void stillImagesPlayShortOnlyOnThePixoo64II() {
        assertEquals(60000, PixooModel.PIXOO_64.stillFrameDelayMs(60000));
        assertEquals(1000, PixooModel.PIXOO_64_II.stillFrameDelayMs(60000));
        assertTrue(PixooModel.PIXOO_64.unsupportedReason("Channel/SetStartupChannel").isEmpty());
        assertTrue(PixooModel.PIXOO_64_II.unsupportedReason("Channel/SetStartupChannel").isPresent());
        assertTrue(PixooModel.PIXOO_64_II.unsupportedReason("Channel/SetBrightness").isEmpty());
    }
}
