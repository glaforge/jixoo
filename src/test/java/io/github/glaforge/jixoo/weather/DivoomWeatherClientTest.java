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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DivoomWeatherClientTest {

    private HttpServer server;
    private int port;
    private final List<String> requestedUris = new ArrayList<>();

    private static final String SAMPLE_CURRENT_WEATHER = """
            {
              "coord": {"lon": 2.4, "lat": 48.9},
              "weather": [{"id": 803, "main": "Clouds", "description": "scattered clouds", "icon": "04d"}],
              "base": "stations",
              "main": {
                "temp": 23.65,
                "feels_like": 23.25,
                "temp_min": 22.81,
                "temp_max": 24.41,
                "pressure": 1021,
                "humidity": 45
              },
              "visibility": 10000,
              "wind": {"speed": 4.12, "deg": 260},
              "clouds": {"all": 54},
              "dt": 1789126588,
              "sys": {"country": "FR", "sunrise": 1789104063, "sunset": 1789150410},
              "timezone": 7200,
              "id": 6452001,
              "name": "Paris",
              "cod": 200
            }
            """;

    private static final String SAMPLE_FORECAST = """
            {
              "cod": "200",
              "message": 0,
              "cnt": 2,
              "list": [
                {
                  "dt": 1789128000,
                  "main": {
                    "temp": 22.94,
                    "feels_like": 22.46,
                    "temp_min": 22.94,
                    "temp_max": 23.03,
                    "pressure": 1021,
                    "humidity": 45
                  },
                  "weather": [{"id": 802, "main": "Clouds", "description": "scattered clouds", "icon": "03d"}],
                  "clouds": {"all": 49},
                  "wind": {"speed": 2.68, "deg": 270},
                  "visibility": 10000,
                  "pop": 0.1,
                  "dt_txt": "2026-09-11 12:00:00"
                }
              ],
              "city": {
                "id": 6452001,
                "name": "Paris",
                "coord": {"lat": 48.9, "lon": 2.4},
                "country": "FR",
                "timezone": 7200,
                "sunrise": 1789104063,
                "sunset": 1789150410
              }
            }
            """;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = server.getAddress().getPort();

        server.createContext("/OpenWeatherMapCache.php", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                synchronized (requestedUris) {
                    requestedUris.add(exchange.getRequestURI().toString());
                }
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, SAMPLE_CURRENT_WEATHER.length());
                OutputStream os = exchange.getResponseBody();
                os.write(SAMPLE_CURRENT_WEATHER.getBytes(StandardCharsets.UTF_8));
                os.close();
            }
        });

        server.createContext("/OpenWeatherMapForecastCache.php", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                synchronized (requestedUris) {
                    requestedUris.add(exchange.getRequestURI().toString());
                }
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, SAMPLE_FORECAST.length());
                OutputStream os = exchange.getResponseBody();
                os.write(SAMPLE_FORECAST.getBytes(StandardCharsets.UTF_8));
                os.close();
            }
        });

        server.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void testGetCurrentWeather() {
        DivoomWeatherClient client = new DivoomWeatherClient(
                "http://127.0.0.1:" + port,
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()
        );

        CurrentWeather weather = client.getCurrentWeather(2.3522, 48.8566);
        assertNotNull(weather);
        assertEquals("Paris", weather.name());
        assertEquals(23.65, weather.temperature(), 0.01);
        assertEquals(23.25, weather.feelsLike(), 0.01);
        assertEquals(45, weather.humidity());
        assertEquals("Clouds", weather.condition());
        assertEquals("scattered clouds", weather.description());
        assertEquals("04d", weather.icon());
        assertEquals("FR", weather.sys().country());

        synchronized (requestedUris) {
            assertEquals(1, requestedUris.size());
            assertTrue(requestedUris.get(0).contains("Lang=en"));
            assertTrue(requestedUris.get(0).contains("Units=metric"));
            assertTrue(requestedUris.get(0).contains("lon") && requestedUris.get(0).contains("2.3522"));
            assertTrue(requestedUris.get(0).contains("lat") && requestedUris.get(0).contains("48.8566"));
        }
    }

    @Test
    void testGetForecast() {
        DivoomWeatherClient client = new DivoomWeatherClient(
                "http://127.0.0.1:" + port,
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()
        );

        WeatherForecast forecast = client.getForecast(2.3522, 48.8566, "fr", "metric");
        assertNotNull(forecast);
        assertEquals("200", forecast.cod());
        assertEquals(2, forecast.cnt());
        assertNotNull(forecast.city());
        assertEquals("Paris", forecast.city().name());
        assertEquals(1, forecast.list().size());

        WeatherForecast.ForecastItem item = forecast.list().get(0);
        assertEquals(22.94, item.temperature(), 0.01);
        assertEquals("scattered clouds", item.description());
        assertEquals("03d", item.icon());
        assertEquals("2026-09-11 12:00:00", item.dtTxt());
        assertEquals(0.1, item.pop(), 0.001);

        synchronized (requestedUris) {
            assertEquals(1, requestedUris.size());
            assertTrue(requestedUris.get(0).contains("Lang=fr"));
        }
    }
}
