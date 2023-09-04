///usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS com.fazecast:jSerialComm:2.10.2
//DEPS org.openjfx:javafx-controls:20.0.2
//DEPS org.openjfx:javafx-graphics:20.0.2:${os.detected.jfxname}
//SOURCES helper/PixelBlazeOutputExpanderHelper.java

import helper.PixelBlazeOutputExpanderHelper;
import javafx.application.Application;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.paint.Color;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

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

    private List<ColorPicker> colorPickers = new ArrayList<>();
    private PixelBlazeOutputExpanderHelper helper;

    @Override
    public void start(Stage stage) {
        helper = new PixelBlazeOutputExpanderHelper("/dev/ttyS0");

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
            Thread t = new Thread(() -> helper.sendAllOff(CHANNEL, NUMBER_OF_LEDS));
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
        helper.closePort();
    }

    private void sendColors() {
        byte[] colors = new byte[NUMBER_OF_LEDS * 3];
        for (int led = 0; led < NUMBER_OF_LEDS; led++) {
            Color c = colorPickers.get(led).getValue();
            colors[3 * led] = (byte) (255 * c.getRed());
            colors[(3 * led) + 1] = (byte) (255 * c.getGreen());
            colors[(3 * led) + 2] = (byte) (255 * c.getBlue());
        }
        Thread t = new Thread(() -> helper.sendColors(CHANNEL, 3, 1, 0, 2, 0, colors, true));
        t.start();
    }

    public static void main(String[] args) {
        launch();
    }
}