package io.holixon.emn.dom4j

import io.holixon.emn.model.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.dom4j.io.SAXReader
import org.junit.jupiter.api.Test
import java.io.StringReader

/**
 * Tests for DOM4j extractFlowElements extension function, focusing on error conditions.
 */
internal class Dom4jExtensionsExtractFlowElementsTest {

  @Test
  fun `extractFlowElements throws exception when source element is not found`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:definitions xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
        <emn:types>
          <emn:commandType id="command-type-1" name="Command Type 1" />
          <emn:informationFlowType id="flow-type-1" name="Flow Type 1" sourceRef="unknown-source-type" targetRef="command-type-1" />
        </emn:types>
        <emn:timeline id="timeline-1">
          <emn:command id="command-1" typeRef="command-type-1" />
          <emn:informationFlow id="flow-1" typeRef="flow-type-1" sourceRef="unknown-source" targetRef="command-1" />
        </emn:timeline>
      </emn:definitions>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))
    val element = doc.rootElement.emnElement("timeline")
    requireNotNull(element) { "Timeline element not found" }

    // Create type maps for the test
    val commandType = CommandType(id = "command-type-1", name = "Command Type 1", schema = null)
    val typesById = mapOf("command-type-1" to commandType)

    val flowType = InformationFlowType(
      id = "flow-type-1",
      name = "Flow Type 1",
      source = FlowNodeTypeReference("unknown-source-type"),
      target = FlowNodeTypeReference("command-type-1")
    )
    val informationFlowTypesById = mapOf("flow-type-1" to flowType)

    assertThatThrownBy { element.extractFlowElements(typesById, informationFlowTypesById) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessageContaining("Expected source element with id 'unknown-source' to exist, but it was not found.")
  }

  @Test
  fun `extractFlowElements throws exception when target element is not found`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:definitions xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
        <emn:types>
          <emn:eventType id="event-type-1" name="Event Type 1" />
          <emn:informationFlowType id="flow-type-1" name="Flow Type 1" sourceRef="event-type-1" targetRef="unknown-target-type" />
        </emn:types>
        <emn:timeline id="timeline-1">
          <emn:event id="event-1" typeRef="event-type-1" />
          <emn:informationFlow id="flow-1" typeRef="flow-type-1" sourceRef="event-1" targetRef="unknown-target" />
        </emn:timeline>
      </emn:definitions>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))
    val element = doc.rootElement.emnElement("timeline")
    requireNotNull(element) { "Timeline element not found" }

    // Create type maps for the test
    val eventType = EventType(id = "event-type-1", name = "Event Type 1", schema = null)
    val typesById = mapOf("event-type-1" to eventType)

    val flowType = InformationFlowType(
      id = "flow-type-1",
      name = "Flow Type 1",
      source = FlowNodeTypeReference("event-type-1"),
      target = FlowNodeTypeReference("unknown-target-type")
    )
    val informationFlowTypesById = mapOf("flow-type-1" to flowType)

    assertThatThrownBy { element.extractFlowElements(typesById, informationFlowTypesById) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessageContaining("Expected target element with id 'unknown-target' to exist, but it was not found.")
  }

  @Test
  fun `extractFlowElements throws exception when flow type is not found`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:definitions xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
        <emn:types>
          <emn:eventType id="event-type-1" name="Event Type 1" />
          <emn:commandType id="command-type-1" name="Command Type 1" />
          <!-- No flow type defined for unknown-flow-type -->
        </emn:types>
        <emn:timeline id="timeline-1">
          <emn:event id="event-1" typeRef="event-type-1" />
          <emn:command id="command-1" typeRef="command-type-1" />
          <emn:informationFlow id="flow-1" typeRef="unknown-flow-type" sourceRef="event-1" targetRef="command-1" />
        </emn:timeline>
      </emn:definitions>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))
    val element = doc.rootElement.emnElement("timeline")
    requireNotNull(element) { "Timeline element not found" }

    // Create type maps for the test
    val eventType = EventType(id = "event-type-1", name = "Event Type 1", schema = null)
    val commandType = CommandType(id = "command-type-1", name = "Command Type 1", schema = null)
    val typesById = mapOf(
      "event-type-1" to eventType,
      "command-type-1" to commandType
    )

    // Empty flow types map to trigger the error
    val informationFlowTypesById = emptyMap<String, InformationFlowType>()

    assertThatThrownBy { element.extractFlowElements(typesById, informationFlowTypesById) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessageContaining("Expected flow type with id 'unknown-flow-type' to exist, but it was not found.")
  }

  @Test
  fun `extractFlowElements handles empty elements correctly`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:definitions xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
        <emn:types>
          <!-- No types defined -->
        </emn:types>
        <emn:timeline id="timeline-1">
          <!-- No elements defined -->
        </emn:timeline>
      </emn:definitions>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))
    val element = doc.rootElement.emnElement("timeline")
    requireNotNull(element) { "Timeline element not found" }

    // Create empty type maps for the test
    val typesById = emptyMap<String, FlowNodeType>()
    val informationFlowTypesById = emptyMap<String, InformationFlowType>()

    val (nodes, flows) = element.extractFlowElements(typesById, informationFlowTypesById)

    assertThat(nodes).isEmpty()
    assertThat(flows).isEmpty()
  }
}
