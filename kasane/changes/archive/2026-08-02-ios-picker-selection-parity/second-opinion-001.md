# セカンドオピニオン: ios-picker-selection-parity (001 回目)
**相方**: codex / **日付**: 2026-08-02 / **対象**: 提案一式 (proposal / specs / tasks / ui/brief / exploration) — spec-review
---
# レビュー結果: ios-picker-selection-parity

**判定**: NEEDS_DISCUSSION  
**件数**: Critical 0 / Major 4 / Minor 2 / Suggestion 1

## サマリー

方針自体は実装可能ですが、現状のままでは中央寄せ・スタイル解決順・範囲外 index の非正規化をテストで保証できません。また、`pageTitle ?: title` は現行 iOS に存在しない挙動であり、意図した追加変更か判断が必要です。

レビュー中に更新された `approved.png` と承認済み `ui/brief.md` は最新状態で確認済みです。指定どおりビルド・テストは実行していません。

## 指摘事項

### [🟠 Major] 中央寄せ要求が Scenario では単なる可視性へ弱まっている

**該当箇所**: `kasane/changes/ios-picker-selection-parity/specs/settings-view-ios-host/spec.md:40`, `kasane/changes/ios-picker-selection-parity/specs/settings-view-ios-host/spec.md:42`, `kasane/changes/ios-picker-selection-parity/specs/settings-view-ios-host/spec.md:47`

**問題点**: Requirement と proposal は `.middle` 相当の中央寄せを要求していますが、Scenario の THEN は「可視領域内」のみです。先頭・末尾寄せでも全 Scenario を通過するため、オーナー決定済みの中央寄せを検証できません。

**推奨修正**: 単一・複数両 Scenario の THEN を「対象項目が `.middle` 相当の位置へスクロールされる」に強化し、中央寄せを判定するテスト条件を `tasks.md:18` に明記してください。端部では UIKit のクランプを許容することも記載すると判定が安定します。

### [🟠 Major] スクロール対象の絞り込みとモデル非正規化が分離されていない

**該当箇所**: `kasane/changes/ios-picker-selection-parity/specs/settings-view-ios-host/spec.md:40`, `kasane/changes/ios-picker-selection-parity/specs/settings-view-ios-host/spec.md:52`, `kasane/changes/android-picker-selection-sheet/specs/settings-view-android-ui/spec.md:100`, `ios/Sources/KsSettingsViewUI/PickerListViewController.swift:53`

**問題点**: 「範囲内のもののみ」がスクロール先だけに適用されるのか、`currentMulti` 自体を正規化してよいのか明記されていません。現行 iOS は範囲外 index を保持し、Android の先例も callback・上限判定を含む非正規化を明示しています。実装時に `selectedIndices` を有効範囲へ丸めると既存 callback が変わります。

また、`items` が空の場合の「先頭から表示」は対象行が存在せず、安易に row 0 へスクロールするとクラッシュし得ます。

**推奨修正**:

- 有効 index の抽出はスクロール先計算だけに使用し、選択集合は変更しないと明記する。
- 有効・範囲外 index が混在する複数選択で、範囲外値が確定 callback に残る Scenario を追加する。
- `items` が空でも0件の選択面を提示し、スクロールを行わない Scenario を追加する。
- `tasks.md:18` に混在 index・空リストのテストを追加する。

### [🟠 Major] `pageTitle ?: title` は「現行維持」ではなく未記録の挙動変更

**該当箇所**: `kasane/changes/ios-picker-selection-parity/ui/brief.md:5`, `kasane/changes/ios-picker-selection-parity/ui/brief.md:9`, `ios/Sources/KsSettingsViewUI/PickerCellView.swift:80`, `ios/Sources/KsSettingsViewUI/PickerListViewController.swift:61`

**問題点**: brief はタイトルを `pageTitle ?: title` としていますが、現行 iOS は `picker.pageTitle` だけを渡し、`nil` ならタイトルも `nil` です。proposal・デルタスペック・tasks に fallback 追加はなく、「構造は現行のまま」という説明とも整合しません。既定の `pageTitle` は `nil` なので、fallback を実装すると多数の利用箇所で見える挙動が変わります。

**推奨修正**: オーナー判断で次のどちらかを確定してください。

- 現行維持: brief を `pageTitle` のみに修正する。
- fallback 追加: proposal・spec・tasks に `pageTitle ?? title` の Requirement／Scenario／テストを追加し、利用者可視の変更として Impact に記載する。

### [🟠 Major] スタイル解決順を誤実装しても現在の Scenario を通過できる

**該当箇所**: `kasane/changes/ios-picker-selection-parity/specs/settings-view-ios-host/spec.md:14`, `kasane/changes/ios-picker-selection-parity/specs/settings-view-ios-host/spec.md:19`, `kasane/changes/ios-picker-selection-parity/specs/settings-view-ios-host/spec.md:24`, `kasane/changes/ios-picker-selection-parity/specs/settings-view-ios-host/spec.md:33`

**問題点**:

- タイトルと背景は Theme 値しか試しておらず、CellStyle が Theme より優先されることを検証できません。
- accent は中段の `style.accentColor` だけで、Cell 固有値の最優先と Theme fallback が未検証です。
- ナビバーも Theme 値だけなので、行の選択印とは別に Theme を直接参照する誤実装が通ります。
- 確定ボタンは複数選択時だけ存在しますが、Scenario の selection mode が未指定です。

**推奨修正**:

- CellStyle と Theme に異なる値を与え、CellStyle が勝つ Scenario をタイトル・背景へ追加する。
- Android 先例と同じ accent 3段階の全 Scenario を用意する。
- ナビバーが「別途 Theme を参照」するのではなく、選択印と同じ解決済み accent を使うことを検証する。
- 単一選択の Cancel と、複数選択の Cancel／確定を分けて検証する。
- `Theme.cellTitleFontSize` が選択済みフォントの size を上書きする既存特殊規則もテストに含める。

### [🟡 Minor] 主配線とキャンセル退行を既存テスト構造では確認できない

**該当箇所**: `kasane/changes/ios-picker-selection-parity/tasks.md:15`, `kasane/changes/ios-picker-selection-parity/tasks.md:19`, `ios/Tests/KsSettingsViewUITests/InputCellsTests.swift:320`, `ios/Tests/KsSettingsViewUITests/InputCellsTests.swift:339`, `ios/Sources/KsSettingsViewUI/PickerCellView.swift:69`

**問題点**: 既存テストは `PickerListViewController` を直接生成するため、`PickerCellView.presentPickerModal` が正しい実効値を渡したかは検証できません。また、既存 Picker テストにはキャンセル経路がありません。`tasks.md:19` の「既存テストで確認」だけでは未検証のまま完了扱いになり得ます。

**推奨修正**: VC 構築結果またはスタイル引数を観測できるテスト seam を用意し、CellStyle／Theme／Cell 固有 accent から提示先までの配線を検証してください。単一・複数のキャンセルで callback が発火しないテストも明示的な追加タスクにしてください。

### [🟡 Minor] Android 先例にあるアクセシビリティ契約を parity の対象外とするか不明

**該当箇所**: `kasane/changes/ios-picker-selection-parity/proposal.md:5`, `kasane/changes/android-picker-selection-sheet/specs/settings-view-android-ui/spec.md:86`, `ios/Sources/KsSettingsViewUI/PickerListViewController.swift:112`

**問題点**: Android の対となる契約は、各行の表示名と選択状態、そのトグル後の更新をアクセシビリティサービスへ公開すると定めています。iOS 側はチェックマークの視覚更新だけで、今回の「parity」がアクセシビリティ状態まで含むか proposal／Non-Goals のどちらにも記載されていません。

**推奨修正**: parity に含めるなら選択状態の公開とトグル後更新の Scenario を追加してください。別変更なら Non-Goals に明記し、追跡先を残してください。

### [🔵 Suggestion] Android の「契約先例」と視覚的先例を区別する

**該当箇所**: `kasane/changes/ios-picker-selection-parity/proposal.md:5`, `kasane/changes/android-picker-selection-sheet/specs/settings-view-android-ui/spec.md:67`, `kasane/changes/android-picker-selection-sheet/deviation.md:3`

**問題点**: Android のデルタスペックが明示するスタイル契約は accent 3段解決であり、初期スクロールは deviation にあります。行タイトル・背景・separator・ハイライトの完全な継承は、少なくとも指定された Android spec/deviation からは契約として追跡できません。

**推奨修正**: proposal では、契約先例を「accent 3段解決＋初期スクロール」、配色の視覚的先例を Android の承認 mock／verification と分けて記述すると、実装・検証時の根拠が明確になります。

## アクションプラン

1. `pageTitle` fallback とアクセシビリティ parity の対象範囲をオーナー確定する。
2. 中央寄せ、非正規化、空リスト、スタイル優先順位の Scenario を補強する。
3. 主配線・キャンセル・境界条件を明示したテストタスクへ更新する。
4. 修正後に実装着手可能です。

## 突き合わせ結果 (ホスト側判定: 2026-08-02)

ホスト側自己レビュー (2周) は指摘ゼロで、以下はすべて相方のみの指摘。根拠 (コード行の裏付け) が強く全件採用した。

| # | 指摘 | 採否 | 対応 |
|---|---|---|---|
| 1 | 中央寄せ要求が Scenario で可視性へ弱まっている (Major) | **採用** | THEN を「中央付近」+ 端部クランプ許容へ強化。tasks 3.5 に中央寄せ判定を明記 |
| 2 | スクロール対象の絞り込みとモデル非正規化が未分離 / 空 items (Major) | **採用** | Requirement に「抽出はスクロール先計算のみ・選択集合は正規化しない」を明記。範囲外混在の callback 保持と空 items の Scenario を追加 |
| 3 | `pageTitle ?: title` は未記録の挙動変更 (Major) | **採用 → オーナー裁定: fallback 追加** | Requirement「選択面のタイトル解決」を新設。proposal Impact に利用者可視変更として記載 |
| 4 | スタイル解決順を誤実装しても通過できる (Major) | **採用** | CellStyle 優先・accent 3段全段・ナビバー = 選択印と同一解決値・単一/複数のボタン構成を Scenario 化 |
| 5 | 主配線とキャンセル退行が既存テスト構造で未検証 (Minor) | **採用** | tasks 3.6 (配線 seam) / 3.7 (キャンセル経路) を新設 |
| 6 | a11y parity の対象が不明 (Minor) | **採用 → オーナー裁定: 含める** | Requirement「候補行のアクセシビリティ状態」を追加 (Android と同型) |
| 7 | 契約先例と視覚的先例の区別 (Suggestion) | **採用** | proposal に先例の区別を追記 |

未解決: なし。
