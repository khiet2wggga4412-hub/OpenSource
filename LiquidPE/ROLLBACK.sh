#!/usr/bin/env sh
set -eu
SCRIPT_PATH=$0
case "$SCRIPT_PATH" in
  *\\*) SCRIPT_PATH=$(cygpath -u "$SCRIPT_PATH") ;;
esac
ROOT=${SCRIPT_PATH%/*}
if [ "$ROOT" = "$SCRIPT_PATH" ]; then ROOT=.; fi
ROOT=$(CDPATH= cd -- "$ROOT" && pwd)

METRICS="$ROOT/app/src/main/java/com/liquid/org/ui/overlay/LiquidBounceUiMetrics.java"
RENDERER="$ROOT/app/src/main/java/com/liquid/org/ui/overlay/ClickGuiRenderer.java"
SIGNING="$ROOT/signing/keystore.properties"

for TARGET in "$METRICS" "$RENDERER"; do
  if [ ! -f "$TARGET" ]; then printf '%s\n' "missing $TARGET" >&2; exit 1; fi
done

sed -i.bak \
  -e 's/TOP_TABS_X = 1210f/TOP_TABS_X = 1320f/' \
  -e 's/TOP_TABS_WIDTH = 520f/TOP_TABS_WIDTH = 300f/' \
  -e 's/TOP_TABS_HEIGHT = 64f/TOP_TABS_HEIGHT = 42f/' \
  -e 's/TOP_TABS_RADIUS = 32f/TOP_TABS_RADIUS = 21f/' \
  -e 's/SEARCH_X = 900f/SEARCH_X = 1020f/' \
  -e 's/SEARCH_Y = 92f/SEARCH_Y = 104f/' \
  -e 's/SEARCH_WIDTH = 1140f/SEARCH_WIDTH = 900f/' \
  -e 's/SEARCH_HEIGHT = 88f/SEARCH_HEIGHT = 75f/' \
  -e 's/SEARCH_RADIUS = 44f/SEARCH_RADIUS = 37f/' \
  "$METRICS"
rm -f "$METRICS.bak"

sed -i.bak \
  -e '/float tabWidth = LiquidBounceUiMetrics.TOP_TABS_WIDTH \/ labels.length;/d' \
  -e 's/float\[\] widths = {tabWidth, tabWidth, tabWidth, tabWidth};/float[] widths = {75, 75, 75, 75};/' \
  -e '/float indicatorRadius = (LiquidBounceUiMetrics.TOP_TABS_HEIGHT - 6f) \* .5f;/d' \
  -e 's/canvas.drawRoundRect(rect, indicatorRadius, indicatorRadius, fill);/canvas.drawRoundRect(rect, 18, 18, fill);/' \
  -e 's/canvas.drawRoundRect(rect, indicatorRadius, indicatorRadius, stroke);/canvas.drawRoundRect(rect, 18, 18, stroke);/' \
  -e 's/, 20, active ? LiquidBounceFonts.bold() : LiquidBounceFonts.medium())/, 14, active ? LiquidBounceFonts.bold() : LiquidBounceFonts.medium())/' \
  -e 's/, 30, LiquidBounceFonts.regular())/, 24, LiquidBounceFonts.regular())/' \
  -e 's/SEARCH_X + 46/SEARCH_X + 38/' \
  -e 's/SEARCH_WIDTH - 34/SEARCH_WIDTH - 25/' \
  -e '/float tabWidth = LiquidBounceUiMetrics.TOP_TABS_WIDTH \/ TopTab.values().length;/d' \
  -e 's/int tabIndex = Math.max(0, Math.min(TopTab.values().length - 1, (int) (local \/ tabWidth)));/int tabIndex = Math.max(0, Math.min(3, (int) (local \/ 75f)));/' \
  "$RENDERER"
rm -f "$RENDERER.bak"

if [ -f "$SIGNING" ]; then
  sed -i.bak 's#^storeFile=.*#storeFile=signing/liquidpe-release.jks#' "$SIGNING"
  rm -f "$SIGNING.bak"
fi

printf '%s\n' 'rollback restored top tabs to 300x42 and search to 900x75'
