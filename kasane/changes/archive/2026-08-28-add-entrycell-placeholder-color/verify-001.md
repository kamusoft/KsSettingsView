# Verify 001: add-entrycell-placeholder-color

- 検証日: 2026-08-27
- 対象: 未コミットの作業ツリー変更 (`git status` の 43 modified + 1 untracked テストファイル)
- 入力: `specs/` 9 capability・`tasks.md`・`deviation.md`・`ui/brief.md`
- 判定: **VALID**

---

## 1. 対応表

### 1.1 cell-types-input

#### Requirement: EntryCell の placeholder 文字色指定 (ADDED)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Cell 固有値が最優先で適用される | iOS `ios/Sources/KsSettingsViewUI/EntryCell.swift:42` / `ios/Sources/KsSettingsViewUI/EffectiveStyle.swift:246` / `ios/Sources/KsSettingsViewUI/EntryCellView.swift:146`、Android `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EntryCell.kt:58` / `EffectiveStyle.kt:385` / `EntryCellViewHolder.kt:182` | `ios/Tests/KsSettingsViewUITests/EffectiveStyleResolutionTests.swift` 「test_effectivePlaceholderColor_EntryCell個別が最優先」、`ios/Tests/KsSettingsViewUITests/InputCellsTests.swift` 「placeholder色はCellStyleとThemeから解決される」、`android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/InputCellsTest.kt` 「EntryCell の placeholder 色は Cell 固有値 CellStyle Theme の順に解決する」 | ✅ 一致 |
| CellStyle → Theme の順で fallback する | iOS `EffectiveStyle.swift:232`、Android `EffectiveStyle.kt:371` | 同上 (3 段の分岐をいずれも実測) + `EffectiveStyleResolutionTests.swift` 「EntryCellがnilならCellStyle」「EntryCellとCellStyleがnilならTheme」、`android/.../EffectiveStyleResolutionTest.kt` 「effectivePlaceholderColor は CellStyle 優先 / Theme フォールバック」 | ✅ 一致 |
| 全段未指定なら platform default になる | iOS: 解決結果 `nil` でプレーン `placeholder` 経路 (`EntryCellView.swift:222`)、Android: 生成時に捕捉した `ColorStateList` を復元 (`EntryCellViewHolder.kt:79`, `:328`) | iOS 「placeholder色未指定はプレーン表示のまま」(素の `UITextField` の既定色と照合)、Android 「placeholder 色が全段未指定ならホストテーマの hint 色をそのまま使う」(`assertSame` で `ColorStateList` 同一性を確認)、両 `effectivePlaceholderColor` の全段 nil テスト | ✅ 一致 |
| 入力済みテキストの色には影響しない | iOS `EntryCellView.swift:143` (textColor は valueText 解決) / `:146` (placeholder は別経路)、Android `EntryCellViewHolder.kt:221` (setTextColor) / `:182` (setHintTextColor) | iOS 「入力済みテキストの色にplaceholder色は影響しない」、Android 「EntryCell の入力文字色は valueText の解決色を使う」+ 視覚証跡 `ui/verification/ios-entry-placeholder-entered.png` / `android-entry-placeholder-entered.png` | ✅ 一致 |
| (Requirement 本文) 明示 placeholder 色は有効・無効で変化しない | iOS `EntryCellView.swift:209` (状態非依存の attributed)、Android `EntryCellViewHolder.kt:325` (`setHintTextColor(Int)` = 単色) | iOS 「明示したplaceholder色は無効状態でも変わらない」、Android 「明示した placeholder 色は無効状態でも変わらない」 | ✅ 一致 |

補足: `placeholderColor` は Compose DSL の `EntryCell` 2 overload にも通っている (`android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/InputCellDsl.kt:48`, `:87`。テスト `InputCellDslTest.kt` 2 本)。

### 1.2 settings-view-ios-ui

#### Requirement: iOS の placeholder 色描画 (ADDED)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 指定色で placeholder が表示される | `ios/Sources/KsSettingsViewUI/EntryCellView.swift:209-224` | `InputCellsTests.swift` 「placeholder色指定で指定色のattributed表示になる」 | ✅ 一致 |
| font 指定と色指定が共存する | `EntryCellView.swift:146` (`font: effective.valueTextFont` を毎回付与) | 「placeholder色指定時のfontは入力テキストと同じ実効fontになる」 | ✅ 一致 |
| 再利用行に色が残らない | `EntryCellView.swift:189` (`prepareForReuse` で `attributedPlaceholder = nil`) | 「再利用で前の行のplaceholder色が残らない」「色指定から未指定への再bindでプレーン表示に戻る」 | ✅ 一致 |
| Theme 変更が表示中の placeholder に追従する | 既存の Theme 再適用経路 (render 再実行) をそのまま利用 | 「test_applyTheme経由のTheme変更で表示中のplaceholder色が追従する」(Controller を window に載せた実描画で検証) | ✅ 一致 |
| placeholder 文字列 nil + 色指定でも安全に描画される | `EntryCellView.swift:210-214` (`guard let text` で両方 nil) | 「placeholder文字列nilなら色指定があってもplaceholderを持たない」「placeholder空文字列は色指定があっても安全に描画される」 | ✅ 一致 |
| (Requirement 本文) `CellStyle.placeholderColor` / `Theme.cellPlaceholderColor` の新設と `EffectiveStyle` 解決 | `CellStyle.swift:50`・`Theme.swift:120`・`EffectiveStyle.swift:64`,`:102`。手動等価性へも列挙 (`CellStyle.swift:101`,`:137` / `Theme.swift:248` / `EntryCell.swift:165`,`:184`) | 「CellStyleとThemeのplaceholderColorだけ異なると非同値」「EntryCell_placeholderColorだけ異なると非同値」「EffectiveStyle_placeholderColorはCellStyleからThemeへ解決する」 | ✅ 一致 |

### 1.3 settings-view-android-ui

#### Requirement: Android の placeholder (hint) 色描画 (ADDED)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 指定色で placeholder が表示される | `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EntryCellViewHolder.kt:182`, `:323` | `InputCellsTest.kt` 「EntryCell の placeholderColor が hint 色へ適用される」 | ✅ 一致 |
| 再利用行に色が残らない | `EntryCellViewHolder.kt:423-426` (`reset` でホスト既定へ戻し適用フラグをクリア) | 「再利用行に placeholder 色が残らない」(`reset` → 別 Cell bind の順で `assertSame`) | ✅ 一致 |
| Theme 変更が表示中の placeholder に追従する | 既存の Theme 通知 → 再 bind 経路 | 新規ファイル `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/EntryCellPlaceholderThemeRefreshTest.kt` 「theme 代入で表示中の EntryCell の placeholder 色が追従する」(Robolectric 実 View 経路) | ✅ 一致 |
| 明示色は無効状態でも変わらない | `EntryCellViewHolder.kt:325` (`setHintTextColor(@ColorInt Int)`) | 「明示した placeholder 色は無効状態でも変わらない」 | ✅ 一致 |
| 明示色から未指定へ戻すとホスト既定の状態別色へ復帰する | `EntryCellViewHolder.kt:79` (生成時に `hintTextColors` を `ColorStateList` のまま捕捉) / `:328-333` | 「placeholder 色を明示から未指定へ戻すとホスト既定の hint 色へ復帰する」(`assertSame` で `ColorStateList` 同一性) | ✅ 一致 |
| (Requirement 本文) 変化の無い placeholder 色を再適用しない | `EntryCellViewHolder.kt:324` (差分判定 + `placeholderColorApplied`) | 「同一 Cell への再バインドで変化の無い placeholder 色を再適用しない」(sentinel 色が保たれることを確認) | ✅ 一致 |
| (Requirement 本文) `CellStyle.placeholderColor` / `Theme.cellPlaceholderColor` 新設と解決順 | `CellStyle.kt:49`・`Theme.kt:117`・`EffectiveStyle.kt:371`,`:385` | `EffectiveStyleResolutionTest.kt` の 4 本 (Cell 固有値 / CellStyle / Theme / 全段未指定)、`InputCellsTest.kt` 「EntryCell は placeholderColor だけ異なると非同値」 | ✅ 一致 |

#### Requirement: 入力文字色の valueText 解決 (規約乖離の是正) (ADDED)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| valueText 色の明示指定が入力文字へ適用される | `EntryCellViewHolder.kt:221` (`effective.titleColor` → `effective.valueTextColor` へ置換) | `InputCellsTest.kt` 「EntryCell の入力文字色は valueText の解決色を使う」+ 証跡 `ui/verification/android-entry-valuetext-theme.png` | ✅ 一致 |
| valueText 未指定なら従来と同じ見た目になる | `EffectiveStyle.kt:327` (`cellStyle.valueTextColor` → `theme.cellValueTextColor` → `theme.cellTitleColor`) | 「EntryCell の入力文字色は valueText 未指定なら title 色へ fallback する」+ 証跡 `ui/verification/android-entry-valuetext-default.png` | ✅ 一致 |
| 無効状態は disabled 文字色が優先される | `EntryCellViewHolder.kt:221` (`if (cell.isEnabled) ... else disabledTextColor`) | 「EntryCell の入力文字色は無効状態で disabledTextColor が優先される」 | ✅ 一致 |
| (最終段の是正) 全段未指定はホストテーマの既定文字色 | `EffectiveStyle.kt:145-152` (`resolveDefaultTitleColor(context)` へ分岐) | 「EntryCell の入力文字色は全段未指定ならホストテーマの文字色になる」(ダークテーマ上で実測) | ⚠️ deviation 記録済み (`deviation.md` の [波及]。`EntryCell` 以外の valueText 描画 Cell 11 種にも及ぶ利用者可視の変化) |

### 1.4 maui-bridge

#### Requirement: placeholder 色の輸送 (ADDED)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| per-cell 値が native cell へ写る | iOS `ios/Sources/KsSettingsViewBridge/KsBridgeEntryCell.swift:25`,`:61`、Android `android/ks-settingsview-bridge/src/main/kotlin/jp/kamusoft/kssettingsview/bridge/KsBridgeEntryCell.kt:36`,`:66`。C# 側は `maui/KsSettingsView.Maui/Internals/KsCellSnapshots.cs:125` → `maui/KsSettingsView.Maui/Platforms/iOS/KsBridgeGateway.cs:369` / `Platforms/Android/KsBridgeGateway.cs:371`、iOS binding `maui/macios/KsSettingsView.Binding.iOS/ApiDefinition.cs:374` | `ios/Tests/KsSettingsViewBridgeTests/KsBridgeCellConversionTests.swift` 「EntryCellDTOのplaceholderColorがNativeの色へ写る」、`android/.../bridge/KsBridgeCellConversionTest.kt` 「EntryCell DTO の placeholderColor が Native の色へ写る」、`maui/KsSettingsView.Maui.Tests/ConversionPathTests.cs` (`snapshot.PlaceholderColor` が ARGB int) + 端末証跡 `ui/verification/maui-ios-entry-placeholder.png` / `maui-android-entry-placeholder.png` | ✅ 一致 |
| Theme 値が native Theme へ写る | iOS `KsBridgeTheme.swift:87`,`:148`、Android `KsBridgeTheme.kt:108`,`:175`、C# `Internals/KsThemeSnapshot.cs:95` → 両 Gateway `:504` / `:507`、binding `ApiDefinition.cs:964` | `KsBridgeThemeTests.swift` 「setTheme_のcellPlaceholderColorがThemeへ変換される」、`KsBridgeThemeTest.kt` 「setTheme の cellPlaceholderColor が Theme へ変換される」 | ✅ 一致 |
| null は未指定として写る | 両 OS とも `KsBridgeColor` 経由で null 透過 | `KsBridgeCellConversionTests.swift` 「placeholderColor未指定はNative側の未指定になる」、`KsBridgeThemeTests.swift` 「cellPlaceholderColor未指定はTheme側の未指定になる」、Android 同名 2 本、`ThemeAndCellStyleTests.cs` `UnspecifiedCellPlaceholderColorIsCarriedAsUnspecified` / `PlaceholderColorIsCarriedByEntryCell` | ✅ 一致 |
| (SHALL NOT) CellStyle の輸送に placeholder 色を追加しない | `KsCellStyleSnapshot` は無改変 | `maui/KsSettingsView.Maui.Tests/CellShapeTests.cs` `PlaceholderColorIsExposedByEntryCellOnly` (`KsCellStyleSnapshot.GetProperty("PlaceholderColor")` が null) | ✅ 一致 |

注: snapshot → 両 OS DTO の C# Gateway 変換 (`Platforms/*/KsBridgeGateway.cs`) はプラットフォーム条件コンパイルのため `KsSettingsView.Maui.Tests` の対象外。ここは端末スクリーンショット証跡が担保している (既存の輸送実装と同じ担保構造)。

### 1.5 maui-cells

#### Requirement: EntryCell.PlaceholderColor facade (ADDED)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| PlaceholderColor が native の placeholder 色に反映される | `maui/KsSettingsView.Maui/EntryCell.cs:33`,`:93`,`:144` | `ThemeAndCellStyleTests.cs` `PlaceholderColorIsCarriedByEntryCell` + 証跡 `ui/verification/maui-ios-entry-placeholder.png` / `maui-android-entry-placeholder.png` (両ターゲットで橙色の placeholder を確認) | ✅ 一致 |
| null は Theme → platform default へ解決される | `EntryCell.cs:144` (`KsWireValues.Color(null)` → null)、解決自体は native 側 | `PlaceholderColorIsCarriedByEntryCell` (未指定 → null)、native 解決は 1.1 / 1.2 / 1.3 のテスト群 | ✅ 一致 |
| 表示中の変更が反映される | `EntryCell.cs:154` (`AffectsSnapshot` に `PlaceholderColor` を追加) | `ThemeAndCellStyleTests.cs` `PlaceholderColorChangeIsPublished` (`ReplaceCell` が届く) | ✅ 一致 |
| (SHALL NOT) CellStyle 段の placeholder 色を持たない | `CellBase` / `LabelCell` / `PickerCell` / `KsCellStyleSnapshot` 無改変 | `CellShapeTests.cs` `PlaceholderColorIsExposedByEntryCellOnly` | ✅ 一致 |

### 1.6 maui-core

#### Requirement: SettingsView.CellPlaceholderColor (Theme 段) (ADDED)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Theme 段の色が全 EntryCell に適用される | `maui/KsSettingsView.Maui/SettingsView.cs:424`,`:831`,`:1037` | `ThemeAndCellStyleTests.cs` `CellPlaceholderColorIsCarriedByTheme` (Theme 写しに ARGB が載る) + native の Theme 段解決テスト (1.2 / 1.3) | ✅ 一致 |
| per-cell 指定が Theme 段より優先される | 解決は native 側 (`EffectiveStyle.effectivePlaceholderColor` の 4 段) | `CellPlaceholderColorIsCarriedByTheme` (Theme と cell 値が混ざらず別々に運ばれる) + iOS/Android の「Cell 固有値が最優先」テスト | ✅ 一致 |
| 表示中の Theme 変更が追従する | `SettingsView.cs:428` (`propertyChanged` → `ApplyTheme()`) | `ThemeAndCellStyleTests.cs` `CellPlaceholderColorChangeWhileConnectedIsApplied` (`SetTheme` が 1 回届く) + native 追従テスト (iOS `applyTheme`、Android `EntryCellPlaceholderThemeRefreshTest`) | ✅ 一致 |
| (既定値) 未指定は null | `SettingsView.cs:427` (`default(Color)`) | `CellShapeTests.cs` (`view.CellPlaceholderColor` が null)、`UnspecifiedCellPlaceholderColorIsCarriedAsUnspecified` | ✅ 一致 |

### 1.7 samples-ios / samples-android / samples-maui

#### Requirement: placeholder 色のデモ行 (ADDED、3 capability 同文)

| Scenario | 実装 | 証跡 | 状態 |
|---|---|---|---|
| (iOS) デモ行で指定色の placeholder が確認できる | `samples/ios/KsSettingsViewSample/InputCellsDemoView.swift:146-151`、色定義 `samples/ios/KsSettingsViewSample/SampleTheme.swift:86` | `ui/verification/ios-entry-placeholder-light.png` / `ios-entry-placeholder-dark.png` — 「表示名」行の placeholder が橙 (#D6885A)、直上の「ニックネーム (callback)」行は OS 既定の灰 | ✅ 一致 |
| (Android) デモ行で指定色の placeholder が確認できる | `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/InputCellsDemoScreen.kt:149-154`、色定義 `SampleTheme.kt:108` | `ui/verification/android-entry-placeholder-light.png` — 同上 | ✅ 一致 |
| (MAUI) デモ行で指定色の placeholder が確認できる (iOS / Android 両ターゲット) | `samples/maui/KsSettingsView.Sample.Maui/Pages/InputCellsDemoPage.xaml:47-50`、VM `ViewModels/InputCellsDemoViewModel.cs:108`、色定義 `SampleTheme.cs:69` | `ui/verification/maui-ios-entry-placeholder.png` / `maui-android-entry-placeholder.png` — 両ターゲットで同一の見え方 | ✅ 一致 |

3 platform の文言パリティ (行タイトル「表示名」/ placeholder「placeholder 色の指定例」/ Section footer の追記) も一致を確認した。

---

## 2. 追加検査

### 2.1 tasks.md の虚偽チェック

全 20 タスクが `[x]`。対応表と突き合わせた結果、**虚偽なし**。

- 1.1〜1.5 (iOS core): `EntryCell.swift` の全列挙箇所 (init 2 種 / `==` / `hash` / `withDSLID` `:212` / `withStyle` `:234` / `withIcon` `:256`) を確認。1.4 の「Theme 再適用経路」は既存経路で成立し、テストで実証済み
- 2.1〜2.7 (Android core): `EntryCell.kt` の手動 `equals` `:86` / `hashCode` `:105` を確認。2.5 の是正は `EntryCellViewHolder.kt:221` で実施済み
- 3.1〜3.3 (Bridge): 両 OS DTO + テスト 4 本 (iOS 2 / Android 2) を確認
- 4.1〜4.6 (MAUI facade): `ApiDefinition.cs` の 2 プロパティ・両 Gateway の 4 箇所を確認。テストは `CellShapeTests` 1 本 + `ConversionPathTests` 1 箇所 + `ThemeAndCellStyleTests` 5 本
- 5.1〜5.2 (サンプル・視覚照合): 3 platform のデモ行と `ui/verification/` の 9 枚、`ui/brief.md` の照合結果節を確認

### 2.2 逆流検査

`git status` 上で `kasane/changes/add-entrycell-placeholder-color/` 配下の変更は以下のみ:

- `tasks.md` (M) — チェックボックスの `[ ]` → `[x]` のみ。本文の書き換えなし (`git diff` で確認)
- `ui/brief.md` (M) — 「照合結果」「トークン候補」節の**追記のみ**。既存節 (画面と状態 / デザイントークン参照 / 承認モック) は無改変。ksn-ui が実装後に埋める節であり逆流にあたらない
- `deviation.md` / `review-001.md` / `review-002.md` / `second-opinion-code-001.md` / `ui/verification/` (untracked) — 新規追加

`proposal.md`・`exploration.md`・`specs/` 9 ファイル・`second-opinion-spec-001.md`・`ui/brief.md` の足場部分・`ui/mock/` は**一切書き換えられていない**。**逆流なし**。

### 2.3 未記録乖離の洗い出し

対応表に ❌ はゼロ。diff にあって Scenario に対応しない変更は次の 3 件で、すべて `deviation.md` に記録済み:

| diff 上の変更 | deviation.md の記録 |
|---|---|
| `ios/Sources/KsSettingsViewUI/Theme.swift` の `backgroundColor` / `cellTitleColor` / `cellTitleFont` の doc comment 3 箇所の履歴記述削除 | [付随修正] 1 件目 (comment-policy 違反の局所修正) |
| `samples/maui/KsSettingsView.Sample.Maui/SampleTheme.cs:68-69` の `<see cref>` 完全修飾 | [付随修正] 2 件目 (CS1574 回避) |
| `android/.../EffectiveStyle.kt:145-152` の valueText 最終段をホストテーマ既定文字色へ | [波及] (valueText を描画する全 Cell 型 11 種に及ぶ) |

その他の差分 (`SampleTheme` への `demoPlaceholderOrange` / `DemoPlaceholderOrange` 追加、`InputCellsDemoViewModel.DisplayName` 追加、Section footer の文言追記) は samples-* Requirement のデモ行実装に必要な付随物であり、Scenario の範囲内。

**未記録乖離: 0 件。**

### 2.4 UI 変更の検査

- `ui/brief.md` に承認モックの記録あり: `mock/placeholder-color.html` 採用、`approved.png`、初回承認 2026-08-27 → セカンドオピニオン指摘反映後に同日再承認
- 照合結果節に 9 枚の証跡と mock 面の対応が記載されている
- 合意済み妥協: 0 件 (brief.md に明記)
- 証跡範囲の限界 (全段未指定時の valueText 最終段はサンプル構成上スクリーンショットで示せないため自動テストで担保) が brief.md に明記されている
- 未取得事項: brief.md に「ユーザーの最終承認は未取得」と記載がある。これは verify の判定対象外 (呼び出し元とユーザーの確認事項として申し送る)

### 2.5 テスト実行結果

| プラットフォーム | コマンド | 結果 |
|---|---|---|
| iOS | `xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,name=iPhone 17'` | **TEST SUCCEEDED** — 606 tests, 0 failures |
| MAUI | `dotnet test KsSettingsView.Maui.Tests/KsSettingsView.Maui.Tests.csproj` | **成功** — 合格 472 / 失敗 0 / スキップ 0 |
| Android | `./gradlew test --rerun-tasks` | **BUILD SUCCESSFUL** — 230 tasks executed。test-results XML の集計で tests 2702 / failures 0 / errors 0 / skipped 0 (debug + release の両 variant 合計) |

Android は Gradle のキャッシュ (`UP-TO-DATE`) を避けるため `--rerun-tasks` で強制再実行し、単独実行 (他の Gradle プロセスと同時に走らせない状態) で確認した。

---

## 3. 判定

**VALID**

- 9 capability・11 Requirement・26 Scenario (Requirement 本文由来の追加検査行を含む) のすべてが「✅ 一致」または「⚠️ deviation 記録済み」
- tasks.md の虚偽チェック該当なし
- 足場アーティファクト (proposal / exploration / specs / mock) の逆流なし
- 未記録乖離 0 件
- 3 プラットフォームのテストが全件成功

### 申し送り (判定に影響しない事項)

1. `ui/brief.md` の照合結果は「ユーザーの最終承認は未取得」と記録されている。アーカイブ前にオーナーの視覚照合承認を取ること
2. `deviation.md` の [波及] は `EntryCell` 以外の 11 Cell 型にも及ぶ利用者可視の変化。蒸留時に concepts (`kasane/concepts/core/styling/style-resolution.md`) 側へ「Android の valueText 最終段はホストテーマの `textColorPrimary`」を反映する候補
3. snapshot → 両 OS DTO の C# Gateway 変換は条件コンパイルのため単体テスト対象外で、端末スクリーンショットが唯一の担保。これは本 change 固有の欠落ではなく既存の輸送実装と同じ構造
