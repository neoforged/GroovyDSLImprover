/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.gdi.runtime;

public class EnumValueGetter {
    public static <T extends Enum<T>> T get(Class<T> enumClass, String name) {
        try {
            return Enum.valueOf(enumClass, name);
        } catch (IllegalArgumentException ignored) {}

        for (final T constant : enumClass.getEnumConstants()) {
            if (constant.name().equalsIgnoreCase(name)) {
                return constant;
            }
        }

        throw new IllegalArgumentException("No enum constant " + enumClass.getCanonicalName() + "." + name);
    }
}
