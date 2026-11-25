#!/usr/bin/env bash
# 文字去重 APK 构建脚本（无需 Gradle）
set -euo pipefail

P="$(cd "$(dirname "$0")" && pwd)"
SDK="${ANDROID_SDK_ROOT:-/root/workspace/sdk}"
BT="${BUILD_TOOLS_DIR:-$SDK/bt/android-14}"
PLAT="${ANDROID_JAR:-$SDK/platform/android-34/android.jar}"
AAPT2="${AAPT2:-/usr/bin/aapt2}"
ZIPALIGN="${ZIPALIGN:-/usr/bin/zipalign}"
OUT="$P/build"
KS="$P/signing.keystore"
ALIAS="${KEY_ALIAS:-dsh}"
VERSION_CODE="2"
VERSION_NAME="1.0.1"
AAPT_BIN="${AAPT_BIN:-aapt}"

rm -rf "$OUT"
mkdir -p "$OUT/gen" "$OUT/classes" "$P/assets"
cp "$P/../index.html" "$P/assets/index.html"

echo "== compile resources"
"$AAPT2" compile --dir "$P/res" -o "$OUT/res.zip"

echo "== link"
"$AAPT2" link -o "$OUT/base.apk" \
  -I "$PLAT" \
  --manifest "$P/AndroidManifest.xml" \
  -A "$P/assets" \
  "$OUT/res.zip" \
  --java "$OUT/gen" \
  --min-sdk-version 29 --target-sdk-version 34 \
  --version-code "$VERSION_CODE" --version-name "$VERSION_NAME"

echo "== javac"
find "$P/src" "$OUT/gen" -name '*.java' > "$OUT/sources.txt"
javac -nowarn -Xlint:-options -source 17 -target 17 -classpath "$PLAT" \
  -d "$OUT/classes" @"$OUT/sources.txt"

echo "== d8"
find "$OUT/classes" -name '*.class' > "$OUT/classes.txt"
java -cp "$BT/lib/d8.jar" com.android.tools.r8.D8 --release --min-api 29 \
  --lib "$PLAT" --output "$OUT" @"$OUT/classes.txt"

echo "== package"
python3 - "$OUT" <<'PY'
import sys, zipfile, os
out = sys.argv[1]
src = os.path.join(out, "base.apk")
dst = os.path.join(out, "unsigned.apk")
with zipfile.ZipFile(src) as zin, zipfile.ZipFile(dst, "w", zipfile.ZIP_DEFLATED) as zout:
    for item in zin.infolist():
        zout.writestr(item, zin.read(item.filename))
    zout.write(os.path.join(out, "classes.dex"), "classes.dex")
print("packed", os.path.getsize(dst))
PY

echo "== zipalign"
"$ZIPALIGN" -f -p 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"

echo "== sign"
if [ ! -f "$KS" ]; then
  keytool -genkeypair -keystore "$KS" -storepass android -keypass android \
    -alias "$ALIAS" -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=Text Dedup, OU=Local, O=TextDedup, C=CN" >/dev/null 2>&1
fi
java -jar "$BT/lib/apksigner.jar" sign --ks "$KS" --ks-pass pass:android \
  --key-pass pass:android --ks-key-alias "$ALIAS" \
  --out "$P/text-dedup.apk" "$OUT/aligned.apk"

echo "== verify"
java -jar "$BT/lib/apksigner.jar" verify --verbose --print-certs "$P/text-dedup.apk" | head -12
"$AAPT_BIN" dump badging "$P/text-dedup.apk" | head -8

mkdir -p "$P/../dist"
cp "$P/text-dedup.apk" "$P/../dist/text-dedup-$VERSION_NAME.apk"

echo "== done -> $P/text-dedup.apk"
ls -lh "$P/text-dedup.apk" "$P/../dist/text-dedup-$VERSION_NAME.apk"
