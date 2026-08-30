## 依存関係

- 前提:
  - `add-monorepo-foundation`（archive 済）
  - `add-settings-view-core`（archive 済）
  - `add-settings-view-ios-ui`（archive 済）
  - `add-settings-view-android-ui`（archive 済）
  - `add-maui-bridge`（先行・archive 必須）: Bridge API + Binding csproj（`Bridge.ApplyDiff` / `Bridge.SetRootHeader` / `KsSettingsRootDiffDTO` 等を含む）
  - `add-partial-update-core`（先行・archive 必須）: `SettingsRootDiff` / `AccessoryTarget` / `SettingsAccessory` 型
  - `add-partial-update-native`（先行・archive 必須）: Native UI 層 `SettingsRootStore` および `applyDiff` API、Root H/F の UI 層プロパティ化
- 後続:
  - `add-samples-maui`: 本提案 archive 後に MAUI Sample アプリ土台を整備
  - `add-maui-cells`: 残り 13 Cell Handler + Sample 拡張 + ItemsSource / Snapshot / 移行ガイド

## 1. KsSettingsView.Maui プロジェクト作成

- [ ] 1.1 `maui/KsSettingsView.Maui/` ディレクトリを作成
- [ ] 1.2 `KsSettingsView.Maui.csproj` を MAUI Library 形式で作成（`<UseMaui>true</UseMaui>`、`<TargetFrameworks>net9.0-ios;net9.0-android</TargetFrameworks>`）
- [ ] 1.3 `<ProjectReference Include="../KsSettingsView.Bindings.iOS/...">` および `Android` 版を TFM 別に追加
- [ ] 1.4 `maui/KsSettingsView.slnx` に `KsSettingsView.Maui.csproj` を登録

## 2. BindableObject 基盤

- [ ] 2.1 `Cells/CellBase.cs` を `BindableObject` 派生で実装：
  - `TitleProperty`、`DescriptionProperty`、`IconProperty`、`HintTextProperty`、`IsEnabledProperty`、`BackgroundColorProperty`、`CellHeightProperty`、`CellIdProperty`（Cell 識別子）の 8 BindableProperty
- [ ] 2.2 `Section.cs` を `BindableObject` 派生で実装：
  - `HeaderProperty`（`SectionAccessory?` または互換 `string?`）、`FooterProperty`（同上）、`CellsProperty`（`IList<CellBase>`、`BindableProperty.Create` の `defaultValueCreator` で `ObservableCollection<CellBase>` の新規インスタンスを返す）
  - `Section.Id` プロパティ（`string`、内部で `Guid.NewGuid().ToString()` で自動採番）を `BindableProperty` 非対象として実装
- [ ] 2.3 `SettingsRootDefinition.cs` は**実装しない**（`add-partial-update-core` で `SettingsRoot.header/footer` 削除に伴い不要となる。`SettingsView : View` がルート View 兼ドメイン構造の入力点となる）
- [ ] 2.4 `SettingsView.cs` を `View` 派生で実装：
  - `SectionsProperty`（`IList<Section>`、`defaultValueCreator` で `ObservableCollection<Section>` の新規インスタンスを返す）
  - `StyleProperty`（Classic / Modern enum）
  - `HeaderViewProperty`（`Microsoft.Maui.Controls.View?`）
  - `FooterViewProperty`（`Microsoft.Maui.Controls.View?`）
- [ ] 2.5 `SettingsView.DefineProperites.cs` に上記 BindableProperty 仕様を集約（旧 AiForms 互換命名）
- [ ] 2.6 XAML content property 構文（`<settings:SettingsView.HeaderView><Label .../></...>`）が記述可能になるよう `[ContentProperty]` または BindableProperty メタデータで属性指定

## 3. SettingsViewHandler 基盤

- [ ] 3.1 `Handlers/SettingsViewHandler.cs`（shared partial）を `partial class SettingsViewHandler : ViewHandler<SettingsView, ...>` で実装し、`static PropertyMapper<SettingsView, SettingsViewHandler> Mapper` を定義する。`SectionsProperty` / `HeaderViewProperty` / `FooterViewProperty` / `StyleProperty` の `propertyChanged` 登録はこの shared partial の `Mapper` 静的初期化子で一元管理する（旧 AiForms と一致、iOS / Android パーシャルで重複登録しない）
- [ ] 3.2 `BuildAndSetRoot()` 初回構築ヘルパを実装：
  - `VirtualView.Sections` を走査、各 `Section.Cells` を走査
  - Bridge `Builder` に `beginSection` / `addLabelCell`（本提案実装範囲は LabelCell のみ）/ `endSection` を呼ぶ
  - `Builder.build()` で `KsSettingsRootDTO` を生成
  - `Bridge.SetRoot(root)` を呼ぶ
  - `VirtualView.HeaderView` / `FooterView` を `MauiView.ToPlatform(MauiContext)` でネイティブ View に変換、Bridge `SetRootHeader(view:)` / `SetRootFooter(view:)` で渡す
  - `Dictionary<string, CellBase>` Map を全体再構築（cellId → CellBase）
- [ ] 3.3 `ApplyDiff(SettingsRootDiff)` 部分更新ヘルパを実装：
  - 渡された `SettingsRootDiff` を `KsSettingsRootDiffDTO` に変換し `Bridge.ApplyDiff(dto)` を呼ぶ
  - 同時に内部 cellId Map を差分更新（InsertCell → 追加、RemoveCell → 削除、ReplaceCell → 更新）
- [ ] 3.4 `OnSectionsCollectionChanged(NotifyCollectionChangedEventArgs)` を実装：
  - `Add` → `ApplyDiff(InsertSection)` 系を呼ぶ（NewStartingIndex を index に流用）
  - `Remove` → `ApplyDiff(RemoveSection)` 系を呼ぶ（OldItems から sectionId を取得）
  - `Move` → `ApplyDiff(MoveSection)`
  - `Replace` → `ApplyDiff(ReplaceSection)`
  - `Reset` → `BuildAndSetRoot()` を呼ぶ（フォールバック）
- [ ] 3.5 `OnCellsCollectionChanged(NotifyCollectionChangedEventArgs, Section)` を実装（各 Section 用にクロージャ登録）：
  - `Add` → `ApplyDiff(InsertCell)`、`Remove` → `ApplyDiff(RemoveCell)`、`Move` → `ApplyDiff(MoveCell)`、`Replace` → `ApplyDiff(ReplaceCell)`、`Reset` → `BuildAndSetRoot()`
  - 旧 AiForms.Maui.NativeCollectionView の `_closureEventHandlerDictionary` パターンに倣い、Section が削除されるときはクロージャも解除する
- [ ] 3.6 `OnSectionPropertyChanged(Section, PropertyChangedEventArgs)` を実装：
  - `Header` / `Footer` プロパティ変更 → `ApplyDiff(UpdateAccessory(target: SectionHeader/Footer, accessory: ...))` を呼ぶ
- [ ] 3.7 `OnCellPropertyChanged(CellBase, PropertyChangedEventArgs)` を実装：
  - 任意のプロパティ変更 → `ApplyDiff(ReplaceCell(cellId: ..., newCell: ...))` を呼ぶ
- [ ] 3.7.1 `SubscribeCollections()` ヘルパを実装：
  - `VirtualView.Sections` を `INotifyCollectionChanged as INotifyCollectionChanged?` でチェックし、実装している場合のみ `CollectionChanged += OnSectionsCollectionChanged` を登録
  - 各 `Section.Cells` も同様に `INotifyCollectionChanged` チェック → 登録（旧 AiForms.Maui.NativeCollectionView の `_closureEventHandlerDictionary` パターンで Section ID をクロージャに閉じ込める）
  - `INotifyCollectionChanged` 非実装の場合は購読を行わず、初回 `BuildAndSetRoot()` のみで静的描画する（`Bridge.ApplyDiff` 経路は使われない）
- [ ] 3.7.2 `UnsubscribeCollections()` ヘルパを実装：購読中の `CollectionChanged` を全解除し、`_closureEventHandlerDictionary` をクリアする
- [ ] 3.7.3 `OnSectionsPropertyChanged()`（`SectionsProperty` 自体の再代入）を実装：旧コレクションを `UnsubscribeCollections()` で解除 → 新コレクションを `SubscribeCollections()` で購読登録 → `BuildAndSetRoot()` で全体再構築
- [ ] 3.8 `Handlers/SettingsViewHandler.iOS.cs` を実装：
  - `CreatePlatformView()` で `KsSettingsViewBridge.makeController(delegate: this)` を呼び PlatformView 取得
  - `ConnectHandler` で初回 `BuildAndSetRoot()` を呼び、`SubscribeCollections()` で `Sections` と各 `Section.Cells` の `CollectionChanged` を購読する（`INotifyCollectionChanged` 実装している場合のみ）。Section/Cell の `PropertyChanged` は常に購読する
  - iOS 固有のプラットフォーム連携（Bridge 呼び出し実装）のみを担当し、`Mapper` への `propertyChanged` 登録は行わない（3.1 の shared partial に集約済み）
- [ ] 3.9 `Handlers/SettingsViewHandler.Android.cs` を実装：
  - `CreatePlatformView()` で `KsSettingsViewBridge.makeView(context, listener: this)` を呼び PlatformView 取得
  - `ConnectHandler` / `SubscribeCollections` は iOS と同パターン
  - Android 固有のプラットフォーム連携のみを担当し、`Mapper` への登録は行わない（3.1 の shared partial に集約済み）
- [ ] 3.10 `KsCellInteractionDelegate` / `KsCellInteractionListener` の C# 実装：
  - インターフェース定義は 14 Cell 種別分のメソッドシグネチャを宣言（本提案では LabelCell 関連のみ実装、他 13 種類は `add-maui-cells` で実体実装）
  - 各メソッドで cellId Map を引いて対応 CellBase の `SetValue(...Property, value)` を呼ぶ集約処理

## 4. CellBaseHandler 基盤

- [ ] 4.1 `Handlers/CellBaseHandler.cs` を `partial class CellBaseHandler<TVirtualCell, TNativeCell> : ElementHandler<TVirtualCell, TNativeCell>` で実装
- [ ] 4.2 `BasePropertyMapper`（Title/Description/Icon/HintText 等の MapXxx）を実装：
  - 各 MapXxx は親 `SettingsViewHandler` に「Cell プロパティ変化」を通知し、`SettingsViewHandler.ApplyDiff(ReplaceCell(cellId, newCell))` 経由で `Bridge.ApplyDiff(...)` を発火させる薄いラッパ
  - 例外パス（高頻度更新）として `add-maui-cells` の EntryCell で `Bridge.UpdateCellValue` 直行を実装する際は、本ラッパを bypass する
- [ ] 4.3 `Handlers/CellBaseHandler.iOS.cs` / `.Android.cs` で `BasePropertyMapper` の実体実装

## 5. LabelCell の最小実装

- [ ] 5.1 `Cells/LabelCell.cs` を `CellBase` 派生で実装：
  - `ValueTextProperty`（`string`、LabelCell 固有の表示プロパティ）
- [ ] 5.2 `Handlers/LabelCellHandler.cs` を `CellBaseHandler<LabelCell, NativeLabelCell>` 派生で実装
- [ ] 5.3 `Handlers/LabelCellHandler.iOS.cs` を実装（iOS Native の LabelCell パラメータ受け渡し）
- [ ] 5.4 `Handlers/LabelCellHandler.Android.cs` を実装（同上）

## 6. MauiAppBuilder 拡張

- [ ] 6.1 `MauiAppBuilderExtension.cs` に `public static MauiAppBuilder AddKsSettingsView(this MauiAppBuilder builder)` を実装
- [ ] 6.2 `ConfigureMauiHandlers` 経由で `SettingsViewHandler` と `LabelCellHandler` の 2 個を登録（残り 13 Cell Handler の登録は `add-maui-cells` で本メソッドに追加していく旨をコードコメントに明記）

## 7. メモリリーク対策

- [ ] 7.1 `Handlers/SettingsViewHandler.cs` の `DisconnectHandler` で以下を実装：
  - Bridge 参照を `null` に解放
  - `Sections.CollectionChanged` / 各 `Section.Cells.CollectionChanged` / 各 Cell の `PropertyChanged` 購読を全解除
  - cellId Map をクリア
- [ ] 7.2 `Handlers/CellBaseHandler.cs` の `DisconnectHandler` で Native Cell 参照を解放
- [ ] 7.3 `HandlerCleanUpHelper.cs` を旧 AiForms から発想して再実装（Page.NavigatedFrom フックで明示的 Disconnect）

## 8. ユニットテストプロジェクト

- [ ] 8.1 `maui/KsSettingsView.Maui.Tests/` プロジェクトを作成（`xunit` + Bridge モック用ヘルパ）
- [ ] 8.2 `maui/KsSettingsView.slnx` に `KsSettingsView.Maui.Tests.csproj` を登録
- [ ] 8.3 Bridge モック用 `MockBridge` クラスを実装（`SetRoot` / `ApplyDiff` / `SetStyle` / `SetRootHeader` / `SetRootFooter` / `UpdateCellValue` の呼び出しを記録）

## 9. テスト実装

- [ ] 9.1 `SettingsViewHandlerCollectionSyncTests.cs`：
  - 初回 `ConnectHandler` で `MockBridge.SetRoot` が 1 回呼ばれる
  - `Sections.Add` で `MockBridge.ApplyDiff(InsertSection)` が 1 回呼ばれる
  - `Sections.Remove` で `MockBridge.ApplyDiff(RemoveSection)` が 1 回呼ばれる
  - `Section.Cells.Add` で `MockBridge.ApplyDiff(InsertCell)` が 1 回呼ばれる
  - `Section.Cells.Remove` で `MockBridge.ApplyDiff(RemoveCell)` が 1 回呼ばれる
  - Cell の `PropertyChanged` で `MockBridge.ApplyDiff(ReplaceCell)` が 1 回呼ばれる
  - `VirtualView.Sections = new List<Section>(...)` で `ConnectHandler` した場合：`MockBridge.SetRoot` は 1 回のみ呼ばれ、`list.Add(...)` 後も `ApplyDiff` / `SetRoot` のどちらも追加発火しない
  - `VirtualView.Sections = newObservableCollection` 再代入時：旧コレクション購読解除と新コレクション購読登録が行われ、`MockBridge.SetRoot` で全体再構築が 1 回呼ばれる
  - Section の `Header` プロパティ変更で `MockBridge.ApplyDiff(UpdateAccessory(SectionHeader))` が 1 回呼ばれる
  - `Sections.Clear()` で `MockBridge.SetRoot` が 1 回呼ばれる（Reset フォールバック）
- [ ] 9.2 `SettingsViewHandlerCellMapTests.cs`：
  - 初回 `BuildAndSetRoot` 後に cellId → CellBase Map が全体構築される
  - `ApplyDiff(InsertCell)` 後に Map にエントリが追加される
  - `ApplyDiff(RemoveCell)` 後に Map からエントリが削除される
  - 擬似テスト用 `TestBoolCell`（`IsOnProperty` 持ち）を使い、Bridge 経由で `didChangeBoolValue(cellId, true)` を擬似発火 → 対応 Cell の `IsOnProperty` が `true` になる
- [ ] 9.3 `SettingsViewHandlerHeaderFooterTests.cs`：
  - `SettingsView.HeaderView = label` で `MockBridge.SetRootHeader` が 1 回呼ばれる（`SetRoot` / `ApplyDiff` 経路は呼ばれない）
  - `HeaderView = null` で `MockBridge.SetRootHeader(nil)` が呼ばれる
  - `FooterView` も同等に検証
- [ ] 9.4 `LabelCellRenderingTests.cs`：LabelCell が Bridge 経由で `Title` / `Description` / `Icon` / `HintText` / `ValueText` の各値を正しくパラメータ渡しされること
- [ ] 9.5 `SettingsViewHandlerStyleTests.cs`：`SettingsView.Style` を Classic → Modern に変更した際、`MockBridge.SetStyle(Modern)` が 1 回呼ばれ、`SetRoot` / `ApplyDiff` 経路が呼ばれないことを検証

## 10. メモリリーク検出テスト

- [ ] 10.1 `MauiSettingsViewLeakTests.cs` を実装：
  - SettingsView を含む Page を 10 回 push/pop する
  - `WeakReference<SettingsView>` および `WeakReference<SettingsViewHandler>` が GC 後に `IsAlive == false` となることを検証
- [ ] 10.2 ローカル実行で `dotnet test --filter MauiSettingsViewLeakTests` が成功することを確認（CI 整備は別フェーズ）

## 11. 全テスト・ビルド確認

- [ ] 11.1 `dotnet build -f net9.0-ios` が警告なしで成功
- [ ] 11.2 `dotnet build -f net9.0-android` が警告なしで成功
- [ ] 11.3 `dotnet test` で全テスト成功（Collection / CellMap / HeaderFooter / LabelCell / Leak）
- [ ] 11.4 既存の Native iOS / Android テスト・`add-maui-bridge` の Bridge テストが影響を受けず全成功

## 完了条件

- 全タスクのチェックボックスが完了している
- `maui-core` capability の全 Scenario が通る
- `KsSettingsView.Maui` プロジェクトが iOS / Android の両 TFM でビルド成功
- 全ユニットテスト + リーク検出テストが成功
- LabelCell が Bridge ↔ Handler 経路で動作することを単体テストで証明済
- `AddKsSettingsView()` 拡張メソッドが `SettingsViewHandler` + `LabelCellHandler` を登録可能
