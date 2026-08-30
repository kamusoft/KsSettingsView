# セカンドオピニオン: android-picker-selection-sheet (001 回目)
**相方**: codex / **日付**: 2026-08-02 / **対象**: 提案一式 (proposal / specs / tasks / ui/brief) — spec-review
---
# レビュー結果: android-picker-selection-sheet

**日付**: 2026-08-02  
**判定**: **NEEDS_DISCUSSION**  
**件数**: Critical 0 / Major 5 / Minor 3 / Suggestion 0

## サマリー

基本フローは proposal・ADR・デルタスペック間で概ね一致しています。一方、既存のスタイル解決契約との矛盾、BottomSheet 固有の dismiss 経路、アクセシビリティ、モデルが許容する不正 index の扱いが未確定です。このまま実装すると、実装者ごとに異なる挙動になり得るため、実装開始前の仕様修正・設計判断が必要です。

指定どおり、ビルド・テストおよびファイル書き込みは行っていません。

## 指摘事項

### [🟠 Major] `CellStyle.accentColor` が強調色の解決順から欠落している

**該当箇所**: [spec.md:54](kasane/changes/android-picker-selection-sheet/specs/settings-view-android-ui/spec.md:54)、[ui/brief.md:33](kasane/changes/android-picker-selection-sheet/ui/brief.md:33)

**問題点**: 提案では解決順が「`PickerCell.accentColor` → theme 既定色」とされていますが、既存契約は「Cell 固有値 → `CellStyle` → `Theme`」です。Android の `EffectiveStyle` も `CellStyle.accentColor → Theme.cellAccentColor` を実装し、iOS 参照実装も `picker.accentColor ?? EffectiveStyle(...).accentColor` を使用しています。[EffectiveStyle.kt:383](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EffectiveStyle.kt:383)、[PickerCellView.swift:81](ios/Sources/KsSettingsViewUI/PickerCellView.swift:81)

このままでは `PickerCell.style.accentColor` が無視される実装も spec 適合と解釈でき、既存の公開スタイル契約に反します。

**推奨修正**: 解決順を `PickerCell.accentColor → PickerCell.style.accentColor → Theme.cellAccentColor` と明記し、各段のフォールバックと値が競合する場合の優先順位を Scenario 化してください。`tasks.md` の色テストも3段階を対象にします。

### [🟠 Major] 非確定 dismiss の対象経路と検証が不足している

**該当箇所**: [spec.md:7](kasane/changes/android-picker-selection-sheet/specs/settings-view-android-ui/spec.md:7)、[spec.md:14](kasane/changes/android-picker-selection-sheet/specs/settings-view-android-ui/spec.md:14)、[tasks.md:17](kasane/changes/android-picker-selection-sheet/tasks.md:17)

**問題点**: Requirement はキャンセル操作と外側タップに触れていますが、Scenario とテストタスクは明示的なキャンセル操作しか扱いません。BottomSheet にはさらに Back 操作と下方向スワイプによる dismiss があります。承認モックも下スワイプをキャンセル経路としています。

特に複数選択では、各 dismiss 経路で作業状態を破棄するのかが検証されないため、`onDismiss` で誤って callback を発火する実装を排除できません。

**推奨修正**: 「単一選択の項目タップ、または複数選択の OK 以外によるすべての dismiss は、callback を発火せず作業状態を破棄する」という不変条件を定義してください。キャンセルボタン・外側タップ・Back・下スワイプを Scenario または表形式テストへ追加します。

### [🟠 Major] 独自チェック描画によるアクセシビリティ退行を防ぐ契約がない

**該当箇所**: [proposal.md:10](kasane/changes/android-picker-selection-sheet/proposal.md:10)、[spec.md:21](kasane/changes/android-picker-selection-sheet/specs/settings-view-android-ui/spec.md:21)、[tasks.md:5](kasane/changes/android-picker-selection-sheet/tasks.md:5)

**問題点**: 現行 `AlertDialog` の Radio/Checkbox を装飾 drawable に置き換えると、TalkBack が提供していた checkable/checked または selected 状態を失う可能性があります。承認モックは視覚状態しか規定できず、spec・tasks のどちらにも候補名、選択状態、操作可能性のアクセシビリティ契約がありません。

**推奨修正**: 各候補行が候補名と現在の選択状態をアクセシビリティサービスへ公開し、複数選択のトグル後にその状態が更新される Requirement / Scenario を追加してください。単一・複数それぞれの accessibility node 状態をテスト対象にします。

### [🟠 Major] 範囲外 index と初期上限超過時の挙動が未確定

**該当箇所**: [spec.md:21](kasane/changes/android-picker-selection-sheet/specs/settings-view-android-ui/spec.md:21)、[spec.md:30](kasane/changes/android-picker-selection-sheet/specs/settings-view-android-ui/spec.md:30)、[PickerCell.kt:41](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/PickerCell.kt:41)

**問題点**: `PickerCell` は空の `items`、範囲外の `selectedIndex`、範囲外 index を含む `selectedIndices`、`maxSelectedNumber` を超えた初期集合を型として許容します。現行 Android/iOS は `selectedIndices` をそのまま作業集合へコピーするため、範囲外 index も callback に残り、上限判定の件数にも含まれます。

新コンポーネントが表示対象だけに正規化する実装も自然に考えられ、proposal の「挙動不変」と衝突する可能性があります。

**推奨修正**: 次のどちらかを明示的に決定してください。

- 現挙動を維持し、範囲外 index も callback と上限件数に保持する
- シート開始時に `items.indices` へ正規化する

空候補、範囲外単一選択、範囲外を含む複数選択、初期上限超過について Scenario を追加してください。

### [🟠 Major] 承認モックが検証対象の動的状態を網羅していない

**該当箇所**: [ui/brief.md:21](kasane/changes/android-picker-selection-sheet/ui/brief.md:21)、[tasks.md:25](kasane/changes/android-picker-selection-sheet/tasks.md:25)

**問題点**: `tasks.md` は単一・複数・上限・項目多数の各状態を `approved.png` と照合するとしていますが、承認画像が示すのは少数項目の単一・複数シートだけです。長いリストの内部スクロール、半分上限、ドラッグ後の全展開、上限拒否時の状態は視覚上の正がありません。「画面約半分」も画面サイズごとの合否を判定できません。

**推奨修正**: `ui/mock/` に少なくとも「長いリストの初期状態」「全展開状態」を承認モックとして追加し、上限状態はどのチェックが残るかを明示してください。動的挙動は `ui/brief.md` の検証条件として、初期高さ・スクロール可否・展開後状態を判定可能な表現にします。

### [🟡 Minor] 候補列挙・formatter・OS 文字列の Scenario 対応がない

**該当箇所**: [spec.md:7](kasane/changes/android-picker-selection-sheet/specs/settings-view-android-ui/spec.md:7)、[tasks.md:17](kasane/changes/android-picker-selection-sheet/tasks.md:17)

**問題点**: Requirement は候補の全件・順序、`displayFormatter`、OS ローカライズ文字列を要求していますが、対応 Scenario とテストタスクがありません。既存の formatter テストは行内 `autoValueText` 用であり、新しいシート内リストを検証しません。

**推奨修正**: items の件数・順序・formatter 適用結果、および OK/Cancel が framework resource から解決されることを Scenario とテストへ追加してください。

### [🟡 Minor] 「触覚フィードバックが発生する」は環境依存で判定不能

**該当箇所**: [spec.md:45](kasane/changes/android-picker-selection-sheet/specs/settings-view-android-ui/spec.md:45)

**問題点**: 実際の振動発生は端末能力やユーザー設定に依存します。ライブラリが保証できるのは触覚 API への要求までであり、現行実装も `performHapticFeedback` の成功を保証していません。

**推奨修正**: THEN を「拒否を示す触覚フィードバックをシステムへ要求する」とし、テストでは API 呼び出しを、実機確認では有効な端末設定下での体感を確認してください。

### [🟡 Minor] UI brief 内で採用済み仕様の表記が一致していない

**該当箇所**: [ui/brief.md:9](kasane/changes/android-picker-selection-sheet/ui/brief.md:9)、[ui/brief.md:19](kasane/changes/android-picker-selection-sheet/ui/brief.md:19)、[ui/brief.md:40](kasane/changes/android-picker-selection-sheet/ui/brief.md:40)

**問題点**: 構造図ではドラッグハンドルが「案により」と任意扱いですが、承認済み Plan B はハンドルありです。また複数選択状態では「完了」で確定と書かれていますが、確定済みラベルは OS リソースの「OK」です。

**推奨修正**: ドラッグハンドルを採用済みとして固定し、操作名を「OK」に統一してください。

## アクションプラン

1. 強調色の解決順と、不正・上限超過 index の正規化方針を決定する。
2. 非確定 dismiss とアクセシビリティの Requirement / Scenario を追加する。
3. 長いリスト・展開状態を表す承認モックを補完する。
4. Requirement の全句をテストタスクへ対応付け、haptic の保証表現を修正する。
5. `ui/brief.md` のハンドルと操作ラベル表記を統一する。

## 突き合わせ結果 (ホスト側判定: 2026-08-02)

ホスト側自己レビュー (2周) は文言整合の指摘のみで、以下はすべて相方のみの指摘。根拠で採否を判定した。

| # | 指摘 | 採否 | 対応 |
|---|---|---|---|
| 1 | accentColor の解決順から CellStyle 段が欠落 (Major) | **採用** (根拠検証済み: EffectiveStyle.kt:384 / PickerCellView.swift:81 で CONFIRMED) | spec「選択印の強調色」を3段解決に書き換え、Scenario 3件化。tasks 1.2 / 3.4 更新 |
| 2 | 非確定 dismiss 経路 (外側タップ/Back/下スワイプ) の Scenario 不足 (Major) | **採用** | Requirement に経路を明記し不変条件化、Scenario 追加。tasks 3.1 更新 |
| 3 | 独自チェック描画のアクセシビリティ退行 (Major) | **採用** | Requirement「候補行のアクセシビリティ状態」+ Scenario 2件を追加。tasks 1.4 / 3.5 追加 |
| 4 | 範囲外 index・初期上限超過の挙動未確定 (Major) | **採用** — 方針は「現挙動維持 (非正規化)」を選択 (proposal の挙動不変原則に従う)。オーナーへ提示済み | Requirement「モデル値の許容と非正規化」+ Scenario 2件を追加。tasks 3.6 追加 |
| 5 | 承認モックが動的状態 (スクロール・全展開・上限) を網羅しない (Major) | **一部採用** | brief に「検証条件 (動的挙動の判定基準)」を追加し判定可能化。全展開フレームの mock 追加はオーナーへ提示 (mock は承認ゲート済みのため) |
| 6 | 候補列挙・formatter・OS 文字列の Scenario 欠落 (Minor) | **採用** | Scenario 2件追加。tasks 3.1 更新 |
| 7 | haptic「発生する」は環境依存で判定不能 (Minor) | **採用** | 「拒否を示す触覚フィードバックをシステムへ要求する」へ表現修正 |
| 8 | brief 内の表記不一致 (ハンドル任意扱い・「完了」残存) (Minor) | **採用** | ハンドル確定・「OK」へ統一 |

未解決: なし。判定が割れた指摘: なし。

追記 (2026-08-02, オーナー裁定): 指摘5の全展開 mock フレーム追加は見送り。全展開は標準 BottomSheetBehavior 挙動であり、brief.md の「検証条件 (動的挙動の判定基準)」での判定で十分とする。
