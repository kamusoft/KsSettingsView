## Why

`add-maui-bridge` で Native Bridge と MAUI バインディングプロジェクトが整備された後、`.NET MAUI` ユーザーが XAML から SettingsView を使えるようにするための「MAUI 本体ライブラリ `KsSettingsView.Maui`」を整備する。本提案は元 `add-maui-bindings` 1 提案を 3 分割した第 2 段で、MAUI Handler 階層・`BindableObject` Cell の基盤・**ObservableCollection 連動 + `Bridge.ApplyDiff` による部分更新方式** の骨組み・メモリリーク対策の基盤までを担当する。

各 Cell 種別の Handler 実装（14 種類）は後続の `add-maui-cells` に委ね、本提案では「Bridge ↔ Handler 経路の動作証明」のため `LabelCell` 1 種類のみを実装する。これにより、(1) Bridge ↔ Handler 経路を最小コストで検証、(2) Handler 基盤に関する設計判断（部分更新方式、cellId Map、`DisconnectHandler` 必須化）を本提案で確定、(3) `add-maui-cells` 着手時に「14 Cell をパターン適用するだけ」の状態にする、という構造が達成できる。

部分更新方式は `AiForms.Maui.NativeCollectionView` の `OnCellCollectionChanged` パターンを継承する。`SettingsView.Sections` と `Section.Cells` は `IList<T>` 型で公開し、利用者が `ObservableCollection<T>` を渡した場合のみ `CollectionChanged` イベントを購読して `NotifyCollectionChangedAction` に応じた `KsSettingsRootDiffDTO`（`add-maui-bridge` で導入）を Bridge に渡す。`IList<T>` でも `ObservableCollection<T>` 以外（例: `List<T>`）が渡された場合は静的描画とし、初回のみ `Bridge.SetRoot` で全体構築する。Root H/F は `SettingsView.HeaderView` / `FooterView` BindableProperty（旧 AiForms.Maui.SettingsView 互換）として公開し、変更時は `Bridge.SetRootHeader(view:)` / `Bridge.SetRootFooter(view:)` を呼ぶ。

## What Changes

- `maui/KsSettingsView.Maui/` プロジェクト新設：
  - `KsSettingsView.Maui.csproj`（MAUI Library 形式、TargetFrameworks `net9.0-ios; net9.0-android`、`KsSettingsView.Bindings.iOS/Android` 参照）
- MAUI 側 `BindableObject` 基盤クラス：
  - `Cells/CellBase.cs`（`BindableObject` 派生、Title/Description/Icon/HintText/IsEnabled/BackgroundColor/CellHeight 等の共通 BindableProperty）
  - `Section.cs`（`BindableObject`、HeaderProperty/FooterProperty/CellsProperty: `IList<CellBase>`、デフォルト値は `ObservableCollection<CellBase>` のインスタンス）
  - `SettingsView.cs`（`View` 派生、`Sections: IList<Section>`（デフォルト値は `ObservableCollection<Section>` のインスタンス）、`Style`、`HeaderView: View?`、`FooterView: View?` プロパティを公開、XAML ルートとして直接利用）
  - 注: `SettingsRootDefinition` 等の「XAML ルート用 BindableObject」型は本提案では導入しない（`add-partial-update-core` で `SettingsRoot.header/footer` が削除されたため、`SettingsView` 自身でルートを保持する設計に統一）
- Handler 階層基盤：
  - `Handlers/SettingsViewHandler.cs`（`partial class`、初回 `BuildAndSetRoot()` ヘルパ・部分更新 `ApplyDiff()` ヘルパ、cellId → CellBase Map 管理、`KsCellInteractionDelegate/Listener` 実装）
  - `Handlers/SettingsViewHandler.iOS.cs`（`CreatePlatformView` で `KsSettingsViewBridge.makeController` を呼ぶ、`ConnectHandler` で購読、`DisconnectHandler` で解放）
  - `Handlers/SettingsViewHandler.Android.cs`（同上、`makeView` を経由）
  - `Handlers/CellBaseHandler.cs`（`partial class CellBaseHandler<TVirtualCell, TNativeCell> : ElementHandler<TVirtualCell, TNativeCell>`、`BasePropertyMapper` は親 `SettingsViewHandler` に Cell 変更を通知し `Bridge.ApplyDiff(ReplaceCell)` を発火する）
- LabelCell の最小実装（Bridge ↔ Handler 経路の動作証明）：
  - `Cells/LabelCell.cs`
  - `Handlers/LabelCellHandler.cs` / `.iOS.cs` / `.Android.cs`
- MauiAppBuilder 拡張：
  - `MauiAppBuilderExtension.cs` に `public static MauiAppBuilder AddKsSettingsView(this MauiAppBuilder builder)` を実装
  - 本提案では `SettingsViewHandler` と `LabelCellHandler` のみ登録（残り 13 Cell Handler の登録は `add-maui-cells` で本メソッドに追加していく）
- メモリリーク対策基盤：
  - `DisconnectHandler` で Bridge 参照を解放するパターンを `SettingsViewHandler` / `CellBaseHandler` に実装
  - `HandlerCleanUpHelper.cs`（旧 AiForms 由来、Page.NavigatedFrom フックで明示的 Disconnect）
  - `MauiSettingsViewLeakTests` で 10 回 push/pop の `WeakReference` 検証テスト
- ユニットテスト：
  - `SettingsViewHandlerCollectionSyncTests.cs`：`Sections.Add` → `Bridge.ApplyDiff(InsertSection)`、`Sections.Remove` → `ApplyDiff(RemoveSection)`、`Section.Cells.Add` → `ApplyDiff(InsertCell)`、Cell `PropertyChanged` → `ApplyDiff(ReplaceCell)`、Section `HeaderProperty` 変更 → `ApplyDiff(UpdateAccessory)`、`Reset` → `Bridge.SetRoot` がそれぞれ呼ばれることを検証
  - `SettingsViewHandlerCellMapTests.cs`：`BuildAndSetRoot` 後の Map 全体構築、`ApplyDiff(InsertCell)` 後の Map 差分追加、`ApplyDiff(RemoveCell)` 後の Map 差分削除を検証
  - `SettingsViewHandlerHeaderFooterTests.cs`：`SettingsView.HeaderView` 変更 → `Bridge.SetRootHeader(view:)`、`null` 代入 → `Bridge.SetRootHeader(nil)` の反映検証

## Capabilities

### New Capabilities
- `maui-core`: `KsSettingsView.Maui` の `SettingsView` / `Section` / `CellBase` の基盤 BindableObject 階層、`SettingsViewHandler` / `CellBaseHandler` の Handler 基盤、`BuildAndSetRoot`（初回・Reset 用）+ `ApplyDiff`（部分更新用）の二段構え経路、`AddKsSettingsView()` 拡張、メモリリーク対策基盤、および最小 1 種類の Cell（`LabelCell`）の振る舞いを規定する

### Modified Capabilities
（なし。本提案は純粋な追加）

> **`purify-core-extract-style-to-ui-layer` 整合 note**: 本提案で扱う `SettingsView` / `Section` / `CellBase` / `SettingsViewHandler` には Theme 経路が含まれない（LabelCell は Color プロパティを持たないため）。Theme は Native UI 層に独立保持され（`KsSettingsViewUI.Theme` / `ks-settingsview-ui` の `Theme`、UIColor / UIFont ／ Compose Color / TextStyle を直接保持）、Bridge 経由で `controller.setTheme(_:)` / `view.setTheme(_:)` 独立 API として扱う。本提案では Theme を扱わないため、`SettingsView.Theme` 等の BindableProperty も導入しない。後続 `add-maui-cells` で Theme BindableProperty が必要になった場合は、Handler PropertyMapper から `Bridge.SetTheme(themeDTO)` を呼ぶ経路として追加する（`add-maui-cells` spec で扱う）。

## Impact

- 影響範囲：
  - 新規 `maui/KsSettingsView.Maui/`（C# プロジェクト全体）
  - 新規 `maui/KsSettingsView.Maui.Tests/`（ユニットテスト）
  - `maui/KsSettingsView.slnx` への追加
- 依存：
  - `add-maui-bridge`（先行・archive 必須）: Bridge API + Binding csproj（本提案で前提とする `Bridge.ApplyDiff` / `Bridge.SetRootHeader` API を含む）
  - `add-monorepo-foundation` / `add-settings-view-*`（archive 済）
  - `add-partial-update-core` / `add-partial-update-native`（先行・archive 必須）: `SettingsRootDiff` 型、`SettingsRootStore` および Native UI 層の `applyDiff` API、Root H/F の UI 層プロパティ化
- 後続：
  - `add-samples-maui`: 本提案 archive 後に MAUI Sample アプリ土台を整備、`LabelCell` の最小デモを表示
  - `add-maui-cells`: 本提案で確立した Handler 階層パターンを 13 Cell に適用、`AddKsSettingsView()` への登録追加、各 Cell の Sample ページ追加
- リスク：中
  - **MAUI 9 Handler のメモリリーク既知問題**：`DisconnectHandler` 必須化と `HandlerCleanUpHelper` パターン、`WeakReference` 検証テストで緩和
  - **Bridge API 設計の見落としが本提案実装中に発覚する可能性**：発覚した場合は `add-maui-bridge` を archive 取り消し → 修正 → 再 archive、または本提案で Bridge 側に追加パッチを当てる運用とする（design.md Open Questions に記載）
