package io.holixon.emn

import io.holixon.emn.dom4j.*
import io.holixon.emn.dom4j.ElementNames.AUTOMATION_TYPE
import io.holixon.emn.dom4j.ElementNames.COMMAND_TYPE
import io.holixon.emn.dom4j.ElementNames.DEFINITIONS
import io.holixon.emn.dom4j.ElementNames.ERROR_TYPE
import io.holixon.emn.dom4j.ElementNames.EVENT_TYPE
import io.holixon.emn.dom4j.ElementNames.EXTERNAL_EVENT_TYPE
import io.holixon.emn.dom4j.ElementNames.EXTERNAL_SYSTEM_TYPE
import io.holixon.emn.dom4j.ElementNames.INFORMATION_FLOW_TYPE
import io.holixon.emn.dom4j.ElementNames.QUERY_TYPE
import io.holixon.emn.dom4j.ElementNames.SPECIFICATION
import io.holixon.emn.dom4j.ElementNames.TIMELINE
import io.holixon.emn.dom4j.ElementNames.TRANSLATION_TYPE
import io.holixon.emn.dom4j.ElementNames.TYPES
import io.holixon.emn.dom4j.ElementNames.VIEW_TYPE
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
    root.emnElement(TYPES)
      ?.elements()
      ?.forEach { element ->
        when (element.name) {
          VIEW_TYPE -> nodeTypes.add(
            ViewType(
              id = element.id(),
              name = element.name(),
              schema = element.schema()
            )
          )

          COMMAND_TYPE -> nodeTypes.add(
            CommandType(
              id = element.id(),
              name = element.name(),
              schema = element.schema()
            )
          )

          EVENT_TYPE -> nodeTypes.add(
            EventType(
              id = element.id(),
              name = element.name(),
              schema = element.schema()
            )
          )

          QUERY_TYPE -> nodeTypes.add(
            QueryType(
              id = element.id(),
              name = element.name(),
              schema = element.schema()
            )
          )

          ERROR_TYPE -> nodeTypes.add(
            ErrorType(
              id = element.id(),
              name = element.name(),
              schema = element.schema()
            )
          )

          EXTERNAL_EVENT_TYPE -> nodeTypes.add(
            ExternalEventType(
              id = element.id(),
              name = element.name(),
              schema = element.schema()
            )
          )

          EXTERNAL_SYSTEM_TYPE -> nodeTypes.add(
            ExternalSystemType(
              id = element.id(),
              name = element.name(),
              schema = element.schema()
            )
          )

          TRANSLATION_TYPE -> nodeTypes.add(
            TranslationType(
              id = element.id(),
              name = element.name(),
              schema = element.schema()
            )
          )

          AUTOMATION_TYPE -> nodeTypes.add(
            AutomationType(
              id = element.id(),
              name = element.name(),
              schema = element.schema()
            )
          )

          INFORMATION_FLOW_TYPE -> informationFlowTypes.add(
            InformationFlowType(
              id = element.id(),
              name = element.name(),
              source = FlowNodeTypeReference(element.sourceRef()),
              target = FlowNodeTypeReference(element.targetRef())
            )
          )

          else -> println("Unknown EMN element '${element.name}'")
        }
      }

    /**
     * Patch message types
     */
    val typesById = nodeTypes.associateBy { it.id }
    val messageFlowTypesById = informationFlowTypes.associateBy { it.id }

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
    val timelines = mutableListOf<Timeline>()
    root.emnElements(TIMELINE).forEach { element ->
      timelines.add(
        Timeline(
          sliceSet = element.sliceSet(),
          laneSet = element.laneSet(),
          nodes = listOf(),
          messages = listOf(),
        ).let { timeline ->
          val (nodes, messages) = element.extractFlowElements(typesById, messageFlowTypesById)
          timeline.copy(nodes = nodes, messages = messages)
        }
      )
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
          interactionLane = timeline.laneSet
            .interactionLane.copy(
              flowElements = timeline.laneSet.interactionLane.flowElements.filterIsInstance<FlowNodeReference>()
                .map { e -> nodesById.getValue(e.id) }
            ),
          conceptLaneSet = timeline.laneSet.conceptLaneSet.map { aggregateLane ->
            aggregateLane.copy(
              flowElements = aggregateLane.flowElements.filterIsInstance<FlowNodeReference>()
                .map { e -> nodesById.getValue(e.id) }
            )
          }
        )
      )
    }

    val specifications = mutableListOf<Specification>()
    root.emnElements(SPECIFICATION)
      .forEach { element ->
      specifications.add(
        Specification(
          id = element.id(),
          name = element.name(),
          scenario = element.scenario(),
          slice = element.sliceRef()?.let { sliceRef -> timelines.map { it.sliceSet }
            .flatten()
            .first { it.id == sliceRef }
                                          },
          givenStage = element.givenStage(typesById),
          whenStage = element.whenStage(typesById),
          thenStage = element.thenStage(typesById),
        )
      )
    }

    return Definitions(
      nodeTypes = nodeTypes,
      flowTypes = flowTypes,
      timelines = patchedTimelines,
      specifications = specifications,
    )
  }

}
