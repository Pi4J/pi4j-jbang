/// usr/bin/env jbang "$0" "$@" ; exit $?

/**
 * This example uses the simplifed main method, which is available since Java 25.
 * More info about using specific Java versions with JBang is documented on
 * https://www.jbang.dev/documentation/guide/latest/javaversions.html
 */
// JAVA 25

/**
 * Example code to illustrate how a Java file can be executed with JBang.
 * Make sure to follow the README of this project to learn more about JBang and how to install it.
 * <p>
 * From the terminal, in the `basic` directory, start this example with:
 * <code>jbang HelloWorld.java</code>
 */
void main() {
    String txt = "Hello World";
    IO.println(txt);
}
