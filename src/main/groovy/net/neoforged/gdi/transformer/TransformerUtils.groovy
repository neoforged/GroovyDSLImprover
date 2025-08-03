/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.gdi.transformer

import groovy.transform.CompileStatic
import groovyjarjarasm.asm.Type
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode

import java.util.stream.Collectors
import java.util.stream.Stream

@CompileStatic
class TransformerUtils {

    static String getInternalName(ClassNode classNode) {
        return classNode.name.replace('.' as char, '/' as char)
    }

    static Type getType(ClassNode classNode) {
        switch (classNode) {
            case ClassHelper.int_TYPE: return Type.INT_TYPE
            case ClassHelper.double_TYPE: return Type.DOUBLE_TYPE
            case ClassHelper.boolean_TYPE: return Type.BOOLEAN_TYPE
            case ClassHelper.float_TYPE: return Type.FLOAT_TYPE
            case ClassHelper.short_TYPE: return Type.SHORT_TYPE
            case ClassHelper.byte_TYPE: return Type.BYTE_TYPE
            case ClassHelper.VOID_TYPE: return Type.VOID_TYPE
            case ClassHelper.long_TYPE: return Type.LONG_TYPE
            case ClassHelper.char_TYPE: return Type.CHAR_TYPE
            default: return Type.getObjectType(getInternalName(classNode))
        }
    }

    static ClassNode getCommonAncestor(ClassNode first, ClassNode second) {
        final secondAncestors = hierarchyTree(second).collect(Collectors.toSet())
        return hierarchyTree(first).filter(secondAncestors.&contains).findFirst().orElse(ClassHelper.OBJECT_TYPE)
    }

    static Stream<ClassNode> hierarchyTree(ClassNode node) {
        final builder = Stream.<ClassNode>builder()
        builder.add(node)
        if (node.superClass !== null) {
            hierarchyTree(node.superClass).forEach(builder.&add)
        }
        node.interfaces.each {
            hierarchyTree(it).forEach(builder.&add)
        }
        builder.build()
    }
}
