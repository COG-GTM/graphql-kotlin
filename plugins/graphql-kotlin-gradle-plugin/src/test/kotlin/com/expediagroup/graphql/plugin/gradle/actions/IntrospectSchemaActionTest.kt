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

import com.expediagroup.graphql.plugin.client.introspectSchema
import com.expediagroup.graphql.plugin.gradle.config.TimeoutConfiguration
import com.expediagroup.graphql.plugin.gradle.parameters.IntrospectSchemaParameters
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

class IntrospectSchemaActionTest {

    @BeforeEach
    fun setUp() {
        mockkStatic("com.expediagroup.graphql.plugin.client.IntrospectSchemaKt")
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic("com.expediagroup.graphql.plugin.client.IntrospectSchemaKt")
    }

    private fun createAction(params: IntrospectSchemaParameters): IntrospectSchemaAction {
        val action = mockk<IntrospectSchemaAction>(relaxed = false)
        every { action.parameters } returns params
        every { action.execute() } answers { callOriginal() }
        return action
    }

    private fun mockParameters(
        endpoint: String,
        headers: Map<String, Any>,
        timeout: TimeoutConfiguration,
        schemaFile: File,
        streamResponse: Boolean
    ): IntrospectSchemaParameters {
        val params = mockk<IntrospectSchemaParameters>()
        val endpointProp = mockk<Property<String>>()
        val headersProp = mockk<MapProperty<String, Any>>()
        val timeoutProp = mockk<Property<TimeoutConfiguration>>()
        val schemaFileProp = mockk<Property<File>>()
        val streamResponseProp = mockk<Property<Boolean>>()

        every { endpointProp.get() } returns endpoint
        every { headersProp.get() } returns headers
        every { timeoutProp.get() } returns timeout
        every { schemaFileProp.get() } returns schemaFile
        every { streamResponseProp.get() } returns streamResponse

        every { params.endpoint } returns endpointProp
        every { params.headers } returns headersProp
        every { params.timeoutConfiguration } returns timeoutProp
        every { params.schemaFile } returns schemaFileProp
        every { params.streamResponse } returns streamResponseProp

        return params
    }

    @Test
    fun `happy path - successfully introspects schema and writes to file`(@TempDir tempDir: Path) {
        val schemaFile = tempDir.resolve("schema.graphql").toFile()
        val endpoint = "http://localhost:8080/graphql"
        val headers = mapOf<String, Any>("Authorization" to "Bearer token123")
        val timeout = TimeoutConfiguration(connect = 5000, read = 15000)
        val streamResponse = true
        val expectedSchema = "type Query { hello: String }"

        every { introspectSchema(endpoint, headers, timeout.connect, timeout.read, streamResponse) } returns expectedSchema

        val params = mockParameters(endpoint, headers, timeout, schemaFile, streamResponse)
        val action = createAction(params)

        action.execute()

        assertTrue(schemaFile.exists())
        assertEquals(expectedSchema, schemaFile.readText())
        verify(exactly = 1) { introspectSchema(endpoint, headers, timeout.connect, timeout.read, streamResponse) }
    }

    @Test
    fun `introspects schema with stream response disabled`(@TempDir tempDir: Path) {
        val schemaFile = tempDir.resolve("schema.graphql").toFile()
        val endpoint = "http://localhost:8080/graphql"
        val headers = emptyMap<String, Any>()
        val timeout = TimeoutConfiguration()
        val streamResponse = false
        val expectedSchema = "type Query { noStream: Boolean }"

        every { introspectSchema(endpoint, headers, timeout.connect, timeout.read, streamResponse) } returns expectedSchema

        val params = mockParameters(endpoint, headers, timeout, schemaFile, streamResponse)
        val action = createAction(params)

        action.execute()

        assertTrue(schemaFile.exists())
        assertEquals(expectedSchema, schemaFile.readText())
        verify(exactly = 1) { introspectSchema(endpoint, headers, timeout.connect, timeout.read, false) }
    }

    @Test
    fun `introspects schema with empty headers`(@TempDir tempDir: Path) {
        val schemaFile = tempDir.resolve("schema.graphql").toFile()
        val endpoint = "http://localhost:8080/graphql"
        val headers = emptyMap<String, Any>()
        val timeout = TimeoutConfiguration()
        val streamResponse = true
        val expectedSchema = "type Query { emptyHeaders: String }"

        every { introspectSchema(endpoint, headers, timeout.connect, timeout.read, streamResponse) } returns expectedSchema

        val params = mockParameters(endpoint, headers, timeout, schemaFile, streamResponse)
        val action = createAction(params)

        action.execute()

        assertEquals(expectedSchema, schemaFile.readText())
    }

    @Test
    fun `introspects schema with custom timeout values`(@TempDir tempDir: Path) {
        val schemaFile = tempDir.resolve("schema.graphql").toFile()
        val endpoint = "http://localhost:8080/graphql"
        val headers = emptyMap<String, Any>()
        val timeout = TimeoutConfiguration(connect = 10000, read = 30000)
        val streamResponse = true
        val expectedSchema = "type Query { customTimeout: Int }"

        every { introspectSchema(endpoint, headers, 10000L, 30000L, streamResponse) } returns expectedSchema

        val params = mockParameters(endpoint, headers, timeout, schemaFile, streamResponse)
        val action = createAction(params)

        action.execute()

        verify(exactly = 1) { introspectSchema(endpoint, headers, 10000L, 30000L, streamResponse) }
        assertEquals(expectedSchema, schemaFile.readText())
    }

    @Test
    fun `propagates exception when introspection fails`(@TempDir tempDir: Path) {
        val schemaFile = tempDir.resolve("schema.graphql").toFile()
        val endpoint = "http://localhost:8080/graphql"
        val headers = emptyMap<String, Any>()
        val timeout = TimeoutConfiguration()
        val streamResponse = true

        every { introspectSchema(endpoint, headers, timeout.connect, timeout.read, streamResponse) } throws RuntimeException("Connection refused")

        val params = mockParameters(endpoint, headers, timeout, schemaFile, streamResponse)
        val action = createAction(params)

        assertFailsWith<RuntimeException>("Connection refused") {
            action.execute()
        }
    }

    @Test
    fun `overwrites existing schema file`(@TempDir tempDir: Path) {
        val schemaFile = tempDir.resolve("schema.graphql").toFile()
        schemaFile.writeText("old introspection result")
        val endpoint = "http://localhost:8080/graphql"
        val headers = emptyMap<String, Any>()
        val timeout = TimeoutConfiguration()
        val streamResponse = true
        val newSchema = "type Query { updated: String }"

        every { introspectSchema(endpoint, headers, timeout.connect, timeout.read, streamResponse) } returns newSchema

        val params = mockParameters(endpoint, headers, timeout, schemaFile, streamResponse)
        val action = createAction(params)

        action.execute()

        assertEquals(newSchema, schemaFile.readText())
    }

    @Test
    fun `introspects schema with multiple headers`(@TempDir tempDir: Path) {
        val schemaFile = tempDir.resolve("schema.graphql").toFile()
        val endpoint = "http://localhost:8080/graphql"
        val headers = mapOf<String, Any>(
            "Authorization" to "Bearer token",
            "X-Custom-Header" to "custom-value",
            "Accept" to "application/json"
        )
        val timeout = TimeoutConfiguration()
        val streamResponse = true
        val expectedSchema = "type Query { multiHeader: String }"

        every { introspectSchema(endpoint, headers, timeout.connect, timeout.read, streamResponse) } returns expectedSchema

        val params = mockParameters(endpoint, headers, timeout, schemaFile, streamResponse)
        val action = createAction(params)

        action.execute()

        verify(exactly = 1) { introspectSchema(endpoint, headers, timeout.connect, timeout.read, streamResponse) }
        assertEquals(expectedSchema, schemaFile.readText())
    }
}
