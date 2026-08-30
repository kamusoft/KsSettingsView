## Why

`add-maui-bridge`（Native Bridge + Binding csproj）と `add-maui-core`（MAUI 本体基盤 + `LabelCell` 1 種類）が完成した後、旧 `AiForms.Maui.SettingsView` ユーザーが期待する全 Cell 群（13 種類）を `KsSettingsView.Maui` で利用可能にする。本提案は元 `add-maui-bindings` 1 提案を 3 分割した第 3 段（最終段）で、(1) 残り 13 Cell の MAUI `BindableObject` + Handler 実装、(2) `samples/maui/` への各 Cell 表示ページ追加、(3) 旧 AiForms 互換の `ItemsSource` / `ItemTemplate`、(4) Snapshot テスト基盤、(5) 移行ガイドを担当する。

本提案は元 `add-maui-bindings` をリネーム + 縮小したものであり、Native Bridge / Binding csproj / MAUI 本体基盤に関する責務は `add-maui-bridge` / `add-maui-core` へ移譲済。capability 名も `maui-bindings` → `maui-cells` にリネームし、責務を以下に明確化する：

1. **Bridge API への 13 Cell 追加メソッド実装**（`add-maui-bridge` の `maui-bridge` capability に MODIFIED 要件として反映）
2. 13 Cell の MAUI Handler 実装
3. `samples/maui/` への Cell ページ追加
4. `ItemsSource` / `ItemTemplate` 動的バインド
5. Snapshot テスト基盤
6. 移行ガイド `docs/migration-from-aiforms.md`

## What Changes

- **13 Cell の MAUI Handler 実装**（`add-maui-core` で確立した `CellBaseHandler<TVirtualCell, TNativeCell>` パターンをパターン適用）：
  - 基本 Cell（6 種）: `CommandCell` / `ButtonCell` / `SwitchCell` / `CheckboxCell` / `RadioCell` / `SimpleCheckCell`
  - 入力 Cell（6 種）: `EntryCell` / `PickerCell` / `TextPickerCell` / `NumberPickerCell` / `TimePickerCell` / `DatePickerCell`
  - Custom Cell（1 種）: `CustomCell`（`ContentTemplate: DataTemplate`、`BindingContext` 連動、C# View → Native View 変換）
- **Bridge API 拡張**（`add-maui-bridge` の Bridge プロジェクトに対する追加実装）：
  - 各 Cell 用 `addXxxCell(...)` メソッドを Builder に追加（iOS Swift / Android Kotlin 両方）
  - Color 引数（`addButtonCell(titleColor:)`、`addSwitchCell(accentColor:)` 等）は MAUI `Microsoft.Maui.Graphics.Color` を受け取り、Bridge 内部で Native `UIColor` / Compose `Color` に 1 段直接変換する（`purify-core-extract-style-to-ui-layer` 方針追随、`KsColor` 中間構造や `KsColorDTO` は経由しない）
  - `KsCellInteractionDelegate` / `KsCellInteractionListener` の 13 種類分のメソッド実体実装
  - EntryCell の Native 側 200ms debounce + `updateCellValue` 直行パス
- **`MauiAppBuilderExtension.AddKsSettingsView()` 拡張**：
  - `add-maui-core` で登録された `SettingsViewHandler` + `LabelCellHandler` に加えて、13 Cell Handler を登録
- **双方向バインドの実装**：
  - `SwitchCell.On` / `CheckboxCell.IsChecked` / `RadioCell.SelectedValue` / `EntryCell.ValueText` / `PickerCell.SelectedItem` / `NumberPickerCell.Number` / `TimePickerCell.Time` / `DatePickerCell.Date` を `BindingMode.TwoWay` で実装（`add-maui-core` の cellId Map 経路を利用）
- **ItemsSource / ItemTemplate（旧 AiForms 互換、XAML 専用 API）**：
  - `SettingsView.ItemsSource: IList` + `SettingsView.ItemTemplate: DataTemplate`（テンプレートは `Section` を生成）：View 全体を動的 Section 列にバインド
  - `Section.ItemsSource: IList` + `Section.ItemTemplate: DataTemplate`（テンプレートは `CellBase` を生成）：Section 内の Cell 列を動的にバインド
  - `Section.Cells`（静的）と `Section.ItemsSource`（動的）の併用を**禁止**
  - `ObservableCollection<T>` の `CollectionChanged` を内部購読し、変更時は `add-maui-core` で確立された `ApplyDiff()` / `BuildAndSetRoot()` 経路に統合（旧 AiForms.Maui.NativeCollectionView `OnCellCollectionChanged` 流儀）
  - Native (iOS / Android Core / UI) 層には**変更を加えない**（SwiftUI / Compose ユーザーは言語標準ループを使う、`ItemsSource` 概念は MAUI XAML レイヤ専用）
- **`samples/maui/` への Cell 表示ページ追加**（`add-samples-maui` で土台が整備された Sample に対する拡張）：
  - 基本 6 種（CommandCell / ButtonCell / SwitchCell / CheckboxCell / RadioCell / SimpleCheckCell）の表示ページ
  - 入力 6 種の表示ページ
  - CustomCell の表示ページ（プロフィールカード Composition）
  - ナビゲーション導線（MainPage から各 Cell カテゴリページへの遷移）
- **Snapshot テスト基盤**：
  - `maui/KsSettingsView.Maui.SnapshotTests/` プロジェクトを新設（Snapshot フレームワークは `Verify + Microsoft.Maui.TestUtils` 等を検討）
  - 基本 7 種（LabelCell 含む）+ 入力 6 種 + Custom の各 Cell のレンダリング Snapshot テストを実装
- **移行ガイド `docs/migration-from-aiforms.md`**：
  - 名前空間変更（`AiForms.Settings` → `KsSettingsView.Maui`）
  - 初期化コード差し替え（`AddSettingsViewHandler` → `AddKsSettingsView`）
  - Cell プロパティ対応表（旧 → 新）
  - Sample の差し替え例
- **メモリリーク対策の徹底**：
  - `add-maui-core` で確立した `DisconnectHandler` パターンを 13 Cell Handler 全てに適用
  - 既存の `MauiSettingsViewLeakTests` を 13 Cell 全て含むケースに拡張

## Capabilities

### New Capabilities
- `maui-cells`: `KsSettingsView.Maui` の 13 Cell（Command / Button / Switch / Checkbox / Radio / SimpleCheck / Entry / Picker / TextPicker / NumberPicker / TimePicker / DatePicker / Custom）の `BindableObject` 定義と Handler 実装、双方向バインド、`ItemsSource` / `ItemTemplate` 動的バインド、`samples/maui/` への Cell ページ追加、Snapshot テスト、移行ガイドの振る舞いを規定する

### Modified Capabilities
- `maui-bridge`: `add-maui-bridge` で「Cell 追加 API は LabelCell のみ」と規定されていた `Requirement: Bridge Builder API` を、13 Cell 用 `addXxxCell(...)` メソッドを含む完全形に拡張する。同じく「実体実装は LabelCell 経路のみ」と規定されていた `Requirement: ユーザー操作 delegate / listener` を、14 Cell 種別分の通知メソッド実体実装と EntryCell 連携を含む完全形に更新する

## Impact

- 影響範囲：
  - `maui/KsSettingsView.Maui/` への 13 Cell + 13 Handler 追加
  - `maui/KsSettingsView.Maui/MauiAppBuilderExtension.cs` への Handler 登録追加（既存メソッドの拡張）
  - `maui/KsSettingsView.Maui/SettingsView.cs` および `Section.cs` への `ItemsSource` / `ItemTemplate` BindableProperty 追加
  - `maui/KsSettingsView.Maui/Handlers/SettingsViewHandler.cs` への ItemsSource 購読ロジック追加
  - `ios/Sources/KsSettingsViewBridge/`、`android/ks-settingsview-bridge/` への各 Cell 用 `addXxxCell` 追加（Bridge API 拡張、`add-maui-bridge` プロジェクトに新規メソッド追加する形）
  - `maui/KsSettingsView.Bindings.iOS/`、`Android/` の `ApiDefinitions.cs` 再生成
  - `samples/maui/` への各 Cell ページ追加（`add-samples-maui` で整備された Sample 土台に対する拡張）
  - 新規 `maui/KsSettingsView.Maui.SnapshotTests/`
  - `docs/migration-from-aiforms.md` 新規
- 依存：
  - `add-monorepo-foundation` / `add-settings-view-*`（archive 済）
  - `add-partial-update-core` / `add-partial-update-native`（先行・archive 必須）: `SettingsRootDiff` / `SettingsRootStore` / Native UI 層の部分更新 API
  - `add-cell-types-basic` / `add-cell-types-input` / `add-cell-types-custom`（Native Cell が揃っている必要）
  - `add-maui-bridge`（先行・archive 必須）: Bridge プロジェクト（`Bridge.ApplyDiff` / `KsSettingsRootDiffDTO` 含む）
  - `add-maui-core`（先行・archive 必須）: MAUI 基盤 + LabelCell + cellId Map + `ApplyDiff` / `BuildAndSetRoot` 経路
  - `add-samples-maui`（先行・archive 必須）: Sample 土台
- 後続：
  - Phase 2 以降の配信整備、KMP、モダン UI
- リスク：高
  - 13 Cell の Handler 実装数が多く、`CellBaseHandler` パターンが正しく適用されないと一貫性が失われる
  - 双方向バインドの実装（`BindingMode.TwoWay`）は cellId Map 経由の経路（`add-maui-core` で確立）が複雑、テスト網羅が重要
  - `ItemsSource` / `ItemTemplate` は `add-maui-core` の `ApplyDiff` / `BuildAndSetRoot` 経路に統合する設計だが、`DataTemplate` インスタンス化や `BindingContext` 連動が MAUI 9 内部実装に依存
  - CustomCell の C# View → Native View 変換は MAUI 9 `MauiView.ToPlatform()` 経路が iOS / Android で挙動差がある
  - **緩和策**: 本提案で全 Cell の Handler 実装と Snapshot テスト・移行ガイドを揃えることでスコープを明確化、`add-maui-core` で確立した経路に乗せることで設計判断は最小化
