package io.holixon.emn

import io.holixon.emn.dom4j.*
import io.holixon.emn.dom4j.ElementNames.DEFINITIONS
import io.holixon.emn.dom4j.ElementNames.SPECIFICATION
import io.holixon.emn.dom4j.ElementNames.TIMELINE
import io.holixon.emn.dom4j.ElementNames.TYPES
import io.holixon.emn.model.*
import org.dom4j.Document
import org.dom4j.io.SAXReader
import java.io.File
import java.net.URL

class EmnDocumentParser {

  fun parseDefinitions(file: File): Definitions {
    val reader = SAXReader()
    val document = reader.read(file)
    return parseDefinitions(document)
  }

  fun parseDefinitions(url: URL): Definitions {
    val reader = SAXReader()
    val document = reader.read(url)
    return parseDefinitions(document)
  }

  fun parseDefinitions(document: Document): Definitions {

    val root = document.rootElement // <definitions>
    requireNotNull(root)
    require(root.name == DEFINITIONS && root.isEmn()) { "Expected root element to be 'emn:definitions', but it was '${root.qualifiedName}'." }

    /*
     * Parse types
     */
    val typeElements = root.emnElement(TYPES)?.emnElements()
    val nodeTypes = typeElements?.toFlowTypes() ?: emptyList()
    val informationFlowTypes = typeElements?.toInformationFlowType() ?: emptyList()

    /*
     * Patch message types
     */
    val typesById = nodeTypes.associateBy { it.id }
    val informationFlowTypesById = informationFlowTypes.associateBy { it.id }

    val flowTypes = informationFlowTypes.map { messageFlowType ->
      val sourceElement =
        requireNotNull(typesById[messageFlowType.source.id]) { "Expected source type with id '${messageFlowType.source.id}' to exist, but it was not found." }
      val targetElement =
        requireNotNull(typesById[messageFlowType.target.id]) { "Expected target type with id '${messageFlowType.target.id}' to exist, but it was not found." }
      val patchedMessageFlowType = messageFlowType.copy(
        source = sourceElement,
        target = targetElement,
      )
      sourceElement.outgoing.add(patchedMessageFlowType)
      targetElement.incoming.add(patchedMessageFlowType)
      patchedMessageFlowType
    }

    /*
     Parse timelines
     */
    val timelines = root.emnElements(TIMELINE).map { element ->
      element.toTimeline(typesById, informationFlowTypesById)
    }

    /*
     * Patch lanes and slices
     */
    val patchedTimelines = timelines.map { timeline ->
      val nodesById: Map<String, FlowNode> = timeline.nodes.associateBy { it.id }
      timeline.copy(
        sliceSet = timeline.sliceSet.map { slice ->
          slice.copy(
            flowElements = slice.flowElements.filterIsInstance<FlowNodeReference>()
              .map { e -> nodesById[e.id] ?: throw IllegalArgumentException("Expected node with id '${e.id}' referenced in slice '${slice.id}' to exist, but it was not found.") }
          )
        },
        laneSet = timeline.laneSet?.let { laneSet ->
          laneSet.copy(
            triggerLaneSet = laneSet.triggerLaneSet.map { triggerLane ->
              triggerLane.copy(
                flowElements = triggerLane.flowElements.filterIsInstance<FlowNodeReference>()
                  .map { e -> nodesById[e.id] ?: throw IllegalArgumentException("Expected node with id '${e.id}' referenced in trigger lane '${triggerLane.id}' to exist, but it was not found.") }
              )
            },
            interactionLane = laneSet.interactionLane?.let { interactionLane ->
                interactionLane.copy(
                  flowElements = interactionLane.flowElements.filterIsInstance<FlowNodeReference>()
                    .map { e -> nodesById[e.id] ?: throw IllegalArgumentException("Expected node with id '${e.id}' referenced in interaction lane '${interactionLane.id}' to exist, but it was not found.") }
                )
              },
            conceptLaneSet = laneSet.conceptLaneSet.map { aggregateLane ->
              aggregateLane.copy(
                flowElements = aggregateLane.flowElements.filterIsInstance<FlowNodeReference>()
                  .map { e -> nodesById[e.id] ?: throw IllegalArgumentException("Expected node with id '${e.id}' referenced in concept lane '${aggregateLane.id}' to exist, but it was not found.") }
              )
            }
          )
        }
      )
    }

    /*
     * Parse specifications
     */
    val specifications = root
      .emnElements(SPECIFICATION)
      .map { element -> element.toSpecification(typesById, timelines) }

    return Definitions(
      nodeTypes = nodeTypes,
      flowTypes = flowTypes,
      timelines = patchedTimelines,
      specifications = specifications,
    )
  }

}
