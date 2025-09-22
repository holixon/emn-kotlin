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
    val file = TestFixtures.resourceUrl("faculty/faculty.emn")

    // when
    val definitions = parser.parseDefinitions(file)

    // then
    val specifications: List<Specification> = definitions.specifications
    assertThat(specifications).isNotEmpty
    assertThat(specifications).hasSize(5)

    // verify IDs and names from faculty.emn resource
    assertThat(specifications)
      .extracting("id", "name")
      .containsExactlyInAnyOrder(
//        tuple("spec_1", "Student subscription for course which is not full"),
//        tuple("spec_2", "Student subscription for course which is full"),
        tuple("Specification_create_course_success", "Create course"),
        tuple("Specification_create_course_error", "Don't create course"),
        tuple("Specification_1gtvjcl", "Rename course"),
        tuple("Specification_1ote18y", "Rename course to the same name"),
        tuple("Specification_0wjemf5", "Rename non-existing course")
      )
  }
}
