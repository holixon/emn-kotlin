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
import io.holixon.emn.dom4j.ElementNames.COMMAND
import io.holixon.emn.dom4j.ElementNames.CONCEPT_LANE
import io.holixon.emn.dom4j.ElementNames.CONCEPT_LANE_SET
import io.holixon.emn.dom4j.ElementNames.ERROR
import io.holixon.emn.dom4j.ElementNames.EVENT
import io.holixon.emn.dom4j.ElementNames.EXTERNAL_EVENT
import io.holixon.emn.dom4j.ElementNames.EXTERNAL_SYSTEM
import io.holixon.emn.dom4j.ElementNames.FLOW_NODE_REF
import io.holixon.emn.dom4j.ElementNames.ID_SCHEMA
import io.holixon.emn.dom4j.ElementNames.INFORMATION_FLOW
import io.holixon.emn.dom4j.ElementNames.LANE_SET
import io.holixon.emn.dom4j.ElementNames.QUERY
import io.holixon.emn.dom4j.ElementNames.SCHEMA
import io.holixon.emn.dom4j.ElementNames.SLICE
import io.holixon.emn.dom4j.ElementNames.SLICE_SET
import io.holixon.emn.dom4j.ElementNames.STAGE_GIVEN
import io.holixon.emn.dom4j.ElementNames.STAGE_THEN
import io.holixon.emn.dom4j.ElementNames.STAGE_WHEN
import io.holixon.emn.dom4j.ElementNames.TRANSLATION
import io.holixon.emn.dom4j.ElementNames.TRIGGER_LANE
import io.holixon.emn.dom4j.ElementNames.TRIGGER_LANE_SET
import io.holixon.emn.dom4j.ElementNames.VALUE
import io.holixon.emn.dom4j.ElementNames.VIEW
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


fun Element.schemaFormat(): String = attributeValue(SCHEMA_FORMAT) ?: APPLICATION_JSON

fun Element.resource(): String? = attributeValue(RESOURCE)

fun Element.sourceRef(): String = requireNotNull(attributeValue(SOURCE_REF)) { "Message flow must define a '$SOURCE_REF' attribute, but $this has none." }

fun Element.targetRef(): String = requireNotNull(attributeValue(TARGET_REF)) { "Message flow must define a '$TARGET_REF' attribute, but $this has none." }

fun Element.triggerLanes(): List<TriggerLane> {
  return this.emnElement(TRIGGER_LANE_SET)?.emnElements(TRIGGER_LANE)?.map { triggerLane ->
    TriggerLane(
      id = triggerLane.id(),
      name = triggerLane.name(),
      flowElements = triggerLane.flowNodeReferences()
    )
  } ?: emptyList()
}

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

fun Element.flowNodeReferences(): List<FlowNodeReference> {
  return this.emnElements(FLOW_NODE_REF).map { ref -> FlowNodeReference(ref.textTrim) }
}

fun Element.sliceSet(): List<Slice> {
  return this.emnElement(SLICE_SET)?.emnElements(SLICE)?.map { slice ->
    Slice(
      id = slice.id(),
      name = slice.name(),
      flowElements = slice.flowNodeReferences()
    )
  } ?: emptyList()
}

fun Element.laneSet(): LaneSet {
  return this.emnElement(LANE_SET)?.let { laneSet ->
    LaneSet(
      triggerLaneSet = laneSet.triggerLanes(),
      interactionLane = InteractionLane(
        id = laneSet.id(),
        name = laneSet.name(),
        flowElements = laneSet.flowNodeReferences(),
      ),
      conceptLaneSet = laneSet.conceptLanes(),
    )
  } ?: LaneSet()
}


fun Element.scenario(): String? = attributeValue(SCENARIO)

fun Element.sliceRef(): String? = attributeValue(SLICE_REF)

fun Element.givenStage(typesById: Map<String, FlowNodeType>): GivenStage? {
  return this.emnElement(STAGE_GIVEN)?.let { givenStageElement ->
    GivenStage(
      id = givenStageElement.id(),
      stateName = givenStageElement.attributeValue(STATE_NAME),
      values = givenStageElement.elementValues(typesById = typesById)
    )
  }
}

fun Element.whenStage(typesById: Map<String, FlowNodeType>): WhenStage? {
  return this.emnElement(STAGE_WHEN)?.let { whenStageElement ->
    WhenStage(
      id = whenStageElement.id(),
      values = whenStageElement.elementValues(typesById = typesById)
    )
  }
}

fun Element.thenStage(typesById: Map<String, FlowNodeType>): ThenStage? {
  return this.emnElement(STAGE_THEN)?.let { thenStageElement ->
    ThenStage(
      id = thenStageElement.id(),
      values = thenStageElement.elementValues(typesById = typesById)
    )
  }
}

fun Element.elementValues(typesById: Map<String, FlowNodeType>): List<FlowNode> {
  return this.emnElements().toFlowNodes(typesById = typesById)
}

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


fun Element.id(): String = requireNotNull(attributeValue(ID)) { "Element must define '$ID' attribute, but $this has none." }


fun Element.name(): String = attributeValue(NAME) ?: ""
fun Element.schema(): Schema? = this.emnElement(SCHEMA)?.toSchema()
fun Element.idSchema(): Schema? = this.emnElement(ID_SCHEMA)?.toSchema()

fun Element.toSchema(): Schema? {
  return if (this.hasContent()) {
    // embedded schema
    EmbeddedSchema(schemaFormat = this.schemaFormat(), content = this.textTrim)
  } else {
    // resource
    val resource = this.resource()
    if (resource != null) {
      ResourceSchema(schemaFormat = this.schemaFormat(), resource = resource)
    } else {
      null
    }
  }
}


inline fun <reified T : FlowNodeType> Element.typeReference(types: Map<String, FlowNodeType>): T {
  val type = requireNotNull(types[requireNotNull(attributeValue(TYPE_REF)) { "Element must define a '$TYPE_REF' attribute, but $this has none." }])
  return type as T
}

fun Element.untypedMessageFlowType() = InformationFlowType.InformationTypeReference(
  requireNotNull(attributeValue(TYPE_REF)) { "Element must define a '$TYPE_REF' attribute, but $this has none." })

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
