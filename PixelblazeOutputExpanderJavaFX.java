///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS com.fazecast:jSerialComm:2.10.2
//DEPS org.openjfx:javafx-controls:20.0.2
//DEPS org.openjfx:javafx-graphics:20.0.2:${os.detected.jfxname}

import com.fazecast.jSerialComm.SerialPort;
import javafx.application.Application;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.paint.Color;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;

/**
 * Example code to use a Pixelblaze Output Expander to send an 8*32 image to a LED matrix.
 * This example is based on PixelblazeOutputExpander.java, so please check its documentation first!
 *
 * Also based on this video: https://www.youtube.com/watch?v=9q-_QhT1fj4
 *
 * An SDK with bundled JavaFX is needed for this example, use SDKMAN to use a specific version:
 * curl -s "https://get.sdkman.io" | bash
 * source "$HOME/.sdkman/bin/sdkman-init.sh"
 * sdk install java 20.0.2.fx-librca 
 * 
 * This example can be executed without sudo:
 * jbang PixelblazeOutputExpanderJavaFX.java
 */
public class PixelblazeOutputExpanderJavaFX extends Application {

    private static final int CHANNEL = 0;
    private static final int NUMBER_OF_LEDS = 11;

    private static final byte CH_WS2812_DATA = 1;
    private static final byte CH_DRAW_ALL = 2;

    private List<ColorPicker> colorPickers = new ArrayList<>();
    private SerialHelper serialHelper;

    @Override
    public void start(Stage stage) {
        serialHelper = new SerialHelper();

        VBox holder = new VBox();
        holder.setSpacing(5);

        for (int led = 0; led < NUMBER_OF_LEDS; led++) {
            ColorPicker colorPicker = new ColorPicker();
            colorPicker.setPrefWidth(150);
            colorPicker.setOnAction(e -> sendColors());
            holder.getChildren().add(colorPicker);
            colorPickers.add(colorPicker);
        }

        Button clearAll = new Button("Clear all");
        clearAll.setPrefWidth(150);
        clearAll.setOnAction(e -> {
            Thread t = new Thread(() -> serialHelper.sendAllOff());
            t.start();
        });
        holder.getChildren().add(clearAll);

        Scene scene = new Scene(new StackPane(holder), 640, 480);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop(){
        System.out.println("Stage is closing");
        serialHelper.closePort();
    }

    private void sendColors() {
        byte[] colors = new byte[NUMBER_OF_LEDS * 3];
        for (int led = 0; led < NUMBER_OF_LEDS; led++) {
            Color c = colorPickers.get(led).getValue();
            colors[3 * led] = (byte) (255 * c.getRed());
            colors[(3 * led) + 1] = (byte) (255 * c.getGreen());
            colors[(3 * led) + 2] = (byte) (255 * c.getBlue());
        }
        Thread t = new Thread(() -> serialHelper.sendColors(CHANNEL, 3, 1, 0, 2, 0, colors, true));
        t.start();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        launch();
    }

    private class SerialHelper {

        private final ExpanderDataWriteAdapter adapter;

        private SerialHelper() {
            System.out.println("Initializing serial");
            adapter = new ExpanderDataWriteAdapter("/dev/ttyS0");
        }

        public void sendAllOff() {
            System.out.println("All off");
            sendColors(CHANNEL, 3, 1, 0, 2, 0, new byte[NUMBER_OF_LEDS * 3], false);
        }

        public void sendColors(int channel, int bytesPerPixel, int rIndex, int gIndex, int bIndex, int wIndex, byte[] pixelData, boolean debug) {
            System.out.println("Sending colors on channel " + channel);

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

            if (debug) {
                // Output the RGB byte array for testing
                // This slows down the execution of the application!
                for (int i = 0; i < pixelData.length; i++) {
                    System.out.printf("%02x ", pixelData[i]);
                    if (i % 12 == 11) {
                        System.out.print("\n");
                    } else if (i % 4 == 3) {
                        System.out.print("\t");
                    }
                }
                System.out.print("\n");
            }

            crc.update(bytes);
            adapter.write(bytes);

            crc.update(pixelData);
            adapter.write(pixelData);

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
            outgoing[index++] = (byte) (val & 0xFF); val = val >> 8;
            outgoing[index++] = (byte) (val & 0xFF); val = val >> 8;
            outgoing[index++] = (byte) (val & 0xFF); val = val >> 8;
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

        private class ExpanderDataWriteAdapter {

            private SerialPort port = null;
            private final String portPath;

            public ExpanderDataWriteAdapter (String portPath) {
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
}