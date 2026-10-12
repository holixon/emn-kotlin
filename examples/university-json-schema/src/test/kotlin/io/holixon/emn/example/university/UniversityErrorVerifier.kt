package io.holixon.emn.example.university

import org.junit.jupiter.api.Assertions.assertEquals

object UniversityErrorVerifier {
  fun verify(errorTypeId: String, expectedDetails: Map<String, Any?>, exception: Throwable) {
    assertEquals(errorTypeId, exception::class.simpleName)
    expectedDetails["message"]?.let { expectedMessage ->
      assertEquals(expectedMessage, exception.message)
    }
  }
}
