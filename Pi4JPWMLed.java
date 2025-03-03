///usr/bin/env jbang "$0" "$@" ; exit $?

//REPOS mavencentral,mavensnapshot=https://oss.sonatype.org/content/groups/public

//DEPS org.slf4j:slf4j-api:2.0.12
//DEPS org.slf4j:slf4j-simple:2.0.12
//DEPS com.pi4j:pi4j-core:3.0.0-SNAPSHOT
//DEPS com.pi4j:pi4j-plugin-raspberrypi:3.0.0-SNAPSHOT
//DEPS com.pi4j:pi4j-plugin-gpiod:3.0.0-SNAPSHOT

import com.pi4j.Pi4J;
import com.pi4j.io.pwm.Pwm;
import com.pi4j.io.pwm.PwmType;

/**
 * Example code to blink a LED (DigitalOutput) and use a button (DigitalInput).
 * Make sure to follow the README of this project to learn more about JBang and how to install it.
 *
 * This example can be executed without sudo:
 * jbang Pi4JPWMLed.java
 */
public class Pi4JPWMLed {

    private static final int MAX_PMW_VALUE = 1000;
    private static final int FADE_STEPS = 10;
    private static final int PIN_LED = 19;

    public static void main(String[] args) {
        System.out.println("Starting PWM output example...");

        try {
            // Initialize the Pi4J context
            var pi4j = Pi4J.newAutoContext();

            // All Raspberry Pi models support a hardware PWM pin on GPIO_01.
            // Raspberry Pi models A+, B+, 2B, 3B also support hardware PWM pins: 
            // BCM 12, 13, 18, and 19 
            var pwmConfig = Pwm.newConfigBuilder(pi4j)
                .address(PIN_LED)
                .pwmType(PwmType.HARDWARE)
                .initial(0)
                .shutdown(0)
                .build();
            var pwm = pi4j.create(pwmConfig);

            // Loop through PWM values 10 times
            for (int loop = 0; loop < 10; loop++) {
                pwm.on();
                for (int useValue = 0; useValue <=MAX_PMW_VALUE; useValue+=MAX_PMW_VALUE/FADE_STEPS) {
                    pwm.setFrequency(useValue);
                    System.out.println("PWM frequency is: " + pwm.getFrequency());
                    Thread.sleep(200);
                }
                pwm.off();
                Thread.sleep(1000);
            }

            // Shut down the Pi4J context
            pi4j.shutdown();

            System.out.println("Done");
        } catch (Exception ex) {
            System.err.println("Error: " + ex.getMessage());
        }
    }
}