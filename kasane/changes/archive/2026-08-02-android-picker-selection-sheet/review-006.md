# レビュー結果: android-picker-selection-sheet (006 回目)

**日付**: 2026-08-02
**判定**: APPROVED

**スコープ**: 本レビューは **review-005 (APPROVED) 後に追加された配色修正 (修正サイクル6) の差分に限定**する。確認対象は (a) 配色変更の配線、(b) 輝度導出の残骸・参照切れ、(c) Android/iOS のサンプル値の同値性、(d) 意図しない巻き込み変更の4点。過去レビューで解消済みの論点 (高さ制約の対称化・ヘッダー配置・アクセシビリティ・上限挙動等) は再確認していない。オーナー裁定済みの配色方針そのもの (確定ラベルへの `Theme.backgroundColor` 採用・サンプル `cellTitleColor` への `#555555` 採用) は指摘対象外として扱い、判断材料となる実測値のみ末尾に客観記録する。

## サマリー

4つの確認事項はいずれも問題なし。確定ボタン文字色は `PickerSheetStyle.onAccentTextColor` を経由して `Theme.backgroundColor` へ正しく配線され、輝度導出の残骸 (ヘルパー・定数・未使用 import・旧挙動を説明するコメント) はコード・テスト・ドキュメントのいずれにも残っていない。Android/iOS のサンプル値は同一 RGBA (`#555555`) で、移植元 AiForms の `DeepTextColor` と一致することも原典で確認した。差分は指定4ファイル + 再撮影スクショ2枚に収まっており、巻き込み変更はない。指摘は承認 mock との配色差が deviation.md に未記録である点 (Minor) と、Suggestion 2件。

**検証した客観事実**:

- `cd android && ./gradlew test --rerun-tasks` → BUILD SUCCESSFUL。`*/build/test-results/*/TEST-*.xml` 92 ファイルを集計し **1192 tests / 0 failures / 0 errors / 0 skipped** を確認 (テスト実行規約に従い件数まで確認。review-005 時点の 1188 から +4)
- `cd samples/android && ./gradlew assembleDebug` → BUILD SUCCESSFUL
- `xcodebuild -project samples/ios/KsSettingsViewSample.xcodeproj -scheme KsSettingsViewSample -destination 'generic/platform=iOS Simulator' build` → **BUILD SUCCEEDED**
- 主実装の Kotlin 警告なし (`PickerSelectionSheet.kt` に対する `w:` 出力ゼロ = 未使用の private メンバー・import は残っていない)
- 移植元原典を直接確認: `AiForms.Maui.SettingsView/Sample/Views/MainPage.xaml:24` の `<Color x:Key="DeepTextColor">#555555</Color>` と `:37` の `<Setter Property="CellTitleColor" Value="{StaticResource DeepTextColor}" />`

## 確認事項ごとの結果

### (a) 配色変更の配線 — 問題なし

`PickerSelectionSheet.kt:76` で `onAccentTextColor = theme.backgroundColor.toArgb()` を解決し、`:334` の `confirmView.setTextColor(sheetStyle.onAccentTextColor)` が唯一の消費点。ピル背景は `:348` で `sheetStyle.accentColor` のままで、文字色と背景色の出所が別になったことによる取り違えはない。

`PickerSheetStyle` の他フィールドとの整合も確認した — シート面 `theme.cellBackgroundColor` / 区切り線 `theme.separatorColor` / ripple `theme.selectedColor` / 候補行文字 `effective.titleColor` はいずれも変更されておらず、今回の変更が `onAccentTextColor` の1フィールドに閉じている。KDoc も `:43` (property) と `:65-66` (解決規則の説明) の2箇所が新しい契約「強調色で塗った面を list 全体の下地色 `Theme.backgroundColor` で抜く」へ更新済みで、実装と一致する。

`cancelView` は従来どおり `accentColor` (`:305`)、`titleView` は `itemTextColor` (`:324`) で、確定ラベルだけを狙った変更になっている。

### (b) 輝度導出の残骸・参照切れ — 検出なし

- リポジトリ全体で `luminance` / `calculateLuminance` / `輝度` / `コントラスト` / `白または黒` を検索し、本変更に関係する残骸はゼロ (唯一の `輝度` ヒットは無関係な `changes/archive/2026-08-01-fix-entrycell-ime-composition/exploration.md` のスクショ解析手法の記述)
- `androidx.core.graphics.ColorUtils` の import (`:26`) は残っているが、`:250` の `setAlphaComponent` (ドラッグハンドル色の減光) で現に使われており、輝度導出の残骸ではない。`MaterialColors` (`:31`) も同じ箇所で使用中
- 未使用定数の残留なし (コンパイラ警告ゼロで裏付け)。`onAccentTextColor` の参照は定義 `:53` / 解決 `:76` / 消費 `:334` / KDoc `:43` の4箇所のみで、参照切れも重複解決もない
- テスト側も旧挙動 (accent の輝度に応じて白/黒) を固定するアサートは残っていない。新テスト `確定ボタンの文字色は Theme の backgroundColor で描画される` (`PickerSelectionSheetTest.kt:484-499`) は `backgroundColor = #F2EFE6` / `cellAccentColor = #CC9900` という互いに異なる値を与えて `confirmView.currentTextColor == #F2EFE6` を検証しており、旧実装 (白 or 黒を導出) では通らない = 退行を実際に検出できる。隣の `取消ボタンの文字色と選択印は強調色で描画される` (`:501-518`) が同じ Theme で accent 側を固定しており、2色が取り違えられた場合も片方が必ず落ちる

### (c) Android / iOS のサンプル値 — 同値、原典とも一致

| | Android (`SampleTheme.kt`) | iOS (`SampleTheme.swift`) |
|---|---|---|
| 定義 | `:54` `val mauiDeepText: Color = Color(0xFF555555)` | `:39` `static let mauiDeepText = UIColor(red: 0x55/255.0, green: 0x55/255.0, blue: 0x55/255.0, alpha: 1.0)` |
| 実 RGBA | (85, 85, 85, 255) | (85, 85, 85, 255) |
| 消費 | `:102` `cellTitleColor = mauiDeepText` | `:81` `cellTitleColor: mauiDeepText` |
| コメント | `DeepTextColor（#555555）。Cell タイトルの文字色。` | 同一文言 |
| 定義位置 | `mauiTitleText` の直後 | `mauiTitleText` の直後 |

`cross/conventions/sample-parity.md` が要求する「同一の RGBA を Sample 共通の定義 (`SampleTheme`) に置いて両 platform から参照する」を満たしている。platform 固有の semantic color は使っていない。`samples/maui` は README のみの未実装であり、同規約が「現時点の対象は `samples/ios` / `samples/android` の2つ」と明記しているため追随対象外 — 片側先行の未追随は発生していない。

移植元原典との一致も確認済み (上記「客観事実」)。原典では `TitleTextColor (#CC9900)` と `DeepTextColor (#555555)` が別トークンで、`CellTitleColor` に当たるのは後者であるため、今回の置き換えは原典への正しい追随になっている。旧値 `mauiTitleText (#CC9900)` は両 platform とも ButtonCell の `CellStyle(titleColor)` (`BasicCellsDemoScreen.kt:184` / `BasicCellsDemoView.swift:168`) で現役のため、削除せず残したのは妥当。

### (d) 意図しない巻き込み変更 — なし

作業ツリーの全ソース・ドキュメントの mtime を走査した結果、review-005 出力 (15:51) 以降に更新されたのは以下だけで、コンテキストパッケージが宣言した範囲と完全に一致する:

| 時刻 | ファイル |
|---|---|
| 17:02 | `PickerSelectionSheet.kt` / `PickerSelectionSheetTest.kt` |
| 17:06 | `samples/android/.../SampleTheme.kt` |
| 17:07 | `samples/ios/KsSettingsViewSample/SampleTheme.swift` |
| 17:08-17:09 | `ui/verification/single-select-sheet.png` / `multi-select-sheet.png` (再撮影) |

足場アーティファクト (proposal / exploration / specs / brief / mock) はいずれも提案記録コミット以降 未変更。`tasks.md` の差分はチェックボックスのみで、グループ4 (4.1 視覚照合 / 4.2 実機確認) は未チェック = 虚偽チェックなし。`decisions/android/0004` の差分は選択印描画の追補1件で、review-005 時点から増えていない (今サイクルの巻き込みではない)。

なお `PickerSelectionSheet.kt:110-113` のクラス KDoc は review-005 の Suggestion どおり「折り畳み表示のあいだ制約し、折り目へ戻ると再び制約される」へ更新されており、同ファイル内の高さ挙動の説明3箇所の食い違いは解消している。

## 指摘事項

### [🟡 Minor] 承認 mock が定める確定ラベルの色 (#fff) と実装 (`Theme.backgroundColor`) の差が deviation.md に記録されていない

**該当箇所**: `kasane/changes/android-picker-selection-sheet/deviation.md` (記載なし) / 対応する実装は `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/PickerSelectionSheet.kt:76`

**問題点**: 承認済みモック `ui/mock/plan-b.html:50` は確定ボタンを `.btn-done { color: #fff; background: var(--accent) }` と定義しており、`approved.png` でも OK ラベルは白で描かれている。`ui/brief.md:34` は「生値はここに書かない。具体レイアウトは mock が正」と明示しているため、確定ラベルの文字色については mock が見た目の正となる。今回の変更はこれを「固定の白」から「`Theme.backgroundColor` から導出」へ変えており、承認済み足場との差分が発生している。オーナー指示による合意済みの変更であり実装上の問題はないが、deviation.md には未記録である。

差分が実質的なのは、これが単なる色味の調整ではなく**新しいスタイリング契約の追加**だからである — 「強調色の上に載せる文字色は `Theme.backgroundColor` が決める」というルールは、デルタスペック (`specs/settings-view-android-ui/spec.md` に該当 Requirement なし)・`concepts/core/styling/` のいずれにも存在しない。ライブラリ利用者から見ると、`Theme.backgroundColor` を変えると PickerCell 選択面の OK ラベル色が連動して変わるという非自明な結合であり、記録が残らないと蒸留時に concepts / ADR へ拾い上げる根拠を失う。

**推奨修正**: deviation.md に1項目追加する。「確定ボタンの文字色: mock は `#fff` 固定 → オーナー指示により `Theme.backgroundColor` から導出 (強調色で塗った面を list 下地色で抜く配色)。理由と、`Theme.backgroundColor` が選択面の確定ラベル色を規定するという新しい結合を明記」。実装・テストの修正は不要。

### [🔵 Suggestion] `ui/verification/` の旧配色スクショ9枚が現行実装と一致しない

**該当箇所**: `kasane/changes/android-picker-selection-sheet/ui/verification/` (16:17-16:27 撮影の9枚)

**問題点**: 今サイクルで再撮影されたのは `single-select-sheet.png` / `multi-select-sheet.png` の2枚のみで、残る9枚は配色変更前の状態を写している。特に `multi-select-limit-reached.png` / `multi-select-limit-tap-rejected.png` は OK ボタンを含み、ラベルが**黒**で写っている (現行は `#F2EFE6`) ほか、Cell タイトルも旧 `#CC9900` (金) のままで、現行の `#555555` と明確に異なる。tasks 4.1 (視覚照合) はまだ未チェックであり、この状態で証跡として確定すると、後から見た人がどちらが正なのか判断できない。

**推奨修正**: tasks 4.1 / 4.2 を実施する際に、OK ボタンまたは Cell タイトルが写る証跡は現行配色で撮り直す。挙動確認が主目的で配色が判定に関わらないもの (landscape の高さ・スクロール系) は、撮影時点が配色変更前である旨を verification 側に一行残せば足りる。

### [🔵 Suggestion] `mauiTitleText` と `mauiHeaderText` が同値 (#CC9900) の二重定義になっている

**該当箇所**: `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/SampleTheme.kt:48-51` / `samples/ios/KsSettingsViewSample/SampleTheme.swift:30-37`

**問題点**: 両 platform とも原典の `TitleTextColor (#CC9900)` に対して `mauiHeaderText` (ヘッダ文字色) と `mauiTitleText` (ButtonCell の `CellStyle(titleColor)` 用) の2定数を持つ。二重定義自体は今サイクル以前からあるが、今回 `cellTitleColor` が `mauiTitleText` から離れたことで `mauiTitleText` の残る用途が ButtonCell 1箇所だけになり、「原典のどのトークンに対応するどの用途か」が名前から読み取りにくくなった (`mauiTitleText` の KDoc は「MAUI Sample の TitleTextColor 用」とだけ書かれ、`mauiHeaderText` との使い分けが読み取れない)。

**推奨修正**: 必須ではない。整理するなら `mauiHeaderText` へ寄せて `mauiTitleText` を畳むか、KDoc に用途の違い (ヘッダ / ButtonCell) を明記する。sample-parity 規約により両 platform 同時に行うこと。

## 裁定済みにつき指摘しない事項の客観記録

配色方針そのものはオーナー裁定済みのため指摘としては挙げないが、後の蒸留・アクセシビリティ検討で必要になる実測値を証跡として残す。**これは修正要求ではない**。

- サンプル Theme の組み合わせにおける確定ラベルのコントラスト比 (WCAG 2.x 相対輝度で算出): 文字 `#F2EFE6` / ピル `#CC9900` = **2.25:1**。参考として、同じピル色に対し黒は 8.13:1、純白は 2.58:1
- 確定ラベルは 14sp Bold (`HEADER_CONFIRM_TEXT_SIZE_SP = 14f`、`Typeface.BOLD`) で WCAG の large text 相当 (基準 3:1)
- ライブラリ既定 Theme (`backgroundColor = #FFFFFF` / `cellAccentColor = #007AFF`) の組み合わせは **4.02:1** で、既定のまま使う利用者には影響しない。低い比が出るのはサンプルのように背景と強調色を近い明度で組んだ場合に限られる
- 上記は Android サンプルの実機実測 (OK 文字 `#F2EFE6` / 候補行文字 `#555555` / シート面 `#FFFFFF`) と整合する

## アクションプラン

1. **(必須・蒸留前)** deviation.md に確定ラベル配色の乖離 (mock `#fff` → `Theme.backgroundColor`) と、その帰結である新しい結合を1項目として記録する。コード変更は不要のため、実装レビューの再周回は不要
2. **(蒸留時)** 上記 deviation を根拠に、「強調色の上に載せる文字色は `Theme.backgroundColor`」を `concepts/core/styling/` の公開契約として拾い上げるか、android/ADR-0005 の追補とするかを判断する。あわせて「裁定済みにつき指摘しない事項」のコントラスト実測値を判断材料として引き継ぐ
3. **(後工程)** tasks 4.1 / 4.2 実施時に、配色が判定に関わる証跡を現行配色で撮り直す (Suggestion 1)
4. **(任意)** `mauiTitleText` / `mauiHeaderText` の整理 (Suggestion 2)。実施する場合は両 platform 同時
5. **(蒸留時・継続)** review-004 / review-005 から申し送りの ADR-0005 の Suggestion 2件 (追補と本文の矛盾・「Material の標準挙動」の文言) は本サイクルでも未解消のまま
