/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.gdi.tests

import groovy.transform.CompileStatic
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.*
import static net.neoforged.gdi.runtime.EnumValueGetter.get

@CompileStatic
class EnumValueGetterTest {

    @Test
    void "can find enum values"() {
        assertEquals(get(SomeEnum, 'VALUE_A'), SomeEnum.VALUE_A)
        assertEquals(get(SomeEnum, 'VAlUE_a'), SomeEnum.VALUE_A)

        assertEquals(get(SomeEnum, 'valueB'), SomeEnum.valueB)
        assertEquals(get(SomeEnum, 'VALUEB'), SomeEnum.valueB)

        assertEquals(get(SomeEnum, 'valuec'), SomeEnum.valuec)
        assertEquals(get(SomeEnum, 'VaLueC'), SomeEnum.valuec)
    }

    @Test
    void "cannot find invalid enum names"() {
        assertThrows(IllegalArgumentException, () -> get(SomeEnum, 'VALUE_B'))
    }

    static enum SomeEnum {
        VALUE_A,
        valueB,
        valuec
    }
}
