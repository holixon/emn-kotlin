package io.holixon.emn.model

import io.holixon.emn.model.FlowElementType.FlowNodeType
import io.holixon.emn.model.FlowElementType.FlowNodeType.*
import io.holixon.emn.model.FlowElementType.MessageFlowType
import org.dom4j.Element
import org.dom4j.io.SAXReader
import java.io.File

class EmnDocumentParser {

  fun parseDefinitions(file: File): Definitions {

    val reader = SAXReader()
    val document = reader.read(file)
    val root = document.rootElement // <definitions>
    requireNotNull(root)
    require(root.name == "definitions") { "Can't parse definitions, this is probably not a EMN file" }

    val nodeTypes = mutableListOf<FlowNodeType>()
    val messageFlowTypes = mutableListOf<MessageFlowType>()

    root.element("types")?.elements()?.forEach { element ->
      when (element.name) {
        "viewType" -> nodeTypes.add(ViewType(id = element.id(), name = element.name(), schema = element.schema()))
        "commandType" -> nodeTypes.add(CommandType(id = element.id(), name = element.name(), schema = element.schema()))
        "eventType" -> nodeTypes.add(EventType(id = element.id(), name = element.name(), schema = element.schema()))
        "queryType" -> nodeTypes.add(QueryType(id = element.id(), name = element.name(), schema = element.schema()))
        "messageFlowType" -> messageFlowTypes.add(
          MessageFlowType(
            id = element.id(),
            name = element.name(),
            source = FlowNodeTypeReference(element.sourceRef()),
            target = FlowNodeTypeReference(element.targetRef())
          )
        )

        else -> println("Unknown element '${element.name}'")
      }
    }

    val typesById = nodeTypes.associateBy { it.id }

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

    return Definitions(
      nodes = nodeTypes,
      flows = flowTypes
    )
  }


  fun Element.id(): String = attributeValue("id")
  fun Element.name(): String = attributeValue("name") ?: ""
  fun Element.schema(): Schema? {
    val schemaElement = this.element("schema")
    return if (schemaElement != null) {
      if (schemaElement.hasContent()) {
        // embedded schema
        Schema.EmbeddedSchema(schemaFormat = this.schemaFormat(), content = schemaElement.textTrim)
      } else {
        // resource
        val resource = schemaElement.resource()
        if (resource != null) {
          Schema.ResourceSchema(schemaFormat = schemaFormat(), resource = resource)
        } else {
          null
        }
      }
    } else {
      null
    }
  }

  fun Element.schemaFormat(): String = attributeValue("schemaFormat") ?: "JSONSchema"
  fun Element.resource(): String? = attributeValue("resource")
  fun Element.sourceRef(): String = requireNotNull(attributeValue("sourceRef")) { "Message flow must define a 'sourceRef' attribute, but $this has none." }
  fun Element.targetRef(): String = requireNotNull(attributeValue("targetRef")) { "Message flow must define a 'targetRef' attribute, but $this has none." }
}
