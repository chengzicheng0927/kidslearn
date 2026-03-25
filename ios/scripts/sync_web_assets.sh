#!/bin/sh
set -eu

PATH="/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin:$PATH"

PROJECT_DIR_PATH="${PROJECT_DIR:-$(cd "$(dirname "$0")/.." && pwd)}"
REPO_ROOT="$(cd "$PROJECT_DIR_PATH/.." && pwd)"
WEB_DIR="$REPO_ROOT/kl-ui"
DIST_DIR="$WEB_DIR/dist"
TARGET_WEB_DIR="${TARGET_BUILD_DIR:-}/$(printf %s "${UNLOCALIZED_RESOURCES_FOLDER_PATH:-}")/web"
CONFIGURATION_NAME="${CONFIGURATION:-Debug}"

if [ ! -d "$WEB_DIR" ]; then
  echo "error: missing web app directory: $WEB_DIR"
  exit 1
fi

if [ -z "${TARGET_BUILD_DIR:-}" ] || [ -z "${UNLOCALIZED_RESOURCES_FOLDER_PATH:-}" ]; then
  echo "error: Xcode build environment is incomplete, cannot determine bundle output path."
  exit 1
fi

if [ "$CONFIGURATION_NAME" = "Debug" ] && [ "${KIDSLEARN_EMBED_WEB_IN_DEBUG:-0}" != "1" ]; then
  echo "Debug模式，跳过打包（使用localhost）。如需打包设 KIDSLEARN_EMBED_WEB_IN_DEBUG=1"
  # exit 0  # 临时注释掉，方便 Debug 模式打包 web 资源
fi

if ! command -v npm >/dev/null 2>&1; then
  echo "error: npm is not available in PATH. Please install Node.js or expose npm to Xcode."
  exit 1
fi

echo "Building web assets for $CONFIGURATION_NAME..."
cd "$WEB_DIR"
npm run build

echo "Syncing $DIST_DIR -> $TARGET_WEB_DIR"
rm -rf "$TARGET_WEB_DIR"
mkdir -p "$TARGET_WEB_DIR"
ditto "$DIST_DIR" "$TARGET_WEB_DIR"

if [ ! -f "$TARGET_WEB_DIR/index.html" ]; then
  echo "error: index.html was not copied into the app bundle."
  exit 1
fi

echo "Web assets synced successfully."
