# sample-parity 対応表: Section 装飾デモ (native ↔ MAUI)

`samples/ios` / `samples/android` の SectionDecorationDemo を正として、MAUI のデモページを実装するときに
揃えた項目を実物のコードから起こした対応表。判定基準は `concepts/cross/conventions/sample-parity.md`
(cross/ADR-0016)。

対象ファイル:

| platform | 画面 | 操作部 | preset | 下地 Theme | バッジ |
|---|---|---|---|---|---|
| iOS | `samples/ios/KsSettingsViewSample/SectionDecorationDemoView.swift` | `SectionDecorationDemoControls.swift` | `SectionDecorationPreset.swift` | `SampleTheme.swift` の `sectionDecorationDemo(...)` | `SampleIconBadge.swift` |
| Android | `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/SectionDecorationDemoScreen.kt` | `SectionDecorationDemoControls.kt` | `SectionDecorationPreset.kt` | `SampleTheme.kt` の `sectionDecorationDemo(...)` | `SampleIconBadge.kt` |
| MAUI | `samples/maui/KsSettingsView.Sample.Maui/Pages/SectionDecorationDemoPage.xaml` (+ `.xaml.cs` / `ViewModels/SectionDecorationDemoViewModel.cs`) | 同 XAML の操作部 `VerticalStackLayout` | `SectionDecorationPreset.cs` | `SampleTheme.cs` の `ApplySectionDecorationDemo(...)` | `SampleIconBadge.cs` + `Resources/Images/ic_badge_*.svg` |

## 1. 画面・メニュー文言

| 項目 | iOS | Android | MAUI | 一致 |
|---|---|---|---|---|
| ルートメニュー項目 = 画面タイトル | `Section 装飾デモ（style 切替）` (`SampleScreen.sectionDecoration.title`) | `Section 装飾デモ（style 切替）` (`SampleScreen.SectionDecoration.title`) | `Section 装飾デモ（style 切替）` (`SampleScreen.All` の Demo 区分 6 件目) | ○ |
| メニュー上の区分 | `デモ` (`SampleScreen.demos`) | `デモ` | `デモ` (`SampleScreenCategory.Demo`) | ○ |
| メニュー内の並び位置 | デモ区分の末尾 (`.visibility` の次) | デモ区分の末尾 (`Visibility` の次) | デモ区分の末尾 (`isVisible デモ（条件付き非表示）` の次) | ○ |
| タイトルの定義元 | 画面側に書かず `SampleScreen` から与える | 同左 | 同左 (`SampleScreen.CreateTitledPage()`) | ○ |

MAUI のメニューには `Store 方式デモ` / `DSL 方式デモ` が無い。これは Store / DSL の公開 API が MAUI facade に
存在しないための規約上の例外 (sample-parity「デモ対象の公開 API が存在しない platform」) で、本デモの
パリティ判定には影響しない。

## 2. 操作部の文言

| 項目 | iOS | Android | MAUI | 一致 |
|---|---|---|---|---|
| style 選択肢の文言 | `Classic` / `Modern` (`Text(...)`) | `Classic` / `Modern` (`KsSettingsViewStyle` の enum 名) | `Classic` / `Modern` (`RadioButton.Content`) | ○ |
| style 選択肢の並び順 | Classic → Modern | Classic → Modern (`listOf(Classic, Modern)`) | Classic → Modern | ○ |
| preset ラベル | `装飾プリセット` | `装飾プリセット` | `装飾プリセット` | ○ |
| preset 選択肢の文言と並び | `既定` / `余白広め・角丸小` / `ボーダーあり` (`allCases` 順) | 同左 (`entries` 順) | 同左 (`SectionDecorationPreset.All` 順) | ○ |
| 操作部の配置 | 設定 list の上に独立して置く | 同左 | 同左 (`Grid RowDefinitions="Auto,*"` の 1 行目) | ○ |

操作部に使う**コントロールの種類**は platform 差がある (→ 7. 許容差分)。

## 3. preset 3 件の 4 属性値

`sectionMargin` は論理方向 (leading / trailing) で表す。MAUI の `Thickness(left, top, right, bottom)` は
`Left = leading` / `Right = trailing` として解釈される。

| preset | 属性 | iOS | Android | MAUI | 一致 |
|---|---|---|---|---|---|
| `既定` | margin / radius / borderWidth / borderColor | 4 属性すべて未指定 (nil) | 4 属性すべて未指定 (null) | 4 属性すべて未指定 (null) | ○ |
| `余白広め・角丸小` | sectionMargin | `NSDirectionalEdgeInsets(top: 32, leading: 32, bottom: 0, trailing: 32)` | `PaddingValues(start = 32.dp, top = 32.dp, end = 32.dp, bottom = 0.dp)` | `new Thickness(32, 32, 32, 0)` (leading 32 / top 32 / trailing 32 / bottom 0) | ○ |
| `余白広め・角丸小` | sectionCornerRadius | `8` | `8.dp` | `8` | ○ |
| `余白広め・角丸小` | borderWidth / borderColor | 未指定 | 未指定 | 未指定 | ○ |
| `ボーダーあり` | sectionBorderWidth | `2` | `2.dp` | `2` | ○ |
| `ボーダーあり` | sectionBorderColor | `SampleTheme.demoSectionBorder` (#C7C7CC) | `SampleTheme.demoSectionBorder` (#C7C7CC) | `SampleTheme.DemoSectionBorder` (#C7C7CC) | ○ |
| `ボーダーあり` | margin / radius | 未指定 | 未指定 | 未指定 | ○ |

3 platform とも preset は色定義を自前で持たず、Sample 共通の色定数 (`demoSectionBorder`) を参照する。

## 4. Section・Cell 構成

Section は 4 件、順序も一致。

| # | Header | Footer | Cell (順に) | 一致 |
|---|---|---|---|---|
| 1 | なし | なし | `SwitchCell 機内モード` (icon: airplane, 初期 off) / `CommandCell Wi-Fi` (valueText `demoAP-0a1b2c-5`, icon: wifi) / `CommandCell Bluetooth` (valueText `オン`, icon: bluetooth) / `CommandCell バッテリー` (icon: battery) | ○ |
| 2 | `外観モード` | `好みに応じて外観モードを選択できます。Header と Footer は箱の外側に配置されます。` | `SwitchCell 自動` (初期 on) / `CommandCell テキストサイズを変更` | ○ |
| 3 | なし | なし | `SwitchCell True Tone` (初期 on) | ○ |
| 4 | `ボーダー指定時の例` | `既定はボーダーなし (width 0)。指定時のみ枠線が箱の輪郭に描かれます。` | `LabelCell sectionBorderWidth: 2` / `LabelCell sectionBorderColor: gray` | ○ |

Section 3 は「単一 Cell の Section には separator を引かない」ことの、Section 4 は「ボーダーは箱の輪郭に描く」
ことの観察対象。

## 5. 下地 Theme

preset が持つ 4 属性以外の Theme 値は 3 platform で同一。

| Theme 項目 | iOS / Android の値 | MAUI の対応 API | 一致 |
|---|---|---|---|
| 背景色 | `mauiViewBackground` (#F2EFE6) | `view.BackgroundColor = MauiViewBackground` | ○ |
| Cell アクセント色 | `demoAccentGreen` (#34C759) | `view.CellAccentColor = DemoAccentGreen` | ○ |
| Header 背景色 | `mauiViewBackground` (#F2EFE6) | `view.HeaderBackgroundColor = MauiViewBackground` | ○ |
| Footer 背景色 | `mauiViewBackground` (#F2EFE6) | `view.FooterBackgroundColor = MauiViewBackground` | ○ |
| アイコンサイズ | `SampleIconBadge.size` = 29 | `view.CellIconSize = SampleIconBadge.Size` (29) | ○ |
| アイコン角丸 | `SampleIconBadge.cornerRadius` = 7 | `view.CellIconRadius = SampleIconBadge.CornerRadius` (7) | ○ |

箱の塗り (`cellBackgroundColor`)・separator 色・Header / Footer の文字色は 3 platform とも未指定で、
ライブラリ既定に解決させる。バッジの地色も一致する (airplane #FF9500 / wifi #007AFF /
bluetooth #0A84FF / battery #34C759)。

## 6. 初期状態

| 項目 | iOS | Android | MAUI | 一致 |
|---|---|---|---|---|
| 初期 style | `.modern` | `KsSettingsViewStyle.Modern` | `SettingsViewStyle.Modern` | ○ |
| 初期 preset | `.standard` (`既定`) | `Standard` (`既定`) | `Standard` (`既定`) | ○ |
| `機内モード` | `false` | `false` | `false` | ○ |
| `自動` | `true` | `true` | `true` | ○ |
| `True Tone` | `true` | `true` | `true` | ○ |

## 7. 許容差分

sample-parity が「platform の見た目そのもの」「公開 API の platform 命名差」として許容する範囲。
いずれも画面上の**文言と設定内容には差が出ない**。

1. **操作部コントロールの種類**
   - iOS: `Picker(...).pickerStyle(.segmented)` + `Picker(...).pickerStyle(.menu)`
   - Android: `SingleChoiceSegmentedButtonRow` + `SegmentedButton` / `TextButton` + `DropdownMenu`
   - MAUI: `RadioButtonGroup` を付けた `HorizontalStackLayout` + `RadioButton` 2 個 / `Picker`
   - MAUI Controls に segmented control と dropdown menu の標準コントロールが無いため、同じ「排他選択 +
     一覧からの選択」を MAUI の標準部品で組む。選択肢の文言・並び順・既定選択は一致させてある。
   - この差は操作部の縦の占有高さに出るため、同じ preset でも設定 list の開始 y 位置が platform 間で
     ずれる。設定 list 自身の描画の比較には影響しない。

2. **バッジ型アイコンのシンボル字形**
   - iOS: SF Symbols (`airplane` / `wifi` / `antenna.radiowaves.left.and.right` / `battery.100percent`) を
     地色の正方形へ描画
   - Android: Material Symbols の drawable (`ic_airplanemode_active` / `ic_wifi` / `ic_bluetooth` /
     `ic_battery_full`) を同様に描画
   - MAUI: Material Symbols Outlined 由来のシンボルを焼き込んだ SVG (`ic_badge_*.svg`)
   - バッジの一辺 (29)・角丸 (7)・地色は 3 platform で一致し、白いシンボルの字形だけが由来ライブラリの差に
     なる。特に Bluetooth はアイコン集合の差がそのまま出る (iOS は電波アイコン、Android / MAUI は
     Bluetooth 記号)。

3. **iOS のタイトル 1 行表示の指定方法**
   - native: `.navigationBarTitleDisplayMode(.inline)`
   - MAUI: `On<iOS>().SetLargeTitleDisplay(LargeTitleDisplayMode.Never)`
   - 公開 API の命名差であり、描画結果 (navigation bar のタイトルが 1 行表示) は一致する。
