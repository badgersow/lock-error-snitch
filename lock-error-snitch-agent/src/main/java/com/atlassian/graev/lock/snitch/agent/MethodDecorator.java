package com.atlassian.graev.lock.snitch.agent;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtMethod;
import javassist.NotFoundException;

/**
 * Generic interface for method bytecode decoration
 */
public interface MethodDecorator {
    void decorate(ClassPool pool, CtMethod method) throws CannotCompileException, NotFoundException;

    String methodName();
}
