///usr/bin/env jbang "$0" "$@" ; exit $?

//REPOS mavencentral,mavensnapshot=https://oss.sonatype.org/content/groups/public

//DEPS org.slf4j:slf4j-api:2.0.12
//DEPS org.slf4j:slf4j-simple:2.0.12
//DEPS com.pi4j:pi4j-core:3.0.0-SNAPSHOT
//DEPS com.pi4j:pi4j-plugin-raspberrypi:3.0.0-SNAPSHOT
//DEPS com.pi4j:pi4j-plugin-gpiod:3.0.0-SNAPSHOT

import com.pi4j.Pi4J;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.util.Console;

/**
 * Example code to blink a LED (DigitalOutput) and use a button (DigitalInput).
 * Make sure to follow the README of this project to learn more about JBang and how to install it.
 *
 * This example must be executed with sudo as it uses PiGpio with:
 * sudo `which jbang` PI4JRGBLed.java
 *
 * More information and a video explaining this example is available on:
 * https://pi4j.com/documentation/building/jbang/
 */
public class Pi4JRGBLed {

    // Connect a button to PIN 23 = BCM 11
    private static final int PIN_RED = 11;
    // Connect a button to PIN 32 = BCM 12
    private static final int PIN_GREEN = 12;
    // Connect a button to PIN 33 = BCM 13
    private static final int PIN_BLUE = 13;

    public static void main(String[] args) throws Exception {

        final var console = new Console();

        var pi4j = Pi4J.newAutoContext();

        var ledRed = pi4j.digitalOutput().create(PIN_RED);
        var ledGreen = pi4j.digitalOutput().create(PIN_GREEN);
        var ledBlue = pi4j.digitalOutput().create(PIN_BLUE);

        // Toggle 10 times RED on and off
        for (int led = 1; led <= 3; led++) {
            DigitalOutput useLed = led == 1 ? ledRed : (led == 2 ? ledGreen : ledBlue);

            for (int i = 0; i < 10; i++) {
                useLed.toggle();
                Thread.sleep(150);

                System.out.println("State of the LED " + useLed.getName() + " has been toggled");
            }

            // Make sure the led is off
            useLed.low();
            System.out.println("LED " + useLed.getName() + " is off");
        }

        Thread.sleep(1000);

        // All three on, should be white
        ledRed.high();
        ledGreen.high();
        ledBlue.high();

        System.out.println("All three on, check if this looks like white...");

        Thread.sleep(5000);

        pi4j.shutdown();
    }
}