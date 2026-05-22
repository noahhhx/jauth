#!/usr/bin/env bash
set -euo pipefail

VERSION="${1:?Usage: ./release.sh <version> (e.g. ./release.sh 0.1.0)}"

if ! echo "$VERSION" | grep -qE '^[0-9]+\.[0-9]+\.[0-9]+$'; then
  echo "ERROR: version must be semver (e.g. 1.0.0)" >&2
  exit 1
fi

if [[ -n $(git status --porcelain) ]]; then
  echo "ERROR: working tree is dirty. Commit or stash changes first." >&2
  exit 1
fi

NEXT_VERSION=$(echo "$VERSION" | awk -F. '{printf "%d.%d.%d", $1, $2, $3+1}')-SNAPSHOT

mvn versions:set -DnewVersion="$VERSION"
rm -f pom.xml.versionsBackup
git add pom.xml
git commit -m "release $VERSION"
git tag "v$VERSION"

mvn versions:set -DnewVersion="$NEXT_VERSION"
rm -f pom.xml.versionsBackup
git add pom.xml
git commit -m "back to dev $NEXT_VERSION"

git push origin main "v$VERSION"

gh release create "v$VERSION" --title "v$VERSION" --generate-notes

echo "Release v$VERSION pushed. Watch the build at:"
echo "  gh run list --workflow release.yml"
