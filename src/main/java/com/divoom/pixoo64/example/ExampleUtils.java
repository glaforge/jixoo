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

/**
 * Utility methods for example applications.
 */
public final class ExampleUtils {

    private ExampleUtils() {}

    /**
     * Resolves the Pixoo64 IP address from command-line arguments, environment variables,
     * or automatic UDP device discovery.
     * <p>
     * Checks first if an IP address is provided as the first element in {@code args}.
     * If not, checks for the {@code PIXOO64_IP_ADDRESS} environment variable.
     * If neither is set, attempts to discover Pixoo64 devices on the local network via UDP broadcast
     * and selects the first discovered device.
     * </p>
     *
     * @param args command-line arguments passed to main()
     * @return the resolved IP address
     * @throws IllegalArgumentException if no IP address is specified and auto-discovery fails
     */
    public static String resolveIpAddress(String[] args) {
        if (args != null && args.length > 0 && args[0] != null && !args[0].isBlank()) {
            return args[0].trim();
        }

        String envIp = System.getenv("PIXOO64_IP_ADDRESS");
        if (envIp != null && !envIp.isBlank()) {
            return envIp.trim();
        }

        System.out.println("No IP specified via argument or PIXOO64_IP_ADDRESS env var. Attempting automatic UDP discovery...");
        List<PixooDevice> devices = PixooDevice.discoverDevices(Duration.ofSeconds(3));
        if (!devices.isEmpty()) {
            String discoveredIp = devices.get(0).ipAddress();
            System.out.println("Auto-discovered Pixoo64 device at IP: " + discoveredIp);
            return discoveredIp;
        }

        throw new IllegalArgumentException(
                "No Pixoo64 IP address specified and automatic UDP discovery found no devices.\n" +
                "Please provide the IP address as the first main argument or set the PIXOO64_IP_ADDRESS environment variable."
        );
    }
}
