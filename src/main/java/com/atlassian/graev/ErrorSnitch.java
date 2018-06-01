package com.atlassian.graev;

import java.lang.instrument.Instrumentation;

public class ErrorSnitch {
    public static void premain(final String agentArgument, final Instrumentation instrumentation) {
        System.out.println("Agent loaded");
    }
}
