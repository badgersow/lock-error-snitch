package com.atlassian.graev.lock.snitch.agent;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.Descriptor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LockDecorator implements ClassFileTransformer {

    private final Map<String, Collection<String>> methodsByClass = new HashMap<>();

    private final List<Class<?>> classes = new ArrayList<>();

    LockDecorator() throws ClassNotFoundException {
        methodsByClass.put("java/util/concurrent/locks/ReentrantLock", Arrays.asList("lock", "unlock"));
        for (String className : methodsByClass.keySet()) {
            classes.add(Class.forName(className.replace('/', '.')));
        }
    }

    void instrument(Instrumentation instrumentation) {
        try {
            instrumentation.addTransformer(this, true);
            instrumentation.retransformClasses(classes.toArray(new Class[0]));
        } catch (UnmodifiableClassException e) {
            throw new RuntimeException("Failed to initialize instrumentation");
        }
    }

    @Override
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] bytecode) {
        for (String targetClass : methodsByClass.keySet()) {
            if (targetClass.equals(className)) {
                return instrumentMethods(bytecode, className, methodsByClass.get(targetClass));
            }
        }

        return bytecode;
    }

    private byte[] instrumentMethods(byte[] bytecode, String className, Collection<String> methods) {
        try {
            return doInstrumentMethods(bytecode, className, methods);
        } catch (NotFoundException | CannotCompileException | IOException e) {
            throw new RuntimeException("Instrumentation failed", e);
        }
    }

    private byte[] doInstrumentMethods(byte[] bytecode, String className, Collection<String> methods) throws NotFoundException, CannotCompileException, IOException {
        if (methods.isEmpty()) {
            return bytecode;
        }

        final ClassPool pool = ClassPool.getDefault();
        final CtClass classDefinition = pool.makeClass(new ByteArrayInputStream(bytecode));

        AgentLogger.print("Instrumenting {0} methods for class {1}", methods.size(), className);

        for (String methodName : methods) {
            final CtMethod method = classDefinition.getMethod(methodName,
                    Descriptor.ofMethod(CtClass.voidType, new CtClass[0]));
            AgentLogger.print("Inserting logging code to the method {0}", methodName);
            method.insertBefore("" +
                    "{ " +
                    "   com.atlassian.graev.lock.snitch.agent.InstrumentedCodeHelper.dummyRecursion(); " +
                    "}");
            method.addCatch("" +
                    "{ " +
                    "   com.atlassian.graev.lock.snitch.agent.InstrumentedCodeHelper.printThrowableToFile($e); " +
                    "   throw $e; " +
                    "}", pool.get("java.lang.Throwable"));
        }

        byte[] newBytecode = classDefinition.toBytecode();
        classDefinition.detach();
        AgentLogger.print("Done! The length of the new bytecode is {0}", newBytecode.length);
        return newBytecode;
    }

}
