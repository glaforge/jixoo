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
package com.divoom.pixoo64.discovery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

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
     * Broadcasts UDP discovery packets to discover Pixoo devices on the local subnet.
     *
     * @param timeout Timeout duration for listening for responses
     * @return List of discovered devices
     */
    public static List<PixooDevice> discoverDevices(Duration timeout) {
        List<PixooDevice> devices = new ArrayList<>();
        int[] discoveryPorts = {5000, 7000, 3333};

        for (int port : discoveryPorts) {
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setBroadcast(true);
                socket.setSoTimeout((int) timeout.toMillis());

                byte[] sendData = "DISCOVER".getBytes(StandardCharsets.UTF_8);
                InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, broadcastAddress, port);

                socket.send(sendPacket);

                byte[] receiveBuf = new byte[2048];
                long endTime = System.currentTimeMillis() + timeout.toMillis();

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

    private static PixooDevice parseDiscoveryResponse(String ip, String jsonResponse) {
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
