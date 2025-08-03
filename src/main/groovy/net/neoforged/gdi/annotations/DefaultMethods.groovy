/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.gdi.annotations

import net.neoforged.gdi.transformer.DefaultMethodsTransformer
import org.codehaus.groovy.transform.GroovyASTTransformationClass

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * Annotate a Groovy interface with this annotation in order to add Java 8 default interface methods to the interface. <br>
 * <strong>Note</strong>: this annotation exists because Groovy 3 default interface methods are not using the Java 8 {@code default}
 * modifier and instead are treated as trait methods.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
@GroovyASTTransformationClass(classes = DefaultMethodsTransformer)
@interface DefaultMethods {

}