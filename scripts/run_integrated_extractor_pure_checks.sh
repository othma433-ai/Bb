#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
kotlinc \
  app/src/main/java/com/althmany/extractor/data/Models.kt \
  app/src/main/java/com/althmany/extractor/engine/ExtractionPolicy.kt \
  app/src/main/java/com/althmany/extractor/engine/LinkExtractor.kt \
  app/src/main/java/com/althmany/extractor/engine/NodeSnapshot.kt \
  app/src/main/java/com/althmany/extractor/engine/EndProofTracker.kt \
  app/src/main/java/com/althmany/extractor/profile/ProfileLaunchPolicy.kt \
  scripts/ExtractorPureEngineChecks.kt -include-runtime -d "$TMP/engine.jar"
java -jar "$TMP/engine.jar"
kotlinc \
  app/src/main/java/com/althmany/extractor/data/ScanModels.kt \
  app/src/main/java/com/althmany/extractor/engine/InviteLinkParser.kt \
  app/src/main/java/com/althmany/extractor/engine/InviteScanClassifier.kt \
  app/src/main/java/com/althmany/extractor/engine/ScanRetryPolicy.kt \
  app/src/main/java/com/althmany/extractor/engine/ScanUiState.kt \
  scripts/ExtractorPureScanChecks.kt -include-runtime -d "$TMP/scan.jar"
java -jar "$TMP/scan.jar"
kotlinc \
  app/src/main/java/com/althmany/extractor/data/PublishModels.kt \
  app/src/main/java/com/althmany/extractor/engine/PublishUiState.kt \
  scripts/ExtractorPurePublishChecks.kt -include-runtime -d "$TMP/publish.jar"
java -jar "$TMP/publish.jar"
kotlinc app/src/main/java/com/althmany/extractor/engine/RuntimeOperationCoordinator.kt scripts/ExtractorPureRuntimeChecks.kt -include-runtime -d "$TMP/runtime.jar"
java -jar "$TMP/runtime.jar"
kotlinc \
  app/src/main/java/com/althmany/extractor/profile/ProfileLaunchPolicy.kt \
  app/src/main/java/com/althmany/extractor/profile/ProfileControlPolicy.kt \
  app/src/main/java/com/althmany/extractor/profile/DualMessengerMatcher.kt \
  scripts/ExtractorPureProfileChecks.kt -include-runtime -d "$TMP/profile.jar"
java -jar "$TMP/profile.jar"
echo 'INTEGRATED EXTRACTOR PURE CHECKS: PASS'
