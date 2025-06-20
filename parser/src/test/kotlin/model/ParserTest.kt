package io.holixon.emn.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File

class ParserTest {

  private val parser = EmnDocumentParser()

  @Test
  fun parses_emn() {

    val file = File("src/test/resources/guest-register.emn")
    val result = parser.parseDefinitions(file)
    println(result)
    assertThat(result).isNotNull

    assertThat(result.nodeTypes).isNotEmpty
    for (type in result.nodeTypes) {
      println("Type: ${type.id}, incoming: ${type.incoming.map { it.id }}, outgoing: ${type.outgoing.map { it.id }}")
      if (type.schema != null) {
        println(" Schema (type=${type.schema!!.schemaFormat}), \n ${type.schema!!.printable()}")
      }
    }

    assertThat(result.flowTypes).isNotEmpty
    for (flow in result.flowTypes) {
      println("FlowType: ${flow.id}, source: ${flow.source.id}, target: ${flow.target.id}")
    }

    assertThat(result.timelines).isNotEmpty

    assertThat(result.timelines[0].laneSet.triggerLaneSet).isNotNull
    assertThat(result.timelines[0].laneSet.aggregateLaneSet).isNotNull

    for (lane in result.timelines[0].laneSet.triggerLaneSet) {
      println("Trigger lane: ${lane.id}, ${lane.name}")
    }
    println("Interaction lane: ${result.timelines[0].laneSet.interactionLane}")
    for (lane in result.timelines[0].laneSet.aggregateLaneSet) {
      println("Aggregate lane: ${lane.id}, ${lane.name}")
    }

    assertThat(result.timelines[0].sliceSet).isNotEmpty
    for (slice in result.timelines[0].sliceSet) {
      println("Slice: ${slice.id}")
    }

    assertThat(result.timelines[0].nodes).isNotEmpty
    for (node in result.timelines[0].nodes) {
      println("Node: ${node.id}, incoming: ${node.incoming.map { it.id }}, outgoing: ${node.outgoing.map { it.id }}, type: ${node.typeReference.id}")
    }

    assertThat(result.timelines[0].messages).isNotEmpty
    for (message in result.timelines[0].messages) {
      println("Flow: ${message.id}, source: ${message.source.id}, target: ${message.target.id}, type: ${message.typeReference.id}")
    }
  }
}
