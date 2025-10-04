/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.gdi.tests

import groovy.transform.CompileStatic
import net.neoforged.gdi.transformer.Unpluralizer
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

@CompileStatic
class UnpluralizerTest {
    @Test
    void "-s test"() {
        Assertions.assertEquals(Unpluralizer.unpluralize('tests'), 'test')
    }

    @Test
    void "-es test ending in s"() {
        Assertions.assertEquals(Unpluralizer.unpluralize('buses'), 'bus')
    }

    @Test
    void "-es test"() {
        Assertions.assertEquals(Unpluralizer.unpluralize('files'), 'file')
    }

    @Test
    void "-ies test"() {
        Assertions.assertEquals(Unpluralizer.unpluralize('fries'), 'fry')
    }

    @Test
    void "fallback test"() {
        Assertions.assertEquals(Unpluralizer.unpluralize('word'), 'word')
    }
}
