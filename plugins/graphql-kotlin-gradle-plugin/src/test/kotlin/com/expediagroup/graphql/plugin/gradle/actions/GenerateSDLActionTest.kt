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

import com.expediagroup.graphql.plugin.gradle.parameters.GenerateSDLParameters
import com.expediagroup.graphql.plugin.schema.generateSDL
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
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GenerateSDLActionTest {

    @BeforeEach
    fun setUp() {
        mockkStatic("com.expediagroup.graphql.plugin.schema.GenerateSDLKt")
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic("com.expediagroup.graphql.plugin.schema.GenerateSDLKt")
    }

    private fun createAction(params: GenerateSDLParameters): GenerateSDLAction {
        val action = mockk<GenerateSDLAction>(relaxed = false)
        every { action.parameters } returns params
        every { action.execute() } answers { callOriginal() }
        return action
    }

    private fun mockParameters(
        supportedPackages: List<String>,
        schemaFile: File
    ): GenerateSDLParameters {
        val params = mockk<GenerateSDLParameters>()
        val supportedPackagesProp = mockk<ListProperty<String>>()
        val schemaFileProp = mockk<Property<File>>()

        every { supportedPackagesProp.get() } returns supportedPackages
        every { schemaFileProp.get() } returns schemaFile

        every { params.supportedPackages } returns supportedPackagesProp
        every { params.schemaFile } returns schemaFileProp

        return params
    }

    @Test
    fun `happy path - generates SDL and writes to file`(@TempDir tempDir: Path) {
        val schemaFile = tempDir.resolve("schema.graphql").toFile()
        val supportedPackages = listOf("com.example.graphql")
        val expectedSDL = "type Query {\n  hello: String\n}"

        every { generateSDL(supportedPackages = supportedPackages) } returns expectedSDL

        val params = mockParameters(supportedPackages, schemaFile)
        val action = createAction(params)

        action.execute()

        assertTrue(schemaFile.exists())
        assertEquals(expectedSDL, schemaFile.readText())
        verify(exactly = 1) { generateSDL(supportedPackages = supportedPackages) }
    }

    @Test
    fun `generates SDL with multiple supported packages`(@TempDir tempDir: Path) {
        val schemaFile = tempDir.resolve("schema.graphql").toFile()
        val supportedPackages = listOf(
            "com.example.graphql.queries",
            "com.example.graphql.mutations",
            "com.example.graphql.types"
        )
        val expectedSDL = "type Query {\n  hello: String\n}\n\ntype Mutation {\n  update: Boolean\n}"

        every { generateSDL(supportedPackages = supportedPackages) } returns expectedSDL

        val params = mockParameters(supportedPackages, schemaFile)
        val action = createAction(params)

        action.execute()

        assertTrue(schemaFile.exists())
        assertEquals(expectedSDL, schemaFile.readText())
        verify(exactly = 1) { generateSDL(supportedPackages = supportedPackages) }
    }

    @Test
    fun `generates SDL with single package`(@TempDir tempDir: Path) {
        val schemaFile = tempDir.resolve("schema.graphql").toFile()
        val supportedPackages = listOf("com.example")
        val expectedSDL = "schema { query: Query }"

        every { generateSDL(supportedPackages = supportedPackages) } returns expectedSDL

        val params = mockParameters(supportedPackages, schemaFile)
        val action = createAction(params)

        action.execute()

        assertEquals(expectedSDL, schemaFile.readText())
    }

    @Test
    fun `propagates exception when SDL generation fails`(@TempDir tempDir: Path) {
        val schemaFile = tempDir.resolve("schema.graphql").toFile()
        val supportedPackages = listOf("com.example.graphql")

        every { generateSDL(supportedPackages = supportedPackages) } throws RuntimeException("Cannot generate SDL as multiple SchemaGeneratorHooksProviders were found on the classpath")

        val params = mockParameters(supportedPackages, schemaFile)
        val action = createAction(params)

        assertFailsWith<RuntimeException> {
            action.execute()
        }
    }

    @Test
    fun `overwrites existing schema file`(@TempDir tempDir: Path) {
        val schemaFile = tempDir.resolve("schema.graphql").toFile()
        schemaFile.writeText("old SDL content")
        val supportedPackages = listOf("com.example.graphql")
        val newSDL = "type Query {\n  updated: String\n}"

        every { generateSDL(supportedPackages = supportedPackages) } returns newSDL

        val params = mockParameters(supportedPackages, schemaFile)
        val action = createAction(params)

        action.execute()

        assertEquals(newSDL, schemaFile.readText())
    }

    @Test
    fun `generates SDL with empty result`(@TempDir tempDir: Path) {
        val schemaFile = tempDir.resolve("schema.graphql").toFile()
        val supportedPackages = listOf("com.example.graphql")
        val emptySDL = ""

        every { generateSDL(supportedPackages = supportedPackages) } returns emptySDL

        val params = mockParameters(supportedPackages, schemaFile)
        val action = createAction(params)

        action.execute()

        assertTrue(schemaFile.exists())
        assertEquals(emptySDL, schemaFile.readText())
    }
}
