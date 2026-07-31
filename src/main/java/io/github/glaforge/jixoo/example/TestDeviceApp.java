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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class TestDeviceApp {

    public static void main(String[] args) {
        String ip;
        try {
            ip = ExampleUtils.resolveIpAddress(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return;
        }
        System.out.println("Testing connection to Pixoo64 at IP: " + ip);

        try {
            PixooClient client = PixooClient.create(ip);

            System.out.println("1. Setting channel to CUSTOM...");
            PixooResponse chResp = client.selectChannel(PixooChannel.CUSTOM);
            System.out.println("   Response: " + chResp);

            System.out.println("2. Sending test 64x64 color grid image...");
            BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            // Draw a 4-color quadrant grid
            g.setColor(Color.RED);
            g.fillRect(0, 0, 32, 32);
            g.setColor(Color.GREEN);
            g.fillRect(32, 0, 32, 32);
            g.setColor(Color.BLUE);
            g.fillRect(0, 32, 32, 32);
            g.setColor(Color.YELLOW);
            g.fillRect(32, 32, 32, 32);
            g.dispose();

            PixooResponse imgResp = client.sendImage(img);
            System.out.println("   Response: " + imgResp);

            System.out.println("3. Setting brightness to 80%...");
            PixooResponse brightResp = client.setBrightness(80);
            System.out.println("   Response: " + brightResp);

            System.out.println("\nSuccess! Pixoo64 display updated at " + ip);

        } catch (Exception e) {
            System.err.println("Error communicating with Pixoo64 at " + ip + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
