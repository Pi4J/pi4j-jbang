package helper.lcd;

import com.pi4j.context.Context;

/**
 * This class provides a simple usage of a LCD Display with Pi4J and the CrowPi.
 * There are different ways possible to use this functionalities from pretty simple to a bit more basic and advanced. *
 */
public class LcdDisplayComponent {
    /**
     * Those default address are to use this class with default CrowPi setup
     */
    private static final int DEFAULT_BUS = 0x1;
    private static final int DEFAULT_DEVICE = 0x21;
    /**
     * MCP IO Configuration makes pins to inputs or outputs
     */
    private static final byte MCP_IO_CONFIG = 0x00;
    /**
     * Commands which are available to execute on the display. Best to use execute method of this class
     */
    private static final byte LCD_CLEAR_DISPLAY = 0x01;
    private static final byte LCD_RETURN_HOME = 0x02;
    private static final byte LCD_ENTRY_MODE_SET = 0x04;
    private static final byte LCD_DISPLAY_CONTROL = 0x08;
    private static final byte LCD_FUNCTION_SET = 0x20;
    private static final byte LCD_SET_DDRAM_ADDR = (byte) 0x80;
    /**
     * Defines home on left side
     */
    private static final byte LCD_ENTRY_LEFT = 0x02;
    private static final byte LCD_ENTRY_SHIFT_DECREMENT = 0x00;
    /**
     * Flags to use with the display control byte
     */
    private static final byte LCD_DISPLAY_ON = 0x04;
    private static final byte LCD_CURSOR_OFF = 0x00;
    private static final byte LCD_BLINK_OFF = 0x00;
    /**
     * Move the cursor or display flags
     */
    // Function set flags
    private static final byte LCD_4BIT_MODE = 0x00;
    private static final byte LCD_2LINE = 0x08;
    private static final byte LCD_1LINE = 0x00;
    private static final byte LCD_5x8DOTS = 0x00;
    /**
     * Display row offsets. Offset for up to 2 rows.
     */
    private static final byte[] LCD_ROW_OFFSETS = {0x00, 0x40};
    /**
     * Pin out LCD auf MCP
     */
    private static final int LCD_RS = 1;
    private static final int LCD_EN = 2;
    private static final int LCD_D4 = 3;
    private static final int LCD_D5 = 4;
    private static final int LCD_D6 = 5;
    private static final int LCD_D7 = 6;
    private static final int LCD_LIGHT = 7;
    /**
     * IO Component used to Display
     */
    private final MCP23008 mcp;
    /**
     * With this Byte cursor visibility is controlled
     */
    private byte displayControl;

    /**
     * Creates a new LCD Display component using the default setup.
     *
     * @param pi4j Pi4J context
     */
    public LcdDisplayComponent(Context pi4j) {
        this(pi4j, DEFAULT_BUS, DEFAULT_DEVICE);
    }

    /**
     * Creates a new LCD Display component with custom bus, device address
     *
     * @param pi4j   Pi4J context
     * @param bus    Custom I2C bus address
     * @param device Custom device address on I2C
     */
    public LcdDisplayComponent(Context pi4j, int bus, int device) {
        this.mcp = new MCP23008(pi4j, bus, device);
        this.mcp.initializeIo(MCP_IO_CONFIG);
    }

    /**
     * Initializes the LCD Display
     */
    public void initialize() {
        // Initialize display
        write((byte) 0b001_10011);
        write((byte) 0b001_10010);

        // Initialize display settings
        this.displayControl = (byte) (LCD_DISPLAY_ON | LCD_CURSOR_OFF | LCD_BLINK_OFF);
        byte displayFunction = (byte) (LCD_4BIT_MODE | LCD_1LINE | LCD_2LINE | LCD_5x8DOTS);
        byte displayMode = (byte) (LCD_ENTRY_LEFT | LCD_ENTRY_SHIFT_DECREMENT);

        // Write Display settings
        executeCommand(LCD_DISPLAY_CONTROL, displayControl);
        write((byte) (LCD_FUNCTION_SET | displayFunction));
        write((byte) (LCD_ENTRY_MODE_SET | displayMode));

        // Clear display
        clearDisplay();

        // Enable backlight
        setDisplayBacklight(true);
    }

    /**
     * Write a Line of Text on the LCD Display
     *
     * @param text Text to display
     * @param line Select Line of Display
     */
    public void writeLine(String text, int line) {
        if (text.length() > 16) {
            throw new IllegalArgumentException("Too long text. Only 16 characters possible: " + text);
        }

        System.out.println("Writing '" + text + "' to line " + line);

        clearLine(line);
        moveCursorHome();
        setCursorToLine(line);

        for (int i = 0; i < text.length(); i++) {
            write(LcdSymbol.getByChar(text.charAt(i)), true);
        }
    }

    /**
     * Returns the Cursor to Home Position (First line, first character)
     */
    public void moveCursorHome() {
        write(LCD_RETURN_HOME);
        SleepHelper.sleep(3);
    }

    /**
     * Set the cursor to line 1 or 2
     *
     * @param line Sets the cursor to this line. Only Range 1-2 allowed.
     */
    public void setCursorToLine(int line) {
        if (line > 2 || line < 1) {
            throw new IllegalArgumentException("CrowPi Display has only 2 Rows!");
        }

        executeCommand(LCD_SET_DDRAM_ADDR, LCD_ROW_OFFSETS[line - 1]);
    }

    /**
     * Enable and Disable the Backlight of the LCD Display
     *
     * @param state Set Backlight ON or OFF
     */
    public void setDisplayBacklight(boolean state) {
        mcp.setAndWritePin(LCD_LIGHT, state);
    }

    /**
     * Clears the display and return the cursor to home
     */
    public void clearDisplay() {
        write(LCD_CLEAR_DISPLAY);
        SleepHelper.sleep(3);
        moveCursorHome();
    }

    /**
     * Clears a line of the display
     *
     * @param line Select line to clear
     */
    public void clearLine(int line) {
        setCursorToLine(line);

        for (int i = 0; i < 16; i++) {
            write(' ', true);
        }
    }

    /**
     * Execute Display commands
     *
     * @param command Select the LCD Command
     * @param data    Setup command data
     */
    protected void executeCommand(byte command, byte data) {
        write((byte) (command | data));
    }

    /**
     * Write a number (byte) to the LCD Display
     *
     * @param c Number to write to the Display
     */
    protected void write(int c) {
        write(c, false);
    }

    /**
     * Write a Number (byte) or character according to the LCD Display
     *
     * @param b        Data to write to the display
     * @param charMode Select data is a number or character
     */
    protected void write(int b, boolean charMode) {
        b &= 0xFF;
        mcp.setAndWritePin(LCD_RS, charMode);

        // high nibble
        mcp.setPin(LCD_D4, (b & 0b0001_0000) > 0);
        mcp.setPin(LCD_D5, (b & 0b0010_0000) > 0);
        mcp.setPin(LCD_D6, (b & 0b0100_0000) > 0);
        mcp.setPin(LCD_D7, (b & 0b1000_0000) > 0);
        mcp.writePins();
        mcp.pulsePin(LCD_EN, 1);

        // low nibble
        mcp.setPin(LCD_D4, (b & 0b0000_0001) > 0);
        mcp.setPin(LCD_D5, (b & 0b0000_0010) > 0);
        mcp.setPin(LCD_D6, (b & 0b0000_0100) > 0);
        mcp.setPin(LCD_D7, (b & 0b0000_1000) > 0);
        mcp.writePins();
        mcp.pulsePin(LCD_EN, 1);
    }
}
