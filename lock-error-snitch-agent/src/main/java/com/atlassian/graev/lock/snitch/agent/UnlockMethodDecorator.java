package com.atlassian.graev.lock.snitch.agent;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtMethod;
import javassist.NotFoundException;

public class UnlockMethodDecorator implements MethodDecorator {
    @Override
    public void decorate(ClassPool pool, CtMethod method) throws CannotCompileException, NotFoundException {
        method.addCatch("" +
                "{ " +
                "   com.atlassian.graev.lock.snitch.agent.InstrumentedCodeHelper.printThrowableToFile($e); " +
                "   throw $e; " +
                "}", pool.get("java.lang.Throwable"));

        method.insertBefore("" +
                "{ " +
                "   try { " +
                "      com.atlassian.graev.lock.snitch.agent.InstrumentedCodeHelper.dummyRecursion(); " +
                "   } catch (java.lang.Throwable t) { " +
                "      com.atlassian.graev.lock.snitch.agent.InstrumentedCodeHelper.printThrowableToFile(t); " +
                "   } " +
                "}");
    }

    @Override
    public String methodName() {
        return "unlock";
    }
}
