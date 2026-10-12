package io.holixon.emn.maven

import io.toolisticon.maven.mojo.AbstractContextAwareMojo
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.shared.model.fileset.FileSet
import org.apache.maven.shared.model.fileset.util.FileSetManager
import java.io.File

abstract class AbstractEmnMojo : AbstractContextAwareMojo() {

  @Parameter(
    property = "emn.resourceDirectory",
    required = true,
    defaultValue = DEFAULT_RESOURCE_DIRECTORY,
  )
  protected lateinit var resourceDirectory: File

  @Parameter(property = "emn.includes")
  protected var includes: Array<String> = DEFAULT_INCLUDES

  protected fun findEmnFiles(failWhenEmpty: Boolean = false): List<File> {
    if (!resourceDirectory.isDirectory) {
      if (failWhenEmpty) throw MojoFailureException("EMN resource directory does not exist: $resourceDirectory")
      log.warn("Skip non existing EMN resource directory $resourceDirectory.")
      return emptyList()
    }
    val includePatterns = includes
    val fileSet = FileSet().apply {
      directory = resourceDirectory.absolutePath
      isFollowSymlinks = false
      includePatterns.forEach(::addInclude)
    }
    val files = FileSetManager().getIncludedFiles(fileSet)
      .filterNotNull()
      .sorted()
      .map { File(resourceDirectory, it) }
    if (failWhenEmpty && files.isEmpty()) {
      throw MojoFailureException("No EMN files found in $resourceDirectory for includes ${includes.contentToString()}.")
    }
    return files
  }

  companion object {
    const val DEFAULT_RESOURCE_DIRECTORY = $$"${project.basedir}/src/main/resources"
    val DEFAULT_INCLUDES = arrayOf("**/*.emn")
  }
}
