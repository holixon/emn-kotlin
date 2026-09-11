package io.holixon.emn.dom4j

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.dom4j.io.SAXReader
import org.junit.jupiter.api.Test
import java.io.StringReader

/**
 * Tests for DOM4j extension functions.
 */
internal class Dom4jExtensionsTest {

  @Test
  fun `detects qualified namespace`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:definitions
        xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL"
        xmlns:other="https://other"
        targetNamespace="es"
      >
        <emn:types />
        <other:other />
      </emn:definitions>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))
    val types = doc.rootElement.element("types")
    assertThat(types.isEmn()).isTrue()
    val other = doc.rootElement.element("other")
    assertThat(other.isEmn()).isFalse()
  }

  @Test
  fun `detects default namespace`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <definitions
        xmlns:other="https://other"
        xmlns="https://holixon.io/spec/EMN/20241231/MODEL"
      >
        <types />
        <other:other />
      </definitions>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))
    val types = doc.rootElement.element("types")
    assertThat(types.isEmn()).isTrue()
    val other = doc.rootElement.element("other")
    assertThat(other.isEmn()).isFalse()
  }

  @Test
  fun `detects local namespace`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <definitions
        xmlns:other="https://other"
      >
        <types xmlns="https://holixon.io/spec/EMN/20241231/MODEL" />
        <other:other />
      </definitions>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))
    val types = doc.rootElement.element("types")
    assertThat(types.isEmn()).isTrue()
    val other = doc.rootElement.element("other")
    assertThat(other.isEmn()).isFalse()
  }

  @Test
  fun `emnElement returns element with EMN namespace`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:definitions
        xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL"
        xmlns:other="https://other"
        targetNamespace="es"
      >
        <emn:types />
        <other:types />
      </emn:definitions>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))
    val emnTypes = doc.rootElement.emnElement("types")
    assertThat(emnTypes).isNotNull
    assertThat(emnTypes!!.isEmn()).isTrue()

    // Should not return non-EMN element with same name
    val otherTypes = doc.rootElement.emnElement("other")
    assertThat(otherTypes).isNull()
  }

  @Test
  fun `emnElement with default namespace`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <definitions
        xmlns:other="https://other"
        xmlns="https://holixon.io/spec/EMN/20241231/MODEL"
      >
        <types />
        <other:types />
      </definitions>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))
    val emnTypes = doc.rootElement.emnElement("types")
    assertThat(emnTypes).isNotNull
    assertThat(emnTypes!!.isEmn()).isTrue()
  }

  @Test
  fun `emnElements returns all elements with EMN namespace`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:definitions
        xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL"
        xmlns:other="https://other"
        targetNamespace="es"
      >
        <emn:types />
        <emn:types />
        <other:types />
      </emn:definitions>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))
    val emnTypes = doc.rootElement.emnElements("types")
    assertThat(emnTypes).hasSize(2)
    assertThat(emnTypes.all { it.isEmn() }).isTrue()

    // Should not include non-EMN elements
    val otherTypes = doc.rootElement.emnElements("other")
    assertThat(otherTypes).isEmpty()
  }

  @Test
  fun `emnElements with default namespace`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <definitions
        xmlns:other="https://other"
        xmlns="https://holixon.io/spec/EMN/20241231/MODEL"
      >
        <types />
        <types />
        <other:types />
      </definitions>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))
    val emnTypes = doc.rootElement.emnElements("types")
    assertThat(emnTypes).hasSize(2)
    assertThat(emnTypes.all { it.isEmn() }).isTrue()
  }

  @Test
  fun `emnElements with mixed namespaces`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <definitions
        xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL"
        xmlns:other="https://other"
      >
        <types xmlns="https://holixon.io/spec/EMN/20241231/MODEL" />
        <emn:types />
        <other:types />
      </definitions>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))
    val emnTypes = doc.rootElement.emnElements("types")
    assertThat(emnTypes).hasSize(2)
    assertThat(emnTypes.all { it.isEmn() }).isTrue()
  }

  @Test
  fun `schemaFormat returns attribute value or default`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:definitions xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
        <emn:element schemaFormat="application/xml" />
        <emn:element />
      </emn:definitions>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))
    val elements = doc.rootElement.elements()

    // Element with schemaFormat attribute
    assertThat(elements[0].schemaFormat()).isEqualTo("application/xml")

    // Element without schemaFormat attribute (should return default)
    assertThat(elements[1].schemaFormat()).isEqualTo("application/json")
  }

  @Test
  fun `resource returns attribute value or null`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:definitions xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
        <emn:element resource="path/to/resource.json" />
        <emn:element />
      </emn:definitions>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))
    val elements = doc.rootElement.elements()

    // Element with resource attribute
    assertThat(elements[0].resource()).isEqualTo("path/to/resource.json")

    // Element without resource attribute
    assertThat(elements[1].resource()).isNull()
  }

  @Test
  fun `id returns attribute value or throws exception`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:definitions xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
        <emn:element id="element-1" />
        <emn:element />
      </emn:definitions>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))
    val elements = doc.rootElement.elements()

    // Element with id attribute
    assertThat(elements[0].id()).isEqualTo("element-1")

    // Element without id attribute (should throw exception)
    assertThatThrownBy { elements[1].id() }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessageContaining("Element must define 'id' attribute")
  }

  @Test
  fun `name returns attribute value or empty string`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:definitions xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
        <emn:element name="Element Name" />
        <emn:element />
      </emn:definitions>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))
    val elements = doc.rootElement.elements()

    // Element with name attribute
    assertThat(elements[0].name()).isEqualTo("Element Name")

    // Element without name attribute (should return empty string)
    assertThat(elements[1].name()).isEmpty()
  }

  @Test
  fun `sourceRef returns attribute value or throws exception`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:definitions xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
        <emn:flow sourceRef="source-1" />
        <emn:flow />
      </emn:definitions>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))
    val elements = doc.rootElement.elements()

    // Element with sourceRef attribute
    assertThat(elements[0].sourceRef()).isEqualTo("source-1")

    // Element without sourceRef attribute (should throw exception)
    assertThatThrownBy { elements[1].sourceRef() }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessageContaining("Message flow must define a 'sourceRef' attribute")
  }

  @Test
  fun `targetRef returns attribute value or throws exception`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:definitions xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
        <emn:flow targetRef="target-1" />
        <emn:flow />
      </emn:definitions>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))
    val elements = doc.rootElement.elements()

    // Element with targetRef attribute
    assertThat(elements[0].targetRef()).isEqualTo("target-1")

    // Element without targetRef attribute (should throw exception)
    assertThatThrownBy { elements[1].targetRef() }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessageContaining("Message flow must define a 'targetRef' attribute")
  }

  @Test
  fun `scenario returns attribute value or null`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:definitions xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
        <emn:element scenario="test-scenario" />
        <emn:element />
      </emn:definitions>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))
    val elements = doc.rootElement.elements()

    // Element with scenario attribute
    assertThat(elements[0].scenario()).isEqualTo("test-scenario")

    // Element without scenario attribute
    assertThat(elements[1].scenario()).isNull()
  }

  @Test
  fun `sliceRef returns attribute value or null`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:definitions xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
        <emn:element sliceRef="slice-1" />
        <emn:element />
      </emn:definitions>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))
    val elements = doc.rootElement.elements()

    // Element with sliceRef attribute
    assertThat(elements[0].sliceRef()).isEqualTo("slice-1")

    // Element without sliceRef attribute
    assertThat(elements[1].sliceRef()).isNull()
  }

  @Test
  fun `schema returns Schema object or null`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:definitions xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
        <emn:element>
          <emn:schema schemaFormat="application/xml"><![CDATA[<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"></xs:schema>]]></emn:schema>
        </emn:element>
        <emn:element>
          <emn:schema schemaFormat="application/json" resource="path/to/schema.json" />
        </emn:element>
        <emn:element />
      </emn:definitions>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))
    val elements = doc.rootElement.elements()

    // Element with embedded schema
    val embeddedSchema = elements[0].schema()
    assertThat(embeddedSchema).isNotNull
    assertThat(embeddedSchema).isInstanceOf(io.holixon.emn.model.EmbeddedSchema::class.java)
    assertThat((embeddedSchema as io.holixon.emn.model.EmbeddedSchema).schemaFormat).isEqualTo("application/xml")
    assertThat(embeddedSchema.content).contains("<xs:schema")

    // Element with resource schema
    val resourceSchema = elements[1].schema()
    assertThat(resourceSchema).isNotNull
    assertThat(resourceSchema).isInstanceOf(io.holixon.emn.model.ResourceSchema::class.java)
    assertThat((resourceSchema as io.holixon.emn.model.ResourceSchema).schemaFormat).isEqualTo("application/json")
    assertThat(resourceSchema.resource).isEqualTo("path/to/schema.json")

    // Element without schema
    assertThat(elements[2].schema()).isNull()
  }

  @Test
  fun `idSchema returns Schema object or null`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:definitions xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
        <emn:element>
          <emn:idSchema schemaFormat="application/xml"><![CDATA[<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"></xs:schema>]]></emn:idSchema>
        </emn:element>
        <emn:element>
          <emn:idSchema schemaFormat="application/json" resource="path/to/schema.json" />
        </emn:element>
        <emn:element />
      </emn:definitions>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))
    val elements = doc.rootElement.elements()

    // Element with embedded idSchema
    val embeddedSchema = elements[0].idSchema()
    assertThat(embeddedSchema).isNotNull
    assertThat(embeddedSchema).isInstanceOf(io.holixon.emn.model.EmbeddedSchema::class.java)
    assertThat((embeddedSchema as io.holixon.emn.model.EmbeddedSchema).schemaFormat).isEqualTo("application/xml")
    assertThat(embeddedSchema.content).contains("<xs:schema")

    // Element with resource idSchema
    val resourceSchema = elements[1].idSchema()
    assertThat(resourceSchema).isNotNull
    assertThat(resourceSchema).isInstanceOf(io.holixon.emn.model.ResourceSchema::class.java)
    assertThat((resourceSchema as io.holixon.emn.model.ResourceSchema).schemaFormat).isEqualTo("application/json")
    assertThat(resourceSchema.resource).isEqualTo("path/to/schema.json")

    // Element without idSchema
    assertThat(elements[2].idSchema()).isNull()
  }

  @Test
  fun `toSchema converts element to Schema object`() {
    val xmlWithContent = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:schema xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL" schemaFormat="application/xml"><![CDATA[<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"></xs:schema>]]></emn:schema>
    """.trimIndent()

    val xmlWithResource = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:schema xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL"
                 schemaFormat="application/json"
                 resource="path/to/schema.json" />
    """.trimIndent()

    val xmlEmpty = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:schema xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL" />
    """.trimIndent()

    // Element with content
    val docWithContent = SAXReader().read(StringReader(xmlWithContent))
    val embeddedSchema = docWithContent.rootElement.toSchema()
    assertThat(embeddedSchema).isNotNull
    assertThat(embeddedSchema).isInstanceOf(io.holixon.emn.model.EmbeddedSchema::class.java)
    assertThat((embeddedSchema as io.holixon.emn.model.EmbeddedSchema).schemaFormat).isEqualTo("application/xml")
    assertThat(embeddedSchema.content).contains("<xs:schema")

    // Element with resource
    val docWithResource = SAXReader().read(StringReader(xmlWithResource))
    val resourceSchema = docWithResource.rootElement.toSchema()
    assertThat(resourceSchema).isNotNull
    assertThat(resourceSchema).isInstanceOf(io.holixon.emn.model.ResourceSchema::class.java)
    assertThat((resourceSchema as io.holixon.emn.model.ResourceSchema).schemaFormat).isEqualTo("application/json")
    assertThat(resourceSchema.resource).isEqualTo("path/to/schema.json")

    // Empty element (no content or resource)
    val docEmpty = SAXReader().read(StringReader(xmlEmpty))
    assertThat(docEmpty.rootElement.toSchema()).isNull()
  }
}
