/// usr/bin/env jbang "$0" "$@" ; exit $?

/**
 * This example uses the simplifed main method, which is available since Java 25.
 * More info about using specific Java versions with JBang is documented on
 * https://www.jbang.dev/documentation/guide/latest/javaversions.html
 */
// JAVA 25

//DEPS org.slf4j:slf4j-api:2.0.17
//DEPS org.slf4j:slf4j-simple:2.0.17
//DEPS com.pi4j:pi4j-core:4.0.0
//DEPS com.pi4j:pi4j-plugin-ffm:4.0.0

import com.pi4j.Pi4J;
import com.pi4j.io.i2c.I2C;
import com.pi4j.io.i2c.I2CImplementation;
import com.pi4j.util.Console;

/**
 * Example code to read the temperature, humidity and pressure from a DHT20 sensor, on an Elecrow CrowPi 3.
 * The sensor is connected on I2C address 0x38
 * <p>
 * From the terminal, in the `i2c` directory, start this example with:
 * <code>jbang CrowPi3DHT20.java</code>
 *
 * <p>
 * Check that the sensor is detected on address 0x77 with `i2cdetect -y 1`.0
 *
 * <p>
 * $ i2cdetect -y 1
 * 0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f
 * 00:                         -- -- -- -- -- -- -- --
 * 10: -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
 * 20: -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
 * 30: -- -- -- -- -- -- -- -- 38 -- -- -- -- -- -- --
 * 40: -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
 * 50: -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
 * 60: -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
 * 70: -- -- -- -- -- -- -- --
 *
 */
private static final Console console = new Console(); // Pi4J Logger

private static final int I2C_BUS = 0x01;
private static final int I2C_DEVICE = 0x38;

public static void main(String[] args) {
    try {
        var pi4j = Pi4J.newAutoContext();

        console.println("Initializing the DHT20 sensor via I2C");

        var i2cConfig = I2C.newConfigBuilder(pi4j)
                .bus(I2C_BUS)
                .device(I2C_DEVICE)
                .i2cImplementation(I2CImplementation.DIRECT)
                .build();
        var i2c = pi4j.create(i2cConfig);

        // Check initialization status
        Thread.sleep(500);
        byte[] statusData = new byte[1];
        i2c.readRegister(0x71, statusData, 0, 1);
        console.println("Read: " + statusData[0]);
        if ((statusData[0] | 0x08) == 0) {
            console.println("Initialization error");
            //return;
        }

        for (int counter = 0; counter < 10; counter++) {
            console.println("**************************************");
            console.println("Reading values, loop " + (counter + 1));

            // Trigger measurement
            i2c.writeRegister(0xAC, new byte[]{0x33, 0x00});
            Thread.sleep(100);

            // Read measurement data
            byte[] data = new byte[7];
            i2c.readRegister(0x71, data, 0, 7);

            // Parse temperature
            int tRaw = ((data[3] & 0x0F) << 16) | ((data[4] & 0xFF) << 8) | (data[5] & 0xFF);
            double temperature = (200.0 * tRaw / Math.pow(2, 20)) - 50.0;

            // Parse humidity
            int hRaw = ((data[3] & 0xF0) >> 4) | ((data[1] & 0xFF) << 12) | ((data[2] & 0xFF) << 4);
            double humidity = 100.0 * hRaw / Math.pow(2, 20);

            System.out.printf("Temperature: %.2f °C%n", temperature);
            System.out.printf("Humidity: %.2f %%%n", humidity);

            Thread.sleep(2_000);
        }

        i2c.close();
        pi4j.shutdown();
    } catch (Exception e) {
        console.println("Error: " + e.getMessage());
    } finally {
        console.println("**************************************");
        console.println("Finished");
    }
}