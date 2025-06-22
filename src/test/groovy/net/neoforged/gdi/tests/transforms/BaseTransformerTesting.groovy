/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.gdi.tests.transforms

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.tools.GeneralUtils
import org.codehaus.groovy.ast.tools.ParameterUtils

import java.util.stream.Stream

@CompileStatic
abstract class BaseTransformerTesting {
    abstract ClassNode getTestedClass()

    protected MethodNode getMethod(String name, Class... parameters) {
        return getMethod(testedClass, name, parameters)
    }

    protected MethodNode getMethodWithReturn(String name, Class returnType, Class... parameters) {
        return getMethodWithReturn(testedClass, name, returnType, parameters)
    }

    @CompileDynamic
    protected static MethodNode getMethod(ClassNode target, String name, Class... parameters) {
        return target.getMethod(name, Stream.of(parameters).map(ClassHelper.&make).map { GeneralUtils.param(it, 'p') }.toArray(Parameter[]::new))
    }

    @CompileDynamic
    protected static MethodNode getMethodWithReturn(ClassNode target, String name, Class returnType, Class... parameters) {
        final params = Stream.of(parameters).map(ClassHelper.&make).map { GeneralUtils.param(it, 'p') }.toArray(Parameter[]::new)
        final ret = ClassHelper.make(returnType)
        for (final MethodNode mtd : target.methods) {
            if (name == mtd.name && ParameterUtils.parametersEqual(params, mtd.parameters) && mtd.returnType == ret) {
                return mtd
            }
        }
        return null
    }
}
