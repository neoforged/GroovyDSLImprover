/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.gdi.transformer.property

import groovy.transform.CompileStatic
import groovyjarjarasm.asm.Opcodes
import net.minecraftforge.gdi.transformer.DSLPropertyTransformer
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.tools.GeneralUtils
import org.codehaus.groovy.ast.tools.GenericsUtils
import org.gradle.api.Action
import org.gradle.api.provider.HasMultipleValues

import java.util.stream.Collectors
import java.util.stream.Stream

/**
 * Handles properties of type {@link HasMultipleValues}.
 */
@CompileStatic
class CollectionPropertyHandler implements PropertyHandler, Opcodes {
    private final Set<ClassNode> colTypes
    CollectionPropertyHandler(Class<? extends HasMultipleValues>... classes) {
        this.colTypes = new HashSet<>();

        for (final def clz in classes) {
            colTypes.add(ClassHelper.make(clz))
        }
    }

    @Override
    boolean handle(MethodNode methodNode, AnnotationNode annotation, String propertyName, DSLPropertyTransformer.Utils utils) {
        final retType = methodNode.returnType
        for (final type : colTypes) {
            if (GeneralUtils.isOrImplements(retType, type)) {
                handleInternal(methodNode, annotation, propertyName, utils)
                return true
            }
        }
        return false
    }

    boolean handleInternal(MethodNode methodNode, AnnotationNode annotation, String propertyName, DSLPropertyTransformer.Utils utils) {
        final singularName = utils.getSingularPropertyName(propertyName, annotation)
        final type = methodNode.returnType.genericsTypes[0].type

        utils.visitPropertyType(type, annotation)
        final factoryMethod = utils.factory(type, annotation, singularName)
        final delegation = factoryMethod === null ? null : new DSLPropertyTransformer.OverloadDelegationStrategy(0, GeneralUtils.callThisX(factoryMethod.name))

        if (utils.getBoolean(annotation, 'isConfigurable', true)) {
            final actionClazzType = GenericsUtils.makeClassSafeWithGenerics(Action, type)
            utils.createAndAddMethod(
                    methodName: singularName,
                    modifiers: ACC_PUBLIC,
                    parameters: [new Parameter(type, 'val'), new Parameter(actionClazzType, 'action')],
                    codeExpr: {
                        final valVar = GeneralUtils.localVarX('val', type)
                        [
                                GeneralUtils.callX(
                                        GeneralUtils.varX('action', actionClazzType),
                                        'execute',
                                        valVar
                                ),
                                GeneralUtils.callX(GeneralUtils.callThisX(methodNode.name), 'add', valVar)
                        ]
                    }(),
                    delegationStrategies: { factoryMethod === null ? [] : [delegation] }
            )

            utils.createAndAddMethod(
                    methodName: singularName,
                    modifiers: ACC_PUBLIC,
                    parameters: [new Parameter(type, 'val'), utils.closureParam(type)],
                    codeExpr: {
                        final valVar = GeneralUtils.localVarX('val', type)
                        utils.delegateAndCall(GeneralUtils.localVarX('closure', DSLPropertyTransformer.RAW_GENERIC_CLOSURE), valVar).tap {
                            it.add(GeneralUtils.callX(GeneralUtils.callThisX(methodNode.name), 'add', valVar))
                        }
                    }(),
                    delegationStrategies: { factoryMethod === null ? [] : [delegation] }
            )
        }

        utils.createAndAddMethod(
                methodName: singularName,
                modifiers: ACC_PUBLIC,
                parameters: [new Parameter(type, 'val')],
                codeExpr: [GeneralUtils.callX(GeneralUtils.callThisX(methodNode.name), 'add', GeneralUtils.localVarX('val', type))]
        )

        final arrayType = type.makeArray()
        utils.createAndAddMethod(
                methodName: propertyName,
                modifiers: ACC_PUBLIC | ACC_VARARGS,
                parameters: [new Parameter(arrayType, 'values')],
                codeExpr: [GeneralUtils.callX(GeneralUtils.callThisX(methodNode.name), 'addAll', GeneralUtils.localVarX('values', arrayType))]
        )

        final upperBounds = new ClassNode[] {type}
        final genericType = new GenericsType(GenericsUtils.newClass(type), upperBounds, null).tap {
            it.wildcard = true
        };
        final genericTypes = new GenericsType[]{genericType}

        final iterable = GenericsUtils.makeClassSafeWithGenerics(ClassHelper.make(Iterable), genericTypes)
        utils.createAndAddMethod(
                methodName: propertyName,
                modifiers: ACC_PUBLIC,
                parameters: [new Parameter(iterable, 'values')],
                codeExpr: [GeneralUtils.callX(GeneralUtils.callThisX(methodNode.name), 'addAll', GeneralUtils.localVarX('values', iterable))]
        )
    }
}
