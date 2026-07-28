#!/bin/sh
set -eu

BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROPERTIES="$BASE_DIR/.mvn/wrapper/maven-wrapper.properties"

if command -v mvn >/dev/null 2>&1; then
    exec mvn "$@"
fi

if [ ! -f "$PROPERTIES" ]; then
    echo "Missing $PROPERTIES" >&2
    exit 1
fi

DISTRIBUTION_URL=$(sed -n 's/^distributionUrl=//p' "$PROPERTIES")
MAVEN_VERSION=$(printf '%s' "$DISTRIBUTION_URL" | sed -n 's#.*apache-maven/\([^/]*\)/.*#\1#p')
MAVEN_HOME="${HOME}/.m2/wrapper/dists/apache-maven-${MAVEN_VERSION}"
MAVEN_BIN="$MAVEN_HOME/bin/mvn"

if [ ! -x "$MAVEN_BIN" ]; then
    ARCHIVE="${TMPDIR:-/tmp}/apache-maven-${MAVEN_VERSION}-bin.zip"
    STAGING="${TMPDIR:-/tmp}/apache-maven-${MAVEN_VERSION}-wrapper"
    rm -rf "$STAGING"
    mkdir -p "$STAGING" "$(dirname "$MAVEN_HOME")"

    echo "Maven is not installed; downloading Maven ${MAVEN_VERSION}..." >&2
    if command -v curl >/dev/null 2>&1; then
        curl -fsSL "$DISTRIBUTION_URL" -o "$ARCHIVE"
    elif command -v wget >/dev/null 2>&1; then
        wget -q "$DISTRIBUTION_URL" -O "$ARCHIVE"
    else
        echo "Install Maven, curl, or wget, then run this command again." >&2
        exit 1
    fi

    if command -v unzip >/dev/null 2>&1; then
        unzip -q "$ARCHIVE" -d "$STAGING"
    else
        echo "The Maven wrapper needs 'unzip'. Install it and run again." >&2
        exit 1
    fi

    rm -rf "$MAVEN_HOME"
    mv "$STAGING/apache-maven-${MAVEN_VERSION}" "$MAVEN_HOME"
    rm -rf "$STAGING" "$ARCHIVE"
fi

exec "$MAVEN_BIN" "$@"
