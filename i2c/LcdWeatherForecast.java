/// usr/bin/env jbang "$0" "$@" ; exit $?

/**
 * This example uses the simplifed main method, which is available since Java 25.
 * More info about using specific Java versions with JBang is documented on
 * https://www.jbang.dev/documentation/guide/latest/javaversions.html
 */
// JAVA 25

//REPOS mavencentral,mavensnapshot=https://central.sonatype.com/repository/mavens/

//DEPS org.slf4j:slf4j-api:2.0.17
//DEPS org.slf4j:slf4j-simple:2.0.17
//DEPS com.pi4j:pi4j-core:4.0.0
//DEPS com.pi4j:pi4j-plugin-ffm:4.0.0
//DEPS com.pi4j:pi4j-drivers:0.0.1-SNAPSHOT
//DEPS com.fasterxml.jackson.core:jackson-databind:2.13.4.1

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pi4j.Pi4J;
import com.pi4j.drivers.display.character.hd44780.Hd44780Driver;
import com.pi4j.io.i2c.I2C;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * This example gets the weather forecast from https://open-meteo.com/en/docs
 * Free for non-commercial use and less than 10.000 daily API calls. It displays the forecast on an LCD display.
 * Use an LCD display with an I2C interfact, because that's a lot easier to use, compared to all the wires needed
 * for direct control of an 1602A display.
 *
 * <p>
 * From the terminal, in the `spi` directory, start this example with:
 * <code>jbang i2c.LcdWeatherForecast.java</code>
 *
 * <p>
 * I2C Wiring for a 16*2 1602A LCD display with I2C interface:
 *
 * <ul>
 *  <li>Vin to 5V</li>
 *  <li>GND to GND</li>
 *  <li>SDA to I2C data SDA (pin 3)</li>
 *  <li>SCL to I2C clock SCL (pin 5)</li>
 * </ul>
 *
 * <p>
 * Make sure I2C is enabled on the Raspberry Pi. Use `sudo raspi-config' > Interface Options > I2C.
 *
 * <p>
 * Check that the LCD display is detected on address 0x27 with `i2cdetect -y 1`.
 * In case your LCD is discovered on a different address, change the I2C address in the code.
 *
 * <p>
 * $ i2cdetect -y 1
 *      0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f
 * 00:                         -- -- -- -- -- -- -- --
 * 10: -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
 * 20: -- -- -- -- -- -- -- 27 -- -- -- -- -- -- -- --
 * 30: -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
 * 40: -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
 * 50: -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
 * 60: -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
 * 70: -- -- -- -- -- -- -- --
 */
private final static int WAIT_BETWEEN_MESSAGES = 3_000;
private static Hd44780Driver lcdDisplay;

void main() throws Exception {
    // Initialize the Pi4J context
    var pi4j = Pi4J.newAutoContext();

    // Initialize the LCD
    var i2c = pi4j.create(I2C.newConfigBuilder(pi4j)
            .bus(0x1)
            .device(0x27)
            .build());

    lcdDisplay = Hd44780Driver.withPcf8574Connection(i2c, 16, 2);

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
            showDate(forecast);
            Thread.sleep(WAIT_BETWEEN_MESSAGES);
            showCurrentWeather(forecast);
            Thread.sleep(WAIT_BETWEEN_MESSAGES);
            showSunInfo(forecast);
            Thread.sleep(WAIT_BETWEEN_MESSAGES);
        }
    }

    // Shutdown the Pi4J context
    pi4j.shutdown();
}

public static String getForecast(Double latitude, Double longitude) {
    var rt = new StringBuilder();

    try {
        var url = new URL("https://api.open-meteo.com/v1/forecast"
                + "?latitude=" + latitude + "&longitude=" + longitude
                + "&daily=weather_code,apparent_temperature_max,sunset,rain_sum,wind_gusts_10m_max,temperature_2m_max,apparent_temperature_min,daylight_duration,temperature_2m_min,sunrise,sunshine_duration,wind_speed_10m_max&forecast_days=1");

        var conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        var responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            var in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
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
        var mapper = new ObjectMapper();
        mapper.configure(
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                false);
        return mapper.readValue(content, Forecast.class);
    } catch (IOException ex) {
        System.err.println("Unable to parse the given string to a Forecast object: " + ex.getMessage());
        return null;
    }
}

private static void showDate(Forecast forecast) {
    System.out.println("Showing date");
    lcdDisplay.clear();
    lcdDisplay.writeAt(0, 0, "Weather for");
    lcdDisplay.writeAt(0, 1, forecast.dailyForecast.date[0]);
}

private static void showCurrentWeather(Forecast forecast) {
    System.out.println("Showing current weather");
    var text = getWmoDescription(forecast.dailyForecast.weatherCode[0]);
    lcdDisplay.clear();
    if (text.length() > 16) {
        lcdDisplay.writeAt(0, 0, text.substring(0, 15));
        lcdDisplay.writeAt(0, 1, text.substring(15));
    } else {
        lcdDisplay.writeAt(0, 0, text);
        lcdDisplay.writeAt(0, 1, "");
    }
}

private static void showSunInfo(Forecast forecast) {
    System.out.println("Showing sun duration");
    var seconds = forecast.dailyForecast.sunshineDurationInSeconds[0];
    var hours = (seconds * 1.0) / 60 / 60;
    var roundedToTwoNumbers = String.format("%.2f", hours);
    lcdDisplay.clear();
    lcdDisplay.writeAt(0, 0, "Hours sun: " + roundedToTwoNumbers);
    lcdDisplay.writeAt(0, 1, getTimeFromTimestamp(forecast.dailyForecast.sunrise[0])
            + " till "
            + getTimeFromTimestamp(forecast.dailyForecast.sunset[0]));
}

private static String getTimeFromTimestamp(String timestamp) {
    if (timestamp.contains("T")) {
        return timestamp.substring(timestamp.indexOf("T") + 1);
    }
    return timestamp;
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

private record DailyForecast(
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