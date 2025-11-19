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
    require(root.name == DEFINITIONS && root.isEmn()) { "Can't parse emn:definitions, this is probably not a EMN file" }

    val nodeTypes = mutableListOf<FlowNodeType>()
    val informationFlowTypes = mutableListOf<InformationFlowType>()

    /*
     * Parse types
     */
    val typeElements = root.emnElement(TYPES)?.emnElements()

    typeElements?.toFlowTypes()?.forEach { noteType -> nodeTypes.add(noteType) }
    typeElements?.toInformationFlowType()?.forEach { informationFlowType -> informationFlowTypes.add(informationFlowType) }

    /*
     * Patch message types
     */
    val typesById = nodeTypes.associateBy { it.id }
    val informationFlowTypesById = informationFlowTypes.associateBy { it.id }

    val flowTypes = informationFlowTypes.map { messageFlowType ->
      val sourceElement =
        requireNotNull(typesById[messageFlowType.source.id]) { "Unknown source ${messageFlowType.source.id}" }
      val targetElement =
        requireNotNull(typesById[messageFlowType.target.id]) { "Unknown target ${messageFlowType.target.id}" }
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
              .map { e -> nodesById.getValue(e.id) }
          )
        },
        laneSet = timeline.laneSet.copy(
          triggerLaneSet = timeline.laneSet.triggerLaneSet.map { triggerLane ->
            triggerLane.copy(
              flowElements = triggerLane.flowElements.filterIsInstance<FlowNodeReference>()
                .map { e -> nodesById.getValue(e.id) }
            )
          },
          interactionLane = timeline.laneSet.interactionLane?.let { interactionLane ->
              interactionLane.copy(
                flowElements = interactionLane.flowElements.filterIsInstance<FlowNodeReference>()
                  .map { e -> nodesById.getValue(e.id) }
              )
            },
          conceptLaneSet = timeline.laneSet.conceptLaneSet.map { aggregateLane ->
            aggregateLane.copy(
              flowElements = aggregateLane.flowElements.filterIsInstance<FlowNodeReference>()
                .map { e -> nodesById.getValue(e.id) }
            )
          }
        )
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
