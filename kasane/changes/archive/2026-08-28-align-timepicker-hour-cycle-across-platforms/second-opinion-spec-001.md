# セカンドオピニオン: align-timepicker-hour-cycle-across-platforms (spec-001)

**相方**: codex / **label**: so-spec-align-timepicker-hour-cycle / **日付**: 2026-08-28 / **対象**: kasane/changes/align-timepicker-hour-cycle-across-platforms/ の提案一式 (proposal.md / specs/ 8 capability / tasks.md)

---

# レビュー結果: align-timepicker-hour-cycle-across-platforms

**日付**: 2026-08-28
**判定**: **NEEDS_DISCUSSION**

## サマリー

ADR-0028 の基本方針は各デルタスペックで概ね一貫しています。一方、公開 API の輸送経路・更新検知・iOS の Locale 契約に実装不能または未検証になり得る穴があり、このまま実装へ進むべきではありません。

静的レビューのみ実施し、ビルド・テスト・ファイル書き込みは行っていません。

## 指摘事項

### [🟠 Major] 変更級と domain が Kasane 規約に一致しない

**該当箇所**: `proposal.md:15`, `proposal.md:28`, `proposal.md:30`, `proposal.md:34`
**問題点**: 8 capability、core・iOS・Android・MAUI を横断する変更を `M` / `domain: core` としています。Kasane の規約では複数 capability 横断は L、複数 domain は `cross` です。現状では platform 固有スキルの解決、design/spec-review、蒸留先の判断が誤ります。また UI を変更する L/M 級で `ui/` を省略する説明も規約上の例外に該当しません。
**推奨修正**: `級: L`、`domain: cross` として再整理し、design と最小限の UI brief／視覚検証基準を追加してください。

### [🟠 Major] Android Compose の公開 TwoWay API が実装範囲から漏れている

**該当箇所**: `tasks.md:5`, `tasks.md:28`
**問題点**: proposal は Compose を対象に含めていますが、Android のタスクは native `TimePickerCell` しか扱っていません。現行 Compose API は `android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/InputCellDsl.kt:236` に独立した明示引数の overload を持つため、ここを変更しないと `is24Hour = false` を渡せません。Android サンプルもこの overload を使用しており、task 5.1 をそのまま実装できません。
**推奨修正**: Compose overload への引数追加・native Cell への透過と、そのテストを tasks と Scenario に追加してください。

### [🟠 Major] iOS Binding assembly の API 定義がタスクから漏れている

**該当箇所**: `tasks.md:17`, `tasks.md:23`
**問題点**: iOS Bridge DTO と MAUI gateway の間には、手書きの `maui/macios/KsSettingsView.Binding.iOS/ApiDefinition.cs:497` があります。Swift DTO にだけプロパティを追加しても C# 側へ露出せず、gateway から設定しようとするとコンパイルできません。
**推奨修正**: `ApiDefinition.cs` に nullable な `is24Hour` の binding を追加するタスクと、Binding→gateway→native の契約テストを明記してください。

### [🟠 Major] 構築後の変更伝播と値同一性が仕様化されていない

**該当箇所**: `specs/cell-types-input/spec.md:7`, `specs/maui-cells/spec.md:9`, `tasks.md:24`
**問題点**: Scenario は初期構築時しか検証しません。しかし現行コードには更新を成立させる明示的な参加箇所があります。

- Android の手書き `equals/hashCode`: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/TimePickerCell.kt:42`
- iOS の手書き `Equatable/Hashable`: `ios/Sources/KsSettingsViewUI/TimePickerCell.swift:104`
- iOS の Cell 再構築 helper: 同ファイル `:136`
- MAUI の明示的な `AffectsSnapshot`: `maui/KsSettingsView.Maui/TimePickerCell.cs:99`

これらへの追加を忘れても、現在予定されている「直接生成→選択面を開く」「snapshot を1回作る」テストは通ります。その場合、同一 ID の Cell や表示済み MAUI Cell で `true → false` を変更しても反映されません。Swift の modifier 適用で `false` が既定値へ戻る可能性もあります。
**推奨修正**: 「表示済み／同一 ID の Cell の `is24Hour` だけを変更すると、次回提示が新しい時制になる」Scenario を3面へ追加してください。equals/hash、Swift Binding initializer・各 modifier、MAUI `AffectsSnapshot` のテストも tasks に明記してください。

### [🟠 Major] iOS の Locale 契約が矛盾気味で、受け入れ判定できない

**該当箇所**: `proposal.md:21`, `proposal.md:27`, `specs/ios-timepicker/spec.md:9`, `tasks.md:12`
**問題点**: proposal は「AM/PM 表記は端末 Locale 由来のまま」としつつ、時制強制の Locale 上書きによる副作用を未解決リスクとして残しています。iOS spec は「自前翻訳しない」としか規定していないため、固定 Locale で時制を強制して表記言語を変える実装も文面上は合格します。また「副作用がないことを確認」は対象 Locale・端末設定・期待文字列がなく、テスト可能な基準ではありません。
**推奨修正**: 次のどちらを契約にするか確定してください。

- 言語・地域・AM/PM 表記は端末 Locale を保ち、hour-cycle だけを上書きする。
- 時制強制に伴う表記変更を許容し、proposal の Non-Goals を変更する。

前者なら、非英語 Locale と端末設定が指定値と逆のケースを含む決定的なテスト seam／検証マトリクスを定義してください。

### [🟡 Minor] Sample パリティの一致対象が具体化されていない

**該当箇所**: `specs/samples-ios/spec.md:7`, `specs/samples-maui/spec.md:7`
**問題点**: 「3 platform でパリティ」とだけあり、title、初期値、pickerTitle、行位置などが列挙されていません。現行 Android は `title = "就寝"`、初期値 `22:15`、`pickerTitle = "就寝時刻"` ですが、iOS／MAUI が異なるデータで実装されても各個別 Scenario は通り得ます。
**推奨修正**: Android の既存デモを基準として一致させる全フィールドを Requirement に列挙するか、3サンプルの文言・初期値・引数・並び順を比較するパリティ検査を task 5.4 に追加してください。

## アクションプラン

1. 変更級を L、domain を cross として再整理する。
2. Compose overload と iOS Binding assembly を明示的な実装対象に加える。
3. 同一性・modifier・`AffectsSnapshot` を含む更新後 Scenario を追加する。
4. iOS の Locale／AM-PM 表記契約を決定し、検証マトリクスを定義する。
5. Sample の完全一致条件を具体化する。

**件数**: Critical 0 / Major 5 / Minor 1 / Suggestion 0

---

## 突き合わせ結果

ホスト側の自己レビュー (チェックリスト2周・全項目クリア) と突き合わせ、コードで根拠を検証して採否を決定した。

| # | 指摘 | 採否 | 根拠 |
|---|---|---|---|
| 1a | domain を cross へ | **採用** | ksn-propose の規約 (「横断・複数該当は cross」) と前例 add-entrycell-placeholder-color (M級・domain: cross) に一致。proposal.md を修正済み |
| 1b | 変更級を L へ | **裁定: M 維持** (2026-08-28 オーナー) | ksn-core の文言 (「複数能力横断 = L」) は相方の読みとして正当だが、同形の前例 (add-entrycell-placeholder-color: 8 capability・3面) が M で運用済み。L との実質差分 design.md は ADR-0028 と重複し、外部スペックレビューは本レビューで実施済みのため M を維持 |
| 1c | ui/ (brief・視覚検証基準) の追加 | **降格** | 本変更は既存ホイール UI の系列切替のみで新規視覚パラメータ・レイアウト判断がない (前例の ui/ は「色」という視覚要素があった)。視覚確認は task 5.4 で担保。proposal に省略理由を明記済み |
| 2 | Compose TwoWay DSL の overload 漏れ | **採用** | `InputCellDsl.kt:236` に明示引数の拡張関数が実在。android-timepicker に Requirement 追加、tasks 1.5 追加 |
| 3 | iOS Binding assembly (ApiDefinition.cs) 漏れ | **採用** | `ApiDefinition.cs:497` に手書き binding が実在。maui-bridge に SHALL 追記、tasks 3.3 追加 |
| 4 | 更新伝播・値同一性の未仕様化 | **採用** | 手書き equals/hashCode (Android)・Equatable/hash と再構築 helper (iOS)・`AffectsSnapshot` (MAUI) が実在。cell-types-input / maui-cells に Scenario 追加、tasks 1.4 / 2.1 / 2.3 / 4.1 / 4.3 へ反映 |
| 5 | iOS Locale 契約の曖昧さ | **採用** | 契約を前者 (hour cycle のみ上書き・表記の言語は端末 Locale 維持) に確定 — proposal の Non-Goals (AM/PM 表記の解決方式は現行維持) と Android 側契約に整合する側。ios-timepicker に SHALL + Scenario 追加。検証マトリクスは「非英語 Locale での表記維持テスト」に縮約 (tasks 2.3) |
| 6 | Sample パリティの具体化 | **採用 (Minor)** | Android 既存デモ (就寝 / 22:15 / 就寝時刻 — 実値確認済み) を基準として samples-ios / samples-maui の Requirement に明記 |

確定 0 / 採用 5 (+Minor 1) / 降格 1 / 裁定済み 1 (変更級: オーナー裁定で M 維持)
