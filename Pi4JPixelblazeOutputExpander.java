///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS org.slf4j:slf4j-api:1.7.35
//DEPS org.slf4j:slf4j-simple:1.7.35
//DEPS com.pi4j:pi4j-core:2.3.0
//DEPS com.pi4j:pi4j-plugin-raspberrypi:2.3.0
//DEPS com.pi4j:pi4j-plugin-pigpio:2.3.0

import com.pi4j.Pi4J;
import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.gpio.digital.DigitalState;
import com.pi4j.io.gpio.digital.PullResistance;
import com.pi4j.util.Console;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.CRC32;

import com.fazecast.jSerialComm.SerialPort;

/**
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

    public static void main(String[] args) throws Exception {

        final var console = new Console();

        var pi4j = Pi4J.newAutoContext();



        pi4j.shutdown();
    }

    ExpanderDataWriteAdapter writeAdapter;

    public ExpanderDriver(ExpanderDataWriteAdapter adapter) {
        this.writeAdapter = adapter;
    }

    public synchronized void sendWs2812(int channel, int bytesPerPixel, int rIndex, int gIndex, int bIndex, int wIndex, byte[] pixelData) {
        checkArgument(bytesPerPixel == 3 || bytesPerPixel == 4);
        checkArgument(rIndex <= 3);
        checkArgument(gIndex <= 3);
        checkArgument(bIndex <= 3);
        checkArgument(wIndex <= 3);
        checkNotNull(pixelData);

        int pixels = pixelData.length / bytesPerPixel;
        CRC32 crc = new CRC32();
        crc.reset();
        ByteBuffer buffer = initHeaderBuffer(10, (byte) channel, CH_WS2812_DATA);
        buffer.put((byte) bytesPerPixel);
        buffer.put((byte) (rIndex | (gIndex << 2) | (bIndex << 4) | (wIndex << 6)));
        buffer.putShort((short) pixels);
        byte[] bytes = buffer.array();

        crc.update(bytes);
        writeAdapter.write(bytes);

        crc.update(pixelData);
        writeAdapter.write(pixelData);

        writeCrc(crc);
    }

    private void writeCrc(CRC32 crc) {
        byte[] crcBytes = new byte[4];
        packInt(crcBytes, 0, (int) crc.getValue());
        writeAdapter.write(crcBytes);
    }

    public synchronized void sendDrawAll() {
        CRC32 crc = new CRC32();
        crc.reset();
        ByteBuffer buffer = initHeaderBuffer(6, (byte) 0xff, CH_DRAW_ALL);
        byte[] bytes = buffer.array();
        crc.update(bytes);
        writeAdapter.write(bytes);
        writeCrc(crc);
    }

    void packInt(byte[] outgoing, int index, int val) {
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

    class ExpanderDataWriteAdapter {

        private SerialPort port = null;
        private final String portPath;
        private boolean debug = false;

        public SerialPortAdapter(String portPath, boolean debug) {
            this.debug = debug;
            this.portPath = portPath;
        }

        private void openPort() {
            if (port != null)
                port.closePort();
            port = null; //set to null in case getCommPort throws, port will remain null.
            port = SerialPort.getCommPort(this.portPath);
            port.setBaudRate(2000000);
            port.setComPortTimeouts(SerialPort.TIMEOUT_NONBLOCKING, 0, 0);
            port.openPort(0, 8192, 8192);
            System.out.println("Opening " + portPath);
        }

        public void write(byte[] data) {
            int lastErrorCode = port != null ? port.getLastErrorCode() : 0;
            boolean isOpen = port != null && port.isOpen();
            if (port == null || !isOpen || lastErrorCode != 0) {
                System.out.println("port was open:" + isOpen + " last error:" + lastErrorCode);
                openPort();
            }
            port.writeBytes(data, data.length);
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