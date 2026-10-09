package io.holixon.emn.generation.schema.maven

import org.apache.maven.shared.model.fileset.FileSet
import org.apache.maven.shared.model.fileset.util.FileSetManager

internal object EmnSchemaExporterMavenPlugin {
  const val DEFAULT_RESOURCES_DIRECTORY = $$"${project.basedir}/src/main/resources"
  val DEFAULT_INCLUDES = arrayOf("**/*.emn")

  fun findIncludedFiles(absPath: String, includes: Array<String>): List<String> {
    val includePatterns = includes
    val fileSet = FileSet().apply {
      directory = absPath
      isFollowSymlinks = false
      includePatterns.forEach(::addInclude)
    }
    return FileSetManager().getIncludedFiles(fileSet).filterNotNull()
  }
}
