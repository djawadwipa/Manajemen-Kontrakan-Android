#!/usr/bin/env bash
set -euo pipefail

ROOT="${1:-.}"
APP="$ROOT/app"
BUILD_FILE="$APP/build.gradle.kts"
MANIFEST="$APP/src/main/AndroidManifest.xml"
PACKAGE_ID='id.djawadwipa.manajemenkontrakan'

if find "$APP/src" -type f \( -name '*.html' -o -name '*.css' -o -name '*.js' -o -name '*.jsx' -o -name '*.ts' -o -name '*.tsx' \) -print -quit | grep -q .; then
  echo "Ditemukan aset web di app/src." >&2
  exit 1
fi

if grep -RInE 'android\.webkit\.WebView|Capacitor|Cordova|service[-_ ]worker|ReactNative|com\.facebook\.react' "$APP/src"; then
  echo "Ditemukan teknologi web/hybrid yang dilarang." >&2
  exit 1
fi

if grep -qE 'android\.permission\.(INTERNET|MANAGE_EXTERNAL_STORAGE|WRITE_EXTERNAL_STORAGE|READ_EXTERNAL_STORAGE)' "$MANIFEST"; then
  echo "Ditemukan permission yang tidak diperlukan." >&2
  exit 1
fi

grep -q 'android:usesCleartextTraffic="false"' "$MANIFEST"
grep -q 'isDebuggable = false' "$BUILD_FILE"
grep -q "namespace = \"$PACKAGE_ID\"" "$BUILD_FILE"
grep -q "applicationId = \"$PACKAGE_ID\"" "$BUILD_FILE"

if find "$ROOT" -type f \( -name '*.jks' -o -name '*.keystore' -o -name '*.p12' -o -name '*.pem' \) -print -quit | grep -q .; then
  echo "Ditemukan material kunci privat di repository." >&2
  exit 1
fi

if find "$ROOT" -type f \( -name '*.xlsx' -o -name '*.xls' \) -print -quit | grep -q .; then
  echo "Workbook sumber/data nyata tidak boleh masuk repository." >&2
  exit 1
fi

if grep -RInE --exclude-dir=build --exclude='*.md' --exclude='*.yml' --exclude='*.yaml' \
  -- '-----BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY-----|AKIA[0-9A-Z]{16}' "$ROOT"; then
  echo "Ditemukan pola secret/kunci privat." >&2
  exit 1
fi

echo "Native-only, package ID, permission, cleartext, release, data, dan secret checks: OK"
