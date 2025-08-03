/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.gdi.transformer.property

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import groovyjarjarasm.asm.Opcodes
import net.neoforged.gdi.runtime.ClosureToAction

import net.neoforged.gdi.transformer.DSLPropertyTransformer
import net.neoforged.gdi.transformer.PropertyQuery
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.tools.GeneralUtils
import org.codehaus.groovy.ast.tools.GenericsUtils
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer

@Slf4j
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

        Expression propertyGetExpr = PropertyQuery.GETTER.getter(methodNode)
        final delegationStrategy = new DSLPropertyTransformer.OverloadDelegationStrategy(0, propertyGetExpr)
        utils.createAndAddMethod(
                methodName: propertyName,
                modifiers: ACC_PUBLIC,
                parameters: [new Parameter(type, propertyName), new Parameter(
                        actionClazzType,
                        'action'
                )],
                codeExpr: {
                    final valVar = GeneralUtils.localVarX(propertyName, type)
                    [
                            GeneralUtils.callX(
                                    GeneralUtils.varX('action', actionClazzType),
                                    'execute',
                                    valVar
                            )
                    ]
                }(),
                delegationStrategies: { [delegationStrategy] }
        )

        return true
    }



    static Expression asAction(Expression closure) {
        GeneralUtils.callX(CLOSURE_TO_ACTION, 'delegateAndCall', closure)
    }
}
