# Exploration: fix-compose-dsl-double-update-flaky-test

## 課題 / 動機

`ks-settingsview-compose` の Robolectric テスト `KsSettingsViewComposeTest.kt:231`「DSL 方式で外部 state を 2 回連続更新しても 2 回目の追加が反映される」が flaky (add-maui-basic-input-cells の review-001 Suggestion-9 で観測):

- `./gradlew test --rerun-tasks` (全モジュール) で 1 回失敗 (expected:3 but was:2)
- モジュール単独で 2 回再実行するといずれも成功

`./gradlew test` 単体では UP-TO-DATE でテストが走らないため見逃されやすい (検出には `--rerun-tasks` が必要)。失敗形 (2 回目の更新の取りこぼし) は recomposition と store 反映の待機条件の問題である可能性があり、テストの待機不備か実装のタイミング穴かの切り分けが必要。

## 検討した選択肢

未検討 (再現の安定化が先。`consolidate-robolectric-wait-helpers` で共通化された待機ヘルパの適用漏れ・待機条件の見直しが有力な出発点)。

## 決定事項

- 公開前トリアージ (2026-08-21): **検証 CI (package-distribution phase-3) の構築前に対応**。CI ランナーは毎回フレッシュで常に `--rerun-tasks` 相当の条件になるため、flaky のまま CI を建てると赤が間欠し後続フェーズの信号が濁る
- 簡易 change として scaffold のみ作成 (オーナー指示 2026-08-11、add-maui-basic-input-cells の蒸留時)。調査・実装は未着手

## ADR 候補

なし (テスト安定化。実装のタイミング穴と判明した場合は再判定)

## 未決の論点

- flakiness の再現手順の確立 (全モジュール並列実行時のみか、負荷依存か)
- テスト側の待機不備か、DSL → store 反映経路の実タイミング穴かの切り分け

## UI 素材

なし

## 変更級の推奨: S (理由)

テスト 1 件の安定化 (実装穴と判明した場合はその時点で再判定)。

## 関連ファイル

- `android/ks-settingsview-compose/src/test/kotlin/.../KsSettingsViewComposeTest.kt:231`
- `android/ks-settingsview-ui/src/test/kotlin/.../KsSettingsViewTestSupport.kt` (待機ヘルパ)
- 出典: `kasane/changes/archive/2026-08-11-add-maui-basic-input-cells/review-001.md` (Suggestion-9)
