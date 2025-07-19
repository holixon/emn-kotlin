package io.holixon.emn.generation.avro

import io.holixon.emn.generation.spi.EmnGenerationContext
import io.toolisticon.kotlin.avro.generator.spi.ProtocolDeclarationContext

object ProtocolDeclarationContextExt {

  val ProtocolDeclarationContext.emnContext: EmnGenerationContext get() = this.tag(EmnGenerationContext::class)!!

}
