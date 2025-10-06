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
        blink10(ledRed);
        blink10(ledGreen);
        blink10(ledBlue);
        Thread.sleep(2_000);

        // Put all off
        ledRed.low();
        ledGreen.low();
        ledBlue.low();

        // Morse
        System.out.println("Starting Morse message");
        morseHelloWorld(ledRed);
        System.out.println("Morse message has been sent");
        Thread.sleep(2_000);

        // All three on, should be (close to) white
        ledRed.high();
        ledGreen.high();
        ledBlue.high();
        System.out.println("All three on, check if this looks like white or close to white...");
        Thread.sleep(2_000);

        // All three off
        ledRed.low();
        ledGreen.low();
        ledBlue.low();

        System.out.println("All three off");

        // Shutdown the Pi4J context
        pi4j.shutdown();
    }

    private static void blink10(DigitalOutput led) throws InterruptedException {
        System.out.println("Start blinking LED " + led.getAddress());

        for (int i = 0; i < 10; i++) {
            led.toggle();
            Thread.sleep(250);
        }

        // Make sure the led is off
        led.low();
        System.out.println("LED " + led.getAddress() + " is off");
    }

    // Contributed by Jonathan Stronkhorst
    // Morse translator: https://morsecode.world/international/translator.html
    // Guide for the Morse timing: https://re06.org/how-to-read-morse-code-a-step-by-step-guide-to-timing-rhythm-and-practice-for-beginners/
    private static void morseHelloWorld(DigitalOutput led) throws InterruptedException {
        String helloWorld = ".... . .-.. .-.. --- / .-- --- .-. .-.. -.. -.-.--";

        /*
         * Upon reading morse guides:
         * Dot is 1 unit
         * dash is 3 units
         * gap between elements in characters is 1 unit
         * gap between letters is 3 units
         * gap between words is 7 units
         */
        for (char c : helloWorld.toCharArray()) {
            System.out.println("Morse: " + c);
            if (c == '.') {
                led.high();
                Thread.sleep(100);
                led.low();
                Thread.sleep(100);
            } else if (c == '-') {
                led.high();
                Thread.sleep(300);
                led.low();
                Thread.sleep(100);
            } else if (c == ' ') {
                Thread.sleep(200);
            } else if (c == '/') {
                Thread.sleep(300);
            }
        }
    }
}