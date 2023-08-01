///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS com.fazecast:jSerialComm:2.10.2

import com.fazecast.jSerialComm.SerialPort;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;
import java.util.zip.CRC32;

/**
 * Example code to use a Pixelblaze Output Expander to send an 8*32 image to a LED matrix.
 * This example is based on PixelblazeOutputExpander.java, so please check its documentation first!
 *
 * This example can be executed without sudo:
 * jbang PixelblazeOutputExpanderImageMatrix.java
 */
public class PixelblazeOutputExpanderImageMatrix {

    private static final int CHANNEL = 2;
    private static final int NUMBER_OF_LEDS = 8 * 32;

    private static final byte CH_WS2812_DATA = 1;
    private static final byte CH_DRAW_ALL = 2;

    private static ExpanderDataWriteAdapter adapter;

    private static final String[] IMAGES = {
            "image_8_32_red.png",
            "image_8_32_green.png",
            "image_8_32_blue.png",
            "image_8_32_stripes.png",
            "image_8_32_stripes_test.png",
            "image_8_32_line_1.png",
            "image_8_32_line_2.png",
            "image_8_32_line_3.png",
            "image_8_32_line_4.png",
            "image_8_32_line_5.png",
            "image_8_32_line_6.png",
            "image_8_32_line_7.png",
            "image_8_32_line_8.png",
            "image_8_32_duke.png",
            "image_8_32_raspberrypi.png"
    };

    public static void main(String[] args) throws IOException, InterruptedException {
        adapter = new ExpanderDataWriteAdapter("/dev/ttyS0");
        int i;

        // Clear any remaining LEDs from previous test
        sendAllOff();
        sendDrawAll();
        Thread.sleep(1000);

        // Check the position of the LEDs, to identify how the LED strip is wired
        for (i = 0; i < NUMBER_OF_LEDS; i++) {
            byte[] pixelData = new byte[NUMBER_OF_LEDS * 3];
            pixelData[i * 3] = (byte) 0xff; // red
            sendWs2812(CHANNEL, 3, 1, 0, 2, 1, pixelData);
            sendDrawAll();
            Thread.sleep(100);
        }

        // Output all defined images
        for (String image : IMAGES) {
            System.out.println("Image: " + image);

            // Get the bytes from the given image
            byte[] pixelData = imageToMatrix(getImageData("data/" + image));

            // Show the image on the LED matrix
            sendWs2812(CHANNEL, 3, 1, 0, 2, 1, pixelData);
            sendDrawAll();
            Thread.sleep(3000);

            // Clear the LEDs
            sendAllOff();
            Thread.sleep(200);
        }

        // Random colors
        Random rd = new Random();
        for (i = 0; i < 100; i++) {
            byte[] random = new byte[8 * 32 * 3];
            rd.nextBytes(random);
            sendWs2812(CHANNEL, 3, 1, 0, 2, 0, random);
            sendDrawAll();
            Thread.sleep(100);
        }
    }

    private static void sendAllOff() {
        System.out.println("All off");
        sendWs2812(CHANNEL, 3, 1, 0, 2, 0, new byte[NUMBER_OF_LEDS * 3]);
        sendDrawAll();
    }

    /**
     * BufferedImage consists of two main classes: Raster & ColorModel. Raster itself consists of two classes,
     * DataBufferByte for image content while the other for pixel color.
     *
     * @param imagePath
     * @return
     */
    private static byte[] getImageData(String imagePath) throws IOException {
        byte[] imageData = new byte[NUMBER_OF_LEDS * 3];

        // Open image
        File imgPath = new File(imagePath);
        BufferedImage bufferedImage = ImageIO.read(imgPath);

        // Read color values for each pixel
        int pixelCounter = 0;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 32; x++) {
                int color = bufferedImage.getRGB(x, y);
                byte blue = (byte) (color & 0xff);
                byte green = (byte) ((color & 0xff00) >> 8);
                byte red = (byte) ((color & 0xff0000) >> 16);
                imageData[pixelCounter * 3] = red;
                imageData[(pixelCounter * 3) + 1] = green;
                imageData[(pixelCounter * 3) + 2] = blue;
                pixelCounter++;
            }
        }

        return imageData;
    }

    /**
     * Image is read to byte array pixel per pixel for each row to get one continuous line of data.
     * But the matrix is wired in columns, first down, second up, third down,...
     *
     * So we need to "mix up" the image byte array to one that matches the coordinates on the matrix.
     *
     * @param imageData
     * @return
     */
    private static byte[] imageToMatrix(byte[] imageData) {
        byte[] matrixData = new byte[imageData.length];

        int imagePosition;
        int row = 0;
        int column = 0;
        for (int pixelCounter = 0; pixelCounter < NUMBER_OF_LEDS; pixelCounter++) {
            imagePosition = row * column;
            matrixData[pixelCounter * 3] = imageData[imagePosition];
            matrixData[(pixelCounter * 3) + 1] = imageData[(imagePosition * 3) + 1];
            matrixData[(pixelCounter * 3) + 2] = imageData[(imagePosition * 3) + 1];
            if (column % 2 == 0) {
                row++;
            } else {
                row--;
            }
            if (row < 0 || row > 7) {
                column++;
                if (column % 2 == 0) {
                    row = 0;
                } else {
                    row = 7;
                }
            }
        }

        return matrixData;
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

        //for (int i = 0; i < pixelData.length; i++) {
            //System.out.printf("%02x ", pixelData[i]);
            //if (i % 12 == 11) {
            //    System.out.print("\n");
            //} else if (i % 4 == 3) {
            //    System.out.print("\t");
            //}
        //}
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