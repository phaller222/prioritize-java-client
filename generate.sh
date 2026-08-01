#!/usr/bin/env bash
#
# Regenerates the Prioritize Java client from the OpenAPI spec.
#
# Usage:
#   ./generate.sh                       # regenerate from the committed openapi/openapi.json
#   ./generate.sh path/to/openapi.json  # first refresh the spec from a released docs/openapi.json,
#                                        # then regenerate
#
# The client is spec-first: sources are generated into target/generated-sources/openapi by the
# openapi-generator-maven-plugin and are never hand-edited.
#
set -euo pipefail
cd "$(dirname "$0")"

SPEC="openapi/openapi.json"

if [ "${1:-}" != "" ]; then
    SRC="$1"
    echo "Refreshing $SPEC from $SRC (pinning the OpenAPI version to 3.0.1)..."
    # Prioritize emits OpenAPI 3.1.0, which OpenAPI Generator's resolver cannot handle even though the
    # schemas use no 3.1-only constructs. Pinning the header to 3.0.1 is loss-less for this contract.
    # (Remove this step once the app emits 3.0.x via springdoc.api-docs.version: openapi_3_0.)
    node -e "const fs=require('fs');const d=JSON.parse(fs.readFileSync(process.argv[1],'utf8'));d.openapi='3.0.1';fs.writeFileSync('$SPEC',JSON.stringify(d,null,2)+'\n');" "$SRC"
    echo "Updated $SPEC. Remember to bump <version> in pom.xml to match the API release."
fi

echo "Generating sources..."
mvn -q clean generate-sources
echo "Done. Generated sources are under target/generated-sources/openapi."
