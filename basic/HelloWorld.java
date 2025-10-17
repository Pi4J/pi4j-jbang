/// usr/bin/env jbang "$0" "$@" ; exit $?

/**
 * Example code to illustrate how a Java file can be executed with JBang.
 * Make sure to follow the README of this project to learn more about JBang and how to install it.
 * <p>
 * From the terminal, in the `basic` directory, start this example with:
 * <code>jbang HelloWorld.java</code>
 */
public class HelloWorld {
    public static void main(String[] args) {
        String txt = "Hello World";
        System.out.println(txt);
    }
}
