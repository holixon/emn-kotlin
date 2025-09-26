package io.holixon.emn.example.faculty

import org.axonframework.test.fixture.AxonTestFixture
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver

/**
 * Fixture JUnit parameter resolver.
 */
class AxonTestFixtureParameterResolver : ParameterResolver {
  override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext) = parameterContext.parameter.type == AxonTestFixture::class.java
  override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext?) = AxonTestFixture.with(UniversityAxonGeneratedApplication().configurer())!!
}
