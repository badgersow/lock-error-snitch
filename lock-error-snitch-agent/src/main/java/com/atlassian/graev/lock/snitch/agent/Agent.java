package com.atlassian.graev.lock.snitch.agent;

import java.lang.instrument.Instrumentation;

/**
 * Entry point for agent
 */
public class Agent {

    public static void premain(final String args, final Instrumentation instrumentation) throws Exception {
        // Instrument locks bytecode
        new LocksDecorator().instrument(instrumentation);
        // Spawn a thread that stores error listings
        final Thread writer = new Thread(new AsyncTracesWriter(), "lock-snitch-writer");
        writer.setDaemon(true);
        writer.start();
    }

}
