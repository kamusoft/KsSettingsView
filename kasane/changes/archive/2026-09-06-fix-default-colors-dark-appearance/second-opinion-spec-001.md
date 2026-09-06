# セカンドオピニオン: fix-default-colors-dark-appearance (spec-001)
**相方**: codex / **label**: so-spec-fix-default-colors-dark-appearance / **日付**: 2026-09-05 / **対象**: 提案一式 (proposal / design / specs 6 capability / tasks / ui/brief + 承認モック)
---
# レビュー結果: fix-default-colors-dark-appearance

**日付**: 2026-09-05  
**判定**: **NEEDS_DISCUSSION**  
**件数**: Critical 0 / Major 6 / Minor 3 / Suggestion 0

## サマリー

ダーク既定色をライブラリ所有にする方向と、Android の `Color.Unspecified` 採用は妥当です。ただし、利用者指定色の外観追随、Android ButtonCell の既定色、表示中の選択面について契約が未確定または矛盾しています。

さらに現行 Android コードには、構造更新によって未解決 Theme を失う具体的な実装トラップがあります。この状態では実装側の判断だけで契約が分岐するため、実装開始前の仕様修正が必要です。

静的レビュー指定に従い、ビルド・テストは実行していません。レビュー結果ファイルも作成していません。

## 照合した規約

- ソースコメント規約（always）
- 実行時挙動の検証規約
- Sample のプラットフォーム間一致
- ローカル開発環境と Sample の実行
- iOS source の Swift 6 言語モード適合確認
- MAUI 検証ホストの実行規約
- core/ADR-0009（accepted）
- android/ADR-0020（accepted）
- core/ADR-0030 は proposed のため、確定済み決定ではなく提案資料としてのみ参照

## 指摘事項

### [🟠 Major] 利用者指定色の外観追随契約が platform ごとの実際の仕組みと矛盾している

**該当箇所**: `proposal.md:12`、`specs/settings-view-ios-ui/spec.md:7`、`design.md:145`、`tasks.md:6`

**問題点**: 「明示指定された色は外観で変わらない」「未指定だけが追随する」という共通表現は、Android の固定 `Color` には当てはまりますが、iOS と MAUI では文字どおりには成立しません。

- iOS では、利用者が明示した dynamic `UIColor` 自体が trait に追随します。iOS spec 7 行目の「未指定の色だけが外観に追随する」は、proposal が案内する dynamic `UIColor` と矛盾します。
- MAUI の `AppThemeBinding` は外観変更時に新しい明示色をプロパティへ供給します。ところがその到達性は未確認の spike 扱いで、Requirement / Scenario には契約がありません。
- spike が失敗すると、本 change を既定色だけでなく「利用者 Theme のダーク対応全体」へ広げた根拠が崩れます。

**推奨修正**: 共通契約を「ライブラリは明示値を別の既定値へ置換しない」と言い換え、platform 別 Scenario を追加してください。

- iOS: 固定 `UIColor` は不変、dynamic `UIColor` は自身の dark 値へ解決される。
- Android: 固定 `Color` は不変。利用者が外観に応じて Theme を再構築・再適用した場合は新しい値になる。
- MAUI: `AppThemeBinding` の light/dark 値が facade → bridge → native まで届く。

MAUI の Scenario 前提を成立させる spike は、仕様承認前に解消するか、「失敗時は本提案を承認しない」ゲートとして明記する必要があります。

### [🟠 Major] Android の ButtonCell dark 既定値を現在の設計では運べない

**該当箇所**: `ui/mock/plan-a.html:59`、`design.md:88`、`design.md:116`、`android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EffectiveStyle.kt:497`

**問題点**: 承認モックは Android ButtonCell title の dark 既定を `#0A84FF` としています。しかし `lightTheme()` / `darkTheme()` が返す `Theme` には ButtonCell 専用フィールドがなく、現行の最終フォールバックは固定の `Theme.DEFAULT_BUTTON_TITLE_COLOR` です。

「解決済み Theme だけを全消費者へ渡す」Decision 6 では、ButtonCell 用の light/dark ロールを `EffectiveStyle` へ伝達できません。`cellAccentColor` に流用すると、利用者が accent だけ変更した場合に ButtonCell title まで変わり、現行の独立した4段フォールバック契約を壊します。Android spec と tasks のテスト列挙にも ButtonCell がありません。

**推奨修正**: ButtonCell 専用既定値をどこで外観解決し、`EffectiveStyle` へ渡すかを design で確定してください。少なくとも次を Scenario／テストへ追加します。

- dark で未指定 ButtonCell title が `#0A84FF` になる。
- `cellAccentColor` だけを明示しても、未指定 ButtonCell title は ButtonCell の外観既定を使う。
- Compose 経路と View 経路が同値になる。

### [🟠 Major] 構造更新経路が解決済み Theme を `themeBacking` へ逆流させる

**該当箇所**: `design.md:116`、`tasks.md:17`、`android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:522`、同 `:559`、同 `:606`、同 `:683`

**問題点**: 現行 `setRootDirect` は引数 Theme を `internalTheme` と `themeBacking` の両方へ代入します。さらに Full diff、Section 置換、可視性変更の各経路は `internalTheme` を渡しています。

提案どおり `internalTheme` が外観解決済みになると、これらの構造更新後に解決済み色が `themeBacking` へ保存されます。以後の uiMode 変更では全色が「明示指定」と判断され、未指定色が追随しなくなります。

**推奨修正**: 構造更新は Theme の利用者入力を変更しないことを design/spec に明記し、root-only 更新と Theme 取り込みを別経路にしてください。次の回帰 Scenario も必要です。

1. 一部だけ色を指定した Theme で表示する。
2. Full diff／Section 置換／可視性変更を適用する。
3. uiMode を変更する。
4. 明示色だけ維持され、未指定色は新しい外観へ追随する。

detach/attach 復元経路も同じ観点で検査対象に含めてください。

### [🟠 Major] MAUI Sample spec が存在しない Store / DSL 画面を前提にしている

**該当箇所**: `specs/samples-maui/spec.md:5`、`samples/maui/KsSettingsView.Sample.Maui/SampleScreen.cs:46`

**問題点**: MAUI spec は対象を「Store 方式 / DSL 方式 / 共通フィールド統合 / isVisible …」と列挙していますが、MAUI facade は Store / DSL を公開しておらず、MAUI Sample にもその2画面は存在しません。handbook の Sample パリティ例外にも、Store / DSL は native 2 platform のみと明記されています。

そのため Requirement の一部が到達不能で、tasks 8.3 の「同上」も実行対象を一意に決められません。

**推奨修正**: MAUI spec から Store / DSL を除き、実在する対象画面と、各画面で本当に未指定のまま使う色ロールを列挙してください。Store / DSL は iOS / Android 固有の検証対象として扱います。

### [🟠 Major] 外観切替時に表示中の選択面をどう扱うかが成果物間で不一致

**該当箇所**: `design.md:126`、`ui/brief.md:7`、`ui/brief.md:8`、`specs/settings-view-android-ui/spec.md:52`、同 `:86`

**問題点**: design は「表示中の選択面・dialog は対象外で、次回表示時に更新」と決めています。一方、brief は選択面を対象状態に含めたうえで、表示中の外観切替では未指定色が描き直されると読めます。外観解決 Requirement も選択面・カレンダーを「すべての描画箇所」に含めていますが、Configuration 変更 Requirement は行・list・装飾・Header / Footer しか列挙していません。

Picker sheet や calendar dialog を開いたまま外観が変わった場合、旧外観の面が新外観の背景上に残る可能性があります。

**推奨修正**: 次のいずれかを明示的に選び、Scenario を追加してください。

- 表示中の選択面も再配色する。
- 外観変更時に閉じる／作り直す。
- 表示中は旧配色を維持し、閉じた後の次回表示から追随する。

3案目を採るなら、brief の「表示中の外観切替」の対象外として明記する必要があります。

### [🟠 Major] 新しい公開 API 名が未確定のまま受け入れ仕様に入っている

**該当箇所**: `specs/settings-view-android-ui/spec.md:31`、`design.md:170`、`tasks.md:12`

**問題点**: `KsSettingsViewDefaults` は「仮称」で、tasks は実装時の改名を許しています。今回は公開 API の追加と既存 public 定数の internal 化を伴うため、名前は ABI・移行先・テストの一部です。このままでは、どの公開面なら仕様適合なのかをレビュー・検証で判定できません。

また `design.md:172` の accent 値も Open Question のままですが、承認済み plan A により `#0A84FF` へ解決済みです。

**推奨修正**: 公開 object と factory の最終名を実装前に確定してください。名前を挙動仕様から外すのであれば、公開 API 設計を別の明示的な受け入れ項目に固定します。解決済みの accent 問題は Open Questions から削除してください。

### [🟡 Minor] 「全消費者の入力が specified」というテスト条件が設計と矛盾する

**該当箇所**: `tasks.md:43`、`design.md:116`、`specs/settings-view-android-ui/spec.md:52`

**問題点**: design/spec は valueText、hintText、placeholder、`sectionBorderColor` を意図的に `Unspecified` のまま後段へ渡します。一方、task 7.4 は「全消費者の入力が specified」としており、文字どおりには成立しません。これを満たそうとすると、既存フォールバックや透明枠線契約を壊すおそれがあります。

**推奨修正**: 「`toArgb()` 等の実値変換点へ到達する時点では specified」と限定し、sentinel を理解するフォールバック／透明解決経路は例外として列挙してください。

### [🟡 Minor] Bridge の CellStyle 変換に対応する回帰テストがない

**該当箇所**: `specs/maui-bridge/spec.md:5`、`tasks.md:27`、`tasks.md:45`

**問題点**: Requirement と実装 task は Theme と CellStyle の両方を `Unspecified` へ変換するとしていますが、Scenario と test task は全 null の Theme DTO しか検査しません。`KsBridgeCellStyle.resolve()` は今回、nullable から非 nullable への書き換えが必要な独立経路です。

**推奨修正**: 全 null と一部明示値を持つ `KsBridgeCellStyle` DTO の変換テストを追加し、未指定だけが `Color.Unspecified`、明示 ARGB は保持されることを検査してください。

### [🟡 Minor] ライト外観の画素一致に再現可能な比較手順がない

**該当箇所**: `tasks.md:55`

**問題点**: 「本 change 前と画素一致」とありますが、light の事前画像、比較対象コミット、OS・端末・scale・フォント等の固定条件、差分許容値が定義されていません。実装後だけでは「本 change 前」の比較対象を一意に復元できません。

**推奨修正**: 同一 Simulator / Emulator、OS、画面状態で変更前後を A/B 撮影する手順と基準コミットを記録してください。完全な画素一致が不要なら、色ロールの値テストと目視によるレイアウト非回帰に分離する方が安定します。

## アクションプラン

1. 利用者指定色と表示中の選択面について、platform 別契約を確定する。
2. MAUI `AppThemeBinding` の到達性を確認し、成立する Scenario を仕様へ追加する。
3. Android ButtonCell の独立した light/dark 既定値の運搬方式を設計する。
4. `themeBacking` を構造更新から保護する契約と回帰 Scenario を追加する。
5. MAUI Sample の対象画面を実在する画面へ修正する。
6. Android の公開 Defaults API 名を確定する。
7. test tasks の `Unspecified` 条件、Bridge CellStyle、ライト A/B 手順を修正する。
8. 修正後に独立提案レビューを再実施する。


## 突き合わせ結果

ホスト側の自己レビュー (2 周) はこれらを検出していない。すべて相方のみの指摘で、該当箇所と実害シナリオが特定されているため採用した。

| # | 指摘 | 採否 | 反映 |
|---|---|---|---|
| Major 1 | 利用者指定色の追随契約が platform の仕組みと矛盾 | 採用 | proposal What Changes の表現を「明示値を別の既定値へ置き換えない」に改め、iOS spec に dynamic 色の Scenario、maui-bridge spec に AppThemeBinding の Scenario を追加。spike を承認ゲート化 (tasks 0.1 / proposal Impact) |
| Major 2 | Android ButtonCell の dark 既定を運べない | 採用 | design Decision 6 に外観引数でパレットの ButtonCell ロールから選ぶ設計を追記。Android spec に Requirement 文と Scenario、tasks 2.3 / 7.4a を追加 |
| Major 3 | 構造更新経路が解決済み Theme を `themeBacking` へ逆流 | 採用 (コードで確認: `setRootDirect` が `themeBacking` へ代入し、Full diff / Section 置換 / 可視性変更が `internalTheme` を渡す) | design Decision 6 と Risks、Android spec の SHALL NOT と回帰 Scenario、tasks 2.2 / 7.3 |
| Major 4 | MAUI Sample spec が存在しない Store / DSL 画面を前提 | 採用 | samples-maui spec と tasks 8.3 を実在画面 (共通フィールド統合 / isVisible / Section 装飾) に修正 |
| Major 5 | 表示中の選択面の扱いが成果物間で不一致 | 採用 (3 案目「表示中は旧配色維持、次回表示から追随」を採る) | design Decision 7、Android spec の SHALL と Scenario、brief の状態記述 |
| Major 6 | 公開 API 名が仮称のまま | 採用 (`KsSettingsViewDefaults` で確定) | design Decision 5 / Open Questions、Android spec、tasks 1.4。accent の Open Question も削除 |
| Minor 1 | 「全消費者の入力が specified」が設計と矛盾 | 採用 | tasks 7.4 を実値変換点の条件と例外経路の列挙に修正 |
| Minor 2 | Bridge の CellStyle 変換の回帰テストがない | 採用 | maui-bridge spec に Scenario、tasks 7.6 |
| Minor 3 | ライトの画素一致に再現手順がない | 採用 | tasks 8.4 を基準コミット + 同条件 A/B 撮影 + 代替 (値テスト + 目視) に修正 |

未解決: なし。降格: なし。相方が挙げた「修正後に独立提案レビューを再実施する」はオーナー判断に委ねる (ksn-propose の自己レビューは 2 周で打ち切りの規律)。
