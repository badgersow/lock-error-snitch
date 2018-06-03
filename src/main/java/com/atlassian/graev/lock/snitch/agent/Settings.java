package com.atlassian.graev.lock.snitch.agent;

/**
 * Helper class to store settings for agent/instrumented code
 */
class Settings {

    /**
     * The directory where trace files are going to be created
     */
    private static final String TRACES_DIRECTORY = System.getProperty("lock.snitch.traces.directory", ".");

    /**
     * Maximum numbers of listing files to keep.
     * This is only process-wide settings. If JVM get's restarted, counter starts from 0.
     */
    private static final int MAX_TRACE_FILES = Integer.getInteger("lock.snitch.max.trace.files", 10);

    /**
     * Depth of dummy recursion.
     * Should be little enough not to affect performance.
     * Should be big enough to give us required stack size for logging.
     * Default value is a trade-off between efficiency and safety.
     */
    private static final int RECURSION_DEPTH = Integer.getInteger("lock.snitch.recursion.depth", 250);

    static String tracesDirectory() {
        return TRACES_DIRECTORY;
    }

    static int maxTraceFiles() {
        return MAX_TRACE_FILES;
    }

    static int recursionDepth() {
        return RECURSION_DEPTH;
    }
}
