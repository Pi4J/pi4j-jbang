package draft; /// usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS org.slf4j:slf4j-api:2.0.17
//DEPS org.slf4j:slf4j-simple:2.0.17

import java.awt.*;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;

/**
 * <p>
 * Example code to control an 8x8 RGB LED Matrix via the FFM API, as found in the CrowPi 2.
 * Make sure to follow the README of this project to learn more about JBang and how to install it.
 * </p>
 * <p>
 * This is an experimental implementation that uses a the FFM API approach. A LED strip requires
 * precise timing which can be achieved with native code.
 * </p>
 * <p>
 * Make sure the required library is installed, see <code>script/build-rpi_ws281x.sh</code>.
 * </p>
 * <p>
 * This example needs Java 22 or newer:<br/>
 * <code>jbang --javaopt='--enable-native-access=ALL-UNNAMED' draft.Pi4JFFMRgbLedMatrix.java</code>
 * </p>
 */
public class Pi4JFFMRgbLedMatrix {

    /**
     * WS281x configuration constants
     */
    private static final int GPIO_PIN = 18;           // PWM GPIO pin
    private static final int LED_COUNT = 64;          // 8x8 matrix
    private static final int LED_FREQ_HZ = 800000;    // LED signal frequency (800kHz)
    private static final int DMA_CHANNEL = 10;        // DMA channel to use for generating signal
    private static final int LED_BRIGHTNESS = 255;    // LED brightness (0-255)
    private static final int LED_INVERT = 0;          // Signal line inversion (0 = normal, 1 = inverted)
    private static final int STRIP_TYPE = 0x00081000; // WS2811_STRIP_GRB

    /**
     * Width and height of the LED matrix
     */
    private static final int WIDTH = 8;
    private static final int HEIGHT = 8;
    /**
     * Native library bindings
     */
    private static final Linker linker = Linker.nativeLinker();
    private static final SymbolLookup stdlib = linker.defaultLookup();
    // Function handles for rpi_ws281x library
    private static final MethodHandle ws2811_init;
    private static final MethodHandle ws2811_render;
    private static final MethodHandle ws2811_fini;
    // Memory layouts for ws2811 structures
    private static final MemoryLayout ws2811_channel_t_layout = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT.withName("count"),      // LED count
            ValueLayout.ADDRESS.withName("leds"),        // LED data array
            ValueLayout.JAVA_BYTE.withName("brightness"), // brightness
            ValueLayout.JAVA_BYTE.withName("invert"),    // invert signal
            ValueLayout.JAVA_BYTE.withName("gpio"),      // GPIO pin
            MemoryLayout.paddingLayout(1),               // padding
            ValueLayout.JAVA_INT.withName("strip_type")  // strip type
    );
    private static final MemoryLayout ws2811_t_layout = MemoryLayout.structLayout(
            ValueLayout.JAVA_LONG.withName("freq"),      // frequency
            ValueLayout.JAVA_INT.withName("dmanum"),     // DMA number
            ws2811_channel_t_layout.withName("channel0"), // channel 0
            ws2811_channel_t_layout.withName("channel1"), // channel 1
            ValueLayout.ADDRESS.withName("device")        // device handle
    );
    // VarHandles for struct field access
    private static final VarHandle FREQ_HANDLE = ws2811_t_layout.varHandle(
            MemoryLayout.PathElement.groupElement("freq"));
    private static final VarHandle DMANUM_HANDLE = ws2811_t_layout.varHandle(
            MemoryLayout.PathElement.groupElement("dmanum"));
    private static final VarHandle CH0_COUNT_HANDLE = ws2811_t_layout.varHandle(
            MemoryLayout.PathElement.groupElement("channel0"),
            MemoryLayout.PathElement.groupElement("count"));
    private static final VarHandle CH0_LEDS_HANDLE = ws2811_t_layout.varHandle(
            MemoryLayout.PathElement.groupElement("channel0"),
            MemoryLayout.PathElement.groupElement("leds"));
    private static final VarHandle CH0_BRIGHTNESS_HANDLE = ws2811_t_layout.varHandle(
            MemoryLayout.PathElement.groupElement("channel0"),
            MemoryLayout.PathElement.groupElement("brightness"));
    private static final VarHandle CH0_GPIO_HANDLE = ws2811_t_layout.varHandle(
            MemoryLayout.PathElement.groupElement("channel0"),
            MemoryLayout.PathElement.groupElement("gpio"));
    private static final VarHandle CH0_STRIP_TYPE_HANDLE = ws2811_t_layout.varHandle(
            MemoryLayout.PathElement.groupElement("channel0"),
            MemoryLayout.PathElement.groupElement("strip_type"));

    static {
        try {
            // Try different possible library locations
            SymbolLookup libws281x = null;
            String[] libraryPaths = {
                    "libws2811.so.1",     // System-wide installation
                    "libws2811.so",       // Symlink
                    "/usr/local/lib/libws2811.so.1",  // Direct path
                    "./libws2811.so.1"    // Local build directory
            };

            RuntimeException lastException = null;
            for (String path : libraryPaths) {
                try {
                    libws281x = SymbolLookup.libraryLookup(path, Arena.global());
                    System.out.println("Successfully loaded library from: " + path);
                    break;
                } catch (Exception e) {
                    lastException = new RuntimeException("Failed to load from " + path, e);
                }
            }

            if (libws281x == null) {
                throw new RuntimeException("Could not load rpi_ws281x library from any location. " +
                        "Make sure it's built and installed. Last error: " +
                        (lastException != null ? lastException.getMessage() : "unknown"));
            }

            // Create method handles for native functions
            ws2811_init = linker.downcallHandle(
                    libws281x.find("ws2811_init").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)
            );

            ws2811_render = linker.downcallHandle(
                    libws281x.find("ws2811_render").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)
            );

            ws2811_fini = linker.downcallHandle(
                    libws281x.find("ws2811_fini").orElseThrow(),
                    FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
            );

        } catch (Throwable e) {
            throw new RuntimeException("Failed to load rpi_ws281x library. " +
                    "Make sure to build and install it from source: " +
                    "https://github.com/jgarff/rpi_ws281x", e);
        }
    }

    /**
     * RGB color buffer for the matrix (3 bytes per LED: R, G, B)
     */
    private final Color[][] colorBuffer;
    // Native memory segments
    private final MemorySegment ws2811_struct;
    private final MemorySegment led_data;
    private final Arena arena;
    /**
     * Default brightness (0.0 to 1.0)
     */
    private double brightness = 0.1;

    /**
     * Creates a new RGB LED matrix component with a custom GPIO pin.
     */
    public Pi4JFFMRgbLedMatrix() {
        this.colorBuffer = new Color[HEIGHT][WIDTH];
        this.arena = Arena.ofConfined();

        // Initialize color buffer
        clear();

        // Allocate native memory for ws2811 structure and LED data
        this.ws2811_struct = arena.allocate(ws2811_t_layout);
        this.led_data = arena.allocateFrom(ValueLayout.JAVA_INT, new int[LED_COUNT]);

        // Initialize ws2811 structure
        initializeWS2811Struct();

        // Initialize the library
        try {
            int result = (int) ws2811_init.invoke(ws2811_struct);
            if (result != 0) {
                throw new RuntimeException("ws2811_init failed with code: " + result +
                        ". Make sure to run as root or configure proper permissions.");
            }
            System.out.println("Successfully initialized rpi_ws281x library");
        } catch (Throwable e) {
            throw new RuntimeException("Failed to initialize ws2811", e);
        }
    }

    public static void main(String[] args) {
        // Create the RGB LED Matrix with try-with-resources for proper cleanup
        var matrix = new Pi4JFFMRgbLedMatrix();

        Runtime.getRuntime().addShutdownHook(new Thread(matrix::close));

        try {
            // Display something on the LED Matrix
            matrix.fill(Color.RED);
            matrix.refresh();
            System.out.println("Filling with red...");
            sleep(1000);

            matrix.fill(Color.BLUE);
            matrix.refresh();
            System.out.println("Filling with blue...");
            sleep(1000);

            matrix.setPixel(2, 3, Color.GREEN);
            matrix.refresh();
            System.out.println("Set one pixel green...");
            sleep(1000);

            // Test brightness control
            matrix.setBrightness(0.5);
            matrix.fill(Color.WHITE);
            matrix.refresh();
            System.out.println("Testing brightness at 50%...");
            sleep(2000);
        } finally {
            matrix.close();
        }

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

    private void initializeWS2811Struct() {
        // Set frequency
        FREQ_HANDLE.set(ws2811_struct, (long) LED_FREQ_HZ);

        // Set DMA channel
        DMANUM_HANDLE.set(ws2811_struct, DMA_CHANNEL);

        // Configure channel 0
        CH0_COUNT_HANDLE.set(ws2811_struct, LED_COUNT);
        CH0_LEDS_HANDLE.set(ws2811_struct, led_data);
        CH0_BRIGHTNESS_HANDLE.set(ws2811_struct, (byte) LED_BRIGHTNESS);
        CH0_GPIO_HANDLE.set(ws2811_struct, (byte) GPIO_PIN);
        CH0_STRIP_TYPE_HANDLE.set(ws2811_struct, STRIP_TYPE);
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
            }
        }
    }

    /**
     * Refreshes the display by sending the current buffer to the LED matrix
     * This converts the RGB buffer to WS2812B format and sends it via PWM
     */
    public void refresh() {
        // Convert color buffer to WS2812B data format
        byte[] ledData = new byte[WIDTH * HEIGHT * 3]; // 3 bytes per LED (GRB format for WS2812B)

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
     * Sends WS2812B data using the rpi_ws281x native library
     * This provides precise timing control for WS2812B LEDs
     *
     * @param data RGB data in GRB format
     */
    private void sendWS2812BData(byte[] data) {
        try {
            // Convert byte array to 32-bit integers (WS2812B format: 0x00GGRRBB)
            int[] ledValues = new int[LED_COUNT];

            for (int i = 0; i < Math.min(data.length / 3, LED_COUNT); i++) {
                int dataIndex = i * 3;

                // Extract GRB values (data is already in GRB format)
                int green = data[dataIndex] & 0xFF;
                int red = data[dataIndex + 1] & 0xFF;
                int blue = data[dataIndex + 2] & 0xFF;

                // Combine into 32-bit value: 0x00GGRRBB
                ledValues[i] = (green << 16) | (red << 8) | blue;
            }

            // Copy data to native memory
            MemorySegment.copy(ledValues, 0, led_data, ValueLayout.JAVA_INT, 0, ledValues.length);

            // Render the LED data
            int result = (int) ws2811_render.invoke(ws2811_struct);
            if (result != 0) {
                System.err.println("ws2811_render failed with code: " + result);
            }

        } catch (Throwable e) {
            System.err.println("Failed to send WS2812B data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Cleanup native resources
     */
    public void close() {
        try {
            ws2811_fini.invoke(ws2811_struct);
            arena.close();
            System.out.println("Successfully cleaned up rpi_ws281x resources");
        } catch (Throwable e) {
            System.err.println("Error during cleanup: " + e.getMessage());
        }
    }
}
