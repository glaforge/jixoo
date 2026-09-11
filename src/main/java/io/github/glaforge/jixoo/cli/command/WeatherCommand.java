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
package io.github.glaforge.jixoo.cli.command;

import io.github.glaforge.jixoo.weather.CurrentWeather;
import io.github.glaforge.jixoo.weather.DivoomWeatherClient;
import io.github.glaforge.jixoo.weather.WeatherForecast;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

/**
 * CLI Command to query real-time weather and forecasts via Divoom's zero-auth weather proxy.
 */
@Command(
        name = "weather",
        description = "Query real-time weather and forecasts via Divoom's zero-auth OpenWeatherMap proxy.",
        subcommands = {
                WeatherCommand.CurrentSubcommand.class,
                WeatherCommand.ForecastSubcommand.class
        }
)
public class WeatherCommand implements Callable<Integer> {

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.")
    private boolean helpRequested;

    @Override
    public Integer call() {
        CommandLine.usage(this, System.out);
        return 0;
    }

    @Command(name = "current", description = "Query current weather conditions.")
    public static class CurrentSubcommand implements Callable<Integer> {
        @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.")
        private boolean helpRequested;

        @Option(names = {"--lon", "--longitude"}, required = true, description = "Longitude coordinate (e.g. 2.3522)")
        private double longitude;

        @Option(names = {"--lat", "--latitude"}, required = true, description = "Latitude coordinate (e.g. 48.8566)")
        private double latitude;

        @Option(names = {"--lang"}, defaultValue = "en", description = "Language code (e.g. en, fr, de, es)")
        private String lang;

        @Option(names = {"--units"}, defaultValue = "metric", description = "Units format: 'metric' (Celsius) or 'imperial' (Fahrenheit)")
        private String units;

        @Override
        public Integer call() {
            try {
                DivoomWeatherClient client = new DivoomWeatherClient();
                CurrentWeather w = client.getCurrentWeather(longitude, latitude, lang, units);

                String unitSymbol = "imperial".equalsIgnoreCase(units) ? "°F" : "°C";
                String speedUnit = "imperial".equalsIgnoreCase(units) ? "mph" : "m/s";

                System.out.printf("Weather for %s, %s (%.4f, %.4f):%n",
                        w.name() != null ? w.name() : "Unknown",
                        w.sys() != null && w.sys().country() != null ? w.sys().country() : "",
                        latitude,
                        longitude);
                System.out.printf("  Condition:   %s (%s)%n", w.condition(), w.description());
                System.out.printf("  Temperature: %.1f%s (Feels like: %.1f%s)%n", w.temperature(), unitSymbol, w.feelsLike(), unitSymbol);
                System.out.printf("  Humidity:    %d%%%n", w.humidity());
                if (w.wind() != null) {
                    System.out.printf("  Wind:        %.1f %s (direction: %d°) %n", w.wind().speed(), speedUnit, w.wind().deg());
                }
                if (w.main() != null) {
                    System.out.printf("  Pressure:    %d hPa%n", w.main().pressure());
                }
                if (w.icon() != null && !w.icon().isBlank()) {
                    System.out.printf("  Icon code:   %s%n", w.icon());
                }
                return 0;
            } catch (Exception e) {
                System.err.printf("Failed to fetch weather: %s%n", e.getMessage());
                return 1;
            }
        }
    }

    @Command(name = "forecast", description = "Query 5-day / 3-hour interval weather forecast.")
    public static class ForecastSubcommand implements Callable<Integer> {
        @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit.")
        private boolean helpRequested;

        @Option(names = {"--lon", "--longitude"}, required = true, description = "Longitude coordinate (e.g. 2.3522)")
        private double longitude;

        @Option(names = {"--lat", "--latitude"}, required = true, description = "Latitude coordinate (e.g. 48.8566)")
        private double latitude;

        @Option(names = {"--lang"}, defaultValue = "en", description = "Language code (e.g. en, fr, de, es)")
        private String lang;

        @Option(names = {"--units"}, defaultValue = "metric", description = "Units format: 'metric' (Celsius) or 'imperial' (Fahrenheit)")
        private String units;

        @Option(names = {"-n", "--limit"}, defaultValue = "8", description = "Number of 3-hour forecast intervals to display (default: 8 = 24h)")
        private int limit;

        @Override
        public Integer call() {
            try {
                DivoomWeatherClient client = new DivoomWeatherClient();
                WeatherForecast forecast = client.getForecast(longitude, latitude, lang, units);

                String unitSymbol = "imperial".equalsIgnoreCase(units) ? "°F" : "°C";

                String cityName = forecast.city() != null ? forecast.city().name() : "Unknown";
                String country = (forecast.city() != null && forecast.city().country() != null) ? forecast.city().country() : "";
                System.out.printf("Forecast for %s, %s (%.4f, %.4f):%n", cityName, country, latitude, longitude);
                System.out.println("---------------------------------------------------------------");

                if (forecast.list() == null || forecast.list().isEmpty()) {
                    System.out.println("No forecast items available.");
                    return 0;
                }

                int count = Math.min(limit, forecast.list().size());
                for (int i = 0; i < count; i++) {
                    WeatherForecast.ForecastItem item = forecast.list().get(i);
                    System.out.printf("  %s | %5.1f%s | %-16s | rain: %3.0f%%%n",
                            item.dtTxt(),
                            item.temperature(),
                            unitSymbol,
                            item.description(),
                            item.pop() != null ? item.pop() * 100 : 0.0);
                }
                return 0;
            } catch (Exception e) {
                System.err.printf("Failed to fetch weather forecast: %s%n", e.getMessage());
                return 1;
            }
        }
    }
}
