package io.holixon.emn.generation.avro

import io.holixon.emn.generation.spi.EmnGenerationContext
import io.toolisticon.kotlin.avro.generator.spi.ProtocolDeclarationContext
import io.toolisticon.kotlin.avro.model.AvroNamedType
import io.toolisticon.kotlin.avro.value.CanonicalName

object ProtocolDeclarationContextExt {

  val ProtocolDeclarationContext.emnContext: EmnGenerationContext get() = this.tag(EmnGenerationContext::class)!!

}
