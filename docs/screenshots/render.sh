#!/usr/bin/env bash
# Vyfoti HTML predlohu headless Chromem. $1 = html, $2 = png, $3 = sirka, $4 = vyska
set -euo pipefail
CHROME="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
"$CHROME" --headless --disable-gpu --hide-scrollbars \
  --force-device-scale-factor=2 \
  --window-size="$3,$4" \
  --screenshot="$2" \
  --allow-file-access-from-files \
  "file://$(cd "$(dirname "$1")" && pwd)/$(basename "$1")" >/dev/null 2>&1
echo "$2 hotovo"
