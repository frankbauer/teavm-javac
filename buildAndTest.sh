#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

VERSION_CODE="${1:-${VERSION_CODE:-102}}"

BUILD_SCRIPT="$ROOT_DIR/scripts/build-v${VERSION_CODE}.sh"
TEST_FILE="$ROOT_DIR/test/v${VERSION_CODE}-worker.test.mjs"

if [[ ! -x "$BUILD_SCRIPT" ]]; then
    if [[ -f "$BUILD_SCRIPT" ]]; then
        chmod +x "$BUILD_SCRIPT"
    else
        echo "Build script not found: $BUILD_SCRIPT"
        exit 1
    fi
fi

if [[ ! -f "$TEST_FILE" ]]; then
    echo "Test file not found: $TEST_FILE"
    exit 1
fi

echo "Running build for v${VERSION_CODE}..."
"$BUILD_SCRIPT"

echo "Running tests for v${VERSION_CODE}..."
node --test "$TEST_FILE"

echo "buildAndTest completed for v${VERSION_CODE}."
