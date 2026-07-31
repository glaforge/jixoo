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

import io.github.glaforge.jixoo.model.command.PixooCommand;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestJsonSerialization {
    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        PixooCommand cmd1 = new PixooCommand.ChannelIndexCommand(3);
        PixooCommand cmd2 = new PixooCommand.ResetGifCommand();
        PixooCommand cmd3 = new PixooCommand.SendGifCommand(1, 0, 12345, 100, "BASE64TEST");

        System.out.println("Cmd1: " + mapper.writeValueAsString(cmd1));
        System.out.println("Cmd2: " + mapper.writeValueAsString(cmd2));
        System.out.println("Cmd3: " + mapper.writeValueAsString(cmd3));
    }
}
