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
 * Example code to fade a LED with a PWM signal and a HARDWARE PWM pin.
 * Make sure to follow the README of this project to learn more about JBang and how to install it.
 * <p>
 * From the terminal, in the `digital` directory, start this example with:
 * <code>jbang FadingLedHardware.java</code>
 * </p>
 * <p>
 * Test the available PWM hardware channels on your Raspberry Pi, e.g. on RPi 4:
 * $  pinctrl | grep PWM
 * 18: a5    pd | lo // GPIO18 = PWM0_0
 * 19: a5    pd | lo // GPIO19 = PWM0_1
 * 40: a0    pn | lo // PWM0_MISO/GPIO40 = PWM1_0
 * 41: a0    pn | lo // PWM1_MOSI/GPIO41 = PWM1_1
 * </p>
 */
public class FadingLedHardware {

    // Raspberry Pi 4:
    //    GPIO18 = PWM0_0 = Chip 0, Channel 0
    //    GPIO19 = PWM0_1 = Chip 0, Channel 1
    private static final int PWM_CHIP = 0;
    private static final int PWM_CHANNEL = 1;

    public static void main(String[] args) {
        System.out.println("Starting PWM output example...");

        try {
            // Initialize the Pi4J context
            var pi4j = Pi4J.newAutoContext();

            // Initialize PWM
            var pwmConfig = Pwm.newConfigBuilder(pi4j)
                    .pwmType(PwmType.HARDWARE)
                    .chip(PWM_CHIP)
                    .channel(PWM_CHANNEL)
                    .initial(0)
                    .frequency(1000)
                    .build();
            System.out.println("PWM config create");
            var pwm = pi4j.create(pwmConfig);
            System.out.println("PWM initialized");

            // Loop through PWM values 10 times
            for (int loop = 0; loop < 10; loop++) {
                // Fade up from 0% to 100%
                for (int dutyCycle = 0; dutyCycle <= 100; dutyCycle += 5) {
                    pwm.on(dutyCycle);
                    System.out.println("PWM duty cycle is: " + pwm.getDutyCycle());
                    Thread.sleep(50);
                }
                // Fade down from 100% to 0%
                for (int dutyCycle = 100; dutyCycle >= 0; dutyCycle -= 5) {
                    pwm.on(dutyCycle);
                    System.out.println("PWM duty cycle is: " + pwm.getDutyCycle());
                    Thread.sleep(50);
                }
            }

            // Turn off PWM
            pwm.off();

            // Shut down the Pi4J context
            pi4j.shutdown();

            System.out.println("Done");
        } catch (Exception ex) {
            System.err.println("Error: " + ex.getMessage());
        }
    }
}
