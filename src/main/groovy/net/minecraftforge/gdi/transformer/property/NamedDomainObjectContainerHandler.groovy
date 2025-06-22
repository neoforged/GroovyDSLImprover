/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.gdi.transformer.property

import groovy.transform.CompileStatic
import groovyjarjarasm.asm.Opcodes
import net.minecraftforge.gdi.runtime.ClosureToAction

import net.minecraftforge.gdi.transformer.DSLPropertyTransformer
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.tools.GeneralUtils
import org.codehaus.groovy.ast.tools.GenericsUtils
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer

@CompileStatic
class NamedDomainObjectContainerHandler implements PropertyHandler, Opcodes {
    private static final ClassNode PROPERTY_TYPE = ClassHelper.make(NamedDomainObjectContainer)
    private static final ClassNode CLOSURE_TO_ACTION = ClassHelper.make(ClosureToAction)

    @Override
    @SuppressWarnings('UnnecessaryQualifiedReference')
    boolean handle(MethodNode methodNode, AnnotationNode annotation, String propertyName, DSLPropertyTransformer.Utils utils) {
        if (!GeneralUtils.isOrImplements(methodNode.returnType, PROPERTY_TYPE)) return false

        final singularName = utils.getSingularPropertyName(propertyName, annotation)
        final type = methodNode.returnType.genericsTypes[0].type

        final actionClazzType = GenericsUtils.makeClassSafeWithGenerics(Action, type)

        utils.createAndAddMethod(
                methodName: singularName,
                modifiers: ACC_PUBLIC,
                parameters: [new Parameter(ClassHelper.STRING_TYPE, 'name'), new Parameter(actionClazzType, 'action')],
                code: GeneralUtils.stmt(GeneralUtils.callX(
                        GeneralUtils.callThisX(methodNode.name),
                        'register',
                        GeneralUtils.args(
                                GeneralUtils.localVarX('name', ClassHelper.STRING_TYPE),
                                GeneralUtils.localVarX('action', actionClazzType)
                        )
                ))
        )

        final scope = new VariableScope()
        scope.putDeclaredVariable(GeneralUtils.localVarX('closure', DSLPropertyTransformer.RAW_GENERIC_CLOSURE))
        utils.createAndAddMethod(
                methodName: singularName,
                modifiers: ACC_PUBLIC,
                parameters: [new Parameter(ClassHelper.STRING_TYPE, 'name'), utils.closureParam(type)],
                code: GeneralUtils.block(scope, GeneralUtils.stmt(GeneralUtils.callX(
                        GeneralUtils.callThisX(methodNode.name),
                        'register',
                        GeneralUtils.args(
                                GeneralUtils.localVarX('name', ClassHelper.STRING_TYPE),
                                asAction(GeneralUtils.localVarX('closure', DSLPropertyTransformer.RAW_GENERIC_CLOSURE))
                        )
                )))
        )

        utils.createAndAddMethod(
                methodName: propertyName,
                modifiers: ACC_PUBLIC,
                parameters: [utils.closureParam(methodNode.returnType)],
                code: GeneralUtils.stmt(GeneralUtils.callX(
                        GeneralUtils.callThisX(methodNode.name),
                        'configure', GeneralUtils.localVarX('closure', DSLPropertyTransformer.RAW_GENERIC_CLOSURE)
                ))
        )

        return true
    }



    static Expression asAction(Expression closure) {
        GeneralUtils.callX(CLOSURE_TO_ACTION, 'delegateAndCall', closure)
    }
}
