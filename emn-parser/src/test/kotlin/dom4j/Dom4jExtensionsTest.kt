package io.holixon.emn.dom4j

import org.assertj.core.api.Assertions.assertThat
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
}
