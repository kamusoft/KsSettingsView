## 依存関係

- 前提:
  - `add-monorepo-foundation` / `add-settings-view-*`（archive 済）
  - `add-cell-types-basic` / `add-cell-types-input` / `add-cell-types-custom`: Native 側 Cell が揃っている必要
  - `add-maui-bridge`（先行・archive 必須）: Bridge プロジェクト + Binding csproj + LabelCell 用 API
  - `add-maui-core`（先行・archive 必須）: MAUI 基盤（`SettingsView` / `Section` / `CellBase`）、`SettingsViewHandler`、`CellBaseHandler<,>`、`ApplyDiff` / `BuildAndSetRoot` 経路、cellId Map、`LabelCell` の最小実装、`MauiAppBuilderExtension.AddKsSettingsView()`、メモリリーク対策基盤
  - `add-partial-update-core` / `add-partial-update-native`（先行・archive 必須）: `SettingsRootDiff` / `SettingsRootStore` / Native UI 層の部分更新 API
  - `add-samples-maui`（先行・archive 必須）: Sample 土台（`LabelCell` 1 セクションのみのデモ）
- **実装着手前提条件（必須）**:
  - 本変更提案の MODIFIED delta spec（`specs/maui-bridge/spec.md`）は `maui-bridge` capability を対象とするため、**`add-maui-bridge` の archive 完了**（`openspec/specs/maui-bridge/spec.md` が Source of Truth に存在する状態）を必須前提とする
  - 同様に、`add-maui-core` の archive 完了（`openspec/specs/maui-core/spec.md` 存在）、`add-samples-maui` の archive 完了（`openspec/specs/samples-maui/spec.md` 存在）も必須前提
  - これら 3 提案のいずれかが未 archive の状態では、本変更提案の実装フェーズに進んではならない
- 後続:
  - Phase 2 以降の配信整備、KMP、モダン UI

## Phase A: Bridge API 拡張・準備

> Snapshot テストフレームワーク確定（Phase D の §12.2 を含む）は本 Phase 着手前にユーザー確認を経て決定し、`docs/development.md` に記載する（design.md Open Questions 参照）

## 1. Bridge API 拡張: iOS

`purify-core-extract-style-to-ui-layer` 追随：Color 引数は MAUI 側 `Microsoft.Maui.Graphics.Color` を ObjC 互換受け入れ型で受け、Bridge 内部で `UIColor` に 1 段直接変換する。`KsColor` 中間構造や `KsColorDTO` は経由しない。

- [ ] 1.1 `ios/Sources/KsSettingsViewBridge/KsSettingsViewBuilder.swift` に以下の Cell 追加メソッドを追加（既存の `addLabelCell` と同じパターンで実装、Color 引数は MAUI Color → UIColor 1 段変換）：
  - `addCommandCell(id:title:description:icon:hintText:hideArrow:)`
  - `addButtonCell(id:title:titleColor:)`（titleColor: MAUI Color、Bridge 内部で UIColor に直接変換）
  - `addSwitchCell(id:title:description:isOn:accentColor:)`（accentColor: MAUI Color、同上）
  - `addCheckboxCell(id:title:description:isChecked:accentColor:)`（accentColor: MAUI Color、同上）
  - `addRadioCell(id:title:groupId:value:selectedValue:)`
  - `addSimpleCheckCell(id:title:isChecked:)`
  - `addEntryCell(id:title:description:text:placeholder:keyboardType:)`
  - `addPickerCell(id:title:items:selectedIndex:)`
  - `addTextPickerCell(id:title:items:selectedIndex:)`
  - `addNumberPickerCell(id:title:min:max:step:value:)`
  - `addTimePickerCell(id:title:time:format:)`
  - `addDatePickerCell(id:title:date:format:minDate:maxDate:)`
  - `addCustomCell(id:contentTypeName:contentJson:)`（または同等の C# View 識別子受け取り用 API）
- [ ] 1.2 `KsCellInteractionDelegate.swift` で 13 Cell 種別分の通知メソッド実体を追加：
  - `didTapCommandCell(cellId:)` / `didTapButton(cellId:)`
  - `didChangeBoolValue(cellId:value:)`（Switch / Checkbox 共用）
  - `didChangeRadioSelection(groupId:value:)`
  - `didChangeTextValue(cellId:value:)`（EntryCell debounce 後）
  - `didChangePickerSelection(cellId:index:)`
  - `didChangeNumberValue(cellId:value:)`
  - `didChangeTimeValue(cellId:value:)`
  - `didChangeDateValue(cellId:value:)`
- [ ] 1.3 Bridge 内で各 Native Cell からの delegate 呼び出しを集約（onValueChanged / onTap / onSelected 等を delegate メソッドに転送）
- [ ] 1.4 EntryCell の 200ms debounce を Native 側で適用、最終確定値のみ `didChangeTextValue` で通知

## 2. Bridge API 拡張: Android

`purify-core-extract-style-to-ui-layer` 追随：Color 引数は MAUI 側 `Microsoft.Maui.Graphics.Color` を Java 互換受け入れ型で受け、Bridge 内部で Compose `Color` に 1 段直接変換する。`KsColor` 中間構造や `KsColorDTO` は経由しない。

- [ ] 2.1 `android/ks-settingsview-bridge/.../KsSettingsViewBuilder.kt` に iOS と対称な 13 Cell 追加メソッドを実装（Color 引数は MAUI Color → Compose Color 1 段変換）
- [ ] 2.2 `KsCellInteractionListener.kt` に 13 Cell 種別分の通知メソッド実体を追加（iOS と対称）
- [ ] 2.3 Bridge 内で各 Native Cell からの listener 呼び出しを集約
- [ ] 2.4 EntryCell の 200ms debounce 適用

## 3. MAUI バインディングプロジェクト更新

- [ ] 3.1 `maui/KsSettingsView.Bindings.iOS/`：Bridge API 拡張後に `objective-sharpie` で `ApiDefinitions.cs` を再生成、必要な手動修正を `Patches/` に追加
- [ ] 3.2 `maui/KsSettingsView.Bindings.Android/`：Bridge API 拡張後に aar を再取り込み、Java バインディング再生成
- [ ] 3.3 `dotnet build -f net9.0-ios` および `-f net9.0-android` が警告なし成功

## Phase B: MAUI Cell 実装

## 4. 基本 Cell の MAUI 実装（6 種）

- [ ] 4.1 `Cells/CommandCell.cs` + `Handlers/CommandCellHandler.cs` + `.iOS/.Android.cs` を実装（`Command: ICommand`、`CommandParameter` BindableProperty + 双方向の Tap 通知）
- [ ] 4.2 `Cells/ButtonCell.cs` + Handler 群
- [ ] 4.3 `Cells/SwitchCell.cs` + Handler 群（`On` を `BindingMode.TwoWay` で実装、Native delegate 通知 → SetValue）
- [ ] 4.4 `Cells/CheckboxCell.cs` + Handler 群（同上、`IsChecked` 双方向）
- [ ] 4.5 `Cells/RadioCell.cs` + Handler 群（`SelectedValue` 双方向）
- [ ] 4.6 `Cells/SimpleCheckCell.cs` + Handler 群（`IsChecked` は OneWay）

## 5. 入力系 Cell の MAUI 実装（6 種）

- [ ] 5.1 `Cells/EntryCell.cs` + Handler 群（`ValueText` 双方向、`KeyboardType` enum マッピング、Bridge `UpdateCellValue` 直行パス）
- [ ] 5.2 `Cells/PickerCell.cs` + Handler 群（`ItemsSource: IList`、`SelectedItem` 双方向）
- [ ] 5.3 `Cells/TextPickerCell.cs` + Handler 群
- [ ] 5.4 `Cells/NumberPickerCell.cs` + Handler 群（`Min` / `Max` / `Step` / `Number` 双方向）
- [ ] 5.5 `Cells/TimePickerCell.cs` + Handler 群（`Time: TimeSpan` 双方向、KsTime 変換）
- [ ] 5.6 `Cells/DatePickerCell.cs` + Handler 群（`Date: DateTime` 双方向、KsDate 変換）

## 6. CustomCell の MAUI 実装

- [ ] 6.1 `Cells/CustomCell.cs` を実装（`ContentTemplate: DataTemplate`、`BindingContext` 連動）
- [ ] 6.2 `Handlers/CustomCellHandler.iOS.cs` で C# View（`Microsoft.Maui.Controls.View`）を `MauiView.ToPlatform(MauiContext)` で UIView に変換し Bridge 経由で渡す
- [ ] 6.3 `Handlers/CustomCellHandler.Android.cs` で同等の処理（C# View → Android View）
- [ ] 6.4 ライフサイクル管理：DataTemplate からのインスタンス化、Cell 再利用時の BindingContext 更新

## 7. ItemsSource / ItemTemplate 実装

- [ ] 7.1 `SettingsView.cs` に `ItemsSourceProperty: IList` と `ItemTemplateProperty: DataTemplate`（`DataTemplateSelector` も受理可能）を追加（旧 AiForms 互換、design.md Decision 4）
- [ ] 7.2 `Section.cs` に `ItemsSourceProperty: IList` と `ItemTemplateProperty: DataTemplate` を追加
- [ ] 7.3 `SettingsViewHandler` の `BuildAndSetRoot()` および ItemsSource 連動経路を拡張：
  - `VirtualView.ItemsSource` が非 null の場合は `ItemTemplate` を各要素に適用して動的に `Section` インスタンス列を生成、それ以外は静的 `Sections` を走査
  - 各 `Section.ItemsSource` が非 null の場合も `ItemTemplate` を適用して動的に `CellBase` インスタンス列を生成、それ以外は静的 `Cells` を走査
  - 動的生成された `Section` / `CellBase` の `BindingContext` には対応するソース要素を設定
- [ ] 7.4 `SettingsViewHandler` に `ItemsSource` 購読管理を実装：
  - `VirtualView.ItemsSource` が `INotifyCollectionChanged` を実装している場合、`CollectionChanged` を購読して `NotifyCollectionChangedAction` 種別に応じた `ApplyDiff` 経路（`InsertSection` / `RemoveSection` / `MoveSection` / `ReplaceSection`、`Reset` のみ `BuildAndSetRoot`）を呼ぶ
  - 各 `Section.ItemsSource` も同様に購読し、`InsertCell` / `RemoveCell` / `MoveCell` / `ReplaceCell` の各 Diff を発行する
  - `propertyChanged` を捕捉し、旧コレクションの購読解除と新コレクションの購読登録を行う
  - 購読中ソースの参照を `List<INotifyCollectionChanged>` 等で保持し、`DisconnectHandler` 時に全購読解除

## 8. MauiAppBuilder 拡張の Cell 登録追加

- [ ] 8.1 `MauiAppBuilderExtension.cs` の `AddKsSettingsView()` に 13 Cell Handler の登録を追加（`add-maui-core` で登録された `SettingsViewHandler` + `LabelCellHandler` に加えて全 13 種類）
- [ ] 8.2 オプションで個別 Handler を上書きできる API（`AddKsSettingsView(configure: handlers => ...)`）を整備

## Phase C: Sample 拡張・テスト

## 9. Sample アプリへのページ追加

- [ ] 9.1 `samples/maui/Pages/BasicCellsPage.xaml` を新規作成（基本 6 種 Cell の表示・操作デモ）
- [ ] 9.2 `samples/maui/Pages/InputCellsPage.xaml` を新規作成（入力 6 種 Cell の表示・操作デモ）
- [ ] 9.3 `samples/maui/Pages/CustomCellPage.xaml` を新規作成（プロフィールカード Composition 例）
- [ ] 9.4 `samples/maui/MainPage.xaml` に各ページへのナビゲーション導線を追加（ボタンまたはリンク）
- [ ] 9.5 各ページの BindingContext として例示用 ViewModel を作成（`SwitchCell.On` ↔ `IsEnabled` の双方向バインド等を実演）

## 10. テスト（Handler 動作・ItemsSource）

- [ ] 10.1 `SettingsViewHandlerItemsSourceTests.cs`：`SettingsView.ItemsSource` に `ObservableCollection<SectionVM>` + `ItemTemplate` を指定した状態で、(1) 初期表示で要素数分の Section が `Bridge.SetRoot` に渡される、(2) `Add` / `Remove` / `Replace` / `Reset` で `Bridge.SetRoot` が 1 回ずつ呼ばれる、(3) 各 Section の `BindingContext` に対応要素が設定される、(4) `ItemsSource = null` で旧コレクションの購読が解除される、ことを検証
- [ ] 10.2 `SectionItemsSourceTests.cs`：`Section.ItemsSource` に `ObservableCollection<AccountVM>` + `ItemTemplate` で `SwitchCell` を生成する構成で、(1) 初期表示で要素数分の Cell、(2) `Add` / `Remove` で `Bridge.SetRoot` が 1 回ずつ、(3) BindingContext 経由の双方向バインドが成立、(4) `DisconnectHandler` で全 `CollectionChanged` 購読解除、を検証
- [ ] 10.3 `ItemsSourceNativeBoundaryTests.cs`：Native（iOS Swift / Android Kotlin）公開シンボル一覧から `ItemsSource` 相当 API が**追加されていない**ことを検証する静的チェックテスト（design.md Decision 4 の Native 層責務境界）
- [ ] 10.4 各 Cell の TwoWay バインドテスト（`SwitchCellTwoWayTests.cs` / `EntryCellDebounceTests.cs` 等）：cellId Map 経由でデリゲートコールバックが C# プロパティに反映されることを検証
- [ ] 10.5 `CustomCellHandlerTests.cs`：DataTemplate からの View 生成と `MauiView.ToPlatform` 変換、BindingContext 連動を検証

## 11. メモリリーク対策の 13 Cell 適用

- [ ] 11.1 各 Cell Handler の `DisconnectHandler` で Native Cell 参照を解放（`add-maui-core` パターン適用）
- [ ] 11.2 `MauiSettingsViewLeakTests` を 14 Cell 全てを含むケースに拡張、10 回 push/pop で WeakReference 全てゼロを検証

## Phase D: Snapshot テスト・ドキュメント・最終確認

## 12. Snapshot テスト

- [ ] 12.1 **【Phase A 開始前の前提条件】** Snapshot フレームワークを選定（候補：Verify + Microsoft.Maui.TestUtils）し、ユーザー確認の上で確定し、`docs/development.md` に記載する。本タスクは spec.md `Requirement: Snapshot テスト基盤` の MUST 記述に対応し、`maui/KsSettingsView.Maui.SnapshotTests/` プロジェクトの新設より前に完了させる
- [ ] 12.2 `maui/KsSettingsView.Maui.SnapshotTests/` プロジェクトを新設、`KsSettingsView.slnx` に登録（12.1 で選定したフレームワーク準拠）
- [ ] 12.3 基本 7 種 Cell（LabelCell 含む）のレンダリング Snapshot テストを実装
- [ ] 12.4 入力 6 種 Cell のレンダリング Snapshot テストを実装
- [ ] 12.5 CustomCell のレンダリング Snapshot テストを実装
- [ ] 12.6 CI で `dotnet test` が成功するように整備（CI 環境のセットアップ含む）

## 13. ドキュメント

- [ ] 13.1 `docs/migration-from-aiforms.md` を新規作成し、以下のセクションを含む：
  - 概要・対象読者
  - 名前空間変更（`AiForms.Settings` → `KsSettingsView.Maui`）
  - 初期化コード差し替え（`AddSettingsViewHandler` → `AddKsSettingsView`、`UseSettingsView(true)` フックの再実装）
  - Cell プロパティ対応表（旧 → 新）の全 15 Cell 分
  - Sample の差し替え例
  - ItemsSource / ItemTemplate の利用例
  - EntryCell の 200ms debounce 仕様の注意点
- [ ] 13.2 `docs/maui-bindings.md`（`add-maui-bridge` で作成済）に「全 Cell 用 `addXxxCell` メソッドが本提案で追加された」旨の更新を追記

## 14. 全テスト・ビルド確認

- [ ] 14.1 Native iOS / Android の Bridge テスト（既存）が影響を受けず全成功
- [ ] 14.2 `maui/` 全プロジェクトの `dotnet build` が `-f net9.0-ios` / `-f net9.0-android` で警告なし成功
- [ ] 14.3 `dotnet test` で `KsSettingsView.Maui.Tests`（`add-maui-core` で整備）と本提案の追加テスト全てが成功
- [ ] 14.4 Snapshot テストが通る
- [ ] 14.5 拡張版リーク検出テストがゼロリーク
- [ ] 14.6 `samples/maui/` を iOS シミュレータと Android エミュレータで起動し、MainPage から各 Cell ページに遷移して全 Cell の表示・操作・双方向バインドを目視確認

## 完了条件

- 全タスクのチェックボックスが完了している
- `maui-cells` capability の全 Scenario が通る
- 13 Cell 全てが MAUI XAML から利用可能で、双方向バインドが正しく動作
- `ItemsSource` / `ItemTemplate` による動的バインドが `SettingsView` と `Section` の両方で動作
- `samples/maui/` で 14 Cell（LabelCell + 13 種類）全てが目視確認可能
- 全自動テスト（Handler / ItemsSource / Snapshot / Leak）が成功
- `docs/migration-from-aiforms.md` が完成し、旧 AiForms ユーザーが移行可能な情報を網羅
