## 参考実装

本変更提案は元 `add-maui-bindings` をリネーム + 縮小したものであり、Bridge / Core 関連の設計判断は `add-maui-bridge` / `add-maui-core` に移譲済。実装着手前に以下を熟読すること。

- [`openspec/changes/add-maui-bridge/design.md`](../add-maui-bridge/design.md) — Bridge API（Builder + setRoot、delegate / listener）の設計判断
- [`openspec/changes/add-maui-core/design.md`](../add-maui-core/design.md) — MAUI 基盤の設計判断（BindableObject 階層、`ApplyDiff/BuildAndSetRoot`、cellId Map、メモリリーク対策）
- [`docs/legacy-aiforms-reference.md`](../../../docs/legacy-aiforms-reference.md) — 移植元の仕様要約
  - **必読セクション**: §2（CellBase 22 プロパティ）、§3（各 Cell の固有プロパティと BindingMode）、§5（Handler / PropertyMapper パターン）、§8（HandlerCleanUpHelper パターン）、§11（旧版との差分対応表）
- 原典コード（**必読**）：
  - [`../AiForms.Maui.SettingsView/SettingsView/Cells/`](file://../AiForms.Maui.SettingsView/SettingsView/Cells/) — 全 15 Cell の BindableProperty
  - [`../AiForms.Maui.SettingsView/SettingsView/Handlers/`](file://../AiForms.Maui.SettingsView/SettingsView/Handlers/) — Handler 階層（CellBaseHandler → LabelCellBaseHandler / EntryCellBaseHandler → 各 Cell Handler）、PropertyMapper パターン、`IsDisconnect` 安全ガード
  - [`../AiForms.Maui.SettingsView/SettingsView/MauiAppBuilderExtension.cs`](file://../AiForms.Maui.SettingsView/SettingsView/MauiAppBuilderExtension.cs) — 全 Handler 一括登録
  - [`../AiForms.Maui.SettingsView/SettingsView/Native/Android/ModelProxy.cs`](file://../AiForms.Maui.SettingsView/SettingsView/Native/Android/ModelProxy.cs) — 旧 AiForms の増分更新方式（参考。本提案では `add-maui-core` で確立した「全体再構築 + setRoot」方式を踏襲し、この方式は採用しない）

**重要事項**:

1. 旧 `AiForms.Maui.SettingsView` の `CellBaseHandler<TCell, TNativeCell>` 階層を `add-maui-core` で確立済。本提案は 13 Cell にこのパターンを**機械的にパターン適用**することが主目的
2. 旧版で確立された `IsDisconnect` ガード・`HandlerCleanUpHelper` パターンは `add-maui-core` で実装済。本提案では新規 13 Cell Handler 全てに同パターンを適用する
3. 旧版で `ValueText`、`On`、`Checked`、`Number`、`Time`、`Date`、`SelectedItem(s)`、`EntryCell.ValueText` が **TwoWay** であることを Bridge の delegate / listener で確実に成立させる（仕様要約 §3 参照）。経路は `add-maui-core` の cellId Map を利用
4. 名前空間は `AiForms.Settings` から `KsSettingsView.Maui` に変更（互換 shim なし、breaking change）

## Context

`add-maui-bridge`（Native Bridge + Binding csproj）と `add-maui-core`（MAUI 本体基盤 + `LabelCell` 1 種類）が archive 済の状態を前提に、旧 AiForms 互換の 13 Cell（Command / Button / Switch / Checkbox / Radio / SimpleCheck / Entry / Picker / TextPicker / NumberPicker / TimePicker / DatePicker / Custom）の MAUI Handler 実装と、`samples/maui/` への各 Cell 表示ページ追加、`ItemsSource` / `ItemTemplate` 動的バインド、Snapshot テスト基盤、移行ガイドを担当する。

本提案は元 `add-maui-bindings` 1 提案（89 タスク・spec.md 468 行）を分割した第 3 段（最終段）であり、Bridge / Binding csproj / MAUI 基盤に関する判断は先行提案で確定済。本提案では(1)「Cell ごとに `CellBaseHandler` パターンを適用する」、(2)「`ItemsSource` を `ApplyDiff/BuildAndSetRoot` 経路に乗せる」、(3)「Sample に Cell ページを追加する」、(4)「Snapshot テスト基盤を整備する」、(5)「移行ガイドを書く」という 5 つの主要トピックに集中する。

## Goals / Non-Goals

**Goals:**

- 13 Cell の `BindableObject` 定義（`CellBase` 派生）と Handler 実装（`CellBaseHandler<TCell, TNativeCell>` 派生）
- `add-maui-bridge` Bridge プロジェクトへの `addXxxCell(...)` メソッド追加（iOS Swift / Android Kotlin）
- `KsCellInteractionDelegate` / `KsCellInteractionListener` の 13 Cell 種別分の実体実装
- EntryCell の Native 側 200ms debounce + `updateCellValue` 直行パスの実体実装
- `BindingMode.TwoWay` 双方向バインドの 8 プロパティ（On / IsChecked / SelectedValue / ValueText / SelectedItem / Number / Time / Date）
- `SettingsView.ItemsSource` / `Section.ItemsSource` + `ItemTemplate` の動的バインド
- `samples/maui/` への基本 6 種ページ・入力 6 種ページ・CustomCell ページの追加、ナビゲーション導線
- Snapshot テスト基盤（`maui/KsSettingsView.Maui.SnapshotTests/`）
- 移行ガイド `docs/migration-from-aiforms.md`
- `MauiAppBuilderExtension.AddKsSettingsView()` への 13 Cell Handler 登録追加

**Non-Goals:**

- Native Bridge ライブラリ自体・Binding csproj の新規作成 → `add-maui-bridge`
- `KsSettingsView.Maui` 基盤（`SettingsView` / `Section` / `CellBase` / `SettingsViewHandler`）の新規作成 → `add-maui-core`
- LabelCell の実装 → `add-maui-core`
- MAUI Sample アプリ土台 → `add-samples-maui`
- macOS Catalyst / Windows サポート → Phase 6 以降
- NuGet 配信整備 → Phase 3
- KMP バインディング → 別変更提案

## Decisions

### Decision 1: Bridge API は本提案で「拡張」する

**選択**: `add-maui-bridge` の Bridge プロジェクト（`KsSettingsViewBridge` Swift / Kotlin、`KsSettingsView.Bindings.iOS/Android.csproj`）に対して、本提案では各 Cell 用 `addXxxCell(...)` メソッドや delegate / listener 実体メソッドを**追加していく**形で利用する。Bridge プロジェクト自体の新規作成は行わない。

**理由**:

- `add-maui-bridge` が「Bridge プロジェクトの存在 + 基本構造 + LabelCell の API」を確立済
- 本提案は確立済 Bridge にメソッドを追加するだけで Cell を増やせる構造
- Cell 種別ごとの追加が機械的なパターン適用で済む

**代替案**:

- Bridge を全部本提案で書く：元 `add-maui-bindings` の構造に戻ってしまい、分割の意義が失われる。**不採用**

### Decision 2: Handler 階層は add-maui-core で確立済パターンをパターン適用

**選択**: `add-maui-core` で確立した以下のパターンを 13 Cell 全てに機械的に適用する：

- `Cells/<Name>Cell.cs`：`CellBase` 派生、固有 BindableProperty を `BindableProperty.Create(...)` で宣言
- `Handlers/<Name>CellHandler.cs`：`CellBaseHandler<<Name>Cell, Native<Name>Cell>` 派生
- `Handlers/<Name>CellHandler.iOS.cs`：iOS 用 PropertyMapper（親 `SettingsViewHandler.ApplyDiff/BuildAndSetRoot()` をトリガする薄いラッパ）
- `Handlers/<Name>CellHandler.Android.cs`：Android 用 PropertyMapper（同上）

**理由**:

- `add-maui-core` の `LabelCell` で経路が動作証明済
- 13 Cell の Handler は基本的にコピー＆カスタマイズで実装可能
- `ApplyDiff/BuildAndSetRoot` 経路に統合することで、`add-maui-core` の Collection 同期テストと整合

**例外**:

- EntryCell の `ValueText` プロパティのみ、`ApplyDiff/BuildAndSetRoot` を経由せず `Bridge.UpdateCellValue` 直行パス（高頻度更新最適化）

**代替案**:

- Cell ごとに独自の Handler パターンを設計：一貫性が失われ保守コスト増。**不採用**

### Decision 3: 双方向バインドは add-maui-core の cellId Map 経路を利用

**選択**: 以下 8 プロパティの双方向バインドは、`add-maui-core` で確立した cellId Map 経路（`SettingsViewHandler` 内の `Dictionary<string, CellBase>`）を利用する：

| Cell | Property | BindingMode | Native → C# 経路 |
|------|----------|-------------|-----------------|
| SwitchCell | `On: bool` | TwoWay | `didChangeBoolValue(cellId, value)` |
| CheckboxCell | `IsChecked: bool` | TwoWay | `didChangeBoolValue(cellId, value)` |
| RadioCell | `SelectedValue: string` | TwoWay | `didChangeRadioSelection(groupId, value)` |
| SimpleCheckCell | `IsChecked: bool` | OneWay（注: 旧 AiForms と同じ。Switch/Checkbox と異なる） | （Native → C# なし） |
| EntryCell | `ValueText: string` | TwoWay | `didChangeTextValue(cellId, value)`（200ms debounce 後） |
| PickerCell / TextPickerCell | `SelectedItem` | TwoWay | `didChangePickerSelection(cellId, index)` |
| NumberPickerCell | `Number: int` | TwoWay | `didChangeNumberValue(cellId, value)` |
| TimePickerCell | `Time: TimeSpan` | TwoWay | `didChangeTimeValue(cellId, KsTime)` |
| DatePickerCell | `Date: DateTime` | TwoWay | `didChangeDateValue(cellId, KsDate)` |

**理由**:

- `add-maui-core` で経路が確立済（cellId → CellBase Map → `SetValue(...Property, value)`）
- 旧 AiForms ユーザーが期待する `BindingMode.TwoWay` を XAML で記述するだけで動作

**代替案**:

- 各 Cell が独自の経路を持つ：実装が分散し、テストが網羅困難。**不採用**

### Decision 4: ItemsSource / ItemTemplate は MAUI 層のみで対応

**選択**: 旧 AiForms `SettingsView.ItemsSource` / `ItemTemplate`（View 全体）と `Section.ItemsSource` / `ItemTemplate`（Section 単位）の互換 API を **`KsSettingsView.Maui` 層にのみ実装する**。Native (iOS / Android) の `KsSettingsViewCore` および `KsSettingsViewUI` 層には対応する API を追加しない。

**API 形（MAUI 層）**:

- `SettingsView.ItemsSource: IList`（`BindableProperty`）+ `SettingsView.ItemTemplate: DataTemplate`（テンプレート結果は `Section`）
- `Section.ItemsSource: IList`（`BindableProperty`）+ `Section.ItemTemplate: DataTemplate`（テンプレート結果は `CellBase` 派生）
- `Section.Cells`（静的）と `Section.ItemsSource`（動的）の**同時設定は禁止**（後者が設定された場合、`Cells` は内部用空コレクションとして扱われる）
- `DataTemplateSelector` も `DataTemplate` の代わりに受理可能（旧 AiForms 互換）
- `ItemsSource` の中身が `INotifyCollectionChanged`（典型的には `ObservableCollection<T>`）を実装している場合、`SettingsViewHandler` が内部で `CollectionChanged` を購読する

**実装経路**:

- `SettingsView.ItemsSource` / `ItemTemplate` の `propertyChanged`、および `ItemsSource` の `CollectionChanged` は `SettingsViewHandler` が捕捉し、`add-maui-core` で確立された `ApplyDiff/BuildAndSetRoot()` を呼ぶ
- `Section.ItemsSource` / `ItemTemplate` も同様。`Section.ItemsSource` を保持する `Section` は `ApplyDiff/BuildAndSetRoot()` 時にテンプレートを各要素に適用して `CellBase` インスタンスを生成し、生成された `Cells` を Bridge `Builder` に積む
- 生成された `CellBase` の `BindingContext` は `ItemsSource` の対応要素に設定される（標準 MAUI の `BindableObject.BindingContext` 経路）
- 素朴な実装としては「毎回全件再生成」で開始し、最適化は後続フェーズに委ねる

**理由**:

- **XAML には言語標準のループ構文がない**ため、`ItemsSource` + `DataTemplate` パターンは XAML 利用者にとって不可欠な API である（旧 AiForms 互換性の観点でも必須）
- **SwiftUI / Compose は言語標準で `ForEach` / `forEach` / `items` を持つ**ため、Native 層に `ItemsSource` 相当の API を入れると言語標準のループと API が二重化し、Native ユーザーに混乱を生む
- MAUI 層では既に `ApplyDiff/BuildAndSetRoot()` が「`ObservableCollection<Section>` / `Section.Cells` / Cell の `PropertyChanged` を購読 → 全体再構築 + `Bridge.SetRoot`」の集約経路として確立されているため、`ItemsSource` の `CollectionChanged` 購読も同じ経路に乗せれば追加複雑性は最小
- Bridge API も `setRoot(root)` 1 個に収束する設計のため、`ItemsSource` 対応のために Bridge API を増やす必要がない

**代替案**:

- **Native Core / UI 層にも `Section(items: [T], template: (T) -> KsCell)` を入れる**: SwiftUI / Compose の言語標準ループと API が二重化、`Hashable` / `equals` 契約破綻、ジェネリック制約の波及を招く。**不採用**
- **`ItemsSource` を `Section` 1 種類だけ対応し `SettingsView` 全体は対応しない**: 旧 AiForms 互換性が片肺になる。両方対応する。**不採用**
- **`DataTemplate` ではなく C# 関数 `Func<T, CellBase>` を受ける API**: XAML から書けないため旧 AiForms 互換性が損なわれる。**不採用**

**Native 層との責務境界（明示）**:

| 層 | ループ表現 | `ItemsSource` 概念 |
|----|-----------|-------------------|
| iOS Core (`KsSettingsViewCore` / Swift) | 静的 `[KsCell]` のみ。`@resultBuilder` DSL でユーザーが Swift コードでループを書く | **持たない** |
| iOS UI (`KsSettingsViewUI` / SwiftUI) | `ForEach` を `Section { ... }` ブロック内で利用 | **持たない** |
| Android Core (`ks-settingsview-core` / Kotlin) | 静的 `List<KsCell>` のみ。Kotlin DSL でユーザーがコードでループを書く | **持たない** |
| Android UI (`ks-settingsview-ui` Compose) | `forEach` / `items` を Compose DSL 内で利用 | **持たない** |
| MAUI (`KsSettingsView.Maui` / XAML) | XAML 標準ループ構文がないため `ItemsSource` + `ItemTemplate` を提供 | **対応**（本 Decision） |

### Decision 5: CustomCell の C# View → Native View 変換

**選択**: `CustomCell.ContentTemplate: DataTemplate` で生成された C# View（`Microsoft.Maui.Controls.View`）を `MauiView.ToPlatform(MauiContext)` でネイティブ View に変換し、Bridge 経由で `KsAnyView`（iOS: `KsAnyView.uiKit`、Android: `KsAnyView.AndroidView`）として Native CustomCell の content に格納する。

**理由**:

- `MauiView.ToPlatform()` は MAUI 標準 API として既に存在
- `add-cell-types-custom` で Native CustomCell が `KsAnyView` を Content として受け取る設計が確立済
- DataTemplate からのインスタンス化、Cell 再利用時の BindingContext 更新は標準 MAUI 機構に乗る

**代替案**:

- C# View を JSON シリアライズして Native に送る：型情報と挙動が失われ実用にならない。**不採用**

### Decision 6: Sample ページ追加は本提案の責務

**選択**: `samples/maui/` への各 Cell 表示ページ追加は、本提案の Handler 実装と**同一提案内**で行う。`add-samples-maui` で整備された Sample 土台（`LabelCell` 1 セクションのみのデモ）に対して、本提案では基本 6 種ページ・入力 6 種ページ・CustomCell ページの 3 つを追加し、MainPage からのナビゲーション導線も整備する。

**理由**:

- Handler 実装と Sample 表示は密接に関連しており、同一提案内で完結すると「実装はあるが動作確認できない」状態を回避できる
- 旧 `add-cell-types-*` 系で残っていた MAUI Sample 拡張の責務移譲問題が、本構造で完全に解消される
- ユーザーは本提案完了時点で「Sample アプリを起動 → 各 Cell 種別の動作を全て目視確認できる」状態になる

**代替案**:

- Sample ページ追加を別変更提案に分ける：依存図が更に複雑化、本提案完了時点で動作確認できない問題が残る。**不採用**

### Decision 7: Snapshot テストフレームワーク

**選択**: Snapshot テストは `Verify + Microsoft.Maui.TestUtils` を第一候補とし、Phase A 実装時にユーザー確認を経て確定する。代替候補として `VerifyTests` + プラットフォーム別レンダリングヘルパも検討する。

**理由**:

- `Verify` は .NET エコシステムで広く使われ、CI 連携も成熟している
- `Microsoft.Maui.TestUtils` で MAUI Page のレンダリングが可能

**Open Question**: 確定はユーザー確認後（後述 Open Questions 参照）

### Decision 8: 移行ガイドは本提案で書く

**選択**: `docs/migration-from-aiforms.md` を本提案で新規作成し、以下を含む：

1. 概要・対象読者
2. 名前空間変更（`AiForms.Settings` → `KsSettingsView.Maui`）
3. 初期化コード差し替え（`AddSettingsViewHandler` → `AddKsSettingsView`）
4. Cell プロパティ対応表（旧 → 新）の全 15 Cell 分
5. Sample の差し替え例
6. ItemsSource / ItemTemplate の利用例
7. メモリリーク対策の差分（`UseSettingsView(true)` フックの再実装）

**理由**:

- 全 Cell 実装と Sample 拡張が完了するタイミングで書くのが最も正確
- ユーザーは本提案完了時点で「旧 AiForms から移行する」全情報が揃う

## Risks / Trade-offs

- **リスク**: 13 Cell の Handler 実装数が多く、`CellBaseHandler` パターンが正しく適用されないと一貫性が失われる
  - **緩和策**: `add-maui-core` の `LabelCellHandler` を参考実装とし、Cell ごとのレビュー基準（PropertyMapper が `ApplyDiff/BuildAndSetRoot` を呼ぶだけか、TwoWay の経路が cellId Map を経由するか）をチェックリスト化
- **リスク**: 双方向バインド（TwoWay）8 プロパティのテスト網羅が不足
  - **緩和策**: 各 Cell の Handler テスト + cellId Map 経由の delegate コールバックテストを Snapshot テストとは別に xunit で実装
- **リスク**: `ItemsSource` / `ItemTemplate` の `DataTemplate` インスタンス化や `BindingContext` 連動が MAUI 9 内部実装に依存
  - **緩和策**: 旧 AiForms `ItemsSourceManager.cs` の実装を参考にしつつ、`BindingContext` 伝播は標準 MAUI 機構（`BindableObject.BindingContext` の自動伝播）に乗る
- **リスク**: CustomCell の C# View → Native View 変換が iOS / Android で挙動差がある
  - **緩和策**: `MauiView.ToPlatform()` の戻り値型差異（iOS: `UIView`、Android: `View`）を Bridge `KsAnyView` で吸収、両 OS での動作確認を Snapshot テストとは別に実機・エミュレータで実施
- **リスク**: EntryCell の Native 側 200ms debounce が C# 側の `BindingMode.TwoWay` 期待と齟齬を起こす
  - **緩和策**: debounce 仕様（200ms、最終確定値のみ通知）を `docs/migration-from-aiforms.md` に明記、旧 AiForms ユーザーが期待する動作との差分を解説
- **リスク**: Sample ページ追加で UI 設計が肥大化し、Sample の起動時間や保守コストが増える
  - **緩和策**: 各 Cell ページは最小構成（1 セクション・全プロパティ表示のみ）に絞り、デザイン凝りすぎない
- **リスク**: Snapshot テストの初回ゴールデンイメージ生成と CI での再現性
  - **緩和策**: CI 環境（macOS / Linux Android）で初回実行 → 開発者承認 → コミット、というワークフローを `docs/development.md` に明記

## Open Questions

- Snapshot テストフレームワークは `Verify + Microsoft.Maui.TestUtils` で確定するか、別フレームワーク（`SnapshotTesting` 等）を採用するか → Phase A 実装着手前にユーザー確認
- `ItemsSource` で生成された `CellBase` インスタンスのライフサイクル管理：「毎回全件再生成」で開始するが、`ObservableCollection.Move` イベントでの最適化（既存インスタンス再利用）は本提案で実装するか後続フェーズに委ねるか → Phase B 着手時に判断
- 移行ガイドの読者層：旧 AiForms の経験者のみ vs. MAUI 初心者も含む → 前者を主読者、後者向けには別途入門ガイドの作成を検討（本提案外）
