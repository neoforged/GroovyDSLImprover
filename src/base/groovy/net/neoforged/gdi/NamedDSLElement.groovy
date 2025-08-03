/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.gdi

import groovy.transform.CompileStatic
import org.gradle.api.tasks.Input

/**
 * Defines a DSL object which has a name.
 */
@CompileStatic
interface NamedDSLElement {

    /**
     * @return The name of the project.
     */
    @Input
    String getName();
}