package io.holixon.emn


import io.holixon.emn.model.Definitions
import io.holixon.emn.model.EmbeddedSchema
import io.holixon.emn.model.EmbeddedValue
import io.holixon.emn.model.Event
import io.holixon.emn.model.EventSequence
import io.holixon.emn.model.EventSequenceType
import io.holixon.emn.model.FlowNodeReference
import io.holixon.emn.model.FlowNodeTypeReference
import io.holixon.emn.model.ReferencedSchema
import io.holixon.emn.model.CommandType
import io.holixon.emn.model.EventType
import io.holixon.emn.model.QueryType
import org.dom4j.DocumentHelper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ParserTest {

  @Test
  fun `parse type definitions without requiring complete diagram flows`() {
    val definitions = parser.parseTypeDefinitions(TestFixtures.resourceUrl("quizmania/quizmania-blueprint.emn"))

    assertThat(definitions.schemaDefinitions).hasSize(24)
    assertThat(definitions.nodeTypes.filterIsInstance<CommandType>()).hasSize(19)
    assertThat(definitions.nodeTypes.filterIsInstance<EventType>()).hasSize(20)
    assertThat(definitions.nodeTypes.filterIsInstance<EventSequenceType>()).hasSize(1)
    assertThat(definitions.nodeTypes.filterIsInstance<QueryType>()).hasSize(11)
    assertThat(definitions.flowTypes).isEmpty()
    assertThat(definitions.timelines).isEmpty()
  }

  @Test
  fun `parses event sequence types definitions occurrences and flows`() {
    val document = DocumentHelper.parseText(
      """
      <emn:definitions xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL"
        id="event-sequence" name="Event Sequence" targetNamespace="https://holixon.io/schema/emn">
        <emn:types>
          <emn:eventType id="GameCreated" name="Game created"/>
          <emn:eventType id="PlayerJoined" name="Player joined"/>
          <emn:eventSequenceType id="GameStartedWithPlayer" name="Game started with player">
            <emn:eventTypeRef>GameCreated</emn:eventTypeRef>
            <emn:eventTypeRef>PlayerJoined</emn:eventTypeRef>
          </emn:eventSequenceType>
          <emn:queryType id="GameState" name="Game state"/>
          <emn:informationFlowType id="sequence-to-state" name="Sequence to state"
            sourceRef="GameStartedWithPlayer" targetRef="GameState"/>
        </emn:types>
        <emn:timeline id="timeline" name="Timeline">
          <emn:informationFlow id="flow" typeRef="sequence-to-state"
            sourceRef="sequence" targetRef="query"/>
          <emn:eventSequence id="sequence" definitionRef="started-game">
              <emn:variable name="GAME_ID">game-1</emn:variable>
          </emn:eventSequence>
          <emn:query id="query" typeRef="GameState"/>
        </emn:timeline>
        <emn:specification id="specification" name="Sequence in given">
          <emn:given id="given">
            <emn:eventSequence id="given-sequence" definitionRef="started-game"/>
            <emn:eventSequence id="overridden-given-sequence" definitionRef="started-game">
              <emn:variable name="GAME_ID">override-game</emn:variable>
            </emn:eventSequence>
          </emn:given>
          <emn:when id="when"/>
          <emn:then id="then"/>
        </emn:specification>
        <emn:eventSequenceDefinition id="started-game" typeRef="GameStartedWithPlayer">
          <emn:variableDefinition name="GAME_ID">default-game</emn:variableDefinition>
          <emn:event id="created" typeRef="GameCreated">
            <emn:value>{"gameId":"${'$'}{GAME_ID}"}</emn:value>
          </emn:event>
          <emn:event id="joined" typeRef="PlayerJoined">
            <emn:value>{"gameId":"${'$'}{GAME_ID}"}</emn:value>
          </emn:event>
        </emn:eventSequenceDefinition>
      </emn:definitions>
      """.trimIndent()
    )

    val result = parser.parseDefinitions(document)
    val sequenceType = result.nodeTypes.filterIsInstance<EventSequenceType>().single()
    assertThat(sequenceType.eventTypes.map { it.id }).containsExactly("GameCreated", "PlayerJoined")
    val definition = result.eventSequenceDefinitions.single()
    assertThat(definition.typeReference).isSameAs(sequenceType)
    assertThat(definition.events.map { it.id }).containsExactly("created", "joined")
    assertThat(definition.variableDefinitions.single().value).isEqualTo("default-game")
    val sequence = result.timelines.single().nodes.filterIsInstance<EventSequence>().single()
    assertThat(sequence.definitionReference).isSameAs(definition)
    assertThat(sequence.variables.single().value).isEqualTo("game-1")
    assertThat(result.timelines.single().messages.single().source).isSameAs(sequence)
    val givenStage = result.specifications.single().givenStage!!
    assertThat(givenStage.values).allMatch { it is EventSequence }
    assertThat(givenStage.events.map { it.id }).containsExactly("created", "joined", "created", "joined")
    assertThat(givenStage.events.map { (it.value as EmbeddedValue).content }).containsExactly(
      "{\"gameId\":\"default-game\"}",
      "{\"gameId\":\"default-game\"}",
      "{\"gameId\":\"override-game\"}",
      "{\"gameId\":\"override-game\"}",
    )
  }

  private val parser = EmnDocumentParser()

  @Test
  fun parses_guest_register() {

    val file = TestFixtures.resourceUrl("guest-register/guest-register.emn")
    val result = parser.parseDefinitions(file)
    // println(result)
    assertThat(result).isNotNull

    assert_all_types_and_timelines(result)
  }

  @Test
  fun parses_faculty() {

    val file = TestFixtures.resourceUrl("faculty/faculty.emn")
    val result = parser.parseDefinitions(file)
    // println(result)
    assertThat(result).isNotNull

    assert_all_types_and_timelines(result)
    assert_specifications(result)
    assert_events_belong_to_aggregates(result, true)
  }

  @Test
  fun parses_schema_definitions() {
    val file = TestFixtures.resourceUrl("json-schema/faculty.emn")
    val result = parser.parseDefinitions(file)

    assertThat(result.schemaDefinitions).hasSize(1)
    assertThat(result.schemaDefinitions.first().id).isEqualTo("CourseType")
    assertThat(result.schemaDefinitions.first().schema).isInstanceOf(EmbeddedSchema::class.java)
  }

  @Test
  fun parses_referenced_schemas() {
    val document = DocumentHelper.parseText(
      """
      <emn:definitions xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL"
        id="schema-reference" name="Schema Reference" targetNamespace="https://holixon.io/schema/emn">
        <emn:types>
          <emn:schemaDefinition id="CourseType" name="Course Type">
            <emn:schema schemaFormat="application/schema+json">{"type":"string","enum":["LIVE","REMOTE"]}</emn:schema>
          </emn:schemaDefinition>
          <emn:commandType id="CreateCourse" name="Create Course">
            <emn:schema schemaFormat="application/schema+json" reference="CourseType"/>
          </emn:commandType>
        </emn:types>
      </emn:definitions>
      """.trimIndent()
    )

    val result = parser.parseDefinitions(document)
    val commandType = result.nodeTypes.filterIsInstance<CommandType>().single()

    assertThat(commandType.schema).isInstanceOf(ReferencedSchema::class.java)
    assertThat((commandType.schema as ReferencedSchema).reference).isEqualTo("CourseType")
  }

  private fun assert_specifications(result: Definitions, verbose: Boolean = false) {
    assertThat(result.specifications).isNotEmpty
    assertThat(result.specifications).hasSize(5)
    for (specification in result.specifications) {
      if (verbose) {
        println("Specification: $specification")
      }
    }
  }

  private fun assert_events_belong_to_aggregates(result: Definitions, verbose: Boolean = false) {
    result.getFlowElement<Event>().forEach { event ->
      assertThat(result.aggregates(event)).isNotEmpty
      if (verbose) {
        println("Event: ${event.typeReference.name}, aggregates: ${result.aggregates(event).map { it.name to it.idSchema }}")
      }
    }
  }

  private fun assert_all_types_and_timelines(result: Definitions, verbose: Boolean = false) {
    assertThat(result.nodeTypes).isNotEmpty
    if (verbose) {
      for (type in result.nodeTypes) {
        println("Type: ${type.id}, incoming: ${type.incoming.map { it.id }}, outgoing: ${type.outgoing.map { it.id }}")
        if (type.schema != null) {
          println(" Schema (type=${type.schema!!.schemaFormat}), \n ${type.schema}")
        }
      }
    }
    assertThat(result.nodeTypes.filterIsInstance<FlowNodeTypeReference>()).isEmpty()

    assertThat(result.flowTypes).isNotEmpty
    if (verbose) {
      for (flow in result.flowTypes) {
        println("FlowType: ${flow.id}, source: ${flow.source.id}, target: ${flow.target.id}")
      }
    }

    assertThat(result.timelines).isNotEmpty

    assertThat(result.timelines[0].laneSet.triggerLaneSet).isNotNull
    assertThat(result.timelines[0].laneSet.aggregateLaneSet).isNotNull

    for (lane in result.timelines[0].laneSet.triggerLaneSet) {
      if (verbose) {
        println("Trigger lane: ${lane.id}, ${lane.name}")
      }
      assertThat(lane.flowElements.filterIsInstance<FlowNodeReference>()).isEmpty()
    }
    if (verbose) {
      println("Interaction lane: ${result.timelines[0].laneSet.interactionLane}")
    }
    assertThat(result.timelines[0].laneSet.interactionLane.flowElements.filterIsInstance<FlowNodeReference>()).isEmpty()

    for (lane in result.timelines[0].laneSet.aggregateLaneSet) {
      if (verbose) {
        println("Aggregate lane: ${lane.id}, ${lane.name}")
      }
      assertThat(lane.flowElements.filterIsInstance<FlowNodeReference>()).isEmpty()
    }

    assertThat(result.timelines[0].sliceSet).isNotEmpty
    for (slice in result.timelines[0].sliceSet) {
      if (verbose) {
        println("Slice: ${slice.id}")
      }
      assertThat(slice.flowElements.filterIsInstance<FlowNodeReference>()).isEmpty()
    }

    assertThat(result.timelines[0].nodes).isNotEmpty
    for (node in result.timelines[0].nodes) {
      if (verbose) {
        println("Node: ${node.id}, incoming: ${node.incoming.map { it.id }}, outgoing: ${node.outgoing.map { it.id }}, type: ${node.typeReference.id}")
      }
    }
    assertThat(result.timelines[0].nodes.filterIsInstance<FlowNodeReference>()).isEmpty()

    assertThat(result.timelines[0].messages).isNotEmpty
    for (message in result.timelines[0].messages) {
      if (verbose) {
        println("Flow: ${message.id}, source: ${message.source.id}, target: ${message.target.id}, type: ${message.typeReference.id}")
      }
    }
  }
}
