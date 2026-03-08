/*
 * Copyright 2026 Expedia, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.expediagroup.graphql.plugin.gradle.actions

import com.expediagroup.graphql.plugin.gradle.parameters.GenerateGraalVmMetadataParameters
import com.expediagroup.graphql.plugin.graalvm.generateGraalVmMetadata
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.assertFailsWith

class GenerateGraalVmMetadataActionTest {

    @BeforeEach
    fun setUp() {
        mockkStatic("com.expediagroup.graphql.plugin.graalvm.GenerateGraalVmMetadataKt")
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic("com.expediagroup.graphql.plugin.graalvm.GenerateGraalVmMetadataKt")
    }

    private fun createAction(params: GenerateGraalVmMetadataParameters): GenerateGraalVmMetadataAction {
        val action = mockk<GenerateGraalVmMetadataAction>(relaxed = false)
        every { action.parameters } returns params
        every { action.execute() } answers { callOriginal() }
        return action
    }

    private fun mockParameters(
        supportedPackages: List<String>,
        mainClassName: String?,
        outputDirectory: File
    ): GenerateGraalVmMetadataParameters {
        val params = mockk<GenerateGraalVmMetadataParameters>()
        val supportedPackagesProp = mockk<ListProperty<String>>()
        val mainClassNameProp = mockk<Property<String>>()
        val outputDirectoryProp = mockk<Property<File>>()

        every { supportedPackagesProp.get() } returns supportedPackages
        if (mainClassName != null) {
            every { mainClassNameProp.orNull } returns mainClassName
        } else {
            every { mainClassNameProp.orNull } returns null
        }
        every { outputDirectoryProp.get() } returns outputDirectory

        every { params.supportedPackages } returns supportedPackagesProp
        every { params.mainClassName } returns mainClassNameProp
        every { params.outputDirectory } returns outputDirectoryProp

        return params
    }

    @Test
    fun `happy path - generates metadata with main class name`(@TempDir tempDir: Path) {
        val outputDirectory = tempDir.resolve("graalvm").toFile()
        outputDirectory.mkdirs()
        val supportedPackages = listOf("com.example.graphql")
        val mainClassName = "com.example.ApplicationKt"

        every { generateGraalVmMetadata(targetDirectory = outputDirectory, supportedPackages = supportedPackages, mainClassName = mainClassName) } returns Unit

        val params = mockParameters(supportedPackages, mainClassName, outputDirectory)
        val action = createAction(params)

        action.execute()

        verify(exactly = 1) {
            generateGraalVmMetadata(
                targetDirectory = outputDirectory,
                supportedPackages = supportedPackages,
                mainClassName = mainClassName
            )
        }
    }

    @Test
    fun `generates metadata without main class name (null)`(@TempDir tempDir: Path) {
        val outputDirectory = tempDir.resolve("graalvm").toFile()
        outputDirectory.mkdirs()
        val supportedPackages = listOf("com.example.graphql")

        every { generateGraalVmMetadata(targetDirectory = outputDirectory, supportedPackages = supportedPackages, mainClassName = null) } returns Unit

        val params = mockParameters(supportedPackages, null, outputDirectory)
        val action = createAction(params)

        action.execute()

        verify(exactly = 1) {
            generateGraalVmMetadata(
                targetDirectory = outputDirectory,
                supportedPackages = supportedPackages,
                mainClassName = null
            )
        }
    }

    @Test
    fun `generates metadata with multiple supported packages`(@TempDir tempDir: Path) {
        val outputDirectory = tempDir.resolve("graalvm").toFile()
        outputDirectory.mkdirs()
        val supportedPackages = listOf(
            "com.example.graphql.queries",
            "com.example.graphql.mutations",
            "com.example.graphql.types"
        )
        val mainClassName = "com.example.ApplicationKt"

        every { generateGraalVmMetadata(targetDirectory = outputDirectory, supportedPackages = supportedPackages, mainClassName = mainClassName) } returns Unit

        val params = mockParameters(supportedPackages, mainClassName, outputDirectory)
        val action = createAction(params)

        action.execute()

        verify(exactly = 1) {
            generateGraalVmMetadata(
                targetDirectory = outputDirectory,
                supportedPackages = supportedPackages,
                mainClassName = mainClassName
            )
        }
    }

    @Test
    fun `generates metadata with single package and no main class`(@TempDir tempDir: Path) {
        val outputDirectory = tempDir.resolve("graalvm").toFile()
        outputDirectory.mkdirs()
        val supportedPackages = listOf("com.example")

        every { generateGraalVmMetadata(targetDirectory = outputDirectory, supportedPackages = supportedPackages, mainClassName = null) } returns Unit

        val params = mockParameters(supportedPackages, null, outputDirectory)
        val action = createAction(params)

        action.execute()

        verify(exactly = 1) {
            generateGraalVmMetadata(
                targetDirectory = outputDirectory,
                supportedPackages = supportedPackages,
                mainClassName = null
            )
        }
    }

    @Test
    fun `propagates exception when metadata generation fails`(@TempDir tempDir: Path) {
        val outputDirectory = tempDir.resolve("graalvm").toFile()
        outputDirectory.mkdirs()
        val supportedPackages = listOf("com.example.graphql")
        val mainClassName = "com.example.ApplicationKt"

        every { generateGraalVmMetadata(targetDirectory = outputDirectory, supportedPackages = supportedPackages, mainClassName = mainClassName) } throws RuntimeException("Cannot generate SDL as multiple SchemaGeneratorHooksProviders were found on the classpath")

        val params = mockParameters(supportedPackages, mainClassName, outputDirectory)
        val action = createAction(params)

        assertFailsWith<RuntimeException> {
            action.execute()
        }
    }

    @Test
    fun `verifies parameters are correctly extracted from properties`(@TempDir tempDir: Path) {
        val outputDirectory = tempDir.resolve("graalvm").toFile()
        outputDirectory.mkdirs()
        val supportedPackages = listOf("com.example.graphql")
        val mainClassName = "com.example.MainApplicationKt"

        every { generateGraalVmMetadata(targetDirectory = any(), supportedPackages = any(), mainClassName = any()) } returns Unit

        val params = mockParameters(supportedPackages, mainClassName, outputDirectory)
        val action = createAction(params)

        action.execute()

        verify(exactly = 1) {
            generateGraalVmMetadata(
                targetDirectory = outputDirectory,
                supportedPackages = listOf("com.example.graphql"),
                mainClassName = "com.example.MainApplicationKt"
            )
        }
    }
}
