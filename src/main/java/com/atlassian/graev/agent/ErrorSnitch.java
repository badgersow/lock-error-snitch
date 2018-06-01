package com.atlassian.graev.agent;

import com.atlassian.graev.agent.instrumentation.ConstructorLogging;

import java.lang.instrument.Instrumentation;

public class ErrorSnitch {
    public static void premain(final String args, final Instrumentation instrumentation) {
        final String file = "errors.log";
        final Class<?> error = StackOverflowError.class;

        new ConstructorLogging(file, error).instrument(instrumentation);
    }
}
