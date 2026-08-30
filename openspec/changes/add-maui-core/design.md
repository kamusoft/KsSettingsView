## 参考実装

本変更提案は元 `add-maui-bindings` から「MAUI 本体基盤」を切り出したものであり、設計判断の多くは元提案の Decision 群を継承する。実装着手前に以下を熟読すること。

- [`openspec/changes/add-maui-cells/design.md`](../add-maui-cells/design.md)（元 `add-maui-bindings` の design.md。Decision 4〜8 の経緯）
- [`openspec/changes/add-maui-bridge/design.md`](../add-maui-bridge/design.md)（先行提案）
- [`docs/legacy-aiforms-reference.md`](../../../docs/legacy-aiforms-reference.md) — §2（CellBase 22 プロパティ）、§4（SettingsView 全体プロパティ）、§5（Handler 階層）、§8（HandlerCleanUpHelper パターン）
- 原典コード（必読）：
  - `../AiForms.Maui.SettingsView/SettingsView/Cells/CellBase.cs`
  - `../AiForms.Maui.SettingsView/SettingsView/SettingsView.DefineProperites.cs`
  - `../AiForms.Maui.SettingsView/SettingsView/Handlers/SettingsViewHandler.cs`
  - `../AiForms.Maui.SettingsView/SettingsView/MauiAppBuilderExtension.cs`

## Context

`add-maui-bridge` で Native Bridge と MAUI バインディングプロジェクトが整備された状態を前提に、`.NET MAUI` XAML ユーザー向けの `KsSettingsView.Maui` 本体ライブラリを整備する。本提案は元 `add-maui-bindings` 1 提案（89 タスク・spec.md 468 行）を分割した第 2 段で、Handler 階層・`BindableObject` Cell 基盤・全体再構築方式・メモリリーク対策の「骨組み」を担当する。

各 Cell 種別の Handler 実装（Command/Button/Switch/Checkbox/Radio/SimpleCheck/Entry/Picker/TextPicker/NumberPicker/TimePicker/DatePicker/Custom の 13 種類）は本提案 archive 後に `add-maui-cells` で実装する。本提案では **LabelCell 1 種類のみ** を実装することで、Bridge ↔ Handler の経路を最小コストで動作証明し、後続提案で 13 Cell をパターン適用するだけの状態を作る。

## Goals / Non-Goals

**Goals:**

- `maui/KsSettingsView.Maui/` 新規プロジェクト（.NET 9 / MAUI Library）
- `SettingsView : View` / `Section : BindableObject` / `CellBase : BindableObject` の基盤（`SettingsRootDefinition` は Decision 5 により不採用、`SettingsView` 自身がルートを保持）
- `SettingsViewHandler` + `CellBaseHandler<TVirtualCell, TNativeCell>` の Handler 階層
- `BuildAndSetRoot()` 共通ヘルパによる「初回構築 + Bridge.SetRoot」経路、および `ApplyDiff(SettingsRootDiff)` ヘルパによる「ObservableCollection 連動 + Bridge.ApplyDiff」経路
- `KsCellInteractionDelegate` / `KsCellInteractionListener` の C# 側実装、cellId → CellBase Map 再構築
- `MauiAppBuilderExtension.AddKsSettingsView()` 拡張
- `DisconnectHandler` 必須化 + `HandlerCleanUpHelper` パターン
- `MauiSettingsViewLeakTests` で `WeakReference` リーク検出
- `LabelCell` 1 種類の最小 Cell 実装（Bridge ↔ Handler 経路動作証明）
- ユニットテスト（Collection 同期 / cellId Map / HeaderFooter / リーク検出）

**Non-Goals:**

- 13 Cell（Command〜Custom）の Handler 実装 → `add-maui-cells`
- `ItemsSource` / `ItemTemplate` 対応（旧 AiForms 互換、`SettingsView.ItemsSource` / `Section.ItemsSource`）→ `add-maui-cells`
- Snapshot テスト基盤 → `add-maui-cells`
- 移行ガイド `docs/migration-from-aiforms.md` → `add-maui-cells`
- MAUI Sample アプリ → `add-samples-maui`
- NuGet 配信整備 → Phase 3 以降

## Decisions

### Decision 1: MAUI Cell は BindableObject 階層を踏襲

**選択**: 旧 `AiForms.Maui.SettingsView` の `CellBase`、`SettingsView`、Handler 階層をほぼそのまま再現する。名前空間は `KsSettingsView.Maui`。

**理由**:

- XAML ユーザーが期待する `BindingMode.TwoWay`、`PropertyMapper` パターンを保証
- 旧 AiForms ユーザーの移行コストを下げる
- 既存の旧 AiForms 実装が参考として大量に利用可能

**代替案**:

- 全く新しい Cell 抽象を設計：移行コスト爆発、XAML 利用者の学習コスト増。**不採用**

### Decision 2: IList 受け入れ + ObservableCollection 自動購読 + applyDiff 方式（旧 AiForms.Maui.NativeCollectionView 方式採用）

**選択**: `SettingsView.Sections` および `Section.Cells` は `IList<T>` 型で公開する（厳格な `ObservableCollection<T>` 型は要求しない）。`SettingsViewHandler` はこれらのコレクションが `INotifyCollectionChanged` を実装するかを実行時に判定し、

- **実装する場合**（例: `ObservableCollection<T>`）：`CollectionChanged` を購読し、変更検知時に **`NotifyCollectionChangedAction` の種別に応じて `KsSettingsRootDiffDTO` を組み立て `Bridge.ApplyDiff(diff)` を呼ぶ**（旧 `AiForms.Maui.NativeCollectionView` の `OnCellCollectionChanged` 流儀）
- **実装しない場合**（例: `List<T>`）：購読は登録せず、初回 `Bridge.SetRoot` のみで静的描画する。後続の `list.Add(...)` 等は反映されない

初回構築および `Reset` アクション時、および `Sections` プロパティ自体の再代入時は `Bridge.SetRoot(root)` を呼ぶ。

`NotifyCollectionChangedAction` ごとの対応：

- `Add` → `KsSettingsRootDiffInsertCellDTO` / `KsSettingsRootDiffInsertSectionDTO`
- `Remove` → `KsSettingsRootDiffRemoveCellDTO` / `KsSettingsRootDiffRemoveSectionDTO`
- `Move` → `KsSettingsRootDiffMoveCellDTO` / `KsSettingsRootDiffMoveSectionDTO`
- `Replace` → `KsSettingsRootDiffReplaceCellDTO` / `KsSettingsRootDiffReplaceSectionDTO`
- `Reset` → `Bridge.SetRoot(root)` を呼ぶ（フォールバック）

Cell の `PropertyChanged` は `KsSettingsRootDiffReplaceCellDTO` に変換、Section の `Header` / `Footer` プロパティ変更は `KsSettingsRootDiffUpdateAccessoryDTO` に変換する。`SettingsView.HeaderView` / `FooterView` 変更は `Bridge.SetRootHeader(view:)` / `Bridge.SetRootFooter(view:)` を呼ぶ（`SettingsRoot` ドメインモデルから `header/footer` が削除されたため）。

**理由**:

- `add-partial-update-core` / `add-partial-update-native` で Native UI 層に導入される部分更新 API を MAUI 側からも享受できる
- 大量データ・高頻度更新ユースケースで O(N) 全体再構築のコストを回避できる
- `AiForms.Maui.NativeCollectionView` の `SetDataSource(IList itemsSource)` の実装と整合：`IList<T>` を受け取り、`ObservableCollection<T>` ならイベント購読する設計
- 利用者の柔軟性：単純なユースケースでは `List<Section>` で静的に渡せる、動的更新が必要なら `ObservableCollection<Section>` を使う
- C# 側 `CollectionChanged` ハンドリングは「Action 分岐 + DTO 生成」のシンプルな構造で、双方向対応関係（cellId Map）の差分更新も同経路で扱える

**代替案**:

- 全体再構築 + `Bridge.SetRoot(root)` 方式（旧 `add-maui-bindings` Decision 6b 継承）：大量データで非効率、`add-partial-update-*` で導入する部分更新の効果が MAUI 側に届かない。**不採用（本提案で方針転換）**
- 旧 `AiForms.Maui.SettingsView` の `ModelProxy` 方式（個別 `Bridge.InsertSection` 等 API）：Bridge API 表面が肥大、`SettingsRootDiff` 抽象を再発明することになる。**不採用（`SettingsRootDiff` を採用済み）**
- `Sections` / `Cells` の型を `ObservableCollection<T>` に厳格化：単純なユースケースで `ObservableCollection` を強制するため利用者の柔軟性が下がる、`AiForms.Maui.NativeCollectionView` との一貫性を欠く。**不採用**

### Decision 3: cellId ベースの双方向対応マップ

**選択**: `SettingsViewHandler` が `Dictionary<string, CellBase>` Map を `BuildAndSetRoot` 時に全体再構築し、`ApplyDiff(InsertCell / RemoveCell / ReplaceCell)` 時に差分更新する。Bridge の delegate / listener コールバックを受けた際は cellId から CellBase を引き、`cell.SetValue(...Property, value)` で C# Cell に書き戻す。

**理由**:

- Cell インスタンスのライフサイクル管理が単純化（`BuildAndSetRoot` で全体構築、`ApplyDiff` で差分更新）
- 双方向バインドの実装が 1 箇所に集約

**代替案**:

- Bridge から直接 C# CellBase 参照を返す：型変換コスト・参照管理コスト増。**不採用**

### Decision 4: 本提案では LabelCell のみ実装

**選択**: 本提案で実装する Cell 種別は `LabelCell` のみ。Bridge 側も `add-maui-bridge` で `addLabelCell(...)` 1 個のみ公開済。残り 13 Cell（Command/Button/Switch/Checkbox/Radio/SimpleCheck/Entry/Picker/TextPicker/NumberPicker/TimePicker/DatePicker/Custom）は `add-maui-cells` で実装する。

**理由**:

- LabelCell は最も単純（双方向バインド不要、操作 delegate 不要）、Bridge ↔ Handler 経路を最小コストで検証できる
- Handler 階層パターン（`CellBaseHandler<TCell, TNativeCell>` 派生、`PropertyMapper` で `ApplyDiff(ReplaceCell)` をトリガ）が `LabelCellHandler` で確立されれば、13 Cell は機械的なパターン適用となる
- `AddKsSettingsView()` への Cell Handler 登録は `add-maui-cells` で追加していく前提（本提案の `AddKsSettingsView()` は `SettingsViewHandler` + `LabelCellHandler` のみ登録）

**代替案**:

- 本提案で 14 Cell 全 Handler を実装：規模が元 `add-maui-bindings` と変わらず分割の意義が失われる。**不採用**
- LabelCell も `add-maui-cells` で実装し、本提案では Handler 基盤のみ：Bridge ↔ Handler 経路がコンパイル可能な状態でない、ユニットテスト書けず動作検証不能。**不採用**

### Decision 5: 命名衝突回避（SettingsRoot）

**選択**:

- Native 側（Bridge 経由で渡る DTO）: `KsSettingsRootDTO`（`add-maui-bridge` で命名確定）
- MAUI 側（XAML ルート）: 専用 BindableObject 型は導入せず、`SettingsView : View` 自身が `Sections` / `HeaderView` / `FooterView` を直接保持する形にする

**理由**:

- `add-partial-update-core` で `SettingsRoot.header/footer` が削除されたため、ドメインモデルは「sections + theme」のみとなり、MAUI 側でも `SettingsRootDefinition` のような「XAML ルート用 BindableObject」型を導入する必要性が低下
- 旧 `AiForms.Maui.SettingsView` / `AiForms.Maui.NativeCollectionView` も `SettingsView` 自身が `Root` または `ItemsSource` を直接保持する設計であり、XAML 利用者の慣習に合致
- 命名衝突は「Native は `KsSettingsRootDTO`、MAUI は `SettingsView`」で十分回避できる

**代替案**:

- どちらも `SettingsRoot` のままで namespace で区別：実装中の混乱が大きい。**不採用**
- `SettingsRootDefinition`（XAML ルート用 BindableObject）を導入：ドメインモデルから `header/footer` が削除されたためメリットが小さい、`SettingsView` 自身でルート保持できる構造が単純。**不採用（本提案で撤回）**

### Decision 6: メモリリーク対策の基盤化

**選択**: 以下のリーク対策を本提案で**基盤として全 Handler に組み込む**：

- `SettingsViewHandler.DisconnectHandler`：Bridge 参照解放、`CollectionChanged` 購読解除、cellId Map クリア
- `CellBaseHandler.DisconnectHandler`：Native Cell 参照解放
- `HandlerCleanUpHelper`：Page.NavigatedFrom フックで明示的 Disconnect（旧 AiForms 由来）
- `MauiSettingsViewLeakTests`：10 回 push/pop で `WeakReference` がゼロになることを検証

**理由**:

- MAUI 9 Handler のメモリリーク既知問題に対応
- `add-maui-cells` で 13 Cell Handler を追加する際、本提案の `CellBaseHandler` を継承するだけでリーク対策が自動適用される

**代替案**:

- リーク対策を `add-maui-cells` で後追い実装：Handler 階層が確立してからリーク対策を入れるのは手戻りが大きい。**不採用**

### Decision 7: AddKsSettingsView の段階的拡張

**選択**: `MauiAppBuilderExtension.AddKsSettingsView()` は本提案で `SettingsViewHandler` + `LabelCellHandler` のみ登録する。`add-maui-cells` 側で残り 13 Cell Handler を本メソッドに追加していく。

**理由**:

- `AddKsSettingsView()` メソッド自体は本提案で完成（追加変更は handler 登録行のみ）
- ユーザーが `AddKsSettingsView()` を呼べば、本提案完了時点で利用可能な Cell（LabelCell）が自動登録される

**代替案**:

- Cell Handler 登録を `AddKsSettingsView()` の引数で個別指定：API 表面積が肥大、デフォルトで全 Cell が使えない。**不採用**

### Decision 8: 連続変更のバッチング最適化は任意

**選択**: 同一 UI フレーム内で `ApplyDiff(...)` が複数回連続して呼ばれそうな場合に `Dispatcher.DispatchDelayed(0, ...)` で 1 回の `BuildAndSetRoot` にまとめる仕組みは、本提案では**任意**とする（実装しても良いが必須ではない）。`add-maui-cells` 完了時点でのパフォーマンス測定により必要性を判断し、別途バッチング最適化変更提案で対応する。

**理由**:

- 設定画面ユースケースでは Section 数 5〜10、Cell 総数 50〜100 が想定範囲、`ApplyDiff` を 1 回ずつ呼ぶ方針でも実用上問題ない見込み（旧 AiForms.Maui.NativeCollectionView も batch を持たない）
- バッチング実装にはタイミング制御の複雑性が伴うため、必要性を測定してから判断する方が安全

**代替案**:

- 本提案で必ずバッチングを実装：複雑性増、必要性が不明な段階での過剰設計。**不採用**

## Risks / Trade-offs

- **リスク**: MAUI 9 Handler のメモリリーク既知問題
  - **緩和策**: `DisconnectHandler` 必須化、`HandlerCleanUpHelper` パターン、CI 自動 `WeakReference` テスト（Decision 6）
- **リスク**: `add-maui-bridge` の API 設計に見落としがあり、本提案実装中に発覚する
  - **緩和策**: 発覚時は (1) 小規模であれば本提案内で Bridge プロジェクトに追加コミット（`add-maui-bridge` 範囲を侵犯しない範囲のみ）、(2) 大規模であれば `add-maui-bridge` を archive 取り消し → 修正 → 再 archive、の運用を Open Questions に記載
- **リスク**: LabelCell のみで Handler 基盤を確立すると、双方向バインド（`BindingMode.TwoWay`）を持つ Cell（Switch / Entry 等）の経路検証が `add-maui-cells` まで先送りされる
  - **緩和策**: 本提案の `KsCellInteractionDelegate` C# 実装には 14 Cell 種別分のメソッドシグネチャを定義しておき、`add-maui-cells` 側で実体実装を埋める形にする。`SettingsViewHandlerCellMapTests.cs` で `didChangeBoolValue` 等の擬似コールバックを発火させて cellId Map → CellBase.SetValue 経路を検証する（CellBase 派生で擬似 BoolProperty を持つテスト用 Cell を使う）
- **リスク**: `HandlerCleanUpHelper` の Page.NavigatedFrom フックが MAUI 9 で動作仕様が変わっている可能性
  - **緩和策**: 旧 AiForms 実装を参考にしつつ、本提案実装時に MAUI 9 公式ドキュメントで `Page` ライフサイクル API を確認、`MauiSettingsViewLeakTests` で実際にリーク検出が機能することを検証

## Migration Plan

本提案は新規追加のため移行手順は不要。archive 順序：

1. `add-maui-bridge` archive 済
2. 本提案（`add-maui-core`）archive
3. `add-samples-maui` archive
4. `add-maui-cells` archive

## Open Questions

- `add-maui-bridge` API 設計の見落とし発覚時の運用：本提案内で Bridge に小規模パッチを当てるか、`add-maui-bridge` archive 取り消し → 修正 → 再 archive とするか。**判断基準**：パッチが Bridge `addLabelCell(...)` 以外の Cell 追加 API に踏み込まない範囲で済むなら本提案内追加、そうでなければ archive 取り消しとする。
- `MauiSettingsViewLeakTests` の実行環境：CI で `dotnet test -f net9.0-ios` / `-f net9.0-android` を走らせるための環境設定は本提案で整備するか、`add-samples-maui` archive 後の CI 整備フェーズで対応するか。**暫定方針**：本提案ではローカル実行のみ確認、CI 設定は別フェーズ。
