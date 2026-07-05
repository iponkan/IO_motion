#!/usr/bin/env bash
# Downloads the three MediaPipe Pose Landmarker model variants into
# core-pose/src/main/assets/models/.
# Run once from the project root before building: bash scripts/download_models.sh
#
# The models/ subdirectory is required, not cosmetic: MediaPipe's Android asset resolver
# rejects a bare filename with no '/' in it (see PoseModelVariant's kdoc).
set -euo pipefail

ASSETS_DIR="core-pose/src/main/assets/models"
mkdir -p "$ASSETS_DIR"

BASE="https://storage.googleapis.com/mediapipe-models/pose_landmarker"

download() {
    local name="$1"
    local dest="${ASSETS_DIR}/pose_landmarker_${name}.task"
    if [[ -f "$dest" ]]; then
        echo "Already present: $dest"
        return
    fi
    echo "Downloading ${name}..."
    curl -fL \
        "${BASE}/pose_landmarker_${name}/float16/latest/pose_landmarker_${name}.task" \
        -o "$dest"
    echo "Saved: $dest"
}

download "lite"
download "full"
download "heavy"

echo "All models ready in ${ASSETS_DIR}/"
