# Deviation: fix-entrycell-enter-focus-crash

- 探索時スコープ (A案 = imeOptions のみ): exploration.md では IME ソフトキー経路の対処のみ → 指示によりスコープ拡張し、生 `KEYCODE_ENTER` (`actionId = IME_NULL`) を `setOnEditorActionListener` で消費する対応まで含める。理由: 修正後ビルドの実機 A/B で `adb shell input keyevent 66` (≒物理キーボード) が依然クラッシュすることが判明し、同一根本バグを残す完了報告を避けるため。ADR-0003 の Decision に追補済み (2026-08-01、オーナー承認)
- exploration.md 検証メモ 3 の「`adb shell input keyevent 66` → 修正後はキーボードが閉じて生存」: 当初の A案のみでは成立しない手順だったが、スコープ拡張後は成立要件に復帰。検証は Gboard ✓ 実押下と keyevent 66 の両経路で行う (2026-08-01)
