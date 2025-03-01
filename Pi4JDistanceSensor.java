///usr/bin/env jbang "$0" "$@" ; exit $?

//REPOS mavencentral,mavensnapshot=https://oss.sonatype.org/content/groups/public

//DEPS org.slf4j:slf4j-api:2.0.12
//DEPS org.slf4j:slf4j-simple:2.0.12
//DEPS com.pi4j:pi4j-core:3.0.0-SNAPSHOT
//DEPS com.pi4j:pi4j-plugin-raspberrypi:3.0.0-SNAPSHOT
//DEPS com.pi4j:pi4j-plugin-gpiod:3.0.0-SNAPSHOT

import java.util.concurrent.TimeUnit;

import com.pi4j.Pi4J;
import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.gpio.digital.DigitalState;
import com.pi4j.io.gpio.digital.PullResistance;

/**
 * Example code to blink a LED (DigitalOutput) and use a button (DigitalInput).
 * Make sure to follow the README of this project to learn more about JBang and how to install it.
 *
 * This example can be executed without sudo:
 * jbang Pi4JDistanceSensor.java
 */
public class Pi4JDistanceSensor {

    private static final int PIN_TRIGGER = 18;
    private static final int PIN_ECHO = 24;

    private static DigitalOutput trigger;
    private static DigitalInput echo;

    public static void main(String[] args) {
        System.out.println("Starting distance sensor example...");

        // Initialize Pi4J context
        var pi4j = Pi4J.newAutoContext();

        try {
            // Initialize the output pin
            trigger = pi4j.digitalOutput().create(PIN_TRIGGER); 
            trigger.low();

            // Initialize the input pin
            var echoConfig = DigitalInput.newConfigBuilder(pi4j)
                .address(PIN_ECHO)
                .pull(PullResistance.PULL_UP) ;
            echo = pi4j.create(echoConfig);

            // Loop and measure the distance 5 times per second
            while (true) {
                measureDistance();
                Thread.sleep(200);
            }
        } catch (Exception ex) {
            System.err.println("Error: " + ex.getMessage());
        } finally {
            // Shutdown the Pi4J context
            pi4j.shutdown();
        }
    }

    private static void measureDistance() {
        try {
            // Set trigger high for 0.01ms
            // Pi4J V2+ only provides a pulse method for milliseconds, but the distance sensor needs a short pulse...
            // This is reaching the limits of what a programming language on Linux can do, but we can try ;-)
            trigger.state(DigitalState.HIGH);
            long startTrigger = System.nanoTime();
            while (System.nanoTime() - startTrigger < 10) {
                // Busy wait
            }
            trigger.state(DigitalState.LOW);

            // Start the measurement
            while (echo.isLow()) {
			    // Wait until the echo pin is high, indicating the ultrasound was sent
		    }
		    long startEcho = System.nanoTime();

            // Wait till measurement is finished
		    while (echo.isHigh()) {
                // Wait until the echo pin is low, indicating the ultrasound was received back
		    }
		    long endEcho = System.nanoTime();
            
            // Output the distance
            float measuredSeconds = getSecondsDifference(startEcho, endEcho);
            System.out.println("Measured distance is: "
                    + getDistance(measuredSeconds, true) + "cm"
                    + " for " + measuredSeconds + "s");
        } catch (Exception ex) {
            System.err.println("Error: " + ex.getMessage());
        }
    }

    /**
     * Get the distance (in cm) for a given duration.
     * The calculation is based on the speed of sound which is 34300 cm/s.
     *
     * @param seconds Number of seconds
     * @param half Flag to define if the calculated distance must be divided
     */
    private static int getDistance(float seconds, boolean half) {
        float distance = seconds * 34300;
        return Math.round(half ? distance / 2 : distance);
    }

    /**
     * Get the number of seconds between two nanosecond timestamps.
     * 1 second = 1000000000 nanoseconds
     *
     * @param start Start timestamp in nanoseconds
     * @param end End timestamp in nanoseconds
     */
    private static float getSecondsDifference(long start, long end) {
        return (end - start) / 1000000000F;
    }
}