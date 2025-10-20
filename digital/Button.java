/// usr/bin/env jbang "$0" "$@" ; exit $?
//REPOS mavencentral,mavensnapshot=https://central.sonatype.com/repository/maven-snapshots/

//DEPS org.slf4j:slf4j-api:2.0.17
//DEPS org.slf4j:slf4j-simple:2.0.17
//DEPS com.pi4j:pi4j-core:4.0.0-SNAPSHOT
//DEPS com.pi4j:pi4j-plugin-ffm:4.0.0-SNAPSHOT

import com.pi4j.Pi4J;
import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalState;
import com.pi4j.io.gpio.digital.PullResistance;

/**
 * Example code to use a button (DigitalInput).
 * Make sure to follow the README of this project to learn more about JBang and how to install it.
 * <p>
 * From the terminal, in the `digital` directory, start this example with:
 * <code>jbang Button.java</code>
 */
public class Button {

    // Connect a button to PIN 15 = BCM 22
    private static final int PIN_BUTTON = 22;

    private static int pressCount = 0;

    public static void main(String[] args) throws Exception {

        // Initialize the Pi4J context
        var pi4j = Pi4J.newAutoContext();

        // Initialize the button configuration
        var buttonConfig = DigitalInput.newConfigBuilder(pi4j)
                .address(PIN_BUTTON)
                .pull(PullResistance.PULL_DOWN)
                .debounce(1_000);

        // Initialize the button (digital input)
        var button = pi4j.create(buttonConfig);
        System.out.println("Button is initialized");

        // Add a listener to the button
        button.addListener(e -> {
            System.out.println("Button changed in listener to: " + e.state());
            if (e.state() == DigitalState.LOW) {
                // Each time the button changes to the low state, increment the counter
                pressCount++;
                System.out.println("Button was pressed for the " + pressCount + "th time");
            }
        });

        // Loop until the button has been pressed 10 times
        var buttonState = button.state();
        System.out.println("Current button state: " + buttonState);
        while (pressCount < 10) {
            if (buttonState != button.state()) {
                buttonState = button.state();
                System.out.println("Button changed in loop to state: " + buttonState);
            }
            Thread.sleep(10);
        }

        // Shutdown the Pi4J context
        pi4j.shutdown();
    }
}