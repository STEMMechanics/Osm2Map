#!/usr/bin/env bash
set -euo pipefail

version="${1:?Usage: release.sh VERSION JAR}"
jar="${2:?Usage: release.sh VERSION JAR}"
: "${GITHUB_REPOSITORY:?GITHUB_REPOSITORY is required}"
: "${GITHUB_SHA:?GITHUB_SHA is required}"

if [[ ! -f "$jar" ]]; then
    echo "Plugin JAR not found: $jar" >&2
    exit 1
fi

tag="v$version"
title="Osm2Map ${version/-build./ build }"

if is_draft=$(gh release view "$tag" --repo "$GITHUB_REPOSITORY" --json isDraft --jq '.isDraft' 2>/dev/null); then
    if [[ "$is_draft" == "false" ]]; then
        echo "Release $tag is already published."
        exit 0
    fi

    # Complete a previous attempt that stopped before publishing its draft.
    gh release upload "$tag" "$jar" --repo "$GITHUB_REPOSITORY" --clobber
    gh release edit "$tag" --repo "$GITHUB_REPOSITORY" --draft=false --latest
else
    gh release create "$tag" "$jar" \
        --repo "$GITHUB_REPOSITORY" \
        --target "$GITHUB_SHA" \
        --title "$title" \
        --generate-notes \
        --latest
fi
