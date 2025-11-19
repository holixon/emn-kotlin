package io.holixon.emn.dom4j

import io.holixon.emn.dom4j.AttributeNames.ID
import io.holixon.emn.dom4j.AttributeNames.NAME
import io.holixon.emn.dom4j.AttributeNames.RESOURCE
import io.holixon.emn.dom4j.AttributeNames.SCENARIO
import io.holixon.emn.dom4j.AttributeNames.SCHEMA_FORMAT
import io.holixon.emn.dom4j.AttributeNames.SLICE_REF
import io.holixon.emn.dom4j.AttributeNames.SOURCE_REF
import io.holixon.emn.dom4j.AttributeNames.STATE_NAME
import io.holixon.emn.dom4j.AttributeNames.TARGET_REF
import io.holixon.emn.dom4j.AttributeNames.TYPE_REF
import io.holixon.emn.dom4j.AttributeNames.VALUE_FORMAT
import io.holixon.emn.dom4j.Defaults.APPLICATION_JSON
import io.holixon.emn.dom4j.ElementNames.AUTOMATION
import io.holixon.emn.dom4j.ElementNames.AUTOMATION_TYPE
import io.holixon.emn.dom4j.ElementNames.COMMAND
import io.holixon.emn.dom4j.ElementNames.COMMAND_TYPE
import io.holixon.emn.dom4j.ElementNames.CONCEPT_LANE
import io.holixon.emn.dom4j.ElementNames.CONCEPT_LANE_SET
import io.holixon.emn.dom4j.ElementNames.INTERACTION_LANE
import io.holixon.emn.dom4j.ElementNames.ERROR
import io.holixon.emn.dom4j.ElementNames.ERROR_TYPE
import io.holixon.emn.dom4j.ElementNames.EVENT
import io.holixon.emn.dom4j.ElementNames.EVENT_TYPE
import io.holixon.emn.dom4j.ElementNames.EXTERNAL_EVENT
import io.holixon.emn.dom4j.ElementNames.EXTERNAL_EVENT_TYPE
import io.holixon.emn.dom4j.ElementNames.EXTERNAL_SYSTEM
import io.holixon.emn.dom4j.ElementNames.EXTERNAL_SYSTEM_TYPE
import io.holixon.emn.dom4j.ElementNames.FLOW_NODE_REF
import io.holixon.emn.dom4j.ElementNames.ID_SCHEMA
import io.holixon.emn.dom4j.ElementNames.INFORMATION_FLOW
import io.holixon.emn.dom4j.ElementNames.INFORMATION_FLOW_TYPE
import io.holixon.emn.dom4j.ElementNames.LANE_SET
import io.holixon.emn.dom4j.ElementNames.QUERY
import io.holixon.emn.dom4j.ElementNames.QUERY_TYPE
import io.holixon.emn.dom4j.ElementNames.SCHEMA
import io.holixon.emn.dom4j.ElementNames.SLICE
import io.holixon.emn.dom4j.ElementNames.SLICE_SET
import io.holixon.emn.dom4j.ElementNames.STAGE_GIVEN
import io.holixon.emn.dom4j.ElementNames.STAGE_THEN
import io.holixon.emn.dom4j.ElementNames.STAGE_WHEN
import io.holixon.emn.dom4j.ElementNames.TRANSLATION
import io.holixon.emn.dom4j.ElementNames.TRANSLATION_TYPE
import io.holixon.emn.dom4j.ElementNames.TRIGGER_LANE
import io.holixon.emn.dom4j.ElementNames.TRIGGER_LANE_SET
import io.holixon.emn.dom4j.ElementNames.VALUE
import io.holixon.emn.dom4j.ElementNames.VIEW
import io.holixon.emn.dom4j.ElementNames.VIEW_TYPE
import io.holixon.emn.model.*
import org.dom4j.Element

/**
 * Checks if the element is defined inside the EMN namespace.
 */
fun Element.isEmn(): Boolean =
  EMN_NAMESPACES.contains(this.namespaceURI.toString())

/**
 * Retrieves a list of elements matching local name and defined in EMN namespace.
 * @param localName local name of the element.
 * @return list of elements.
 */
fun Element.emnElements(localName: String): List<Element> = this.elements(localName).filter { it.isEmn() }

/**
 * Retrieves a list of all child elements defined in EMN namespace.
 * @return list of elements.
 */
fun Element.emnElements(): List<Element> = this.elements().filter { it.isEmn() }

/**
 * Retrieves a list of elements matching local name and defined in EMN namespace.
 * @param localName local name of the element.
 * @return list of elements.
 */
fun Element.emnElement(localName: String): Element? = this.element(localName)?.takeIf { it.isEmn() }

/**
 * Converts as list of element into [InformationFlow] model elements, if
 * applied on a list of `emn:informationFlow` elements.
 */
fun List<Element>.toInformationFlows(): List<InformationFlow> {
  return this.mapNotNull { element ->
    when (element.name) {
      INFORMATION_FLOW -> InformationFlow(
        id = element.id(),
        typeReference = element.untypedMessageFlowType(),
        source = FlowNodeReference(id = element.sourceRef()),
        target = FlowNodeReference(id = element.targetRef())
      )

      else -> null
    }
  }
}

/**
 * Converts a list of elements into [InformationFlowType] model elements.
 * Only processes elements with name matching [INFORMATION_FLOW_TYPE].
 *
 * @return list of information flow type model elements
 */
fun List<Element>.toInformationFlowType(): List<InformationFlowType> {
  return this.mapNotNull { element ->
    when (element.name) {
      INFORMATION_FLOW_TYPE ->
        InformationFlowType(
          id = element.id(),
          name = element.name(),
          source = FlowNodeTypeReference(element.sourceRef()),
          target = FlowNodeTypeReference(element.targetRef())
        )

      else -> null
    }
  }
}

/**
 * Converts the current element to a [Timeline] model element.
 * Extracts slice set, lane set, nodes, and messages from the element.
 *
 * @param typesById map of flow node types by their IDs
 * @param informationFlowTypesById map of information flow types by their IDs
 * @return timeline model element
 */
fun Element.toTimeline(
  typesById: Map<String, FlowNodeType>,
  informationFlowTypesById: Map<String, InformationFlowType>
): Timeline {
  return Timeline(
    sliceSet = this.sliceSet(),
    laneSet = this.laneSet(),
    nodes = listOf(),
    messages = listOf(),
  ).let { timeline ->
    val (nodes, messages) = this.extractFlowElements(typesById, informationFlowTypesById)
    timeline.copy(nodes = nodes, messages = messages)
  }
}

/**
 * Converts the current element to a [Specification] model element.
 * Extracts id, name, scenario, slice reference, and stages (given, when, then) from the element.
 *
 * @param typesById map of flow node types by their IDs
 * @param timelines list of timelines to resolve slice references
 * @return specification model element
 */
fun Element.toSpecification(
  typesById: Map<String, FlowNodeType>,
  timelines: List<Timeline>
): Specification {
  return Specification(
    id = this.id(),
    name = this.name(),
    scenario = this.scenario(),
    slice = this.sliceRef()?.let { sliceRef ->
      timelines.map { it.sliceSet }
        .flatten()
        .first { it.id == sliceRef }
    },
    givenStage = this.givenStage(typesById),
    whenStage = this.whenStage(typesById),
    thenStage = this.thenStage(typesById),
  )
}


/**
 * Converts a list of elements into [FlowNodeType] model elements.
 * Processes elements with names matching various type constants (VIEW_TYPE, COMMAND_TYPE, etc.)
 * and creates the corresponding model objects.
 *
 * @return list of flow node type model elements
 */
fun List<Element>.toFlowTypes(): List<FlowNodeType> {
  return this.mapNotNull { element ->
    when (element.name) {
      VIEW_TYPE ->
        ViewType(
          id = element.id(),
          name = element.name(),
          schema = element.schema()
        )

      COMMAND_TYPE ->
        CommandType(
          id = element.id(),
          name = element.name(),
          schema = element.schema()
        )

      EVENT_TYPE ->
        EventType(
          id = element.id(),
          name = element.name(),
          schema = element.schema()
        )

      QUERY_TYPE ->
        QueryType(
          id = element.id(),
          name = element.name(),
          schema = element.schema()
        )

      ERROR_TYPE ->
        ErrorType(
          id = element.id(),
          name = element.name(),
          schema = element.schema()
        )

      EXTERNAL_EVENT_TYPE ->
        ExternalEventType(
          id = element.id(),
          name = element.name(),
          schema = element.schema()
        )

      EXTERNAL_SYSTEM_TYPE ->
        ExternalSystemType(
          id = element.id(),
          name = element.name(),
          schema = element.schema()
        )

      TRANSLATION_TYPE ->
        TranslationType(
          id = element.id(),
          name = element.name(),
          schema = element.schema()
        )

      AUTOMATION_TYPE ->
        AutomationType(
          id = element.id(),
          name = element.name(),
          schema = element.schema()
        )

      else -> null
    }
  }
}

/**
 * Converts all child elements of the current element into flow nodes.
 * First filters for elements in the EMN namespace, then delegates to List<Element>.toFlowNodes
 * to convert them to the appropriate flow node types.
 *
 * @param typesById map of flow node types by their IDs to resolve type references
 * @return list of flow node model elements created from the child elements
 */
fun Element.toFlowNodes(typesById: Map<String, FlowNodeType>): List<FlowNode> {
  return this.emnElements().toFlowNodes(typesById = typesById)
}

/**
 * Converts a list of elements into [FlowNode] model elements.
 * Processes elements with names matching various node constants (VIEW, COMMAND, etc.)
 * and creates the corresponding model objects.
 *
 * @param typesById map of flow node types by their IDs to resolve type references
 * @return list of flow node model elements
 */
fun List<Element>.toFlowNodes(typesById: Map<String, FlowNodeType>): List<FlowNode> {
  return this.mapNotNull { element ->
    when (element.name) {
      VIEW -> View(
        id = element.id(), typeReference = element.typeReference(typesById), value = element.elementValue()
      )

      COMMAND -> Command(
        id = element.id(), typeReference = element.typeReference(typesById), value = element.elementValue()
      )

      EVENT -> Event(
        id = element.id(), typeReference = element.typeReference(typesById), value = element.elementValue()
      )

      QUERY -> Query(
        id = element.id(), typeReference = element.typeReference(typesById), value = element.elementValue()
      )

      ERROR -> Error(
        id = element.id(), typeReference = element.typeReference(typesById), value = element.elementValue()
      )

      EXTERNAL_EVENT -> ExternalEvent(
        id = element.id(), typeReference = element.typeReference(typesById), value = element.elementValue()
      )

      EXTERNAL_SYSTEM -> ExternalSystem(
        id = element.id(), typeReference = element.typeReference(typesById), value = element.elementValue()
      )

      TRANSLATION -> Translation(
        id = element.id(), typeReference = element.typeReference(typesById), value = element.elementValue()
      )

      AUTOMATION -> Automation(
        id = element.id(), typeReference = element.typeReference(typesById), value = element.elementValue()
      )

      else -> null
    }
  }
}


/**
 * Gets the schema format attribute value from the element.
 * @return the schema format or APPLICATION_JSON as default if not specified
 */
fun Element.schemaFormat(): String = attributeValue(SCHEMA_FORMAT) ?: APPLICATION_JSON

/**
 * Gets the resource attribute value from the element.
 * @return the resource value or null if not specified
 */
fun Element.resource(): String? = attributeValue(RESOURCE)

/**
 * Gets the source reference attribute value from the element.
 * @return the source reference value
 * @throws IllegalArgumentException if the source reference attribute is not present
 */
fun Element.sourceRef(): String = requireNotNull(attributeValue(SOURCE_REF)) { "Message flow must define a '$SOURCE_REF' attribute, but $this has none." }

/**
 * Gets the target reference attribute value from the element.
 * @return the target reference value
 * @throws IllegalArgumentException if the target reference attribute is not present
 */
fun Element.targetRef(): String = requireNotNull(attributeValue(TARGET_REF)) { "Message flow must define a '$TARGET_REF' attribute, but $this has none." }

/**
 * Extracts a lane set from the current element.
 * Looks for a lane set element and extracts trigger lanes, interaction lane, and concept lanes.
 *
 * @return lane set model element or an empty lane set if no lane set element is found
 */
fun Element.laneSet(): LaneSet {
  return this.emnElement(LANE_SET)?.let { laneSet ->
    val interactionLaneElement = laneSet.emnElement(INTERACTION_LANE)
    LaneSet(
      triggerLaneSet = laneSet.triggerLanes(),
      interactionLane = laneSet.interactionLane(),
      conceptLaneSet = laneSet.conceptLanes(),
    )
  } ?: LaneSet()
}

/**
 * Extracts an interaction lane from the current element.
 * Looks for an interaction lane element and extracts its ID, name, and flow elements.
 * If no interaction lane element is found, creates a default interaction lane using the current element's ID and name.
 *
 * @return interaction lane model element
 */
fun Element.interactionLane(): InteractionLane {
  return this.emnElement(INTERACTION_LANE)?.let { interactionLane ->
    InteractionLane(
      id = interactionLane.id(),
      name = interactionLane.name(),
      flowElements = interactionLane.flowNodeReferences(),
    )
  } ?: InteractionLane(
    id = this.id(),
    name = this.name(),
    flowElements = listOf(),
  )
}

/**
 * Extracts trigger lanes from the current element.
 * Looks for a trigger lane set element and extracts all trigger lanes within it.
 *
 * @return list of trigger lane model elements or empty list if no trigger lane set is found
 */
fun Element.triggerLanes(): List<TriggerLane> {
  return this.emnElement(TRIGGER_LANE_SET)?.emnElements(TRIGGER_LANE)?.map { triggerLane ->
    TriggerLane(
      id = triggerLane.id(),
      name = triggerLane.name(),
      flowElements = triggerLane.flowNodeReferences()
    )
  } ?: emptyList()
}

/**
 * Extracts concept lanes from the current element.
 * Looks for a concept lane set element and extracts all concept lanes within it.
 *
 * @return list of concept lane model elements or empty list if no concept lane set is found
 */
fun Element.conceptLanes(): List<ConceptLane> {
  return this.emnElement(CONCEPT_LANE_SET)?.emnElements(CONCEPT_LANE)?.map { conceptLane ->
    ConceptLane(
      id = conceptLane.id(),
      name = conceptLane.name(),
      flowElements = conceptLane.flowNodeReferences(),
      idSchema = conceptLane.idSchema()
    )
  } ?: emptyList()
}

/**
 * Extracts flow node references from the current element.
 * Looks for elements with name matching [FLOW_NODE_REF] and creates reference objects from their text content.
 *
 * @return list of flow node reference model elements
 */
fun Element.flowNodeReferences(): List<FlowNodeReference> {
  return this.emnElements(FLOW_NODE_REF).map { ref -> FlowNodeReference(ref.textTrim) }
}

/**
 * Extracts slices from the current element.
 * Looks for a slice set element and extracts all slices within it.
 *
 * @return list of slice model elements or empty list if no slice set is found
 */
fun Element.sliceSet(): List<Slice> {
  return this.emnElement(SLICE_SET)?.emnElements(SLICE)?.map { slice ->
    Slice(
      id = slice.id(),
      name = slice.name(),
      flowElements = slice.flowNodeReferences()
    )
  } ?: emptyList()
}

/**
 * Gets the scenario attribute value from the element.
 * @return the scenario value or null if not specified
 */
fun Element.scenario(): String? = attributeValue(SCENARIO)

/**
 * Gets the slice reference attribute value from the element.
 * @return the slice reference value or null if not specified
 */
fun Element.sliceRef(): String? = attributeValue(SLICE_REF)

/**
 * Extracts a given stage from the current element.
 * Looks for a given stage element and extracts its ID, state name, and values.
 *
 * @param typesById map of flow node types by their IDs to resolve type references in values
 * @return given stage model element or null if no given stage element is found
 */
fun Element.givenStage(typesById: Map<String, FlowNodeType>): GivenStage? {
  return this.emnElement(STAGE_GIVEN)?.let { givenStageElement ->
    GivenStage(
      id = givenStageElement.id(),
      stateName = givenStageElement.attributeValue(STATE_NAME),
      values = givenStageElement.toFlowNodes(typesById = typesById)
    )
  }
}

/**
 * Extracts a when stage from the current element.
 * Looks for a when stage element and extracts its ID and values.
 *
 * @param typesById map of flow node types by their IDs to resolve type references in values
 * @return when stage model element or null if no when stage element is found
 */
fun Element.whenStage(typesById: Map<String, FlowNodeType>): WhenStage? {
  return this.emnElement(STAGE_WHEN)?.let { whenStageElement ->
    WhenStage(
      id = whenStageElement.id(),
      values = whenStageElement.toFlowNodes(typesById = typesById)
    )
  }
}

/**
 * Extracts a then stage from the current element.
 * Looks for a then stage element and extracts its ID and values.
 *
 * @param typesById map of flow node types by their IDs to resolve type references in values
 * @return then stage model element or null if no then stage element is found
 */
fun Element.thenStage(typesById: Map<String, FlowNodeType>): ThenStage? {
  return this.emnElement(STAGE_THEN)?.let { thenStageElement ->
    ThenStage(
      id = thenStageElement.id(),
      values = thenStageElement.toFlowNodes(typesById = typesById)
    )
  }
}

/**
 * Extracts an element value from the current element.
 * Looks for a value element and creates either an embedded value or a resource value.
 *
 * If the value element has content, creates an embedded value with that content.
 * If the value element has a resource attribute, creates a resource value with that resource.
 *
 * @return element value model element or null if no value element is found or it has neither content nor resource
 */
fun Element.elementValue(): ElementValue? {
  val valueElement = this.emnElement(VALUE)
  return if (valueElement != null) {
    val valueFormat = valueElement.attributeValue(VALUE_FORMAT) ?: APPLICATION_JSON
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


/**
 * Gets the ID attribute value from the element.
 * @return the ID value
 * @throws IllegalArgumentException if the ID attribute is not present
 */
fun Element.id(): String = requireNotNull(attributeValue(ID)) { "Element must define '$ID' attribute, but $this has none." }

/**
 * Gets the name attribute value from the element.
 * @return the name value or empty string if not specified
 */
fun Element.name(): String = attributeValue(NAME) ?: ""

/**
 * Gets the schema element from the current element and converts it to a Schema object.
 * @return the Schema object or null if no schema element is present
 */
fun Element.schema(): Schema? = this.emnElement(SCHEMA)?.toSchema()

/**
 * Gets the ID schema element from the current element and converts it to a Schema object.
 * @return the Schema object or null if no ID schema element is present
 */
fun Element.idSchema(): Schema? = this.emnElement(ID_SCHEMA)?.toSchema()

/**
 * Converts the current element to a Schema object.
 * If the element has non-empty content, it creates an EmbeddedSchema with the content.
 * If the element has a resource attribute, it creates a ResourceSchema with the resource.
 *
 * @return the Schema object or null if neither content nor resource is present
 */
fun Element.toSchema(): Schema? {
  // Check for resource first
  val resource = this.resource()
  if (resource != null) {
    return ResourceSchema(schemaFormat = this.schemaFormat(), resource = resource)
  }

  // Then check for non-empty content
  if (this.hasContent() && this.textTrim.isNotEmpty()) {
    return EmbeddedSchema(schemaFormat = this.schemaFormat(), content = this.textTrim)
  }

  // Neither resource nor content
  return null
}


/**
 * Gets a type reference from the current element.
 * Extracts the type reference attribute and looks up the corresponding type in the provided map.
 *
 * @param T the specific type of FlowNodeType to return
 * @param types map of flow node types by their IDs
 * @return the flow node type corresponding to the type reference
 * @throws IllegalArgumentException if the type reference attribute is not present
 * @throws IllegalStateException if the referenced type is not found in the map
 */
inline fun <reified T : FlowNodeType> Element.typeReference(types: Map<String, FlowNodeType>): T {
  val type = requireNotNull(types[requireNotNull(attributeValue(TYPE_REF)) { "Element must define a '$TYPE_REF' attribute, but $this has none." }])
  return type as T
}

/**
 * Creates an untyped information flow type reference from the current element.
 * Extracts the type reference attribute and creates a reference without resolving the actual type.
 *
 * @return an information type reference
 * @throws IllegalArgumentException if the type reference attribute is not present
 */
fun Element.untypedMessageFlowType() = InformationFlowType.InformationTypeReference(
  requireNotNull(attributeValue(TYPE_REF)) { "Element must define a '$TYPE_REF' attribute, but $this has none." })

/**
 * Extracts flow nodes and information flows from the current element.
 * Processes all child elements except slice set and lane set elements.
 * Resolves references between nodes and flows, and patches the flows with the actual node and type references.
 *
 * @param typesById map of flow node types by their IDs to resolve type references
 * @param informationFlowTypesById map of information flow types by their IDs to resolve flow type references
 * @return a pair of (list of flow nodes, list of information flows)
 * @throws IllegalArgumentException if a referenced source, target, or type is not found
 */
fun Element.extractFlowElements(
  typesById: Map<String, FlowNodeType>,
  informationFlowTypesById: Map<String, InformationFlowType>
): Pair<List<FlowNode>, List<InformationFlow>> {
  val nodes = mutableListOf<FlowNode>()
  val informationFlows = mutableListOf<InformationFlow>()

  val allFlowElements = this.emnElements().filterNot { it.name == SLICE_SET || it.name == LANE_SET }
  nodes.addAll(allFlowElements.toFlowNodes(typesById = typesById))
  informationFlows.addAll(allFlowElements.toInformationFlows())

  /*
   * Patch flows
   */
  val elementsById = nodes.associateBy { it.id }

  val flows = informationFlows.map { messageFlow ->
    val sourceElement =
      requireNotNull(elementsById[messageFlow.source.id]) { "Unknown source ${messageFlow.source.id}" }
    val targetElement =
      requireNotNull(elementsById[messageFlow.target.id]) { "Unknown target ${messageFlow.target.id}" }
    val messageFlowType =
      requireNotNull(informationFlowTypesById[messageFlow.typeReference.id]) { "Unknown type ${messageFlow.typeReference.id}" }
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
