## Why

KsSettingsView の全プラットフォーム（iOS Native、Android Native、MAUI）が共通して扱う設定画面のドメインモデル（`SettingsRoot`、`Section`、`Cell` の抽象、`Theme`、`CellStyle`）を、まず Native 言語ごとに正準ソースとして確立する必要がある。これがないと iOS UI / Android UI / Cell 群 / MAUI バインディングが相互に矛盾した型定義を持つリスクがある。本変更提案は UI 描画やプラットフォーム依存型を含まない、純粋データモデル層のみを定義する。

## What Changes

- iOS Swift モジュール `KsSettingsViewCore` を新設し、以下を定義：
  - `public protocol KsCell: Hashable, Identifiable { var id: UUID { get }; var style: CellStyle { get } }`
  - `public struct AnyCell: Hashable`（型消去ラッパ）
  - `public enum SectionAccessory: Hashable`（`.text(String)` / `.custom(AnyCell)` の sum type）
  - `public struct Section: Hashable, Identifiable`（id・header（`SectionAccessory?`）・footer（`SectionAccessory?`）・cells）
  - `public struct SettingsRoot: Hashable`（sections・theme）
  - `public struct Theme`（separatorColor・cellBackgroundColor・headerTextColor・footerTextColor 等の論理スタイル）
  - `public struct CellStyle`（titleColor・descriptionColor・titleFont・descriptionFont・iconSize・cellHeight 等）
- Android Kotlin モジュール `ks-settingsview-core` を新設し、以下を定義：
  - `sealed interface Cell { val id: String; val style: CellStyle }`
  - `sealed interface SectionAccessory`（`data class Text(val value: String)` / `data class Custom(val cell: Cell)` の sum type）
  - `data class Section(val id: String, val header: SectionAccessory?, val footer: SectionAccessory?, val cells: List<Cell>)`
  - `data class SettingsRoot(val sections: List<Section>, val theme: Theme)`
  - `data class Theme(...)`、`data class CellStyle(...)`
- 両モジュールで「論理スタイル」のみ表現し、UIKit `UIColor` / Android `Color` などのプラットフォーム型は持たない（変換は UI 層の責務）
- 各モジュールにユニットテスト（XCTest / JUnit）を配置：等価性、Hashable、SettingsRoot 構築、空 Section / 空 cells のエッジケース、`SectionAccessory.text` / `.custom` 双方の構築・等価性

## Capabilities

### New Capabilities
- `settings-view-core`: 全プラットフォーム共通のドメインモデル（SettingsRoot / Section / Cell 抽象 / Theme / CellStyle）の振る舞いを規定する

### Modified Capabilities
（なし）

## Impact

- 影響範囲：Native コアモジュールのみ。UI 層・MAUI 層は本変更では一切触らない
- 依存：`add-monorepo-foundation` の完了
- 後続変更が依存：`add-settings-view-ios-ui`、`add-settings-view-android-ui`、`add-cell-types-basic`、`add-cell-types-input`、`add-cell-types-custom`、`add-maui-bindings`
- リスク：低。データモデルのみで、振る舞いを伴わない
