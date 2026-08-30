## Why

`KsSettingsView` の現状実装は `SettingsRoot` 全体差し替え方式で、root を代入するたびに O(N) で全 cell をループして snapshot/list を再構築する。動的な追加・削除のたびに root 全体を作り直す必要があり、大量データ（10,000 件超 + 高頻度更新）で実用上の問題が発生する。本提案は、`AiForms.Maui.NativeCollectionView` の部分更新パターンを参考に、部分更新を可能にするための **Core 層のデータ抽象** を整備する。

`SettingsRootDiff` 型と `AccessoryTarget` 型を Core に追加することで、後続提案 `add-partial-update-native` で Native UI 層の部分更新 API と `SettingsRootStore` を、進行中の `add-maui-bridge` / `add-maui-core` で MAUI Bridge と Handler の部分更新経路を実装するための共通基盤を提供する。

## What Changes

- **BREAKING**: `SettingsRoot.header` / `SettingsRoot.footer` プロパティを削除する
  - Root Header / Root Footer は `SettingsRoot` ドメインモデルの責務ではなく、UI 層（View）の責務とみなす
  - 既存の `SettingsRoot` 等価性判定および Hashable 実装も `header` / `footer` を含まない構造に変更
- 新規型 `SettingsRootDiff` を `settings-view-core` に追加（Swift `enum` / Kotlin `sealed interface`）
  - `full(SettingsRoot)`: 全体差し替え
  - `insertSection(index, Section)` / `removeSection(sectionID)` / `moveSection(from, to)` / `replaceSection(sectionID, new)`
  - `insertCell(sectionID, index, Cell)` / `removeCell(cellID)` / `replaceCell(cellID, new)` / `moveCell(cellID, toIndex)`
  - `updateAccessory(target, accessory?)`: Root H/F / Section H/F の更新を統一表現
  - `updateTheme(Theme)`: Theme 差分更新
- 新規型 `AccessoryTarget` を追加（Swift `enum` / Kotlin `sealed interface`）
  - `.rootHeader` / `.rootFooter`: Root レベルの H/F
  - `.sectionHeader(sectionID)` / `.sectionFooter(sectionID)`: Section レベルの H/F
- 新規型 `SettingsAccessory` を追加（Swift `enum` / Kotlin `sealed interface`）
  - `Root H/F` と `Section H/F` で異なる型 (`RootAccessory` / `SectionAccessory`) を `updateAccessory` 1 つの Diff ケースで扱うための統一ラッパ
  - `.root(RootAccessory)` / `.section(SectionAccessory)` の 2 ケース
- `RootAccessory` / `SectionAccessory` 型自体は維持（区別を保持）
- `Hashable` / `equals` 契約に `SettingsRootDiff` / `AccessoryTarget` を追加（Swift `Hashable`、Kotlin `data class`）

## Capabilities

### New Capabilities
（なし。本提案は既存 capability の変更のみ）

### Modified Capabilities
- `settings-view-core`: `SettingsRoot` から `header` / `footer` 削除、`SettingsRootDiff` / `AccessoryTarget` / `SettingsAccessory` を新規追加し、Diff 駆動の部分更新を支える Core 層の型契約を確立

## Impact

- 影響範囲：
  - 既存 `ios/Sources/KsSettingsViewCore/SettingsRoot.swift` の `header` / `footer` プロパティ削除
  - 既存 `android/ks-settingsview-core/src/main/kotlin/jp/kamusoft/kssettingsview/core/SettingsRoot.kt` の `header` / `footer` プロパティ削除
  - 新規 `SettingsRootDiff.swift` / `SettingsRootDiff.kt`
  - 新規 `AccessoryTarget.swift` / `AccessoryTarget.kt`
  - 新規 `SettingsAccessory.swift` / `SettingsAccessory.kt`
  - 既存テスト `SettingsRootTest` などの修正
- 依存：
  - `add-monorepo-foundation`（archive 済）
  - `add-settings-view-core`（archive 済、MODIFIED 対象）
- 後続：
  - `add-partial-update-native`: 本提案の型を使って Native UI 層の `SettingsRootStore` と `applyDiff` API を実装
  - `add-maui-bridge` / `add-maui-core` 修正: 本提案の Diff 型を Bridge DTO / Handler に反映
- リスク：**高**（破壊的変更）
  - **`SettingsRoot.header/footer` 削除による既存利用コードの破壊**: 利用者は現状おらず（変更提案中）、サンプルコード（`samples-ios` / `samples-android` / 進行中 `add-samples-maui`）は後続提案で修正する前提
  - **Root H/F の View プロパティ化（UI 層責務化）**: UI 層側の API として後続提案 `add-partial-update-native` で `rootHeader` / `rootFooter` View プロパティ、進行中 MAUI 側で `SettingsView.HeaderView` / `FooterView` BindableProperty を導入する設計と整合
  - **Diff API のスコープ漏れ**: 設計時に網羅したケースで対応できないユースケースが後続提案実装中に判明する可能性。発覚時は本提案を archive 取り消し → Diff 型に追加 → 再 archive、または後続提案で Diff 型に追加パッチを当てる運用とする（design.md Open Questions に記載）
