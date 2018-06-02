package com.atlassian.graev.lock.snitch.agent;

import java.text.MessageFormat;

class AgentLogger {
    static void print(String s, Object... args) {
        if (args.length == 0) {
            print(s);
        }

        print(MessageFormat.format(s, args));
    }

    private static void print(String s) {
        System.out.println(s);
    }
}
