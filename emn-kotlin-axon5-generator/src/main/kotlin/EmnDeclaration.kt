package io.holixon.emn.generation

import io.holixon.emn.model.*
import io.toolisticon.kotlin.avro.value.CanonicalName

class EmnDeclaration(
  val definitions: Definitions
) {

  val avroReferenceTypes: Map<CanonicalName, FlowNodeType> by lazy {
    definitions.nodeTypes.filter { it.hasAvroTypeDefinitionRef() }
      .associateBy { CanonicalName.parse(it.schemaReference()) }
  }

  val commandsBySchemaReference: Map<CanonicalName, CommandType> by lazy {
    avroReferenceTypes.filter { it.value is CommandType }
      .mapValues {
        it.value as CommandType
      }
  }

  val eventsBySchemaReference: Map<CanonicalName, EventType> by lazy {
    avroReferenceTypes.filter { it.value is EventType }
      .mapValues {
        it.value as EventType
      }
  }
}
