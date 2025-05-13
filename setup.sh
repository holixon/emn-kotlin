#!/usr/bin/env bash

GITHUB_ORGANIZATION=toolisticon
GITHUB_REPOSITORY=kotlin-lib
BASE_GROUP_ID=io.toolisticon.template
BASE_DESCRIPTION=$GITHUB_REPOSITORY
BASE_ARTIFACT_ID=$GITHUB_REPOSITORY

case "$OSTYPE" in
  darwin*)  # support mac with special SED
    BIN="sed -i ''"
    ;;
  *)        # use standard SED
    BIN="sed -i"
    ;;
esac

for FILE in `find . \( -name "pom.xml" -o -name "*.kt" -o -name "*.md" \) -type f`
do
  $BIN "s/GITHUB_ORGANIZATION/${GITHUB_ORGANIZATION}/g" ""$FILE""
  $BIN "s/GITHUB_REPOSITORY/${GITHUB_REPOSITORY}/g" "$FILE"
  $BIN "s/BASE_GROUP_ID/${BASE_GROUP_ID}/g" "$FILE"
  $BIN "s/BASE_ARTIFACT_ID/${BASE_ARTIFACT_ID}/g" "$FILE"
  $BIN "s/BASE_DESCRIPTION/${BASE_DESCRIPTION}/g" "$FILE"
done
