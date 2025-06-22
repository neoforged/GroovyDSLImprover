/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.gdi.tests.property.file

import groovy.transform.CompileStatic
import net.neoforged.gdi.annotations.DSLProperty
import net.neoforged.gdi.annotations.ProjectGetter
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty

import javax.inject.Inject

@CompileStatic
abstract class FilePropertyOwner {
    final Project project
    @Inject
    FilePropertyOwner(Project project) {
        this.project = project
    }

    @ProjectGetter
    Project getProject() { project }

    @DSLProperty
    abstract ConfigurableFileCollection getCollectionEntries()

    @DSLProperty
    abstract DirectoryProperty getDirectory()

    @DSLProperty
    abstract RegularFileProperty getFile()
}
