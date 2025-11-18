package io.holixon.emn.dom4j

import io.holixon.emn.model.InformationFlow
import io.holixon.emn.model.InformationFlowType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.dom4j.io.SAXReader
import org.junit.jupiter.api.Test
import java.io.StringReader

/**
 * Tests for DOM4j information flow related extension functions.
 */
internal class Dom4jExtensionsInformationFlowTest {

  @Test
  fun `untypedMessageFlowType creates reference with type ID`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:flow xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL" typeRef="flow-type-1" />
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))
    val element = doc.rootElement

    val typeReference = element.untypedMessageFlowType()
    assertThat(typeReference).isNotNull
    assertThat(typeReference).isInstanceOf(InformationFlowType.InformationTypeReference::class.java)
    assertThat(typeReference.id).isEqualTo("flow-type-1")
  }

  @Test
  fun `untypedMessageFlowType throws exception when typeRef attribute is missing`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:flow xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL" />
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))
    val element = doc.rootElement

    assertThatThrownBy { element.untypedMessageFlowType() }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessageContaining("Element must define a 'typeRef' attribute")
  }

  @Test
  fun `toInformationFlows converts elements to information flows`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:definitions xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
        <emn:informationFlow id="flow-1" typeRef="flow-type-1" sourceRef="source-1" targetRef="target-1" />
        <emn:informationFlow id="flow-2" typeRef="flow-type-2" sourceRef="source-2" targetRef="target-2" />
        <emn:otherElement id="other-1" />
      </emn:definitions>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))
    val elements = doc.rootElement.elements()

    val flows = elements.toInformationFlows()

    assertThat(flows).hasSize(2)

    // First flow
    assertThat(flows[0].id).isEqualTo("flow-1")
    assertThat(flows[0].typeReference.id).isEqualTo("flow-type-1")
    assertThat(flows[0].source.id).isEqualTo("source-1")
    assertThat(flows[0].target.id).isEqualTo("target-1")

    // Second flow
    assertThat(flows[1].id).isEqualTo("flow-2")
    assertThat(flows[1].typeReference.id).isEqualTo("flow-type-2")
    assertThat(flows[1].source.id).isEqualTo("source-2")
    assertThat(flows[1].target.id).isEqualTo("target-2")
  }

  @Test
  fun `toInformationFlows ignores non-informationFlow elements`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:definitions xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
        <emn:otherElement id="other-1" />
        <emn:anotherElement id="another-1" />
      </emn:definitions>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))
    val elements = doc.rootElement.elements()

    val flows = elements.toInformationFlows()

    assertThat(flows).isEmpty()
  }

  @Test
  fun `toInformationFlows throws exception when sourceRef attribute is missing`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:definitions xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
        <emn:informationFlow id="flow-1" typeRef="flow-type-1" targetRef="target-1" />
      </emn:definitions>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))
    val elements = doc.rootElement.elements()

    assertThatThrownBy { elements.toInformationFlows() }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessageContaining("Message flow must define a 'sourceRef' attribute")
  }

  @Test
  fun `toInformationFlows throws exception when targetRef attribute is missing`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:definitions xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
        <emn:informationFlow id="flow-1" typeRef="flow-type-1" sourceRef="source-1" />
      </emn:definitions>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))
    val elements = doc.rootElement.elements()

    assertThatThrownBy { elements.toInformationFlows() }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessageContaining("Message flow must define a 'targetRef' attribute")
  }
}
