/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.gdi.transformer

import groovy.transform.CompileStatic
import groovyjarjarasm.asm.Opcodes
import groovyjarjarasm.asm.Type
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.tools.GeneralUtils
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.codehaus.groovy.transform.sc.StaticCompileTransformation

import java.util.stream.Collectors
import java.util.stream.Stream

@CompileStatic
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class BouncerMethodTransformer extends AbstractASTTransformation implements Opcodes {
    private static final ClassNode CSTATIC = ClassHelper.make(CompileStatic)
    @Override
    void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {
        this.init(astNodes, sourceUnit)
        if (astNodes[1] !instanceof MethodNode) return
        final method = (MethodNode) astNodes[1]
        ClassNode clazz = method.declaringClass
        if (clazz instanceof InnerClassNode && clazz.name.endsWith('Trait$Helper')) {
            clazz = ((InnerClassNode) clazz).outerClass
        }

        if (method.isVoidMethod()) {
            addError('Cannot use @BouncerMethod on void methods!', method)
            return
        }

        final bouncerType = getMemberClassValue((AnnotationNode)astNodes[0], 'returnType')
        if ((bouncerType.isInterface() && !method.returnType.implementsInterface(bouncerType)) || (!bouncerType.isInterface() && !method.returnType.isDerivedFrom(bouncerType))) {
            addError("Cannot use class $bouncerType as bouncer type as it is not a superclass of ${method.returnType}!", method)
            return
        }

        final List<Parameter> actualParams = Stream.of(method.parameters)
                .filter { it.name != '$self' }
                .collect(Collectors.toList())

        final mtd = new MethodNode(
                method.name, ACC_BRIDGE | ACC_SYNTHETIC | ACC_PUBLIC, bouncerType,
                actualParams.toArray() as Parameter[], method.exceptions,
                GeneralUtils.returnS(GeneralUtils.bytecodeX {
                    it.visitIntInsn(ALOAD, 0) // Load self
                    final List<Type> params = actualParams.stream()
                            .map { Parameter parameter -> TransformerUtils.getType(parameter.type) }
                            .collect(Collectors.toList())
                    for (int i = 0; i < params.size(); i++) {
                        it.visitIntInsn(params[i].getOpcode(ILOAD), i + 1)
                    }
                    it.visitMethodInsn(INVOKEINTERFACE,
                            TransformerUtils.getInternalName(clazz),
                            method.name,
                            Type.getMethodDescriptor(TransformerUtils.getType(method.returnType), params as Type[]),
                            true
                    )
                })
        )

        clazz.addMethod(mtd)

        final ann = new AnnotationNode(CSTATIC)
        mtd.addAnnotation(ann)
        new StaticCompileTransformation().visit(new ASTNode[] {
                ann, mtd
        }, sourceUnit)
    }
}
