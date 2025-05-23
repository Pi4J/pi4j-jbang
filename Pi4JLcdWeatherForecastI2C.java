///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS org.slf4j:slf4j-api:2.0.12
//DEPS org.slf4j:slf4j-simple:2.0.12
//DEPS com.pi4j:pi4j-core:3.0.1
//DEPS com.pi4j:pi4j-plugin-raspberrypi:3.0.1
//DEPS com.pi4j:pi4j-plugin-linuxfs:3.0.1
//DEPS com.fasterxml.jackson.core:jackson-databind:2.13.4.1

//SOURCES helper/lcd/Component.java
//SOURCES helper/lcd/I2CDevice.java
//SOURCES helper/lcd/LcdDisplay.java

import com.pi4j.Pi4J;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import helper.lcd.LcdDisplay;

/**
 * This example gets the weather forecast from https://open-meteo.com/en/docs
 * Free for non-commercial use and less than 10.000 daily API calls.
 *
 * jbang Pi4JLcdWeatherForecast.java
 */
public class Pi4JLcdWeatherForecastI2C {

    private final static int WAIT_BETWEEN_MESSAGES = 3_000;

    public static void main(String[] args) throws Exception {

        // Initialize the Pi4J context
        var pi4j = Pi4J.newAutoContext();

        // Initialize the LCD
        LcdDisplay lcdDisplay = new LcdDisplay(pi4j, 2, 16);

        // Get the weather forecast as JSON String
        var forecastContent = getForecast(52.52, 13.41);

        // Convert to Java object
        var forecast = convertForecast(forecastContent);

        if (forecast == null) {
            System.err.println("Can't show the forecast, the object is null...");
        } else {
            System.out.println("Forecast received for " + forecast.dailyForecast.date[0]);

            // Show the data of the received forecast 10 times
            for (int i = 0; i < 10; i++) {
                showDate(lcdDisplay, forecast);
                Thread.sleep(WAIT_BETWEEN_MESSAGES);
                showCurrentWeather(lcdDisplay, forecast);
                Thread.sleep(WAIT_BETWEEN_MESSAGES);
                showSunInfo(lcdDisplay, forecast);
                Thread.sleep(WAIT_BETWEEN_MESSAGES);
            }
        }

        // Shutdown the Pi4J context
        pi4j.shutdown();
    }

    public static String getForecast(Double latitude, Double longitude) {
        StringBuilder rt = new StringBuilder();

        try {
            URL url = new URL("https://api.open-meteo.com/v1/forecast"
                    + "?latitude=" + latitude + "&longitude=" + longitude
                    + "&daily=weather_code,apparent_temperature_max,sunset,rain_sum,wind_gusts_10m_max,temperature_2m_max,apparent_temperature_min,daylight_duration,temperature_2m_min,sunrise,sunshine_duration,wind_speed_10m_max&forecast_days=1");

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                String readLine;
                while ((readLine = in.readLine()) != null) {
                    rt.append(readLine);
                }
                in.close();
            } else {
                System.err.println("Wrong response code: " + responseCode);
            }
        } catch (Exception ex) {
            System.err.println("Request error: " + ex.getMessage());
        }

        var data = rt.toString();
        System.out.println("JSON received: " + data);
        return data;
    }

    private static Forecast convertForecast(String content) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(
                    DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                    false);
            return mapper.readValue(content, Forecast.class);
        } catch (IOException ex) {
            System.err.println("Unable to parse the given string to a Forecast object: " + ex.getMessage());
            return null;
        }
    }

    private static void showDate(LcdDisplay lcd, Forecast forecast) {
        System.out.println("Showing date");
        lcd.clearDisplay();
        lcd.displayLineOfText("Weather for", 0);
        lcd.displayLineOfText(forecast.dailyForecast.date[0], 1);
    }

    private static void showCurrentWeather(LcdDisplay lcd, Forecast forecast) {
        System.out.println("Showing current weather");
        var text = getWmoDescription(forecast.dailyForecast.weatherCode[0]);
        lcd.clearDisplay();
        if (text.length() > 16) {
            lcd.displayLineOfText(text.substring(0, 15), 0);
            lcd.displayLineOfText(text.substring(15), 1);
        } else {
            lcd.displayLineOfText(text, 0);
            lcd.displayLineOfText("", 1);
        }
    }

    private static void showSunInfo(LcdDisplay lcd, Forecast forecast) {
        System.out.println("Showing sun duration");
        var seconds = forecast.dailyForecast.sunshineDurationInSeconds[0];
        var hours = (seconds * 1.0) / 60 / 60;
        String roundedToTwoNumbers = String.format("%.2f", hours);
        lcd.clearDisplay();
        lcd.displayLineOfText("Hours sun: " + roundedToTwoNumbers, 0);
        lcd.displayLineOfText(getTimeFromTimestamp(forecast.dailyForecast.sunrise[0])
                + " till "
                + getTimeFromTimestamp(forecast.dailyForecast.sunset[0]), 1);
    }

    private static String getTimeFromTimestamp(String timestamp) {
        if (timestamp.contains("T")) {
            return timestamp.substring(timestamp.indexOf("T") + 1);
        }
        return timestamp;
    }

    private record Forecast(
        @JsonProperty("timezone")
        String timezone,

        @JsonProperty("elevation")
        Float elevation,

        @JsonProperty("daily")
        DailyForecast dailyForecast) {

        /*
        {
          "latitude": 52.52,
          "longitude": 13.419998,
          "generationtime_ms": 0.14412403106689453,
          "utc_offset_seconds": 0,
          "timezone": "GMT",
          "timezone_abbreviation": "GMT",
          "elevation": 38.0,
          "daily_units": {
            "time": "iso8601",
            "weather_code": "wmo code",
            "apparent_temperature_max": "°C",
            "sunset": "iso8601",
            "rain_sum": "mm",
            "wind_gusts_10m_max": "km/h",
            "temperature_2m_max": "°C",
            "apparent_temperature_min": "°C",
            "daylight_duration": "s",
            "temperature_2m_min": "°C",
            "sunrise": "iso8601",
            "sunshine_duration": "s",
            "wind_speed_10m_max": "km/h"
          },
          "daily": {
            ...
          }
        }
        */
    }

    private record DailyForecast (
        @JsonProperty("time")
        String[] date,

        @JsonProperty("weather_code")
        Integer[] weatherCode,

        @JsonProperty("apparent_temperature_max")
        Double[] tempMax,

        @JsonProperty("apparent_temperature_min")
        Double[] tempMin,

        @JsonProperty("sunrise")
        String[] sunrise,

        @JsonProperty("sunset")
        String[] sunset,

        @JsonProperty("sunshine_duration")
        Long[] sunshineDurationInSeconds) {

        /*
        "time": [
          "2025-03-14"
        ],
        "weather_code": [
          3
        ],
        "apparent_temperature_max": [
          5.4
        ],
        "sunset": [
          "2025-03-14T17:08"
        ],
        "rain_sum": [
          0.00
        ],
        "wind_gusts_10m_max": [
          25.2
        ],
        "temperature_2m_max": [
          8.5
        ],
        "apparent_temperature_min": [
          0.0
        ],
        "daylight_duration": [
          42396.27
        ],
        "temperature_2m_min": [
          3.1
        ],
        "sunrise": [
          "2025-03-14T05:22"
        ],
        "sunshine_duration": [
          25877.89
        ],
        "wind_speed_10m_max": [
          11.3
        ]
        */
    }

    private static String getWmoDescription(int code) {
        return switch (code) {
            case 0 -> "Clear sky";
            case 1 -> "Mainly clear";
            case 2 -> "Partly cloudy";
            case 3 -> "Overcast";
            case 45 -> "Fog";
            case 48 -> "Fog and depositing rime fog";
            case 51 -> "Light drizzle";
            case 53 -> "Moderate drizzle";
            case 55 -> "Dense intensity drizzle";
            case 56 -> "Freezing light rizzle";
            case 57 -> "Freezing dense rizzle";
            case 61 -> "Slight rain";
            case 63 -> "Moderate rain";
            case 65 -> "Heavy intensity rain";
            case 66 -> "Freezing light rain";
            case 67 -> "Freezing heavy rain";
            case 71 -> "Slight snow fall";
            case 73 -> "Moderate snow fall";
            case 75 -> "Heavy snow fall";
            case 77 -> "Snow grains";
            case 80 -> "Slight rain showers";
            case 81 -> "Moderate rain showers";
            case 82 -> "Violent rain showers";
            case 85 -> "Slight snow showers";
            case 86 -> "Heavy snow showers";
            case 95 -> "Thunderstorm";
            case 96 -> "Thunderstorm with slight hail";
            case 99 -> "Thunderstorm with heavy hail";
            default -> "Unknown";
        };
    }
}