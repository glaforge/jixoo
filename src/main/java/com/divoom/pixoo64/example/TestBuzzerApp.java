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

import com.divoom.pixoo64.api.PixooClient;
import com.divoom.pixoo64.api.PixooResponse;

public class TestBuzzerApp {

    public static void main(String[] args) {
        String ip;
        try {
            ip = ExampleUtils.resolveIpAddress(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return;
        }
        System.out.println("Triggering buzzer on Pixoo64 at IP: " + ip);

        try {
            PixooClient client = PixooClient.create(ip);

            // Pattern 1: Standard alarm beep (300ms sound, 300ms silence, 3000ms total duration)
            System.out.println("1. Playing alarm beep pattern (3 seconds)...");
            PixooResponse resp1 = client.playBuzzer(300, 300, 3000);
            System.out.println("   Response: " + resp1);

            Thread.sleep(3500);

            // Pattern 2: Short double beep (100ms sound, 100ms silence, 1000ms total duration)
            System.out.println("2. Playing short rapid beep pattern (1 second)...");
            PixooResponse resp2 = client.playBuzzer(100, 100, 1000);
            System.out.println("   Response: " + resp2);

            System.out.println("\nBuzzer test completed!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
