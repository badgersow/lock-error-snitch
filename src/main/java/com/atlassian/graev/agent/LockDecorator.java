package com.atlassian.graev.agent;

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
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class LockDecorator implements ClassFileTransformer {

    private final Map<String, Collection<String>> methodsByClass = new HashMap<>();

    LockDecorator() {
        methodsByClass.put("java/util/concurrent/locks/ReentrantLock", Arrays.asList("lock", "unlock"));
    }

    void instrument(Instrumentation instrumentation) {
        instrumentation.addTransformer(this);
    }

    @Override
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] bytecode) {
        final Collection<String> matchingMethods = new ArrayList<>();
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

        Log.print("Instructing {0} methods for class {1}", methods.size(), className);

        for (String methodName : methods) {
            final CtMethod method = classDefinition.getMethod("unlock",
                    Descriptor.ofMethod(CtClass.voidType, new CtClass[0]));
            Log.print("Inserting logging code to the method {0}", methodName);
            method.addCatch("{ System.out.println(\"Hello Kitty \" + $e); throw $e; }", pool.get("java.lang.Error"));
        }

        byte[] newBytecode = classDefinition.toBytecode();
        classDefinition.detach();
        Log.print("Done! The length of the new bytecode is {0}", newBytecode.length);
        return bytecode;
    }

}
