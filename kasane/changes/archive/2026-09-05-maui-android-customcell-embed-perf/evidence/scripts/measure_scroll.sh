#!/bin/sh
# 使い方: measure_scroll.sh <serial> <pkg> <label>
# 前提: 対象画面 (CustomCell デモ) を開いた状態で呼ぶ
SERIAL=$1; PKG=$2; LABEL=$3
adb -s "$SERIAL" shell dumpsys gfxinfo "$PKG" reset > /dev/null
adb -s "$SERIAL" logcat -c
sleep 1
for i in $(seq 1 8); do adb -s "$SERIAL" shell input swipe 540 1900 540 500 80; sleep 0.7; done
for i in $(seq 1 8); do adb -s "$SERIAL" shell input swipe 540 500 540 1900 80; sleep 0.7; done
sleep 1
echo "## $LABEL"
adb -s "$SERIAL" shell dumpsys gfxinfo "$PKG" | sed -n '/Total frames rendered/,/HISTOGRAM/p' | grep -v HISTOGRAM
adb -s "$SERIAL" logcat -d -s KsPerfProbe:D > "./probe-$LABEL.log"
echo "probe lines: $(wc -l < "./probe-$LABEL.log")"
