/// usr/bin/env jbang "$0" "$@" ; exit $?
//REPOS mavencentral,mavensnapshot=https://central.sonatype.com/repository/maven-snapshots/

//DEPS org.slf4j:slf4j-api:2.0.17
//DEPS org.slf4j:slf4j-simple:2.0.17
//DEPS com.pi4j:pi4j-core:4.0.0-SNAPSHOT
//DEPS com.pi4j:pi4j-plugin-ffm:4.0.0-SNAPSHOT

import com.pi4j.Pi4J;
import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.gpio.digital.DigitalState;
import com.pi4j.io.gpio.digital.PullResistance;
import com.pi4j.util.Console;

/**
 * Example code to blink a LED (DigitalOutput) and use a button (DigitalInput).
 * Make sure to follow the README of this project to learn more about JBang and how to install it.
 * <p>
 * From the terminal, in the `digital` directory, start this example with:
 * <code>jbang ButtonLed.java</code>
 * <p>
 * More information and a video explaining this example is available on:
 * https://pi4j.com/documentation/building/jbang/
 */
public class ButtonLed {

    // Wiring see: https://pi4j.com/getting-started/minimal-example-application/

    // Connect a button to PIN 15 = BCM 22
    private static final int BCM_BUTTON = 22;
    // Connect a LED to PIN 18 = BCM 24
    private static final int BCM_LED = 24;

    private static int pressCount = 0;

    public static void main(String[] args) throws Exception {

        final var console = new Console();

        var pi4j = Pi4J.newAutoContext();

        var ledConfig = DigitalOutput.newConfigBuilder(pi4j)
                .id("led")
                .name("LED Flasher")
                .bcm(BCM_LED)
                .shutdown(DigitalState.LOW)
                .initial(DigitalState.LOW);
        var led = pi4j.create(ledConfig);

        var buttonConfig = DigitalInput.newConfigBuilder(pi4j)
                .id("button")
                .name("Press button")
                .bcm(BCM_BUTTON)
                .pull(PullResistance.PULL_DOWN)
                .debounce(100_000_000L);
        var button = pi4j.create(buttonConfig);
        button.addListener(e -> {
            if (e.state() == DigitalState.LOW) {
                pressCount++;
                console.println("Button was pressed for the " + pressCount + "th time");
            }
        });

        while (pressCount < 5) {
            if (led.equals(DigitalState.HIGH)) {
                console.println("LED low");
                led.low();
            } else {
                console.println("LED high");
                led.high();
            }
            Thread.sleep(500 / (pressCount + 1));
        }

        pi4j.shutdown();
    }
}