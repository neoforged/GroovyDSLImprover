/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.gdi.tests.property

import groovy.transform.CompileStatic
import groovyjarjarasm.asm.Opcodes
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.tools.GeneralUtils
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.internal.util.collections.Iterables

import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Stream

import static org.junit.jupiter.api.Assertions.*

class PropertyTest {
    @TempDir
    private static File gradleProjectDir
    private static PropertyOwner owner
    private static ClassNode node

    @BeforeAll
    @CompileStatic
    static void setup() {
        final Project project = ProjectBuilder.builder()
                .withProjectDir(gradleProjectDir)
                .withName("DummyTestingProject")
                .build()

        owner = project.getObjects().newInstance(PropertyOwnerImpl.class, project.objects)
        node = ClassHelper.make(PropertyOwner)
    }

    @Test
    void "String property generates methods"() {
        owner.invokeMethod('stringProperty', 'Some String')
        assertEquals(owner.stringProperty.get(), 'Some String')

        assertNotNull(getMethod('stringProperty', String))
    }

    @Test
    void "boolean 'is' property generates methods"() {
        owner.invokeMethod('existing', true)
        assertEquals(owner.isExisting.get(), true)

        // Reset to false to test again
        owner.isExisting.set(false)
        assertEquals(owner.isExisting.get(), false)

        owner.existing()
        assertEquals(owner.isExisting.get(), true)

        // Methods for wrapper types generate primitive DSL methods
        assertNotNull(getMethod('existing', boolean))
        assertNotNull(getMethod('setExisting', boolean))

        // As for booleans, we also generate a no-arg setter that sets the value to true
        assertNotNull(getMethod('existing'))
    }

    @Test
    @SuppressWarnings('ConfigurationAvoidance')
    void "NDOC with configurable type generates methods"() {
        owner.invokeMethod('configurableObject', ['clos', {
            string = 'yes'
            bool = false
        }].toArray())
        owner.invokeMethod('configurableObject', ['act', {
            it.string = 'no'
            it.bool = true
        } as Action<ConfigurableObject>].toArray())

        assert owner.configurableObjects.getByName('clos').string.get() == 'yes'
        assert owner.configurableObjects.getByName('act').bool.get() == true

        assertNotNull(getMethod('configurableObject', String, Action))
        assertNotNull(getMethod('configurableObject', String, Closure))

        assertNotNull(getMethod('configurableObjects', Action))
    }

    @Test
    void "collection properties generate methods"() {
        owner.invokeMethod('integer', 13)
        assertEquals(owner.integers.get(), [13])
        assertNotNull(getMethod('integer', Integer))

        owner."integers"(14, 557)
        assertEquals(owner.integers.get(), [13, 14, 557])
        assertNotNull(getMethod('integers', Integer[]))
        assertTrue((getMethod('integers', Integer[]).modifiers & Opcodes.ACC_VARARGS) !== 0, 'method is not varargs')

        owner.invokeMethod('integers', new Iterable() {
            @Override
            Iterator iterator() {
                Stream.of(44).iterator()
            }
        })
        assertEquals(owner.integers.get().get(3), 44)
        assertNotNull(getMethod('integers', Iterable))

        owner.invokeMethod('configurableListed', owner.factory.newInstance(ConfigurableObject, 'dummy').tap {
            it.string = 'Hello!'
        })
        assert owner.configurableListed.get().size() == 1 && owner.configurableListed.get()[0].string.get() == 'Hello!'
        assertNotNull(getMethod('configurableListed', ConfigurableObject))

        owner.invokeMethod('configurableListed', {
            bool = true
        })
        assert owner.configurableListed.get().size() == 2 && owner.configurableListed.get()[1].bool.get() == true
        assertNotNull(getMethod('configurableListed', Closure))
        assertNotNull(getMethod('configurableListed', Action))

        owner.invokeMethod('configurableListed', [owner.factory.newInstance(ConfigurableObject, 'dummy'), {
            it.string = 'mhhm'
        } as Action<ConfigurableObject>].toArray())
        assert owner.configurableListed.get().size() == 3 && owner.configurableListed.get()[2].string.get() == 'mhhm'
        assertNotNull(getMethod('configurableListed', ConfigurableObject, Closure))
        assertNotNull(getMethod('configurableListed', ConfigurableObject, Action))
    }
    
    @Test
    void "enum string variants are generated"() {
        owner.invokeMethod('enumValue', 'maybe')
        assertEquals(owner.enumValue.get(), TestEnum.MAYBE)
        assertNotNull(getMethod('enumValue', String))
        assertNotNull(getMethod('enumValue', TestEnum))
    }

    @Test
    void "Map properties generate methods"() {
        owner.invokeMethod('mapEntry', ['key1', 'value1'].toArray())
        assertEquals(owner.map.get(), [key1: 'value1'])
        assertNotNull(getMethod('mapEntry', String, String))

        owner.invokeMethod('map', ['hello world': 'sup?'])
        assertEquals(owner.map.get(), [key1: 'value1', 'hello world': 'sup?'])
        assertNotNull(getMethod('map', Map))

        owner.map.set(new HashMap<String, String>())
        owner.invokeMethod('map', ['hi', 'its a me', 'mario', 'ok?'] as String[])
        assertEquals(owner.map.get(), [hi: 'its a me', 'mario': 'ok?'])
        assertNotNull(getMethod('map', String[]))

        assertThrows(IllegalArgumentException) {
            owner.invokeMethod('map', ['abc', 'def', 'ghi'] as String[])
        }

        assertThrows(ClassCastException) {
            owner.invokeMethod('weirdMap', [12, new AtomicInteger()] as Object[])
        }

        assertThrows(ClassCastException) {
            owner.invokeMethod('weirdMap', ['abcd', TestEnum.DEFINITELY] as Object[])
        }

        owner.invokeMethod('weirdMap', [13, TestEnum.DEFINITELY] as Object[])
        assertEquals(owner.weirdMap.get(), [13: TestEnum.DEFINITELY])
    }

    @Test
    void "Property of configurable type generates methods"() {
        owner.objectWhichCanBeConfigured.convention(owner.factory.newInstance(ConfigurableObject, 'dummy'))

        owner.invokeMethod('objectWhichCanBeConfigured', {
            string = 'hm, no'
        })
        assertEquals(owner.objectWhichCanBeConfigured.get().string.get(), 'hm, no')

        assertNotNull(getMethod('objectWhichCanBeConfigured', Action))
        assertNotNull(getMethod('objectWhichCanBeConfigured', Closure))

        // We expect a setter as it's a Property
        assertNotNull(getMethod('objectWhichCanBeConfigured', ConfigurableObject))
        assertNotNull(getMethod('objectWhichCanBeConfigured', ConfigurableObject, Action))
        assertNotNull(getMethod('objectWhichCanBeConfigured', ConfigurableObject, Closure))
    }

    @Test
    void "Direct properties generate only getter methods"() {
        owner.invokeMethod('subOwner', {
            value = 'abc'
        })
        assertEquals(owner.subOwner.value.get(), 'abc')

        assertNotNull(getMethod('subOwner', Action))
        assertNotNull(getMethod('subOwner', Closure))

        // We don't expect a setter as it's direct
        assertNull(getMethod('subOwner', NiceSubOwner))
        assertNull(getMethod('subOwner', NiceSubOwner, Action))
        assertNull(getMethod('subOwner', NiceSubOwner, Closure))
    }

    private static MethodNode getMethod(String name, Class... parameters) {
        return node.getMethod(name, Stream.of(parameters).map(ClassHelper.&make).map { GeneralUtils.param(it, 'p') }.toArray(Parameter[]::new))
    }
}
