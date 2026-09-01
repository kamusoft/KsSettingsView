# Deviation: adopt-android-explicit-api-mode

- [付随修正] `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsCellRegistry.kt`: 公開型の KDoc を独自 Cell 登録の利用者向け契約だけに整理し、内部メンバーと内部 Adapter の説明を除去した。spec 外だが同じ Android 公開 API 境界能力にあり、本務で触れたファイル内のコメントだけを直すため。公開 API・実行時挙動の変更なし (2026-09-01)
- [付随修正] `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsCellRegistry.kt`: `isRegistered` の KDoc に、自動登録が既存登録を上書きしない判定とテスト・診断に使う実態を反映した。spec 外だが同じ能力・同じファイル内のコメント修正であり、公開 API・実行時挙動の変更なし (2026-09-01)
- [付随修正] `kasane/changes/adopt-android-explicit-api-mode/evidence/release-aar-abi-diff.txt`: `javap -public` が member 単位の変化を観測する手法であることと、型単位の誤った internal 化が指定 3 関数以外の `internal` 追加なしという別走査で否定されたことを追記した。spec 外だが同じ能力の本務証跡を正確化する局所修正であり、公開 API・実行時挙動の変更なし (2026-09-01)
- [付随修正] `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/Theme.kt`: `DEFAULT_BACKGROUND_COLOR` の公開 KDoc から change 識別子と履歴記述を除去し、現在の仕様だけにした。spec 外だが同じ能力・本務で触れたファイル内のコメント修正であり、公開 API・実行時挙動の変更なし (2026-09-01)
