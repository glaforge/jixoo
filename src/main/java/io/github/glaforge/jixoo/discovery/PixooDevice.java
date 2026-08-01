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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Discovered Pixoo device information.
 *
 * @param ipAddress  The IP address of the device
 * @param macAddress The MAC address of the device
 * @param deviceName The name of the device
 * @param deviceId   The numeric ID of the device
 */
public record PixooDevice(
        String ipAddress,
        String macAddress,
        String deviceName,
        int deviceId
) {
    private static final Logger log = LoggerFactory.getLogger(PixooDevice.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Discover Pixoo devices on the local network using multi-strategy discovery:
     * 1. Multi-interface UDP broadcast
     * 2. System ARP table scanning with HTTP probing
     * 3. Subnet HTTP sweep fallback
     *
     * @param timeout Timeout duration for listening for responses
     * @return List of discovered devices
     */
    public static List<PixooDevice> discoverDevices(Duration timeout) {
        List<PixooDevice> devices = new ArrayList<>();
        Set<String> discoveredIps = Collections.newSetFromMap(new ConcurrentHashMap<>());

        // Strategy 1: UDP Broadcast across all active network interfaces
        List<PixooDevice> udpDevices = discoverDevicesUdp(Duration.ofMillis(Math.min(1000, timeout.toMillis())));
        for (PixooDevice d : udpDevices) {
            if (discoveredIps.add(d.ipAddress())) {
                devices.add(d);
            }
        }

        if (!devices.isEmpty()) {
            return devices;
        }

        // Strategy 2: System ARP Table + HTTP Probe
        Map<String, String> arpEntries = getSystemArpTable();
        if (!arpEntries.isEmpty()) {
            List<PixooDevice> arpDevices = probeCandidateIps(arpEntries);
            for (PixooDevice d : arpDevices) {
                if (discoveredIps.add(d.ipAddress())) {
                    devices.add(d);
                }
            }
        }

        if (!devices.isEmpty()) {
            return devices;
        }

        // Strategy 3: Subnet HTTP Sweep Fallback
        List<String> subnetIps = getLocalSubnetIps();
        if (!subnetIps.isEmpty()) {
            Map<String, String> subnetMap = new LinkedHashMap<>();
            for (String ip : subnetIps) {
                subnetMap.put(ip, "");
            }
            List<PixooDevice> subnetDevices = probeCandidateIps(subnetMap);
            for (PixooDevice d : subnetDevices) {
                if (discoveredIps.add(d.ipAddress())) {
                    devices.add(d);
                }
            }
        }

        return devices;
    }

    private static List<PixooDevice> discoverDevicesUdp(Duration timeout) {
        List<PixooDevice> devices = new ArrayList<>();
        int[] discoveryPorts = {5000, 7000, 3333};
        List<InetAddress> broadcastAddresses = getBroadcastAddresses();
        long perPortTimeoutMs = Math.max(50, timeout.toMillis() / discoveryPorts.length);

        for (int port : discoveryPorts) {
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setBroadcast(true);
                socket.setSoTimeout((int) perPortTimeoutMs);

                byte[] sendData = "DISCOVER".getBytes(StandardCharsets.UTF_8);
                for (InetAddress bAddr : broadcastAddresses) {
                    try {
                        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, bAddr, port);
                        socket.send(sendPacket);
                    } catch (Exception ignored) {
                    }
                }

                byte[] receiveBuf = new byte[2048];
                long endTime = System.currentTimeMillis() + perPortTimeoutMs;

                while (System.currentTimeMillis() < endTime) {
                    try {
                        DatagramPacket receivePacket = new DatagramPacket(receiveBuf, receiveBuf.length);
                        socket.receive(receivePacket);

                        String responseStr = new String(receivePacket.getData(), 0, receivePacket.getLength(), StandardCharsets.UTF_8).trim();
                        String senderIp = receivePacket.getAddress().getHostAddress();

                        PixooDevice device = parseDiscoveryResponse(senderIp, responseStr);
                        if (device != null && devices.stream().noneMatch(d -> d.ipAddress().equals(senderIp))) {
                            devices.add(device);
                        }
                    } catch (SocketTimeoutException e) {
                        break;
                    }
                }
            } catch (Exception e) {
                log.debug("UDP discovery check failed on port {}", port, e);
            }
        }
        return devices;
    }

    private static List<InetAddress> getBroadcastAddresses() {
        Set<InetAddress> addresses = new HashSet<>();
        try {
            addresses.add(InetAddress.getByName("255.255.255.255"));
            var interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isUp() && !ni.isLoopback()) {
                    for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                        InetAddress b = ia.getBroadcast();
                        if (b != null) {
                            addresses.add(b);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return new ArrayList<>(addresses);
    }

    private static Map<String, String> getSystemArpTable() {
        try {
            Process process = new ProcessBuilder("arp", "-a").redirectErrorStream(true).start();
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }
            process.waitFor(1, TimeUnit.SECONDS);
            return parseArpOutput(sb.toString());
        } catch (Exception e) {
            log.debug("Failed to read system ARP table: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    static Map<String, String> parseArpOutput(String arpOutput) {
        Map<String, String> ipToMac = new LinkedHashMap<>();
        if (arpOutput == null || arpOutput.isBlank()) {
            return ipToMac;
        }
        Pattern ipPattern = Pattern.compile("\\b((?:192\\.168|10\\.|172\\.(?:1[6-9]|2[0-9]|3[01]))\\.\\d{1,3}\\.\\d{1,3})\\b");
        Pattern macPattern = Pattern.compile("\\b([0-9a-fa-f]{1,2}[:-][0-9a-fa-f]{1,2}[:-][0-9a-fa-f]{1,2}[:-][0-9a-fa-f]{1,2}[:-][0-9a-fa-f]{1,2}[:-][0-9a-fa-f]{1,2})\\b", Pattern.CASE_INSENSITIVE);

        for (String line : arpOutput.split("\\r?\\n")) {
            String lower = line.toLowerCase();
            if (lower.contains("incomplete") || lower.contains("interface:") || lower.contains("ff:ff:ff:ff:ff:ff") || lower.contains("ff-ff-ff-ff-ff-ff")) {
                continue;
            }
            Matcher ipMatcher = ipPattern.matcher(line);
            if (ipMatcher.find()) {
                String ip = ipMatcher.group(1);
                if (ip.endsWith(".255") || ip.endsWith(".0")) {
                    continue;
                }
                String mac = "";
                Matcher macMatcher = macPattern.matcher(line);
                if (macMatcher.find()) {
                    mac = macMatcher.group(1);
                }
                ipToMac.putIfAbsent(ip, mac);
            }
        }
        return ipToMac;
    }

    private static List<PixooDevice> probeCandidateIps(Map<String, String> candidateIps) {
        List<PixooDevice> found = new ArrayList<>();
        if (candidateIps.isEmpty()) {
            return found;
        }

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<PixooDevice>> futures = new ArrayList<>();
            for (Map.Entry<String, String> entry : candidateIps.entrySet()) {
                String ip = entry.getKey();
                String mac = entry.getValue();
                futures.add(executor.submit(() -> probePixooHttp(ip, mac)));
            }

            for (Future<PixooDevice> f : futures) {
                try {
                    PixooDevice device = f.get();
                    if (device != null) {
                        found.add(device);
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return found;
    }

    private static final HttpClient PROBE_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofMillis(800))
            .build();

    private static PixooDevice probePixooHttp(String ip, String macAddress) {
        int[] ports = {80, 8080};

        for (int port : ports) {
            try {
                String uriStr = "http://" + ip + (port != 80 ? ":" + port : "") + "/post";
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(uriStr))
                        .timeout(Duration.ofMillis(1000))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString("{\"Command\": \"Channel/GetIndex\"}"))
                        .build();

                HttpResponse<String> response = PROBE_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200 && response.body() != null && response.body().contains("\"error_code\": 0")) {
                    return new PixooDevice(ip, macAddress != null ? macAddress : "", "Pixoo64", 0);
                }
            } catch (Exception e) {
                log.debug("HTTP probe failed for {}:{}", ip, port, e);
            }
        }
        return null;
    }

    private static List<String> getLocalSubnetIps() {
        List<String> ips = new ArrayList<>();
        try {
            var interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isUp() && !ni.isLoopback()) {
                    for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                        InetAddress addr = ia.getAddress();
                        if (addr instanceof java.net.Inet4Address) {
                            String hostAddress = addr.getHostAddress();
                            int lastDot = hostAddress.lastIndexOf('.');
                            if (lastDot > 0) {
                                String subnetPrefix = hostAddress.substring(0, lastDot + 1);
                                for (int i = 1; i <= 254; i++) {
                                    ips.add(subnetPrefix + i);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return ips;
    }

    static PixooDevice parseDiscoveryResponse(String ip, String jsonResponse) {
        try {
            if (jsonResponse.startsWith("{")) {
                JsonNode root = MAPPER.readTree(jsonResponse);
                String mac = root.has("DeviceMac") ? root.get("DeviceMac").asText() : "";
                String name = root.has("DeviceName") ? root.get("DeviceName").asText() : "Pixoo64";
                int id = root.has("DeviceId") ? root.get("DeviceId").asInt() : 0;
                return new PixooDevice(ip, mac, name, id);
            }
        } catch (Exception ignored) {
        }
        return new PixooDevice(ip, "", "Pixoo64", 0);
    }
}

