///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS com.fazecast:jSerialComm:2.10.2

import com.fazecast.jSerialComm.SerialPort;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.CRC32;

/**
 * Example code to use a Pixelblaze to control a LED strip.
 * Make sure to follow the README of this project to learn more about JBang and how to install it.
 *
 * This example must be executed as sudo with:
 * sudo `which jbang` Pi4JPixelblazeOutputExpander.java
 * 
 * Thanks to Jeff Vyduna for his Java driver for the Output Expander that has been used in this example.
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
 * Status of the Pixelblaze LED
 *
 * <ul>
 *     <li>Fading / pulsing orange LED: has not seen any valid looking data</li>
 *     <li>Solid orange: received expander data</li>
 *     <li>Green LED: received data for its channels and is drawing</li>
 * </ul>
 *
 * Enable serial on the Raspberry Pi
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
public class Pi4JPixelblazeOutputExpander {

    // Wiring see:

    private static final byte CH_WS2812_DATA = 1;
    private static final byte CH_DRAW_ALL = 2;
    private static final byte CH_APA102_DATA = 3;
    private static final byte CH_APA102_CLOCK = 4;
    private static ExpanderDataWriteAdapter adapter;

    public static void main(String[] args) throws Exception {
        adapter = new ExpanderDataWriteAdapter("/dev/ttyS0", true);
        byte[] pixelData = new byte[]{(byte) 0xff, (byte) 0x00, (byte) 0x00};;

        sendWs2812(0, 3, 0, 0, 0, 0, pixelData);

        adapter.closePort();
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

        crc.update(bytes);
        adapter.write(bytes);

        crc.update(pixelData);
        adapter.write(pixelData);

        writeCrc(crc);
    }

    private static void writeCrc(CRC32 crc) {
        byte[] crcBytes = new byte[4];
        packInt(crcBytes, 0, (int) crc.getValue());
        adapter.write(crcBytes);
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
        private boolean debug = false;
        public ExpanderDataWriteAdapter (String portPath, boolean debug) {
            this.debug = debug;
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
            if (debug) {
                for (int i = 0; i < data.length; i++) {
                    System.out.printf("%02x ", data[i]);
                    if (i % 12 == 11) {
                        System.out.print("\n");
                    } else if (i % 4 == 3) {
                        System.out.print("\t");
                    }
                }
                System.out.print("\n");
            }
        }
    }
}