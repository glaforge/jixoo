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
 * Represents the 5-day / 3-hour interval weather forecast returned by Divoom's weather proxy.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WeatherForecast(
        @JsonProperty("cod") String cod,
        @JsonProperty("message") Integer message,
        @JsonProperty("cnt") int cnt,
        @JsonProperty("list") List<ForecastItem> list,
        @JsonProperty("city") CityDetails city
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ForecastItem(
            @JsonProperty("dt") long dt,
            @JsonProperty("main") CurrentWeather.MainDetails main,
            @JsonProperty("weather") List<CurrentWeather.Condition> weather,
            @JsonProperty("clouds") CurrentWeather.CloudsDetails clouds,
            @JsonProperty("wind") CurrentWeather.WindDetails wind,
            @JsonProperty("visibility") Integer visibility,
            @JsonProperty("pop") Double pop,
            @JsonProperty("dt_txt") String dtTxt
    ) {
        public double temperature() {
            return main != null ? main.temp() : 0.0;
        }

        public String description() {
            return (weather != null && !weather.isEmpty()) ? weather.get(0).description() : "";
        }

        public String icon() {
            return (weather != null && !weather.isEmpty()) ? weather.get(0).icon() : "";
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CityDetails(
            @JsonProperty("id") long id,
            @JsonProperty("name") String name,
            @JsonProperty("coord") CurrentWeather.Coord coord,
            @JsonProperty("country") String country,
            @JsonProperty("timezone") int timezone,
            @JsonProperty("sunrise") long sunrise,
            @JsonProperty("sunset") long sunset
    ) {}
}
