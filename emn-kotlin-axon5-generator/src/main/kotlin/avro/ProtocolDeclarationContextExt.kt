package io.holixon.emn.generation.avro

import io.holixon.emn.generation.spi.EmnGenerationContext
import io.toolisticon.kotlin.avro.generator.spi.ProtocolDeclarationContext
import io.toolisticon.kotlin.avro.model.AvroNamedType

object ProtocolDeclarationContextExt {

  val ProtocolDeclarationContext.emnContext: EmnGenerationContext get() = this.tag(EmnGenerationContext::class)!!

  val ProtocolDeclarationContext.allDeclaredTypes: List<AvroNamedType> get() = this.avroTypes.values
    .filterIsInstance<AvroNamedType>()

}
