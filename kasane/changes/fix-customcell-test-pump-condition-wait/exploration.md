# Exploration: fix-customcell-test-pump-condition-wait

harden-compose-settingsroot-dsl の蒸留時 (2026-08-24) に受け皿として簡易起票。

## 課題 / 動機

`android/ks-settingsview-ui` のテスト 2 箇所の待機ヘルパ `pump()` が、`kasane/handbook/cross/test-execution.md` の待機規約 (実時間 deadline で区切る・ループ内で `Thread.sleep(1)` により実行機会を譲る・超過時は実測値付きで `fail()`) に反した形で残っている:

- `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/CustomCellRecycleTest.kt:389` — 反復回数で区切るループ内で `Thread.yield()`
- `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/CustomCellBuilderReleaseTest.kt:162` — 同上

反復回数区切りのため、`Thread.yield()` → `Thread.sleep(1)` の単純置換では済まず、条件ベース (deadline + 収束条件) への作り替えが必要。

harden-compose-settingsroot-dsl で同種の `Thread.yield()` 待機 (`KsSettingsViewTestSupport.awaitConvergence` / `awaitDifferCommit` ほか) は修正済みだが、この 2 箇所はオーナー指示により同 change の対象外とされ、申し送りになった。

現状確認: 2026-08-24 に両箇所の `Thread.yield()` の残存をコードで確認済み (行番号は同日時点)。

出典: `kasane/changes/archive/2026-08-24-harden-compose-settingsroot-dsl/deviation.md` (3 項目め)、同 change の `review-002.md` 範囲外の申し送り。

## 検討した選択肢 (却下案と理由を含む)

- 案A (採用): 既存の規約準拠ヘルパ `awaitDifferCommit` (KsSettingsViewTestSupport.kt) を再利用し、`pump()` を「差分コミットの収束条件つき待機 + `settle()`」に作り替える
- 案B (却下): 各テストに独自の deadline ループを書く — 同型の待機ループが 3 つ目・4 つ目になり保守が重複する

## 決定事項

- `pump()` の待機は `awaitDifferCommit` 再利用で条件ベース化する (2026-09-01 ユーザー承認)
- 収束条件は「`setRootDirect` で流した root の Cell 構成が Adapter の `currentList` に反映されたこと」。CustomCell は `LabelCell` ではなく `committedTexts` で観測できないため、`Cell.id` の一致で観測する
- 共通ロジックは KsSettingsViewTestSupport.kt に 1 箇所だけ置く (2 テストで重複させない)

## ADR 候補

なし (テスト内部の作り替えで、既存規約 test-execution.md への追従)

## 未決の論点

なし

## 変更級の推奨: S (確定)

テスト 2 ファイルの待機ヘルパの作り替えのみ。プロダクトコード・公開 API の変更なし。実害は CPU 競合時の flaky として顕在化し得る (test-execution.md の記載と同型)。直接実装で進める (2026-09-01 ユーザー承認)。
