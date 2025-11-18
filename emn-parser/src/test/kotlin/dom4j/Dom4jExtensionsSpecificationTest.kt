package io.holixon.emn.dom4j

import io.holixon.emn.model.*
import org.assertj.core.api.Assertions.assertThat
import org.dom4j.io.SAXReader
import org.junit.jupiter.api.Test
import java.io.StringReader

/**
 * Tests for DOM4j specification extraction extension functions.
 */
internal class Dom4jExtensionsSpecificationTest {

  @Test
  fun `givenStage extracts given stage from element`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:definitions xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
        <emn:given id="given-1" stateName="initial">
          <emn:event id="event-1" typeRef="event-type-1" />
        </emn:given>
      </emn:definitions>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))

    // Create a map of types for the test
    val eventType = EventType(
      id = "event-type-1",
      name = "Test Event Type",
      schema = null
    )
    val typesById = mapOf("event-type-1" to eventType)

    val givenStage = doc.rootElement.givenStage(typesById)

    assertThat(givenStage).isNotNull
    assertThat(givenStage!!.id).isEqualTo("given-1")
    assertThat(givenStage.stateName).isEqualTo("initial")
    assertThat(givenStage.values).hasSize(1)
    assertThat(givenStage.values[0]).isInstanceOf(Event::class.java)
    assertThat((givenStage.values[0] as Event).id).isEqualTo("event-1")
    assertThat((givenStage.values[0] as Event).typeReference).isEqualTo(eventType)
  }

  @Test
  fun `whenStage extracts when stage from element`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:definitions xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
        <emn:when id="when-1">
          <emn:command id="command-1" typeRef="command-type-1" />
        </emn:when>
      </emn:definitions>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))

    // Create a map of types for the test
    val commandType = CommandType(
      id = "command-type-1",
      name = "Test Command Type",
      schema = null
    )
    val typesById = mapOf("command-type-1" to commandType)

    val whenStage = doc.rootElement.whenStage(typesById)

    assertThat(whenStage).isNotNull
    assertThat(whenStage!!.id).isEqualTo("when-1")
    assertThat(whenStage.values).hasSize(1)
    assertThat(whenStage.values[0]).isInstanceOf(Command::class.java)
    assertThat((whenStage.values[0] as Command).id).isEqualTo("command-1")
    assertThat((whenStage.values[0] as Command).typeReference).isEqualTo(commandType)
  }

  @Test
  fun `thenStage extracts then stage from element`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:definitions xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
        <emn:then id="then-1">
          <emn:event id="event-2" typeRef="event-type-2" />
        </emn:then>
      </emn:definitions>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))

    // Create a map of types for the test
    val eventType = EventType(
      id = "event-type-2",
      name = "Test Event Type 2",
      schema = null
    )
    val typesById = mapOf("event-type-2" to eventType)

    val thenStage = doc.rootElement.thenStage(typesById)

    assertThat(thenStage).isNotNull
    assertThat(thenStage!!.id).isEqualTo("then-1")
    assertThat(thenStage.values).hasSize(1)
    assertThat(thenStage.values[0]).isInstanceOf(Event::class.java)
    assertThat((thenStage.values[0] as Event).id).isEqualTo("event-2")
    assertThat((thenStage.values[0] as Event).typeReference).isEqualTo(eventType)
  }

  @Test
  fun `toSpecification parses complete specification element`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:definitions xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
        <emn:specification id="spec-1" name="Test Specification" scenario="test-scenario" sliceRef="slice-1">
          <emn:given id="given-1" stateName="initial">
            <emn:event id="event-1" typeRef="event-type-1" />
          </emn:given>
          <emn:when id="when-1">
            <emn:command id="command-1" typeRef="command-type-1" />
          </emn:when>
          <emn:then id="then-1">
            <emn:event id="event-2" typeRef="event-type-2" />
          </emn:then>
        </emn:specification>
      </emn:definitions>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))
    val specElement = doc.rootElement.emnElement("specification")

    // Create a map of types for the test
    val eventType1 = EventType(id = "event-type-1", name = "Test Event Type 1", schema = null)
    val commandType = CommandType(id = "command-type-1", name = "Test Command Type", schema = null)
    val eventType2 = EventType(id = "event-type-2", name = "Test Event Type 2", schema = null)
    val typesById = mapOf(
      "event-type-1" to eventType1,
      "command-type-1" to commandType,
      "event-type-2" to eventType2
    )

    // Create a timeline with a slice for the test
    val slice = Slice(id = "slice-1", name = "Test Slice", flowElements = emptyList())
    val timeline = Timeline(
      sliceSet = listOf(slice),
      laneSet = LaneSet(),
      nodes = emptyList(),
      messages = emptyList()
    )
    val timelines = listOf(timeline)

    val specification = specElement!!.toSpecification(typesById, timelines)

    // Check basic properties
    assertThat(specification.id).isEqualTo("spec-1")
    assertThat(specification.name).isEqualTo("Test Specification")
    assertThat(specification.scenario).isEqualTo("test-scenario")

    // Check slice reference
    assertThat(specification.slice).isNotNull
    assertThat(specification.slice!!.id).isEqualTo("slice-1")

    // Check given stage
    assertThat(specification.givenStage).isNotNull
    assertThat(specification.givenStage!!.id).isEqualTo("given-1")
    assertThat(specification.givenStage.stateName).isEqualTo("initial")
    assertThat(specification.givenStage.values).hasSize(1)
    assertThat(specification.givenStage.values[0]).isInstanceOf(Event::class.java)
    assertThat((specification.givenStage.values[0] as Event).id).isEqualTo("event-1")

    // Check when stage
    assertThat(specification.whenStage).isNotNull
    assertThat(specification.whenStage!!.id).isEqualTo("when-1")
    assertThat(specification.whenStage.values).hasSize(1)
    assertThat(specification.whenStage.values[0]).isInstanceOf(Command::class.java)
    assertThat((specification.whenStage.values[0] as Command).id).isEqualTo("command-1")

    // Check then stage
    assertThat(specification.thenStage).isNotNull
    assertThat(specification.thenStage!!.id).isEqualTo("then-1")
    assertThat(specification.thenStage.values).hasSize(1)
    assertThat(specification.thenStage.values[0]).isInstanceOf(Event::class.java)
    assertThat((specification.thenStage.values[0] as Event).id).isEqualTo("event-2")
  }
}
