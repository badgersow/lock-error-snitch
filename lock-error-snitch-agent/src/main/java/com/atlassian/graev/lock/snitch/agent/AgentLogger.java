package com.atlassian.graev.lock.snitch.agent;

import java.text.MessageFormat;

/**
 * Helper class to log the actions of agent to stdio
 */
class AgentLogger {

    static void debug(String s, Object... args) {
        if (Boolean.getBoolean("lock.snitch.debug")) {
            print(s, args);
        }
    }

    static void print(String s, Object... args) {
        if (args.length == 0) {
            print(s);
            return;
        }

        print(MessageFormat.format(s, args));
    }

    private static void print(String s) {
        System.out.println(s);
    }
}
