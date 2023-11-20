/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.gdi.annotations

import net.minecraftforge.gdi.transformer.BouncerMethodTransformer
import org.codehaus.groovy.transform.GroovyASTTransformationClass

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * Adds a bouncer synthetic method with the given return type.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
@GroovyASTTransformationClass(classes = BouncerMethodTransformer)
@interface BouncerMethod {
    /**
     * @return the return type of the bouncer method
     */
    Class<?> returnType()
}