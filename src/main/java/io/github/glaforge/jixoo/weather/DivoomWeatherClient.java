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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.glaforge.jixoo.api.exception.PixooException;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Public client for Divoom's zero-auth weather proxy service.
 * <p>
 * Proxies OpenWeatherMap real-time data and forecasts without requiring an API key.
 */
public class DivoomWeatherClient {

    private static final String DEFAULT_BASE_URL = "https://wea.divoom-gz.com";

    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public DivoomWeatherClient() {
        this(DEFAULT_BASE_URL, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    }

    public DivoomWeatherClient(HttpClient httpClient) {
        this(DEFAULT_BASE_URL, httpClient);
    }

    public DivoomWeatherClient(String baseUrl, HttpClient httpClient) {
        this.baseUrl = baseUrl;
        this.httpClient = httpClient;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Fetches current weather for the given coordinates using default metric units and English language.
     *
     * @param longitude the longitude
     * @param latitude  the latitude
     * @return CurrentWeather data
     */
    public CurrentWeather getCurrentWeather(double longitude, double latitude) {
        return getCurrentWeather(longitude, latitude, "en", "metric");
    }

    /**
     * Fetches current weather for the given coordinates.
     *
     * @param longitude the longitude
     * @param latitude  the latitude
     * @param lang      language code (e.g. "en", "fr", "de")
     * @param units     units ("metric" or "imperial")
     * @return CurrentWeather data
     */
    public CurrentWeather getCurrentWeather(double longitude, double latitude, String lang, String units) {
        String url = String.format("%s/OpenWeatherMapCache.php?Lang=%s&Units=%s&Query[lon]=%s&Query[lat]=%s",
                baseUrl,
                URLEncoder.encode(lang != null ? lang : "en", StandardCharsets.UTF_8),
                URLEncoder.encode(units != null ? units : "metric", StandardCharsets.UTF_8),
                longitude,
                latitude);
        return executeGet(url, CurrentWeather.class);
    }

    /**
     * Fetches 5-day / 3-hour forecast for the given coordinates using default metric units and English language.
     *
     * @param longitude the longitude
     * @param latitude  the latitude
     * @return WeatherForecast data
     */
    public WeatherForecast getForecast(double longitude, double latitude) {
        return getForecast(longitude, latitude, "en", "metric");
    }

    /**
     * Fetches 5-day / 3-hour forecast for the given coordinates.
     *
     * @param longitude the longitude
     * @param latitude  the latitude
     * @param lang      language code (e.g. "en", "fr", "de")
     * @param units     units ("metric" or "imperial")
     * @return WeatherForecast data
     */
    public WeatherForecast getForecast(double longitude, double latitude, String lang, String units) {
        String url = String.format("%s/OpenWeatherMapForecastCache.php?Lang=%s&Units=%s&Query[lon]=%s&Query[lat]=%s",
                baseUrl,
                URLEncoder.encode(lang != null ? lang : "en", StandardCharsets.UTF_8),
                URLEncoder.encode(units != null ? units : "metric", StandardCharsets.UTF_8),
                longitude,
                latitude);
        return executeGet(url, WeatherForecast.class);
    }

    private <T> T executeGet(String url, Class<T> responseClass) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", "jixoo64/0.3.0")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new PixooException("Weather request failed with HTTP " + response.statusCode() + ": " + response.body());
            }
            return objectMapper.readValue(response.body(), responseClass);
        } catch (IOException e) {
            throw new PixooException("Failed to read weather response from " + url, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PixooException("Weather request interrupted", e);
        }
    }
}
