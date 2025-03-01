///usr/bin/env jbang "$0" "$@" ; exit $?

//REPOS mavencentral,mavensnapshot=https://oss.sonatype.org/content/groups/public

//DEPS org.slf4j:slf4j-api:2.0.12
//DEPS org.slf4j:slf4j-simple:2.0.12
//DEPS com.pi4j:pi4j-core:3.0.0-SNAPSHOT
//DEPS com.pi4j:pi4j-plugin-raspberrypi:3.0.0-SNAPSHOT
//DEPS com.pi4j:pi4j-plugin-gpiod:3.0.0-SNAPSHOT

import com.pi4j.Pi4J;
import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalState;

/**
 * Example code to blink a LED (DigitalOutput) and use a button (DigitalInput).
 * Make sure to follow the README of this project to learn more about JBang and how to install it.
 *
 * This example can be executed without sudo:
 * jbang PI4JButton.java
 */
public class Pi4JButton {

    // Connect a button to PIN 18 = BCM 24
    private static final int PIN_BUTTON = 24;

    private static int pressCount = 0;

    public static void main(String[] args) throws Exception {

        var pi4j = Pi4J.newAutoContext();

        var buttonConfig = DigitalInput.newConfigBuilder(pi4j)
                .id("button")
                .name("Press button")
                .address(PIN_BUTTON)
                //.pull(PullResistance.PULL_DOWN) // enable this line to compare with and without pull down resistor
                .debounce(3000L);
                
        var button = pi4j.create(buttonConfig);
        button.addListener(e -> {
            if (e.state() == DigitalState.LOW) {
                pressCount++;
                System.out.println("Button was pressed for the " + pressCount + "th time");
            }
        });

        // Loop until the button has been pressed 10 times
        while (pressCount < 10) {
            Thread.sleep(10);
        }

        pi4j.shutdown();
    }
}