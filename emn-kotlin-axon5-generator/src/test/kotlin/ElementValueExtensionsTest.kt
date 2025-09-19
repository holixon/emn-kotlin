package io.holixon.emn.generation

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.holixon.emn.model.EmbeddedValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ElementValueExtensionsTest {

    private val objectMapper = ObjectMapper().registerKotlinModule()

    @Test
    fun `getEmbeddedJsonValueAsMap should parse JSON string to Map`() {
        // Given
        val jsonContent = """{"courseId": "4711", "name": "Physics I"}"""
        val elementValue = EmbeddedValue(valueFormat = "application/json", content = jsonContent)

        // When
        val result = elementValue.getEmbeddedJsonValueAsMap(objectMapper)

        // Then
        assertThat(result).isNotNull
        assertThat(result).containsEntry("courseId", "4711")
        assertThat(result).containsEntry("name", "Physics I")
    }
}
