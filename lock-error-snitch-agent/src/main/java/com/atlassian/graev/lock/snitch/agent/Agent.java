package com.atlassian.graev.lock.snitch.agent;

import java.lang.instrument.Instrumentation;

/**
 * Java agent that can catch Throwables inside locks and reliably write them to file
 */
public class Agent {

    /**
     * Entry point for agent
     */
    public static void premain(final String args, final Instrumentation instrumentation) throws Exception {
        instrumentLockBytecode(instrumentation);
        spawnTraceWriterThread();
    }

    private static void instrumentLockBytecode(Instrumentation instrumentation) throws ClassNotFoundException {
        new LocksDecorator().instrument(instrumentation);
        AgentLogger.print("All locks bytecode has been instrumented");
    }

    private static void spawnTraceWriterThread() {
        final Thread writer = new Thread(new AsyncTracesWriter(), "lock-snitch-writer");
        writer.setDaemon(true);
        writer.start();
        AgentLogger.print("Traces writing thread has been spawned");
    }

}
