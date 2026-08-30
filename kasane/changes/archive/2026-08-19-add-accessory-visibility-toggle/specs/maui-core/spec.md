# Delta: maui-core (add-accessory-visibility-toggle)

## ADDED Requirements

### Requirement: Section.IsHeaderVisible / IsFooterVisible の公開

facade `Section` は BindableProperty `IsHeaderVisible` / `IsFooterVisible` (いずれも `bool`、既定 `true`) を公開する (SHALL)。命名は .NET 慣例に従い、AiForms の `FooterVisible` 命名は踏襲しない (Native 起点の新概念のため maui/ADR-0008 の互換命名の対象外)。`false` のとき、内容があっても該当の Header / Footer は表示されない (core/ADR-0023 の AND 合成)。

#### Scenario: 初期構築時のトグルが反映される
- **GIVEN** header 内容を持ち `IsHeaderVisible="False"` を指定した Section
- **WHEN** SettingsView を表示する
- **THEN** その Section の Header は表示されない

#### Scenario: 既定値では現行挙動と一致する
- **GIVEN** トグルを指定しない Section
- **WHEN** SettingsView を表示する
- **THEN** Header / Footer の表示は従来どおり内容の有無だけで決まる

### Requirement: 実行時トグル変更の反映経路

実行時の `IsHeaderVisible` / `IsFooterVisible` 変更は、`IsVisible` / `HeaderHeight` と同じ Section 置換のバッチ反映経路で native へ配信する (SHALL)。専用の gateway 操作は追加しない。

#### Scenario: 実行時のトグル変更が native へ配信される
- **GIVEN** 表示中の SettingsView と Header が表示されている Section
- **WHEN** `IsHeaderVisible = false` に変更する
- **THEN** Section 置換として native へ配信され、Header が非表示になる (逆方向の `true` への変更では再表示される)

#### Scenario: トグルが gateway の Section 置換に反映される
- **GIVEN** fake gateway を注入した SettingsView (net10.0 ユニットテスト)
- **WHEN** `IsFooterVisible = false` に変更する
- **THEN** gateway は該当 Section の置換を1回受け取り、置換後 Section の `IsFooterVisible` は `false` である

#### Scenario: トグル変更の Section 置換で Cell の identity と内容が保持される
- **GIVEN** 表示中の SettingsView と、値を持つ Cell 群を含む Section
- **WHEN** `IsHeaderVisible` を変更する
- **THEN** Section 置換後も Cell の ID と表示内容は保持される
