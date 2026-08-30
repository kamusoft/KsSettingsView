# セカンドオピニオン: add-entrycell-placeholder-color (spec-001)

**相方**: codex / **label**: so-spec-add-entrycell-placeholder-color / **日付**: 2026-08-27 / **対象**: 提案一式 (proposal.md / specs/ 9 capability / tasks.md / ui/)

---

# レビュー結果: add-entrycell-placeholder-color

**日付**: 2026-08-27
**判定**: **NEEDS_DISCUSSION**
**件数**: Critical 0 / Major 7 / Minor 1 / Suggestion 0

## サマリー

機能の基本方針と4段解決は既存 concepts と整合しています。一方、同時成立しない承認モック、無効状態と Android platform default の未確定な意味論、変更検知・公開 API・Binding の実装タスク漏れがあり、このままでは仕様を満たさない実装やビルド不能が生じます。

静的レビューのため、ビルド・テストは実行していません。依頼どおりレビュー結果ファイルも作成していません。

## 指摘事項

### [🟠 Major] 複数ドメイン変更なのに `domain: core` になっている

**該当箇所**: `proposal.md:31`
**問題点**: 変更は core / iOS / Android / MAUI にまたがっています。`kasane/concepts/rules.md:16` は複数ドメインの proposal を `cross` とする規約です。`core` のままでは、実装時に Android・MAUI の domain-skills が解決されず、蒸留先の判断も誤ります。
**推奨修正**: `domain: cross` に変更し、実装対象ドメインとして core / ios / android / maui をオーケストレーション時に解決すること。

### [🟠 Major] 承認モックの Theme 行と OS 既定行は同一 SettingsView 内で同時成立しない

**該当箇所**: `ui/mock/placeholder-color.html:37`、`ui/mock/placeholder-color.html:38`、`specs/maui-core/spec.md:7`、`specs/maui-core/spec.md:9`
**問題点**: モックは同じ画面に「未指定（OS 既定）」と「Theme 一括指定」を並べています。しかし `cellPlaceholderColor` は画面内の全 EntryCell に適用されるため、Theme 指定中の未指定行も Theme 色になり、モックの状態は再現不能です。各 sample spec は追加行を1行と定めており、モックとの照合対象も一致しません。
**推奨修正**: 次のいずれかを選んで brief・mock・sample spec・tasks を揃えること。

- Theme 未指定画面で「OS 既定／Cell 個別／入力済み」を示し、Theme 適用は別画面または別状態にする。
- モックから Theme 行を外し、Theme 経路は自動テストで検証する。
- 同一画像内でも、別々の SettingsView／状態であることが明確な構成へ分ける。

### [🟠 Major] 無効状態と Android platform default の関係が確定していない

**該当箇所**: `specs/cell-types-input/spec.md:7`、`specs/settings-view-android-ui/spec.md:7`、`ui/brief.md:9`
**問題点**: 一方では「無効状態でも placeholder は解決済み色のまま」、他方では未指定時にホストの `android:textColorHint` を維持するとしています。`textColorHint` は状態別 `ColorStateList` になり得るため、無効時の platform 色を維持するのか、通常時の単色へ固定するのかが決まっていません。実装によっては未指定時の OS 外観か、無効時不変のどちらかを破ります。
**推奨修正**: 少なくとも次を別々に規定してください。

- Cell / CellStyle / Theme の明示色は無効状態でも同じ色か。
- 全段未指定ではホストの状態別 hint 色をそのまま維持するか。
- 明示色から null へ戻した際、元の `ColorStateList` を復元するか。

各状態の Scenario も追加してください。

### [🟠 Major] 変更検知に必要な手動等価性更新が tasks から漏れている

**該当箇所**: `tasks.md:6`、`tasks.md:13`
**問題点**: iOS の `Theme` と `CellStyle` は手動 `==`、CellStyle はさらに `hashCellStyle` の列挙を持ちます（`ios/Sources/KsSettingsViewUI/Theme.swift:222`、`ios/Sources/KsSettingsViewUI/CellStyle.swift:83`、同`:112`）。Android `EntryCell` も data class ですが、`equals` / `hashCode` を手動 override しています（`android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EntryCell.kt:65`）。ここへ新フィールドを加えないと、placeholder 色だけの変更が同値扱いとなり、内容更新・Theme 更新が発火しません。
**推奨修正**: tasks に次を明記し、色だけを変えたとき非同値になるテストを追加すること。

- iOS: `Theme.==`、`CellStyle.==`、`hashCellStyle`
- Android: `EntryCell.equals`、`EntryCell.hashCode`

### [🟠 Major] Android Compose DSL と iOS Binding 定義が実装対象に入っていない

**該当箇所**: `tasks.md:13`、`tasks.md:22`、`tasks.md:30`
**問題点**:

- Android の公開 Compose DSL には EntryCell overload が2つありますが、`placeholderColor` 追加タスクがありません（`android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/InputCellDsl.kt:33`、同`:69`）。data class へ追加するだけでは通常の Compose DSL や sample から指定できません。
- iOS Binding は `ApiDefinition.cs` を入力とする手書き Binding です（`maui/macios/KsSettingsView.Binding.iOS/KsSettingsView.Binding.iOS.csproj:22`）。`KsBridgeEntryCell` と `KsBridgeTheme` のプロパティを Swift 側だけへ追加しても、C# Gateway から参照できずコンパイルできません。

**推奨修正**: Android の両 DSL overload とそのテスト、ならびに `maui/macios/KsSettingsView.Binding.iOS/ApiDefinition.cs` の Entry / Theme 定義更新を明示タスクに追加すること。

### [🟠 Major] Scenario に対するテスト計画が不足している

**該当箇所**: `tasks.md:22`、`tasks.md:23`、`tasks.md:31`
**問題点**: 現在の計画は MAUI の変換パスと null 輸送だけです。以下の Scenario に対応するテストが明示されていません。

- `specs/maui-cells/spec.md:19`: 表示中の per-cell 変更
- `specs/maui-core/spec.md:19`: 表示中の Theme 変更
- `specs/maui-bridge/spec.md:9`: 両 native Bridge の Entry DTO resolve
- `specs/maui-bridge/spec.md:14`: 両 native Bridge の Theme resolve
- 明示色から null へのクリアと fallback
- 無効状態の placeholder 契約

MAUI の fake gateway テストだけでは、Swift/Kotlin Bridge の `makeCell` / Theme resolve は検証できません。
**推奨修正**: Scenario-to-test 対応を tasks に明記し、iOS Bridge Tests、Android bridge tests、MAUI public property shape・dirty-set／Theme 更新テストを追加すること。

### [🟠 Major] 「破壊的変更なし／追加 API のみ」が Impact と矛盾する

**該当箇所**: `proposal.md:23`、`proposal.md:24`
**問題点**: 直後に、Android 利用者の入力文字色が変わることを明記しています。また Kotlin data class の公開コンストラクタ変更は、既定引数付きでも既存バイナリとの互換性を別途評価する必要があります。「追加 API のみ」は正確ではありません。
**推奨修正**: source / binary / behavioral compatibility を分けて記載し、少なくとも「Android に意図した挙動変更あり」と明示してください。これを互換修正として許容するか、別 change／リリース単位へ分けるかも決定が必要です。

### [🟡 Minor] placeholder 文字列が null／空の場合の色指定挙動が未定義

**該当箇所**: `specs/settings-view-ios-ui/spec.md:7`
**問題点**: `placeholder` 自体は optional ですが、色だけが指定された場合に `attributedPlaceholder` をどう扱うか決まっていません。素直に実装すると optional 文字列の扱いで分岐漏れが起きます。
**推奨修正**: null なら attributed/plain とも placeholder なし、空文字列なら空表示とする等を明記し、色指定ありの null ケースをテストしてください。

## アクションプラン

1. `domain`、Android 無効時の platform default、互換性評価を決定する。
2. Theme の適用範囲と同時成立するよう UI mock・sample Scenario を修正する。
3. iOS/Android の手動等価性、Compose DSL、iOS Binding 定義を tasks に追加する。
4. 全 Scenario とテストの対応表を補完し、クリア遷移・Bridge native resolve・表示中更新を検証対象にする。
5. 修正後に再度 spec-review を行う。

---

## 突き合わせ結果

ホスト側自己レビュー (2周、チェックリスト通過) との突き合わせ。相方の全指摘についてコードで根拠を検証した (rules.md:16 の cross 規約・iOS `Theme.==` / `CellStyle.==` / `hashCellStyle`・Android `EntryCell` の手動 equals/hashCode・Compose DSL の EntryCell 2 overload・`maui/macios/KsSettingsView.Binding.iOS/ApiDefinition.cs` の実在をすべて確認)。

| # | 指摘 | 採否 | 対応 |
|---|---|---|---|
| 1 | domain: core → cross | **採用** (相方のみ・根拠強: rules.md:16 規約に実在) | proposal.md を `domain: cross` へ修正 |
| 2 | mock の Theme 行と OS 既定行が同時成立しない | **採用** (相方のみ・根拠強) | 推奨案2を採用: mock から Theme 行を外し (サンプル構成 = Theme 未指定 + Cell 個別1行に一致させる)、Theme 経路は自動テストで検証。mock 再承認をユーザーへ依頼 |
| 3 | 無効状態 × Android ColorStateList の意味論未確定 | **採用** (相方のみ・根拠強) | Android spec に3点 (明示色は無効でも同色 / 全段未指定は状態別 hint 色を維持 / 明示→未指定は既定 ColorStateList 復元) を SHALL + Scenario で追加 |
| 4 | 手動等価性 (iOS ==/hash、Android equals/hashCode) の列挙漏れ | **採用** (相方のみ・根拠強: 全箇所実在確認) | tasks に明記 + 非同値テストを追加 |
| 5 | Compose DSL 2 overload / iOS Binding ApiDefinition.cs の漏れ | **採用** (相方のみ・根拠強: 両方実在確認。ホスト側事前調査の見逃し) | tasks に追加 |
| 6 | Scenario-to-test 対応の不足 | **採用** (相方のみ・根拠中〜強) | tasks のテスト項目を Scenario 対応で拡充 (Bridge native resolve・表示中更新・クリア遷移・無効状態) |
| 7 | 「破壊的変更なし」と Impact の矛盾 | **採用** (相方のみ・根拠強) | Impact を source / binary / behavioral に分けて書き直し。挙動変更の同梱は探索でユーザー決定済みのため別 change 分離はしない |
| 8 | (Minor) placeholder null/空 + 色指定の挙動未定義 | **採用** | iOS spec に null / 空文字列の扱いを SHALL + Scenario で追加 |

採用 8 / 降格 0 / 未解決 0。判定 NEEDS_DISCUSSION の論点はすべて上記の対応で解消 (mock 再承認のみユーザー確認待ち)。
