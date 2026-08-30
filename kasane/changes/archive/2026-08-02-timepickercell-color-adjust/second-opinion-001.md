# セカンドオピニオン: timepickercell-color-adjust (001 回目)
**相方**: codex / **日付**: 2026-08-02 / **対象**: 提案一式 (proposal / specs / tasks / ui brief) — spec-review モード
---
# レビュー結果: timepickercell-color-adjust

**日付**: 2026-08-02  
**判定**: **NEEDS_DISCUSSION**

## サマリー

背景トークンと視覚仕様が未確定で、承認済みモックも存在しないため、現状のまま実装へ進むことはできません。また、12時間表示、コントラスト判定、色の適用範囲に検証可能性の穴があります。

指摘件数: Critical 0 / Major 6 / Minor 1 / Suggestion 1。制約に従いビルド・テストは実行していません。

## 指摘事項

### [🟠 Major] ダイアログ背景の正が未決定のまま文書間で矛盾している

**該当箇所**: [proposal.md:11](kasane/changes/timepickercell-color-adjust/proposal.md:11)、[spec.md:9](kasane/changes/timepickercell-color-adjust/specs/cell-types-input/spec.md:9)、[brief.md:24](kasane/changes/timepickercell-color-adjust/ui/brief.md:24)、[brief.md:32](kasane/changes/timepickercell-color-adjust/ui/brief.md:32)

**問題点**: proposal とデルタスペックは `Theme.backgroundColor` に確定していますが、UI brief は `Theme.backgroundColor` と `Theme.cellBackgroundColor` の二択を未決定としています。承認モック欄も空で、`ui/mock/approved.png` もありません。両トークンは既存契約上、canvas と Cell 背景を表す独立値であり、交換可能ではありません。

**推奨修正**: どちらをダイアログ背景とするかオーナー判断で確定し、proposal・spec・brief・tasks を統一してください。その後、採用 HTML と `approved.png` を brief に記録してから実装ゲートを開いてください。

### [🟠 Major] 「既定配色が残る領域がない」の適用範囲と色マッピングが定義不足

**該当箇所**: [proposal.md:10](kasane/changes/timepickercell-color-adjust/proposal.md:10)、[spec.md:18](kasane/changes/timepickercell-color-adjust/specs/cell-types-input/spec.md:18)、[brief.md:28](kasane/changes/timepickercell-color-adjust/ui/brief.md:28)

**問題点**: spec の「プラットフォーム既定配色のまま残る領域がない」は、scrim、モード切替アイコン、非選択フィールド、時計盤面、フォーカス・無効・入力エラー状態まで含むのか判定できません。一方、中間面の背景由来色は brief にだけ登場し、導出規則も「低透過の黒/白」としか決まっていません。

**推奨修正**: 対象部位と状態を表にし、各部位を背景・accent・title・on-accent・背景由来中間面・対象外のいずれかへ割り当ててください。「既定色が残らない」はその表の対象部位に限定する受け入れ条件へ変更し、中間面の明暗判定も決定してください。

### [🟠 Major] タイトル文字色の解決順が共通規則どおりか判定できない

**該当箇所**: [proposal.md:13](kasane/changes/timepickercell-color-adjust/proposal.md:13)、[spec.md:11](kasane/changes/timepickercell-color-adjust/specs/cell-types-input/spec.md:11)、[style-resolution.md:28](kasane/concepts/core/styling/style-resolution.md:28)、[EffectiveStyle.kt:111](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EffectiveStyle.kt:111)

**問題点**: 「`cellTitleColor` の解決値」「解決済みタイトル文字色」だけでは、`CellStyle.titleColor` が `Theme.cellTitleColor` より優先される既存契約を実装者が維持する保証がありません。Scenario と tasks にも CellStyle 優先または platform default へのフォールバック試験がありません。

**推奨修正**: `CellStyle.titleColor → Theme.cellTitleColor → platform default` を明記し、CellStyle 優先・Theme fallback・双方未指定の各 Scenario とテストタスクを追加してください。

### [🟠 Major] 12時間表示の AM/PM 契約がデルタスペックと検証計画から抜けている

**該当箇所**: [proposal.md:12](kasane/changes/timepickercell-color-adjust/proposal.md:12)、[brief.md:12](kasane/changes/timepickercell-color-adjust/ui/brief.md:12)、[spec.md:7](kasane/changes/timepickercell-color-adjust/specs/cell-types-input/spec.md:7)、[tasks.md:20](kasane/changes/timepickercell-color-adjust/tasks.md:20)

**問題点**: proposal と brief は AM/PM 選択状態を対象に含めていますが、spec の部位一覧と Scenario には登場しません。tasks は `state_checked` 実装には触れるものの、12時間表示のスクリーンショットや状態遷移確認を要求していません。実装されなくても受け入れ検証を通過できます。

**推奨修正**: 12時間形式について、時計／キーボード両モード、AM／PM 切替前後、選択・非選択状態の Scenario と実機検証項目を追加してください。

### [🟠 Major] on-accent の白黒判定が決定的・検証可能になっていない

**該当箇所**: [spec.md:32](kasane/changes/timepickercell-color-adjust/specs/cell-types-input/spec.md:32)、[brief.md:27](kasane/changes/timepickercell-color-adjust/ui/brief.md:27)、[tasks.md:29](kasane/changes/timepickercell-color-adjust/tasks.md:29)

**問題点**: 「明度」「可読性」の計算式、閾値、半透明 accent の合成先が未定義です。実装者が YIQ、単純平均、WCAG relative luminance のどれを選んでも Scenario を満たしたと主張でき、境界色では結果が変わります。

**推奨修正**: 例えば「背景へ合成後の色に対し、黒と白のうち WCAG contrast ratio が高い方を選ぶ」のように決定的な規則を定めてください。明・暗の代表値だけでなく、切替境界と半透明色もテスト対象にしてください。

### [🟠 Major] tasks が accepted ADR にない `OnHierarchyChangeListener` を追加している

**該当箇所**: [tasks.md:21](kasane/changes/timepickercell-color-adjust/tasks.md:21)、[ADR-0005:25](kasane/decisions/android/0005-timepicker-dialog-runtime-coloring-via-view-traversal.md:25)、[exploration.md:36](kasane/changes/timepickercell-color-adjust/exploration.md:36)

**問題点**: ADR と exploration は ViewStub の遅延生成を `OnPreDrawListener` で追随すると決定していますが、tasks は追加で `OnHierarchyChangeListener` を要求しています。`ViewGroup.setOnHierarchyChangeListener` は単一 listener を置き換える API なので、Material 内部が使用していた場合に挙動を壊すリスクがあります。追加理由や安全性検証もありません。

**推奨修正**: 原則として tasks から削除し、accepted ADR どおり pre-draw の全走査だけで追随してください。必要性がスパイクで判明した場合は、既存 listener を破壊しない代替フックを選び、設計判断として明示してください。

### [🟡 Minor] デルタスペックに見た目の詳細が入り、mock／brief と責務が重複している

**該当箇所**: [spec.md:7](kasane/changes/timepickercell-color-adjust/specs/cell-types-input/spec.md:7)、[spec.md:9](kasane/changes/timepickercell-color-adjust/specs/cell-types-input/spec.md:9)

**問題点**: 部位ごとの視覚マッピングがデルタスペックと brief に重複し、すでに背景トークンの不整合が発生しています。これは ksn-core の UI lint が意図する「spec は挙動、mock は見た目」の分離を弱めます。

**推奨修正**: spec は「開く／モードを切り替える／値を選択する際に、承認済みの色ロールが維持される」という観察可能な契約へ寄せ、部位一覧と視覚状態は brief と approved mock に一本化してください。色解決順のような非視覚的契約は spec に残して構いません。

### [🔵 Suggestion] アクセント解決ロジックの二重実装を避ける

**該当箇所**: [tasks.md:12](kasane/changes/timepickercell-color-adjust/tasks.md:12)、[EffectiveStyle.kt:140](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EffectiveStyle.kt:140)、[EffectiveStyle.kt:386](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EffectiveStyle.kt:386)

**問題点**: `CellStyle.accentColor → Theme.cellAccentColor` は既に `EffectiveStyle` が解決しています。tasks の「解決を実装する」が独立 resolver の追加を意味すると、共通規則が二重管理になります。

**推奨修正**: `TimePickerCell.accentColor ?: effective.accentColor` という既存 Cell と同じ接続方式を tasks に明記し、新規ロジックは Cell 固有値の優先だけに限定してください。

## アクションプラン

1. 背景トークンを確定し、承認済み mock と `approved.png` を作る。
2. 対象部位・状態・色ロールの対応表を確定する。
3. 12時間表示、タイトル色解決、on-accent 計算を Scenario と tests に追加する。
4. `OnHierarchyChangeListener` を削除または設計判断として再検討する。
5. spec と UI brief/mock の責務を整理してから再レビューする。

## 突き合わせ結果 (ホスト側判定、2026-08-02)

| # | 指摘 | 採否 | 対応 |
|---|---|---|---|
| 1 | Major: 背景トークン未確定・approved.png 無し | **時点解消** | レビュー依頼と並行してオーナーが A案 (`Theme.backgroundColor`) を承認済み。brief 承認記録 + approved.png 併置済み (指摘時点では正しい指摘) |
| 2 | Major: 「既定配色が残る領域がない」の適用範囲未定義 | **採用** | brief.md に部位対応表 (部位 → 背景/強調/通常文字/アクセント上文字/中間面/対象外) を新設。spec の THEN を「対応表で割り当てられた部位」に限定 |
| 3 | Major: タイトル文字色の解決順が不明 | **採用** | spec / proposal に `CellStyle.titleColor` → `Theme.cellTitleColor` → プラットフォーム既定を明記 (EffectiveStyle の既存解決に乗る) |
| 4 | Major: 12h AM/PM が spec / tasks から欠落 | **採用** | Scenario「12時間フォーマットでの反映」追加、tasks 5.4 (12h 実機確認) 追加、強調ロールに AM/PM 選択状態を明記 |
| 5 | Major: on-accent 白黒判定が非決定的 | **採用** | 「黒と白のうちコントラスト比が高い方」の決定的規則へ spec / proposal を修正。tasks 4.2 に境界近傍色のテストを追加 |
| 6 | Major: tasks が ADR に無い `OnHierarchyChangeListener` を追加 | **採用** | tasks 3.4 から削除し pre-draw のみに統一 (単一リスナー置換 API のリスクも注記) |
| 7 | Minor: spec に見た目詳細が重複 | **採用 (指摘2と同時解決)** | 部位列挙を brief の対応表へ一本化し、spec は色ロールの契約に留めた |
| 8 | Suggestion: アクセント解決の二重実装回避 | **採用** | tasks 2.1 を `cell.accentColor ?: effective.accentColor` 方式に明記 |

- 双方一致の指摘: なし (ホスト自己レビューはスコープ表記の1件のみで相方と重複せず)
- 降格・未解決: なし。判定 NEEDS_DISCUSSION の主因 (#1) はタイミング起因で解消済みのため、反映後の提案一式で確定とする
