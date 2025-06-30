package io.holixon.emn

import io.holixon.emn.model.*
import org.dom4j.Document
import org.dom4j.Element
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
    require(root.name == "definitions") { "Can't parse definitions, this is probably not a EMN file" }

    val nodeTypes = mutableListOf<FlowElementType.FlowNodeType>()
    val messageFlowTypes = mutableListOf<FlowElementType.MessageFlowType>()

    /*
     * Parse types
     */
    root.element("types")?.elements()?.forEach { element ->
      when (element.name) {
        "viewType" -> nodeTypes.add(
          FlowElementType.FlowNodeType.ViewType(
            id = element.id(),
            name = element.name(),
            schema = element.schema()
          )
        )

        "commandType" -> nodeTypes.add(
          FlowElementType.FlowNodeType.CommandType(
            id = element.id(),
            name = element.name(),
            schema = element.schema()
          )
        )

        "eventType" -> nodeTypes.add(
          FlowElementType.FlowNodeType.EventType(
            id = element.id(),
            name = element.name(),
            schema = element.schema()
          )
        )

        "queryType" -> nodeTypes.add(
          FlowElementType.FlowNodeType.QueryType(
            id = element.id(),
            name = element.name(),
            schema = element.schema()
          )
        )

        "errorType" -> nodeTypes.add(
          FlowElementType.FlowNodeType.ErrorType(
            id = element.id(),
            name = element.name(),
            schema = element.schema()
          )
        )

        "externalEventType" -> nodeTypes.add(
          FlowElementType.FlowNodeType.ExternalEventType(
            id = element.id(),
            name = element.name(),
            schema = element.schema()
          )
        )

        "externalSystemType" -> nodeTypes.add(
          FlowElementType.FlowNodeType.ExternalSystemType(
            id = element.id(),
            name = element.name(),
            schema = element.schema()
          )
        )

        "translationType" -> nodeTypes.add(
          FlowElementType.FlowNodeType.TranslationType(
            id = element.id(),
            name = element.name(),
            schema = element.schema()
          )
        )

        "automationType" -> nodeTypes.add(
          FlowElementType.FlowNodeType.AutomationType(
            id = element.id(),
            name = element.name(),
            schema = element.schema()
          )
        )

        "messageFlowType" -> messageFlowTypes.add(
          FlowElementType.MessageFlowType(
            id = element.id(),
            name = element.name(),
            source = FlowElementType.FlowNodeType.FlowNodeTypeReference(element.sourceRef()),
            target = FlowElementType.FlowNodeType.FlowNodeTypeReference(element.targetRef())
          )
        )

        else -> println("Unknown element '${element.name}'")
      }
    }

    /**
     * Patch message types
     */
    val typesById = nodeTypes.associateBy { it.id }
    val messageFlowTypesById = messageFlowTypes.associateBy { it.id }

    val flowTypes = messageFlowTypes.map { messageFlowType ->
      val sourceElement = requireNotNull(typesById[messageFlowType.source.id]) { "Unknown source ${messageFlowType.source.id}" }
      val targetElement = requireNotNull(typesById[messageFlowType.target.id]) { "Unknown target ${messageFlowType.target.id}" }
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
    root.elements("timeline")?.forEach { element ->
      timelines.add(
        Timeline(
          sliceSet = element.sliceSet(),
          laneSet = element.element("laneSet")?.let { laneSet ->
            LaneSet(
              triggerLaneSet = laneSet.triggerLanes(),
              interactionLane = Lane.InteractionLane(
                id = laneSet.id(),
                name = laneSet.name(),
                flowElements = laneSet.flowNodeReferences(),
              ),
              aggregateLaneSet = laneSet.aggregateLanes(),
            )
          } ?: LaneSet("UNSET"),
          nodes = listOf(),
          messages = listOf(),
        ).let { timeline ->
          val (nodes, messages) = extractFlowElements(element, typesById, messageFlowTypesById)
          timeline.copy(nodes = nodes, messages = messages)
        }
      )
    }


    /*
     * Patch lanes and slices
     */
    val patchedTimelines = timelines.map { timeline ->
      val nodesById: Map<String, FlowElement.FlowNode> = timeline.nodes.associateBy { it.id }
      timeline.copy(
        sliceSet = timeline.sliceSet.map { slice ->
          slice.copy(flowElements = slice.flowElements.filterIsInstance<FlowElement.FlowNode.FlowNodeReference>().map { e -> nodesById.getValue(e.id) })
        },
        laneSet = timeline.laneSet.copy(
          triggerLaneSet = timeline.laneSet.triggerLaneSet.map { triggerLane ->
            triggerLane.copy(
              flowElements = triggerLane.flowElements.filterIsInstance<FlowElement.FlowNode.FlowNodeReference>().map { e -> nodesById.getValue(e.id) })
          },
          interactionLane = timeline.laneSet
            .interactionLane.copy(
              flowElements = timeline.laneSet.interactionLane.flowElements.filterIsInstance<FlowElement.FlowNode.FlowNodeReference>()
                .map { e -> nodesById.getValue(e.id) }),
          aggregateLaneSet = timeline.laneSet.aggregateLaneSet.map { aggregateLane ->
            aggregateLane.copy(
              flowElements = aggregateLane.flowElements.filterIsInstance<FlowElement.FlowNode.FlowNodeReference>().map { e -> nodesById.getValue(e.id) })
          }
        )
      )
    }

    return Definitions(
      nodeTypes = nodeTypes,
      flowTypes = flowTypes,
      timelines = patchedTimelines
    )
  }

  fun extractFlowElements(
    timeline: Element,
    typesById: Map<String, FlowElementType.FlowNodeType>,
    messageFlowTypesById: Map<String, FlowElementType.MessageFlowType>
  ):
    Pair<List<FlowElement.FlowNode>, List<FlowElement.MessageFlow>> {
    val nodes = mutableListOf<FlowElement.FlowNode>()
    val messageFlows = mutableListOf<FlowElement.MessageFlow>()

    timeline.elements()
      .filterNot { it.name == "sliceSet" || it.name == "laneSet" }.map { element ->
        when (element.name) {
          "view" -> nodes.add(FlowElement.FlowNode.View(id = element.id(), typeReference = element.typeReference(typesById)))
          "command" -> nodes.add(FlowElement.FlowNode.Command(id = element.id(), typeReference = element.typeReference(typesById)))
          "event" -> nodes.add(FlowElement.FlowNode.Event(id = element.id(), typeReference = element.typeReference(typesById)))
          "query" -> nodes.add(FlowElement.FlowNode.Query(id = element.id(), typeReference = element.typeReference(typesById)))
          "error" -> nodes.add(FlowElement.FlowNode.Error(id = element.id(), typeReference = element.typeReference(typesById)))
          "externalEvent" -> nodes.add(FlowElement.FlowNode.ExternalEvent(id = element.id(), typeReference = element.typeReference(typesById)))
          "externalSystem" -> nodes.add(FlowElement.FlowNode.ExternalSystem(id = element.id(), typeReference = element.typeReference(typesById)))
          "translation" -> nodes.add(FlowElement.FlowNode.Translation(id = element.id(), typeReference = element.typeReference(typesById)))
          "automation" -> nodes.add(FlowElement.FlowNode.Automation(id = element.id(), typeReference = element.typeReference(typesById)))
          "messageFlow" -> messageFlows.add(
            FlowElement.MessageFlow(
              id = element.id(),
              typeReference = element.untypedMessageFlowType(),
              source = FlowElement.FlowNode.FlowNodeReference(id = element.sourceRef()),
              target = FlowElement.FlowNode.FlowNodeReference(id = element.targetRef())
            )
          )

          else -> println("Unknown element '${element.name}'")
        }
      }
    /*
     * Patch flows
     */
    val elementsById = nodes.associateBy { it.id }

    val flows = messageFlows.map { messageFlow ->
      val sourceElement = requireNotNull(elementsById[messageFlow.source.id]) { "Unknown source ${messageFlow.source.id}" }
      val targetElement = requireNotNull(elementsById[messageFlow.target.id]) { "Unknown target ${messageFlow.target.id}" }
      val messageFlowType = requireNotNull(messageFlowTypesById[messageFlow.typeReference.id]) { "Unknown type ${messageFlow.typeReference.id}" }
      val patchedMessageFlowType = messageFlow.copy(
        source = sourceElement,
        target = targetElement,
        typeReference = messageFlowType
      )
      sourceElement.outgoing.add(patchedMessageFlowType)
      targetElement.incoming.add(patchedMessageFlowType)
      patchedMessageFlowType
    }

    return nodes to flows
  }


  fun Element.id(): String = requireNotNull(attributeValue("id")) { "Element must define 'id' attribute, but $this has none." }
  fun Element.name(): String = attributeValue("name") ?: ""
  fun Element.schema(): Schema? {
    val schemaElement = this.element("schema")
    return if (schemaElement != null) {
      if (schemaElement.hasContent()) {
        // embedded schema
        Schema.EmbeddedSchema(schemaFormat = schemaElement.schemaFormat(), content = schemaElement.textTrim)
      } else {
        // resource
        val resource = schemaElement.resource()
        if (resource != null) {
          Schema.ResourceSchema(schemaFormat = schemaElement.schemaFormat(), resource = resource)
        } else {
          null
        }
      }
    } else {
      null
    }
  }

  inline fun <reified T : FlowElementType.FlowNodeType> Element.typeReference(types: Map<String, FlowElementType.FlowNodeType>): T {
    val type = requireNotNull(types[requireNotNull(attributeValue("typeRef")) { "Element must define a 'typeRef' attribute, but $this has none." }])
    return type as T
  }

  fun Element.untypedMessageFlowType() = FlowElementType.MessageFlowType.MessageTypeReference(
    requireNotNull(attributeValue("typeRef")) { "Element must define a 'typeRef' attribute, but $this has none." }
  )

  fun Element.schemaFormat(): String = attributeValue("schemaFormat") ?: "JSONSchema"
  fun Element.resource(): String? = attributeValue("resource")
  fun Element.sourceRef(): String = requireNotNull(attributeValue("sourceRef")) { "Message flow must define a 'sourceRef' attribute, but $this has none." }
  fun Element.targetRef(): String = requireNotNull(attributeValue("targetRef")) { "Message flow must define a 'targetRef' attribute, but $this has none." }

  fun Element.triggerLanes(): List<Lane.TriggerLane> {
    return this.element("triggerLaneSet")?.elements("triggerLane")?.map { triggerLane ->
      Lane.TriggerLane(id = triggerLane.id(), name = triggerLane.name(), flowElements = triggerLane.flowNodeReferences())
    } ?: emptyList()
  }

  fun Element.aggregateLanes(): List<Lane.AggregateLane> {
    return this.element("aggregateLaneSet")?.elements("aggregateLane")?.map { aggregateLane ->
      Lane.AggregateLane(id = aggregateLane.id(), name = aggregateLane.name(), flowElements = aggregateLane.flowNodeReferences())
    } ?: emptyList()
  }

  fun Element.flowNodeReferences(): List<FlowElement.FlowNode.FlowNodeReference> {
    return this.elements("flowNodeRef")?.map { ref -> FlowElement.FlowNode.FlowNodeReference(ref.textTrim) } ?: emptyList()
  }

  fun Element.sliceSet(): List<Slice> {
    return this.element("sliceSet")?.elements("slice")?.map { slice ->
      Slice(
        id = slice.id(),
        name = slice.name(),
        flowElements = slice.flowNodeReferences()
      )
    } ?: emptyList()
  }
}
