/// usr/bin/env jbang "$0" "$@" ; exit $?
//REPOS mavencentral,mavensnapshot=https://central.sonatype.com/repository/maven-snapshots/

//DEPS org.slf4j:slf4j-api:2.0.17
//DEPS org.slf4j:slf4j-simple:2.0.17
//DEPS com.pi4j:pi4j-core:4.0.0-SNAPSHOT
//DEPS com.pi4j:pi4j-plugin-ffm:4.0.0-SNAPSHOT

import com.pi4j.Pi4J;
import com.pi4j.io.gpio.digital.DigitalOutput;

/**
 * Example code to blink three LEDs (DigitalOutput).
 * Make sure to follow the README of this project to learn more about JBang and how to install it.
 * <p>
 * This example can be executed without sudo:
 * jbang Pi4JRgbLed.java
 */
public class Pi4JRgbLed {

    // Connect a LED to PIN 16 = BCM 23
    private static final int PIN_RED = 23;
    // Connect a LED to PIN 18 = BCM 24
    private static final int PIN_GREEN = 24;
    // Connect a LED to PIN 22 = BCM 25
    private static final int PIN_BLUE = 25;

    public static void main(String[] args) throws Exception {

        // Initialize the Pi4J context
        var pi4j = Pi4J.newAutoContext();

        // Initialize the three LEDs
        var ledRed = pi4j.digitalOutput().create(PIN_RED);
        var ledGreen = pi4j.digitalOutput().create(PIN_GREEN);
        var ledBlue = pi4j.digitalOutput().create(PIN_BLUE);

        // Start with all off
        ledRed.low();
        ledGreen.low();
        ledBlue.low();

        // Toggle 10 times each color on and off
        for (int led = 1; led <= 3; led++) {
            DigitalOutput useLed = led == 1 ? ledRed : (led == 2 ? ledGreen : ledBlue);
            System.out.println("Start blinking LED " + useLed.getAddress());

            for (int i = 0; i < 10; i++) {
                useLed.toggle();
                Thread.sleep(250);
            }

            // Make sure the led is off
            useLed.low();
            System.out.println("LED " + useLed.getAddress() + " is off");
        }

        Thread.sleep(1000);

        // All three on, should be (close to) white
        ledRed.high();
        ledGreen.high();
        ledBlue.high();

        System.out.println("All three on, check if this looks like white or close to white...");

        Thread.sleep(5000);

        // All three off
        ledRed.low();
        ledGreen.low();
        ledBlue.low();

        System.out.println("All three off");

        // Shutdown the Pi4J context
        pi4j.shutdown();
    }
}