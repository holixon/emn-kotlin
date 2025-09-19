package io.holixon.emn

import io.holixon.emn.model.Specification
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.api.Test
import java.io.File

class SpecificationParserTest {

  private val parser = EmnDocumentParser()

  @Test
  fun `parses faculty emn and finds specification elements`() {
    // given
    val file = File("src/test/resources/faculty.emn")

    // when
    val definitions = parser.parseDefinitions(file)

    // then
    val specifications: List<Specification> = definitions.specifications
    assertThat(specifications).isNotEmpty
    assertThat(specifications).hasSize(2)

    // verify IDs and names from faculty.emn resource
    assertThat(specifications)
      .extracting("id", "name")
      .containsExactlyInAnyOrder(
        tuple("spec_1", "Student subscription for course which is not full"),
        tuple("spec_2", "Student subscription for course which is full")
      )
  }
}
