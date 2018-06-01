package com.atlassian.graev.agent;

import javassist.CannotCompileException;
import javassist.NotFoundException;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class LockLogging implements ClassFileTransformer {

    private final List<Method> methods;

    LockLogging(Method... methods) {
        this.methods = Arrays.asList(methods);
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
        final List<Method> matchingMethods = methods.stream()
                .filter(m -> dotName(className).equals(m.getDeclaringClass().getName()))
                .collect(Collectors.toList());

        return instrumentMethods(bytecode, matchingMethods);
    }

    private byte[] instrumentMethods(byte[] bytecode, List<Method> methods) {
        try {
            return doInstrumentMethods(bytecode, methods);
        } catch (NotFoundException | CannotCompileException | IOException e) {
            throw new RuntimeException("Instrumentation failed", e);
        }
    }

    private byte[] doInstrumentMethods(byte[] bytecode, List<Method> methods) throws NotFoundException, CannotCompileException, IOException {
//        if (methods.isEmpty()) {
//            return bytecode;
//        }
//
//        final ClassPool pool = ClassPool.getDefault();
//        final CtClass classDefinition = pool.makeClass(new ByteArrayInputStream(bytecode));
//
//        Log.print("Instructing {0} methods for class {1}", methods.size(),
//                methods.iterator().next().getDeclaringClass().getSimpleName());
//
//        for (Method method : methods) {
//            final CtMethod ctMethod = classDefinition.getMethod("unlock",
//                    Descriptor.ofMethod(CtClass.voidType, new CtClass[0]));
//            Log.print("Inserting logging code to the method {0}", method.getName());
//            ctMethod.addCatch("{ System.out.println(\"Hello Kitty \" + $e); throw $e; }", pool.get("java.lang.Error"));
//        }

//        byte[] newBytecode = classDefinition.toBytecode();
//        classDefinition.detach();
//        Log.print("Done! The length of the new bytecode is {0}", newBytecode.length);
        return bytecode;
    }

    private String dotName(String name) {
        return name.replace('/', '.');
    }
}
