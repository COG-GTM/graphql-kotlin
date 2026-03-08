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

import com.expediagroup.graphql.plugin.client.generateClient
import com.expediagroup.graphql.plugin.client.generator.GraphQLScalar
import com.expediagroup.graphql.plugin.client.generator.GraphQLSerializer
import com.expediagroup.graphql.plugin.gradle.config.GraphQLParserOptions
import com.expediagroup.graphql.plugin.gradle.parameters.GenerateClientParameters
import com.squareup.kotlinpoet.FileSpec
import graphql.parser.ParserOptions
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
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

class GenerateClientActionTest {

    @BeforeEach
    fun setUp() {
        mockkStatic("com.expediagroup.graphql.plugin.client.GenerateClientKt")
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic("com.expediagroup.graphql.plugin.client.GenerateClientKt")
    }

    private fun createAction(params: GenerateClientParameters): GenerateClientAction {
        val action = mockk<GenerateClientAction>(relaxed = false)
        every { action.parameters } returns params
        every { action.execute() } answers { callOriginal() }
        return action
    }

    private fun mockParameters(
        packageName: String,
        allowDeprecated: Boolean,
        customScalars: List<com.expediagroup.graphql.plugin.gradle.config.GraphQLScalar>,
        serializer: com.expediagroup.graphql.plugin.gradle.config.GraphQLSerializer,
        schemaPath: String,
        queryFiles: List<File>,
        targetDirectory: File,
        useOptionalInputWrapper: Boolean,
        parserOptions: GraphQLParserOptions
    ): GenerateClientParameters {
        val params = mockk<GenerateClientParameters>()
        val packageNameProp = mockk<Property<String>>()
        val allowDeprecatedProp = mockk<Property<Boolean>>()
        val customScalarsProp = mockk<ListProperty<com.expediagroup.graphql.plugin.gradle.config.GraphQLScalar>>()
        val serializerProp = mockk<Property<com.expediagroup.graphql.plugin.gradle.config.GraphQLSerializer>>()
        val schemaPathProp = mockk<Property<String>>()
        val queryFilesProp = mockk<ListProperty<File>>()
        val targetDirectoryProp = mockk<Property<File>>()
        val useOptionalInputWrapperProp = mockk<Property<Boolean>>()
        val parserOptionsProp = mockk<Property<GraphQLParserOptions>>()

        every { packageNameProp.get() } returns packageName
        every { allowDeprecatedProp.get() } returns allowDeprecated
        every { customScalarsProp.get() } returns customScalars
        every { serializerProp.get() } returns serializer
        every { schemaPathProp.get() } returns schemaPath
        every { queryFilesProp.get() } returns queryFiles
        every { targetDirectoryProp.get() } returns targetDirectory
        every { useOptionalInputWrapperProp.get() } returns useOptionalInputWrapper
        every { parserOptionsProp.get() } returns parserOptions

        every { params.packageName } returns packageNameProp
        every { params.allowDeprecated } returns allowDeprecatedProp
        every { params.customScalars } returns customScalarsProp
        every { params.serializer } returns serializerProp
        every { params.schemaPath } returns schemaPathProp
        every { params.queryFiles } returns queryFilesProp
        every { params.targetDirectory } returns targetDirectoryProp
        every { params.useOptionalInputWrapper } returns useOptionalInputWrapperProp
        every { params.parserOptions } returns parserOptionsProp

        return params
    }

    @Test
    fun `happy path - generates client with valid parameters`(@TempDir tempDir: Path) {
        val targetDirectory = tempDir.resolve("generated").toFile()
        targetDirectory.mkdirs()
        val schemaPath = "schema.graphql"
        val queryFiles = listOf(File("query.graphql"))
        val packageName = "com.example.generated"
        val parserOptions = GraphQLParserOptions()

        val realFileSpec = FileSpec.builder("com.example.generated", "TestQuery").build()

        every {
            generateClient(
                packageName = packageName,
                allowDeprecated = false,
                customScalarsMap = emptyList(),
                serializer = GraphQLSerializer.JACKSON,
                schemaPath = schemaPath,
                queries = queryFiles,
                useOptionalInputWrapper = false,
                parserOptions = any<ParserOptions.Builder.() -> Unit>(),
                useSharedResponseTypes = false
            )
        } returns listOf(realFileSpec)

        val params = mockParameters(
            packageName = packageName,
            allowDeprecated = false,
            customScalars = emptyList(),
            serializer = com.expediagroup.graphql.plugin.gradle.config.GraphQLSerializer.JACKSON,
            schemaPath = schemaPath,
            queryFiles = queryFiles,
            targetDirectory = targetDirectory,
            useOptionalInputWrapper = false,
            parserOptions = parserOptions
        )
        val action = createAction(params)

        action.execute()

        verify(exactly = 1) {
            generateClient(
                packageName = packageName,
                allowDeprecated = false,
                customScalarsMap = emptyList(),
                serializer = GraphQLSerializer.JACKSON,
                schemaPath = schemaPath,
                queries = queryFiles,
                useOptionalInputWrapper = false,
                parserOptions = any<ParserOptions.Builder.() -> Unit>(),
                useSharedResponseTypes = false
            )
        }
        assertTrue(targetDirectory.resolve("com/example/generated/TestQuery.kt").exists())
    }

    @Test
    fun `generates client with deprecated fields allowed`(@TempDir tempDir: Path) {
        val targetDirectory = tempDir.resolve("generated").toFile()
        targetDirectory.mkdirs()
        val schemaPath = "schema.graphql"
        val queryFiles = listOf(File("query.graphql"))
        val packageName = "com.example.generated"
        val parserOptions = GraphQLParserOptions()

        every {
            generateClient(
                packageName = packageName,
                allowDeprecated = true,
                customScalarsMap = any(),
                serializer = GraphQLSerializer.JACKSON,
                schemaPath = schemaPath,
                queries = queryFiles,
                useOptionalInputWrapper = false,
                parserOptions = any<ParserOptions.Builder.() -> Unit>(),
                useSharedResponseTypes = false
            )
        } returns emptyList()

        val params = mockParameters(
            packageName = packageName,
            allowDeprecated = true,
            customScalars = emptyList(),
            serializer = com.expediagroup.graphql.plugin.gradle.config.GraphQLSerializer.JACKSON,
            schemaPath = schemaPath,
            queryFiles = queryFiles,
            targetDirectory = targetDirectory,
            useOptionalInputWrapper = false,
            parserOptions = parserOptions
        )
        val action = createAction(params)

        action.execute()

        verify(exactly = 1) {
            generateClient(
                packageName = packageName,
                allowDeprecated = true,
                customScalarsMap = any(),
                serializer = GraphQLSerializer.JACKSON,
                schemaPath = schemaPath,
                queries = queryFiles,
                useOptionalInputWrapper = false,
                parserOptions = any<ParserOptions.Builder.() -> Unit>(),
                useSharedResponseTypes = false
            )
        }
    }

    @Test
    fun `generates client with custom scalars`(@TempDir tempDir: Path) {
        val targetDirectory = tempDir.resolve("generated").toFile()
        targetDirectory.mkdirs()
        val schemaPath = "schema.graphql"
        val queryFiles = listOf(File("query.graphql"))
        val packageName = "com.example.generated"
        val parserOptions = GraphQLParserOptions()

        val gradleScalars = listOf(
            com.expediagroup.graphql.plugin.gradle.config.GraphQLScalar("UUID", "java.util.UUID", "com.example.UUIDConverter")
        )
        val expectedScalars = listOf(
            GraphQLScalar("UUID", "java.util.UUID", "com.example.UUIDConverter")
        )

        val customScalarsSlot = slot<List<GraphQLScalar>>()

        every {
            generateClient(
                packageName = packageName,
                allowDeprecated = false,
                customScalarsMap = capture(customScalarsSlot),
                serializer = GraphQLSerializer.JACKSON,
                schemaPath = schemaPath,
                queries = queryFiles,
                useOptionalInputWrapper = false,
                parserOptions = any<ParserOptions.Builder.() -> Unit>(),
                useSharedResponseTypes = false
            )
        } returns emptyList()

        val params = mockParameters(
            packageName = packageName,
            allowDeprecated = false,
            customScalars = gradleScalars,
            serializer = com.expediagroup.graphql.plugin.gradle.config.GraphQLSerializer.JACKSON,
            schemaPath = schemaPath,
            queryFiles = queryFiles,
            targetDirectory = targetDirectory,
            useOptionalInputWrapper = false,
            parserOptions = parserOptions
        )
        val action = createAction(params)

        action.execute()

        val capturedScalars = customScalarsSlot.captured
        assertTrue(capturedScalars.size == 1)
        assertTrue(capturedScalars[0].scalar == expectedScalars[0].scalar)
        assertTrue(capturedScalars[0].type == expectedScalars[0].type)
        assertTrue(capturedScalars[0].converter == expectedScalars[0].converter)
    }

    @Test
    fun `generates client with KOTLINX serializer`(@TempDir tempDir: Path) {
        val targetDirectory = tempDir.resolve("generated").toFile()
        targetDirectory.mkdirs()
        val schemaPath = "schema.graphql"
        val queryFiles = listOf(File("query.graphql"))
        val packageName = "com.example.generated"
        val parserOptions = GraphQLParserOptions()

        every {
            generateClient(
                packageName = packageName,
                allowDeprecated = false,
                customScalarsMap = any(),
                serializer = GraphQLSerializer.KOTLINX,
                schemaPath = schemaPath,
                queries = queryFiles,
                useOptionalInputWrapper = false,
                parserOptions = any<ParserOptions.Builder.() -> Unit>(),
                useSharedResponseTypes = false
            )
        } returns emptyList()

        val params = mockParameters(
            packageName = packageName,
            allowDeprecated = false,
            customScalars = emptyList(),
            serializer = com.expediagroup.graphql.plugin.gradle.config.GraphQLSerializer.KOTLINX,
            schemaPath = schemaPath,
            queryFiles = queryFiles,
            targetDirectory = targetDirectory,
            useOptionalInputWrapper = false,
            parserOptions = parserOptions
        )
        val action = createAction(params)

        action.execute()

        verify(exactly = 1) {
            generateClient(
                packageName = packageName,
                allowDeprecated = false,
                customScalarsMap = any(),
                serializer = GraphQLSerializer.KOTLINX,
                schemaPath = schemaPath,
                queries = queryFiles,
                useOptionalInputWrapper = false,
                parserOptions = any<ParserOptions.Builder.() -> Unit>(),
                useSharedResponseTypes = false
            )
        }
    }

    @Test
    fun `generates client with optional input wrapper enabled`(@TempDir tempDir: Path) {
        val targetDirectory = tempDir.resolve("generated").toFile()
        targetDirectory.mkdirs()
        val schemaPath = "schema.graphql"
        val queryFiles = listOf(File("query.graphql"))
        val packageName = "com.example.generated"
        val parserOptions = GraphQLParserOptions()

        every {
            generateClient(
                packageName = packageName,
                allowDeprecated = false,
                customScalarsMap = any(),
                serializer = GraphQLSerializer.JACKSON,
                schemaPath = schemaPath,
                queries = queryFiles,
                useOptionalInputWrapper = true,
                parserOptions = any<ParserOptions.Builder.() -> Unit>(),
                useSharedResponseTypes = false
            )
        } returns emptyList()

        val params = mockParameters(
            packageName = packageName,
            allowDeprecated = false,
            customScalars = emptyList(),
            serializer = com.expediagroup.graphql.plugin.gradle.config.GraphQLSerializer.JACKSON,
            schemaPath = schemaPath,
            queryFiles = queryFiles,
            targetDirectory = targetDirectory,
            useOptionalInputWrapper = true,
            parserOptions = parserOptions
        )
        val action = createAction(params)

        action.execute()

        verify(exactly = 1) {
            generateClient(
                packageName = packageName,
                allowDeprecated = false,
                customScalarsMap = any(),
                serializer = GraphQLSerializer.JACKSON,
                schemaPath = schemaPath,
                queries = queryFiles,
                useOptionalInputWrapper = true,
                parserOptions = any<ParserOptions.Builder.() -> Unit>(),
                useSharedResponseTypes = false
            )
        }
    }

    @Test
    fun `generates client with multiple query files`(@TempDir tempDir: Path) {
        val targetDirectory = tempDir.resolve("generated").toFile()
        targetDirectory.mkdirs()
        val schemaPath = "schema.graphql"
        val queryFiles = listOf(
            File("query1.graphql"),
            File("query2.graphql"),
            File("query3.graphql")
        )
        val packageName = "com.example.generated"
        val parserOptions = GraphQLParserOptions()

        every {
            generateClient(
                packageName = packageName,
                allowDeprecated = false,
                customScalarsMap = any(),
                serializer = GraphQLSerializer.JACKSON,
                schemaPath = schemaPath,
                queries = queryFiles,
                useOptionalInputWrapper = false,
                parserOptions = any<ParserOptions.Builder.() -> Unit>(),
                useSharedResponseTypes = false
            )
        } returns emptyList()

        val params = mockParameters(
            packageName = packageName,
            allowDeprecated = false,
            customScalars = emptyList(),
            serializer = com.expediagroup.graphql.plugin.gradle.config.GraphQLSerializer.JACKSON,
            schemaPath = schemaPath,
            queryFiles = queryFiles,
            targetDirectory = targetDirectory,
            useOptionalInputWrapper = false,
            parserOptions = parserOptions
        )
        val action = createAction(params)

        action.execute()

        verify(exactly = 1) {
            generateClient(
                packageName = packageName,
                allowDeprecated = false,
                customScalarsMap = any(),
                serializer = GraphQLSerializer.JACKSON,
                schemaPath = schemaPath,
                queries = queryFiles,
                useOptionalInputWrapper = false,
                parserOptions = any<ParserOptions.Builder.() -> Unit>(),
                useSharedResponseTypes = false
            )
        }
    }

    @Test
    fun `propagates exception when generation fails`(@TempDir tempDir: Path) {
        val targetDirectory = tempDir.resolve("generated").toFile()
        targetDirectory.mkdirs()
        val schemaPath = "schema.graphql"
        val queryFiles = listOf(File("query.graphql"))
        val packageName = "com.example.generated"
        val parserOptions = GraphQLParserOptions()

        every {
            generateClient(
                packageName = any(),
                allowDeprecated = any(),
                customScalarsMap = any(),
                serializer = any(),
                schemaPath = any(),
                queries = any(),
                useOptionalInputWrapper = any(),
                parserOptions = any<ParserOptions.Builder.() -> Unit>(),
                useSharedResponseTypes = any()
            )
        } throws RuntimeException("Schema parsing failed")

        val params = mockParameters(
            packageName = packageName,
            allowDeprecated = false,
            customScalars = emptyList(),
            serializer = com.expediagroup.graphql.plugin.gradle.config.GraphQLSerializer.JACKSON,
            schemaPath = schemaPath,
            queryFiles = queryFiles,
            targetDirectory = targetDirectory,
            useOptionalInputWrapper = false,
            parserOptions = parserOptions
        )
        val action = createAction(params)

        val exception = assertFailsWith<RuntimeException> {
            action.execute()
        }
        assertEquals("Schema parsing failed", exception.message)
    }

    @Test
    fun `generates client with multiple file specs and writes each to target`(@TempDir tempDir: Path) {
        val targetDirectory = tempDir.resolve("generated").toFile()
        targetDirectory.mkdirs()
        val schemaPath = "schema.graphql"
        val queryFiles = listOf(File("query.graphql"))
        val packageName = "com.example.generated"
        val parserOptions = GraphQLParserOptions()

        val realFileSpec1 = FileSpec.builder("com.example.generated", "Query1").build()
        val realFileSpec2 = FileSpec.builder("com.example.generated", "Query2").build()

        every {
            generateClient(
                packageName = any(),
                allowDeprecated = any(),
                customScalarsMap = any(),
                serializer = any(),
                schemaPath = any(),
                queries = any(),
                useOptionalInputWrapper = any(),
                parserOptions = any<ParserOptions.Builder.() -> Unit>(),
                useSharedResponseTypes = any()
            )
        } returns listOf(realFileSpec1, realFileSpec2)

        val params = mockParameters(
            packageName = packageName,
            allowDeprecated = false,
            customScalars = emptyList(),
            serializer = com.expediagroup.graphql.plugin.gradle.config.GraphQLSerializer.JACKSON,
            schemaPath = schemaPath,
            queryFiles = queryFiles,
            targetDirectory = targetDirectory,
            useOptionalInputWrapper = false,
            parserOptions = parserOptions
        )
        val action = createAction(params)

        action.execute()

        assertTrue(targetDirectory.resolve("com/example/generated/Query1.kt").exists())
        assertTrue(targetDirectory.resolve("com/example/generated/Query2.kt").exists())
    }
}
