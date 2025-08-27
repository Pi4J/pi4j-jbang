/// usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS org.slf4j:slf4j-api:2.0.12
//DEPS org.slf4j:slf4j-simple:2.0.12
//DEPS com.pi4j:pi4j-core:3.0.1
//DEPS com.pi4j:pi4j-plugin-raspberrypi:3.0.1
//DEPS com.pi4j:pi4j-plugin-linuxfs:3.0.1

//DEPS org.slf4j:slf4j-api:2.0.12
//DEPS org.slf4j:slf4j-simple:2.0.12
//DEPS com.pi4j:pi4j-core:3.0.1
//DEPS com.pi4j:pi4j-plugin-raspberrypi:3.0.1
//DEPS com.pi4j:pi4j-plugin-linuxfs:3.0.1

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.pwm.Pwm;
import com.pi4j.io.pwm.PwmType;
import com.pi4j.plugin.linuxfs.provider.pwm.LinuxFsPwmProviderImpl;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

/**
 * Example code to control an 8x8 RGB LED Matrix via PWM, as found in the CrowPi 2.
 * Make sure to follow the README of this project to learn more about JBang and how to install it.
 * <p>
 * This example must be executed with sudo as it uses PiGpio with:
 * jbang Pi4JRgbLedMatrixPwm.java
 *
 */
public class Pi4JRgbLedMatrixPwm {

    /**
     * RGB LED Matrix in CrowPi 2 is connected to WPI 26 = BCM 12 = on RPi 5 is Channel 0
     */
    private static final int CHANNEL = 0;

    /**
     * Width and height of the LED matrix
     */
    private static final int WIDTH = 8;
    private static final int HEIGHT = 8;
    private static final int TOTAL_LEDS = WIDTH * HEIGHT;

    /**
     * PWM frequency for WS2812B timing (800kHz)
     */
    private static final int PWM_FREQUENCY = 800000;

    /**
     * RGB color buffer for the matrix (3 bytes per LED: R, G, B)
     */
    private final Color[][] colorBuffer;

    /**
     * Monochrome buffer for compatibility with existing matrix symbols
     */
    private final boolean[][] monoBuffer;

    /**
     * Pi4J PWM instance for controlling WS2812B LEDs
     */
    private final Pwm pwm;

    /**
     * Default brightness (0.0 to 1.0)
     */
    private double brightness = 0.1; // Start dim to avoid power issues

    /**
     * Creates a new RGB LED matrix component with a custom GPIO pin.
     *
     * @param pi4j Pi4J context
     */
    public Pi4JRgbLedMatrixPwm(Context pi4j) {
        this.colorBuffer = new Color[HEIGHT][WIDTH];
        this.monoBuffer = new boolean[HEIGHT][WIDTH];

        // Initialize buffers
        clear();

        // Create PWM configuration for WS2812B
        // Note: WS2812B requires precise timing that's difficult with Pi4J PWM
        // This is a conceptual implementation - real usage would need native library
        var pwmConfig = Pwm.newConfigBuilder(pi4j)
                .address(CHANNEL)
                .pwmType(PwmType.HARDWARE)
                .initial(0)
                .shutdown(0)
                .build();

        this.pwm = pi4j.create(pwmConfig);
    }

    public static void main(String[] args) {
        // Initialize the Pi4J context
        var pi4j = Pi4J.newContextBuilder()
                .add(new LinuxFsPwmProviderImpl("/sys/class/pwm/", 0))
                .build();

        // Initialize the RGB LED Matrix
        var matrix = new Pi4JRgbLedMatrixPwm(pi4j);

        // Display something on the LED Matrix
        matrix.fill(Color.RED);
        System.out.println("Filling with red...");
        sleep(1000);

        matrix.fill(Color.BLUE);
        System.out.println("Filling with blue...");
        sleep(1000);

        matrix.setPixel(2, 3, Color.GREEN);
        System.out.println("Set one pixel green...");
        sleep(1000);

        // Shut down the Pi4J contextq
        pi4j.shutdown();

        System.out.println("Done");
    }

    /**
     * Utility function to sleep for the specified amount of milliseconds.
     * An {@link InterruptedException} will be catched and ignored while setting the interrupt flag again.
     *
     * @param milliseconds Time in milliseconds to sleep
     */
    private static void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Gets the current brightness level
     *
     * @return Current brightness (0.0 to 1.0)
     */
    public double getBrightness() {
        return brightness;
    }

    /**
     * Sets the brightness of the matrix (0.0 to 1.0)
     * Note: Be careful with high brightness values as they can draw significant current
     *
     * @param brightness Brightness level (0.0 = off, 1.0 = full brightness)
     */
    public void setBrightness(double brightness) {
        this.brightness = Math.max(0.0, Math.min(1.0, brightness));
    }

    /**
     * Sets a pixel to a specific color
     *
     * @param x     X coordinate (0-7)
     * @param y     Y coordinate (0-7)
     * @param color Color to set
     */
    public void setPixel(int x, int y, Color color) {
        if (x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT) {
            colorBuffer[y][x] = color;
            monoBuffer[y][x] = !color.equals(Color.BLACK);
        }
    }

    /**
     * Sets a pixel to on or off (for monochrome compatibility)
     *
     * @param x     X coordinate (0-7)
     * @param y     Y coordinate (0-7)
     * @param state true for on (white), false for off (black)
     */
    public void setPixel(int x, int y, boolean state) {
        setPixel(x, y, state ? Color.WHITE : Color.BLACK);
    }

    /**
     * Gets the color of a specific pixel
     *
     * @param x X coordinate (0-7)
     * @param y Y coordinate (0-7)
     * @return Color of the pixel
     */
    public Color getPixel(int x, int y) {
        if (x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT) {
            return colorBuffer[y][x];
        }
        return Color.BLACK;
    }

    /**
     * Clears the entire matrix (all pixels off)
     */
    public void clear() {
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                colorBuffer[y][x] = Color.BLACK;
                monoBuffer[y][x] = false;
            }
        }
    }

    /**
     * Sets the entire matrix to a single color
     *
     * @param color Color to fill the matrix with
     */
    public void fill(Color color) {
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                colorBuffer[y][x] = color;
                monoBuffer[y][x] = !color.equals(Color.BLACK);
            }
        }
    }

    /**
     * Refreshes the display by sending the current buffer to the LED matrix
     * This converts the RGB buffer to WS2812B format and sends it via PWM
     */
    public void refresh() {
        // Convert color buffer to WS2812B data format
        byte[] ledData = new byte[TOTAL_LEDS * 3]; // 3 bytes per LED (GRB format for WS2812B)

        int dataIndex = 0;
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                Color pixel = colorBuffer[y][x];

                // Apply brightness scaling
                int red = (int) (pixel.getRed() * brightness);
                int green = (int) (pixel.getGreen() * brightness);
                int blue = (int) (pixel.getBlue() * brightness);

                // WS2812B expects GRB format
                ledData[dataIndex++] = (byte) green;
                ledData[dataIndex++] = (byte) red;
                ledData[dataIndex++] = (byte) blue;
            }
        }

        // Send data to LEDs via PWM (this is a simplified approach)
        // In a real implementation, you'd need to convert to proper WS2812B timing
        sendWS2812BData(ledData);
    }

    /**
     * Simplified method to send WS2812B data via PWM
     * Note: This is a conceptual implementation. Real WS2812B control requires
     * precise timing that may need native code or specialized libraries
     *
     * @param data RGB data in GRB format
     */
    private void sendWS2812BData(byte[] data) {
        // This is a placeholder implementation
        // Real WS2812B control requires precise timing:
        // - 0 bit: 0.4µs high, 0.85µs low
        // - 1 bit: 0.8µs high, 0.45µs low
        // - Reset: >50µs low

        // For a complete implementation, you would typically use:
        // 1. DMA + PWM for precise timing
        // 2. A native library like rpi_ws281x
        // 3. SPI with carefully crafted bit patterns

        System.out.println("Sending " + data.length + " bytes to WS2812B matrix");
        // Placeholder PWM duty cycle setting
        if (data.length > 0) {
            pwm.on(50, PWM_FREQUENCY); // 50% duty cycle as example
        } else {
            pwm.off();
        }
    }

    /**
     * Updates the monochrome buffer based on the color buffer
     */
    private void updateMonoBuffer() {
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                monoBuffer[y][x] = !colorBuffer[y][x].equals(Color.BLACK);
            }
        }
    }

    /**
     * Drawing support for RGB images
     */
    public void draw(Consumer<Graphics2D> drawer) {
        final var image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        final var graphics = image.createGraphics();
        drawer.accept(graphics);
        draw(image);
    }

    public void draw(BufferedImage image) {
        if (image.getWidth() != WIDTH || image.getHeight() != HEIGHT) {
            throw new IllegalArgumentException("Image must be exactly " + WIDTH + "x" + HEIGHT + " pixels");
        }

        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                Color pixelColor = new Color(image.getRGB(x, y));
                setPixel(x, y, pixelColor);
            }
        }
        refresh();
    }
}
