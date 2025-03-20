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
}