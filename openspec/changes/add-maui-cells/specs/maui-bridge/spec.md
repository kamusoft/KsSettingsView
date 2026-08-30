## MODIFIED Requirements

### Requirement: Bridge Builder API

`KsSettingsViewBridge` は Builder インスタンスを生成するファクトリメソッド `makeBuilder()` を公開しなければならない (SHALL)。Builder は以下のメソッドで `SettingsRoot` を宣言的に構築できなければならない (MUST)：

- `beginSection(header:footer:)` / `endSection()`：Section の開始・終了
- 全 14 種類の Cell 追加メソッド：
  - `addLabelCell(id:title:description:valueText:icon:hintText:)`：LabelCell の追加
  - `addCommandCell(id:title:description:icon:hintText:hideArrow:)` / `addButtonCell(id:title:titleColor:)` / `addSwitchCell(id:title:description:isOn:accentColor:)` / `addCheckboxCell(id:title:description:isChecked:accentColor:)` / `addRadioCell(id:title:groupId:value:selectedValue:)` / `addSimpleCheckCell(id:title:isChecked:)`
  - `addEntryCell(id:title:description:text:placeholder:keyboardType:)` / `addPickerCell(id:title:items:selectedIndex:)` / `addTextPickerCell(id:title:items:selectedIndex:)` / `addNumberPickerCell(id:title:min:max:step:value:)` / `addTimePickerCell(id:title:time:format:)` / `addDatePickerCell(id:title:date:format:minDate:maxDate:)`
  - `addCustomCell(id:contentTypeName:contentJson:)`（または同等の C# View → Native View 変換用 API）
- `build() -> KsSettingsRootDTO`：構築済み `SettingsRoot` 相当 DTO を返す

Color パラメータ（`addButtonCell` の `titleColor`、`addSwitchCell` / `addCheckboxCell` の `accentColor` 等）は ObjC / Java 互換の Color 受け入れ型として MAUI 側から `Microsoft.Maui.Graphics.Color` を渡し、Bridge 内部で **Native 型 (iOS: `UIColor`、Android: Compose `androidx.compose.ui.graphics.Color`) に 1 段直接変換**してから Native Cell に格納しなければならない (MUST)。`purify-core-extract-style-to-ui-layer` により `KsColor` 中間構造が Core から削除されたため、Bridge は `KsColorDTO` 等の独自 Color DTO 型を導入してはならない (MUST NOT)。

Builder は **Theme を扱わない** (MUST NOT、`add-maui-bridge` で確定)。Theme 適用は `controller.setTheme(_:)` / `view.setTheme(_:)` 独立 API で行う（`add-maui-bridge` の `Bridge Controller / View API` Requirement 参照）。

`SettingsRoot` 値型自体に Root H/F は含まないため (MUST NOT、`add-partial-update-core` で確定)、Builder にも `setRootHeader` / `setRootFooter` メソッドは公開しない (MUST NOT)。Root H/F は Bridge Controller / View 側の `setRootHeader(view:)` / `setRootFooter(view:)` で別途設定する（`add-maui-bridge` の `Bridge Controller / View API` Requirement 参照）。

#### Scenario: Builder による LabelCell 構築（iOS）

- **GIVEN** `let builder = KsSettingsViewBridge.makeBuilder()`
- **WHEN** `builder.beginSection(header: "test", footer: nil)` → `builder.addLabelCell(id: "c1", title: "Title", description: nil, valueText: nil, icon: nil, hintText: nil)` → `builder.endSection()` → `let root = builder.build()`
- **THEN** `root` は 1 セクション・1 LabelCell を含む `KsSettingsRootDTO` となる

#### Scenario: Builder による LabelCell 構築（Android）

- **GIVEN** `val builder = KsSettingsViewBridge.makeBuilder()`
- **WHEN** `builder.beginSection("test", null); builder.addLabelCell("c1", "Title", null, null, null, null); builder.endSection(); val root = builder.build()`
- **THEN** `root` は 1 セクション・1 LabelCell を含む `KsSettingsRootDTO` となる

#### Scenario: Builder による SwitchCell 構築（iOS）

- **GIVEN** `let builder = KsSettingsViewBridge.makeBuilder()`
- **WHEN** `builder.beginSection(header: nil, footer: nil)` → `builder.addSwitchCell(id: "sw1", title: "Enable", description: nil, isOn: true, accentColor: nil)` → `builder.endSection()` → `let root = builder.build()`
- **THEN** `root` は 1 SwitchCell（id="sw1"、isOn=true）を含む `KsSettingsRootDTO` となる

#### Scenario: Builder による全 14 Cell 種別の利用可能性

- **GIVEN** `KsSettingsViewBuilder` クラス（iOS Swift / Android Kotlin）
- **WHEN** 公開メソッド一覧を確認する
- **THEN** `addLabelCell` / `addCommandCell` / `addButtonCell` / `addSwitchCell` / `addCheckboxCell` / `addRadioCell` / `addSimpleCheckCell` / `addEntryCell` / `addPickerCell` / `addTextPickerCell` / `addNumberPickerCell` / `addTimePickerCell` / `addDatePickerCell` / `addCustomCell` の 14 メソッドが全て公開されている

#### Scenario: Root H/F は Builder では設定不可

- **GIVEN** `let builder = KsSettingsViewBridge.makeBuilder()` の公開 API
- **WHEN** 公開メソッド一覧を確認する
- **THEN** `setRootHeader` / `setRootFooter` 系のメソッドは存在しない（Root H/F は Bridge Controller / View 側の `setRootHeader(view:)` / `setRootFooter(view:)` で設定する設計のため）

#### Scenario: Theme は Builder では設定不可

- **GIVEN** `let builder = KsSettingsViewBridge.makeBuilder()` の公開 API
- **WHEN** 公開メソッド一覧を確認する
- **THEN** `setTheme` / `theme` 系のメソッド・プロパティは存在しない（Theme は Bridge Controller / View 側の `setTheme(_:)` 独立 API で適用する設計のため）

#### Scenario: addButtonCell の titleColor は MAUI Color → Native Color 直接変換

- **GIVEN** MAUI `Microsoft.Maui.Graphics.Color(red: 0.2, green: 0.4, blue: 0.8, alpha: 1.0)` を `addButtonCell(id: "b1", title: "Save", titleColor: <mauiColor>)` に渡す
- **WHEN** Builder が Native Cell を生成し `build()` する
- **THEN** Bridge 内部で `UIColor(red: 0.2, green: 0.4, blue: 0.8, alpha: 1.0)`（iOS）または Compose `Color(red = 0.2f, green = 0.4f, blue = 0.8f, alpha = 1.0f)`（Android）に 1 段直接変換され、Native `ButtonCell.titleColor` に格納される（旧 `KsColor` 構造は経由しない）

#### Scenario: addSwitchCell / addCheckboxCell の accentColor も同様

- **GIVEN** MAUI Color 1 つを `addSwitchCell(..., accentColor: <mauiColor>)` または `addCheckboxCell(..., accentColor: <mauiColor>)` に渡す
- **WHEN** Builder が Native Cell を生成する
- **THEN** Bridge 内部で Native `UIColor?` / Compose `Color?` に 1 段直接変換され、Native Cell の `accentColor` に格納される。`KsColor` 中間構造や `KsColorDTO` は使用されない

### Requirement: ユーザー操作 delegate / listener

Bridge は単一の `KsCellInteractionDelegate`（iOS）/ `KsCellInteractionListener`（Android）に全 Cell の操作通知を集約しなければならない (SHALL)。Cell 種別はメソッド名で識別する（`didChangeBoolValue` / `didChangeTextValue` / `didTapCommand` 等）。14 Cell 種別分の通知メソッドを**実体実装**として完備しなければならない (MUST)。EntryCell の値変更通知は Bridge 内部で 200ms debounce を適用し、最終確定値のみ `didChangeTextValue` で通知しなければならない (MUST)。

#### Scenario: 14 Cell 種別分の通知メソッド実体実装

- **GIVEN** Bridge モジュールのソース
- **WHEN** `KsCellInteractionDelegate.swift` / `KsCellInteractionListener.kt` を確認する
- **THEN** 14 Cell 種別分の通知メソッド（`didTapCell` / `didTapCommandCell` / `didTapButton` / `didChangeBoolValue` / `didChangeRadioSelection` / `didChangeTextValue` / `didChangePickerSelection` / `didChangeNumberValue` / `didChangeTimeValue` / `didChangeDateValue` 他）が宣言され、Native Cell からの実体実装も完備している

#### Scenario: LabelCell のタップ通知

- **GIVEN** LabelCell を含む `SettingsRoot` を `setRoot` した Controller / View
- **WHEN** ユーザーが LabelCell をタップする（LabelCell に `onTap` が設定されている場合）
- **THEN** delegate / listener の `didTapCell(cellId:)` メソッドが該当 cellId で呼び出される

#### Scenario: SwitchCell の値変更通知

- **GIVEN** SwitchCell を含む `SettingsRoot` を `setRoot` した Controller / View、ユーザーが SwitchCell をタップして OFF → ON
- **WHEN** Native SwitchCell の `onValueChanged` が発火する
- **THEN** delegate / listener の `didChangeBoolValue(cellId: ..., value: true)` メソッドが呼び出される

#### Scenario: EntryCell の debounce 動作

- **GIVEN** EntryCell を含む `SettingsRoot` を `setRoot` した Controller / View、Native EntryCell の `onTextChanged` を 100ms 間隔で 5 回連続発火
- **WHEN** ユーザーが連続的に文字入力する
- **THEN** delegate / listener の `didChangeTextValue` は最終値で 1 回のみ呼ばれる（最後の発火から 200ms 経過後）

#### Scenario: debounce ユーティリティの単体動作

- **GIVEN** Bridge 内部 200ms debounce ユーティリティのインスタンス
- **WHEN** 100ms 間隔で 5 回連続して値（`"a"` / `"ab"` / `"abc"` / `"abcd"` / `"abcde"`）を投入する
- **THEN** ユーティリティのコールバックは最終値 `"abcde"` で 1 回のみ呼ばれる（最後の投入から 200ms 経過後）
