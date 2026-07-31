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
package io.github.glaforge.jixoo.example;

import io.github.glaforge.jixoo.api.PixooChannel;
import io.github.glaforge.jixoo.api.PixooClient;
import io.github.glaforge.jixoo.api.PixooResponse;
import io.github.glaforge.jixoo.discovery.PixooDevice;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class NetworkScannerApp {

    public static void main(String[] args) {
        System.out.println("=== Divoom Pixoo64 Device Scanner ===");

        // Step 1: UDP Broadcast Discovery
        System.out.println("\n[Step 1] Attempting UDP Broadcast Discovery (ports 5000, 7000, 3333)...");
        List<PixooDevice> udpDevices = PixooDevice.discoverDevices(Duration.ofSeconds(3));

        if (!udpDevices.isEmpty()) {
            System.out.println("✓ Discovered via UDP Broadcast:");
            for (PixooDevice dev : udpDevices) {
                System.out.println("  - IP: " + dev.ipAddress() + " | Name: " + dev.deviceName() + " | MAC: " + dev.macAddress());
            }
            return;
        }
        System.out.println("  No responses from UDP broadcast.");

        // Step 2: Probing active IPs from ARP table
        System.out.println("\n[Step 2] Reading local ARP table for active network IPs...");
        List<String> arpIps = getArpIps();
        System.out.println("  Found " + arpIps.size() + " active IP(s) in ARP table. Probing for Pixoo64 HTTP API on port 80...");

        List<String> foundIps = probeIpList(arpIps);
        if (!foundIps.isEmpty()) {
            System.out.println("\n✓ FOUND Pixoo64 via ARP probe:");
            for (String ip : foundIps) {
                System.out.println("  -> IP: " + ip);
            }
            return;
        }

        // Step 3: Full Subnet Probe
        System.out.println("\n[Step 3] Full 192.168.86.1 - 192.168.86.254 HTTP probe (ports 80 & 8080)...");
        List<String> allSubnetIps = new ArrayList<>();
        for (int i = 1; i <= 254; i++) {
            allSubnetIps.add("192.168.86." + i);
        }

        foundIps = probeIpList(allSubnetIps);
        if (!foundIps.isEmpty()) {
            System.out.println("\n✓ FOUND Pixoo64 via full subnet probe:");
            for (String ip : foundIps) {
                System.out.println("  -> IP: " + ip);
            }
        } else {
            System.out.println("\n✖ No Pixoo64 device detected on the local network (192.168.86.*).");
            System.out.println("  Please check that the device is powered on, connected to the same Wi-Fi SSID, and that local device isolation is disabled on your router.");
        }
    }

    private static List<String> getArpIps() {
        List<String> ips = new ArrayList<>();
        try {
            Process process = Runtime.getRuntime().exec("arp -a");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains(" at ") && !line.contains("(incomplete)")) {
                        int start = line.indexOf('(');
                        int end = line.indexOf(')');
                        if (start != -1 && end != -1 && end > start) {
                            String ip = line.substring(start + 1, end);
                            if (ip.startsWith("192.168.86.") && !ip.endsWith(".255")) {
                                ips.add(ip);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading ARP table: " + e.getMessage());
        }
        return ips;
    }

    private static List<String> probeIpList(List<String> ips) {
        List<String> matched = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(60);
        List<Future<String>> futures = new ArrayList<>();

        for (String ip : ips) {
            futures.add(executor.submit(() -> probePixoo(ip)));
        }

        for (Future<String> future : futures) {
            try {
                String result = future.get();
                if (result != null) {
                    matched.add(result);
                }
            } catch (Exception ignored) {
            }
        }

        executor.shutdown();
        return matched;
    }

    private static String probePixoo(String ip) {
        int[] ports = {80, 8080};
        for (int port : ports) {
            try {
                PixooClient client = PixooClient.builder()
                        .ipAddress(ip)
                        .port(port)
                        .connectTimeout(Duration.ofMillis(400))
                        .requestTimeout(Duration.ofMillis(600))
                        .autoSwitchToCustomChannel(false)
                        .build();

                PixooResponse response = client.selectChannel(PixooChannel.CUSTOM);
                if (response != null && response.isSuccess()) {
                    return ip + (port != 80 ? ":" + port : "");
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
