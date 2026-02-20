/// usr/bin/env jbang "$0" "$@" ; exit $?

/**
 * This example uses the simplifed main method, which is available since Java 25.
 * More info about using specific Java versions with JBang is documented on
 * https://www.jbang.dev/documentation/guide/latest/javaversions.html
 */
// JAVA 25

//DEPS org.slf4j:slf4j-api:2.0.17
//DEPS org.slf4j:slf4j-simple:2.0.17
//DEPS com.pi4j:pi4j-core:4.0.0
//DEPS com.pi4j:pi4j-plugin-ffm:4.0.0

import com.pi4j.Pi4J;
import com.pi4j.io.pwm.Pwm;
import com.pi4j.io.pwm.PwmType;

/**
 * Example code to fade a LED with a PWM signal and a SOFTWARE PWM pin.
 * Make sure to follow the README of this project to learn more about JBang and how to install it.
 * <p>
 * From the terminal, in the `digital` directory, start this example with:
 * <code>jbang FadingLedSoftware.java</code>
 * <p>
 * WARNING! At this moment (December 2025), software PWM is not available yet with the FFM plugin...
 * So this example will not work yet, but it is here for future testing...
 * </p>
 */
// Connect a LED to PIN 16 = BCM 23
private static final int BCM_LED = 23;

void main() {
    System.out.println("Starting PWM output example...");

    try {
        // Initialize the Pi4J context
        var pi4j = Pi4J.newAutoContext();

        // All Raspberry Pi models support a hardware PWM pin on GPIO_01.
        // Raspberry Pi models A+, B+, 2B, 3B also support hardware PWM pins:
        // BCM 12, 13, 18, and 19
        var pwmConfig = Pwm.newConfigBuilder(pi4j)
                .pwmType(PwmType.SOFTWARE)
                .bcm(BCM_LED)
                .initial(0)
                .frequency(1000)
                .build();
        System.out.println("PWM config create");
        var pwm = pi4j.create(pwmConfig);
        System.out.println("PWM initialized");

        // Loop through PWM values 10 times
        for (int loop = 0; loop < 10; loop++) {
            // Fade up from 0% to 100%
            System.out.println("Fading up");
            for (int dutyCycle = 0; dutyCycle <= 100; dutyCycle += 1) {
                pwm.on(dutyCycle);
                Thread.sleep(10);
            }
            // Fade down from 100% to 0%
            System.out.println("Fading down");
            for (int dutyCycle = 100; dutyCycle >= 0; dutyCycle -= 1) {
                pwm.on(dutyCycle);
                Thread.sleep(10);
            }
        }

        // Shut down the Pi4J context
        pi4j.shutdown();

        System.out.println("Done");
    } catch (Exception ex) {
        System.err.println("Error: " + ex.getMessage());
    }
}
