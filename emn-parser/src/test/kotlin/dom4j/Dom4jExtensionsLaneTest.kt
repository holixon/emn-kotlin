package io.holixon.emn.dom4j

import io.holixon.emn.model.EmbeddedSchema
import org.assertj.core.api.Assertions.assertThat
import org.dom4j.io.SAXReader
import org.junit.jupiter.api.Test
import java.io.StringReader

/**
 * Tests for DOM4j lane extraction extension functions.
 */
internal class Dom4jExtensionsLaneTest {

  @Test
  fun `triggerLanes extracts trigger lanes from element`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:definitions xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
        <emn:laneSet id="laneSet-1" name="Lane Set">
          <emn:triggerLaneSet>
            <emn:triggerLane id="triggerLane-1" name="Trigger Lane 1">
              <emn:flowNodeRef>node-1</emn:flowNodeRef>
              <emn:flowNodeRef>node-2</emn:flowNodeRef>
            </emn:triggerLane>
            <emn:triggerLane id="triggerLane-2" name="Trigger Lane 2">
              <emn:flowNodeRef>node-3</emn:flowNodeRef>
            </emn:triggerLane>
          </emn:triggerLaneSet>
        </emn:laneSet>
      </emn:definitions>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))
    val laneSet = doc.rootElement.emnElement("laneSet")

    val triggerLanes = laneSet!!.triggerLanes()

    assertThat(triggerLanes).hasSize(2)

    // First trigger lane
    assertThat(triggerLanes[0].id).isEqualTo("triggerLane-1")
    assertThat(triggerLanes[0].name).isEqualTo("Trigger Lane 1")
    assertThat(triggerLanes[0].flowElements).hasSize(2)
    assertThat(triggerLanes[0].flowElements[0].id).isEqualTo("node-1")
    assertThat(triggerLanes[0].flowElements[1].id).isEqualTo("node-2")

    // Second trigger lane
    assertThat(triggerLanes[1].id).isEqualTo("triggerLane-2")
    assertThat(triggerLanes[1].name).isEqualTo("Trigger Lane 2")
    assertThat(triggerLanes[1].flowElements).hasSize(1)
    assertThat(triggerLanes[1].flowElements[0].id).isEqualTo("node-3")
  }

  @Test
  fun `conceptLanes extracts concept lanes from element`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:definitions xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
        <emn:laneSet id="laneSet-1" name="Lane Set">
          <emn:conceptLaneSet>
            <emn:conceptLane id="conceptLane-1" name="Concept Lane 1">
              <emn:flowNodeRef>node-1</emn:flowNodeRef>
              <emn:flowNodeRef>node-2</emn:flowNodeRef>
              <emn:idSchema schemaFormat="application/json"><![CDATA[{"type": "string"}]]></emn:idSchema>
            </emn:conceptLane>
            <emn:conceptLane id="conceptLane-2" name="Concept Lane 2">
              <emn:flowNodeRef>node-3</emn:flowNodeRef>
            </emn:conceptLane>
          </emn:conceptLaneSet>
        </emn:laneSet>
      </emn:definitions>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))
    val laneSet = doc.rootElement.emnElement("laneSet")

    val conceptLanes = laneSet!!.conceptLanes()

    assertThat(conceptLanes).hasSize(2)

    // First concept lane
    assertThat(conceptLanes[0].id).isEqualTo("conceptLane-1")
    assertThat(conceptLanes[0].name).isEqualTo("Concept Lane 1")
    assertThat(conceptLanes[0].flowElements).hasSize(2)
    assertThat(conceptLanes[0].flowElements[0].id).isEqualTo("node-1")
    assertThat(conceptLanes[0].flowElements[1].id).isEqualTo("node-2")
    assertThat(conceptLanes[0].idSchema).isNotNull
    assertThat((conceptLanes[0].idSchema as EmbeddedSchema).content).contains("string")

    // Second concept lane
    assertThat(conceptLanes[1].id).isEqualTo("conceptLane-2")
    assertThat(conceptLanes[1].name).isEqualTo("Concept Lane 2")
    assertThat(conceptLanes[1].flowElements).hasSize(1)
    assertThat(conceptLanes[1].flowElements[0].id).isEqualTo("node-3")
    assertThat(conceptLanes[1].idSchema).isNull()
  }

  @Test
  fun `laneSet extracts complete lane set from element`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:definitions xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
        <emn:laneSet id="laneSet-1" name="Lane Set">
          <emn:triggerLaneSet>
            <emn:triggerLane id="triggerLane-1" name="Trigger Lane">
              <emn:flowNodeRef>trigger-node</emn:flowNodeRef>
            </emn:triggerLane>
          </emn:triggerLaneSet>
          <emn:interactionLane id="interaction-1" name="Interaction Lane">
            <emn:flowNodeRef>interaction-node</emn:flowNodeRef>
          </emn:interactionLane>
          <emn:conceptLaneSet>
            <emn:conceptLane id="conceptLane-1" name="Concept Lane">
              <emn:flowNodeRef>concept-node</emn:flowNodeRef>
            </emn:conceptLane>
          </emn:conceptLaneSet>
        </emn:laneSet>
      </emn:definitions>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))
    val laneSet = doc.rootElement.laneSet()

    // Interaction lane
    assertThat(laneSet.interactionLane.id).isEqualTo("interaction-1")
    assertThat(laneSet.interactionLane.name).isEqualTo("Interaction Lane")
    assertThat(laneSet.interactionLane.flowElements).hasSize(1)
    assertThat(laneSet.interactionLane.flowElements[0].id).isEqualTo("interaction-node")

    // Trigger lane set
    assertThat(laneSet.triggerLaneSet).hasSize(1)
    assertThat(laneSet.triggerLaneSet[0].id).isEqualTo("triggerLane-1")
    assertThat(laneSet.triggerLaneSet[0].name).isEqualTo("Trigger Lane")
    assertThat(laneSet.triggerLaneSet[0].flowElements).hasSize(1)
    assertThat(laneSet.triggerLaneSet[0].flowElements[0].id).isEqualTo("trigger-node")

    // Concept lane set
    assertThat(laneSet.conceptLaneSet).hasSize(1)
    assertThat(laneSet.conceptLaneSet[0].id).isEqualTo("conceptLane-1")
    assertThat(laneSet.conceptLaneSet[0].name).isEqualTo("Concept Lane")
    assertThat(laneSet.conceptLaneSet[0].flowElements).hasSize(1)
    assertThat(laneSet.conceptLaneSet[0].flowElements[0].id).isEqualTo("concept-node")
  }

  @Test
  fun `laneSet returns empty lane set when no lane set element exists`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:definitions xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
      </emn:definitions>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))
    val laneSet = doc.rootElement.laneSet()

    assertThat(laneSet.interactionLane.id).isEqualTo("UNSET")
    assertThat(laneSet.interactionLane.name).isNull()
    assertThat(laneSet.interactionLane.flowElements).isEmpty()
    assertThat(laneSet.triggerLaneSet).isEmpty()
    assertThat(laneSet.conceptLaneSet).isEmpty()
  }

  @Test
  fun `laneSet returns lane set with empty trigger and concept lanes when lane sets are empty`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:definitions xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
        <emn:laneSet id="laneSet-1" name="Lane Set">
          <emn:triggerLaneSet>
            <!-- Empty trigger lane set -->
          </emn:triggerLaneSet>
          <emn:interactionLane id="interaction-1" name="Interaction Lane">
            <emn:flowNodeRef>interaction-node</emn:flowNodeRef>
          </emn:interactionLane>
          <emn:conceptLaneSet>
            <!-- Empty concept lane set -->
          </emn:conceptLaneSet>
        </emn:laneSet>
      </emn:definitions>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))
    val laneSet = doc.rootElement.laneSet()

    // Interaction lane should be populated
    assertThat(laneSet.interactionLane.id).isEqualTo("interaction-1")
    assertThat(laneSet.interactionLane.name).isEqualTo("Interaction Lane")
    assertThat(laneSet.interactionLane.flowElements).hasSize(1)
    assertThat(laneSet.interactionLane.flowElements[0].id).isEqualTo("interaction-node")

    // Trigger and concept lane sets should be empty
    assertThat(laneSet.triggerLaneSet).isEmpty()
    assertThat(laneSet.conceptLaneSet).isEmpty()
  }

  @Test
  fun `triggerLanes and conceptLanes returns empty list when lane set is empty`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:definitions xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
        <emn:laneSet id="laneSet-1" name="Lane Set">
          <!-- No trigger lane set -->
          <!-- No concept lane set -->
        </emn:laneSet>
      </emn:definitions>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))
    val laneSet = doc.rootElement.emnElement("laneSet")

    val triggerLanes = laneSet!!.triggerLanes()
    assertThat(triggerLanes).isEmpty()
    val conceptLanes = laneSet.conceptLanes()
    assertThat(conceptLanes).isEmpty()
  }

  @Test
  fun `triggerLanes returns empty list when trigger lane set is empty`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:definitions xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
        <emn:laneSet id="laneSet-1" name="Lane Set">
          <emn:triggerLaneSet>
            <!-- Empty trigger lane set -->
          </emn:triggerLaneSet>
        </emn:laneSet>
      </emn:definitions>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))
    val laneSet = doc.rootElement.emnElement("laneSet")

    val triggerLanes = laneSet!!.triggerLanes()
    assertThat(triggerLanes).isEmpty()
  }

  @Test
  fun `conceptLanes returns empty list when concept lane set is empty`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:definitions xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
        <emn:laneSet id="laneSet-1" name="Lane Set">
          <emn:conceptLaneSet>
            <!-- Empty concept lane set -->
          </emn:conceptLaneSet>
        </emn:laneSet>
      </emn:definitions>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))
    val laneSet = doc.rootElement.emnElement("laneSet")

    val conceptLanes = laneSet!!.conceptLanes()
    assertThat(conceptLanes).isEmpty()
  }
}
