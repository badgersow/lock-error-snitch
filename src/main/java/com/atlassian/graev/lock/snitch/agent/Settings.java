package com.atlassian.graev.lock.snitch.agent;

class Settings {

    private static final int MAX_TRACE_FILES = Integer.getInteger("lock.snitch.max.trace.files", 10);
    private static final int RECURSION_DEPTH = Integer.getInteger("lock.snitch.recursion.depth", 100);

    static int maxTraceFiles() {
        return MAX_TRACE_FILES;
    }

    static int recursionDepth() {
        return RECURSION_DEPTH;
    }
}
