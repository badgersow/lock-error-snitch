package com.atlassian.graev.lock.snitch.agent;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.Descriptor;

import javax.xml.bind.DatatypeConverter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

/**
 * This class instruments bytecode for locks
 */
class LocksDecorator implements ClassFileTransformer {

    private final Map<String, Collection<String>> methodsByClass = new HashMap<>();

    private final List<Class<?>> classes = new ArrayList<>();

    LocksDecorator() throws ClassNotFoundException {
        List<String> methodDecorators = asList("lock", "unlock");
        methodsByClass.put("java/util/concurrent/locks/ReentrantLock", methodDecorators);
        methodsByClass.put("java/util/concurrent/locks/ReentrantReadWriteLock$ReadLock", methodDecorators);
        methodsByClass.put("java/util/concurrent/locks/ReentrantReadWriteLock$WriteLock", methodDecorators);

        for (String className : methodsByClass.keySet()) {
            classes.add(Class.forName(className.replace('/', '.')));
        }
    }

    void instrument(Instrumentation instrumentation) {
        try {
            instrumentation.addTransformer(this, true);
            // This is a required step because these classes are loaded before the agent
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
            method.addCatch("" +
                    "{ " +
                    "   com.atlassian.graev.lock.snitch.agent.AsyncTracesWriter.pendingThrowable = $e; " +
                    "   throw $e; " +
                    "}", pool.get("java.lang.Throwable"));

        }

        byte[] newBytecode = classDefinition.toBytecode();
        classDefinition.detach();
        AgentLogger.print("Done! The length of the new bytecode is {0}", newBytecode.length);
        AgentLogger.debug("The bytecode for this class is below:");
        AgentLogger.debug("{0}", hexString(newBytecode));
        return newBytecode;
    }

    private static String hexString(byte[] bytes) {
        return DatatypeConverter.printHexBinary(bytes);
    }

}
