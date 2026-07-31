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
package com.divoom.pixoo64.example;

import com.divoom.pixoo64.discovery.PixooDevice;

import java.time.Duration;
import java.util.List;

public class DiscoverApp {
    public static void main(String[] args) {
        System.out.println("Searching for Divoom Pixoo64 devices on the local network (5s timeout)...");
        List<PixooDevice> devices = PixooDevice.discoverDevices(Duration.ofSeconds(5));

        if (devices.isEmpty()) {
            System.out.println("No Pixoo64 devices responded to UDP discovery.");
        } else {
            System.out.println("Found " + devices.size() + " Pixoo64 device(s):");
            for (PixooDevice device : devices) {
                System.out.println(" - Name: " + device.deviceName());
                System.out.println("   IP:   " + device.ipAddress());
                System.out.println("   MAC:  " + device.macAddress());
                System.out.println("   ID:   " + device.deviceId());
            }
        }
    }
}
