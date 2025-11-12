/// usr/bin/env jbang "$0" "$@" ; exit $?
//REPOS mavencentral,mavensnapshot=https://central.sonatype.com/repository/maven-snapshots/

//DEPS org.slf4j:slf4j-api:2.0.17
//DEPS org.slf4j:slf4j-simple:2.0.17
//DEPS com.pi4j:pi4j-core:4.0.0-SNAPSHOT
//DEPS com.pi4j:pi4j-plugin-ffm:4.0.0-SNAPSHOT
//DEPS com.pi4j:pi4j-drivers:0.0.1-SNAPSHOT

import com.pi4j.Pi4J;
import com.pi4j.drivers.sensor.environment.bmx280.Bmx280Driver;
import com.pi4j.io.i2c.I2CConfigBuilder;
import com.pi4j.util.Console;

/**
 * Example code to read the temperature, humidity and pressure from a BME280 sensor, on an Adafruit board via I2C and SPI.
 * <p>
 * From the terminal, in the `i2c` directory, start this example with:
 * <code>jbang TemperatureHumidityPressure.java</code>
 * <p>
 * Based on:
 *
 * <ul>
 *  <li>https://github.com/Pi4J/pi4j-example-devices/blob/master/src/main/java/com/pi4j/devices/bmp280/README.md</li>
 *  <li>https://www.adafruit.com/product/2652</li>
 *  <li>https://learn.adafruit.com/adafruit-bme280-humidity-barometric-pressure-temperature-sensor-breakout/pinouts</li>
 * </ul>
 * <p>
 * I2C Wiring
 *
 * <ul>
 *  <li>Vin to 3.3V</li>
 *  <li>GND to GND</li>
 *  <li>SDI to I2C data SDA (pin 3)</li>
 *  <li>SCK to I2C clock SCL (pin 5)</li>
 *  <li>CS to 3.3V</li>
 * </ul>
 *
 * <p>
 * Make sure I2C is enabled on the Raspberry Pi. Use `sudo raspi-config' > Interface Options > I2C.
 * <p>
 * Check that the sensor is detected on address 0x77 with `i2cdetect -y 1`.0
 *
 * <p>
 * $ i2cdetect -y 1
 *      0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f
 * 00:                         -- -- -- -- -- -- -- --
 * 10: -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
 * 20: -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
 * 30: -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
 * 40: -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
 * 50: -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
 * 60: -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
 * 70: -- -- -- -- -- -- -- 77
 *
 */
public class TemperatureHumidityPressure {

    private static final Console console = new Console(); // Pi4J Logger

    private static final int I2C_BUS = 0x01;
    private static final int I2C_DEVICE = 0x77;

    public static void main(String[] args) {
        try {
            var pi4j = Pi4J.newAutoContext();

            console.println("Initializing the sensor via I2C");

            var i2c = pi4j.create(I2CConfigBuilder.newInstance(pi4j).bus(I2C_BUS).device(I2C_DEVICE));
            var sensor = new Bmx280Driver(i2c);

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