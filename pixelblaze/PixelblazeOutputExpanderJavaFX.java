package pixelblaze; /// usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS com.fazecast:jSerialComm:2.10.2
//DEPS org.openjfx:javafx-controls:20.0.2
//DEPS org.openjfx:javafx-graphics:20.0.2:${os.detected.jfxname}
//SOURCES i2c.helper/PixelBlazeOutputExpanderHelper.java

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import pixelblaze.helper.PixelBlazeOutputExpanderHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Example code to change the colors of a LED strip with a JavaFX User Interface.
 * This example is based on pixelblaze.PixelblazeOutputExpander.java, so please check its documentation first!
 * <p>
 * Also based on this video: https://www.youtube.com/watch?v=9q-_QhT1fj4
 * <p>
 * An SDK with bundled JavaFX is needed for this example, use SDKMAN to use a specific version:
 * curl -s "https://get.sdkman.io" | bash
 * source "$HOME/.sdkman/bin/sdkman-init.sh"
 * sdk install java 22.0.1.fx-zulu
 * <p>
 * This example can be executed without sudo:
 * jbang pixelblaze.PixelblazeOutputExpanderJavaFX.java
 */
public class PixelblazeOutputExpanderJavaFX extends Application {

    private static final int BYTES_PER_PIXEL = 3;
    private static final int CHANNEL = 0;
    private static final int NUMBER_OF_LEDS = 11;

    private final List<ColorPicker> colorPickers = new ArrayList<>();
    private PixelBlazeOutputExpanderHelper helper;

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) {
        // Depending on the type of board and the connection you are using
        // (GPIO pin, or other serial connection), this can be a different port.
        // Most probably it will be `/dev/ttyS0` (Raspberry Pi 4 or earlier),
        // or `/dev/ttyAMA0` (Raspberry Pi 5).
        helper = new PixelBlazeOutputExpanderHelper("/dev/ttyS0");

        VBox holder = new VBox();
        holder.setFillWidth(true);
        holder.setAlignment(Pos.CENTER);
        holder.setSpacing(5);

        // Color picker to control the LEDs one-by-one
        holder.getChildren().add(new Label("One by one"));
        for (int led = 0; led < NUMBER_OF_LEDS; led++) {
            ColorPicker colorPicker = new ColorPicker();
            colorPicker.setPrefWidth(200);
            colorPicker.setOnAction(e -> sendColors());
            holder.getChildren().add(colorPicker);
            colorPickers.add(colorPicker);
        }

        // Color picker to put the same color on all LEDs
        holder.getChildren().add(new Label("All same color"));
        ColorPicker colorPicker = new ColorPicker();
        colorPicker.setPrefWidth(200);
        colorPicker.setOnAction(e -> sendAll(colorPicker.getValue()));
        holder.getChildren().add(colorPicker);

        // Clear button
        holder.getChildren().add(new Label("Clear"));
        Button clearAll = new Button("All");
        clearAll.setPrefWidth(200);
        clearAll.setOnAction(e -> helper.sendAllOff(CHANNEL, NUMBER_OF_LEDS));
        holder.getChildren().add(clearAll);

        Scene scene = new Scene(new StackPane(holder), 400, 700);
        stage.setTitle("Pixelblaze Test");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        System.out.println("Stage is closing");
        helper.closePort();
    }

    private void sendColors() {
        byte[] colors = new byte[NUMBER_OF_LEDS * BYTES_PER_PIXEL];
        for (int led = 0; led < NUMBER_OF_LEDS; led++) {
            Color color = colorPickers.get(led).getValue();
            colors[BYTES_PER_PIXEL * led] = (byte) (255 * color.getRed());
            colors[(BYTES_PER_PIXEL * led) + 1] = (byte) (255 * color.getGreen());
            colors[(BYTES_PER_PIXEL * led) + 2] = (byte) (255 * color.getBlue());
        }
        helper.sendColors(CHANNEL, BYTES_PER_PIXEL, 1, 0, 2, 0, colors, true);
    }

    private void sendAll(Color color) {
        byte[] colors = new byte[NUMBER_OF_LEDS * 3];
        for (int led = 0; led < NUMBER_OF_LEDS; led++) {
            colors[BYTES_PER_PIXEL * led] = (byte) (255 * color.getRed());
            colors[(BYTES_PER_PIXEL * led) + 1] = (byte) (255 * color.getGreen());
            colors[(BYTES_PER_PIXEL * led) + 2] = (byte) (255 * color.getBlue());
        }
        helper.sendColors(CHANNEL, BYTES_PER_PIXEL, 1, 0, 2, 0, colors, true);
    }
}