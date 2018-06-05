package com.atlassian.graev.lock.snitch.agent;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Separate thread that writes traces to files.
 * We are doing it from another thread because the thread that catched StackOverflowError might not have
 * enough stack frames for logging.
 * <p>
 * It's OK to have data races here as long as we have liveness property.
 */
public class AsyncTracesWriter implements Runnable {

    /**
     * Control how many files we wrote because we don't want to spam hard drive with them
     */
    private static long writtenListings = 0;

    /**
     * This field is an entry point for another threads to schedule logging.
     * We don't use methods because we might not have stack frames.
     */
    @SuppressWarnings("WeakerAccess")
    public static volatile Throwable pendingThrowable = null;

    @Override
    public void run() {
        createInitFileIfNeeded();

        try {
            while (true) {
                writePendingErrors();
                Thread.sleep(Settings.storePollPeriodMs());

                if (writtenListings >= Settings.maxTraceFiles()) {
                    // If enough trace files are created, we don't need this thread anymore
                    return;
                }
            }
        } catch (InterruptedException e) {
            // Interruption can only happen on application shutdown. We shouldn't do anything special.
        }
    }

    private void writePendingErrors() {
        final Throwable currentState = pendingThrowable;
        if (currentState == null) {
            return;
        }

        pendingThrowable = null;
        writeTraceToFile(currentState);
    }

    private void writeTraceToFile(Throwable t) {
        try {
            final String filename = "snitch-trace-" + generateUid();
            final String tracePath = Settings.tracesDirectory() + File.separator + filename;
            final PrintWriter out = new PrintWriter(tracePath);
            t.printStackTrace(out);
            out.close();
            writtenListings++;
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Init file just signals that agent is running and can write to output directory.
     */
    private static void createInitFileIfNeeded() {
        if (Settings.skipInitFile()) {
            return;
        }

        try {
            final String filename = "snitch-init-" + generateUid();
            final String initPath = Settings.tracesDirectory() + File.separator + filename;
            //noinspection ResultOfMethodCallIgnored
            new File(initPath).createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String generateUid() {
        return String.valueOf(System.nanoTime());
    }

}
