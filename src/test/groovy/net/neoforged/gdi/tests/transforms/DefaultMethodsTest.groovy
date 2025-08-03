/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.gdi.tests.transforms

import groovy.transform.CompileStatic
import net.neoforged.gdi.annotations.DefaultMethods
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.junit.jupiter.api.Test

@CompileStatic
class DefaultMethodsTest extends BaseTransformerTesting {
    final ClassNode testedClass = ClassHelper.make(TestInterface)

    @Test
    void "default methods have the default modifier"() {
        assert getMethod('notAbstract').isDefault() && !getMethod('notAbstract').isAbstract()
    }

    @Test
    void "abstract methods don't have the default modifier"() {
        assert getMethod('someAbstract').isAbstract()
    }

    @CompileStatic
    @DefaultMethods
    static interface TestInterface {
        String someAbstract()

        default void notAbstract() {

        }
    }
}
