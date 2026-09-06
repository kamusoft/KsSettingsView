## ADDED Requirements

### Requirement: Cell 単位の色プロパティの変更は表示中の Cell に届く

MAUI の Cell の色プロパティのうち、その Cell が内容 (snapshot) に写す項目 (`CellBase` の `TitleColor` / `DescriptionColor` / `ValueTextColor` / `HintTextColor` / `BackgroundColor`、Cell 固有の `AccentColor` / `PlaceholderColor` / `AndroidButtonColor`) を表示中に変更すると、その Cell の内容更新として facade → snapshot → bridge → native の Cell 置換まで届く (SHALL)。届いた色のうち、その Cell がその platform で描画に使う項目は表示中の行が新しい色で描き直される (SHALL)。描画に使わない項目 (`AndroidButtonColor` の iOS、対応するスロットを表示していない Cell の色) は届いても見た目を変えない (SHALL — 本 change 前と同じ)。`CustomCell` はタイトル・説明文・ヒント系の style を snapshot に写さない現行契約のままで、それらの変更は内容更新にならない (SHALL — 本 change 前と同じ)。`AppThemeBinding` を設定したプロパティは、アプリの外観変更で供給される新しい値によって同じ経路で届く (SHALL)。未指定 (`null`) に戻したプロパティは native の未指定表現へ写され、Theme または外観既定へ継承する (SHALL)。両外観で異なる色を使いたい利用者の手段は Cell の色プロパティに `AppThemeBinding` を書くことであり、facade は外観を受け取る別の型やイベントを持たない (SHALL NOT)。

#### Scenario: 表示中の Cell の TitleColor 変更が Cell 置換として配信される
- **GIVEN** 表示中の LabelCell (色プロパティ未設定)
- **WHEN** `TitleColor` に色を設定する
- **THEN** その Cell だけを対象にした置換が gateway へ配信され、置換後の style の title 色は設定した色の ARGB である

#### Scenario: 表示中の Cell 固有色の変更が Cell 置換として配信される
- **GIVEN** 表示中の SwitchCell と EntryCell と DatePickerCell (色プロパティ未設定)
- **WHEN** SwitchCell の `AccentColor`、EntryCell の `PlaceholderColor`、DatePickerCell の `AndroidButtonColor` に色を設定する
- **THEN** それぞれの Cell の置換が配信され、置換後の Cell 固有色は設定した色の ARGB である

#### Scenario: 色プロパティを null に戻すと未指定として配信される
- **GIVEN** `TitleColor` を設定して表示中の LabelCell
- **WHEN** `TitleColor` を `null` に戻す
- **THEN** 置換後の style の title 色は未指定 (全項目未指定なら style 自体が未指定) である

#### Scenario: CustomCell のテキスト系 style 変更は内容更新にならない
- **GIVEN** 表示中の CustomCell
- **WHEN** `TitleColor` に色を設定する
- **THEN** 置換は配信されず、`BackgroundColor` を設定したときだけ行 style の置換が配信される

#### Scenario: Cell プロパティの AppThemeBinding の値が native まで届く
- **GIVEN** `TitleColor` に `AppThemeBinding` (light / dark の値) を設定した ButtonCell を持つ SettingsView を表示中
- **WHEN** アプリの外観をダークへ切り替える
- **THEN** ButtonCell の title は binding の dark 側の値で描き直される (iOS / Android 両方)

### Requirement: Android bridge の Cell 固有色の変換

Android bridge の Cell DTO (`KsBridgeEntryCell` / `KsBridgeButtonCell` / `KsBridgeSwitchCell` / `KsBridgeCheckboxCell` / `KsBridgeSimpleCheckCell` / `KsBridgeRadioCell` / `KsBridgePickerCell` / `KsBridgeNumberPickerCell` / `KsBridgeTimePickerCell` / `KsBridgeDatePickerCell`) の色項目は、未指定 (`null`) を native の `Color.Unspecified` へ、明示された ARGB を同じ値の `Color` へ写す (SHALL)。DTO の wire 形式 (項目・型・null の意味) は本 change 前と同じである (SHALL)。

#### Scenario: Cell DTO の未指定色は Unspecified になる
- **GIVEN** 色項目がすべて null の EntryCell DTO と DatePickerCell DTO
- **WHEN** native の Cell へ変換する
- **THEN** `placeholderColor` / `accentColor` / `androidButtonColor` は `Color.Unspecified` である

#### Scenario: Cell DTO の明示 ARGB は保たれる
- **GIVEN** `accentColor` に ARGB を持つ SwitchCell DTO
- **WHEN** native の Cell へ変換する
- **THEN** `accentColor` は同じ ARGB の `Color` である

### Requirement: CellStyle DTO が輸送する項目

Cell 個別スタイルの輸送 DTO (`KsBridgeCellStyle` iOS / Android と facade の `KsCellStyleSnapshot`) は、MAUI の Cell が CellStyle 段として公開する項目 (title / description / valueText / hint の色とフォント、icon の寸法、行の高さ、行の背景色) を輸送する (SHALL)。native の `CellStyle` が持つ accent と placeholder の CellStyle 段は MAUI から設定する手段がなく、MAUI の `AccentColor` / `PlaceholderColor` は Cell 固有段として per-type の Cell DTO で輸送される (SHALL)。DTO の wire 形式は本 change 前と同じである (SHALL)。

#### Scenario: MAUI の PlaceholderColor は Cell 固有段として届く
- **GIVEN** `PlaceholderColor` を設定した MAUI の EntryCell と、`CellPlaceholderColor` を設定した SettingsView
- **WHEN** native の EntryCell へ変換し実効 placeholder 色を解決する
- **THEN** placeholder 色は EntryCell の値 (Cell 固有段) になり、SettingsView の値より優先する
