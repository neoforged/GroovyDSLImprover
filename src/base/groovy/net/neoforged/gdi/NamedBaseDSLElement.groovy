/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.gdi

import groovy.transform.CompileStatic

/**
 * Defines a DSL object which is configurable, part of a project structure, and has a name.
 * @param <TSelf> The type of the implementing class.
 */
@CompileStatic
interface NamedBaseDSLElement<TSelf extends NamedBaseDSLElement<TSelf>> extends BaseDSLElement<TSelf> {

    /**
     * @return The name of the project.
     */
    String getName();
}
