///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS com.fazecast:jSerialComm:2.10.2
//SOURCES helper/PixelBlazeOutputExpanderHelper.java

import helper.PixelBlazeOutputExpanderHelper;

import java.util.Random;

/**
 * Example code to use a Pixelblaze Output Expander to control a LED strip.
 * Make sure to follow the README of this project to learn more about JBang and how to install it.
 *
 * Although this sample is part of the Pi4J JBang examples, it doesn't use Pi4J ;-)
 * The Pixelblaze Output Expander is a serial device, and the library com.fazecast.jSerialComm seems to provide
 * good support for the data speed required by the Pixelblaze.
 *
 * Product page: https://shop.electromage.com/products/pixelblaze-output-expander-serial-to-8x-ws2812-apa102-driver
 *
 * This example can be executed without sudo:
 * jbang PixelblazeOutputExpander.java
 *
 * Thanks to Jeff Vyduna for his Java driver for the Pixelblaze Output Expander that has been used in this example.
 * Serial data format info: https://github.com/simap/pixelblaze_output_expander/tree/v3.x
 * 
 * Serial Wiring
 *
 * <ul>
 *  <li>GND to GND, common with RPi</li>
 *  <li>5V to external power supply</li>
 *  <li>DAT to BCM14 (pin 8 = UART Tx)</li>
 * </ul>
 *
 * All info about this example is described here:
 * https://pi4j.com/examples/jbang/pixelblaze_output_expander/
 */
public class PixelblazeOutputExpander {

    private static final int BYTES_PER_PIXEL = 3;
    private static final int CHANNEL_STRIP_SHORT = 0;
    private static final int CHANNEL_STRIP_LONG = 1;
    private static final int CHANNEL_MATRIX = 2;
    private static final int NUMBER_OF_LEDS_STRIP_SHORT = 11;
    private static final int NUMBER_OF_LEDS_STRIP_LONG = 300;
    private static final int NUMBER_OF_LEDS_MATRIX = 256; // 8*32

    private static PixelBlazeOutputExpanderHelper helper;

    public static void main(String[] args) throws InterruptedException {
        helper = new PixelBlazeOutputExpanderHelper("/dev/ttyS0");

        // All off, short LED strip
        helper.sendAllOff(CHANNEL_STRIP_SHORT, NUMBER_OF_LEDS_STRIP_SHORT);
        Thread.sleep(500);

        // One by one red, short LED strip
        sendOneByOne(CHANNEL_STRIP_SHORT, NUMBER_OF_LEDS_STRIP_SHORT, (byte) 0xff, (byte) 0x00, (byte) 0x00);

        // All the same color red, green, blue, short LED strip
        for (int color = 0; color < BYTES_PER_PIXEL; color++) {
            System.out.println("All " + (color == 0 ? "red" : (color == 1 ? "green" : "blue")));
            byte[] allSame = new byte[NUMBER_OF_LEDS_STRIP_SHORT * BYTES_PER_PIXEL];
            for (int i = 0; i < NUMBER_OF_LEDS_STRIP_SHORT; i++) {
                allSame[(BYTES_PER_PIXEL * i) + color] = (byte) 0xff;
            }
            helper.sendColors(CHANNEL_STRIP_SHORT, BYTES_PER_PIXEL, 1, 0, 2, 0, allSame, false);

            Thread.sleep(1000);
        }

        // Fill strip with random colors, short LED strip
        Random rd = new Random();
        for (int i = 0; i < 5; i++) {
            System.out.println("Random colors " + (i + 1));
            byte[] random = new byte[NUMBER_OF_LEDS_STRIP_SHORT * BYTES_PER_PIXEL];
            rd.nextBytes(random);
            helper.sendColors(CHANNEL_STRIP_SHORT, BYTES_PER_PIXEL, 1, 0, 2, 0, random, false);
            Thread.sleep(1000);
        }

        // Red alert, short LED strip
        byte[] red = new byte[NUMBER_OF_LEDS_STRIP_SHORT * BYTES_PER_PIXEL];
        int i;
        for (i = 0; i < NUMBER_OF_LEDS_STRIP_SHORT; i++) {
            red[i*BYTES_PER_PIXEL]= (byte) 0xff;
        }
        for (i = 0; i < 5; i++) {
            System.out.println("All red");
            helper.sendColors(CHANNEL_STRIP_SHORT, BYTES_PER_PIXEL, 1, 0, 2, 0, red, false);
            Thread.sleep(100);
            helper.sendAllOff(CHANNEL_STRIP_SHORT, NUMBER_OF_LEDS_STRIP_SHORT);
            Thread.sleep(100);
        }

        // One by one red/green/blue on long strip, 5 meter with 60 LEDs/meter
        sendOneByOne(CHANNEL_STRIP_LONG, NUMBER_OF_LEDS_STRIP_LONG, (byte) 0xff, (byte) 0x00, (byte) 0x00);
        sendOneByOne(CHANNEL_STRIP_LONG, NUMBER_OF_LEDS_STRIP_LONG, (byte) 0x00, (byte) 0xff, (byte) 0x00);
        sendOneByOne(CHANNEL_STRIP_LONG, NUMBER_OF_LEDS_STRIP_LONG, (byte) 0x00, (byte) 0x00, (byte) 0xff);

        // Flash all red/white on long strip, 5 meter with 60 LEDs/meter
        byte[] fiveMeterRed = new byte[NUMBER_OF_LEDS_STRIP_LONG * BYTES_PER_PIXEL];
        byte[] fiveMeterWhite = new byte[NUMBER_OF_LEDS_STRIP_LONG * BYTES_PER_PIXEL];
        for (i = 0; i < NUMBER_OF_LEDS_STRIP_LONG; i++) {
            fiveMeterRed[i*BYTES_PER_PIXEL]= (byte) 0xff;
            fiveMeterWhite[i*BYTES_PER_PIXEL]= (byte) 0xff;
            fiveMeterWhite[(i*BYTES_PER_PIXEL) + 1]= (byte) 0xff;
            fiveMeterWhite[(i*BYTES_PER_PIXEL) + 2]= (byte) 0xff;
        }
        for (i = 0; i < 5; i++) {
            System.out.println("All RED on LED strip on channel 1");
            helper.sendColors(CHANNEL_STRIP_LONG, BYTES_PER_PIXEL, 1, 0, 2, 0, fiveMeterRed, false);
            Thread.sleep(500);
            System.out.println("All RED on LED strip on channel 1");
            helper.sendColors(CHANNEL_STRIP_LONG, BYTES_PER_PIXEL, 1, 0, 2, 0, fiveMeterWhite, false);
            Thread.sleep(500);
        }

        // All off, long LED strip
        helper.sendAllOff(CHANNEL_STRIP_LONG, NUMBER_OF_LEDS_STRIP_LONG);
        Thread.sleep(100);

        // All red, 8*32 LED matrix
        byte[] redMatrix = new byte[NUMBER_OF_LEDS_MATRIX * BYTES_PER_PIXEL];
        for (i = 0; i < NUMBER_OF_LEDS_MATRIX; i++) {
            redMatrix[i*BYTES_PER_PIXEL]= (byte) 0xff;
        }
        for (i = 0; i < 5; i++) {
            System.out.println("All red on LED matrix on channel 2");
            helper.sendColors(CHANNEL_MATRIX, BYTES_PER_PIXEL, 1, 0, 2, 0, redMatrix, false);
            Thread.sleep(100);
            helper.sendAllOff(CHANNEL_MATRIX, NUMBER_OF_LEDS_MATRIX);
            Thread.sleep(100);
        }

        helper.closePort();
    }

    private static void sendOneByOne(int channel, int numberOfLeds, byte red, byte green, byte blue) throws InterruptedException {
         for (int i = 0; i < numberOfLeds; i++) {
            System.out.println("One by one on channel " + channel + ", LED " + i);
            byte[] oneLed = new byte[numberOfLeds * BYTES_PER_PIXEL];
            oneLed[i * BYTES_PER_PIXEL] = red;
            oneLed[(i * BYTES_PER_PIXEL) + 1] = green;
            oneLed[(i * BYTES_PER_PIXEL) + 2] = blue;
            helper.sendColors(channel, BYTES_PER_PIXEL, 1, 0, 2, 0, oneLed, false);
            Thread.sleep(50);
        }
    }
}