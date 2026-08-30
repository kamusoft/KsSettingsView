## ADDED Requirements

### Requirement: 13 Cell の BindableObject 定義

`KsSettingsView.Maui` には、`add-maui-core` で実装済の `LabelCell` に加えて、以下 13 種類の `CellBase` 派生クラスが存在しなければならない (SHALL)：

- 基本 Cell（6 種）: `CommandCell` / `ButtonCell` / `SwitchCell` / `CheckboxCell` / `RadioCell` / `SimpleCheckCell`
- 入力 Cell（6 種）: `EntryCell` / `PickerCell` / `TextPickerCell` / `NumberPickerCell` / `TimePickerCell` / `DatePickerCell`
- Custom Cell（1 種）: `CustomCell`

各 Cell は旧 `AiForms.Maui.SettingsView` の対応 Cell の主要 BindableProperty（プロパティ名・型・デフォルト値・BindingMode）を保持しなければならない (MUST)。詳細は `docs/legacy-aiforms-reference.md` §3 を参照する。

#### Scenario: 各 Cell クラスの存在

- **GIVEN** `KsSettingsView.Maui` アセンブリ
- **WHEN** `Cells/<Name>Cell.cs`（13 種類）のクラス定義を確認する
- **THEN** 全 13 クラスが `CellBase` 派生で存在し、それぞれの固有プロパティが `BindableProperty.Create(...)` で宣言されている

#### Scenario: CommandCell のプロパティ

- **GIVEN** `Cells/CommandCell.cs`
- **WHEN** クラス定義を確認する
- **THEN** `CommandProperty: ICommand`、`CommandParameterProperty: object` が宣言されている

#### Scenario: SwitchCell のプロパティ

- **GIVEN** `Cells/SwitchCell.cs`
- **WHEN** クラス定義を確認する
- **THEN** `OnProperty: bool`（デフォルト false、BindingMode TwoWay）、`OnColorProperty: Color` が宣言されている

#### Scenario: EntryCell のプロパティ

- **GIVEN** `Cells/EntryCell.cs`
- **WHEN** クラス定義を確認する
- **THEN** `ValueTextProperty: string`（デフォルト null、BindingMode TwoWay）、`PlaceholderProperty: string`、`KeyboardTypeProperty: KsKeyboardType` が宣言されている

#### Scenario: CustomCell のプロパティ

- **GIVEN** `Cells/CustomCell.cs`
- **WHEN** クラス定義を確認する
- **THEN** `ContentTemplateProperty: DataTemplate` が宣言され、`BindingContext` 連動の挙動を持つ

### Requirement: 13 Cell の Handler 実装

`KsSettingsView.Maui` には、`add-maui-core` で実装済の `LabelCellHandler` に加えて、13 Cell 種別ごとの Handler クラスが存在しなければならない (SHALL)。各 Handler は `add-maui-core` で確立された `CellBaseHandler<TVirtualCell, TNativeCell>` 派生で実装され、`PropertyMapper` は親 `SettingsViewHandler.ApplyDiff/BuildAndSetRoot()` をトリガする薄いラッパでなければならない (MUST)。各 Handler が Bridge へ Cell を積む際に呼ぶ Bridge API（`addXxxCell(...)` 等）の仕様は、本提案の `maui-bridge` MODIFIED delta spec（`specs/maui-bridge/spec.md`）の `Requirement: Bridge Builder API` を参照する。

`ButtonCell.TitleColor` / `SwitchCell.OnColor` / `CheckboxCell.AccentColor` 等の `Microsoft.Maui.Graphics.Color` 型プロパティは、Handler の PropertyMapper 経由で Bridge `addXxxCell(...)` / `applyDiff(ReplaceCell)` の Color 引数として MAUI Color のまま渡し、**Bridge 内部で Native `UIColor` / Compose `Color` への 1 段直接変換**が行われる（`add-maui-bridge` の `Bridge Builder API` Requirement に追加された Color 変換規約に従う）。Handler 側で `KsColor` 等の Core 経由中間変換は行わない (MUST NOT、`purify-core-extract-style-to-ui-layer` で `KsColor` は Core から削除済み)。

EntryCell の `ValueText` プロパティのみ、`ApplyDiff/BuildAndSetRoot` を経由せず `Bridge.UpdateCellValue` 直行パスで Native debounce 後の値を反映しなければならない (MUST)。

#### Scenario: 各 Cell Handler の存在

- **GIVEN** `KsSettingsView.Maui` アセンブリ
- **WHEN** `Handlers/<Name>CellHandler.cs`（13 種類）および `.iOS.cs` / `.Android.cs` のクラス定義を確認する
- **THEN** 全 13 Handler が `CellBaseHandler<<Name>Cell, Native<Name>Cell>` 派生で存在し、iOS / Android のパーシャル実装に分割されている

#### Scenario: PropertyMapper が ApplyDiff(ReplaceCell) をトリガ（SwitchCell 例）

- **GIVEN** SwitchCell が登録された SettingsView と Handler、Bridge モック記録モード
- **WHEN** SwitchCell の `On` プロパティを `false` → `true` に変更する
- **THEN** Bridge モックの `ApplyDiff` 記録が 1 回増え、引数 DTO は `KsSettingsRootDiffReplaceCellDTO` 相当である（`SetRoot` は呼ばれない）

#### Scenario: EntryCell の高頻度更新パス

- **GIVEN** EntryCell が登録された SettingsView、Bridge モック記録モード
- **WHEN** EntryCell の `ValueText` を `"a"` → `"ab"` → `"abc"` と 100ms 間隔で連続変更する
- **THEN** Bridge モックの `SetRoot` / `ApplyDiff` 記録は変更されず、`UpdateCellValue` が最終値 `"abc"` で 1 回のみ呼ばれる（200ms debounce 後）

### Requirement: 双方向バインドの実装

`SwitchCell.On` / `CheckboxCell.IsChecked` / `RadioCell.SelectedValue` / `EntryCell.ValueText` / `PickerCell.SelectedItem` / `TextPickerCell.SelectedItem` / `NumberPickerCell.Number` / `TimePickerCell.Time` / `DatePickerCell.Date` の各プロパティは `BindingMode.TwoWay` で実装されなければならない (SHALL)。Native 側のユーザー操作通知は `add-maui-core` で確立された cellId Map 経路を経由して、対応 Cell の `SetValue(...Property, value)` で C# 側に書き戻されなければならない (MUST)。

SimpleCheckCell の `IsChecked` は **OneWay**（旧 AiForms 仕様準拠）であり、Native 側通知経路を持たない (MUST)。

#### Scenario: SwitchCell の TwoWay バインド（C# → Native）

- **GIVEN** SwitchCell が登録された SettingsView、`On` プロパティが ViewModel の `IsEnabled` に TwoWay バインド
- **WHEN** ViewModel の `IsEnabled` を `false` → `true` に変更する
- **THEN** Bridge `SetRoot` 経由で Native SwitchCell の `isOn` が `true` に更新される

#### Scenario: SwitchCell の TwoWay バインド（Native → C#）

- **GIVEN** SwitchCell が表示中、ユーザーが UISwitch / SwitchCompat をタップして OFF → ON にする
- **WHEN** Native の `onValueChanged` が発火し、Bridge `didChangeBoolValue(cellId, true)` が delegate / listener に通知される
- **THEN** Handler の cellId Map から対応 SwitchCell が引かれ、`SetValue(OnProperty, true)` で C# 側に反映、ViewModel の `IsEnabled` が `true` に更新される

#### Scenario: RadioCell の TwoWay バインド（Native → C#、groupId 経由）

- **GIVEN** 同一 `GroupId` の RadioCell が 3 つ表示中（`SelectedValue` プロパティが ViewModel の `SelectedRadio` に TwoWay バインド）、現在 `value="A"` の RadioCell が選択中
- **WHEN** ユーザーが `value="B"` の RadioCell をタップする、Native の `onSelected` が発火し Bridge `didChangeRadioSelection(groupId, "B")` が delegate / listener に通知される
- **THEN** Handler の cellId Map から `groupId` 経由で対応 RadioCell 群が引かれ、各 RadioCell の `SelectedValueProperty` が `"B"` に SetValue され、ViewModel の `SelectedRadio` が `"B"` に更新される

#### Scenario: SimpleCheckCell の OneWay 仕様

- **GIVEN** SimpleCheckCell の `IsChecked` プロパティ宣言
- **WHEN** `BindableProperty.Create(...)` の引数を確認する
- **THEN** `defaultBindingMode: BindingMode.OneWay` が指定されている、または `BindableProperty.DefaultBindingMode` が OneWay である

### Requirement: SettingsView の ItemsSource / ItemTemplate

`SettingsView` クラスには `ItemsSourceProperty: IList`（`BindableProperty`、デフォルト null）と `ItemTemplateProperty: DataTemplate`（`BindableProperty`、デフォルト null）が追加されなければならない (SHALL)。`ItemTemplate` には `DataTemplateSelector` も受理可能でなければならない (MUST)。`ItemsSource` 非 null 時、`SettingsViewHandler.ApplyDiff/BuildAndSetRoot()` は `ItemTemplate` を各要素に適用して動的に `Section` インスタンスを生成し、`BindingContext` を対応要素に設定しなければならない (MUST)。

`ItemsSource` が `INotifyCollectionChanged` を実装している場合、`SettingsViewHandler` は `CollectionChanged` を内部で購読し、変更時に `ApplyDiff/BuildAndSetRoot()` を呼ばなければならない (MUST)。

#### Scenario: ItemsSource プロパティの存在

- **GIVEN** `SettingsView` クラス
- **WHEN** 公開 BindableProperty を確認する
- **THEN** `ItemsSourceProperty` と `ItemTemplateProperty` が宣言されている

#### Scenario: ObservableCollection への動的バインド

- **GIVEN** `SettingsView.ItemsSource = new ObservableCollection<SectionVM>()` および `SettingsView.ItemTemplate = sectionTemplate`、Bridge モック記録モード
- **WHEN** `ItemsSource.Add(new SectionVM { Title = "X" })` を実行する
- **THEN** Bridge `SetRoot` が 1 回呼ばれ、引数 `KsSettingsRootDTO` に動的生成された Section が含まれる

#### Scenario: ItemsSource null リセットでの購読解除

- **GIVEN** ObservableCollection を `ItemsSource` に設定済、購読中
- **WHEN** `SettingsView.ItemsSource = null` を実行する
- **THEN** 旧 ObservableCollection の `CollectionChanged` 購読が解除され、Bridge `SetRoot` が 1 回呼ばれて空セクション列となる

### Requirement: Section の ItemsSource / ItemTemplate

`Section` クラスには `ItemsSourceProperty: IList`（`BindableProperty`、デフォルト null）と `ItemTemplateProperty: DataTemplate`（`BindableProperty`、デフォルト null）が追加されなければならない (SHALL)。`Section.Cells`（静的）と `Section.ItemsSource`（動的）の**同時設定は禁止**であり、`ItemsSource` 設定時は `Cells` を空として扱わなければならない (MUST)。

`SettingsViewHandler.ApplyDiff/BuildAndSetRoot()` は各 `Section.ItemsSource` が非 null の場合に `ItemTemplate` を各要素に適用して動的に `CellBase` インスタンスを生成し、`BindingContext` を対応要素に設定しなければならない (MUST)。

#### Scenario: Section の ItemsSource プロパティの存在

- **GIVEN** `Section` クラス
- **WHEN** 公開 BindableProperty を確認する
- **THEN** `ItemsSourceProperty` と `ItemTemplateProperty` が宣言されている

#### Scenario: BindingContext 経由の双方向バインド

- **GIVEN** `Section.ItemsSource = ObservableCollection<AccountVM>` + `ItemTemplate` で SwitchCell を生成（`On = {Binding IsEnabled, Mode=TwoWay}`）、ユーザーが Switch をタップ
- **WHEN** Native `didChangeBoolValue(cellId, true)` が発火する
- **THEN** SwitchCell の `OnProperty` が `true` になり、`BindingContext.IsEnabled` も `true` に更新される

#### Scenario: 静的 Cells と動的 ItemsSource の同時設定禁止

- **GIVEN** `Section` インスタンス
- **WHEN** `Cells.Add(new LabelCell())` と `ItemsSource = ...` を両方設定する
- **THEN** 設定された方（後者）が優先され、もう一方が空として扱われる、または例外が発生する（実装方針はドキュメントコメントに明記）

### Requirement: Native 層への ItemsSource 概念の不持ち込み

`KsSettingsViewCore` / `KsSettingsViewUI`（iOS: Swift）および `ks-settingsview-core` / `ks-settingsview-ui`（Android: Kotlin）の公開シンボル一覧には、`Section(items:template:)` / `itemsSource(_:template:)` / `section(items:itemContent:)` 等の `ItemsSource` 相当 API が**追加されてはならない** (MUST NOT)。

#### Scenario: Native iOS 層の公開 API チェック

- **GIVEN** `ios/Sources/KsSettingsViewCore/` / `ios/Sources/KsSettingsViewUI/` の公開シンボル一覧
- **WHEN** `Section` 型のイニシャライザ・メソッドを確認する
- **THEN** `items:template:` / `itemsSource:` 等のパラメータ・メソッドが存在しない

#### Scenario: Native Android 層の公開 API チェック

- **GIVEN** `android/ks-settingsview-core/` / `android/ks-settingsview-ui/` の公開シンボル一覧
- **WHEN** `Section` クラスのコンストラクタ・メソッド・拡張関数を確認する
- **THEN** `items: List<T>, template: (T) -> Cell` 相当のシグネチャが存在しない

### Requirement: CustomCell Handler の振る舞い

`CustomCellHandler` は、DataTemplate からインスタンス化された C# View（`Microsoft.Maui.Controls.View`）を `MauiView.ToPlatform(MauiContext)` でネイティブ View（iOS: `UIView`、Android: `View`）に変換し、Bridge 経由で `KsAnyView`（iOS: `KsAnyView.uiKit`、Android: `KsAnyView.AndroidView`）として Native CustomCell の content に格納しなければならない (SHALL)。Cell 再利用時の `BindingContext` 更新は標準 MAUI 機構（`BindableObject.BindingContext` の自動伝播）に乗らなければならない (MUST)。

#### Scenario: CustomCell の Native View 変換

- **GIVEN** `CustomCell.ContentTemplate = template`（`<Label Text="{Binding Name}" />` を生成）、`BindingContext = profileVM`
- **WHEN** Handler が `MauiView.ToPlatform(MauiContext)` を呼ぶ
- **THEN** ネイティブ View（iOS: `UILabel`、Android: `TextView`）が返り、Bridge `addCustomCell` 経由で Native CustomCell に渡される

#### Scenario: BindingContext 連動

- **GIVEN** CustomCell が表示中、`BindingContext = vm1`
- **WHEN** `BindingContext = vm2` に変更する
- **THEN** ContentTemplate 内の Binding 式が `vm2` のプロパティに自動的に再バインドされる

### Requirement: MauiAppBuilder 拡張の追加 Cell 登録

`add-maui-core` で実装された `MauiAppBuilderExtension.AddKsSettingsView()` は、本提案により 13 Cell Handler の登録が追加されなければならない (SHALL)。

#### Scenario: 全 Cell Handler 登録

- **GIVEN** `MauiAppBuilder` インスタンス
- **WHEN** `builder.AddKsSettingsView()` を呼ぶ
- **THEN** `SettingsViewHandler` + 14 Cell Handler（LabelCell + 13 種類）の合計 15 個が `ConfigureMauiHandlers` 経由で登録される

#### Scenario: オプションでの個別 Handler 上書き

- **GIVEN** ユーザー側で `MySwitchCellHandler` を定義
- **WHEN** `builder.AddKsSettingsView(configure: handlers => handlers.AddHandler<SwitchCell, MySwitchCellHandler>())` を呼ぶ
- **THEN** SwitchCell の Handler が `MySwitchCellHandler` で上書き登録される

### Requirement: Sample アプリへの Cell ページ追加

`add-samples-maui` で整備された `samples/maui/` の Sample アプリには、本提案により以下のページ追加が行われなければならない (SHALL)：

- 基本 Cell ページ（CommandCell / ButtonCell / SwitchCell / CheckboxCell / RadioCell / SimpleCheckCell の表示と操作）
- 入力 Cell ページ（EntryCell / PickerCell / TextPickerCell / NumberPickerCell / TimePickerCell / DatePickerCell の表示と操作）
- CustomCell ページ（プロフィールカード Composition 例）
- MainPage から各ページへのナビゲーション導線

#### Scenario: 基本 Cell ページの存在

- **GIVEN** `samples/maui/Pages/`（または同等のディレクトリ）
- **WHEN** XAML ページ一覧を確認する
- **THEN** 基本 6 種・入力 6 種・CustomCell の合計 3 ページ以上（カテゴリ別または Cell 別に分割可能）が存在する

#### Scenario: MainPage からのナビゲーション

- **GIVEN** Sample アプリを起動した直後
- **WHEN** MainPage を確認する
- **THEN** 「基本 Cell」「入力 Cell」「CustomCell」（または同等のカテゴリ名）へのナビゲーションボタンまたはリンクが存在する

#### Scenario: 基本 Cell ページでの操作可能性

- **GIVEN** Sample アプリで基本 Cell ページに遷移
- **WHEN** SwitchCell をタップする
- **THEN** Switch の状態が切り替わり、画面上の表示（例: ラベル）が連動して変化する（双方向バインドの目視確認）

### Requirement: Snapshot テスト基盤

`maui/KsSettingsView.Maui.SnapshotTests/` プロジェクトが新設されなければならない (SHALL)。本プロジェクトは、基本 7 種（LabelCell 含む）+ 入力 6 種 + Custom の各 Cell のレンダリング Snapshot テストを実装しなければならない (MUST)。Snapshot テストフレームワークは Phase A 実装着手前にユーザー確認を経て確定され、`docs/development.md` に記載されなければならない (MUST)。

#### Scenario: SnapshotTests プロジェクトの存在

- **GIVEN** リポジトリのクローン直後
- **WHEN** `maui/KsSettingsView.Maui.SnapshotTests/` を確認する
- **THEN** C# テストプロジェクトが存在し、`KsSettingsView.slnx` に登録されている

#### Scenario: 全 Cell の Snapshot テスト

- **GIVEN** SnapshotTests プロジェクト
- **WHEN** テスト一覧を確認する
- **THEN** 14 Cell 全て（LabelCell + 13 種類）の Snapshot テストが存在し、`dotnet test` で全成功する

#### Scenario: Snapshot ゴールデンイメージの管理

- **GIVEN** Snapshot テストの初回実行
- **WHEN** ゴールデンイメージファイル生成と承認フローを確認する
- **THEN** ゴールデンイメージファイルがバージョン管理され、CI で差分検出が可能（差分時はテスト失敗）

### Requirement: 移行ガイド

`docs/migration-from-aiforms.md` が新規作成され、以下のセクションを含まなければならない (SHALL)：

1. 概要・対象読者
2. 名前空間変更（`AiForms.Settings` → `KsSettingsView.Maui`）
3. 初期化コード差し替え（`AddSettingsViewHandler` → `AddKsSettingsView`、`UseSettingsView(true)` フックの再実装）
4. Cell プロパティ対応表（旧 → 新）の全 15 Cell 分（LabelCell + 13 種類 + 旧 AiForms 固有プロパティの非対応リスト）
5. Sample の差し替え例
6. ItemsSource / ItemTemplate の利用例
7. EntryCell の 200ms debounce 仕様の注意点

#### Scenario: 移行ガイドの存在

- **GIVEN** リポジトリのクローン直後
- **WHEN** `docs/migration-from-aiforms.md` を確認する
- **THEN** 上記 7 セクションを含むドキュメントが存在する

#### Scenario: Cell プロパティ対応表の網羅性

- **GIVEN** `docs/migration-from-aiforms.md` の Cell プロパティ対応表
- **WHEN** 各 Cell の項目を確認する
- **THEN** LabelCell + 13 種類の Cell について、旧 AiForms の BindableProperty 名と新 KsSettingsView.Maui での対応名・差分が表形式で記載されている

### Requirement: メモリリーク対策の 13 Cell 適用

`add-maui-core` で確立された `DisconnectHandler` パターンは、本提案により 13 Cell Handler 全てに適用されなければならない (SHALL)。`MauiSettingsViewLeakTests` は 13 Cell 全てを含むケースに拡張されなければならない (MUST)。

#### Scenario: 各 Cell Handler の DisconnectHandler

- **GIVEN** 各 Cell Handler のクラス定義
- **WHEN** `DisconnectHandler` メソッドを確認する
- **THEN** Native Cell 参照の解放と、必要に応じた購読解除が実装されている

#### Scenario: 全 Cell を含むリーク検出テスト

- **GIVEN** `MauiSettingsViewLeakTests` の拡張版
- **WHEN** 14 Cell 全てを含む SettingsView を Page に配置し 10 回 push/pop して `dotnet test --filter MauiSettingsViewLeakTests` を実行する
- **THEN** `WeakReference<SettingsView>` および各 Cell の `WeakReference` が GC 後に全て `IsAlive == false` となり、テスト成功
