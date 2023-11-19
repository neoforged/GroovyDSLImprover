/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.gdi.runtime;

import org.gradle.api.provider.MapProperty;
import org.jetbrains.annotations.ApiStatus;

import java.util.Map;
import java.util.function.BiConsumer;

@ApiStatus.Internal
public class MapUtils {
    public static <K, V> void put(Class<K> keyType, Class<V> valueType, Map<K, V> map, Object values) {
        put(keyType, valueType, map::put, values);
    }

    public static <K, V> void put(Class<K> keyType, Class<V> valueType, MapProperty<K, V> map, Object values) {
        put(keyType, valueType, map::put, values);
    }

    public static <K, V> void put(Class<K> keyType, Class<V> valueType, BiConsumer<K, V> putter, Object values) {
        if (!values.getClass().isArray()) {
            throw new IllegalArgumentException("Expecting array, but got: " + values.getClass());
        }
        final Object[] array = ((Object[]) values);
        if (array.length % 2 != 0) {
            throw new IllegalArgumentException("Expected even number of arguments, but got odd number of arguments. Vararg map.put operations must be provided an array of keys, each followed by their value!");
        }

        for (int i = 0; i < array.length; i += 2) {
            final Object key = array[i];
            if (!keyType.isInstance(key)) {
                throw new ClassCastException("Key at position " + i + " is not a " + keyType);
            }

            final Object value = array[i + 1];
            if (!valueType.isInstance(value)) {
                throw new ClassCastException("Value at position " + (i + 1) + " is not a " + value);
            }
            putter.accept((K) key, (V) value);
        }
    }
}
