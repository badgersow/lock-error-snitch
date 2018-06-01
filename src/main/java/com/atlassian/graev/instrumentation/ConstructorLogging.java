package com.atlassian.graev.instrumentation;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.NotFoundException;
import javassist.bytecode.Descriptor;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class ConstructorLogging implements ClassFileTransformer {

    private final String logFile;

    private final Class<?> clazz;

    public ConstructorLogging(String logFile, Class<?> clazz) {
        this.logFile = logFile;
        this.clazz = clazz;
    }

    public void instrument(Instrumentation instrumentation) {
        instrumentation.addTransformer(this);
    }

    @Override
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] bytecode) throws IllegalClassFormatException {
        System.out.println(className);

        if (className.equals(clazz.getName())) {
            return instrumentConstructor(bytecode);
        }

        return bytecode;
    }

    private byte[] instrumentConstructor(byte[] bytecode) {
        try {
            return doInstrumentConstructor(bytecode);
        } catch (NotFoundException | CannotCompileException | IOException e) {
            throw new RuntimeException("Instrumentation failed", e);
        }
    }

    private byte[] doInstrumentConstructor(byte[] bytecode) throws NotFoundException, CannotCompileException, IOException {
        ClassPool pool = ClassPool.getDefault();
        CtClass cc = pool.get(clazz.getSimpleName());
        CtConstructor constructor = cc.getConstructor(Descriptor.ofConstructor(new CtClass[0]));
        constructor.insertBefore("System.our.println(\"Hey Joe!\")");

        System.out.println(cc + " " + constructor);
        return cc.toBytecode();
    }
}
