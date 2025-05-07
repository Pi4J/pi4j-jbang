///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS com.fazecast:jSerialComm:2.10.2
//DEPS org.openjfx:javafx-controls:20.0.2
//DEPS org.openjfx:javafx-graphics:20.0.2:${os.detected.jfxname}
//DEPS com.fasterxml.jackson.core:jackson-annotations:2.14.1
//DEPS com.fasterxml.jackson.core:jackson-core:2.14.1
//DEPS com.fasterxml.jackson.core:jackson-databind:2.14.1

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.Serial;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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
    final XYChart.Series<String, Integer> data = new XYChart.Series<>();

    @Override
    public void start(Stage stage) {
        // Depending on the type of board and the connection you are using
        // (GPIO pin, or other serial connection), this can be a different port.
        // Most probably it will be `/dev/ttyS0` (Raspberry Pi 4 or earlier),
        // or `/dev/ttyAMA0` (Raspberry Pi 5).

        SerialHelper serialHelper = new SerialHelper("dev/ttyS0");
        Thread t = new Thread(serialHelper);
        t.start();
        
        Scene scene = new Scene(new MeasurementChart(), 400, 700);
        stage.setTitle("Arduino sensor chart");
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

    class MeasurementChart extends VBox {
        /**
         * Constructor which will build the UI with the chart
         * and start the serial communication
         *
         * @param serialDevice the serial device
         */
        public MeasurementChart() {
            // Initialize the data holder for the chart
            XYChart.Series<String, Integer> data = new XYChart.Series<>();
            data.setName("Value");

            // Initialize the chart
            CategoryAxis xAxis = new CategoryAxis();
            xAxis.setLabel("Time");
            NumberAxis yAxis = new NumberAxis();
            yAxis.setLabel("Value");

            LineChart lineChart = new LineChart(xAxis, yAxis);
            lineChart.setTitle("Light measurement");

            lineChart.getData().add(data);

            this.getChildren().add(lineChart);            
        }
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

    class SerialHelper implements Runnable {
        private static int INTERVAL_SEND_SECONDS = 5;

        private SerialPort port = null;
        private final String portPath;

        public SerialHelper(String portPath) {
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
                port.setBaudRate(38400);
                port.setComPortTimeouts(SerialPort.TIMEOUT_NONBLOCKING, 0, 0);
                port.openPort(0, 8192, 8192);
                port.addDataListener(new SerialListener());
                System.out.println("Opening " + portPath);
            } catch (Exception e) {
                System.err.println("Could not open serial port " + e.getMessage());
            }
        }

        @Override
        public void run() {
            // Keep looping until an error occurs
            boolean keepRunning = true;
            while (keepRunning) {
                try {
                    // Write a text to the Arduino, as demo
                    write("Timestamp: " + System.currentTimeMillis());

                    // Wait predefined time for next loop
                    Thread.sleep(INTERVAL_SEND_SECONDS * 1000);
                } catch (Exception ex) {
                    System.err.println("Error: " + ex.getMessage());
                    keepRunning = false;
                }
            }
        }

        public void write(String data) {
            write(data.getBytes());
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

    class SerialListener implements SerialPortDataListener {
        private final DateTimeFormatter formatter;

        /**
         * Constructor which initializes the date formatter.
         *
         * @param data The data series to which the light values must be added
         */
        public SerialListener() {
            this.formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
        }

        @Override
        public int getListeningEvents() {
            return SerialPort.LISTENING_EVENT_DATA_RECEIVED;
        }

        @Override
        public void serialEvent(SerialPortEvent event) {
            if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_RECEIVED) {
                return;
            }

            try {
                byte[] newData = event.getReceivedData();
                String received = new String(newData)
                        .replace("\t", "")
                        .replace("\n", "");

                ArduinoMessage arduinoMessage = ArduinoMessageMapper.map(received);
                String timestamp = LocalTime.now().format(formatter);

                if (arduinoMessage != null && arduinoMessage.type.equals("light")) {
                    // We need to use the runLater approach as this data is handled
                    // in another thread as the UI-component
                    Platform.runLater(() -> {
                        data.getData().add(
                                new XYChart.Data<>(timestamp, arduinoMessage.getIntValue())
                        );
                    });
                }

                System.out.println(timestamp + " - Received: " + received);
            } catch (Exception ex) {
                System.err.println("Serial error: " + ex.getMessage());
            }
        }
    }
}