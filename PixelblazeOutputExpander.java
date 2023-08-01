///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS com.fazecast:jSerialComm:2.10.2

import com.fazecast.jSerialComm.SerialPort;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channel;
import java.util.Random;
import java.util.zip.CRC32;

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
 * Status of the Pixelblaze Output Expander LEDs
 *
 * <ul>
 *     <li>Fading / pulsing orange: has not seen any valid looking data</li>
 *     <li>Solid orange (for short time): received expander data</li>
 *     <li>Green LED (for short time): received data for its channels and is drawing</li>
 * </ul>
 *
 * Enabling serial port on the Raspberry Pi to be used by software
 *
 * <ul>
 *     <li>In terminal: sudo raspi-config</li>
 *     <li>Go to "Interface Options"</li>
 *     <li>Go to "Serial Port"</li>
 *     <li>Select "No" for "login shell"</li>
 *     <li>Select "Yes" for "hardware enabled"</li>
 * </ul>
 * 
 */
public class PixelblazeOutputExpander {

    private static final byte CH_WS2812_DATA = 1;
    private static final byte CH_DRAW_ALL = 2;
    private static final byte CH_APA102_DATA = 3;
    private static final byte CH_APA102_CLOCK = 4;

    private static final int NUMBER_OF_LEDS = 11;

    private static ExpanderDataWriteAdapter adapter;

    public static void main(String[] args) throws InterruptedException {
        adapter = new ExpanderDataWriteAdapter("/dev/ttyS0");

        // All off
        sendAllOff(0, NUMBER_OF_LEDS);
        Thread.sleep(500);

        // One by one red
        System.out.println("One by one red");
        for (int i = 0; i < NUMBER_OF_LEDS; i++) {
            byte[] oneRed = new byte[NUMBER_OF_LEDS * 3];
            oneRed[i * 3] = (byte) 0xff;
            sendWs2812(0, 3, 1, 0, 2, 0, oneRed);
            sendDrawAll();
            Thread.sleep(250);
        }

        // All same color red, green, blue
        for (int color = 0; color < 3; color++) {
            System.out.println("All " + (color == 0 ? "red" : (color == 1 ? "green" : "blue")));
            byte[] allSame = new byte[NUMBER_OF_LEDS * 3];
            for (int i = 0; i < NUMBER_OF_LEDS; i++) {                
                allSame[(3 * i) + color] = (byte) 0xff;
            }
            sendWs2812(0, 3, 1, 0, 2, 0, allSame);
            sendDrawAll();

            Thread.sleep(1000);
        }

        // Fill strip with random colors        
        Random rd = new Random();
        for (int i = 0; i < 5; i++) {
            System.out.println("Random colors " + (i + 1));
            byte[] random = new byte[NUMBER_OF_LEDS * 3];
            rd.nextBytes(random);
            sendWs2812(0, 3, 1, 0, 2, 0, random);
            sendDrawAll();

            Thread.sleep(1000);
        }

        // Red alert!
        byte[] red = new byte[NUMBER_OF_LEDS * 3];
        int i;
        for (i = 0; i < NUMBER_OF_LEDS; i++) {
            red[i*3]= (byte) 0xff;
        }
        for (i = 0; i < 5; i++) {
            System.out.println("All red");
            sendWs2812(0, 3, 1, 0, 2, 0, red);
            sendDrawAll();
            Thread.sleep(100);
            sendAllOff(0, NUMBER_OF_LEDS);
            Thread.sleep(100);
        }

        // Send to LED strip on Channel 1, 5 meter with 60 LEDs/meter
        byte[] redFiveMeter = new byte[300 * 3];
        for (i = 0; i < 300; i++) {
            redFiveMeter[i*3]= (byte) 0xff;
        }
        for (i = 0; i < 5; i++) {
            System.out.println("All red on LED strip on channel 1");
            sendWs2812(1, 3, 1, 0, 2, 0, redFiveMeter);
            sendDrawAll();
            Thread.sleep(100);
            sendAllOff(1, 300);
            Thread.sleep(100);
        }

        // Send to 8*32 LED matrix on Channel 2
        byte[] redMatrix = new byte[300 * 3];
        for (i = 0; i < 300; i++) {
            redMatrix[i*3]= (byte) 0xff;
        }
        for (i = 0; i < 5; i++) {
            System.out.println("All red on LED matrix on channel 2");
            sendWs2812(2, 3, 1, 0, 2, 0, redMatrix);
            sendDrawAll();
            Thread.sleep(100);
            sendAllOff(2, 300);
            Thread.sleep(100);
        }

        adapter.closePort();
    }

    private static void sendAllOff(int channel, int numberOfLeds) {
        System.out.println("All off on channel " + channel + " for " + numberOfLeds + " LEDs");
        sendWs2812(channel, 3, 1, 0, 2, 0, new byte[numberOfLeds * 3]);
        sendDrawAll();
    }

    private static void sendWs2812(int channel, int bytesPerPixel, int rIndex, int gIndex, int bIndex, int wIndex, byte[] pixelData) {
        if (bytesPerPixel != 3 && bytesPerPixel != 4) {
            System.out.println("bytesPerPixel not within expected range");
            return;
        }
        if (rIndex > 3 || gIndex > 3 || bIndex > 3 || wIndex > 3) {
            System.out.println("one or more indexes not within expected range");
            return;
        }
        if (pixelData == null) {
            System.out.println("pixelData can not be null");
            return;
        }

        int pixels = pixelData.length / bytesPerPixel;
        CRC32 crc = new CRC32();
        crc.reset();
        ByteBuffer buffer = initHeaderBuffer(10, (byte) channel, CH_WS2812_DATA);
        buffer.put((byte) bytesPerPixel);
        buffer.put((byte) (rIndex | (gIndex << 2) | (bIndex << 4) | (wIndex << 6)));
        buffer.putShort((short) pixels);
        byte[] bytes = buffer.array();

        for (int i = 0; i < pixelData.length; i++) {
            System.out.printf("%02x ", pixelData[i]);
            //if (i % 12 == 11) {
            //    System.out.print("\n");
            //} else if (i % 4 == 3) {
            //    System.out.print("\t");
            //}
        }
        System.out.print("\n");

        crc.update(bytes);
        adapter.write(bytes);

        crc.update(pixelData);
        adapter.write(pixelData);

        writeCrc(crc);
    }

    public static void sendDrawAll() {
        CRC32 crc = new CRC32();
        crc.reset();
        ByteBuffer buffer = initHeaderBuffer(6, (byte) 0xff, CH_DRAW_ALL);
        byte[] bytes = buffer.array();
        crc.update(bytes);
        adapter.write(bytes);
        writeCrc(crc);
    }

    private static void writeCrc(CRC32 crc) {
        byte[] crcBytes = new byte[4];
        packInt(crcBytes, 0, (int) crc.getValue());
        adapter.write(crcBytes);
    }

    private static void packInt(byte[] outgoing, int index, int val) {
        outgoing[index++] = (byte) (val & 0xFF); val = val >> 8;
        outgoing[index++] = (byte) (val & 0xFF); val = val >> 8;
        outgoing[index++] = (byte) (val & 0xFF); val = val >> 8;
        outgoing[index] = (byte) (val & 0xFF);
    }

    private static ByteBuffer initHeaderBuffer(int size, byte channel, byte command) {
        ByteBuffer buffer = ByteBuffer.allocate(size);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        buffer.put((byte) 'U');
        buffer.put((byte) 'P');
        buffer.put((byte) 'X');
        buffer.put((byte) 'L');
        buffer.put(channel);
        buffer.put(command);
        return buffer;
    }

    static class ExpanderDataWriteAdapter {

        private SerialPort port = null;
        private final String portPath;

        public ExpanderDataWriteAdapter (String portPath) {
            this.portPath = portPath;
        }

        private void openPort() {
            if (port != null) {
                System.out.println("Closing " + portPath);
                port.closePort();
            }
            port = null; //set to null in case getCommPort throws, port will remain null.
            port = SerialPort.getCommPort(this.portPath);
            port.setBaudRate(2000000);
            port.setComPortTimeouts(SerialPort.TIMEOUT_NONBLOCKING, 0, 0);
            port.openPort(0, 8192, 8192);
            System.out.println("Opening " + portPath);
        }

        public void closePort() {
            if (port != null) {
                System.out.println("Closing " + portPath);
                port.closePort();
            }
        }

        public void write(byte[] data) {
            int lastErrorCode = port != null ? port.getLastErrorCode() : 0;
            boolean isOpen = port != null && port.isOpen();
            if (port == null || !isOpen || lastErrorCode != 0) {
                System.out.println("Port was open:" + isOpen + ", last error:" + lastErrorCode);
                openPort();
            }
            port.writeBytes(data, data.length);
        }
    }
}