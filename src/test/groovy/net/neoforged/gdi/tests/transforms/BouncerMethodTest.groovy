/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.gdi.tests.transforms

import groovy.transform.CompileStatic
import net.neoforged.gdi.annotations.BouncerMethod
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

import java.lang.reflect.Method

import static org.mockito.Mockito.*

@CompileStatic
class BouncerMethodTest extends BaseTransformerTesting {
    final ClassNode testedClass = ClassHelper.make(TestInterface)

    @Test
    void "bouncer method exists"() {
        Assertions.assertNotNull(getMethodWithReturn('getIt', Object))
        Assertions.assertNotNull(getMethodWithReturn('callSmth', CharSequence, Object, int, String))
    }

    @Test
    void "bouncer methods delegate to actual methods"() {
        final TestInterface iface = mock(TestInterface)

        when(iface.getIt()).thenReturn('Some string')
        Assertions.assertEquals('Some string', getMethod('getIt', Object).invoke(iface))

        when(iface.callSmth(null, 13, 'Hmm')).thenReturn('Maybe')
        Assertions.assertEquals('Maybe', getMethod('callSmth', CharSequence, Object, int, String).invoke(iface, null, 13, 'Hmm'))

        Assertions.assertNull(getMethod('callSmth', CharSequence, Object, int, String).invoke(iface, 'Not null', 1478, 'hjaks'))
    }

    private static Method getMethod(String name, Class returnType, Class... parameters) {
        for (final Method mtd : TestInterface.class.declaredMethods) {
            if (mtd.name == name && mtd.returnType == returnType && mtd.parameterTypes == parameters) {
                return mtd
            }
        }
        return null
    }

    static interface TestInterface {
        @BouncerMethod(returnType = Object)
        String getIt()

        @BouncerMethod(returnType = CharSequence)
        String callSmth(Object param1, int param2, String param3)
    }
}
