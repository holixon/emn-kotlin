package io.holixon.emn.dom4j

import io.holixon.emn.model.*
import org.assertj.core.api.Assertions.assertThat
import org.dom4j.io.SAXReader
import org.junit.jupiter.api.Test
import java.io.StringReader

/**
 * Tests for DOM4j flow type and information flow type extension functions.
 */
internal class Dom4jExtensionsFlowTypeTest {

  @Test
  fun `toFlowTypes parses view type elements`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:definitions xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
        <emn:viewType id="view-type-1" name="View Type 1">
          <emn:schema schemaFormat="application/json"><![CDATA[{"type": "object"}]]></emn:schema>
        </emn:viewType>
      </emn:definitions>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))
    val flowTypes = doc.rootElement.elements().toFlowTypes()

    assertThat(flowTypes).hasSize(1)
    assertThat(flowTypes[0]).isInstanceOf(ViewType::class.java)

    val viewType = flowTypes[0] as ViewType
    assertThat(viewType.id).isEqualTo("view-type-1")
    assertThat(viewType.name).isEqualTo("View Type 1")
    assertThat(viewType.schema).isNotNull
    assertThat(viewType.schema).isInstanceOf(EmbeddedSchema::class.java)
    assertThat((viewType.schema as EmbeddedSchema).content).contains("object")
  }

  @Test
  fun `toFlowTypes parses command type elements`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:definitions xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
        <emn:commandType id="command-type-1" name="Command Type 1">
          <emn:schema schemaFormat="application/json"><![CDATA[{"type": "object"}]]></emn:schema>
        </emn:commandType>
      </emn:definitions>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))
    val flowTypes = doc.rootElement.elements().toFlowTypes()

    assertThat(flowTypes).hasSize(1)
    assertThat(flowTypes[0]).isInstanceOf(CommandType::class.java)

    val commandType = flowTypes[0] as CommandType
    assertThat(commandType.id).isEqualTo("command-type-1")
    assertThat(commandType.name).isEqualTo("Command Type 1")
    assertThat(commandType.schema).isNotNull
  }

  @Test
  fun `toFlowTypes parses event type elements`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:definitions xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
        <emn:eventType id="event-type-1" name="Event Type 1">
          <emn:schema schemaFormat="application/json"><![CDATA[{"type": "object"}]]></emn:schema>
        </emn:eventType>
      </emn:definitions>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))
    val flowTypes = doc.rootElement.elements().toFlowTypes()

    assertThat(flowTypes).hasSize(1)
    assertThat(flowTypes[0]).isInstanceOf(EventType::class.java)

    val eventType = flowTypes[0] as EventType
    assertThat(eventType.id).isEqualTo("event-type-1")
    assertThat(eventType.name).isEqualTo("Event Type 1")
    assertThat(eventType.schema).isNotNull
  }

  @Test
  fun `toFlowTypes parses query type elements`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:definitions xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
        <emn:queryType id="query-type-1" name="Query Type 1">
          <emn:schema schemaFormat="application/json"><![CDATA[{"type": "object"}]]></emn:schema>
        </emn:queryType>
      </emn:definitions>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))
    val flowTypes = doc.rootElement.elements().toFlowTypes()

    assertThat(flowTypes).hasSize(1)
    assertThat(flowTypes[0]).isInstanceOf(QueryType::class.java)

    val queryType = flowTypes[0] as QueryType
    assertThat(queryType.id).isEqualTo("query-type-1")
    assertThat(queryType.name).isEqualTo("Query Type 1")
    assertThat(queryType.schema).isNotNull
  }

  @Test
  fun `toFlowTypes parses error type elements`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:definitions xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
        <emn:errorType id="error-type-1" name="Error Type 1">
          <emn:schema schemaFormat="application/json"><![CDATA[{"type": "object"}]]></emn:schema>
        </emn:errorType>
      </emn:definitions>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))
    val flowTypes = doc.rootElement.elements().toFlowTypes()

    assertThat(flowTypes).hasSize(1)
    assertThat(flowTypes[0]).isInstanceOf(ErrorType::class.java)

    val errorType = flowTypes[0] as ErrorType
    assertThat(errorType.id).isEqualTo("error-type-1")
    assertThat(errorType.name).isEqualTo("Error Type 1")
    assertThat(errorType.schema).isNotNull
  }

  @Test
  fun `toFlowTypes parses external event type elements`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:definitions xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
        <emn:externalEventType id="external-event-type-1" name="External Event Type 1">
          <emn:schema schemaFormat="application/json"><![CDATA[{"type": "object"}]]></emn:schema>
        </emn:externalEventType>
      </emn:definitions>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))
    val flowTypes = doc.rootElement.elements().toFlowTypes()

    assertThat(flowTypes).hasSize(1)
    assertThat(flowTypes[0]).isInstanceOf(ExternalEventType::class.java)

    val externalEventType = flowTypes[0] as ExternalEventType
    assertThat(externalEventType.id).isEqualTo("external-event-type-1")
    assertThat(externalEventType.name).isEqualTo("External Event Type 1")
    assertThat(externalEventType.schema).isNotNull
  }

  @Test
  fun `toFlowTypes parses external system type elements`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:definitions xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
        <emn:externalSystemType id="external-system-type-1" name="External System Type 1">
          <emn:schema schemaFormat="application/json"><![CDATA[{"type": "object"}]]></emn:schema>
        </emn:externalSystemType>
      </emn:definitions>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))
    val flowTypes = doc.rootElement.elements().toFlowTypes()

    assertThat(flowTypes).hasSize(1)
    assertThat(flowTypes[0]).isInstanceOf(ExternalSystemType::class.java)

    val externalSystemType = flowTypes[0] as ExternalSystemType
    assertThat(externalSystemType.id).isEqualTo("external-system-type-1")
    assertThat(externalSystemType.name).isEqualTo("External System Type 1")
    assertThat(externalSystemType.schema).isNotNull
  }

  @Test
  fun `toFlowTypes parses translation type elements`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:definitions xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
        <emn:translationType id="translation-type-1" name="Translation Type 1">
          <emn:schema schemaFormat="application/json"><![CDATA[{"type": "object"}]]></emn:schema>
        </emn:translationType>
      </emn:definitions>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))
    val flowTypes = doc.rootElement.elements().toFlowTypes()

    assertThat(flowTypes).hasSize(1)
    assertThat(flowTypes[0]).isInstanceOf(TranslationType::class.java)

    val translationType = flowTypes[0] as TranslationType
    assertThat(translationType.id).isEqualTo("translation-type-1")
    assertThat(translationType.name).isEqualTo("Translation Type 1")
    assertThat(translationType.schema).isNotNull
  }

  @Test
  fun `toFlowTypes parses automation type elements`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:definitions xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
        <emn:automationType id="automation-type-1" name="Automation Type 1">
          <emn:schema schemaFormat="application/json"><![CDATA[{"type": "object"}]]></emn:schema>
        </emn:automationType>
      </emn:definitions>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))
    val flowTypes = doc.rootElement.elements().toFlowTypes()

    assertThat(flowTypes).hasSize(1)
    assertThat(flowTypes[0]).isInstanceOf(AutomationType::class.java)

    val automationType = flowTypes[0] as AutomationType
    assertThat(automationType.id).isEqualTo("automation-type-1")
    assertThat(automationType.name).isEqualTo("Automation Type 1")
    assertThat(automationType.schema).isNotNull
  }

  @Test
  fun `toInformationFlowType parses information flow type elements`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:definitions xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
        <emn:informationFlowType id="flow-type-1" name="Flow Type 1" sourceRef="source-1" targetRef="target-1" />
      </emn:definitions>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))
    val flowTypes = doc.rootElement.elements().toInformationFlowType()

    assertThat(flowTypes).hasSize(1)

    val flowType = flowTypes[0]
    assertThat(flowType.id).isEqualTo("flow-type-1")
    assertThat(flowType.name).isEqualTo("Flow Type 1")
    assertThat(flowType.source.id).isEqualTo("source-1")
    assertThat(flowType.target.id).isEqualTo("target-1")
  }

  @Test
  fun `toInformationFlowType ignores non-information flow type elements`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:definitions xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
        <emn:informationFlowType id="flow-type-1" name="Flow Type 1" sourceRef="source-1" targetRef="target-1" />
        <emn:otherElement id="other-1" />
      </emn:definitions>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))
    val flowTypes = doc.rootElement.elements().toInformationFlowType()

    assertThat(flowTypes).hasSize(1)
    assertThat(flowTypes[0].id).isEqualTo("flow-type-1")
  }

  @Test
  fun `toFlowTypes ignores non-flow type elements`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:definitions xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
        <emn:viewType id="view-type-1" name="View Type 1" />
        <emn:otherElement id="other-1" />
      </emn:definitions>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))
    val flowTypes = doc.rootElement.elements().toFlowTypes()

    assertThat(flowTypes).hasSize(1)
    assertThat(flowTypes[0].id).isEqualTo("view-type-1")
  }
}
