///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS org.slf4j:slf4j-api:1.7.35
//DEPS org.slf4j:slf4j-simple:1.7.35
//DEPS com.pi4j:pi4j-core:2.3.0
//DEPS com.pi4j:pi4j-plugin-raspberrypi:2.3.0
//DEPS com.pi4j:pi4j-plugin-linuxfs:2.3.0

import com.pi4j.Pi4J;
import com.pi4j.util.Console;
import com.pi4j.io.i2c.I2C;
import com.pi4j.io.i2c.I2CConfig;
import com.pi4j.io.i2c.I2CProvider;

/**
 * Example code to read the temperature, humidity and pressure from a BME280 sensor, on an Adafruit board via I2C and SPI.
 *
 * Based on:
 * 
 * <ul>
 *  <li>https://github.com/Pi4J/pi4j-example-devices/blob/master/src/main/java/com/pi4j/devices/bmp280/README.md</li>
 *  <li>https://www.adafruit.com/product/2652</li>
 *  <li>https://learn.adafruit.com/adafruit-bme280-humidity-barometric-pressure-temperature-sensor-breakout/pinouts</li>
 * </ul>
 * 
 * I2C
 *
 * <ul>
 *  <li>Vin to 3.3V</li>
 *  <li>GND to GND</li>
 *  <li>SCK to I2C clock SCL (pin 5)</li>
 *  <li>SDI to I2C data SDA (pin 3)</li>
 * </ul>
 * 
 * Make sure I2C is enabled on the Raspberry Pi. Use `sudo raspi-config' > Interface Options > I2C.
 * 
 * Check that the sensor is detected on address 0x77 with ``.
 * 
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
public class Pi4JTempHumPress {

    // Wiring see:
    private static final Console console = new Console(); // Pi4J Logger helper

    private static final int I2C_BUS = 0x01;
    private static final int I2C_ADDRESS = 0x77; // When connecting SDO to GND = 0x76

    public static void main(String[] args) throws Exception {

        var pi4j = Pi4J.newAutoContext();

        // Initialize I2C
        console.println("Initializing the sensor via I2C");

        I2CProvider i2CProvider = pi4j.provider("linuxfs-i2c");

        I2CConfig i2cConfig = I2C.newConfigBuilder(pi4j)
                .id("BME280")
                .bus(I2C_BUS)
                .device(I2C_ADDRESS)
                .build();

        try (I2C bme280 = i2CProvider.create(i2cConfig)) {           
            // Read values 10 times
            for (int counter = 0; counter < 10; counter++) {
                resetSensor(bme280);

                // The sensor needs some time to make the measurement
                Thread.sleep(100);

                getTemperature(bme280);
                getHumidity(bme280);
                getPressure(bme280);

                Thread.sleep(1000);
            }
        }

        pi4j.shutdown();
    }

    private static void resetSensor(I2C device) {

    }

    private static void getTemperature(I2C device) {
        double value = 0; // TODO

        console.println("Measure temperature: " + value + "°C");
    }

    private static void getHumidity(I2C device) {
        double value = 0; // TODO
        
        console.println("Humidity: " + value + "%");
    }

    private static void getPressure(I2C device) {
        double value = 0; // TODO
        
        console.println("Pressure: " + value + "Pa");
    }
}