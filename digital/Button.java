/// usr/bin/env jbang "$0" "$@" ; exit $?

/**
 * This example uses the simplifed main method, which is available since Java 25.
 * More info about using specific Java versions with JBang is documented on
 * https://www.jbang.dev/documentation/guide/latest/javaversions.html
 */
// JAVA 25

//REPOS mavencentral,mavensnapshot=https://central.sonatype.com/repository/maven-snapshots/

//DEPS org.slf4j:slf4j-api:2.0.17
//DEPS org.slf4j:slf4j-simple:2.0.17
//DEPS com.pi4j:pi4j-core:4.0.0-SNAPSHOT
//DEPS com.pi4j:pi4j-plugin-ffm:4.0.0-SNAPSHOT

import com.pi4j.Pi4J;
import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalState;
import com.pi4j.io.gpio.digital.PullResistance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Example code to use a button (DigitalInput).
 * Make sure to follow the README of this project to learn more about JBang and how to install it.
 * <p>
 * From the terminal, in the `digital` directory, start this example with:
 * <code>jbang Button.java</code>
 */

// Connect a button to PIN 15 = BCM 22
private static final int BCM_BUTTON = 22;

void main() throws Exception {
    System.setProperty(org.slf4j.simple.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "TRACE");
    Logger logger = LoggerFactory.getLogger(this.getClass());

    // Initialize the Pi4J context
    var pi4j = Pi4J.newAutoContext();

    // Initialize the button configuration
    var buttonConfig = DigitalInput.newConfigBuilder(pi4j)
            .bcm(BCM_BUTTON)
            .pull(PullResistance.PULL_DOWN)
            .debounce(100_000_000L);

    // Initialize the button (digital input)
    var button = pi4j.create(buttonConfig);
    System.out.println("Button is initialized");

    // Add a listener to the button
    var pressCounter = new AtomicInteger(0);
    button.addListener(e -> {
        System.out.println("Button changed in listener to: " + e.state());
        if (e.state() == DigitalState.LOW) {
            // Each time the button changes to the low state, increment the counter
            var currentCount = pressCounter.incrementAndGet();
            System.out.println("Button was pressed for the " + currentCount + "th time");
        }
    });

    // Loop until the button has been pressed 10 times
    while (pressCounter.get() < 10) {
        Thread.sleep(10);
    }

    // Shutdown the Pi4J context
    pi4j.shutdown();
}