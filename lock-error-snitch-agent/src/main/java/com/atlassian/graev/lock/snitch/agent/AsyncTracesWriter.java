package com.atlassian.graev.lock.snitch.agent;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Separate thread that will write lock throwables to file.
 * We are doing it from another thread because first one doesn't have enough stack frames for it.
 * <p>
 * It's OK to have data race here as long as we have liveness property.
 */
public class AsyncTracesWriter implements Runnable {

    private static long writtenListings = 0;

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
                    // Finish the thread if we wrote enough traces
                    return;
                }
            }
        } catch (InterruptedException e) {
            // OK, whatever
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
