package io.holixon.emn.dom4j

import io.holixon.emn.model.EmbeddedValue
import io.holixon.emn.model.ResourceValue
import org.assertj.core.api.Assertions.assertThat
import org.dom4j.io.SAXReader
import org.junit.jupiter.api.Test
import java.io.StringReader

/**
 * Tests for DOM4j elementValue extension function.
 */
internal class Dom4jExtensionsElementValueTest {

  @Test
  fun `elementValue returns null when value element is not present`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:element xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
      </emn:element>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))
    val element = doc.rootElement

    val value = element.elementValue()
    assertThat(value).isNull()
  }

  @Test
  fun `elementValue returns EmbeddedValue when value element has content`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:element xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
        <emn:value valueFormat="application/xml"><![CDATA[<test>content</test>]]></emn:value>
      </emn:element>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))
    val element = doc.rootElement

    val value = element.elementValue()
    assertThat(value).isNotNull
    assertThat(value).isInstanceOf(EmbeddedValue::class.java)
    assertThat((value as EmbeddedValue).valueFormat).isEqualTo("application/xml")
    assertThat(value.content).isEqualTo("<test>content</test>")
  }

  @Test
  fun `elementValue returns ResourceValue when value element has resource attribute`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:element xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
        <emn:value valueFormat="application/json" resource="path/to/resource.json" />
      </emn:element>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))
    val element = doc.rootElement

    val value = element.elementValue()
    assertThat(value).isNotNull
    assertThat(value).isInstanceOf(ResourceValue::class.java)
    assertThat((value as ResourceValue).valueFormat).isEqualTo("application/json")
    assertThat(value.resource).isEqualTo("path/to/resource.json")
  }

  @Test
  fun `elementValue returns null when value element has neither content nor resource`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:element xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
        <emn:value valueFormat="application/json" />
      </emn:element>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))
    val element = doc.rootElement

    val value = element.elementValue()
    assertThat(value).isNull()
  }

  @Test
  fun `elementValue uses default valueFormat when not specified`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:element xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
        <emn:value><![CDATA[{"test": "content"}]]></emn:value>
      </emn:element>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))
    val element = doc.rootElement

    val value = element.elementValue()
    assertThat(value).isNotNull
    assertThat(value).isInstanceOf(EmbeddedValue::class.java)
    assertThat((value as EmbeddedValue).valueFormat).isEqualTo("application/json") // Default value
    assertThat(value.content).isEqualTo("{\"test\": \"content\"}")
  }
}
