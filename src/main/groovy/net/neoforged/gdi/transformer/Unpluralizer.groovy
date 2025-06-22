/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.gdi.transformer

import groovy.transform.CompileStatic

@CompileStatic
class Unpluralizer {
    static String unpluralize(String str) {
        if (str.endsWith('ies')) {
            return str.dropRight(3) + 'y'
        } else if (str.endsWith('es')) {
            final String drop = str.dropRight(2)
            if ('apply -es rule'(drop)) {
                return drop
            } else {
                return str.dropRight(1) // Only drop the s
            }
        } else if (str.endsWith('s')) {
            return str.dropRight(1)
        }
        return str
    }

    static boolean 'apply -es rule'(String str) {
        return str.endsWith('s') || str.endsWith('x') || str.endsWith('z') || str.endsWith('sh') || str.endsWith('ch')
    }
}
