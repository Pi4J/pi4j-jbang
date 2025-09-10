/// usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS org.slf4j:slf4j-api:2.0.17
//DEPS org.slf4j:slf4j-simple:2.0.17
//DEPS com.pi4j:pi4j-core:4.0.0-SNAPSHOT
//DEPS com.pi4j:pi4j-plugin-raspberrypi:4.0.0-SNAPSHOT
//DEPS com.pi4j:pi4j-plugin-gpiod:4.0.0-SNAPSHOT

import com.pi4j.Pi4J;
import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalState;
import com.pi4j.io.gpio.digital.PullResistance;

/**
 * Example code to blink a LED (DigitalOutput) and use a button (DigitalInput).
 * Make sure to follow the README of this project to learn more about JBang and how to install it.
 * <p>
 * This example can be executed without sudo:
 * jbang PI4JButton.java
 */
public class Pi4JButton {

    // Connect a button to PIN 18 = BCM 24
    private static final int PIN_BUTTON = 24;

    private static int pressCount = 0;

    public static void main(String[] args) throws Exception {

        // Initialize the Pi4J context
        var pi4j = Pi4J.newAutoContext();

        // Initialize the button configuration
        var buttonConfig = DigitalInput.newConfigBuilder(pi4j)
                .id("button")
                .name("Press button")
                .address(PIN_BUTTON)
                .pull(PullResistance.PULL_DOWN) // disable this line to compare with and without pull down resistor
                //.pull(PullResistance.PULL_UP) // try this instead of PULL_DOWN
                .debounce(3000L);

        // Initialize the button (digital input)
        var button = pi4j.create(buttonConfig);
        System.out.println("Button is initialized");

        // Add a listener to the button
        button.addListener(e -> {
            if (e.state() == DigitalState.LOW) {
                // Each time the button changes to the low state, increment the counter
                pressCount++;
                System.out.println("Button was pressed for the " + pressCount + "th time");
            }
        });

        // Loop until the button has been pressed 10 times
        while (pressCount < 10) {
            Thread.sleep(10);
        }

        // Shutdown the Pi4J context
        pi4j.shutdown();
    }
}