package io.holixon.emn.dom4j

import org.assertj.core.api.Assertions
import org.dom4j.io.SAXReader
import org.junit.jupiter.api.Test
import java.io.StringReader

/**
 * Tests for DOM4j slice extraction extension functions.
 */
internal class Dom4jExtensionsSliceSetTest {

  @Test
  fun `sliceSet extracts slices from element`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:definitions xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
        <emn:sliceSet>
          <emn:slice id="slice-1" name="Slice 1">
            <emn:flowNodeRef>node-1</emn:flowNodeRef>
            <emn:flowNodeRef>node-2</emn:flowNodeRef>
          </emn:slice>
          <emn:slice id="slice-2" name="Slice 2">
            <emn:flowNodeRef>node-3</emn:flowNodeRef>
          </emn:slice>
        </emn:sliceSet>
      </emn:definitions>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))

    val slices = doc.rootElement.sliceSet()

    Assertions.assertThat(slices).hasSize(2)

    // First slice
    Assertions.assertThat(slices[0].id).isEqualTo("slice-1")
    Assertions.assertThat(slices[0].name).isEqualTo("Slice 1")
    Assertions.assertThat(slices[0].flowElements).hasSize(2)
    Assertions.assertThat(slices[0].flowElements[0].id).isEqualTo("node-1")
    Assertions.assertThat(slices[0].flowElements[1].id).isEqualTo("node-2")

    // Second slice
    Assertions.assertThat(slices[1].id).isEqualTo("slice-2")
    Assertions.assertThat(slices[1].name).isEqualTo("Slice 2")
    Assertions.assertThat(slices[1].flowElements).hasSize(1)
    Assertions.assertThat(slices[1].flowElements[0].id).isEqualTo("node-3")
  }

  @Test
  fun `sliceSet returns empty list when no slice set element exists`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:definitions xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
      </emn:definitions>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))

    val slices = doc.rootElement.sliceSet()

    Assertions.assertThat(slices).isEmpty()
  }

  @Test
  fun `sliceSet returns empty list when slice set element is empty`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:definitions xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
        <emn:sliceSet>
          <!-- Empty slice set -->
        </emn:sliceSet>
      </emn:definitions>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))

    val slices = doc.rootElement.sliceSet()

    Assertions.assertThat(slices).isEmpty()
  }
}
