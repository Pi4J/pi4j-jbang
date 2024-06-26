package helper;

import com.fazecast.jSerialComm.SerialPort;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.CRC32;

public class PixelBlazeOutputExpanderHelper {

    private static final byte CH_WS2812_DATA = 1;
    private static final byte CH_DRAW_ALL = 2;

    private final ExpanderDataWriteAdapter adapter;

    public PixelBlazeOutputExpanderHelper(String address) {
        System.out.println("Initializing serial");
        adapter = new ExpanderDataWriteAdapter(address);
    }

    public void sendAllOff(int channel, int numberOfLeds) {
        System.out.println("All off on channel " + channel + " with " + numberOfLeds);
        sendColors(channel, 3, 1, 0, 2, 0, new byte[numberOfLeds * 3], false);
    }

    public void sendColors(int channel, byte[] rgbPerPixel, boolean debug) {
        sendColors(channel, 3, rgbPerPixel, debug);
    }

    public void sendColors(int channel, int bytesPerPixel, byte[] rgbPerPixel, boolean debug) {
        sendColors(channel, bytesPerPixel, 1, 0, 2, 0, rgbPerPixel, debug);
    }

    public void sendColors(int channel, int bytesPerPixel, int rIndex, int gIndex, int bIndex, int wIndex,
            byte[] rgbPerPixel, boolean debug) {
        if (debug) {
            System.out.println("Sending colors on channel " + channel);
        }

        if (bytesPerPixel != 3 && bytesPerPixel != 4) {
            System.out.println("bytesPerPixel not within expected range");
            return;
        }
        if (rIndex > 3 || gIndex > 3 || bIndex > 3 || wIndex > 3) {
            System.out.println("one or more indexes not within expected range");
            return;
        }
        if (rgbPerPixel == null) {
            System.out.println("rgbPerPixel can not be null");
            return;
        }

        int pixels = rgbPerPixel.length / bytesPerPixel;
        CRC32 crc = new CRC32();
        crc.reset();
        ByteBuffer headerBuffer = initHeaderBuffer(10, (byte) channel, CH_WS2812_DATA);
        headerBuffer.put((byte) bytesPerPixel);
        headerBuffer.put((byte) (rIndex | (gIndex << 2) | (bIndex << 4) | (wIndex << 6)));
        headerBuffer.putShort((short) pixels);
        byte[] header = headerBuffer.array();

        if (debug) {
            // Output the RGB byte array for testing
            // This slows down the execution of the application!
            for (int i = 0; i < rgbPerPixel.length; i++) {
                System.out.printf("%02x ", rgbPerPixel[i]);
                if (i % 12 == 11) {
                    System.out.print("\n");
                } else if (i % 4 == 3) {
                    System.out.print("\t");
                }
            }
            System.out.print("\n");
        }

        crc.update(header);
        adapter.write(header);

        crc.update(rgbPerPixel);
        adapter.write(rgbPerPixel);

        writeCrc(crc);

        sendDrawAll();
    }

    public void closePort() {
        adapter.closePort();
    }

    private void sendDrawAll() {
        CRC32 crc = new CRC32();
        crc.reset();
        ByteBuffer buffer = initHeaderBuffer(6, (byte) 0xff, CH_DRAW_ALL);
        byte[] bytes = buffer.array();
        crc.update(bytes);
        adapter.write(bytes);
        writeCrc(crc);
    }

    private void writeCrc(CRC32 crc) {
        byte[] crcBytes = new byte[4];
        packInt(crcBytes, 0, (int) crc.getValue());
        adapter.write(crcBytes);
    }

    private void packInt(byte[] outgoing, int index, int val) {
        outgoing[index++] = (byte) (val & 0xFF);
        val = val >> 8;
        outgoing[index++] = (byte) (val & 0xFF);
        val = val >> 8;
        outgoing[index++] = (byte) (val & 0xFF);
        val = val >> 8;
        outgoing[index] = (byte) (val & 0xFF);
    }

    private ByteBuffer initHeaderBuffer(int size, byte channel, byte command) {
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

    private static class ExpanderDataWriteAdapter {

        private SerialPort port = null;
        private final String portPath;

        public ExpanderDataWriteAdapter(String portPath) {
            this.portPath = portPath;
            openPort();
        }

        private void openPort() {
            if (port != null) {
                System.out.println("Closing " + portPath);
                port.closePort();
            }
            try {
                port = null; //set to null in case getCommPort throws, port will remain null.
                port = SerialPort.getCommPort(this.portPath);
                port.setBaudRate(2000000);
                port.setComPortTimeouts(SerialPort.TIMEOUT_NONBLOCKING, 0, 0);
                port.openPort(0, 8192, 8192);
                System.out.println("Opening " + portPath);
            } catch (Exception e) {
                System.err.println("Could not open serial port " + e.getMessage());
            }
        }

        private void closePort() {
            if (port != null) {
                System.out.println("Closing " + portPath);
                try {
                    // Make sure all pending commands are sent before closing the port
                    port.getOutputStream().flush();
                } catch (IOException e) {
                    System.err.println("Error while flushing the data: " + e.getMessage());
                }
                port.closePort();
            }
        }

        public void write(byte[] data) {
            int lastErrorCode = port != null ? port.getLastErrorCode() : 0;
            int lastErrorLocation = port != null ? port.getLastErrorLocation() : 0;
            boolean isOpen = port != null && port.isOpen();
            if (port == null || !isOpen || lastErrorCode != 0) {
                System.out.println("Port was open:" + isOpen + ", last error:" + lastErrorCode + " " + lastErrorLocation);
                openPort();
            }
            port.writeBytes(data, data.length);
        }
    }
}
