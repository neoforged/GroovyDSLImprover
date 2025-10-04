/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.gdi.transformer.property.files

import groovy.transform.CompileStatic
import groovyjarjarasm.asm.Opcodes
import net.neoforged.gdi.transformer.DSLPropertyTransformer
import net.neoforged.gdi.transformer.property.PropertyHandler
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.tools.GeneralUtils
import org.gradle.api.file.ConfigurableFileCollection

@CompileStatic
class FileCollectionPropertyHandler implements PropertyHandler, Opcodes {
    private static final ClassNode TYPE = ClassHelper.make(ConfigurableFileCollection)

    @Override
    boolean handle(MethodNode methodNode, AnnotationNode annotation, String propertyName, DSLPropertyTransformer.Utils utils) {
        if (!GeneralUtils.isOrImplements(methodNode.returnType, TYPE)) return false
        final singularName = utils.getSingularPropertyName(propertyName, annotation)

        final objArray = ClassHelper.OBJECT_TYPE.makeArray()
        utils.createAndAddMethod(
                methodName: propertyName,
                modifiers: ACC_PUBLIC,
                parameters: [new Parameter(objArray, 'paths')],
                code: GeneralUtils.stmt(GeneralUtils.callX(
                        GeneralUtils.callThisX(methodNode.name),
                        'from',
                        GeneralUtils.localVarX('paths', objArray)
                ))
        )

        utils.createAndAddMethod(
                methodName: singularName,
                modifiers: ACC_PUBLIC,
                parameters: [new Parameter(ClassHelper.OBJECT_TYPE, 'path')],
                code: GeneralUtils.stmt(GeneralUtils.callX(
                        GeneralUtils.callThisX(methodNode.name),
                        'from',
                        GeneralUtils.localVarX('path', ClassHelper.OBJECT_TYPE)
                ))
        )

        return true
    }
}
