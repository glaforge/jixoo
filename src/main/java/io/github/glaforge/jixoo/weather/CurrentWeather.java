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
package io.github.glaforge.jixoo.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Represents the current weather data returned by Divoom's weather proxy.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CurrentWeather(
        @JsonProperty("coord") Coord coord,
        @JsonProperty("weather") List<Condition> weather,
        @JsonProperty("main") MainDetails main,
        @JsonProperty("wind") WindDetails wind,
        @JsonProperty("clouds") CloudsDetails clouds,
        @JsonProperty("visibility") Integer visibility,
        @JsonProperty("dt") Long dt,
        @JsonProperty("sys") SysDetails sys,
        @JsonProperty("timezone") Integer timezone,
        @JsonProperty("id") Long id,
        @JsonProperty("name") String name,
        @JsonProperty("cod") Integer cod
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Coord(@JsonProperty("lon") double lon, @JsonProperty("lat") double lat) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Condition(
            @JsonProperty("id") int id,
            @JsonProperty("main") String main,
            @JsonProperty("description") String description,
            @JsonProperty("icon") String icon
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MainDetails(
            @JsonProperty("temp") double temp,
            @JsonProperty("feels_like") double feelsLike,
            @JsonProperty("temp_min") double tempMin,
            @JsonProperty("temp_max") double tempMax,
            @JsonProperty("pressure") int pressure,
            @JsonProperty("humidity") int humidity
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WindDetails(
            @JsonProperty("speed") double speed,
            @JsonProperty("deg") int deg
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CloudsDetails(
            @JsonProperty("all") int all
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SysDetails(
            @JsonProperty("country") String country,
            @JsonProperty("sunrise") long sunrise,
            @JsonProperty("sunset") long sunset
    ) {}

    public double temperature() {
        return main != null ? main.temp() : 0.0;
    }

    public double feelsLike() {
        return main != null ? main.feelsLike() : 0.0;
    }

    public int humidity() {
        return main != null ? main.humidity() : 0;
    }

    public String description() {
        if (weather != null && !weather.isEmpty()) {
            return weather.get(0).description();
        }
        return "";
    }

    public String condition() {
        if (weather != null && !weather.isEmpty()) {
            return weather.get(0).main();
        }
        return "";
    }

    public String icon() {
        if (weather != null && !weather.isEmpty()) {
            return weather.get(0).icon();
        }
        return "";
    }
}
