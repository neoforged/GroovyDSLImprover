/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.gdi.tests.property

import groovy.transform.CompileStatic
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory

import javax.inject.Inject

@CompileStatic
abstract class PropertyOwnerImpl implements PropertyOwner {

    final NamedDomainObjectContainer<ConfigurableObject> configurableObjects
    @Inject
    PropertyOwnerImpl(ObjectFactory factory) {
        configurableObjects = factory.domainObjectContainer(ConfigurableObject, {factory.newInstance(ConfigurableObject, it)})
    }

}
