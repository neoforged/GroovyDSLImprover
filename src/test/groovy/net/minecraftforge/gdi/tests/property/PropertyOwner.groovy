/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.gdi.tests.property

import groovy.transform.CompileStatic
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType
import net.minecraftforge.gdi.annotations.DSLProperty
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested

import javax.inject.Inject

@CompileStatic
interface PropertyOwner {
    @DSLProperty
    Property<String> getStringProperty()

    @DSLProperty
    Property<Boolean> getIsExisting()

    @DSLProperty
    ListProperty<Integer> getIntegers()

    @DSLProperty(factory = { getFactory().newInstance(ConfigurableObject, 'dummy') })
    ListProperty<ConfigurableObject> getConfigurableListed()

    @DSLProperty
    NamedDomainObjectContainer<ConfigurableObject> getConfigurableObjects()

    @DSLProperty
    Property<ConfigurableObject> getObjectWhichCanBeConfigured()

    @DSLProperty
    Property<TestEnum> getEnumValue()

    @DSLProperty(singularName = 'mapEntry')
    MapProperty<String, String> getMap()

    @DSLProperty(singularName = 'weirdMapEntry')
    MapProperty<Integer, TestEnum> getWeirdMap()

    @Inject
    ObjectFactory getFactory()

    @Nested
    @DSLProperty(isConfigurable = true)
    abstract NiceSubOwner getSubOwner()
}

@CompileStatic
abstract class NiceSubOwner {
    @DSLProperty
    abstract Property<String> getValue()
}

@CompileStatic
enum TestEnum {
    MAYBE,
    DEFINITELY
}