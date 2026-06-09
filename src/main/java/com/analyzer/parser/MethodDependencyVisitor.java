package com.analyzer.parser;

import com.analyzer.graph.MethodGraph;
import com.analyzer.model.*;
import org.objectweb.asm.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MethodDependencyVisitor extends ClassVisitor {
    private static final Logger logger = LoggerFactory.getLogger(MethodDependencyVisitor.class);

    private MethodGraph methodGraph;
    private String currentClass;
    private String currentMethod;
    private String currentDescriptor;

    public MethodDependencyVisitor(MethodGraph methodGraph) {
        super(Opcodes.ASM9);
        this.methodGraph = methodGraph;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.currentClass = name.replace("/", ".");
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        String fieldType = org.objectweb.asm.Type.getType(descriptor).getClassName();
        FieldNode field = new FieldNode(currentClass, name, fieldType);
        field.accessLevel = accessLevel(access);
        field.isStatic = (access & Opcodes.ACC_STATIC) != 0;
        field.isFinal = (access & Opcodes.ACC_FINAL) != 0;
        methodGraph.addField(field);
        return super.visitField(access, name, descriptor, signature, value);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        this.currentMethod = name;
        this.currentDescriptor = descriptor;

        MethodNode method = new MethodNode(currentClass, name, descriptor);
        method.accessLevel = accessLevel(access);
        method.isStatic = (access & Opcodes.ACC_STATIC) != 0;
        method.isAbstract = (access & Opcodes.ACC_ABSTRACT) != 0;
        method.returnType = org.objectweb.asm.Type.getReturnType(descriptor).getClassName();

        org.objectweb.asm.Type[] paramTypes = org.objectweb.asm.Type.getArgumentTypes(descriptor);
        for (org.objectweb.asm.Type paramType : paramTypes) {
            method.parameters.add(paramType.getClassName());
        }

        if (exceptions != null) {
            for (String exc : exceptions) {
                method.exceptionThrows.add(exc.replace("/", "."));
            }
        }

        methodGraph.addMethod(method);
        logger.debug("Method found: {}.{}", currentClass, name);

        return new MethodVisitor(Opcodes.ASM9) {
            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                String from = currentClass + "#" + currentMethod + currentDescriptor;
                String to = owner.replace("/", ".") + "#" + name + descriptor;
                methodGraph.addMethodCall(from, to, "invoke");
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
            }

            @Override
            public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                String method = currentClass + "#" + currentMethod + currentDescriptor;
                String field = owner.replace("/", ".") + "#" + name;
                methodGraph.addFieldAccess(method, field, "read_write");
                super.visitFieldInsn(opcode, owner, name, descriptor);
            }

            @Override
            public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
                if (type != null) {
                    // exception tracking: 
                        // type.replace("/", "."));
                }
                super.visitTryCatchBlock(start, end, handler, type);
            }
        };
    }

    private String accessLevel(int access) {
        if ((access & Opcodes.ACC_PUBLIC) != 0) return "public";
        if ((access & Opcodes.ACC_PROTECTED) != 0) return "protected";
        if ((access & Opcodes.ACC_PRIVATE) != 0) return "private";
        return "package";
    }
}