package io.holixon.emn.generation.schema

class EmnSchemaPackageLayout {

  fun packageName(rootPackageName: String, typeKind: EmnSchemaTypeKind): String {
    val root = KotlinIdentifier.requirePackageName(rootPackageName)
    return KotlinIdentifier.requirePackageName("$root.${packageSuffix(typeKind)}")
  }

  fun packageSuffix(typeKind: EmnSchemaTypeKind): String = when (typeKind) {
    EmnSchemaTypeKind.COMMAND -> "command"
    EmnSchemaTypeKind.EVENT -> "event"
    EmnSchemaTypeKind.QUERY -> "query"
    EmnSchemaTypeKind.SCHEMA_DEFINITION -> "common"
  }
}
