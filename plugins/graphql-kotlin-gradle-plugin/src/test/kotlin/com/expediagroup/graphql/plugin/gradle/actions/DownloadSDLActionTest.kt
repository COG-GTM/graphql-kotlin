/*
 * Copyright 2025 Expedia, Inc
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

import com.expediagroup.graphql.plugin.client.downloadSchema
import com.expediagroup.graphql.plugin.gradle.config.TimeoutConfiguration
import com.expediagroup.graphql.plugin.gradle.parameters.RetrieveSchemaParameters
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.gradle.api.provider.MapProperty
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

class DownloadSDLActionTest {

    @BeforeEach
    fun setUp() {
        mockkStatic("com.expediagroup.graphql.plugin.client.DownloadSchemaKt")
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic("com.expediagroup.graphql.plugin.client.DownloadSchemaKt")
    }

    private fun createAction(params: RetrieveSchemaParameters): DownloadSDLAction {
        val action = mockk<DownloadSDLAction>(relaxed = false)
        every { action.parameters } returns params
        every { action.execute() } answers { callOriginal() }
        return action
    }

    private fun mockParameters(
        endpoint: String,
        headers: Map<String, Any>,
        timeout: TimeoutConfiguration,
        schemaFile: File
    ): RetrieveSchemaParameters {
        val params = mockk<RetrieveSchemaParameters>()
        val endpointProp = mockk<Property<String>>()
        val headersProp = mockk<MapProperty<String, Any>>()
        val timeoutProp = mockk<Property<TimeoutConfiguration>>()
        val schemaFileProp = mockk<Property<File>>()

        every { endpointProp.get() } returns endpoint
        every { headersProp.get() } returns headers
        every { timeoutProp.get() } returns timeout
        every { schemaFileProp.get() } returns schemaFile

        every { params.endpoint } returns endpointProp
        every { params.headers } returns headersProp
        every { params.timeoutConfiguration } returns timeoutProp
        every { params.schemaFile } returns schemaFileProp

        return params
    }

    @Test
    fun `happy path - successfully downloads schema and writes to file`(@TempDir tempDir: Path) {
        val schemaFile = tempDir.resolve("schema.graphql").toFile()
        val endpoint = "http://localhost:8080/sdl"
        val headers = mapOf<String, Any>("Authorization" to "Bearer token123")
        val timeout = TimeoutConfiguration(connect = 5000, read = 15000)
        val expectedSchema = "type Query { hello: String }"

        every { downloadSchema(endpoint, headers, timeout.connect, timeout.read) } returns expectedSchema

        val params = mockParameters(endpoint, headers, timeout, schemaFile)
        val action = createAction(params)

        action.execute()

        assertTrue(schemaFile.exists())
        assertEquals(expectedSchema, schemaFile.readText())
        verify(exactly = 1) { downloadSchema(endpoint, headers, timeout.connect, timeout.read) }
    }

    @Test
    fun `downloads schema with empty headers`(@TempDir tempDir: Path) {
        val schemaFile = tempDir.resolve("schema.graphql").toFile()
        val endpoint = "http://localhost:8080/sdl"
        val headers = emptyMap<String, Any>()
        val timeout = TimeoutConfiguration()
        val expectedSchema = "type Query { world: String }"

        every { downloadSchema(endpoint, headers, timeout.connect, timeout.read) } returns expectedSchema

        val params = mockParameters(endpoint, headers, timeout, schemaFile)
        val action = createAction(params)

        action.execute()

        assertTrue(schemaFile.exists())
        assertEquals(expectedSchema, schemaFile.readText())
    }

    @Test
    fun `downloads schema with custom timeout values`(@TempDir tempDir: Path) {
        val schemaFile = tempDir.resolve("schema.graphql").toFile()
        val endpoint = "http://localhost:8080/sdl"
        val headers = emptyMap<String, Any>()
        val timeout = TimeoutConfiguration(connect = 10000, read = 30000)
        val expectedSchema = "type Query { custom: Boolean }"

        every { downloadSchema(endpoint, headers, timeout.connect, timeout.read) } returns expectedSchema

        val params = mockParameters(endpoint, headers, timeout, schemaFile)
        val action = createAction(params)

        action.execute()

        verify(exactly = 1) { downloadSchema(endpoint, headers, 10000L, 30000L) }
        assertEquals(expectedSchema, schemaFile.readText())
    }

    @Test
    fun `downloads schema with multiple headers`(@TempDir tempDir: Path) {
        val schemaFile = tempDir.resolve("schema.graphql").toFile()
        val endpoint = "http://localhost:8080/sdl"
        val headers = mapOf<String, Any>(
            "Authorization" to "Bearer token",
            "X-Custom-Header" to "custom-value",
            "X-Request-Id" to "12345"
        )
        val timeout = TimeoutConfiguration()
        val expectedSchema = "type Query { multiHeader: String }"

        every { downloadSchema(endpoint, headers, timeout.connect, timeout.read) } returns expectedSchema

        val params = mockParameters(endpoint, headers, timeout, schemaFile)
        val action = createAction(params)

        action.execute()

        verify(exactly = 1) { downloadSchema(endpoint, headers, timeout.connect, timeout.read) }
        assertEquals(expectedSchema, schemaFile.readText())
    }

    @Test
    fun `propagates exception when download fails`(@TempDir tempDir: Path) {
        val schemaFile = tempDir.resolve("schema.graphql").toFile()
        val endpoint = "http://localhost:8080/sdl"
        val headers = emptyMap<String, Any>()
        val timeout = TimeoutConfiguration()

        every { downloadSchema(endpoint, headers, timeout.connect, timeout.read) } throws RuntimeException("Connection refused")

        val params = mockParameters(endpoint, headers, timeout, schemaFile)
        val action = createAction(params)

        assertFailsWith<RuntimeException>("Connection refused") {
            action.execute()
        }
    }

    @Test
    fun `overwrites existing schema file`(@TempDir tempDir: Path) {
        val schemaFile = tempDir.resolve("schema.graphql").toFile()
        schemaFile.writeText("old schema content")
        val endpoint = "http://localhost:8080/sdl"
        val headers = emptyMap<String, Any>()
        val timeout = TimeoutConfiguration()
        val newSchema = "type Query { updated: String }"

        every { downloadSchema(endpoint, headers, timeout.connect, timeout.read) } returns newSchema

        val params = mockParameters(endpoint, headers, timeout, schemaFile)
        val action = createAction(params)

        action.execute()

        assertEquals(newSchema, schemaFile.readText())
    }
}
