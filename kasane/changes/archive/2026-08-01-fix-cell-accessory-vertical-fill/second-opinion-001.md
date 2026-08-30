# セカンドオピニオン: fix-cell-accessory-vertical-fill (001 回目・spec-review)
**相方**: codex / **日付**: 2026-08-01 / **対象**: 提案一式 (proposal / specs / tasks / ui) + ios/ADR-0001
---
# レビュー結果: fix-cell-accessory-vertical-fill

**日付**: 2026-08-01  
**判定**: **NEEDS_DISCUSSION**

## サマリー

提案の目的と ADR の基本方針は整合していますが、現行実装と両立しない MUST、再描画時のライフサイクル未定義、Android を含める範囲の矛盾があります。さらに、`hintText` との衝突や承認モックとの不整合など、実装者の判断だけでは解消できない設計選択が残っているため、実装開始前の合意が必要です。

指摘件数: Critical 0 / Major 7 / Minor 2 / Suggestion 0  
依頼どおりビルド・テストおよびファイル書き込みは実施していません。

## 指摘事項

### [🟠 Major] デルタスペックが UI lint に反し、見た目と実装構造の正が重複している

**該当箇所**: [kasane/changes/fix-cell-accessory-vertical-fill/specs/settings-view-ios-host/spec.md:9](kasane/changes/fix-cell-accessory-vertical-fill/specs/settings-view-ios-host/spec.md:9)、[kasane/changes/fix-cell-accessory-vertical-fill/specs/cell-types-basic/spec.md:7](kasane/changes/fix-cell-accessory-vertical-fill/specs/cell-types-basic/spec.md:7)

**問題点**: Kasane のデルタスペックは挙動の契約であり、具体的なレイアウト配置や特定 UIKit control 名は mock／brief／ADR 側へ置く規約です。本 spec は `UIStackView` 階層、配置順、`UISwitch` などを詳細に規定しており、承認 mock と ADR の双方に同じ判断が複製されています。実際、後述のとおり mock と現行寸法には差があり、どれを優先すべきか既に曖昧です。

**推奨修正**: デルタスペックには「description とアクセサリが交差しない」「アクセサリの中心がセル全体の垂直中心と一致する」「valueText は主行に残る」など観察可能な結果だけを残してください。具体的な Stack 階層と `accessoryHolder` の設計は ADR、実装手順は tasks、見た目は承認 mock に一本化します。

### [🟠 Major] `contentView.subviews` 変更禁止が既存 Picker 系実装と衝突する

**該当箇所**: [kasane/changes/fix-cell-accessory-vertical-fill/specs/settings-view-ios-host/spec.md:34](kasane/changes/fix-cell-accessory-vertical-fill/specs/settings-view-ios-host/spec.md:34)、[kasane/changes/fix-cell-accessory-vertical-fill/tasks.md:18](kasane/changes/fix-cell-accessory-vertical-fill/tasks.md:18)

**問題点**: spec は派生 Renderer による `contentView.subviews` の変更を全面禁止しています。一方、現行の `NumberPickerCellView`、`TimePickerCellView`、`DatePickerCellView` は入力 Picker を表示する `EmbeddedPickerHostField` を `contentView` 直下へ追加します（例: [TimePickerCellView.swift:39](ios/Sources/KsSettingsViewUI/TimePickerCellView.swift:39)）。tasks はこれらを「chevron の移動」だけで済ませるため、spec 準拠と既存入力機能の維持を同時に達成できません。

**推奨修正**: 次のいずれかを設計判断として確定してください。

- `stackH` などの恒常階層を除去・置換しない、という禁止へ狭め、背面の入力ホストを明示的に例外化する。
- 入力ホストの配置先を変更し、Picker 系3種の追加改修と first-responder 回帰テストを tasks に含める。

### [🟠 Major] 再 render 時のアクセサリ置換規則が未定義

**該当箇所**: [kasane/changes/fix-cell-accessory-vertical-fill/specs/settings-view-ios-host/spec.md:29](kasane/changes/fix-cell-accessory-vertical-fill/specs/settings-view-ios-host/spec.md:29)、[kasane/changes/fix-cell-accessory-vertical-fill/tasks.md:10](kasane/changes/fix-cell-accessory-vertical-fill/tasks.md:10)

**問題点**: nil の場合に holder を空にすることは規定されていますが、non-nil から別の non-nil へ再描画するとき、既存内容を除去して置換するかが決まっていません。chevron は [CellBaseLayout.swift:165](ios/Sources/KsSettingsViewUI/CellBaseLayout.swift:165) で render ごとに新規生成されるため、単純な `addSubview` 実装では reconfigure のたびに蓄積します。

**推奨修正**: `applyCellBaseLayout` は毎回 holder の旧内容を除去してから、0個または1個の `accessoryView` を設定する、と明記してください。少なくとも以下の Scenario が必要です。

- non-nil A → non-nil B で B のみ残る。
- non-nil → nil で空領域も消える。
- 同じ永続 View を連続指定しても重複しない。
- `hideArrow: false → true → false` の再描画で chevron が常に0個または1個になる。

### [🟠 Major] iOS-only の提案に対し、Android を含む契約が現行 EntryCell と矛盾する

**該当箇所**: [kasane/changes/fix-cell-accessory-vertical-fill/proposal.md:18](kasane/changes/fix-cell-accessory-vertical-fill/proposal.md:18)、[kasane/changes/fix-cell-accessory-vertical-fill/specs/cell-types-basic/spec.md:14](kasane/changes/fix-cell-accessory-vertical-fill/specs/cell-types-basic/spec.md:14)、[kasane/changes/fix-cell-accessory-vertical-fill/specs/cell-types-basic/spec.md:28](kasane/changes/fix-cell-accessory-vertical-fill/specs/cell-types-basic/spec.md:28)

**問題点**: proposal は Android を Non-Goal としていますが、デルタスペックは2系統配置を両 platform の契約とし、EntryCell では Cell 級アクセサリ領域を確保しないと規定しています。現行 Android の EntryCell は実際には `EditText` を `accessoryHolder` に配置しています（[EntryCellViewHolder.kt:220](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EntryCellViewHolder.kt:220)）。したがって「Android は既存実装で本要件を満たす」という Scenario は、物理構造としては成立しません。

**推奨修正**: iOS-only を維持するなら Requirement と Scenario を iOS の観察可能な挙動へ限定し、Android Scenario と task 4.4 を外してください。クロスプラットフォーム契約へ昇格するなら、「行内」の意味を物理コンテナではなく視覚結果で定義し直すか、Android EntryCell の変更を正式にスコープへ含める必要があります。

### [🟠 Major] `hintText` と右端アクセサリの衝突時挙動が決まっていない

**該当箇所**: [kasane/changes/fix-cell-accessory-vertical-fill/specs/settings-view-ios-host/spec.md:27](kasane/changes/fix-cell-accessory-vertical-fill/specs/settings-view-ios-host/spec.md:27)、[kasane/changes/fix-cell-accessory-vertical-fill/ui/brief.md:7](kasane/changes/fix-cell-accessory-vertical-fill/ui/brief.md:7)

**問題点**: `hintLabel` はセル右端基準を維持し、アクセサリも右端列へ配置されます。現行制約では hint は `cell.trailingAnchor - 10`（[KsListCellBase.swift:233](ios/Sources/KsSettingsViewUI/KsListCellBase.swift:233)）、アクセサリは stack の右 margin 内に置かれるため、Switch や checkmark と重なる可能性があります。`hintText` は公開された任意フィールドですが、承認 mock に hint とアクセサリの組み合わせがありません。

**推奨修正**: hint をアクセサリより leading 側へ制限する、アクセサリ存在時は別位置へ移す、または重なりを許容する、のいずれかを明示的に決定してください。決定後、`hintText + Switch` と `hintText + chevron + valueText` を mock と Scenario に追加します。

### [🟠 Major] 承認 mock が「既存見た目を維持」と両立していない

**該当箇所**: [kasane/changes/fix-cell-accessory-vertical-fill/ui/brief.md:15](kasane/changes/fix-cell-accessory-vertical-fill/ui/brief.md:15)、[kasane/changes/fix-cell-accessory-vertical-fill/ui/mock/plan-a.html:43](kasane/changes/fix-cell-accessory-vertical-fill/ui/mock/plan-a.html:43)、[kasane/changes/fix-cell-accessory-vertical-fill/ui/mock/plan-a.html:63](kasane/changes/fix-cell-accessory-vertical-fill/ui/mock/plan-a.html:63)

**問題点**: brief はアクセサリ以外の見た目を維持するとしますが、承認 mock は `stackV` 相当の gap を2px、checkbox を22pxで描画しています。現行実装はそれぞれ4pt（[KsListCellBase.swift:78](ios/Sources/KsSettingsViewUI/KsListCellBase.swift:78)）と20pt（[KsCheckBoxView.swift:33](ios/Sources/KsSettingsViewUI/KsCheckBoxView.swift:33)）です。brief が近似値として明示しているのは配色だけなので、Kasane 上「見た目の正」である mock に従うと、スコープ外の寸法変更まで要求されます。

**推奨修正**: 現行トークン・寸法を再現し、アクセサリ列だけを変更した mock を再承認してください。近似 mock として扱うのであれば、何が非規範的かを brief に明示するだけでなく、「見た目の正」として利用できる比較基準を別途用意する必要があります。

### [🟠 Major] 核心となるレイアウト結果の受け入れ基準が再現可能でない

**該当箇所**: [kasane/changes/fix-cell-accessory-vertical-fill/specs/cell-types-basic/spec.md:16](kasane/changes/fix-cell-accessory-vertical-fill/specs/cell-types-basic/spec.md:16)、[kasane/changes/fix-cell-accessory-vertical-fill/tasks.md:29](kasane/changes/fix-cell-accessory-vertical-fill/tasks.md:29)

**問題点**: Scenario の長文はプレースホルダーで、セル幅、行高モード、trait、layout 実行条件がありません。「重ならない」「垂直センター」も比較対象と許容差が未定義です。既存の hierarchy assert だけでは、holder が幅0、余計に伸長、制約競合していても合格できます。目視比較も対象 Simulator、viewport、差分許容が決まっていません。

**推奨修正**: 固定幅のセルと具体的な長文を使い、layout 後に少なくとも次を判定する Scenario／テストを追加してください。

- `descriptionLabel.frame.maxX <= accessoryHolder.frame.minX`
- `accessoryView.center.y` とセル内容領域の `midY` が許容差内
- holder 幅がアクセサリの自然幅を保持し、stackV が残り幅を取る
- accessory nil 時は stackV が同じ条件で右端まで回復する
- Switch、chevron、checkmark の代表3サイズで成立する

### [🟡 Minor] `prepareForReuse` の全除去要件と first-responder 保護に例外条件がない

**該当箇所**: [kasane/changes/fix-cell-accessory-vertical-fill/specs/settings-view-ios-host/spec.md:41](kasane/changes/fix-cell-accessory-vertical-fill/specs/settings-view-ios-host/spec.md:41)、[kasane/changes/fix-cell-accessory-vertical-fill/specs/settings-view-ios-host/spec.md:67](kasane/changes/fix-cell-accessory-vertical-fill/specs/settings-view-ios-host/spec.md:67)

**問題点**: 本文は追加 View を除去しつつ first responder 保護を維持するとしていますが、Scenario は無条件で `contentStack == [titleLabel]` を要求します。first responder を含む View の場合、現行契約では残るため、両方を同時に満たせません。

**推奨修正**: Scenario の GIVEN を「first responder を含まない trailing view」に限定し、別 Scenario で編集中の Entry field が再 render 時に維持されることを規定してください。

### [🟡 Minor] Android テストを追加しても実行タスクがない

**該当箇所**: [kasane/changes/fix-cell-accessory-vertical-fill/tasks.md:26](kasane/changes/fix-cell-accessory-vertical-fill/tasks.md:26)、[kasane/changes/fix-cell-accessory-vertical-fill/tasks.md:27](kasane/changes/fix-cell-accessory-vertical-fill/tasks.md:27)

**問題点**: task 4.4 は必要に応じて Android 回帰テストを追加しますが、完了条件は `swift test` だけです。追加した Android テストが一度も実行されないまま完了扱いにできます。

**推奨修正**: Android をスコープに残す場合は、対象 Gradle test task の実行を完了条件へ追加してください。iOS-only に戻す場合は task 4.4 自体を削除します。

## アクションプラン

1. Picker 系 `contentView` 例外、Android の扱い、`hintText` とアクセサリの共存方針を先に決定する。
2. デルタスペックを観察可能な挙動へ絞り、構造は ADR、見た目は mock へ一本化する。
3. 現行寸法を維持した承認 mock を作り直し、hint を含む組み合わせを追加する。
4. 再 render の置換規則と、固定条件で判定できる geometry Scenario を追加する。
5. first-responder 例外と platform ごとのテスト実行タスクを整備したうえで再レビューする。

## 突き合わせ結果 (ホスト側自己レビューとの採否)

| # | 指摘 | 採否 | 対応 |
|---|---|---|---|
| 1 | UI lint 違反 (構造記述の重複) | **降格** | 自己レビューで既検討の論点。階層はテストで観察可能な内部 API 契約であり、リポジトリ慣例 (openspec 時代の同 capability spec) とも整合。視覚パラメータは既に排除済み |
| 2 | contentView.subviews 変更禁止が Picker 系 EmbeddedPickerHostField と衝突 | **採用** (コードで検証済: TimePickerCellView.swift:41-43) | MUST NOT を「恒常階層の除去・置換禁止」に狭め、補助 view の背面追加を明示的に許容 |
| 3 | 再 render 時のアクセサリ置換規則が未定義 | **採用** | 「毎回旧内容を除去、常に 0 or 1 個」を Requirement 化し Scenario「再 render でアクセサリが蓄積しない」を追加。tasks 4.3 追加 |
| 4 | Android EntryCell (EditText が accessoryHolder) と 2 系統契約が矛盾 | **採用** (コードで検証済: EntryCellViewHolder.kt:224-227) | 行内 trailing の EntryCell 規定を iOS 限定に修正。Android EntryCell は既存配置維持を明記。Android テストタスク (旧 4.4) を削除 |
| 5 | hintText とアクセサリの衝突時挙動 | **降格** | hintLabel は本 change で挙動不変 (spec 明記済)。既存契約に「衝突時は hint 前面」の規定があり、アクセサリの垂直センター化はむしろ右上 hint との重なりを現状より減らす方向 |
| 6 | 承認 mock の寸法が現行実装と乖離 | **採用** (brief で解決) | brief.md に mock の規範範囲 (配置関係のみ規範、寸法・生値は非規範) を明記 |
| 7 | 受け入れ基準が再現不可能 | **採用** | 固定幅 + layoutIfNeeded での幾何 Scenario (非交差・垂直センター・nil 時回復) を追加。tasks 4.4 追加 |
| 8 | prepareForReuse Scenario と first responder 保護の矛盾 | **採用** | Scenario の GIVEN を「first responder を含まない view」に限定 |
| 9 | Android テストの実行タスク欠如 | **採用** | #4 の対応 (Android タスク削除) で解消 |

採用 7 / 降格 2 / 未解決 0。相方判定 NEEDS_DISCUSSION の主要因 (#2 / #4) はコード検証の上で spec 修正により解消した。
