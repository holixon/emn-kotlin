package io.holixon.emn.dom4j

import io.holixon.emn.model.*
import org.assertj.core.api.Assertions.assertThat
import org.dom4j.io.SAXReader
import org.junit.jupiter.api.Test
import java.io.StringReader

/**
 * Tests for DOM4j timeline extension functions.
 */
internal class Dom4jExtensionsTimelineTest {

  @Test
  fun `toTimeline parses timeline elements with empty nodes and messages`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:definitions xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
        <emn:timeline id="timeline-1">
          <emn:sliceSet>
            <emn:slice id="slice-1" name="Slice 1">
              <emn:flowNodeRef>node-1</emn:flowNodeRef>
            </emn:slice>
          </emn:sliceSet>
          <emn:laneSet id="laneSet-1" name="Lane Set">
            <emn:triggerLaneSet>
              <emn:triggerLane id="triggerLane-1" name="Trigger Lane">
                <emn:flowNodeRef>trigger-node</emn:flowNodeRef>
              </emn:triggerLane>
            </emn:triggerLaneSet>
            <emn:interactionLane id="interaction-1" name="Interaction">
              <emn:flowNodeRef>interaction-node</emn:flowNodeRef>
            </emn:interactionLane>
            <emn:conceptLaneSet>
              <emn:conceptLane id="conceptLane-1" name="Concept Lane">
                <emn:flowNodeRef>concept-node</emn:flowNodeRef>
              </emn:conceptLane>
            </emn:conceptLaneSet>
          </emn:laneSet>
        </emn:timeline>
      </emn:definitions>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))
    val timeline = doc.rootElement.emnElement("timeline")

    // Create empty maps for the test
    val typesById = mapOf<String, FlowNodeType>()
    val informationFlowTypesById = mapOf<String, InformationFlowType>()

    val result = timeline!!.toTimeline(typesById, informationFlowTypesById)

    // Check slice set
    assertThat(result.sliceSet).hasSize(1)
    assertThat(result.sliceSet[0].id).isEqualTo("slice-1")
    assertThat(result.sliceSet[0].name).isEqualTo("Slice 1")
    assertThat(result.sliceSet[0].flowElements).hasSize(1)
    assertThat(result.sliceSet[0].flowElements[0].id).isEqualTo("node-1")

    // Check lane set
    assertThat(result.laneSet.interactionLane!!.id).isEqualTo("interaction-1")
    assertThat(result.laneSet.interactionLane.name).isEqualTo("Interaction")
    assertThat(result.laneSet.interactionLane.flowElements).hasSize(1)
    assertThat(result.laneSet.interactionLane.flowElements[0].id).isEqualTo("interaction-node")

    // Check trigger lane set
    assertThat(result.laneSet.triggerLaneSet).hasSize(1)
    assertThat(result.laneSet.triggerLaneSet[0].id).isEqualTo("triggerLane-1")
    assertThat(result.laneSet.triggerLaneSet[0].name).isEqualTo("Trigger Lane")
    assertThat(result.laneSet.triggerLaneSet[0].flowElements).hasSize(1)
    assertThat(result.laneSet.triggerLaneSet[0].flowElements[0].id).isEqualTo("trigger-node")

    // Check concept lane set
    assertThat(result.laneSet.conceptLaneSet).hasSize(1)
    assertThat(result.laneSet.conceptLaneSet[0].id).isEqualTo("conceptLane-1")
    assertThat(result.laneSet.conceptLaneSet[0].name).isEqualTo("Concept Lane")
    assertThat(result.laneSet.conceptLaneSet[0].flowElements).hasSize(1)
    assertThat(result.laneSet.conceptLaneSet[0].flowElements[0].id).isEqualTo("concept-node")

    // Check nodes and messages
    assertThat(result.nodes).isEmpty()
    assertThat(result.messages).isEmpty()
  }

  @Test
  fun `toTimeline parses timeline elements with flow nodes and messages`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:definitions xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
        <emn:timeline id="timeline-1">
          <emn:sliceSet>
            <emn:slice id="slice-1" name="Slice 1">
              <emn:flowNodeRef>event-1</emn:flowNodeRef>
            </emn:slice>
          </emn:sliceSet>
          <emn:laneSet id="laneSet-1" name="Lane Set">
            <emn:triggerLaneSet>
              <emn:triggerLane id="triggerLane-1" name="Trigger Lane" />
            </emn:triggerLaneSet>
            <emn:interactionLane id="interaction-1" name="Interaction" />
            <emn:conceptLaneSet>
              <emn:conceptLane id="conceptLane-1" name="Concept Lane">
                <emn:flowNodeRef>event-1</emn:flowNodeRef>
              </emn:conceptLane>
            </emn:conceptLaneSet>
          </emn:laneSet>
          <emn:event id="event-1" typeRef="event-type-1" />
          <emn:command id="command-1" typeRef="command-type-1" />
          <emn:informationFlow id="flow-1" typeRef="flow-type-1" sourceRef="event-1" targetRef="command-1" />
        </emn:timeline>
      </emn:definitions>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))
    val timeline = doc.rootElement.emnElement("timeline")

    // Create type maps for the test
    val eventType = EventType(id = "event-type-1", name = "Event Type 1", schema = null)
    val commandType = CommandType(id = "command-type-1", name = "Command Type 1", schema = null)
    val typesById = mapOf(
      "event-type-1" to eventType,
      "command-type-1" to commandType
    )

    val flowType = InformationFlowType(
      id = "flow-type-1",
      name = "Flow Type 1",
      source = FlowNodeTypeReference("event-type-1"),
      target = FlowNodeTypeReference("command-type-1")
    )
    val informationFlowTypesById = mapOf("flow-type-1" to flowType)

    val result = timeline!!.toTimeline(typesById, informationFlowTypesById)

    // Check nodes
    assertThat(result.nodes).hasSize(2)
    assertThat(result.nodes[0]).isInstanceOf(Event::class.java)
    assertThat(result.nodes[0].id).isEqualTo("event-1")
    assertThat((result.nodes[0] as Event).typeReference).isEqualTo(eventType)

    assertThat(result.nodes[1]).isInstanceOf(Command::class.java)
    assertThat(result.nodes[1].id).isEqualTo("command-1")
    assertThat((result.nodes[1] as Command).typeReference).isEqualTo(commandType)

    // Check messages
    assertThat(result.messages).hasSize(1)
    assertThat(result.messages[0].id).isEqualTo("flow-1")
    assertThat(result.messages[0].typeReference.id).isEqualTo("flow-type-1")
    assertThat(result.messages[0].source.id).isEqualTo("event-1")
    assertThat(result.messages[0].target.id).isEqualTo("command-1")
  }

  @Test
  fun `extractFlowElements extracts flow nodes and information flows`() {
    val xml = """
      <?xml version="1.0" encoding="UTF-8"?>
      <emn:definitions xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL">
        <emn:timeline>
          <emn:event id="event-1" typeRef="event-type-1" />
          <emn:command id="command-1" typeRef="command-type-1" />
          <emn:view id="view-1" typeRef="view-type-1" />
          <emn:query id="query-1" typeRef="query-type-1" />
          <emn:error id="error-1" typeRef="error-type-1" />
          <emn:externalEvent id="external-event-1" typeRef="external-event-type-1" />
          <emn:externalSystem id="external-system-1" typeRef="external-system-type-1" />
          <emn:translation id="translation-1" typeRef="translation-type-1" />
          <emn:automation id="automation-1" typeRef="automation-type-1" />
          <emn:informationFlow id="flow-1" typeRef="flow-type-1" sourceRef="event-1" targetRef="command-1" />
          <emn:informationFlow id="flow-2" typeRef="flow-type-2" sourceRef="command-1" targetRef="view-1" />
          <emn:sliceSet />
          <emn:laneSet />
        </emn:timeline>
      </emn:definitions>
    """.trimIndent()

    val doc = SAXReader().read(StringReader(xml))
    val timeline = doc.rootElement.emnElement("timeline")

    // Create type maps for the test
    val eventType = EventType(id = "event-type-1", name = "Event Type 1", schema = null)
    val commandType = CommandType(id = "command-type-1", name = "Command Type 1", schema = null)
    val viewType = ViewType(id = "view-type-1", name = "View Type 1", schema = null)
    val queryType = QueryType(id = "query-type-1", name = "Query Type 1", schema = null)
    val errorType = ErrorType(id = "error-type-1", name = "Error Type 1", schema = null)
    val externalEventType = ExternalEventType(id = "external-event-type-1", name = "External Event Type 1", schema = null)
    val externalSystemType = ExternalSystemType(id = "external-system-type-1", name = "External System Type 1", schema = null)
    val translationType = TranslationType(id = "translation-type-1", name = "Translation Type 1", schema = null)
    val automationType = AutomationType(id = "automation-type-1", name = "Automation Type 1", schema = null)

    val typesById = mapOf(
      "event-type-1" to eventType,
      "command-type-1" to commandType,
      "view-type-1" to viewType,
      "query-type-1" to queryType,
      "error-type-1" to errorType,
      "external-event-type-1" to externalEventType,
      "external-system-type-1" to externalSystemType,
      "translation-type-1" to translationType,
      "automation-type-1" to automationType
    )

    val flowType1 = InformationFlowType(
      id = "flow-type-1",
      name = "Flow Type 1",
      source = FlowNodeTypeReference("event-type-1"),
      target = FlowNodeTypeReference("command-type-1")
    )

    val flowType2 = InformationFlowType(
      id = "flow-type-2",
      name = "Flow Type 2",
      source = FlowNodeTypeReference("command-type-1"),
      target = FlowNodeTypeReference("view-type-1")
    )

    val informationFlowTypesById = mapOf(
      "flow-type-1" to flowType1,
      "flow-type-2" to flowType2
    )

    val (nodes, flows) = timeline!!.extractFlowElements(typesById, informationFlowTypesById)

    // Check nodes
    assertThat(nodes).hasSize(9)

    // Check specific node types
    assertThat(nodes.filterIsInstance<Event>()).hasSize(1)
    assertThat(nodes.filterIsInstance<Command>()).hasSize(1)
    assertThat(nodes.filterIsInstance<View>()).hasSize(1)
    assertThat(nodes.filterIsInstance<Query>()).hasSize(1)
    assertThat(nodes.filterIsInstance<Error>()).hasSize(1)
    assertThat(nodes.filterIsInstance<ExternalEvent>()).hasSize(1)
    assertThat(nodes.filterIsInstance<ExternalSystem>()).hasSize(1)
    assertThat(nodes.filterIsInstance<Translation>()).hasSize(1)
    assertThat(nodes.filterIsInstance<Automation>()).hasSize(1)

    // Check information flows
    assertThat(flows).hasSize(2)
    assertThat(flows[0].id).isEqualTo("flow-1")
    assertThat(flows[0].typeReference.id).isEqualTo("flow-type-1")
    assertThat(flows[0].source.id).isEqualTo("event-1")
    assertThat(flows[0].target.id).isEqualTo("command-1")

    assertThat(flows[1].id).isEqualTo("flow-2")
    assertThat(flows[1].typeReference.id).isEqualTo("flow-type-2")
    assertThat(flows[1].source.id).isEqualTo("command-1")
    assertThat(flows[1].target.id).isEqualTo("view-1")
  }
}
