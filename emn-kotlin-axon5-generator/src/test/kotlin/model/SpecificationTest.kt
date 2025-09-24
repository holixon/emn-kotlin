package io.holixon.emn.generation.model

import io.holixon.emn.model.GivenStage
import io.holixon.emn.model.Specification
import io.holixon.emn.model.ThenStage
import io.holixon.emn.model.WhenStage
import org.junit.jupiter.api.Test


class SpecificationTest {

  @Test
  fun `create from valid`() {
    val spec = Specification(
      id = "1",
      name = "Spec 1",
      givenStage = GivenStage(id="g1", values = emptyList()),
      whenStage = WhenStage(id="w1", values = emptyList()),
      thenStage = ThenStage(id="t1", values = emptyList()),
    )

    //val emnSpec = Specification(spec)

    //println(validateSpecification(spec).isValid)
  }
}
