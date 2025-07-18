package io.holixon.emn


import io.holixon.emn.model.Definitions
import io.holixon.emn.model.FlowElement
import io.holixon.emn.model.FlowElementType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File

class ParserTest {

    private val parser = EmnDocumentParser()

    @Test
    fun parses_guest_register() {

        val file = File("src/test/resources/guest-register.emn")
        val result = parser.parseDefinitions(file)
        // println(result)
        assertThat(result).isNotNull

        assert_all_types_and_timelines(result)
    }

    @Test
    fun parses_faculty() {

        val file = File("src/test/resources/faculty.emn")
        val result = parser.parseDefinitions(file)
        // println(result)
        assertThat(result).isNotNull

        assert_all_types_and_timelines(result)
        assert_specifications(result)
        assert_events_belong_to_aggregates(result, true)
    }

    private fun assert_specifications(result: Definitions, verbose: Boolean = false) {
        assertThat(result.specifications).isNotEmpty
        assertThat(result.specifications).hasSize(2)
        for (specification in result.specifications) {
            if (verbose) {
                println("Specification: $specification")
            }
        }
    }

    private fun assert_events_belong_to_aggregates(result: Definitions, verbose: Boolean = false) {
        result.getFlowElement<FlowElement.FlowNode.Event>().forEach { event ->
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
        assertThat(result.nodeTypes.filterIsInstance<FlowElementType.FlowNodeType.FlowNodeTypeReference>()).isEmpty()

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
            assertThat(lane.flowElements.filterIsInstance<FlowElement.FlowNode.FlowNodeReference>()).isEmpty()
        }
        if (verbose) {
            println("Interaction lane: ${result.timelines[0].laneSet.interactionLane}")
        }
        assertThat(result.timelines[0].laneSet.interactionLane.flowElements.filterIsInstance<FlowElement.FlowNode.FlowNodeReference>()).isEmpty()

        for (lane in result.timelines[0].laneSet.aggregateLaneSet) {
            if (verbose) {
                println("Aggregate lane: ${lane.id}, ${lane.name}")
            }
            assertThat(lane.flowElements.filterIsInstance<FlowElement.FlowNode.FlowNodeReference>()).isEmpty()
        }

        assertThat(result.timelines[0].sliceSet).isNotEmpty
        for (slice in result.timelines[0].sliceSet) {
            if (verbose) {
                println("Slice: ${slice.id}")
            }
            assertThat(slice.flowElements.filterIsInstance<FlowElement.FlowNode.FlowNodeReference>()).isEmpty()
        }

        assertThat(result.timelines[0].nodes).isNotEmpty
        for (node in result.timelines[0].nodes) {
            if (verbose) {
                println("Node: ${node.id}, incoming: ${node.incoming.map { it.id }}, outgoing: ${node.outgoing.map { it.id }}, type: ${node.typeReference.id}")
            }
        }
        assertThat(result.timelines[0].nodes.filterIsInstance<FlowElement.FlowNode.FlowNodeReference>()).isEmpty()

        assertThat(result.timelines[0].messages).isNotEmpty
        for (message in result.timelines[0].messages) {
            if (verbose) {
                println("Flow: ${message.id}, source: ${message.source.id}, target: ${message.target.id}, type: ${message.typeReference.id}")
            }
        }
    }
}
