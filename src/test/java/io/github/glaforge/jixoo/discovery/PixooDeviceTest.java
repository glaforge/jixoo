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
package io.github.glaforge.jixoo.discovery;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PixooDeviceTest {

    @Test
    void testParseArpOutputMacOs() {
        String macOsArpOutput = """
                ? (192.168.86.1) at 70:a7:41:aa:bb:cc on en0 ifscope [ethernet]
                ? (192.168.86.161) at d8:c0:a6:11:22:33 on en0 ifscope [ethernet]
                ? (224.0.0.251) at 1:0:5e:0:0:fb on en0 ifscope permanent [ethernet]
                ? (255.255.255.255) at ff:ff:ff:ff:ff:ff on en0 ifscope [ethernet]
                """;

        Map<String, String> parsed = PixooDevice.parseArpOutput(macOsArpOutput);

        assertEquals(2, parsed.size());
        assertEquals("70:a7:41:aa:bb:cc", parsed.get("192.168.86.1"));
        assertEquals("d8:c0:a6:11:22:33", parsed.get("192.168.86.161"));
        assertNull(parsed.get("224.0.0.251"));
    }

    @Test
    void testParseArpOutputLinux() {
        String linuxArpOutput = """
                router (192.168.1.1) at 70:a7:41:aa:bb:cc [ether] on eth0
                pixoo (192.168.1.161) at d8:c0:a6:11:22:33 [ether] on eth0
                incomplete (192.168.1.200) at <incomplete> [ether] on eth0
                """;

        Map<String, String> parsed = PixooDevice.parseArpOutput(linuxArpOutput);

        assertEquals(2, parsed.size());
        assertEquals("70:a7:41:aa:bb:cc", parsed.get("192.168.1.1"));
        assertEquals("d8:c0:a6:11:22:33", parsed.get("192.168.1.161"));
    }

    @Test
    void testParseArpOutputWindows() {
        String windowsArpOutput = """
                Interface: 192.168.1.50 --- 0x2
                  Internet Address      Physical Address      Type
                  192.168.1.1           70-a7-41-aa-bb-cc     dynamic
                  192.168.1.161         d8-c0-a6-11-22-33     dynamic
                  192.168.1.255         ff-ff-ff-ff-ff-ff     static
                """;

        Map<String, String> parsed = PixooDevice.parseArpOutput(windowsArpOutput);

        assertEquals(2, parsed.size());
        assertEquals("70-a7-41-aa-bb-cc", parsed.get("192.168.1.1"));
        assertEquals("d8-c0-a6-11-22-33", parsed.get("192.168.1.161"));
    }

    @Test
    void testParseDiscoveryResponseJson() {
        String json = """
                {"DeviceMac":"d8c0a6112233","DeviceName":"MyPixoo","DeviceId":12345}
                """;

        PixooDevice device = PixooDevice.parseDiscoveryResponse("192.168.1.100", json);

        assertNotNull(device);
        assertEquals("192.168.1.100", device.ipAddress());
        assertEquals("d8c0a6112233", device.macAddress());
        assertEquals("MyPixoo", device.deviceName());
        assertEquals(12345, device.deviceId());
    }

    @Test
    void testParseDiscoveryResponseFallback() {
        PixooDevice device = PixooDevice.parseDiscoveryResponse("192.168.1.100", "PLAIN_TEXT_RESPONSE");

        assertNotNull(device);
        assertEquals("192.168.1.100", device.ipAddress());
        assertEquals("", device.macAddress());
        assertEquals("Pixoo64", device.deviceName());
        assertEquals(0, device.deviceId());
    }
}
