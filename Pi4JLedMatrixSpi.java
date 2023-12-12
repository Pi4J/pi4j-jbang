///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS org.slf4j:slf4j-api:1.7.35
//DEPS org.slf4j:slf4j-simple:1.7.35
//DEPS com.pi4j:pi4j-core:2.3.0
//DEPS com.pi4j:pi4j-plugin-raspberrypi:2.3.0
//DEPS com.pi4j:pi4j-plugin-linuxfs:2.3.0
//DEPS com.pi4j:pi4j-plugin-pigpio:2.3.0

import com.pi4j.Pi4J;
import com.pi4j.io.spi.*;
import com.pi4j.util.Console;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Example code to control an 8x8 LED Matrix via SPI.
 * Make sure to follow the README of this project to learn more about JBang and how to install it.
 *
 * This example is based on the following project that was created with Pi4J V1:
 * https://github.com/FDelporte/JavaOnRaspberryPi/tree/master/Chapter_09_Pi4J/java-pi4j-spi
 *
 * The full description is available on:
 * https://pi4j.com/examples/jbang/jbang_pi4j_spi_led_matrix/
 *
 * This example must be executed with sudo as it uses PiGpio with:
 * sudo `which jbang` Pi4JLedMatrixSpi.java
 *
 */
public class Pi4JLedMatrixSpi {

    private static final Console console = new Console(); // Pi4J Logger helper

    private static Spi spi;

    public static void main(String[] args) throws Exception {
        var pi4j = Pi4J.newAutoContext();

        // Initialize SPI
        console.println("Initializing the matrix via SPI");

        var spiConfig = Spi.newConfigBuilder(pi4j)
                .id("Matrix SPI Provider")
                .name("matrix-spi")
                .bus(SpiBus.BUS_0)
                .chipSelect(SpiChipSelect.CS_0)
                .baud(Spi.DEFAULT_BAUD)
                .mode(SpiMode.MODE_0)
                .provider("pigpio-spi")
                .build();
        spi = pi4j.create(spiConfig);

        spi.write(SpiCommand.TEST.getValue(), (byte) 0x01);
        System.out.println("Test mode all on");
        Thread.sleep(1000);

        spi.write(SpiCommand.TEST.getValue(), (byte) 0x00);
        System.out.println("Test mode all off");
        Thread.sleep(1000);

        spi.write(SpiCommand.DECODE_MODE.getValue(), (byte) 0x00);
        System.out.println("Use all bits");

        spi.write(SpiCommand.BRIGHTNESS.getValue(), (byte) 0x08);
        System.out.println("Changed brightness to medium level"
                + " (0x00 lowest, 0x0F highest)");

        spi.write(SpiCommand.SCAN_LIMIT.getValue(), (byte) 0x0f);
        System.out.println("Configured to scan all digits");

        spi.write(SpiCommand.SHUTDOWN_MODE.getValue(), (byte) 0x01);
        System.out.println("Woke up the MAX7219, is off on startup");

        allOff();

        showOneByOne(100);

        showRows( 250);
        showCols( 250);

        showRandomOutput( 5, 500);

        showAllImages( 2000);
        showAllAsciiCharacters( 750);
        scrollAllAsciiCharacters( 50);

        allOff();

        pi4j.shutdown();

        console.println("Finished");
    }

    public static void allOff() {
        try {
            for (int row = 1; row <= 8; row++) {
                spi.write((byte) row, (byte) 0x00);
            }
        } catch (Exception ex) {
            System.err.println("Error during row demo: " + ex.getMessage());
        }
    }

    /**
     * Highlight all LEDs one by one.
     *
     * @param waitBetween Number of milliseconds to wait between every LED output
     */
    public static void showOneByOne(int waitBetween) {
        try {
            for (int row = 1; row <= 8; row++) {
                System.out.println("One by one on row " + row);
                for (int led = 0; led < 8; led++) {
                    allOff();
                    spi.write((byte) row, (byte) (1 << led));
                    Thread.sleep(waitBetween);
                }
            }
        } catch (Exception ex) {
            System.err.println("Error during row demo: " + ex.getMessage());
        }
    }

    /**
     * Highlight all rows one by one.
     *
     * @param waitBetween Number of milliseconds to wait between every row output
     */
    public static void showRows(int waitBetween) {
        try {
            for (int onRow = 1; onRow <= 8; onRow++) {
                for (int row = 1; row <= 8; row++) {
                    spi.write((byte) row, (onRow == row ? (byte) 0xff : (byte) 0x00));
                }
                System.out.println("Row " + onRow + " is on");
                Thread.sleep(waitBetween);
            }
        } catch (Exception ex) {
            System.err.println("Error during row demo: " + ex.getMessage());
        }
    }

    /**
     * Highlight all columns one by one.
     *
     * @param waitBetween Number of milliseconds to wait between every column output
     */
    public static void showCols(int waitBetween) {
        try {
            for (int onColumn = 0; onColumn < 8; onColumn++) {
                for (int row = 1; row <= 8; row++) {
                    spi.write((byte) row, (byte) (1 << (8 - onColumn)));
                }
                System.out.println("Col " + onColumn + " is on");
                Thread.sleep(waitBetween);
            }
        } catch (Exception ex) {
            System.err.println("Error during column demo: " + ex.getMessage());
        }
    }

    /**
     * Demo mode which generates specified number of cycles of random enabled LEDs.
     *
     * @param numberOfLoops Number of random outputs to be generated
     * @param waitBetween Number of milliseconds to wait between random screens
     */
    public static void showRandomOutput(int numberOfLoops, int waitBetween) {
        try {
            Random r = new Random();
            int min = 0;
            int max = 255;

            for (int loop = 1; loop <= numberOfLoops; loop++) {
                for (int row = 1; row <= 8; row++) {
                    spi.write((byte) row, (byte) (r.nextInt((max - min) + 1) + min));
                }
                System.out.println("Random effect " + loop);
                Thread.sleep(waitBetween);
            }
        } catch (Exception ex) {
            System.err.println("Error during random demo: " + ex.getMessage());
        }
    }

    /**
     * Show all the images as defined in the enum.
     *
     * @param waitBetween Number of milliseconds to wait between every image output
     */
    public static void showAllImages(int waitBetween) {
        try {
            for (Image image : Image.values()) {
                showImage(image);
                System.out.println("Showing image " + image.name());
                Thread.sleep(waitBetween);
            }
        } catch (Exception ex) {
            System.err.println("Error during images: " + ex.getMessage());
        }
    }

    /**
     * Output the given image to the matrix.
     *
     * @param image Image to be shown
     */
    public static void showImage(Image image) {
        try {
            for (int i = 0; i < 8; i++) {
                spi.write((byte) (i + 1), image.getRows().get(i));
            }
        } catch (Exception ex) {
            System.err.println("Error during images: " + ex.getMessage());
        }
    }

    /**
     * Show all the configured characters.
     *
     * @param waitBetween Milliseconds between every AsciiCharacter
     */
    public static void showAllAsciiCharacters(int waitBetween) {
        try {
            for (AsciiCharacter asciiCharacter : AsciiCharacter.values()) {
                showAsciiCharacter(asciiCharacter);
                System.out.println("Written to SPI : " + asciiCharacter.name());
                Thread.sleep(waitBetween);
            }
        } catch (Exception ex) {
            System.err.println("Error during Ascii: " + ex.getMessage());
        }
    }

    /**
     * Output the given alphabet character to the display.
     *
     * @param asciiCharacter AsciiCharacter to be shown
     */
    public static void showAsciiCharacter(AsciiCharacter asciiCharacter) {
        try {
            for (int row = 0; row < 8; row++) {
                spi.write((byte) (row + 1), asciiCharacter.getRows().get(row));
            }
        } catch (Exception ex) {
            System.err.println("Error during images: " + ex.getMessage());
        }
    }

    /**
     * Show all the configured characters.
     *
     * @param waitBetweenMove Milliseconds between every column move
     */
    public static void scrollAllAsciiCharacters(int waitBetweenMove) {
        try {
            for (AsciiCharacter asciiCharacter : AsciiCharacter.values()) {
                scrollAsciiCharacter(asciiCharacter, waitBetweenMove);
                System.out.println("Scrolled : " + asciiCharacter.name());
                Thread.sleep(250);
            }
        } catch (Exception ex) {
            System.err.println("Error during Ascii: " + ex.getMessage());
        }
    }

    /**
     * Scroll a character over the screen.
     *
     * @param asciiCharacter AsciiCharacter to be scrolled
     * @param waitBetweenMove Milliseconds between every column move
     */
    public static void scrollAsciiCharacter(AsciiCharacter asciiCharacter, int waitBetweenMove) {
        try {
            for (int move = 0; move < ((8 * 2) + 1); move++) {
                for (int row = 0; row < 8; row++) {
                    int rowValue = 0xFF & asciiCharacter.getRows().get(row);
                    if (move < 8) {
                        rowValue = 0xFF & (rowValue >> (8 - move));
                    } else {
                        rowValue = 0xFF & (rowValue << (move - 8));
                    }
                    spi.write((byte) (row + 1), (byte) rowValue);
                }
                Thread.sleep(waitBetweenMove);
            }
        } catch (Exception ex) {
            System.err.println("Error during images: " + ex.getMessage());
        }
    }

    public enum SpiCommand {
        DECODE_MODE((byte) 0x09),
        BRIGHTNESS((byte) 0x0A),
        SCAN_LIMIT((byte) 0x0B),
        SHUTDOWN_MODE((byte) 0x0C),
        TEST((byte) 0x0F);

        private final byte value;

        SpiCommand(byte value) {
            this.value = value;
        }

        public byte getValue() {
            return value;
        }
    }

    public enum AsciiCharacter {
        SPACE(0x20, 2, Arrays.asList(
                (byte) Integer.parseInt("00", 2),
                (byte) Integer.parseInt("00", 2),
                (byte) Integer.parseInt("00", 2),
                (byte) Integer.parseInt("00", 2),
                (byte) Integer.parseInt("00", 2),
                (byte) Integer.parseInt("00", 2),
                (byte) Integer.parseInt("00", 2),
                (byte) Integer.parseInt("00", 2)
        )),
        A(0x41, 6, Arrays.asList(
                (byte) Integer.parseInt("001100", 2),
                (byte) Integer.parseInt("001100", 2),
                (byte) Integer.parseInt("010010", 2),
                (byte) Integer.parseInt("010010", 2),
                (byte) Integer.parseInt("011110", 2),
                (byte) Integer.parseInt("111111", 2),
                (byte) Integer.parseInt("100001", 2),
                (byte) Integer.parseInt("100001", 2)
        )),
        B(0x42, 6, Arrays.asList(
                (byte) Integer.parseInt("111110", 2),
                (byte) Integer.parseInt("100001", 2),
                (byte) Integer.parseInt("100001", 2),
                (byte) Integer.parseInt("111110", 2),
                (byte) Integer.parseInt("100001", 2),
                (byte) Integer.parseInt("100001", 2),
                (byte) Integer.parseInt("100001", 2),
                (byte) Integer.parseInt("111110", 2)
        )),
        E(0x45, 6, Arrays.asList(
                (byte) Integer.parseInt("111111", 2),
                (byte) Integer.parseInt("100000", 2),
                (byte) Integer.parseInt("100000", 2),
                (byte) Integer.parseInt("111111", 2),
                (byte) Integer.parseInt("100000", 2),
                (byte) Integer.parseInt("100000", 2),
                (byte) Integer.parseInt("100000", 2),
                (byte) Integer.parseInt("111111", 2)
        )),
        I(0x49, 3, Arrays.asList(
                (byte) Integer.parseInt("111", 2),
                (byte) Integer.parseInt("010", 2),
                (byte) Integer.parseInt("010", 2),
                (byte) Integer.parseInt("010", 2),
                (byte) Integer.parseInt("010", 2),
                (byte) Integer.parseInt("010", 2),
                (byte) Integer.parseInt("010", 2),
                (byte) Integer.parseInt("111", 2)
        )),
        S(0x53, 6, Arrays.asList(
                (byte) Integer.parseInt("011110", 2),
                (byte) Integer.parseInt("100001", 2),
                (byte) Integer.parseInt("100000", 2),
                (byte) Integer.parseInt("011110", 2),
                (byte) Integer.parseInt("000001", 2),
                (byte) Integer.parseInt("000001", 2),
                (byte) Integer.parseInt("100001", 2),
                (byte) Integer.parseInt("011110", 2)
        )),
        T(0x54, 5, Arrays.asList(
                (byte) Integer.parseInt("11111", 2),
                (byte) Integer.parseInt("00100", 2),
                (byte) Integer.parseInt("00100", 2),
                (byte) Integer.parseInt("00100", 2),
                (byte) Integer.parseInt("00100", 2),
                (byte) Integer.parseInt("00100", 2),
                (byte) Integer.parseInt("00100", 2),
                (byte) Integer.parseInt("00100", 2)
        ));

        private final int ascii;
        private final int numberOfColumns;
        private final List<Byte> rows;

        AsciiCharacter(int ascii, int numberOfColumns, List<Byte> rows) {
            this.ascii = ascii;
            this.numberOfColumns = numberOfColumns;
            this.rows = rows;
        }

        public int getAscii() {
            return ascii;
        }

        public int getNumberOfColumns() {
            return numberOfColumns;
        }

        public List<Byte> getRows() {
            return rows;
        }

        public static AsciiCharacter getByAscii(int ascii) {
            for (AsciiCharacter asciiCharacter : AsciiCharacter.values()) {
                if (asciiCharacter.getAscii() == ascii) {
                    return asciiCharacter;
                }
            }
            return null;
        }

        public static AsciiCharacter getByChar(char character) {
            return getByAscii(character);
        }
    }

    public enum Image {
        HEART(Arrays.asList(
                (byte) Integer.parseInt("00100100", 2),
                (byte) Integer.parseInt("01111110", 2),
                (byte) Integer.parseInt("11111111", 2),
                (byte) Integer.parseInt("11111111", 2),
                (byte) Integer.parseInt("01111110", 2),
                (byte) Integer.parseInt("00111100", 2),
                (byte) Integer.parseInt("00111100", 2),
                (byte) Integer.parseInt("00011000", 2)
        )),
        PI_LOGO(Arrays.asList(
                (byte) Integer.parseInt("01100110", 2),
                (byte) Integer.parseInt("00111100", 2),
                (byte) Integer.parseInt("01011010", 2),
                (byte) Integer.parseInt("01011010", 2),
                (byte) Integer.parseInt("10100101", 2),
                (byte) Integer.parseInt("10100101", 2),
                (byte) Integer.parseInt("01011010", 2),
                (byte) Integer.parseInt("00111100", 2)
        )),
        SMILEY(Arrays.asList(
                (byte) Integer.parseInt("00011000", 2),
                (byte) Integer.parseInt("01100110", 2),
                (byte) Integer.parseInt("10000001", 2),
                (byte) Integer.parseInt("10100101", 2),
                (byte) Integer.parseInt("10000001", 2),
                (byte) Integer.parseInt("10011001", 2),
                (byte) Integer.parseInt("01100110", 2),
                (byte) Integer.parseInt("00011000", 2)
        )),
        ARROW_LEFT(Arrays.asList(
                (byte) Integer.parseInt("00000011", 2),
                (byte) Integer.parseInt("00001100", 2),
                (byte) Integer.parseInt("00110000", 2),
                (byte) Integer.parseInt("11000000", 2),
                (byte) Integer.parseInt("11000000", 2),
                (byte) Integer.parseInt("00110000", 2),
                (byte) Integer.parseInt("00001100", 2),
                (byte) Integer.parseInt("00000011", 2)
        )),
        CROSS(Arrays.asList(
                (byte) Integer.parseInt("10000001", 2),
                (byte) Integer.parseInt("01000010", 2),
                (byte) Integer.parseInt("00100100", 2),
                (byte) Integer.parseInt("00011000", 2),
                (byte) Integer.parseInt("00011000", 2),
                (byte) Integer.parseInt("00100100", 2),
                (byte) Integer.parseInt("01000010", 2),
                (byte) Integer.parseInt("10000001", 2)
        ));

        private final List<Byte> rows;

        Image(List<Byte> rows) {
            this.rows = rows;
        }

        public List<Byte> getRows() {
            return rows;
        }
    }
}