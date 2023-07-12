///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS org.slf4j:slf4j-api:1.7.35
//DEPS org.slf4j:slf4j-simple:1.7.35
//DEPS com.pi4j:pi4j-core:2.3.0
//DEPS com.pi4j:pi4j-plugin-raspberrypi:2.3.0
//DEPS com.pi4j:pi4j-plugin-linuxfs:2.3.0

import com.pi4j.Pi4J;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.gpio.digital.DigitalState;
import com.pi4j.io.spi.*;
import com.pi4j.util.Console;

import java.text.DecimalFormat;

/**
 * Example code to read the temperature, humidity and pressure from a BME280 sensor, on an Adafruit board via I2C and SPI.
 *
 * This example can be executed without sudo with:
 * jbang Pi4JTempHumPressSpi.java
 *
 * Based on:
 * 
 * <ul>
 *  <li>https://github.com/Pi4J/pi4j-example-devices/blob/master/src/main/java/com/pi4j/devices/bmp280/README.md</li>
 *  <li>https://www.adafruit.com/product/2652</li>
 *  <li>https://learn.adafruit.com/adafruit-bme280-humidity-barometric-pressure-temperature-sensor-breakout/pinouts</li>
 * </ul>
 * 
 * SPI Wiring
 *
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
public class Pi4JTempHumPressSpi {

    private static final Console console = new Console(); // Pi4J Logger helper

    private static final String SPI_PROVIDER_NAME = "BME280 SPI Provider";
    private static final String SPI_PROVIDER_ID = "BME280-spi";
    private static DigitalOutput csGpio;
    private static Spi spi;
    public static void main(String[] args) throws Exception {

        var pi4j = Pi4J.newAutoContext();

        // Initialize SPI
        console.println("Initializing the sensor via SPI");

        SpiChipSelect chipSelect = SpiChipSelect.CS_0;
        SpiBus spiBus = SpiBus.BUS_0;
        int csPin = 21;

        var csGpioConfig = DigitalOutput.newConfigBuilder(pi4j)
                .id("CS_pin")
                .name("CS")
                .address(csPin)
                .shutdown(DigitalState.HIGH)
                .initial(DigitalState.HIGH)
                .provider("pigpio-digital-output");
        csGpio = pi4j.create(csGpioConfig);

        var spiConfig = Spi.newConfigBuilder(pi4j)
                .id("SPI" + spiBus + "_BMP280")
                .name("D/A converter")
                .bus(spiBus)
                .chipSelect(chipSelect)
                //1 20 19 18 17 16 15 14 13 12 11 10  9  8  7  6  5  4  3  2  1  0
                //b  b  b  b  b  b  R  T  n  n  n  n  W  A u2 u1 u0 p2 p1 p0  m  m
                // .flags(0b0000000000000000100000L)  // MODE0, ux GPIO not used for chip select
                .baud(Spi.DEFAULT_BAUD)    // Max 10MHz
                .mode(SpiMode.MODE_0)
                .provider("pigpio-spi")
                .build();
        spi = pi4j.create(spiConfig);

        // Read values 10 times
        for (int counter = 0; counter < 10; counter++) {
            console.println("**************************************");
            console.println("Reading values, loop " + (counter + 1));

            resetSensor();

            // The sensor needs some time to make the measurement
            Thread.sleep(100);

            getMeasurements();

            Thread.sleep(5000);
        }

        pi4j.shutdown();

        console.println("**************************************");
        console.println("Finished");
    }

    private static void resetSensor() {
        // Set forced mode to leave sleep ode state and initiate measurements.
        // At measurement completion chip returns to sleep mode
        int ctlReg = readRegister(BMP280Declares.ctrl_meas);
        ctlReg |= BMP280Declares.ctl_forced;
        ctlReg &= ~BMP280Declares.tempOverSampleMsk;   // mask off all temperature bits
        ctlReg |= BMP280Declares.ctl_tempSamp1;      // Temperature oversample 1
        ctlReg &= ~BMP280Declares.presOverSampleMsk;   // mask off all pressure bits
        ctlReg |= BMP280Declares.ctl_pressSamp1;   //  Pressure oversample 1

        byte[] regVal = new byte[1];
        regVal[0] = (byte)(BMP280Declares.ctrl_meas);
        byte[] ctlVal = new byte[1];
        ctlVal[0] = (byte) ctlReg;

        writeRegister(BMP280Declares.ctrl_meas,ctlVal[0]);
    }

    private static void getMeasurements() {
        byte[] buff = new byte[6];
        readRegister(BMP280Declares.press_msb, buff);
        long adc_T = (long) ((buff[3] & 0xFF) << 12) + (long) ((buff[4] & 0xFF) << 4) + (long) (buff[5] & 0xFF);
        long adc_P = (long) ((buff[0] & 0xFF) << 12) + (long) ((buff[1] & 0xFF) << 4) + (long) (buff[2] & 0xFF);

        byte[] compVal = new byte[2];

        DecimalFormat df = new DecimalFormat("0.###");

        // Temperature
        readRegister(BMP280Declares.reg_dig_t1, compVal);
        long dig_t1 = castOffSignInt(compVal);

        readRegister(BMP280Declares.reg_dig_t2, compVal);
        int dig_t2 = signedInt(compVal);

        readRegister(BMP280Declares.reg_dig_t3, compVal);
        int dig_t3 = signedInt(compVal);

        double var1 = (((double) adc_T) / 16384.0 - ((double) dig_t1) / 1024.0) * ((double) dig_t2);
        double var2 = ((((double) adc_T) / 131072.0 - ((double) dig_t1) / 8192.0) *
                (((double) adc_T) / 131072.0 - ((double) dig_t1) / 8192.0)) * ((double) dig_t3);
        double t_fine = (int) (var1 + var2);
        double temperature = (var1 + var2) / 5120.0;

        console.println("Measure temperature: " + df.format(temperature) + "°C");

        // Pressure
        readRegister(BMP280Declares.reg_dig_p1, compVal);
        long dig_p1 = castOffSignInt(compVal);

        readRegister(BMP280Declares.reg_dig_p2, compVal);
        int dig_p2 = signedInt(compVal);

        readRegister(BMP280Declares.reg_dig_p3, compVal);
        int dig_p3 = signedInt(compVal);

        readRegister(BMP280Declares.reg_dig_p4, compVal);
        int dig_p4 = signedInt(compVal);

        readRegister(BMP280Declares.reg_dig_p5, compVal);
        int dig_p5 = signedInt(compVal);

        readRegister(BMP280Declares.reg_dig_p6, compVal);
        int dig_p6 = signedInt(compVal);

        readRegister(BMP280Declares.reg_dig_p7, compVal);
        int dig_p7 = signedInt(compVal);

        readRegister(BMP280Declares.reg_dig_p8, compVal);
        int dig_p8 = signedInt(compVal);

        readRegister(BMP280Declares.reg_dig_p9, compVal);
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
     * @param register
     * @return 8bit value read from register
     */
    private static int readRegister(int register) {
        console.println(">>> Enter readRegister   : " + String.format("0X%02x: ", register));
        csGpio.low();
        byte data[] = new byte[]{(byte) (0b10000000 | register)};
        int bytesWritten = spi.write(data);
        byte value[] = new byte[1];
        byte rval = spi.readByte();
        csGpio.high();
        console.println("<<< Exit readRegister   : " + String.format("0X%02x: ", rval));
        return (rval);
    }

    /**
     * @param register register address
     * @param buffer   Buffer to return read data
     * @return count     number bytes read or fail -1
     */
    private static int readRegister(int register, byte[] buffer) {
        console.println(">>> Enter readRegister   : " + String.format("0X%02x: ", register));
        byte data[] = new byte[]{(byte) (0b10000000 | register)};
        csGpio.low();
        int bytesWritten = spi.write(data);
        int bytesRead = spi.read(buffer);
        csGpio.high();
        console.println("<<< Exit readRegister   : " + String.format("0X%02x: ", buffer[0]) + String.format("0X%02x: ", buffer[0]));
        return (bytesRead);
    }

    /**
     * @param register register
     * @param data     byte to write
     * @return bytes written, else -1
     */
    private static int writeRegister(int register, int data) {
        console.println(">>> Enter writeRegister   : " + String.format("0X%02x: ", register));
        int rval = 0;
        int byteswritten = -1;
        byte buffer[] = new byte[]{(byte) (0b01111111 & register),
                (byte) data
        };
        byte dummy[] = new byte[2];
        // send read request to BMP chip via SPI channel
        csGpio.low();
        byteswritten = spi.write(buffer);
        csGpio.high();

        console.println("<<< Exit writeRegister wrote : " + byteswritten);
        return (rval);
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