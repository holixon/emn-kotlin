package io.holixon.emn.generation.kcg

import io.holixon.emn.generation.kcg.name.transformUpperCamelcase
import io.holixon.emn.generation.kcg.name.transformUpperSnakeCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource


class NameTest {

  @ParameterizedTest
  @CsvSource(
    value = [
      "foo, Foo",
      "fooBar, FooBar",
      "FOO, Foo",
    ]
  )
  fun transformUpperCamelcase(input: String, expected: String) {
    assertThat(input.transformUpperCamelcase()).isEqualTo(expected)
  }


  @ParameterizedTest
  @CsvSource(
    value = [
      "foo, FOO",
      "fooBar, FOO_BAR",
      "fooBarHelloWorld, FOO_BAR_HELLO_WORLD",
      "FOO, FOO",
    ]
  )
  fun transformUpperSnakeCase(input: String, expected: String) {
    assertThat(input.transformUpperSnakeCase()).isEqualTo(expected)
  }


}
