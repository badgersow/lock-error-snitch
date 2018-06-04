package com.atlassian.graev.lock.snitch.agent;

import java.io.File;
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
    private static AtomicLong dummyCounterForJit = new AtomicLong();

    /**
     * Control how many files we create
     */
    private static AtomicLong writtenListings = new AtomicLong();

    static {
        createInitFileIfNeeded();
    }

    /**
     * Do recursion by ourselves to catch StackOverflowError and to still have enough stack space for logging to file
     */
    public static void dummyRecursion() {
        final long time = System.nanoTime();
        doDummyRecursion(Settings.recursionDepth(),
                e(time, 1), e(time, 2), e(time, 3), e(time, 4), e(time, 5),
                e(time, 6), e(time, 7), e(time, 8), e(time, 9), e(time, 10),
                e(time, 11), e(time, 12), e(time, 13), e(time, 14), e(time, 15),
                e(time, 16), e(time, 17), e(time, 18), e(time, 19), e(time, 20));
    }

    private static long e(long time, int bit) {
        return time & (1 << bit);
    }

    /**
     * Approx ~ 20 * 8 + 4 + 8 = 172 bytes per recursion.
     * Dummy operations to make JIT not to optimize stack frame size.
     */
    static void doDummyRecursion(
            int depth,
            long a1, long a2, long a3, long a4, long a5, long a6, long a7, long a8, long a9, long a10,
            long a11, long a12, long a13, long a14, long a15, long a16, long a17, long a18, long a19, long a20
    ) {
        if (depth <= 1) {
            final long sum = a1 + a2 + a3 + a4 + a5 + a6 + a7 + a8 + a9 + a10 +
                    a11 + a12 + a13 + a14 + a15 + a16 + a17 + a18 + a19 + a20;
            dummyCounterForJit.addAndGet(sum);
            return;
        }

        doDummyRecursion(depth - 1,
                a20 + a19, a19 + a18, a18 + a17, a17 + a16, a16 + a15,
                a15 + a14, a14 + a13, a13 + a12, a12 + a11, a11 + a10,
                a10 + a9, a9 + a8, a8 + a7, a7 + a6, a6 + a5,
                a5 + a4, a4 + a3, a3 + a2, a2 + a1, a1 + a20);
    }

    /**
     * We are going to use the counter to make JIT not to optimize away recursion
     */
    public static long getDummyCounterForJit() {
        return dummyCounterForJit.get();
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
            final String filename = "lock-snitch-trace-" + generateUid() + "-" + getDummyCounterForJit();
            final String tracePath = Settings.tracesDirectory() + File.separator + filename;
            final PrintWriter out = new PrintWriter(tracePath);
            t.printStackTrace(out);
            out.close();
            writtenListings.incrementAndGet();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String generateUid() {
        return String.valueOf(System.nanoTime());
    }

    private static void createInitFileIfNeeded() {
        if (Settings.skipInitFile()) {
            return;
        }

        try {
            final String filename = "lock-snitch-init-" + generateUid();
            final String initPath = Settings.tracesDirectory() + File.separator + filename;
            //noinspection ResultOfMethodCallIgnored
            new File(initPath).createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
