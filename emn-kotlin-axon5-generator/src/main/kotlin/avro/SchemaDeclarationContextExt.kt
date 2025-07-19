package io.holixon.emn.generation.avro

import io.holixon.emn.generation.spi.EmnGenerationContext
import io.toolisticon.kotlin.avro.generator.spi.SchemaDeclarationContext
import io.toolisticon.kotlin.avro.model.RecordType

object SchemaDeclarationContextExt {

  val SchemaDeclarationContext.emnContext: EmnGenerationContext get() = this.tag(EmnGenerationContext::class)!!

  fun SchemaDeclarationContext.isEntityId(type: Any): Boolean = when (type) {
    // TODO check emnContext for entities
    is RecordType -> type.name.value.endsWith("Id")
    else -> false
  }

  fun SchemaDeclarationContext.entityName(type: RecordType): String {
    require(this.isEntityId(type)) { "Entity ID name is not a valid entity ID" }
    // TODO derive from context
    val name = type.name.value.removeSuffix("Id")
    return name;
  }


}
