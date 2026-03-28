#!/bin/bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$REPO_ROOT/.gradle-test-home}"

cd "$REPO_ROOT"

bash integration-tests/scripts/start-stack.sh
./gradlew :integration-tests:test
