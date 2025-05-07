///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS com.fazecast:jSerialComm:2.10.2
//DEPS org.openjfx:javafx-controls:20.0.2
//DEPS org.openjfx:javafx-graphics:20.0.2:${os.detected.jfxname}

import javafx.application.Application;
import javafx.scene.control.Label;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * Example code to read a sensor value sent by an Arduino via serial.
 *
 * An SDK with bundled JavaFX is needed for this example, use SDKMAN to use a specific version:
 * curl -s "https://get.sdkman.io" | bash
 * source "$HOME/.sdkman/bin/sdkman-init.sh"
 * sdk install java 22.0.1.fx-zulu
 * 
 * This example can be executed without sudo:
 * jbang PixelblazeOutputExpanderJavaFX.java
 */
public class SerialSensorJavaFX extends Application {

    @Override
    public void start(Stage stage) {
        // Depending on the type of board and the connection you are using
        // (GPIO pin, or other serial connection), this can be a different port.
        // Most probably it will be `/dev/ttyS0` (Raspberry Pi 4 or earlier),
        // or `/dev/ttyAMA0` (Raspberry Pi 5).
        


        Scene scene = new Scene(new Labe("Test"), 400, 700);
        stage.setTitle("Pixelblaze Test");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop(){
        System.out.println("Stage is closing");
    }

    public static void main(String[] args) {
        launch();
    }

    class ArduinoMessage {
        @JsonProperty("type")
        public String type;

        @JsonProperty("value")
        public String value;

        public Integer getIntValue() {
            if (this.value.matches("-?(0|[1-9]\\d*)")) {
                return Integer.parseInt(this.value);
            }
            return null;
        }

        public Float getFloatValue() {
            if (this.value.matches("[-+]?[0-9]*\\.?[0-9]+")) {
                return Float.parseFloat(this.value);
            }
            return null;
        }
    }

    class ArduinoMessageMapper {
        public static ArduinoMessage map(String jsonString) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                mapper.configure(
                        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false
                );
                return mapper.readValue(jsonString, ArduinoMessage.class);
            } catch (IOException ex) {
                System.err.println("Unable to parse string to Forecast: "
                        + ex.getMessage());
                return null;
            }
        }
    }

    class SerialSender implements Runnable {
        private static int INTERVAL_SEND_SECONDS = 5;

        final Serial serial;

        /**
         * Constructor which gets the serial object to be used to send data.
         *
         * @param serial
         */
        public SerialSender(Serial serial) {
            this.serial = serial;
        }

        @Override
        public void run() {
            // Keep looping until an error occurs
            boolean keepRunning = true;
            while (keepRunning) {
                try {
                    // Write a text to the Arduino, as demo
                    this.serial.writeln("Timestamp: " + System.currentTimeMillis());

                    // Wait predefined time for next loop
                    Thread.sleep(INTERVAL_SEND_SECONDS * 1000);
                } catch (Exception ex) {
                    System.err.println("Error: " + ex.getMessage());
                    keepRunning = false;
                }
            }
        }
    }

    class SerialListener implements SerialDataEventListener {
        private final DateTimeFormatter formatter;
        private final XYChart.Series<String, Integer> data;

        /**
         * Constructor which initializes the date formatter.
         *
         * @param data The data series to which the light values must be added
         */
        public SerialListener(XYChart.Series<String, Integer> data) {
            this.data = data;
            this.formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
        }

        /**
         * Called by Serial when new data is received.
         */
        @Override
        public void dataReceived(SerialDataEvent event) {
            try {
                String received = event.getAsciiString()
                        .replace("\t", "")
                        .replace("\n", "");

                ArduinoMessage arduinoMessage = ArduinoMessageMapper.map(received);
                String timestamp = LocalTime.now().format(formatter);

                if (arduinoMessage.type.equals("light")) {
                    // We need to use the runLater approach as this data is handled
                    // in another thread as the UI-component
                    Platform.runLater(() -> {
                        data.getData().add(
                                new XYChart.Data(timestamp, arduinoMessage.getIntValue())
                        );
                    });
                }

                System.out.println(timestamp + " - Received: " + received);
            } catch (IOException ex) {
                System.err.println("Serial error: " + ex.getMessage());
            }
        }
    }
}