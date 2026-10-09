package io.holixon.emn.verify.annotation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EmnSpecificationTest {

  @Test
  fun `is runtime visible without depending on a test framework`() {
    val annotation = SpecificationFixture::class.java
      .getDeclaredMethod("implementsSpecification")
      .getAnnotation(EmnSpecification::class.java)

    assertThat(annotation.id).isEqualTo("specification-id")
  }
}

private class SpecificationFixture {
  @EmnSpecification("specification-id")
  fun implementsSpecification() = Unit
}
