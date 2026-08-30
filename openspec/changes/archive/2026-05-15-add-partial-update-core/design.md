## 参考実装

本変更提案は、`AiForms.Maui.NativeCollectionView` の部分更新パターンを Core 層に反映する。実装着手前に以下を熟読すること。

- `../AiForms.Maui.NativeCollectionView/AiForms.Maui.NativeCollectionView/Platforms/iOS/NativeViewProviderOfSectionModel.cs` — iOS の `NSDiffableDataSourceSnapshot` 部分操作パターン
- `../AiForms.Maui.NativeCollectionView/AiForms.Maui.NativeCollectionView/Platforms/Android/NativeViewProviderOfSectionModel.cs` — Android の `ListAdapter.SubmitList` パターン
- `openspec/drafts/02-partial-update-design.md` — 探索モードでの議論結果まとめ

## Context

archive 済み `add-settings-view-core` で確立した `SettingsRoot` / `Section` / `RootAccessory` / `SectionAccessory` / `KsCell` 等のドメイン型は、`Hashable` / `equals` 契約を満たすことで `UICollectionViewDiffableDataSource` / `DiffUtil` の差分検出を前提とする値型ベース設計だった。しかし「root を毎回作り直す」前提なため、部分更新が必要な大量データ・高頻度更新ユースケースには対応できない。

本提案は Core 層に Diff 型を追加し、後続提案 `add-partial-update-native` で部分更新 API を実装するための型基盤を整備する。Core 層は UI 非依存であるため、Diff 型自体は「データ変更の表現」のみを担い、実際の `applyDiff` 実装は Native UI 層・MAUI 層が担当する。

## Goals / Non-Goals

**Goals:**

- `SettingsRootDiff` 型（Swift `enum` / Kotlin `sealed interface`）の追加
- `AccessoryTarget` 型の追加（Root / Section の H/F を統一表現）
- `SettingsAccessory` 型の追加（`RootAccessory` / `SectionAccessory` を `updateAccessory` Diff で扱うラッパ）
- `SettingsRoot` から `header` / `footer` プロパティを削除し、`SettingsRoot` のドメイン責務を「sections + theme」に絞る
- 既存 `RootAccessory` / `SectionAccessory` 型は維持（区別を保持する設計決定 5）
- ユニットテストで Diff 型および AccessoryTarget の生成・Hashable / equals 契約を検証

**Non-Goals:**

- `SettingsRootStore` などのストア抽象 → `add-partial-update-native`（Native UI 層の責務）
- `KsSettingsViewController.applyDiff` などの Native UI 層実装 → 同上
- MAUI Bridge の `applyDiff` DTO → 進行中の `add-maui-bridge` で対応
- Diff の「適用順序の保証」「アニメーション結合」などの Native 実装挙動 → Native UI 層の責務

## Decisions

### Decision 1: SettingsRoot から header / footer を削除し、UI 層責務化

**選択**: `SettingsRoot.header: RootAccessory?` / `SettingsRoot.footer: RootAccessory?` を削除する。Root H/F は UI 層（View）のプロパティとして扱う（後続提案 `add-partial-update-native` で `KsSettingsViewController.rootHeader` などを定義、進行中 MAUI 提案で `SettingsView.HeaderView` BindableProperty を定義）。

**理由**:

- Root H/F は「データ」というより「View 装飾」の責務であり、ドメインモデルから切り離す方が責務分離が明確
- 旧 `AiForms.Maui.SettingsView` / `AiForms.Maui.NativeCollectionView` も `HeaderView` / `FooterView` を View 側のプロパティとして公開しており、移行コスト面でも有利
- `SettingsRoot` の Hashable / equals 契約がシンプル化（`KsAnyView` 関連の特例実装が `Section.header/footer` のみに集約）
- `SettingsRoot` の責務が「sections + theme」に絞られ、Diff API も Section 中心となり一貫性が高まる

**代替案**:

- `SettingsRoot.header/footer` を維持し、UI 層からも参照する：データと View 責務が混在し、Diff API も `updateRootAccessory` と `updateSectionAccessory` で表現が分かれる。**不採用**
- `SettingsAccessory` 型自体を撤廃し、`RootAccessory` を `SettingsRoot` に置く：UI 層責務化の方針と矛盾する。**不採用**

### Decision 2: SettingsRootDiff は sealed enum / sealed interface

**選択**: `SettingsRootDiff` を Swift `enum`、Kotlin `sealed interface` として定義する。各ケースは `case` / `data class` として独立した payload を持つ。

**理由**:

- `switch` / `when` で網羅性チェックが効く（コンパイル時に未対応ケースを検出）
- Diff の種類を Core で固定することで、Native UI 層・MAUI Handler 層の `applyDiff` 実装が網羅可能になる
- `data class` / `enum case` は `Hashable` / `equals` を自動取得でき、テストが書きやすい

**代替案**:

- `protocol SettingsRootDiff` + 各 Diff を struct/class で実装：拡張性は高いが、Core 外部からも Diff を作れてしまい型安全性が下がる。**不採用**
- 単一 struct + `DiffKind` enum + payload の Dictionary：型安全性が下がる、API も冗長。**不採用**

### Decision 3: updateAccessory は target 引数で Root / Section を統一表現

**選択**: Accessory 更新の Diff ケースは `updateAccessory(target: AccessoryTarget, accessory: SettingsAccessory?)` の 1 つに統一する。

```swift
// Swift
case updateAccessory(target: AccessoryTarget, accessory: SettingsAccessory?)

public enum AccessoryTarget: Hashable {
    case rootHeader
    case rootFooter
    case sectionHeader(sectionID: UUID)
    case sectionFooter(sectionID: UUID)
}

public enum SettingsAccessory: Hashable {
    case root(RootAccessory)
    case section(SectionAccessory)
}
```

**理由**:

- API 表面積を抑えられる（4 ケース × 1 ケースで済む）
- Native UI 層の `applyDiff` 実装で switch 分岐が 1 箇所に集約
- `accessory = nil` で「削除」、非 nil で「追加・更新」を表現でき、利用者 API がシンプル

**代替案**:

- `updateRootHeader` / `updateRootFooter` / `updateSectionHeader` / `updateSectionFooter` の 4 ケース：API ケース数が増え、`applyDiff` の switch も冗長。**不採用**
- Diff 型を Root / Section で分ける（`SettingsRootDiff` と `SectionDiff`）：Diff のフラットな配列で扱えなくなり、Store API が複雑化。**不採用**

### Decision 4: RootAccessory / SectionAccessory を区別保持

**選択**: `RootAccessory` 型と `SectionAccessory` 型を統一せず、別型として維持する（archive 済 `settings-view-core` spec の Requirement を継承）。`SettingsAccessory` は両者を `updateAccessory` Diff で扱うための統一ラッパとして導入する。

**理由**:

- archive 済み `settings-view-core` spec で「SectionAccessory との別型保証」Scenario が定義されており、API 互換性を維持
- 将来的に Root と Section で異なる accessory ケースを追加する余地を残す（例: Root のみ `.banner` ケース等）
- 型安全性（Root H/F 用 API には `RootAccessory` を、Section H/F 用 API には `SectionAccessory` を要求）

**代替案**:

- `RootAccessory` と `SectionAccessory` を統一して `Accessory` にする：archive 済 spec の Requirement を破壊する。**不採用**
- `SettingsAccessory` を撤廃し、`updateAccessory` で `Any` / `Object` を受ける：型安全性が大幅低下。**不採用**

### Decision 5: Section 内 KsCell の moveCell は toIndex のみで指定

**選択**: `moveCell(cellID: KsCellID, toIndex: Int)` とし、`fromIndex` は内部状態から解決する。Section 間移動はサポートしない（Section 内移動のみ）。

**理由**:

- `cellID` から `fromIndex` は一意に決まるため、引数の二重指定は冗長
- Section 間移動は AiForms 系の `NotifyCollectionChangedAction.Move` 仕様でもサポートされない（同一 ObservableCollection 内のみ）
- Section 間移動が必要な場合は `removeCell` + `insertCell` の 2 操作で表現できる

**代替案**:

- `moveCell(cellID, fromIndex, toIndex)` 形式：冗長、`fromIndex` が内部状態と矛盾する場合の挙動が不明確。**不採用**
- Section 間移動 `moveCell(cellID, toSectionID, toIndex)` をサポート：使用頻度が低く、`applyDiff` 実装の複雑度が増す。**不採用**

### Decision 6: moveSection は from / to の両方を Int で指定

**選択**: `moveSection(from: Int, to: Int)` とする。`sectionID` ベースではなく index ベース。

**理由**:

- 旧 AiForms `NotifyCollectionChangedAction.Move` の `OldStartingIndex` / `NewStartingIndex` 仕様と一致
- Section 数は Cell 数より少ないため、index ベースで十分判別可能
- `moveCell` の `toIndex` だけ指定とは異なるが、Section レベルでは ObservableCollection の Move アクションがそのまま使えるため互換性重視

**代替案**:

- `moveSection(sectionID: UUID, to: Int)`：内部解決必要、`moveCell` との一貫性は取れるが ObservableCollection 互換性が下がる。**不採用**

### Decision 7: Diff の Hashable / equals 契約

**選択**: `SettingsRootDiff` / `AccessoryTarget` / `SettingsAccessory` は Swift `Hashable`、Kotlin `data class` で `equals` / `hashCode` を自動取得する。ただし `replaceCell` / `insertCell` / `replaceSection` / `insertSection` の payload に含まれる `any KsCell` / `Section` / `Theme` などは既存型の `Hashable` / `equals` 契約に従う。

**理由**:

- テストで Diff インスタンスの比較がしやすい
- 重複 Diff の検出や、テスト用 mock の比較で必要
- 既存型の Hashable / equals 契約と整合

**代替案**:

- Diff を Hashable / equals 非対応にする：テストが書きにくい、参照同一性のみで比較する不便さ。**不採用**

## Risks / Trade-offs

- **リスク**: `SettingsRoot.header/footer` 削除による既存利用コードの破壊
  - **緩和策**: 利用者は現状おらず（変更提案がまだ archive されていない）、archive 済み `samples-ios` / `samples-android` のサンプルコードは後続 `add-partial-update-native` で修正する前提。本提案単体では archive せず、`add-partial-update-native` と同時に archive する運用を検討
- **リスク**: Diff API のスコープ漏れ
  - **緩和策**: 探索モードで NativeCollectionView の `NotifyCollectionChangedAction` 全 5 ケース（Add/Remove/Move/Replace/Reset）を網羅。Section レベル・Cell レベル・Accessory・Theme をカバー。発覚時は本提案を archive 取り消し → Diff 型に追加 → 再 archive、または後続提案で Diff 型に追加パッチを当てる運用
- **リスク**: `SettingsAccessory` 統一ラッパが冗長
  - **緩和策**: `updateAccessory` 1 ケースで Root / Section H/F を表現するために必要。利用者は通常 `RootAccessory` / `SectionAccessory` を直接扱うため、`SettingsAccessory` への変換は Store API 側で吸収する設計とし、Diff DTO のみが知る型として運用

## Migration Plan

本提案は archive 済み `settings-view-core` の MODIFIED であり、`SettingsRoot.header/footer` の削除は破壊的変更。archive 順序：

1. `add-monorepo-foundation`（archive 済）
2. `add-settings-view-core`（archive 済、本提案で MODIFIED）
3. **本提案**（`add-partial-update-core`）archive ※ 後続 `add-partial-update-native` archive と同時に行う
4. `add-partial-update-native` archive

`add-settings-view-core` で archive された spec の以下 Requirement / Scenario を MODIFIED する：

- `SettingsRoot ドメインモデル` Requirement の `header` / `footer` 部分を削除
- `等価性（H/F なし同士）` / `等価性（text 同士）` / `等価性（view 同士、中身は無視）` / `等価性（nil と非 nil）` Scenario を削除
- 新規 Requirement: `SettingsRootDiff 型` / `AccessoryTarget 型` / `SettingsAccessory 型` を追加

## Open Questions

- **Cell ID の表現**: Swift では `KsCellID(cell:)` で Cell 自身から ID を導出するが、Kotlin の `Cell.id: String` との一貫性をどう取るか。本提案では Swift `KsCellID`、Kotlin `String` のまま Diff 型で扱う（既存設計を継承）が、`replaceCell` 時に「ID は同じだが内容が違う Cell」を渡す場合の identity 保証は Native UI 層の責務とする
- **Diff の連続適用順序**: 本提案では Diff 単体の表現のみ定義し、「複数 Diff の適用順序」「逆順適用」などの保証は Native UI 層の責務とする。バッチング最適化は将来の別提案で検討
- **本提案単体 archive 可否**: `SettingsRoot.header/footer` 削除により既存サンプルコードが破壊されるため、`add-partial-update-native` と同時 archive を強く推奨。OpenSpec 運用上、archive 順序の制約をどう表現するか（Migration Plan に記載するか、`dependsOn` 関係で表現するか）は実装着手時に判断
