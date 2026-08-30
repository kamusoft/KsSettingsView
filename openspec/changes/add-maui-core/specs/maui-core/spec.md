## ADDED Requirements

### Requirement: KsSettingsView.Maui プロジェクトの存在

`maui/KsSettingsView.Maui/KsSettingsView.Maui.csproj` が存在し、MAUI Library 形式（`<UseMaui>true</UseMaui>`）で `<TargetFrameworks>net9.0-ios;net9.0-android</TargetFrameworks>` を指定しなければならない (SHALL)。本プロジェクトは `add-maui-bridge` で整備された `KsSettingsView.Bindings.iOS` および `KsSettingsView.Bindings.Android` を `<ProjectReference>` で参照しなければならない (MUST)。

#### Scenario: csproj の存在

- **GIVEN** リポジトリのクローン直後
- **WHEN** `maui/KsSettingsView.Maui/KsSettingsView.Maui.csproj` を確認する
- **THEN** ファイルが存在し、`<UseMaui>true</UseMaui>` と `<TargetFrameworks>net9.0-ios;net9.0-android</TargetFrameworks>` を含む

#### Scenario: バインディングプロジェクトへの参照

- **GIVEN** `KsSettingsView.Maui.csproj`
- **WHEN** `<ProjectReference>` 要素を確認する
- **THEN** `KsSettingsView.Bindings.iOS.csproj` と `KsSettingsView.Bindings.Android.csproj` の両方が TFM 別に参照されている

#### Scenario: ビルド成功

- **GIVEN** `maui/KsSettingsView.Maui/`
- **WHEN** `dotnet build -f net9.0-ios` および `dotnet build -f net9.0-android` を実行する
- **THEN** どちらも警告なしで成功する

#### Scenario: slnx への登録

- **GIVEN** `maui/KsSettingsView.slnx`
- **WHEN** プロジェクト参照一覧を確認する
- **THEN** `KsSettingsView.Maui` プロジェクトが含まれている

### Requirement: BindableObject 基盤クラス

`KsSettingsView.Maui` には以下の `BindableObject` 派生クラスが存在しなければならない (SHALL)：

- `CellBase`：全 Cell の基底。共通 BindableProperty として `Title`、`Description`、`Icon`、`HintText`、`IsEnabled`、`BackgroundColor`、`CellHeight`、および各 Cell を識別する `CellId` を持つ
- `Section`：`HeaderProperty`（`SectionAccessory?` または `string?`）、`FooterProperty`（同上）、`CellsProperty`（`IList<CellBase>`、デフォルト値として `ObservableCollection<CellBase>` のインスタンスを生成）を持つ
- `SettingsView`：`View` 派生、`Sections: IList<Section>`（デフォルト値として `ObservableCollection<Section>` のインスタンスを生成）、`Style`、`HeaderView: View?`、`FooterView: View?` プロパティを公開

利用者が `Sections` / `Cells` に独自の `IList<T>` を代入することは許容しなければならない (MUST)。`SettingsViewHandler` は受け取ったコレクションが `INotifyCollectionChanged` を実装するかどうかで動作を切り替える：

- 実装する場合（例: `ObservableCollection<T>`）：`CollectionChanged` イベントを購読して部分更新（`Bridge.ApplyDiff`）を行う
- 実装しない場合（例: `List<T>`）：購読は行わず、初回 `Bridge.SetRoot` による静的描画のみとなる（後続の追加・削除は反映されない、利用者は `Sections = newList` のように再代入する必要がある）

`SettingsView` の `HeaderView` / `FooterView` BindableProperty は旧 AiForms.Maui.SettingsView の互換 API として公開する (MUST、`add-partial-update-core` で `SettingsRoot.header/footer` が削除されたため、ドメインモデルではなく `View` 派生プロパティとして提供)。

`SettingsRootDefinition` 等の「XAML ルート用 BindableObject」型は本提案では**導入しない** (MUST NOT)。`SettingsView` 自身がルート View であり、`Sections` / `HeaderView` / `FooterView` 等のプロパティを直接保持する設計とする（旧 AiForms / `add-maui-bindings` Decision 5 で計画されていた `SettingsRootDefinition` 命名は撤回）。

#### Scenario: CellBase の存在

- **GIVEN** `KsSettingsView.Maui` アセンブリ
- **WHEN** `CellBase` クラス定義を確認する
- **THEN** `BindableObject` 派生で、上記 8 プロパティ全てが `BindableProperty.Create(...)` で宣言されている

#### Scenario: Section の存在

- **GIVEN** `KsSettingsView.Maui` アセンブリ
- **WHEN** `Section` クラス定義を確認する
- **THEN** `BindableObject` 派生で、`HeaderProperty`、`FooterProperty`、`CellsProperty`（`IList<CellBase>`、デフォルトは `ObservableCollection<CellBase>` の新規インスタンス）が `BindableProperty.Create(...)` で宣言されている

#### Scenario: SettingsView のプロパティ

- **GIVEN** `SettingsView` クラス
- **WHEN** 公開 BindableProperty を確認する
- **THEN** `SectionsProperty`（`IList<Section>`、デフォルトは `ObservableCollection<Section>` の新規インスタンス）、`StyleProperty`、`HeaderViewProperty`、`FooterViewProperty` が全て宣言されている

#### Scenario: ObservableCollection 以外の IList 受け入れ（List）

- **GIVEN** `SettingsView` インスタンス
- **WHEN** `view.Sections = new List<Section> { sectionA, sectionB }` を代入する
- **THEN** `Sections` プロパティは `List<Section>` を保持し、`SettingsViewHandler` は `INotifyCollectionChanged` 非実装と判定して `CollectionChanged` 購読を行わない（後続の `list.Add(...)` は UI に反映されない）

#### Scenario: ObservableCollection の自動購読

- **GIVEN** `view.Sections = new ObservableCollection<Section> { sectionA }` を代入済の `SettingsView`、Handler が `ConnectHandler` 済の状態
- **WHEN** `view.Sections.Add(sectionB)` を呼ぶ
- **THEN** `SettingsViewHandler` が `CollectionChanged` を購読しているため、`Bridge.ApplyDiff(InsertSection)` が発火する

#### Scenario: SettingsRootDefinition の非導入

- **GIVEN** `KsSettingsView.Maui` アセンブリ
- **WHEN** クラス一覧を確認する
- **THEN** `SettingsRootDefinition` 等の「XAML ルート用 BindableObject」型は存在しない。`SettingsView` がルート View 兼ドメイン構造の入力点となる

#### Scenario: HeaderView BindableProperty の存在

- **GIVEN** `SettingsView` クラス
- **WHEN** `HeaderViewProperty` を確認する
- **THEN** `BindableProperty.Create(nameof(HeaderView), typeof(View), typeof(SettingsView), defaultBindingMode: BindingMode.OneWay)` 相当で定義されており、XAML から `<HeaderView>...</HeaderView>` で View を渡せる

### Requirement: SettingsViewHandler の基盤

`KsSettingsView.Maui` には `SettingsViewHandler` クラスが存在し、以下を実装しなければならない (SHALL)：

- `partial class`（iOS / Android のパーシャル実装に分割）
- `KsCellInteractionDelegate` / `KsCellInteractionListener` の C# 実装
- 初回構築用 `BuildAndSetRoot()` ヘルパ：`SettingsView.Sections` を走査して Bridge `Builder` に Cell を積み、`Bridge.SetRoot(KsSettingsRootDTO)` を 1 回呼ぶ（`ConnectHandler` 時または `Sections` 全置換時）
- 部分更新変換 `ApplyDiff(SettingsRootDiff)` ヘルパ：`Bridge.ApplyDiff(KsSettingsRootDiffDTO)` を呼ぶ薄いラッパ
- `Dictionary<string, CellBase>` Map を `BuildAndSetRoot` 時に再構築 + `ApplyDiff` 時に差分更新
- `ConnectHandler` で `VirtualView.Sections` および各 `Section.Cells` が `INotifyCollectionChanged` を実装している場合のみ `CollectionChanged` を購読する。各 Cell の `PropertyChanged`、Section の `PropertyChanged`（Header/Footer 変化）は常に購読する
- `CollectionChanged` の `NotifyCollectionChangedAction` を `SettingsRootDiff` 系 DTO に変換し `Bridge.ApplyDiff(...)` を呼ぶ（旧 AiForms.Maui.NativeCollectionView `OnCellCollectionChanged` 流儀）
- `Sections` プロパティ自体が再代入された場合（`SectionsProperty` の `propertyChanged`）、旧コレクションの `CollectionChanged` 購読を解除し、新コレクションが `INotifyCollectionChanged` なら購読登録し、`BuildAndSetRoot()` で全体再構築する
- `Mapper` に `HeaderViewProperty` / `FooterViewProperty` の変更を `Bridge.SetRootHeader(view:)` / `Bridge.SetRootFooter(view:)` 呼び出しに接続
- `Mapper` に `StyleProperty` の変更を登録し、変更時に `Bridge.SetStyle(style)` を呼ぶ（`SetRoot` 経路とは別経路）(MUST)
- `DisconnectHandler` で Bridge 参照解放、購読解除、cellId Map クリア

#### Scenario: SettingsViewHandler の存在

- **GIVEN** `KsSettingsView.Maui` アセンブリ
- **WHEN** `SettingsViewHandler` クラス定義を確認する
- **THEN** `Microsoft.Maui.Handlers.ViewHandler<SettingsView, ...>` 派生で、`partial class` 宣言を持つ

#### Scenario: BuildAndSetRoot による初回 setRoot 呼び出し

- **GIVEN** `SettingsView` に 2 Section + 各 1 LabelCell を含む `Sections` を設定済
- **WHEN** Bridge モックを記録モードにして `BuildAndSetRoot()` を 1 回呼ぶ
- **THEN** モックの `SetRoot` 記録が 1 回のみ存在し、引数 `KsSettingsRootDTO` は 2 セクション・各 1 Cell を含む

#### Scenario: Sections.CollectionChanged.Add で applyDiff(InsertSection) を呼ぶ

- **GIVEN** Handler が `ConnectHandler` 済、Sections は 1 Section 1 LabelCell
- **WHEN** `Sections.Add(new Section { Cells = { new LabelCell { Title = "X" } } })` を実行する
- **THEN** Bridge モックの `ApplyDiff` 記録が 1 回増え、引数 DTO は `KsSettingsRootDiffInsertSectionDTO(index: 1, section: ...)` 相当である（`SetRoot` は呼ばれない）

#### Scenario: Section.Cells.CollectionChanged.Add で applyDiff(InsertCell) を呼ぶ

- **GIVEN** Handler が `ConnectHandler` 済、Section A に Cell が 1 つ存在
- **WHEN** `sectionA.Cells.Add(new LabelCell { Title = "Y" })` を実行する
- **THEN** Bridge モックの `ApplyDiff` 記録が 1 回増え、引数 DTO は `KsSettingsRootDiffInsertCellDTO(sectionId: sectionA.Id, index: 1, cell: ...)` 相当である

#### Scenario: Section.Cells.CollectionChanged.Remove で applyDiff(RemoveCell) を呼ぶ

- **GIVEN** Handler が `ConnectHandler` 済、Section A に Cell C が存在
- **WHEN** `sectionA.Cells.Remove(C)` を実行する
- **THEN** Bridge モックの `ApplyDiff` 記録が 1 回増え、引数 DTO は `KsSettingsRootDiffRemoveCellDTO(cellId: C.CellId)` 相当である

#### Scenario: CellBase.PropertyChanged で applyDiff(ReplaceCell) を呼ぶ

- **GIVEN** Handler が `ConnectHandler` 済、Cell C の `Title = "old"`
- **WHEN** `C.Title = "new"` に変更する
- **THEN** Bridge モックの `ApplyDiff` 記録が 1 回増え、引数 DTO は `KsSettingsRootDiffReplaceCellDTO(cellId: C.CellId, newCell: ...)` 相当である（`SetRoot` 経路は走らない）

#### Scenario: Section.Header 変更で applyDiff(UpdateAccessory) を呼ぶ

- **GIVEN** Handler が `ConnectHandler` 済、Section A に `Header = "old"`
- **WHEN** `sectionA.Header = "new"` に変更する
- **THEN** Bridge モックの `ApplyDiff` 記録が 1 回増え、引数 DTO は `KsSettingsRootDiffUpdateAccessoryDTO(target: SectionHeader(sectionId: A.Id), accessory: ...)` 相当である

#### Scenario: SettingsView.HeaderView 変更で Bridge.SetRootHeader を呼ぶ

- **GIVEN** `SettingsView` の `HeaderView = null`、Bridge モック記録モード
- **WHEN** `SettingsView.HeaderView = new Label { Text = "プロフィール" }` に変更する
- **THEN** Bridge モックの `SetRootHeader` 記録が 1 回増え、引数は対応する `KsAnyViewDTO` である（`SetRoot` / `ApplyDiff` 経路は呼ばれない）

#### Scenario: Sections.CollectionChanged.Reset で setRoot を呼ぶ

- **GIVEN** Handler が `ConnectHandler` 済
- **WHEN** `Sections.Clear()` 等で `NotifyCollectionChangedAction.Reset` が発火する
- **THEN** Bridge モックの `SetRoot` 記録が 1 回増える（フォールバックパス、`ApplyDiff` ではなく全体差し替え）

#### Scenario: ObservableCollection 以外の IList では購読しない

- **GIVEN** `VirtualView.Sections = new List<Section> { ... }`（`INotifyCollectionChanged` 非実装）を代入した状態の Handler
- **WHEN** `ConnectHandler` で初期化する
- **THEN** Handler は `Bridge.SetRoot` を 1 回呼んで初回構築を行うが、`Sections` には `CollectionChanged` 購読を登録しない。以後 `list.Add(...)` を呼んでも `Bridge.ApplyDiff` / `Bridge.SetRoot` のどちらも発火しない

#### Scenario: Sections プロパティの再代入

- **GIVEN** Handler が `VirtualView.Sections = oldObservable`（`ObservableCollection<Section>`）で `ConnectHandler` 済、`CollectionChanged` 購読中
- **WHEN** `VirtualView.Sections = newObservable`（別の `ObservableCollection<Section>`）に再代入する
- **THEN** Handler は旧 `oldObservable` の `CollectionChanged` 購読を解除し、新 `newObservable` に対して購読登録する。`Bridge.SetRoot` で全体再構築が 1 回呼ばれる

#### Scenario: DisconnectHandler での解放

- **GIVEN** Handler が `ConnectHandler` 済、Bridge 参照を保持、`Sections.CollectionChanged` を購読中
- **WHEN** `DisconnectHandler` を呼ぶ
- **THEN** Bridge 参照が `null` になり、`CollectionChanged` 購読が全て解除され、cellId Map が空になる

#### Scenario: StyleProperty 変更時の Bridge.SetStyle 呼び出し

- **GIVEN** `SettingsView` が表示中、`Style = Classic`、Bridge モック記録モード
- **WHEN** `SettingsView.Style = Modern` に変更する
- **THEN** Bridge モックの `SetStyle(Modern)` 記録が 1 回増え、`SetRoot` / `ApplyDiff` 経路は呼ばれない（Style 切替は専用 API 経路）

### Requirement: cellId ベースの双方向対応マップ

`SettingsViewHandler` は `Dictionary<string, CellBase>` を保持し、`BuildAndSetRoot` 時に全体再構築、`ApplyDiff` 時に差分更新（追加・削除・置換に応じて追加・削除・更新）しなければならない (SHALL)。`KsCellInteractionDelegate` / `KsCellInteractionListener` のコールバックを受けた際は cellId から CellBase を引き、対象 Cell の `SetValue(...Property, value)` で C# 側 BindableProperty に書き戻さなければならない (MUST)。

#### Scenario: Map 全体再構築

- **GIVEN** `SettingsView` に Cell A（id="a"）と Cell B（id="b"）が登録された状態
- **WHEN** `BuildAndSetRoot()` を呼ぶ
- **THEN** Handler 内部 Map が `{"a": CellA, "b": CellB}` となる

#### Scenario: Map 差分更新（InsertCell 時）

- **GIVEN** Handler 内部 Map が `{"a": CellA}`
- **WHEN** `Section.Cells.Add(CellB)` で `ApplyDiff(InsertCell)` 経由で更新が発生する
- **THEN** Handler 内部 Map が `{"a": CellA, "b": CellB}` に更新される（既存エントリは保持）

#### Scenario: Map 差分更新（RemoveCell 時）

- **GIVEN** Handler 内部 Map が `{"a": CellA, "b": CellB}`
- **WHEN** `Section.Cells.Remove(CellB)` で `ApplyDiff(RemoveCell)` 経由で更新が発生する
- **THEN** Handler 内部 Map が `{"a": CellA}` に更新される

#### Scenario: Bridge コールバックでの書き戻し（擬似テスト用 BoolProperty）

- **GIVEN** Cell A（id="a"、テスト用 `TestBoolCell` で `IsOnProperty` を持つ） が Map 登録済
- **WHEN** Handler の `KsCellInteractionDelegate.didChangeBoolValue(cellId: "a", value: true)` メソッドを擬似的に呼ぶ
- **THEN** Cell A の `IsOnProperty` が `true` に SetValue される

### Requirement: CellBaseHandler の基盤

`CellBaseHandler<TVirtualCell, TNativeCell>` を `ElementHandler<TVirtualCell, TNativeCell>` 派生で実装しなければならない (SHALL)。`BasePropertyMapper` は親 `SettingsViewHandler` に Cell プロパティ変更を通知し、`SettingsRootDiff.replaceCell(cellID:, new:)` 相当の DTO を生成して `Bridge.ApplyDiff(...)` を呼ぶ薄いラッパでなければならない (MUST)。

#### Scenario: CellBaseHandler の存在

- **GIVEN** `KsSettingsView.Maui` アセンブリ
- **WHEN** `CellBaseHandler<,>` クラス定義を確認する
- **THEN** `Microsoft.Maui.Handlers.ElementHandler<TVirtualCell, TNativeCell>` 派生で、`BasePropertyMapper` が定義されている

#### Scenario: PropertyMapper が ApplyDiff(ReplaceCell) をトリガ

- **GIVEN** LabelCell が登録された SettingsView と Handler、Bridge モック記録モード
- **WHEN** LabelCell の `Title` プロパティを `"A"` → `"B"` に変更する
- **THEN** Bridge モックの `ApplyDiff` 記録が 1 回増え、引数 DTO は `KsSettingsRootDiffReplaceCellDTO(cellId: ..., newCell: ...)` 相当である（`SetRoot` は呼ばれない）

### Requirement: LabelCell の最小実装

`Cells/LabelCell.cs` および `Handlers/LabelCellHandler.cs` / `.iOS.cs` / `.Android.cs` を実装しなければならない (SHALL)。`LabelCellHandler` は `CellBaseHandler<LabelCell, NativeLabelCell>` 派生でなければならない (MUST)。本 Cell は Bridge ↔ Handler 経路の動作証明用最小 Cell として位置付けられ、`Title` / `Description` / `Icon` / `HintText` の表示を保証する。

#### Scenario: LabelCell の存在

- **GIVEN** `KsSettingsView.Maui` アセンブリ
- **WHEN** `LabelCell` クラス定義を確認する
- **THEN** `CellBase` 派生で、`ValueTextProperty`（LabelCell 固有の表示プロパティ）が `BindableProperty` で宣言されている

#### Scenario: LabelCellHandler の存在

- **GIVEN** `KsSettingsView.Maui` アセンブリ
- **WHEN** `LabelCellHandler` クラス定義を確認する
- **THEN** `CellBaseHandler<LabelCell, ...>` 派生で、iOS / Android パーシャル実装に分割されている

#### Scenario: LabelCell の Bridge への反映

- **GIVEN** `SettingsView` に LabelCell 1 個（`Title = "Hello"`）が登録された状態、Bridge モック記録モード
- **WHEN** `BuildAndSetRoot()` を呼ぶ
- **THEN** Bridge モックの記録に `Builder.addLabelCell(id:..., title:"Hello", ...)` 相当の呼び出しが含まれる

### Requirement: MauiAppBuilder 拡張

`KsSettingsView.Maui.MauiAppBuilderExtension` に `public static MauiAppBuilder AddKsSettingsView(this MauiAppBuilder builder)` を実装しなければならない (SHALL)。本メソッドは `SettingsViewHandler` および `LabelCellHandler` を `Handlers` プロパティに登録しなければならない (MUST)。

#### Scenario: AddKsSettingsView の存在

- **GIVEN** `KsSettingsView.Maui` アセンブリ
- **WHEN** `MauiAppBuilderExtension.AddKsSettingsView` メソッドを確認する
- **THEN** `MauiAppBuilder` 拡張メソッドとして公開されている

#### Scenario: Handler 登録

- **GIVEN** `MauiAppBuilder` インスタンス
- **WHEN** `builder.AddKsSettingsView()` を呼ぶ
- **THEN** `ConfigureMauiHandlers` 経由で `SettingsViewHandler` と `LabelCellHandler` の 2 個が登録される

### Requirement: メモリリーク対策基盤

`KsSettingsView.Maui` は以下のメモリリーク対策基盤を実装しなければならない (SHALL)：

- `SettingsViewHandler.DisconnectHandler` で Bridge 参照解放、`CollectionChanged` 購読解除、cellId Map クリア
- `CellBaseHandler.DisconnectHandler` で Native Cell 参照解放
- `HandlerCleanUpHelper.cs`：Page.NavigatedFrom フックで明示的 Disconnect（旧 AiForms 由来パターン）
- `MauiSettingsViewLeakTests`：10 回 push/pop で `WeakReference` がゼロになることを検証

#### Scenario: HandlerCleanUpHelper の存在

- **GIVEN** `KsSettingsView.Maui` アセンブリ
- **WHEN** `HandlerCleanUpHelper` クラスを確認する
- **THEN** Page ライフサイクルにフックして `SettingsViewHandler.DisconnectHandler` を呼ぶ実装が存在する

#### Scenario: WeakReference リーク検出テスト

- **GIVEN** `MauiSettingsViewLeakTests`
- **WHEN** `dotnet test --filter MauiSettingsViewLeakTests` を実行する
- **THEN** SettingsView を含む Page の 10 回 push/pop 後、`WeakReference.IsAlive` が全て `false`（GC 後）となり、テスト全成功

### Requirement: ユニットテスト

`maui/KsSettingsView.Maui.Tests/` プロジェクトに以下のテストが存在しなければならない (SHALL)：

- `SettingsViewHandlerCollectionSyncTests.cs`：以下を検証
  - 初回 `ConnectHandler` で `Bridge.SetRoot` が 1 回呼ばれること
  - `Sections.Add` → `Bridge.ApplyDiff(InsertSection)` が 1 回呼ばれ、`SetRoot` は呼ばれないこと
  - `Sections.Remove` → `Bridge.ApplyDiff(RemoveSection)` が 1 回呼ばれること
  - `Section.Cells.Add` → `Bridge.ApplyDiff(InsertCell)` が 1 回呼ばれること
  - `Section.Cells.Remove` → `Bridge.ApplyDiff(RemoveCell)` が 1 回呼ばれること
  - Cell `PropertyChanged` → `Bridge.ApplyDiff(ReplaceCell)` が 1 回呼ばれること
  - Section `Header` プロパティ変更 → `Bridge.ApplyDiff(UpdateAccessory(SectionHeader))` が 1 回呼ばれること
  - `Sections.Clear()`（Reset） → `Bridge.SetRoot` が 1 回呼ばれること（フォールバック）
  - `VirtualView.Sections = new List<Section>(...)`（`INotifyCollectionChanged` 非実装）の場合：初回 `ConnectHandler` で `Bridge.SetRoot` が 1 回呼ばれること、`list.Add(...)` 後も `Bridge.ApplyDiff` / `Bridge.SetRoot` のいずれも発火しないこと
  - `VirtualView.Sections = newObservable` の再代入時：旧コレクションの購読が解除され、新コレクションが購読され、`Bridge.SetRoot` で全体再構築が 1 回呼ばれること
- `SettingsViewHandlerCellMapTests.cs`：
  - 初回 `BuildAndSetRoot` 後に cellId → CellBase Map が全体構築されること
  - `ApplyDiff(InsertCell)` 後に Map にエントリが追加されること
  - `ApplyDiff(RemoveCell)` 後に Map からエントリが削除されること
  - Bridge delegate コールバックで対応する Cell の `SetValue` が呼ばれること（擬似テスト用 BoolProperty を持つ `TestBoolCell` で検証）
- `SettingsViewHandlerHeaderFooterTests.cs`：`SettingsView.HeaderView` 設定時に `Bridge.SetRootHeader(view:)` が 1 回呼ばれること（`SetRoot` / `ApplyDiff` 経路は呼ばれない）、`null` リセットで `Bridge.SetRootHeader(nil)` が呼ばれること。`FooterView` も同等に検証
- `SettingsViewHandlerStyleTests.cs`：`SettingsView.Style` 変更時に `Bridge.SetStyle(style)` が 1 回呼ばれ、`SetRoot` / `ApplyDiff` 経路が呼ばれないこと
- `LabelCellRenderingTests.cs`：LabelCell が Bridge 経由で正しくパラメータ渡しされること

#### Scenario: LabelCellRendering テストの存在と成功

- **GIVEN** `maui/KsSettingsView.Maui.Tests/LabelCellRenderingTests.cs`
- **WHEN** テスト内容を確認し `dotnet test --filter LabelCellRenderingTests` を実行する
- **THEN** LabelCell が Bridge 経由で `Title` / `Description` / `Icon` / `HintText` / `ValueText` の各値を正しくパラメータ渡しすることを検証するテストケースが存在し、全成功する

#### Scenario: 全テスト成功

- **GIVEN** `maui/KsSettingsView.Maui.Tests/`
- **WHEN** `dotnet test` を実行する
- **THEN** 全テストが成功する
