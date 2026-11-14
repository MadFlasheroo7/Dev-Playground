#!/usr/bin/env bash
# ==============================================================================
# Turnkey Standalone Exporter for :shader-engine
# Detaches the shader-engine Kotlin Multiplatform library into a self-contained
# repository ready for independent GitHub publishing or submodule distribution.
# ==============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
DEST_DIR="${1:-${ROOT_DIR}/../shader-engine-dist}"

echo "=========================================================="
echo "  Exporting :shader-engine to: ${DEST_DIR}"
echo "=========================================================="

mkdir -p "${DEST_DIR}"

# 1. Copy source code and build definitions
echo "-> Copying sources and library structure..."
rm -rf "${DEST_DIR}/src"
cp -r "${ROOT_DIR}/shader-engine/src" "${DEST_DIR}/"
cp "${ROOT_DIR}/shader-engine/build.gradle.kts" "${DEST_DIR}/build.gradle.kts"
cp "${ROOT_DIR}/shader-engine/standalone-settings.gradle.kts" "${DEST_DIR}/settings.gradle.kts"
cp "${ROOT_DIR}/shader-engine/README.md" "${DEST_DIR}/README.md"

# 2. Copy Gradle wrapper and version catalog
echo "-> Copying Gradle wrapper and catalog..."
mkdir -p "${DEST_DIR}/gradle"
cp -r "${ROOT_DIR}/gradle/wrapper" "${DEST_DIR}/gradle/"
cp "${ROOT_DIR}/gradle/libs.versions.toml" "${DEST_DIR}/gradle/"
cp "${ROOT_DIR}/gradlew" "${DEST_DIR}/"
cp "${ROOT_DIR}/gradlew.bat" "${DEST_DIR}/"
chmod +x "${DEST_DIR}/gradlew"

# 3. Copy local.properties for Android SDK location if available
if [ -f "${ROOT_DIR}/local.properties" ]; then
    echo "-> Copying local.properties for Android SDK detection..."
    cp "${ROOT_DIR}/local.properties" "${DEST_DIR}/local.properties"
fi

# 4. Create .gitignore
echo "-> Generating .gitignore..."
cat << 'EOF' > "${DEST_DIR}/.gitignore"
.gradle
build/
.idea/
*.iml
.DS_Store
*.hprof
local.properties
EOF

# 4. Initialize Git repository
if [ ! -d "${DEST_DIR}/.git" ]; then
    echo "-> Initializing fresh Git repository..."
    (
        cd "${DEST_DIR}"
        git init
        git add .
        git commit -m "Initial release: Standalone Compose Multiplatform Shader Engine" || true
    )
fi

echo "=========================================================="
echo "  Verification: Testing standalone assemble..."
echo "=========================================================="
(
    cd "${DEST_DIR}"
    ./gradlew assemble
)

echo "=========================================================="
echo "  SUCCESS: Standalone shader-engine exported cleanly to:"
echo "  ${DEST_DIR}"
echo "=========================================================="
