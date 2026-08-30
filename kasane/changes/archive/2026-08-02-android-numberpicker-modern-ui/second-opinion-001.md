# セカンドオピニオン: android-numberpicker-modern-ui (001 回目)
**相方**: codex / **日付**: 2026-08-02 / **対象**: 提案一式 (proposal.md / specs / tasks.md / ui/brief.md)
---
# レビュー結果: android-numberpicker-modern-ui

**日付**: 2026-08-02  
**判定**: **NEEDS_DISCUSSION**  
**指摘件数**: Critical 0 / Major 5 / Minor 1 / Suggestion 0

## サマリー

ADR-0007との方式整合、`unit` パリティ、確定／破棄の基本方針は一貫しています。一方、候補数の実装可能範囲、ホイール操作中の状態遷移、公開API互換性など、実装前に設計判断が必要な契約が未確定です。

## 指摘事項

### [🟠 Major] `Int` 全域では候補列挙契約を実装できない

**該当箇所**: `kasane/changes/android-numberpicker-modern-ui/specs/settings-view-android-ui/spec.md:36`、`kasane/concepts/core/cells/input-cells.md:53`、`android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/NumberPickerCellViewHolder.kt:67`

**問題点**:  
公開契約上は `min <= max` 以外の範囲制約がなく、全候補の列挙を要求しています。しかし、例えば `Int.MIN_VALUE..Int.MAX_VALUE` は `RecyclerView.Adapter.itemCount` の `Int` 上限を超えます。現行の `generateSequence { it + step }.toList()` 方式では、`Int.MAX_VALUE` 付近で加算がオーバーフローし、巨大リスト生成や事実上の無限列挙につながります。仕様どおり実装できない有効入力が存在します。

**推奨修正**:  
最大候補数と超過時の挙動（非提示＋警告、例外など）を公開契約として決めてください。全範囲対応を求めるなら、`Long` による件数計算と遅延ラベル生成を要求したうえで、候補数が `Int.MAX_VALUE` を超える場合の扱いも定義してください。`min = max = Int.MAX_VALUE` と候補数上限超過のScenarioも必要です。

### [🟠 Major] `valueText` の優先規則を候補表示にも適用するよう読める

**該当箇所**: `kasane/changes/android-numberpicker-modern-ui/specs/settings-view-android-ui/spec.md:7`、`ios/Sources/KsSettingsViewUI/NumberPickerCell.swift:126`、`ios/Sources/KsSettingsViewUI/NumberPickerCell.swift:134`

**問題点**:  
specは「`valueText` 明示指定を優先する規則」を「選択面の候補表示にも適用する」と記述しています。字義どおりなら、`valueText = "十五ピクセル"` のとき全候補が同じ文字列になる解釈も成立します。一方、iOSではCell行だけが `effectiveValueText()` で明示値を優先し、候補は各候補値を `format(value:unit:)` で個別に整形します。

**推奨修正**:  
「`valueText` の優先はCell行のみ。選択候補は常に各候補値と `unit` から生成する」と分離して明記してください。`valueText` 明示指定済みのCellで選択面を開いても、候補が「10 px」「15 px」になるScenarioを追加してください。

### [🟠 Major] スクロール中の選択値と下スワイプの競合が未定義

**該当箇所**: `kasane/changes/android-numberpicker-modern-ui/specs/settings-view-android-ui/spec.md:63`、`kasane/changes/android-numberpicker-modern-ui/specs/settings-view-android-ui/spec.md:77`、`kasane/changes/android-numberpicker-modern-ui/ui/brief.md:39`、`android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/PickerSelectionSheet.kt:442`

**問題点**:  
ホイールが `DRAGGING`／`SETTLING` 中にどの候補を「選択中」とするか、慣性スクロール中にOKを押した場合に何を確定するかが決まっていません。また、ホイールの縦スクロールと「下方向スワイプでシートをdismiss」が同じジェスチャーです。既存 `PickerSelectionSheet` はリストのスクロールをシートへ伝播させない設計であり、ジェスチャー開始位置によって実装結果が分かれます。

**推奨修正**:  
ホイールの状態遷移を定義してください。例えば「選択値はスナップ完了時のみ更新し、SETTLING中はOKを無効化する」などです。下スワイプdismissについても、ハンドル／ヘッダーからのドラッグだけか、ホイール先頭での下方向オーバースクロールも含むかを決め、各Scenarioを追加してください。

### [🟠 Major] 公開API追加を「非破壊」とする根拠が不足している

**該当箇所**: `kasane/changes/android-numberpicker-modern-ui/proposal.md:24`、`android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/NumberPickerCell.kt:24`、`android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/InputCellDsl.kt:198`

**問題点**:  
Kotlinの既定引数は、既存バイナリに対するABI互換性を保証しません。data classのprimary constructorへプロパティを追加すると、constructor、`copy`、default引数用synthetic signatureが変わります。Compose DSLもJVMシグネチャが変わります。また、引数を既存引数の途中へ挿入すれば、位置引数を使うソースにも非互換です。

**推奨修正**:  
次のどちらを契約として採るか明記してください。

- プレリリースのためABI破壊を許容し、「ソース互換も末尾追加の場合に限る」とImpactを修正する。
- バイナリ互換を保証するため、既存シグネチャの維持方法を設計し、ABI検証を受け入れ条件へ追加する。

少なくともDSL引数の追加位置と、旧バイナリ互換性の扱いは確定が必要です。

### [🟠 Major] アクセシビリティは状態公開だけで操作可能性を保証していない

**該当箇所**: `kasane/changes/android-numberpicker-modern-ui/specs/settings-view-android-ui/spec.md:105`、`kasane/changes/android-numberpicker-modern-ui/tasks.md:13`

**問題点**:  
選択中ラベルの公開だけが規定され、TalkBack等から選択値を変更する操作契約がありません。自作ホイールでは、通常のスワイプがアクセシビリティフォーカス移動として消費されるため、視覚操作と同じスクロールが成立するとは限りません。状態を読めても値を変更できない実装がspec適合になってしまいます。

**推奨修正**:  
候補行を個別に選択可能にするか、ホイールに前／次候補のアクセシビリティアクションを提供するかを決めてください。選択値の `stateDescription` 更新、操作後の通知、先頭／末尾での挙動をScenario化してください。

### [🟡 Minor] 複数のScenarioに自動検証タスクが割り当てられていない

**該当箇所**: `kasane/changes/android-numberpicker-modern-ui/tasks.md:8`、`kasane/changes/android-numberpicker-modern-ui/tasks.md:13`、`kasane/changes/android-numberpicker-modern-ui/tasks.md:21`、`android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/InputCellsTest.kt:591`

**問題点**:  
候補行へのunit反映、アクセシビリティ状態、強調色の3段階解決には実装タスクがありますが、統合状態を判定する自動テストが明示されていません。フォーマッタ単体テストだけでは候補行への適用を証明できません。また、既存テストは `AlertDialog` の提示を明示的に期待しており、更新対象として記載されていません。

**推奨修正**:  
以下をtasksへ明記してください。

- wheel adapterへのunit適用テスト
- 選択変更後のAccessibilityNodeInfo更新テスト
- `accentColor → style → Theme` 各分岐のテスト
- 既存 `AlertDialog` 提示テストのボトムシート提示テストへの置換

## アクションプラン

1. 候補数上限／overflow時の契約を決定する。
2. ホイールの操作状態とdismissジェスチャーの競合を仕様化する。
3. `valueText` と候補フォーマットの責務を分離する。
4. ABI／ソース互換性の保証範囲を決定する。
5. アクセシビリティ操作契約と不足テストを追加する。
6. 修正後にスペックレビューを再実施する。

指定どおり静的レビューのみで、ビルド・テスト・ファイル書き込みは行っていません。


## 突き合わせ結果

ホスト側自己レビュー (2周、指摘なし) との突き合わせ。採否規則: 相方のみ + 根拠強 → 採用。

| # | 指摘 | 採否 | 反映 |
|---|---|---|---|
| 1 | Int 全域では候補列挙契約を実装できない (Major) | **採用** | spec「NumberPickerCell 選択面の提示」に候補件数の Long 算出・上限超過時の非提示 + 警告ログ・オーバーフロー安全な終端の契約と Scenario を追加 |
| 2 | valueText の優先規則が候補表示にも適用と読める (Major) | **採用** | spec「unit と表示値の生成」を「valueText 優先は Cell 行のみ、候補は常に各候補値 + unit から生成」に分離し Scenario 追加 |
| 3 | スクロール中の選択値と下スワイプの競合が未定義 (Major) | **採用** | spec「選択候補の初期状態と選択操作」に「選択中候補の更新はスナップ静止時のみ」「ホイール領域の下方向操作は dismiss にならない」の契約と Scenario を追加 |
| 4 | 公開 API 追加を「非破壊」とする根拠が不足 (Major) | **採用** | proposal の Impact を「ソース互換 (named 引数・iOS と同順の挿入位置)。ABI 互換は保証対象外」に修正 |
| 5 | アクセシビリティは状態公開だけで操作可能性を保証していない (Major) | **採用** | spec「候補のアクセシビリティ状態」に前/次候補への操作アクション契約と Scenario を追加、tasks 2.2 を拡張 |
| 6 | 複数の Scenario に自動検証タスクが割り当てられていない (Minor) | **採用** | tasks に wheel adapter への unit 適用・a11y 更新・accent 3分岐・既存 AlertDialog 期待テスト置換を明記 |

降格・未解決: なし。判定: 全件を提案に反映して解消 (NEEDS_DISCUSSION → 反映後に再確認可能な状態)。
