package io.holixon.emn.generation

import _ktx.ResourceKtx.resourceUrl
import io.holixon.emn.generation.TestFixtures.EMN_PARSER
import org.junit.jupiter.api.Test

class EmnDeclarationTest {

  private val definitions = EMN_PARSER.parseDefinitions(resourceUrl("manual/faculty-manual.emn"))

  @Test
  fun `read declaration`() {
    val declaration = EmnDeclaration(definitions)

    println(declaration.commandsBySchemaReference)
    println(declaration.eventsBySchemaReference)

  }
}
