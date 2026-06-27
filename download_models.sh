#!/usr/bin/env bash
# download_models.sh — fetch MediaPipe Pose Landmarker model files
#
# Run once from the project root before building:
#   chmod +x download_models.sh && ./download_models.sh
#
# Models are downloaded into core-pose/src/main/assets/ which is where
# the app loads them at runtime via PoseLandmarkerHelper.
#
# Re-running this script is safe — already-downloaded files are skipped.

set -euo pipefail

BASE_URL="https://storage.googleapis.com/mediapipe-models/pose_landmarker"
DEST="core-pose/src/main/assets"

mkdir -p "$DEST"

download_model() {
    local variant="$1"
    local filename="pose_landmarker_${variant}.task"
    local url="${BASE_URL}/pose_landmarker_${variant}/float16/latest/${filename}"

    if [ -f "${DEST}/${filename}" ]; then
        echo "[skip] ${filename} already present"
        return
    fi

    echo "[download] ${filename} ..."
    curl --fail --silent --show-error --location "$url" --output "${DEST}/${filename}"
    echo "[done]     ${filename} ($(du -sh "${DEST}/${filename}" | cut -f1))"
}

download_model "lite"
download_model "full"
download_model "heavy"

echo ""
echo "All models available in ${DEST}/"
ls -lh "${DEST}/"*.task
