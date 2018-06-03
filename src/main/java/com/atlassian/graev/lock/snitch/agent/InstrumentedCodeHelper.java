package com.atlassian.graev.lock.snitch.agent;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Agent will load this class by Bootstrap classloader.
 * It is going to be used instrumented code.
 */
@SuppressWarnings("WeakerAccess")
public class InstrumentedCodeHelper {

    /**
     * This counter thing keeps JIT from optimizing away pointless recursion.
     */
    private static AtomicLong successfulRuns = new AtomicLong();

    /**
     * Control how many files we create
     */
    private static AtomicLong writtenListings = new AtomicLong();

    /**
     * Do recursion by ourselves to catch StackOverflowError and to still have enough stack space for logging to file
     */
    public static void dummyRecursion() {
        doDummyRecursion(Settings.recursionDepth());
    }

    static void doDummyRecursion(int depth) {
        if (depth <= 1) {
            successfulRuns.incrementAndGet();
            return;
        }

        doDummyRecursion(depth - 1);
    }

    /**
     * We are going to use the counter to make JIT not to optimize away recursion
     */
    public static long getSuccessfulRuns() {
        return successfulRuns.get();
    }

    /**
     * Create unique file with stacktrace.
     */
    public static void printThrowableToFile(Throwable t) {
        if (writtenListings.get() >= Settings.maxTraceFiles()) {
            // We need to be careful not to spam disk with huge traces
            return;
        }

        try {
            final String uuid = System.nanoTime() + "";
            final PrintWriter out = new PrintWriter("lock-snitch-trace-" + uuid + "-" + getSuccessfulRuns());
            t.printStackTrace(out);
            out.close();
            writtenListings.incrementAndGet();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
