package io.holixon.emn.generation

import io.holixon.emn.model.Definitions
import io.holixon.emn.model.FlowElementType
import io.holixon.emn.model.Timeline
import io.toolisticon.kotlin.avro.value.CanonicalName

class EmnDeclaration(
    val definitions: Definitions
) {

    val avroReferenceTypes: Map<CanonicalName, FlowElementType.FlowNodeType> by lazy {
        definitions.nodeTypes.filter { it.hasAvroTypeDefinition() }
            .associateBy { CanonicalName.parse(it.schemaReference()) }
    }

    val commandsBySchemaReference: Map<CanonicalName, FlowElementType.FlowNodeType.CommandType> by lazy {
        avroReferenceTypes.filter { it.value is FlowElementType.FlowNodeType.CommandType }
            .mapValues {
                it.value as FlowElementType.FlowNodeType.CommandType
            }
    }

    val eventsBySchemaReference: Map<CanonicalName, FlowElementType.FlowNodeType.EventType> by lazy {
        avroReferenceTypes.filter { it.value is FlowElementType.FlowNodeType.EventType }
            .mapValues {
                it.value as FlowElementType.FlowNodeType.EventType
            }
    }

    val timelines: List<Timeline> by lazy {
        definitions.timelines
    }
}
