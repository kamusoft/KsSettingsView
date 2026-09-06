# セカンドオピニオン: cell-style-dark-appearance-parity (code-001)
**相方**: codex / **label**: so-code-cell-style-dark-appearance-parity / **日付**: 2026-09-06 / **対象**: 作業ツリーの未コミット差分 (android/ ios/ maui/ kasane/handbook/maui/integration-host-verification.md) + 新規テスト 4 ファイル
---
# レビュー結果: cell-style-dark-appearance-parity

**判定**: `CHANGES_REQUESTED`  
**件数**: Critical 0 / Major 1 / Minor 4 / Suggestion 0

## サマリー

本体実装では、対象となる Cell 固有色からの直接的な `Unspecified.toArgb()` 残存は見つかりませんでした。解決順、等価性、bridge 変換、iOS dynamic color、MAUI の再代入経路も概ね spec と整合しています。

ただし、Android Compose の主要な回帰テストが `AsyncListDiffer` の完了を待っておらず、負荷次第で不安定になるため承認できません。指定どおりビルド・テストは再実行せず、提示済み結果を採用しました。

## 照合した規約

- `cross/comment-policy.md` — always
- `cross/test-execution.md` — テスト結果と非同期収束待機
- `ios/swift6-language-mode-check.md`
- `maui/integration-host-verification.md`
- `kasane/lessons/code-review.md`
- Android は設定に従い `kotlin-impl-skill` のレビュー観点も適用

## 指摘事項

### [🟠 Major] Compose の色更新テストが AsyncListDiffer の完了を待っていない

**該当箇所**: `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/compose/DSLCellStyleUpdateTest.kt:59`

**問題点**: 初期描画後と状態変更後の両方で `composeRule.waitForIdle()` の直後に行の色を読んでいます。しかし、色変更は `replaceCells` → `submitContentUpdate` → `ListAdapter.submitList` の非同期差分計算を経由し、再 bind は commit callback 後です。Compose の idle はこのバックグラウンド処理を待ちません。CPU 競合時には古い色や未生成の行を観測する可能性があり、`cross/test-execution.md` が明示的に禁止している待機方法です。一度の全件成功ではこの競合を排除できません。

**推奨修正**: 初期色と変更後の色について、それぞれ「対象行が存在し、期待色になった」ことを実時間 deadline 付きの条件ベース待機で確認してください。既存の `awaitRows` / `waitForAdapterItemCount` と同様に、待機中は Compose と main looper を進め、タイムアウト時には現在色・行一覧を出してください。

### [🟡 Minor] Cell 固有色の一部消費点に回帰検出テストがない

**該当箇所**: `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CheckboxCellViewHolder.kt:40`、`android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EntryCellViewHolder.kt:237`

**問題点**: 現在の実装はいずれも `toArgbOrElse` を正しく使っていますが、追加された実描画テストが観測する消費点は主に Switch / SimpleCheck / Radio です。Checkbox の `buttonTintList` と Entry の `highlightColor` は、将来ここだけ素の `toArgb()` に戻っても、今回追加された「12 本」のモデル既定値・等価性テストでは検出できません。

**推奨修正**: Checkbox の tint と Entry の highlightColor について、Cell 固有色が未指定なら Theme の accent になる実描画アサーションを追加してください。

### [🟡 Minor] 新しい MauiHost 検証が handbook の完了条件に含まれていない

**該当箇所**: `kasane/handbook/maui/integration-host-verification.md:120`

**問題点**: 外観追随ボタンの操作と期待値は本文へ追加されていますが、直後の完了条件は従来の内容更新・再訪問だけです。このままでは、将来 handbook の完了条件だけを満たして外観追随を確認せずに検証完了と報告できます。

**推奨修正**: MauiHost の完了条件へ、表示中の外観切替により同じ行の title 色が両 OS で切り替わることを追加してください。

### [🟡 Minor] exploration の確定スコープが現行 proposal と矛盾している

**該当箇所**: `kasane/changes/cell-style-dark-appearance-parity/exploration.md:80`

**問題点**: オーナー確定済みとされる変更級説明に、`placeholderColor` DTO の追加と MAUI Sample の `AppThemeBinding` 化が現在の対象として残っています。どちらも現行 proposal では Non-Goal であり、実装も行われていません。

**推奨修正**: 現在の確定スコープへ書き換えるか、当初案であり後続の spike・裁定で撤回されたことを明記してください。

### [🟡 Minor] 別 change の evidence を壊れやすい相対パスで参照している

**該当箇所**: `kasane/changes/maui-appthemebinding-coverage/exploration.md:18`

**問題点**: `../cell-style-dark-appearance-parity/evidence/...` は別 change への参照です。独立に archive されるとディレクトリ階層が変わり、参照できなくなります。change 相対パスは同じ change 内に限定するという Kasane のパス規約にも合いません。

**推奨修正**: `kasane/changes/cell-style-dark-appearance-parity/evidence/...` のリポジトリ相対表記へ変更してください。

## アクションプラン

1. `DSLCellStyleUpdateTest` を条件ベース待機へ修正する。
2. Checkbox / Entry accent の実消費点テストを追加する。
3. handbook の完了条件へ外観追随確認を追加する。
4. exploration の確定スコープと別 change 参照パスを整理する。
5. 修正後に指定の全件テストを再実行し、件数を再確認する。

依頼に従い、レビュー結果ファイルは作成していません。


## 突き合わせ結果 (ホスト側 review-001 = APPROVED / Minor 3 + Suggestion 1 との照合、2026-09-06)

| 相方の指摘 | ホスト側 | 採否 | 根拠 |
|---|---|---|---|
| [Major] `DSLCellStyleUpdateTest` が `waitForIdle()` だけで `AsyncListDiffer` の完了を待っていない | 指摘なし | **採用 (Major、ホスト側の見逃し)** | handbook `cross/test-execution.md` の Android 節が「idle 系では差分計算の完了を待てない」と明記し、`DSLCellStyleUpdateTest.kt` は初期・変更後とも `waitForIdle()` 直後に行の色を読む。実害: 並列実行時のみ落ちる flaky |
| [Minor] Checkbox の tint / Entry の highlightColor に回帰検出テストがない | Minor 1 (Entry のみ、probe で実証) | **確定** (Checkbox も含める) | 双方一致。ホストの probe 25 件のいずれも Entry 消費点を観測せず |
| [Minor] MauiHost の外観追随が handbook の完了条件に無い | 指摘なし | **採用 (Minor)** | 該当箇所特定・完了条件だけ満たして未確認で通せる実害あり。1 行で閉じる |
| [Minor] exploration.md:80 の確定スコープが現行 proposal と矛盾 | 指摘なし | **採用・対処済み** | 探索時点の見積もりであり現行は proposal が正、と注記を追加 |
| [Minor] 別 change の evidence を change 相対で参照 | 指摘なし | **採用・対処済み** | `maui-appthemebinding-coverage/exploration.md` をリポジトリ相対 (+ archive 後の解決) に修正 |

ホスト側のみの指摘 (相方にない): Minor 2「Android の DSL / Store 経路で行が作り直されていないことが未観測」(確定、修正サイクルへ)、Minor 3「蒸留申し送りに ADR-0030 Decision 3 の MAUI 節の限定が無い」(確定、tasks 申し送りに追記)、Suggestion「spec の `replaceCell` は Android DSL では `replaceCells`」(蒸留で実態どおり書く)。

総合判定: **CHANGES_REQUESTED** (採用 Major 1 + Minor 4 を修正サイクル 1 周で処理)。
