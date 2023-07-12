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

import java.text.DecimalFormat;

/**
 * Example code to read the temperature, humidity and pressure from a BME280 sensor, on an Adafruit board via I2C and SPI.
 *
 * This example can be executed without sudo with:
 * jbang Pi4JTempHumPressI2C.java
 *
 * Based on:
 * 
 * <ul>
 *  <li>https://github.com/Pi4J/pi4j-example-devices/blob/master/src/main/java/com/pi4j/devices/bmp280/README.md</li>
 *  <li>https://www.adafruit.com/product/2652</li>
 *  <li>https://learn.adafruit.com/adafruit-bme280-humidity-barometric-pressure-temperature-sensor-breakout/pinouts</li>
 * </ul>
 * 
 * I2C Wiring
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
public class Pi4JTempHumPressI2C {

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

        // Read values 10 times
        try (I2C bme280 = i2CProvider.create(i2cConfig)) {
            for (int counter = 0; counter < 10; counter++) {
                console.println("**************************************");
                console.println("Reading values, loop " + (counter + 1));

                resetSensor(bme280);

                // The sensor needs some time to make the measurement
                Thread.sleep(100);

                getMeasurements(bme280);

                Thread.sleep(5000);
            }
        }

        pi4j.shutdown();

        console.println("**************************************");
        console.println("Finished");
    }

    private static void resetSensor(I2C device) {
        // Set forced mode to leave sleep ode state and initiate measurements.
        // At measurement completion chip returns to sleep mode
        int ctlReg = device.readRegister(BMP280Declares.ctrl_meas);
        ctlReg |= BMP280Declares.ctl_forced;
        ctlReg &= ~BMP280Declares.tempOverSampleMsk;   // mask off all temperature bits
        ctlReg |= BMP280Declares.ctl_tempSamp1;      // Temperature oversample 1
        ctlReg &= ~BMP280Declares.presOverSampleMsk;   // mask off all pressure bits
        ctlReg |= BMP280Declares.ctl_pressSamp1;   //  Pressure oversample 1

        byte[] regVal = new byte[1];
        regVal[0] = (byte)(BMP280Declares.ctrl_meas);
        byte[] ctlVal = new byte[1];
        ctlVal[0] = (byte) ctlReg;

        device.writeRegister(regVal, ctlVal, ctlVal.length);
    }

    private static void getMeasurements(I2C device) {
        byte[] buff = new byte[6];
        device.readRegister(BMP280Declares.press_msb, buff);
        long adc_T = (long) ((buff[3] & 0xFF) << 12) + (long) ((buff[4] & 0xFF) << 4) + (long) (buff[5] & 0xFF);
        long adc_P = (long) ((buff[0] & 0xFF) << 12) + (long) ((buff[1] & 0xFF) << 4) + (long) (buff[2] & 0xFF);

        byte[] wrtReg = new byte[1];
        wrtReg[0] = (byte) BMP280Declares.reg_dig_t1;

        byte[] compVal = new byte[2];

        DecimalFormat df = new DecimalFormat("0.###");

        // Temperature
        device.readRegister(wrtReg, compVal);
        long dig_t1 = castOffSignInt(compVal);

        device.readRegister(BMP280Declares.reg_dig_t2, compVal);
        int dig_t2 = signedInt(compVal);

        device.readRegister(BMP280Declares.reg_dig_t3, compVal);
        int dig_t3 = signedInt(compVal);

        double var1 = (((double) adc_T) / 16384.0 - ((double) dig_t1) / 1024.0) * ((double) dig_t2);
        double var2 = ((((double) adc_T) / 131072.0 - ((double) dig_t1) / 8192.0) *
                (((double) adc_T) / 131072.0 - ((double) dig_t1) / 8192.0)) * ((double) dig_t3);
        double t_fine = (int) (var1 + var2);
        double temperature = (var1 + var2) / 5120.0;

        console.println("Measure temperature: " + df.format(temperature) + "°C");

        // Pressure
        device.readRegister(BMP280Declares.reg_dig_p1, compVal);
        long dig_p1 = castOffSignInt(compVal);

        device.readRegister(BMP280Declares.reg_dig_p2, compVal);
        int dig_p2 = signedInt(compVal);

        device.readRegister(BMP280Declares.reg_dig_p3, compVal);
        int dig_p3 = signedInt(compVal);

        device.readRegister(BMP280Declares.reg_dig_p4, compVal);
        int dig_p4 = signedInt(compVal);

        device.readRegister(BMP280Declares.reg_dig_p5, compVal);
        int dig_p5 = signedInt(compVal);

        device.readRegister(BMP280Declares.reg_dig_p6, compVal);
        int dig_p6 = signedInt(compVal);

        device.readRegister(BMP280Declares.reg_dig_p7, compVal);
        int dig_p7 = signedInt(compVal);

        device.readRegister(BMP280Declares.reg_dig_p8, compVal);
        int dig_p8 = signedInt(compVal);

        device.readRegister(BMP280Declares.reg_dig_p9, compVal);
        int dig_p9 = signedInt(compVal);
        
        var1 = ((double) t_fine / 2.0) - 64000.0;
        var2 = var1 * var1 * ((double) dig_p6) / 32768.0;
        var2 = var2 + var1 * ((double) dig_p5) * 2.0;
        var2 = (var2 / 4.0) + (((double) dig_p4) * 65536.0);
        var1 = (((double) dig_p3) * var1 * var1 / 524288.0 + ((double) dig_p2) * var1) / 524288.0;
        var1 = (1.0 + var1 / 32768.0) * ((double) dig_p1);
        double pressure = 0;
        if (var1 != 0.0) {
            // avoid exception caused by division by zero
            pressure = 1048576.0 - (double) adc_P;
            pressure = (pressure - (var2 / 4096.0)) * 6250.0 / var1;
            var1 = ((double) dig_p9) * pressure * pressure / 2147483648.0;
            var2 = pressure * ((double) dig_p8) / 32768.0;
            pressure = pressure + (var1 + var2 + ((double) dig_p7)) / 16.0;
        }
        
        console.println("Pressure: " + df.format(pressure) + " Pa");
        // 1 Pa = 0.00001 bar or 1 bar = 100,000 Pa
        console.println("Pressure: " + df.format(pressure / 100_000) + " bar");
        // 1 Pa = 0.0000098692316931 atmosphere (standard) and 1 atm = 101.325 kPa
        console.println("Pressure: " + df.format(pressure / 101_325) + " atm");

        // Humidity
        double humidity = 0; // TODO
        console.println("Humidity: " + humidity + "%");
    }

    /**
     * @param read 8 bits data
     * @return unsigned value
     */
    private static int castOffSignByte(byte read) {
        return ((int) read & 0Xff);
    }

    /**
     * @param read 16 bits of data  stored in 8 bit array
     * @return 16 bit signed
     */
    private static int signedInt(byte[] read) {
        int temp = 0;
        temp = (read[0] & 0xff);
        temp += (((long) read[1]) << 8);
        return (temp);
    }

    /**
     * @param read 16 bits of data  stored in 8 bit array
     * @return 64 bit unsigned value
     */
    private static long castOffSignInt(byte[] read) {
        long temp = 0;
        temp = ((long) read[0] & 0xff);
        temp += (((long) read[1] & 0xff)) << 8;
        return (temp);
    }

    private static class BMP280Declares {
        /*  Begin device register definitions.        */
        static int temp_xlsb = 0xFC;
        static int temp_lsb = 0xFB;
        static int temp_msb = 0xFA;
        static int press_xlsb = 0xF9;
        static int press_lsb = 0xF8;
        static int press_msb = 0xF7;
        static int config = 0xF5;
        static int ctrl_meas = 0xF4;
        static int status = 0xF3;
        static int reset = 0xE0;
        static int chipId = 0xD0;


        // errata register definitions
        static int reg_dig_t1 = 0x88;
        static int reg_dig_t2 = 0x8A;
        static int reg_dig_t3 = 0x8C;

        static int reg_dig_p1 = 0x8E;
        static int reg_dig_p2 = 0x90;
        static int reg_dig_p3 = 0x92;
        static int reg_dig_p4 = 0x94;
        static int reg_dig_p5 = 0x96;
        static int reg_dig_p6 = 0x98;
        static int reg_dig_p7 = 0x9A;
        static int reg_dig_p8 = 0x9C;
        static int reg_dig_p9 = 0x9E;

        // register contents
        static int reset_cmd = 0xB6;  // written to reset

        // Pertaining to 0xF3 status register
        static int stat_measure = 0x08;  // set, conversion running
        static int stat_update = 0x01;  // set, NVM being copied

        // Pertaining to 0xF4 ctrl_meas register
        static int tempOverSampleMsk = 0xE0;  // mask bits 5,6,7
        static int presOverSampleMsk = 0x1C;  // mask bits 2,3,4
        static int pwrModeMsk = 0x03;  // mask bits 0,1


        // Pertaining to 0xF5 config register
        static int inactDurationMsk = 0xE0;  // mask bits 5,6,7
        static int iirFltMsk = 0x1C;  // mask bits 2,3,4
        static int enableSpiMsk = 0x01;  // mask bits 0

        // Pertaining to 0xF7 0xF8 0xF9 press  register
        static int pressMsbMsk = 0xFF;  // mask bits 0 - 7
        static int pressLsbMsk = 0xFF;  // mask bits 0 - 7
        static int pressXlsbMsk = 0x0F;  // mask bits 0 - 3

        // Pertaining to 0xFA 0xFB 0xFC temp  register
        static int tempMsbMsk = 0xFF;  // mask bits 0 - 7
        static int tempLsbMsk = 0xFF;  // mask bits 0 - 7
        static int tempXlsbMsk = 0x0F;  // mask bits 0 - 3
        static int idValueMsk = 0x58;   // expected chpId value

        // For the control reg 0xf4
        static int ctl_forced = 0x01;
        static int ctl_tempSamp1 = 0x20;   // oversample *1
        static int ctl_pressSamp1 = 0x04;   // oversample *1
    }
}