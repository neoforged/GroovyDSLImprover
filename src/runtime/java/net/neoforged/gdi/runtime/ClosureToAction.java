/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.gdi.runtime;

import groovy.lang.Closure;
import org.gradle.api.Action;

public class ClosureToAction {
    @SuppressWarnings("rawtypes")
    public static <T> Action<T> delegateAndCall(Closure closure) {
        return t -> {
            closure.setDelegate(t);
            closure.setResolveStrategy(Closure.DELEGATE_FIRST);
            closure.call(t);
        };
    }
}
