/// usr/bin/env jbang "$0" "$@" ; exit $?
//REPOS mavencentral,mavensnapshot=https://central.sonatype.com/repository/maven-snapshots/

//DEPS org.slf4j:slf4j-api:2.0.17
//DEPS org.slf4j:slf4j-simple:2.0.17
//DEPS com.pi4j:pi4j-core:4.0.0-SNAPSHOT
//DEPS com.pi4j:pi4j-plugin-ffm:4.0.0-SNAPSHOT
//DEPS com.pi4j:pi4j-drivers:0.0.1-SNAPSHOT

import com.pi4j.Pi4J;
import com.pi4j.drivers.sensor.environment.bmx280.Bmx280Driver;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.gpio.digital.DigitalState;
import com.pi4j.io.spi.*;
import com.pi4j.util.Console;

/**
 * Example code to read the temperature, humidity and pressure from a BME280 sensor, on an Adafruit board via I2C and SPI.
 * Make sure to follow the README of this project to learn more about JBang and how to install it.
 * <p>
 * From the terminal, in the `spi` directory, start this example with:
 * <code>jbang TemperatureHumidityPressure.java</code>
 * <p>
 * Based on:
 * <ul>
 *  <li>https://github.com/Pi4J/pi4j-example-devices/blob/master/src/main/java/com/pi4j/devices/bmp280/README.md</li>
 *  <li>https://www.adafruit.com/product/2652</li>
 *  <li>https://learn.adafruit.com/adafruit-bme280-humidity-barometric-pressure-temperature-sensor-breakout/pinouts</li>
 * </ul>
 * <p>
 * SPI Wiring
 * <ul>
 *  <li>Vin to 3.3V</li>
 *  <li>GND to GND</li>
 *  <li>SDI to MOSI (BCM10, pin 19)</li>
 *  <li>SDO to MISO (BCM9, pin 21)</li>
 *  <li>SCK to SCLK (BCM11, pin 23)</li>
 *  <li>CS to BCM21 (pin 40)</li>
 * </ul>
 *
 */
public class TemperatureHumidityPressure {

    static final int SPI_BUS = 0;
    static final int SPI_CSB = 21;
    private static final Console console = new Console(); // Pi4J Logger

    public static void main(String[] args) throws Exception {
        try {
            var pi4j = Pi4J.newAutoContext();

            console.println("Initializing the sensor via I2C");

            var csb = pi4j.create(DigitalOutput.newConfigBuilder(pi4j)
                    .bcm(SPI_CSB)
                    .initial(DigitalState.HIGH)
                    .shutdown(DigitalState.HIGH)
                    .build());
            var spi = pi4j.create(SpiConfigBuilder.newInstance(pi4j)
                    .channel(0)
                    .bus(SPI_BUS)
                    .mode(SpiMode.MODE_0)
                    .baud(Spi.DEFAULT_BAUD)
                    .build());
            var sensor = new Bmx280Driver(spi, csb);

            for (int counter = 0; counter < 10; counter++) {
                console.println("**************************************");
                console.println("Reading values, loop " + (counter + 1));

                var measurement = sensor.readMeasurement();
                console.println("Humidity : " + measurement.getHumidity());
                console.println("Pressure : " + measurement.getPressure());
                console.println("Temperature : " + measurement.getTemperature());

                Thread.sleep(2_000);
            }

            sensor.close();
            pi4j.shutdown();
        } catch (Exception e) {
            console.println("Error: " + e.getMessage());
        } finally {
            console.println("**************************************");
            console.println("Finished");
        }
    }
}