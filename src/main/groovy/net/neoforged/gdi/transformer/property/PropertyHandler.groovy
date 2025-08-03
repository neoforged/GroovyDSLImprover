/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.gdi.transformer.property

import groovy.transform.CompileStatic
import net.neoforged.gdi.transformer.DSLPropertyTransformer
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.MethodNode

/**
 * Interface used to handle implementing {@link net.neoforged.gdi.annotations.DSLProperty} methods.
 * @see DSLPropertyTransformer#HANDLERS
 */
@CompileStatic
interface PropertyHandler {
    /**
     * Handles the DSL property.
     * @param methodNode the method declaring the property
     * @param annotation the DSL property annotation
     * @param propertyName the name of the property
     * @param utils a utility class for adding methods
     * @return if this handler handled the property type
     */
    boolean handle(MethodNode methodNode, AnnotationNode annotation, String propertyName, DSLPropertyTransformer.Utils utils)
}