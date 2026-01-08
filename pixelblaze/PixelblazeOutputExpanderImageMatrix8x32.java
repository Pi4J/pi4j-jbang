/// usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS com.fazecast:jSerialComm:2.10.2
//SOURCES helper/ImageHelper.java
//SOURCES helper/PixelBlazeOutputExpanderHelper.java

import helper.PixelBlazeOutputExpanderHelper;

import java.io.IOException;
import java.util.Random;

import static helper.ImageHelper.getImageData;
import static helper.ImageHelper.imageToMatrix;

/**
 * Example code to use a Pixelblaze Output Expander to send an 8*32 image to a LED matrix. This example is based on
 * pixelblaze.PixelblazeOutputExpander.java, so please check its documentation first!
 * <p>
 * This example can be executed without sudo:
 * jbang pixelblaze.PixelblazeOutputExpanderImageMatrix8x32.java
 */
public class PixelblazeOutputExpanderImageMatrix8x32 {

    private static final int BYTES_PER_PIXEL = 3;
    private static final int CHANNEL = 2;
    private static final int MATRIX_WIDTH = 32;
    private static final int MATRIX_HEIGHT = 8;
    private static final int NUMBER_OF_LEDS = MATRIX_HEIGHT * MATRIX_WIDTH;

    public static void main(String[] args) throws IOException, InterruptedException {
        // Depending on the type of board and the connection you are using
        // (GPIO pin, or other serial connection), this can be a different port.
        // Most probably it will be `/dev/ttyS0` (Raspberry Pi 4 or earlier),
        // or `/dev/ttyAMA0` (Raspberry Pi 5).
        PixelBlazeOutputExpanderHelper helper = new PixelBlazeOutputExpanderHelper("/dev/ttyS0");
        int i;

        // Clear any remaining LEDs from previous test
        helper.sendAllOff(CHANNEL, NUMBER_OF_LEDS);
        Thread.sleep(1000);

        // Check the position of the LEDs, to identify how the LED strip is wired
        System.out.println("One by one RED");
        for (i = 0; i < NUMBER_OF_LEDS; i++) {
            byte[] pixelData = new byte[NUMBER_OF_LEDS * BYTES_PER_PIXEL];
            pixelData[i * BYTES_PER_PIXEL] = (byte) 0xff; // red
            helper.sendColors(CHANNEL, BYTES_PER_PIXEL, 1, 0, 2, 0, pixelData, false);
            Thread.sleep(20);
        }

        // All white to test load on power supply
        System.out.println("Full RGB");
        byte[] allWhite = new byte[NUMBER_OF_LEDS * BYTES_PER_PIXEL];
        for (i = 0; i < NUMBER_OF_LEDS * BYTES_PER_PIXEL; i++) {
            allWhite[i] = (byte) 0xff;
        }
        helper.sendColors(CHANNEL, BYTES_PER_PIXEL, 1, 0, 2, 0, allWhite, false);
        Thread.sleep(5000);

        // Output all defined images
        for (TestImage testImage : TestImage.values()) {
            // System.out.println("Image: " + testImage);

            // Get the bytes from the given image
            byte[] pixelData = imageToMatrix(
                    getImageData("data/" + testImage.getFileName(), BYTES_PER_PIXEL, MATRIX_WIDTH, MATRIX_HEIGHT),
                    BYTES_PER_PIXEL, MATRIX_WIDTH, MATRIX_HEIGHT);

            // Show the image on the LED matrix
            helper.sendColors(CHANNEL, BYTES_PER_PIXEL, 1, 0, 2, 1, pixelData, false);

            Thread.sleep(testImage.getDuration());
        }

        // Random colors
        Random rd = new Random();
        for (i = 0; i < 100; i++) {
            byte[] random = new byte[8 * 32 * BYTES_PER_PIXEL];
            rd.nextBytes(random);
            helper.sendColors(CHANNEL, BYTES_PER_PIXEL, 1, 0, 2, 0, random, false);
            Thread.sleep(50);
        }

        // Done
        helper.sendAllOff(CHANNEL, NUMBER_OF_LEDS);
        Thread.sleep(1000);

        helper.closePort();
    }

    private enum TestImage {
        LINE_1("image_8_32_line_1.png", 250),
        LINE_2("image_8_32_line_2.png", 250),
        LINE_3("image_8_32_line_3.png", 250),
        LINE_4("image_8_32_line_4.png", 250),
        LINE_5("image_8_32_line_5.png", 250),
        LINE_6("image_8_32_line_6.png", 250),
        LINE_7("image_8_32_line_7.png", 250),
        LINE_8("image_8_32_line_8.png", 250),
        RED("image_8_32_red.png", 500),
        GREEN("image_8_32_green.png", 500),
        BLUE("image_8_32_blue.png", 500),
        STRIPES("image_8_32_stripes.png", 2000),
        STRIPES_TEST("image_8_32_stripes_test.png", 2000),
        DUKE("image_8_32_duke.png", 2000),
        RPI("image_8_32_raspberrypi.png", 2000);

        private final String fileName;
        private final int duration;

        TestImage(String fileName, int duration) {
            this.fileName = fileName;
            this.duration = duration;
        }

        public String getFileName() {
            return fileName;
        }

        public int getDuration() {
            return duration;
        }
    }
}