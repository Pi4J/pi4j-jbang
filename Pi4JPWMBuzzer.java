/// usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS org.slf4j:slf4j-api:2.0.12
//DEPS org.slf4j:slf4j-simple:2.0.12
//DEPS com.pi4j:pi4j-core:3.0.1
//DEPS com.pi4j:pi4j-plugin-raspberrypi:3.0.1
//DEPS com.pi4j:pi4j-plugin-linuxfs:3.0.1

import com.pi4j.Pi4J;
import com.pi4j.io.pwm.Pwm;
import com.pi4j.io.pwm.PwmType;
import com.pi4j.plugin.linuxfs.provider.pwm.LinuxFsPwmProviderImpl;

/**
 * Example code to fade a LED with a PWM signal.
 * Make sure to follow the README of this project to learn more about JBang and how to install it.
 * <p>
 * This example can be executed without sudo:
 * jbang Pi4JPWMLed.java
 */
public class Pi4JPWMBuzzer {

    // BCM 19 on Raspberry Pi 4 = PWM Channel 1
    // BCM 19 on Raspberry Pi 5 = PWM Channel 3
    // Buzzer in CrowPi is connected to BCM 18 = on RPi 5 is Channel 2
    private static final int CHANNEL = 2;
    private static Pwm pwm;

    public static void main(String[] args) {
        System.out.println("Starting PWM output example...");

        try {
            // Initialize the Pi4J context
            var pi4j = Pi4J.newContextBuilder()
                    .add(new LinuxFsPwmProviderImpl("/sys/class/pwm/", 0))
                    .build();

            // All Raspberry Pi models support a hardware PWM pin on GPIO_01.
            // Raspberry Pi models A+, B+, 2B, 3B also support hardware PWM pins:
            // BCM 12, 13, 18, and 19
            var pwmConfig = Pwm.newConfigBuilder(pi4j)
                    .address(CHANNEL)
                    .pwmType(PwmType.HARDWARE)
                    .initial(0)
                    .shutdown(0)
                    .build();
            pwm = pi4j.create(pwmConfig);

            int[] frequencies = new int[]{
                    262, // C4(262)
                    294, // D4(294)q
                    330, // E4(330),
                    349, // F4(349),
                    392, // G4(392),
                    440, // A4(440),
                    494, // B4(494),
                    523 // C5(523),
            };

            for (int frequency : frequencies) {
                System.out.println("Playing tone with frequency " + frequency + " Hz");
                playTone(frequency, 500);
            }

            // Shut down the Pi4J contextq
            pi4j.shutdown();

            System.out.println("Done");
        } catch (Exception ex) {
            System.err.println("Error: " + ex.getMessage());
        }
    }

    /**
     * Plays a tone with the given frequency in Hz for a specific duration.
     * This method is blocking and will sleep until the specified duration has passed.
     * A frequency of zero causes the buzzer to play silence.
     * A duration of zero to play the tone indefinitely and return immediately.
     *
     * @param frequency Frequency in Hz
     * @param duration  Duration in milliseconds
     */
    private static void playTone(int frequency, int duration) {
        if (frequency > 0) {
            // Activate the PWM with a duty cycle of 50% and the given frequency in Hz.
            // This causes the buzzer to be on for half of the time during each cycle, resulting in the desired frequency.
            pwm.on(50, frequency);

            // If the duration is larger than zero, the tone should be automatically stopped after the given duration.
            if (duration > 0) {
                sleep(duration);
                playSilence();
            }
        } else {
            playSilence(duration);
        }
    }

    /**
     * Silences the buzzer and returns immediately.
     */
    private static void playSilence() {
        pwm.off();
    }

    /**
     * Silences the buzzer and waits for the given duration.
     * This method is blocking and will sleep until the specified duration has passed.
     *
     * @param duration Duration in milliseconds
     */
    private static void playSilence(int duration) {
        playSilence();
        sleep(duration);
    }

    /**
     * Utility function to sleep for the specified amount of milliseconds.
     * An {@link InterruptedException} will be catched and ignored while setting the interrupt flag again.
     *
     * @param milliseconds Time in milliseconds to sleep
     */
    private static void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}