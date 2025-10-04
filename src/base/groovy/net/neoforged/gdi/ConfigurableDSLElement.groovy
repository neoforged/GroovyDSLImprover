/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.gdi

import groovy.transform.CompileStatic
import net.neoforged.gdi.annotations.BouncerMethod
import net.neoforged.gdi.annotations.DefaultMethods
import net.neoforged.gdi.markers.IsConfigurable
import net.neoforged.gdi.runtime.ClosureToAction
import org.gradle.api.Action
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.Internal
import org.gradle.util.Configurable
import org.jetbrains.annotations.NotNull

import java.util.function.Consumer

/**
 * Defines an object which supports configuration using different systems available in Gradle.
 * @param <T> The type of the object, needs to be the target type.
 */
@CompileStatic
@DefaultMethods
interface ConfigurableDSLElement<T extends ConfigurableDSLElement<T>> extends ExtensionAware, IsConfigurable {

    /**
     * Returns the current instance cast to the right target.
     * @return The current instance.
     */
    @SuppressWarnings("unchecked")
    @NotNull
    @Internal
    default T getThis() {
        return (T) this;
    }

    /**
     * Configures this object using the given action.
     * @param consumer The action used to configure the target.
     * @return This object.
     */
    @NotNull
    default T configure(final Consumer<T> consumer) {
        consumer.accept(getThis());
        return getThis();
    }

    /**
     * Configures this object using the given action.
     * @param consumer The action used to configure the target.
     * @return This object.
     */
    @NotNull
    default T configure(final Action<T> consumer) {
        consumer.execute(getThis());
        return getThis();
    }
}
