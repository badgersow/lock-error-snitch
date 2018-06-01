package com.atlassian.graev.agent;

import java.lang.instrument.Instrumentation;

public class Agent {

    public static void premain(final String args, final Instrumentation instrumentation) throws Exception {
        new LockDecorator().instrument(instrumentation);
    }

}
