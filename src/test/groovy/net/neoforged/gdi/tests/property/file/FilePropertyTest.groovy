/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.gdi.tests.property.file

import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.tools.GeneralUtils
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import java.util.stream.Collectors
import java.util.stream.Stream

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNotNull

class FilePropertyTest {
    @TempDir
    private static File gradleProjectDir
    private static FilePropertyOwner owner
    private static ClassNode node

    @BeforeAll
    @CompileStatic
    static void setup() {
        final Project project = ProjectBuilder.builder()
                .withProjectDir(gradleProjectDir)
                .withName("DummyTestingProject")
                .build()

        owner = project.getObjects().newInstance(FilePropertyOwner.class, project)
        node = ClassHelper.make(FilePropertyOwner)
    }

    @Test
    void "ConfigurableFileCollection property generates methods"() {
        owner.invokeMethod('collectionEntry', [[
                new File(gradleProjectDir, 'yes.txt'),
                'subdir/no.yes',
                gradleProjectDir.toPath().resolve('hmm.java')
        ].toArray()].toArray())
        assertEquals(owner.collectionEntries.files.stream()
            .map { it.name }.collect(Collectors.toList()), ['yes.txt', 'no.yes', 'hmm.java'])

        assertNotNull(getMethod('collectionEntry', Object))
        assertNotNull(getMethod('collectionEntries', Object[]))
    }

    @Test
    void "DirectoryProperty generates methods"() {
        owner.invokeMethod('directory', 'hmmm')
        assertEquals(owner.directory.get().asFile, new File(gradleProjectDir, 'hmmm'))

        assertNotNull(getMethod('directory', Object))
        assertNotNull(getMethod('directory', File))
        assertNotNull(getMethod('directory', Directory))
    }

    @Test
    void "RegularFileProperty generates methods"() {
        owner.invokeMethod('file', 'someFile.txt')
        assertEquals(owner.file.get().asFile, new File(gradleProjectDir, 'someFile.txt'))

        assertNotNull(getMethod('file', Object))
        assertNotNull(getMethod('file', File))
        assertNotNull(getMethod('file', RegularFile))
    }

    private static MethodNode getMethod(String name, Class... parameters) {
        return node.getMethod(name, Stream.of(parameters).map(ClassHelper.&make).map { GeneralUtils.param(it, 'p') }.toArray(Parameter[]::new))
    }
}