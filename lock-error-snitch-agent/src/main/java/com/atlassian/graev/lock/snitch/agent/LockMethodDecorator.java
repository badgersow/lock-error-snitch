package com.atlassian.graev.lock.snitch.agent;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtMethod;
import javassist.NotFoundException;

public class LockMethodDecorator implements MethodDecorator {
    @Override
    public void decorate(ClassPool pool, CtMethod method) throws CannotCompileException, NotFoundException {
        // Before useful lock code let's do dummy recursion to provoke StackOverflowError
        method.insertBefore("" +
                "{ " +
                "   com.atlassian.graev.lock.snitch.agent.InstrumentedCodeHelper.dummyRecursion(); " +
                "}");
        // Catch errors and save them to file
        method.addCatch("" +
                "{ " +
                "   com.atlassian.graev.lock.snitch.agent.InstrumentedCodeHelper.printThrowableToFile($e); " +
                "   throw $e; " +
                "}", pool.get("java.lang.Throwable"));
    }

    @Override
    public String methodName() {
        return "lock";
    }
}
