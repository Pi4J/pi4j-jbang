/// usr/bin/env jbang "$0" "$@" ; exit $?
//REPOS mavencentral,mavensnapshot=https://central.sonatype.com/repository/maven-snapshots/

//DEPS org.slf4j:slf4j-api:2.0.17
//DEPS org.slf4j:slf4j-simple:2.0.17
//DEPS com.pi4j:pi4j-core:4.0.0-SNAPSHOT
//DEPS com.pi4j:pi4j-plugin-ffm:4.0.0-SNAPSHOT

import com.pi4j.Pi4J;
import com.pi4j.io.pwm.Pwm;
import com.pi4j.io.pwm.PwmType;

/**
 * Example code to fade a LED with a PWM signal.
 * Make sure to follow the README of this project to learn more about JBang and how to install it.
 * <p>
 * From the terminal, in the `digital` directory, start this example with:
 * <code>jbang FadingLed.java</code>
 */
public class FadingLed {

    // BCM 19 on Raspberry Pi 4 = PWM Channel 1
    // BCM 19 on Raspberry Pi 5 = PWM Channel 3
    // Buzzer in CrowPi is connected to BCM 18 = on RPi 5 is Channel 2
    private static final int CHANNEL = 0;

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
                    .frequency(1)
                    .busNumber(2)
                    //.shutdown(0)
                    .build();
            System.out.println("PWM config create");
            var pwm = pi4j.create(pwmConfig);
            System.out.println("PWM initialized");

            // Loop through PWM values 10 times
            for (int loop = 0; loop < 10; loop++) {
                for (int useValue = 0; useValue <= 100; useValue += 5) {
                    pwm.on(useValue, 1000);
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
