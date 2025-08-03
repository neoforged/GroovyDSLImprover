/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.gdi.tests.property

import groovy.transform.CompileStatic
import net.neoforged.gdi.runtime.ClosureToAction
import org.gradle.api.Named
import org.gradle.api.provider.Property
import org.gradle.util.Configurable

import javax.inject.Inject

@CompileStatic
abstract class ConfigurableObject implements Named, Configurable<ConfigurableObject> {
    abstract Property<String> getString()

    abstract Property<Boolean> getBool()

    final String name
    @Inject
    ConfigurableObject(String name) {
        this.name = name
    }

    @Override
    ConfigurableObject configure(Closure cl) {
        ClosureToAction.delegateAndCall(cl).execute(this)
        this
    }
}
