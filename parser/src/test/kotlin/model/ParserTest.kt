package io.holixon.emn.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File

class ParserTest {

  private val parser = EmnDocumentParser()

  @Test
  fun parses_emn() {

    val file = File("src/test/resources/guest-register.emn")
    val result = parser.parseDefinitions(file)
    assertThat(result).isNotNull
    assertThat(result.typeDefinitions).isNotEmpty

    for (type in result.nodes) {
      println("Type: ${type.id}, incoming: ${type.incoming.map { it.id }}, outgoing: ${type.outgoing.map { it.id }}")
      if (type.schema != null) {
        println(" Schema (type=${type.schema!!.schemaFormat}): \n${type.schema!!.printable()}")
      }
    }

    for (flow in result.flows) {
      println("Flow: ${flow.id}, source: ${flow.source.id}, target: ${flow.target.id}")
    }

  }
}
