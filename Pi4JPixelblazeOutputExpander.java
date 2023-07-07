///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS org.slf4j:slf4j-api:1.7.35
//DEPS org.slf4j:slf4j-simple:1.7.35
//DEPS com.pi4j:pi4j-core:2.3.0
//DEPS com.pi4j:pi4j-plugin-raspberrypi:2.3.0
//DEPS com.pi4j:pi4j-plugin-pigpio:2.3.0

import com.pi4j.Pi4J;
import com.pi4j.io.serial.FlowControl;
import com.pi4j.io.serial.Parity;
import com.pi4j.io.serial.Serial;
import com.pi4j.io.serial.StopBits;
import com.pi4j.util.Console;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.CRC32;

/**
 * This example must be executed as sudo
 * sudo `which jbang` Pi4JPixelblazeOutputExpander.java
 * 
 * Thanks to Jeff Vyduna for his Java driver for the Output Expander that has been used in this example.
 *
 * Serial data format info: https://github.com/simap/pixelblaze_output_expander/tree/v3.x
 */
public class Pi4JPixelblazeOutputExpander {

    // Wiring see:

    private static final byte CH_WS2812_DATA = 1;
    private static final byte CH_DRAW_ALL = 2;
    private static final byte CH_APA102_DATA = 3;
    private static final byte CH_APA102_CLOCK = 4;
    private static Console console;
    private static ExpanderDataWriteAdapter adapter;

    public static void main(String[] args) throws Exception {
        console = new Console();
        var pi4j = Pi4J.newAutoContext();
        var serialConfig = Serial.newConfigBuilder(pi4j)
                .baud(2_000_000)
                .dataBits_8()
                .parity(Parity.NONE)
                .stopBits(StopBits._1)
                .flowControl(FlowControl.NONE)
                .id("my-serial")
                .device("/dev/ttyS0")
                .provider("pigpio-serial")
                .build();
        /*
        Config used by Jeff Vyduna
        port.setBaudRate(2_000_000);
        port.setComPortTimeouts(SerialPort.TIMEOUT_NONBLOCKING, 0, 0);
        port.openPort(0, 8192, 8192);
        */
        var serial = pi4j.create(serialConfig);
        serial.open();

        adapter = new ExpanderDataWriteAdapter(serial, true);
        byte[] pixelData = new byte[]{(byte) 0xff, (byte) 0x00, (byte) 0x00};;

        sendWs2812(0, 3, 0, 0, 0, 0, pixelData);

        pi4j.shutdown();
    }

    private static void sendWs2812(int channel, int bytesPerPixel, int rIndex, int gIndex, int bIndex, int wIndex, byte[] pixelData) {
        if (bytesPerPixel != 3 && bytesPerPixel != 4) {
            console.println("bytesPerPixel not within expected range");
            return;
        }
        if (rIndex > 3 || gIndex > 3 || bIndex > 3 || wIndex > 3) {
            console.println("one or more indexes not within expected range");
            return;
        }
        if (pixelData == null) {
            console.println("pixelData can not be null");
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

        private Serial serial;
        private boolean debug = false;

        public ExpanderDataWriteAdapter(Serial serial, boolean debug) {
            this.serial = serial;
            this.debug = debug;
        }

        public void write(byte[] data) {
            boolean isOpen = serial != null && serial.isOpen();
            if (serial == null || !isOpen) {
                console.println("Port open:" + isOpen);
                return;
            }
            // TODO serial.writeBytes(data, data.length);
            if (debug) {
                for (int i = 0; i < data.length; i++) {
                    System.out.printf("%02x ", data[i]);
                    if (i % 12 == 11)
                        System.out.print("\n");
                }
                System.out.print("\n");
            }
        }
    }
}