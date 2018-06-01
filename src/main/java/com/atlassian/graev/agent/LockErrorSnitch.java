package com.atlassian.graev.agent;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.concurrent.locks.ReentrantLock;

public class LockErrorSnitch {
    public static void premain(final String args, final Instrumentation instrumentation) {
        try {
            doPremain(instrumentation);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void doPremain(Instrumentation instrumentation) throws NoSuchMethodException {
        final Method rlUnlock = ReentrantLock.class.getDeclaredMethod("unlock");
        new LockLogging(rlUnlock).instrument(instrumentation);
    }
}
