package io.holixon.emn

import java.io.File
import java.net.URL
import java.nio.file.Path
import kotlin.io.path.Path

object TestFixtures {
  private val CLASS_LOADER = TestFixtures::class.java.classLoader

  @Throws(IllegalArgumentException::class)
  fun resourceUrl(
    resource: String,
    path: String? = null
  ): URL = resourceUrl(resourcePath = Path(path ?: "").resolve(resource), classLoader = CLASS_LOADER)

  fun resourceUrl(resourcePath: Path, classLoader: ClassLoader = CLASS_LOADER): URL =
    with(resourcePath.toString().removePrefix(File.separator)) {
      requireNotNull(classLoader.getResource(this)) { "Resource not found: $this" }
    }
}
