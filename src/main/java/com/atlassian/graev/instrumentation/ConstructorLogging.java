package com.atlassian.graev.instrumentation;

import com.atlassian.graev.Log;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.NotFoundException;
import javassist.bytecode.Descriptor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;

public class ConstructorLogging implements ClassFileTransformer {

    private final String logFile;

    private final Class<?> clazz;

    public ConstructorLogging(String logFile, Class<?> clazz) {
        this.logFile = logFile;
        this.clazz = clazz;
    }

    public void instrument(Instrumentation instrumentation) {
        try {
            instrumentation.addTransformer(this, true);
            instrumentation.retransformClasses(clazz);
        } catch (UnmodifiableClassException e) {
            throw new RuntimeException("Wasn't able to retransform the target class " + clazz.getSimpleName(), e);
        }
    }

    @Override
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] bytecode) throws IllegalClassFormatException {
        if (dotName(className).equals(clazz.getName())) {
            Log.print("Instrumenting {0}", clazz.getSimpleName());
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
        final ClassPool pool = ClassPool.getDefault();
        final CtClass classDefinition = pool.makeClass(new ByteArrayInputStream(bytecode));
        final CtConstructor constructor = classDefinition.getConstructor(Descriptor.ofConstructor(new CtClass[0]));

        Log.print("Inserting logging code to the default constructor of {0}. " +
                "The length of old bytecode: {1}", constructor.getName(), classDefinition.toBytecode().length);

        constructor.insertAfter("{  }");

        Log.print("Done! The length of the new bytecode is {0}", classDefinition.toBytecode().length);

        return classDefinition.toBytecode();
    }

    private String dotName(String name) {
        return name.replace('/', '.');
    }
}
