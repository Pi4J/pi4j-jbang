/// usr/bin/env jbang "$0" "$@" ; exit $?

/**
 * To be able to execute the code in this example, dependencies are needed.
 * Therefore, you can't run it with `java basic.JsonParsing.java`.
 * But it can be executed with https://www.jbang.dev when you
 * define the dependencies in the following format:
 */
//DEPS com.fasterxml.jackson.core:jackson-annotations:2.14.1
//DEPS com.fasterxml.jackson.core:jackson-core:2.14.1
//DEPS com.fasterxml.jackson.core:jackson-databind:2.14.1

/**
 * This example uses a Record, which is available since Java 14.
 * More info about using specific Java versions with JBang is documented on
 * https://www.jbang.dev/documentation/guide/latest/javaversions.html
 */
//JAVA 14+

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;

/**
 * Example code to illustrate how a Java file that needs dependencies can be executed with JBang.
 * Make sure to follow the README of this project to learn more about JBang and how to install it.
 * <p>
 * From the terminal, in the `basic` directory, start this example with:
 * <code>jbang JsonParsing.java</code>
 */
public class JsonParsing {

    public static void main(String[] args) {
        var json = """
                [
                    { 
                        "level": 0,
                        "timestamp": 1675867184342,
                        "message": "Program started"
                    },
                    {
                        "level": 5,
                        "timestamp": 1675867185921,
                        "message": "File X not found"
                    },
                    {
                        "level": 9,
                        "timestamp": 1675867186357,
                        "message": "Error at line Y"
                    }
                ]
                """;

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            LogMessage[] logMessages = objectMapper.readValue(json, LogMessage[].class);

            System.out.println("Data loaded from JSON:\n");

            for (LogMessage logMessage : logMessages) {
                System.out.println("Log message at " + logMessage.getZonedDateTime()
                        + "\n\tSeverity: " + logMessage.level().getLabel()
                        + "\n\tMessage: " + logMessage.message());
            }
        } catch (IOException ex) {
            System.err.println("Json processing exception: " + ex.getMessage());
        }
    }

    enum Level {
        INFO(0, "Informative message", 0x00aa00),
        WARNING(5, "Warning message", 0xFFA500),
        ERROR(9, "Error message", 0xA30000);

        @JsonValue
        private final int severity;
        private final String label;
        private final int color;

        Level(int severity, String label, int color) {
            this.severity = severity;
            this.label = label;
            this.color = color;
        }

        @JsonCreator
        public static Level fromValue(Integer severity) throws IllegalArgumentException {
            return Arrays.stream(Level.values()).sequential()
                    .filter(v -> v.getSeverity() == severity)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("The given severity does not exist:" + severity));
        }

        public int getSeverity() {
            return severity;
        }

        public String getLabel() {
            return label;
        }

        public int getColor() {
            return color;
        }
    }

    record LogMessage(Level level, Long timestamp, String message) {
        // If you want to export ZonedDateTime to JSON,
        // you need to comment '@JsonIgnore', and uncomment '@JsonFormat'.
        @JsonIgnore
        //@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
        public ZonedDateTime getZonedDateTime() {
            return ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp()), ZoneId.of("UTC"));
        }
    }
}
