#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TMP="${TMPDIR:-/tmp}/althmany-stability-4.0-$$"
LEGACY_TIMEOUT_SECONDS="${LEGACY_TIMEOUT_SECONDS:-300}"
mkdir -p "$TMP"
trap 'rm -rf "$TMP"' EXIT
cd "$ROOT"

phase() { printf '\n=== %s ===\n' "$1"; }

phase "SOURCE VALIDATION"
python3 scripts/validate_source.py

phase "INTEGRATED SOURCE CONTRACT"
python3 scripts/validate_integrated_extractor.py

phase "INTEGRATED EXTRACTOR PURE CHECKS"
bash scripts/run_integrated_extractor_pure_checks.sh

phase "4.0 FOCUSED PURE CHECKS"
kotlinc \
  app/src/main/java/com/althmany/extractor/engine/RuntimeOperationCoordinator.kt \
  app/src/main/java/com/althmany/extractor/engine/ConversationTitlePolicy.kt \
  scripts/StabilityOwnershipChecks.kt \
  scripts/ConversationTitlePolicyChecks.kt \
  scripts/SenderGuardPureChecks.kt \
  -include-runtime -d "$TMP/stability-focused.jar"
java -cp "$TMP/stability-focused.jar" StabilityOwnershipChecksKt
java -cp "$TMP/stability-focused.jar" ConversationTitlePolicyChecksKt
java -cp "$TMP/stability-focused.jar" SenderGuardPureChecksKt

phase "ANDROID XML PARSE"
python3 - <<'PY'
from pathlib import Path
import xml.etree.ElementTree as ET
root=Path('app/src/main')
files=list(root.rglob('*.xml'))
errors=[]
for p in files:
    try: ET.parse(p)
    except Exception as exc: errors.append((p, exc))
if errors:
    for p,e in errors: print(f'FAIL: {p}: {e}')
    raise SystemExit(1)
print(f'XML_PARSE_PASS: {len(files)} files')
PY

phase "LEGACY PURE KOTLIN REGRESSION"
if [[ "${SKIP_LEGACY:-0}" == "1" ]]; then
  echo "LEGACY_REGRESSION_SKIPPED: run bash scripts/run_pure_kotlin_regressions.sh separately"
else
  set +e
  timeout "${LEGACY_TIMEOUT_SECONDS}s" bash scripts/run_pure_kotlin_regressions.sh
  legacy_status=$?
  set -e
  if [[ $legacy_status -eq 124 ]]; then
    echo "LEGACY_REGRESSION_TIMEOUT after ${LEGACY_TIMEOUT_SECONDS}s"
    exit 124
  fi
  if [[ $legacy_status -ne 0 ]]; then
    echo "LEGACY_REGRESSION_FAIL status=${legacy_status}"
    exit "$legacy_status"
  fi
fi

echo
echo "AL-THMANY 4.0 STABILITY CHECKS: PASS"
