package com.atlassian.graev.lock.snitch.agent;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * Agent will load this class by Bootstrap classloader.
 * It is going to be used instrumented code.
 */
public class InstrumentedCodeHelper {

    /**
     * This counter thing keeps JIT from optimizing away pointless recursion.
     */
    private static long successfulRuns = 0L;

    private static int writtenListings = 0;

    public static void dummyRecursion() {
        doDummyRecursion(Settings.recursionDepth());
    }

    static void doDummyRecursion(int depth) {
        if (depth <= 1) {
            successfulRuns++;
            return;
        }

        doDummyRecursion(depth - 1);
    }

    /**
     * We are going to use the counter to make JIT not to optimize away recursion
     */
    public static long getSuccessfulRuns() {
        return successfulRuns;
    }

    /**
     * Create unique file with stacktrace.
     */
    public static void printThrowableToFile(Throwable t) {
        if (++writtenListings > Settings.maxTraceFiles()) {
            // We need to be careful not to spam disk with huge traces
            return;
        }

        try {
            final String uuid = System.nanoTime() + "";
            final PrintWriter out = new PrintWriter("lock-snitch-trace-" + uuid + "-" + getSuccessfulRuns());
            t.printStackTrace(out);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
