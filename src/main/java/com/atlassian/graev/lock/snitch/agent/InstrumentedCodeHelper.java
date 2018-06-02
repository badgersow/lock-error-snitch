package com.atlassian.graev.lock.snitch.agent;

/**
 * This class will be loaded by bootstrap classloader and it can be used from instrumented code
 */
public class InstrumentedCodeHelper {
    public static void doRecursion(int depth) {
        if (depth <= 1) {
            return;
        }

        doRecursion(depth - 1);
    }
}
