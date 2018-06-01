package com.atlassian.graev.agent;

import java.lang.instrument.Instrumentation;

public class Agent {

    public static void premain(final String args, final Instrumentation instrumentation) {
        new LockDecorator().instrument(instrumentation);
    }

}
