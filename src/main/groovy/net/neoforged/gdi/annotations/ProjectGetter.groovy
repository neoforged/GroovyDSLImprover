/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.gdi.annotations

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty

import java.lang.annotation.*

/**
 * Annotate a method returning an instance of {@link Project} with this annotation
 * in order to inform DSL properties in the class that this method should be used for accessing the project, when needed.
 * <br>
 * {@link DirectoryProperty DirectoryProperties} and {@link RegularFileProperty RegularFileProperties}
 * <strong>need</strong> to have a project getter defined for accessing files relative to the project ({@linkplain Project#file(Object)}.
 *
 * Example:
 * <pre>
 * {@code
 * interface MyExtensionProperties {
 *     @ProjectGetter Project getProject()
 *     @DSLProperty RegularFileProperty getInput()
 * }
 * }
 * </pre>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
@interface ProjectGetter {
}