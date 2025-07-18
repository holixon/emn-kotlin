package io.holixon.emn

import io.holixon.emn.model.*
import io.holixon.emn.model.FlowElement.FlowNode.FlowNodeReference
import io.holixon.emn.model.Lane.AggregateLane
import io.holixon.emn.model.Lane.TriggerLane
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
                    val (nodes, messages) = element.extractFlowElements(typesById, messageFlowTypesById)
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

    fun Element.extractFlowElements(
        typesById: Map<String, FlowElementType.FlowNodeType>,
        messageFlowTypesById: Map<String, FlowElementType.MessageFlowType>
    ): Pair<List<FlowElement.FlowNode>, List<FlowElement.MessageFlow>> {
        val nodes = mutableListOf<FlowElement.FlowNode>()
        val messageFlows = mutableListOf<FlowElement.MessageFlow>()

        val allFlowElements = this.elements().filterNot { it.name == "sliceSet" || it.name == "laneSet" }
        nodes.addAll(allFlowElements.toFlowNodes(typesById = typesById))
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

    fun List<Element>.toMessageFlows(): List<FlowElement.MessageFlow> {
        return this.mapNotNull { element ->
            when (element.name) {
                "messageFlow" ->
                    FlowElement.MessageFlow(
                        id = element.id(),
                        typeReference = element.untypedMessageFlowType(),
                        source = FlowNodeReference(id = element.sourceRef()),
                        target = FlowNodeReference(id = element.targetRef())
                    )

                else -> null
            }
        }
    }

    fun List<Element>.toFlowNodes(typesById: Map<String, FlowElementType.FlowNodeType>): List<FlowElement.FlowNode> {
        return this.mapNotNull { element ->
            when (element.name) {
                "view" ->
                    FlowElement.FlowNode.View(
                        id = element.id(),
                        typeReference = element.typeReference(typesById),
                        example = element.example()
                    )

                "command" ->
                    FlowElement.FlowNode.Command(
                        id = element.id(),
                        typeReference = element.typeReference(typesById),
                        example = element.example()
                    )

                "event" ->
                    FlowElement.FlowNode.Event(
                        id = element.id(),
                        typeReference = element.typeReference(typesById),
                        example = element.example()
                    )

                "query" ->
                    FlowElement.FlowNode.Query(
                        id = element.id(),
                        typeReference = element.typeReference(typesById),
                        example = element.example()
                    )

                "error" ->
                    FlowElement.FlowNode.Error(
                        id = element.id(),
                        typeReference = element.typeReference(typesById),
                        example = element.example()
                    )

                "externalEvent" ->
                    FlowElement.FlowNode.ExternalEvent(
                        id = element.id(),
                        typeReference = element.typeReference(typesById),
                        example = element.example()
                    )

                "externalSystem" ->
                    FlowElement.FlowNode.ExternalSystem(
                        id = element.id(),
                        typeReference = element.typeReference(typesById),
                        example = element.example()
                    )

                "translation" ->
                    FlowElement.FlowNode.Translation(
                        id = element.id(),
                        typeReference = element.typeReference(typesById),
                        example = element.example()
                    )

                "automation" ->
                    FlowElement.FlowNode.Automation(
                        id = element.id(),
                        typeReference = element.typeReference(typesById),
                        example = element.example()
                    )

                else -> null
            }
        }
    }

    fun Element.id(): String =
        requireNotNull(attributeValue("id")) { "Element must define 'id' attribute, but $this has none." }

    fun Element.name(): String = attributeValue("name") ?: ""
    fun Element.schema(): Schema? = constructSchema(this.element("schema"))
    fun Element.idSchema(): Schema? = constructSchema(this.element("idSchema"))

    private fun constructSchema(schemaElement: Element?): Schema? {
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
        val type =
            requireNotNull(types[requireNotNull(attributeValue("typeRef")) { "Element must define a 'typeRef' attribute, but $this has none." }])
        return type as T
    }

    fun Element.untypedMessageFlowType() = FlowElementType.MessageFlowType.MessageTypeReference(
        requireNotNull(attributeValue("typeRef")) { "Element must define a 'typeRef' attribute, but $this has none." }
    )

    fun Element.schemaFormat(): String = attributeValue("schemaFormat") ?: "JSONSchema"
    fun Element.resource(): String? = attributeValue("resource")
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
    fun Element.givenStage(typesById: Map<String, FlowElementType.FlowNodeType>): Stage.GivenStage? {
        return this.element("given")?.let { givenStageElement ->
            Stage.GivenStage(
                id = givenStageElement.id(),
                stateName = givenStageElement.attributeValue("stateName"),
                examples = givenStageElement.examples(typesById = typesById)
            )
        }
    }

    fun Element.whenStage(typesById: Map<String, FlowElementType.FlowNodeType>): Stage.WhenStage? {
        return this.element("when")?.let { whenStageElement ->
            Stage.WhenStage(
                id = whenStageElement.id(),
                examples = whenStageElement.examples(typesById = typesById)
            )
        }
    }

    fun Element.thenStage(typesById: Map<String, FlowElementType.FlowNodeType>): Stage.ThenStage? {
        return this.element("then")?.let { thenStageElement ->
            Stage.ThenStage(
                id = thenStageElement.id(),
                examples = thenStageElement.examples(typesById = typesById)
            )
        }
    }

    fun Element.examples(typesById: Map<String, FlowElementType.FlowNodeType>): List<FlowElement.FlowNode> {
        return this.elements().toFlowNodes(typesById = typesById)
    }

    fun Element.example(): ExampleValue? {
        val exampleElement = this.element("example")
        return if (exampleElement != null) {
            val valueFormat = exampleElement.attributeValue("valueFormat") ?: "application/json"
            if (exampleElement.hasContent()) {
                // embedded example
                ExampleValue.EmbeddedValue(valueFormat = valueFormat, content = exampleElement.textTrim)
            } else {
                val resource = exampleElement.resource()
                if (resource != null) {
                    // external example resource
                    ExampleValue.ResourceValue(valueFormat = valueFormat, resource = resource)
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