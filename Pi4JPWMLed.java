///usr/bin/env jbang "$0" "$@" ; exit $?

//REPOS mavencentral,mavensnapshot=https://oss.sonatype.org/content/groups/public

//DEPS org.slf4j:slf4j-api:2.0.12
//DEPS org.slf4j:slf4j-simple:2.0.12
//DEPS com.pi4j:pi4j-core:3.0.0-SNAPSHOT
//DEPS com.pi4j:pi4j-plugin-raspberrypi:3.0.0-SNAPSHOT
//DEPS com.pi4j:pi4j-plugin-linuxfs:3.0.0-SNAPSHOT

import com.pi4j.Pi4J;
import com.pi4j.io.pwm.Pwm;
import com.pi4j.io.pwm.PwmType;

/**
 * Example code to fade a LED with a PWM signal.
 * Make sure to follow the README of this project to learn more about JBang and how to install it.
 *
 * This example can be executed without sudo:
 * jbang Pi4JPWMLed.java
 */
public class Pi4JPWMLed {

    // BCM 19 on Raspberry Pi 4 = PWM Channel 1
    // BCM 19 on Raspberry Pi 5 = PWM Channel 3
    private static final int CHANNEL = 1;

    public static void main(String[] args) {
        System.out.println("Starting PWM output example...");

        try {
            // Initialize the Pi4J context
            var pi4j = Pi4J.newAutoContext();

            // All Raspberry Pi models support a hardware PWM pin on GPIO_01.
            // Raspberry Pi models A+, B+, 2B, 3B also support hardware PWM pins: 
            // BCM 12, 13, 18, and 19 
            var pwmConfig = Pwm.newConfigBuilder(pi4j)
                .address(CHANNEL)
                .pwmType(PwmType.HARDWARE)
                .initial(0)
                .shutdown(0)
                .build();
            var pwm = pi4j.create(pwmConfig);

            // Loop through PWM values 10 times
            for (int loop = 0; loop < 10; loop++) {
                for (int useValue = 0; useValue <= 100; useValue += 5) {
                    pwm.on(useValue, 1500);
                    System.out.println("PWM duty cycle / frequency is: " + pwm.getDutyCycle() + "/" + pwm.getFrequency());
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