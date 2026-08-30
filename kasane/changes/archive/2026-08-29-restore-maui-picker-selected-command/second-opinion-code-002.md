# セカンドオピニオン: restore-maui-picker-selected-command (code-002)

**相方**: codex / **label**: so-code-restore-maui-picker-selected-command / **日付**: 2026-08-29 / **対象**: `afc15ae` から作業ツリーまでの実装 diff と変更アーティファクト一式

---

# レビュー結果: restore-maui-picker-selected-command

**日付**: 2026-08-29  
**判定**: **NEEDS_DISCUSSION**

## サマリー

`SelectedCommand` の公開形状、書き戻し後の実行順序、通知種別に基づく引数選択、同値再確定・未知 ID の境界について、中核実装とテストはデルタスペックに整合しています。

一方、必須 UI 足場がないことと、変更スコープ外の Kasane 実行設定が混入していることがブロッカーです。件数は **Critical 0 / Major 2 / Minor 1 / Suggestion 0** です。

## 指摘事項

### [🟠 Major] 新規画面に必須の UI アーティファクトがない

**該当箇所**: `proposal.md:16`  
**問題点**: M 級変更で新しい Sample 画面と表示構成を追加していますが、change 配下に `ui/` がありません。`proposal.md:39` は「UI の新規デザインもない」としていますが、新規画面は既存 Cell の組み合わせでも UI 変更です。Kasane 規約では UI に触れる変更は `ui/brief.md`、承認済み mock、verification が必要です。`evidence/` の画像は動作証跡であり、見た目の正となる承認済み mock の代替にはなりません。

**推奨修正**: オーナーが現在の画面構成を承認可能か判断し、`ui/brief.md`・`ui/mock/approved.png` を含む承認済み足場と、最終照合結果を `ui/verification/` に整備してください。仕様の凍結部分は書き換えず、UI 承認の扱いを決める必要があります。

### [🟠 Major] 変更と無関係なワーカー実行設定が混入している

**該当箇所**: `kasane/config.yaml:50`  
**問題点**: `workers` を追加し、`impl`・`verify`・`scout`・`extract` を `counterpart` へ切り替えています。これは `proposal.md:33` が定める facade・テスト・Sample のスコープ外で、`deviation.md` による合意もありません。さらに、コメント自身が示すとおり、書き込み・ネットワーク許可と外部持ち出し経路に関わる信頼境界の変更です。

**推奨修正**: この変更から除外するか、Kasane 運用設定の独立した change として、外部実行リスクを含めて明示的な承認を得てください。

### [🟡 Minor] 完了扱いの iOS 証跡がタスク記述を満たしていない

**該当箇所**: `tasks.md:37`  
**問題点**: 4 操作を「それぞれ操作前後で撮る」として完了済みにしていますが、入力 Cell デモには `ios-input-cells-cancel-before.png` と `ios-input-cells-dismiss-before.png` がなく、対応するのは after 画像だけです。隣接操作の画像を前状態として再利用した可能性はありますが、その対応関係も記録されていません。

**推奨修正**: 対応する before 証跡を追加するか、既存画像を再利用した対応関係を明記してください。満たすまではタスクを未完了へ戻してください。

## アクションプラン

1. 新規 Sample 画面の見た目をオーナー判断し、必須 UI 足場を整備する。
2. `kasane/config.yaml` のワーカー設定を今回の差分から分離する。
3. iOS の不足証跡または画像対応記録を補う。
4. 上記対応後に再レビューする。

ビルド・テストは依頼どおり実行せず、提示された **516 tests / 0 failures / 0 skipped**、両 Sample ビルド成功、Simulator / Emulator 検証済みという事実を前提にしました。レビュー結果ファイルは制約に従い作成していません。



## 突き合わせ結果

ホスト側 `review-002.md` の判定は APPROVED (Critical 0 / Major 0 / Minor 2 / Suggestion 3)。相方は NEEDS_DISCUSSION (Major 2 / Minor 1)。指摘ごとの採否:

- **確定: iOS の非確定操作に「操作前」証跡がない** — 双方一致 (ホスト Minor 1 / 相方 Minor 1)。`ios-input-cells-cancel-before.png` と `ios-input-cells-dismiss-before.png` を追加し、対応する after も撮り直して対にした。いずれも「最後のイベント」が `担当者 → 鈴木 一郎` のまま不変で、`SelectedCommand` 未発火を確認済み
- **確定: `kasane/config.yaml` の `workers:` 追加が作業ツリーに混入** — 双方一致 (ホスト Minor 2 / 相方 Major 2)。重要度は高い方を採り Major として扱う。本 change のスコープ外のハーネス運用設定であり、この change のコミット対象から外す
- **未解決 → オーナー裁定で決着: 新規 Sample 画面の `ui/` 一式** — 両者の判断が割れた。ホストは「新規の視覚デザイン判断が無く既存 Cell / Section / SampleTheme の組み合わせに閉じるため `ui/` 不在は妥当」と判定、相方は「新規画面の追加は UI 変更であり `ui/brief.md` と承認済み mock が必要」と Major で主張。同一論点は spec-001 でも提起され降格していたため、収束シグナルとしてオーナーへ提示。**オーナー裁定 (2026-08-29): 実経路検証が両 OS・単一/複数・確定/再確定/非確定を網羅できているため、本 change では `ui/` を求めない**
- **降格: テスト足場 `PickerScope` の重複** — second-opinion-code-001 で降格済みの論点であり方針を維持する
- **降格: 新 Sample ViewModel の初期選択がリテラル二重管理** — 回帰検出力や仕様充足に影響しないスタイル上の提案
- **降格 (申し送り): 利用者向け移行表が「SelectedCommand は提供しない」のまま** — 本 change の Non-Goal どおり触らず、蒸留後に `docs-refresh` の明示依頼で追従する
