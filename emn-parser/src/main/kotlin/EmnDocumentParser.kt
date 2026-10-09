package io.holixon.emn

import io.holixon.emn.model.*
import org.dom4j.Document
import org.dom4j.Element
import org.dom4j.io.SAXReader
import java.io.File
import java.lang.reflect.Type
import java.net.URL

class EmnDocumentParser {

  fun parseDefinitions(file: File): Definitions {
    val reader = SAXReader()
    val document = reader.read(file)
    return parseDefinitions(document)
  }

  fun parseTypeDefinitions(file: File): Definitions {
    val reader = SAXReader()
    val document = reader.read(file)
    return parseDefinitions(document, typesOnly = true)
  }

  fun parseTypeDefinitions(url: URL): Definitions {
    val reader = SAXReader()
    val document = reader.read(url)
    return parseDefinitions(document, typesOnly = true)
  }

  fun parseDefinitions(url: URL): Definitions {
    val reader = SAXReader()
    val document = reader.read(url)
    return parseDefinitions(document)
  }

  fun parseDefinitions(document: Document): Definitions = parseDefinitions(document, typesOnly = false)

  private fun parseDefinitions(document: Document, typesOnly: Boolean): Definitions {

    val root = document.rootElement // <definitions>
    requireNotNull(root)
    require(root.name == "definitions") { "Can't parse definitions, this is probably not a EMN file" }

    val nodeTypes = mutableListOf<FlowNodeType>()
    val messageFlowTypes = mutableListOf<MessageFlowType>()
    val schemaDefinitions = mutableListOf<SchemaDefinition>()
    val eventSequenceTypeElements = mutableListOf<Element>()

    /*
     * Parse types
     */
    root.element("types")?.elements()?.forEach { element ->
      when (element.name) {
        "schemaDefinition" -> schemaDefinitions.add(
          SchemaDefinition(
            id = element.id(),
            name = element.name(),
            schema = element.schema()
          )
        )

        "viewType" -> nodeTypes.add(
          ViewType(
            id = element.id(),
            name = element.name(),
            schema = element.schema()
          )
        )

        "commandType" -> nodeTypes.add(
          CommandType(
            id = element.id(),
            name = element.name(),
            schema = element.schema()
          )
        )

        "eventType" -> nodeTypes.add(
          EventType(
            id = element.id(),
            name = element.name(),
            schema = element.schema()
          )
        )

        "eventSequenceType" -> eventSequenceTypeElements.add(element)

        "queryType" -> nodeTypes.add(
          QueryType(
            id = element.id(),
            name = element.name(),
            schema = element.schema()
          )
        )

        "errorType" -> nodeTypes.add(
          ErrorType(
            id = element.id(),
            name = element.name(),
            schema = element.schema()
          )
        )

        "externalEventType" -> nodeTypes.add(
          ExternalEventType(
            id = element.id(),
            name = element.name(),
            schema = element.schema()
          )
        )

        "externalSystemType" -> nodeTypes.add(
          ExternalSystemType(
            id = element.id(),
            name = element.name(),
            schema = element.schema()
          )
        )

        "translationType" -> nodeTypes.add(
          TranslationType(
            id = element.id(),
            name = element.name(),
            schema = element.schema()
          )
        )

        "automationType" -> nodeTypes.add(
          AutomationType(
            id = element.id(),
            name = element.name(),
            schema = element.schema()
          )
        )

        "messageFlowType", "informationFlowType" -> if (!typesOnly) {
          messageFlowTypes.add(
            MessageFlowType(
              id = element.id(),
              name = element.name(),
              source = FlowNodeTypeReference(element.sourceRef()),
              target = FlowNodeTypeReference(element.targetRef())
            )
          )
        }

        else -> if (!typesOnly) {
          println("Unknown element '${element.name}'")
        }
      }
    }

    val eventTypesById = nodeTypes.filterIsInstance<EventType>().associateBy { it.id }
    eventSequenceTypeElements.forEach { element ->
      nodeTypes.add(
        EventSequenceType(
          id = element.id(),
          name = element.name(),
          eventTypes = element.elements("eventTypeRef").map { reference ->
            val eventTypeId = reference.textTrim
            requireNotNull(eventTypesById[eventTypeId]) {
              "Event sequence type '${element.id()}' references unknown event type '$eventTypeId'."
            }
          },
        )
      )
    }

    if (typesOnly) {
      return Definitions(
        nodeTypes = nodeTypes,
        flowTypes = emptyList(),
        timelines = emptyList(),
        specifications = emptyList(),
        schemaDefinitions = schemaDefinitions,
        eventSequenceDefinitions = emptyList(),
      )
    }

    /**
     * Patch message types
     */
    val typesById = nodeTypes.associateBy { it.id }
    val messageFlowTypesById = messageFlowTypes.associateBy { it.id }

    val flowTypes = messageFlowTypes.map { messageFlowType ->
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

    val eventSequenceDefinitions = root.elements("eventSequenceDefinition").map { element ->
      val typeId = element.typeRef()
      val type = requireNotNull(typesById[typeId] as? EventSequenceType) {
        "Event sequence definition '${element.id()}' references unknown event sequence type '$typeId'."
      }
      EventSequenceDefinition(
        id = element.id(),
        typeReference = type,
        variableDefinitions = element.elements("variableDefinition").map { variable ->
          VariableDefinition(name = variable.requiredName(), value = variable.textTrim)
        },
        events = element.elements("event")
          .toFlowNodes(typesById, emptyMap())
          .filterIsInstance<Event>(),
      )
    }
    val eventSequenceDefinitionsById = eventSequenceDefinitions.associateBy { it.id }

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
              interactionLane = InteractionLane(
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
          val (nodes, messages) = element.extractFlowElements(
            typesById,
            messageFlowTypesById,
            eventSequenceDefinitionsById,
          )
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
          aggregateLaneSet = timeline.laneSet.aggregateLaneSet.map { aggregateLane ->
            aggregateLane.copy(
              flowElements = aggregateLane.flowElements.filterIsInstance<FlowNodeReference>()
                .map { e -> nodesById.getValue(e.id) }
            )
          }
        )
      )
    }

    val specifications = mutableListOf<Specification>()
    root.elements("specification")?.forEach { element ->
      specifications.add(
        Specification(
          id = element.id(),
          name = element.name(),
          scenario = element.scenario(),
          slice = element.sliceRef()
            ?.let { sliceRef -> timelines.map { it.sliceSet }.flatten().first { it.id == sliceRef } },
          givenStage = element.givenStage(typesById, eventSequenceDefinitionsById),
          whenStage = element.whenStage(typesById, eventSequenceDefinitionsById),
          thenStage = element.thenStage(typesById, eventSequenceDefinitionsById),
        )
      )
    }

    return Definitions(
      nodeTypes = nodeTypes,
      flowTypes = flowTypes,
      timelines = patchedTimelines,
      specifications = specifications,
      schemaDefinitions = schemaDefinitions,
      eventSequenceDefinitions = eventSequenceDefinitions,
    )
  }

  fun Element.extractFlowElements(
    typesById: Map<String, FlowNodeType>,
    messageFlowTypesById: Map<String, MessageFlowType>,
    eventSequenceDefinitionsById: Map<String, EventSequenceDefinition>,
  ): Pair<List<FlowNode>, List<MessageFlow>> {
    val nodes = mutableListOf<FlowNode>()
    val messageFlows = mutableListOf<MessageFlow>()

    val allFlowElements = this.elements().filterNot { it.name == "sliceSet" || it.name == "laneSet" }
    nodes.addAll(allFlowElements.toFlowNodes(typesById, eventSequenceDefinitionsById))
    messageFlows.addAll(allFlowElements.toMessageFlows())

    /*
     * Patch flows
     */
    val elementsById = nodes.associateBy { it.id }

    val flows = messageFlows.map { messageFlow ->
      val sourceElement =
        requireNotNull(elementsById[messageFlow.source.id]) { "Unknown source ${messageFlow.source.id}" }
      val targetElement =
        requireNotNull(elementsById[messageFlow.target.id]) { "Unknown target ${messageFlow.target.id}" }
      val messageFlowType =
        requireNotNull(messageFlowTypesById[messageFlow.typeReference.id]) { "Unknown type ${messageFlow.typeReference.id}" }
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

  fun List<Element>.toMessageFlows(): List<MessageFlow> {
    return this.mapNotNull { element ->
      when (element.name) {
        "messageFlow", "informationFlow" ->
          MessageFlow(
            id = element.id(),
            typeReference = element.untypedMessageFlowType(),
            source = FlowNodeReference(id = element.sourceRef()),
            target = FlowNodeReference(id = element.targetRef())
          )

        else -> null
      }
    }
  }

  fun List<Element>.toFlowNodes(
    typesById: Map<String, FlowNodeType>,
    eventSequenceDefinitionsById: Map<String, EventSequenceDefinition> = emptyMap(),
  ): List<FlowNode> {
    return this.mapNotNull { element ->
      when (element.name) {
        "view" ->
          View(
            id = element.id(),
            typeReference = element.typeReference(typesById),
            value = element.elementValue()
          )

        "command" ->
          Command(
            id = element.id(),
            typeReference = element.typeReference(typesById),
            value = element.elementValue()
          )

        "event" ->
          Event(
            id = element.id(),
            typeReference = element.typeReference(typesById),
            value = element.elementValue()
          )

        "eventSequence" -> {
          val definitionId = element.definitionRef()
          EventSequence(
            id = element.id(),
            definitionReference = requireNotNull(eventSequenceDefinitionsById[definitionId]) {
              "Event sequence '${element.id()}' references unknown definition '$definitionId'."
            },
            variables = element.elements("variable").map { variable ->
              Variable(name = variable.requiredName(), value = variable.textTrim)
            },
            value = element.elementValue(),
          )
        }

        "query" ->
          Query(
            id = element.id(),
            typeReference = element.typeReference(typesById),
            value = element.elementValue()
          )

        "error" ->
          Error(
            id = element.id(),
            typeReference = element.typeReference(typesById),
            value = element.elementValue()
          )

        "externalEvent" ->
          ExternalEvent(
            id = element.id(),
            typeReference = element.typeReference(typesById),
            value = element.elementValue()
          )

        "externalSystem" ->
          ExternalSystem(
            id = element.id(),
            typeReference = element.typeReference(typesById),
            value = element.elementValue()
          )

        "translation" ->
          Translation(
            id = element.id(),
            typeReference = element.typeReference(typesById),
            value = element.elementValue()
          )

        "automation" ->
          Automation(
            id = element.id(),
            typeReference = element.typeReference(typesById),
            value = element.elementValue()
          )

        else -> null
      }
    }
  }

  fun Element.id(): String =
    requireNotNull(attributeValue("id")) { "Element must define 'id' attribute, but $this has none." }

  fun Element.name(): String = attributeValue("name") ?: ""
  fun Element.requiredName(): String =
    requireNotNull(attributeValue("name")) { "Element must define a 'name' attribute, but $this has none." }
  fun Element.typeRef(): String =
    requireNotNull(attributeValue("typeRef")) { "Element must define a 'typeRef' attribute, but $this has none." }
  fun Element.definitionRef(): String =
    requireNotNull(attributeValue("definitionRef")) { "Element must define a 'definitionRef' attribute, but $this has none." }
  fun Element.schema(): Schema? = constructSchema(this.element("schema"))
  fun Element.idSchema(): Schema? = constructSchema(this.element("idSchema"))

  private fun constructSchema(schemaElement: Element?): Schema? {
    return if (schemaElement != null) {
      if (schemaElement.hasContent()) {
        // embedded schema
        EmbeddedSchema(schemaFormat = schemaElement.schemaFormat(), content = schemaElement.textTrim)
      } else {
        // resource
        val resource = schemaElement.resource()
        if (resource != null) {
          ResourceSchema(schemaFormat = schemaElement.schemaFormat(), resource = resource)
        } else {
          val reference = schemaElement.reference()
          if (reference != null) {
            ReferencedSchema(schemaFormat = schemaElement.schemaFormat(), reference = reference)
          } else {
            null
          }
        }
      }
    } else {
      null
    }
  }

  inline fun <reified T : FlowNodeType> Element.typeReference(types: Map<String, FlowNodeType>): T {
    val type =
      requireNotNull(types[requireNotNull(attributeValue("typeRef")) { "Element must define a 'typeRef' attribute, but $this has none." }])
    return type as T
  }

  fun Element.untypedMessageFlowType() = MessageFlowType.MessageTypeReference(
    requireNotNull(attributeValue("typeRef")) { "Element must define a 'typeRef' attribute, but $this has none." }
  )

  fun Element.schemaFormat(): String = attributeValue("schemaFormat") ?: "JSONSchema"
  fun Element.resource(): String? = attributeValue("resource")
  fun Element.reference(): String? = attributeValue("reference")
  fun Element.sourceRef(): String =
    requireNotNull(attributeValue("sourceRef")) { "Message flow must define a 'sourceRef' attribute, but $this has none." }

  fun Element.targetRef(): String =
    requireNotNull(attributeValue("targetRef")) { "Message flow must define a 'targetRef' attribute, but $this has none." }

  fun Element.triggerLanes(): List<TriggerLane> {
    return this.element("triggerLaneSet")?.elements("triggerLane")?.map { triggerLane ->
      TriggerLane(
        id = triggerLane.id(),
        name = triggerLane.name(),
        flowElements = triggerLane.flowNodeReferences()
      )
    } ?: emptyList()
  }

  fun Element.aggregateLanes(): List<AggregateLane> {
    return this.element("aggregateLaneSet")?.elements("aggregateLane")?.map { aggregateLane ->
      AggregateLane(
        id = aggregateLane.id(),
        name = aggregateLane.name(),
        flowElements = aggregateLane.flowNodeReferences(),
        idSchema = aggregateLane.idSchema()
      )
    } ?: emptyList()
  }

  fun Element.flowNodeReferences(): List<FlowNodeReference> {
    return this.elements("flowNodeRef")?.map { ref -> FlowNodeReference(ref.textTrim) }
      ?: emptyList()
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

  fun Element.scenario(): String? = attributeValue("scenario")
  fun Element.sliceRef(): String? = attributeValue("sliceRef")
  fun Element.givenStage(
    typesById: Map<String, FlowNodeType>,
    eventSequenceDefinitionsById: Map<String, EventSequenceDefinition>,
  ): GivenStage? {
    return this.element("given")?.let { givenStageElement ->
      GivenStage(
        id = givenStageElement.id(),
        stateName = givenStageElement.attributeValue("stateName"),
        values = givenStageElement.elementValues(typesById, eventSequenceDefinitionsById)
      )
    }
  }

  fun Element.whenStage(
    typesById: Map<String, FlowNodeType>,
    eventSequenceDefinitionsById: Map<String, EventSequenceDefinition>,
  ): WhenStage? {
    return this.element("when")?.let { whenStageElement ->
      WhenStage(
        id = whenStageElement.id(),
        values = whenStageElement.elementValues(typesById, eventSequenceDefinitionsById)
      )
    }
  }

  fun Element.thenStage(
    typesById: Map<String, FlowNodeType>,
    eventSequenceDefinitionsById: Map<String, EventSequenceDefinition>,
  ): ThenStage? {
    return this.element("then")?.let { thenStageElement ->
      ThenStage(
        id = thenStageElement.id(),
        values = thenStageElement.elementValues(typesById, eventSequenceDefinitionsById)
      )
    }
  }

  fun Element.elementValues(
    typesById: Map<String, FlowNodeType>,
    eventSequenceDefinitionsById: Map<String, EventSequenceDefinition>,
  ): List<FlowNode> {
    return this.elements().toFlowNodes(typesById, eventSequenceDefinitionsById)
  }

  fun Element.elementValue(): ElementValue? {
    val valueElement = this.element("value")
    return if (valueElement != null) {
      val valueFormat = valueElement.attributeValue("valueFormat") ?: "application/json"
      if (valueElement.hasContent()) {
        // embedded example
        EmbeddedValue(valueFormat = valueFormat, content = valueElement.textTrim)
      } else {
        val resource = valueElement.resource()
        if (resource != null) {
          // external example resource
          ResourceValue(valueFormat = valueFormat, resource = resource)
        } else {
          return null
        }
      }
    } else {
      null
    }
  }

  //
}
