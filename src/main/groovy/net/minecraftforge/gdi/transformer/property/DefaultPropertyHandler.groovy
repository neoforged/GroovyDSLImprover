/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.gdi.transformer.property

import groovy.transform.CompileStatic
import groovyjarjarasm.asm.Opcodes
import net.minecraftforge.gdi.runtime.EnumValueGetter
import net.minecraftforge.gdi.transformer.DSLPropertyTransformer
import net.minecraftforge.gdi.transformer.PropertyQuery
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.tools.GeneralUtils
import org.codehaus.groovy.ast.tools.GenericsUtils
import org.gradle.api.Action
import org.gradle.api.provider.Property

@CompileStatic
class DefaultPropertyHandler implements PropertyHandler, Opcodes {
    private static final ClassNode PROPERTY_TYPE = ClassHelper.make(Property)
    private static final ClassNode ENUM_TYPE = ClassHelper.make(Enum)
    private static final ClassNode ENUM_VALUE_GETTER_TYPE = ClassHelper.make(EnumValueGetter)

    @Override
    boolean handle(MethodNode methodNode, AnnotationNode annotation, String propertyName, DSLPropertyTransformer.Utils utils) {
        if (GeneralUtils.isOrImplements(methodNode.returnType, PROPERTY_TYPE)) {
            return generateDirectProperty(methodNode.returnType.genericsTypes[0].type, PropertyQuery.PROPERTY, methodNode, annotation, propertyName, utils)
        } else {
            generateDirectProperty(methodNode.returnType, PropertyQuery.GETTER, methodNode, annotation, propertyName, utils)
        }
        return true
    }

    static void generateDirectProperty(ClassNode type, PropertyQuery query, MethodNode methodNode, AnnotationNode annotation, String propertyName, DSLPropertyTransformer.Utils utils) {
        utils.visitPropertyType(type, annotation)
        type = DSLPropertyTransformer.WRAPPER_TO_PRIMITIVE.getOrDefault(type, type)

        Expression propertyGetExpr = query.getter(methodNode)
        final createDefaultMethod = utils.factory(type, annotation, propertyName)

        if (createDefaultMethod !== null) {
            propertyGetExpr = query.getOrElse(methodNode, GeneralUtils.callThisX(createDefaultMethod.name))
        }

        final delegationStrategy = new DSLPropertyTransformer.OverloadDelegationStrategy(0, propertyGetExpr)

        final setter = query.setter(methodNode, GeneralUtils.localVarX(propertyName, type))

        final defaultSetter = { String methodName ->
            if (setter !== null) {
                utils.createAndAddMethod(
                        methodName: methodName,
                        modifiers: ACC_PUBLIC,
                        parameters: [new Parameter(type, propertyName)],
                        code: GeneralUtils.stmt(setter)
                )
            }
        }

        if (propertyName.startsWith('is')) {
            final name = propertyName.substring(2)
            defaultSetter("set$name")
            defaultSetter(name.uncapitalize())
        } else {
            defaultSetter(propertyName)
        }

        if (utils.getBoolean(annotation, 'isConfigurable', true)) {
            final actionClazzType = GenericsUtils.makeClassSafeWithGenerics(Action, type)
            if (setter !== null) {
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
                                    ),
                                    query.setter(methodNode, valVar)
                            ]
                        }(),
                        delegationStrategies: { [delegationStrategy] }
                )

                utils.createAndAddMethod(
                        methodName: propertyName,
                        modifiers: ACC_PUBLIC,
                        parameters: [new Parameter(type, propertyName), utils.closureParam(type)],
                        codeExpr: {
                            final List<Expression> expr = []
                            final closure = GeneralUtils.varX('closure', DSLPropertyTransformer.RAW_GENERIC_CLOSURE)
                            final valVar = GeneralUtils.localVarX(propertyName, type)
                            expr.addAll(utils.delegateAndCall(closure, valVar))
                            expr.add(query.setter(methodNode, valVar))
                            return expr
                        }(),
                        delegationStrategies: { [delegationStrategy] }
                )
            } else {
                utils.createAndAddMethod(
                        methodName: propertyName,
                        modifiers: ACC_PUBLIC,
                        parameters: [new Parameter(
                                actionClazzType,
                                'action'
                        )],
                        codeExpr: [GeneralUtils.callX(
                                GeneralUtils.varX('action', actionClazzType),
                                'execute',
                                query.getter(methodNode)
                        )]
                )

                utils.createAndAddMethod(
                        methodName: propertyName,
                        modifiers: ACC_PUBLIC,
                        parameters: [utils.closureParam(type)],
                        codeExpr: utils.delegateAndCall(GeneralUtils.varX('closure', DSLPropertyTransformer.RAW_GENERIC_CLOSURE), query.getter(methodNode))
                )
            }
        }

        if (type.superClass == ENUM_TYPE && setter !== null) {
            utils.createAndAddMethod(
                    methodName: propertyName,
                    modifiers: ACC_PUBLIC,
                    parameters: [new Parameter(ClassHelper.STRING_TYPE, propertyName)],
                    code: GeneralUtils.stmt(GeneralUtils.callX(
                            GeneralUtils.callThisX(methodNode.name),
                            'set',
                            GeneralUtils.callX(ENUM_VALUE_GETTER_TYPE, 'get', GeneralUtils.args(GeneralUtils.classX(type), GeneralUtils.localVarX(propertyName, ClassHelper.STRING_TYPE)))
                    ))
            )
        }
    }
}
