## Why

現在の `KsSettingsViewCore` は「プラットフォーム非依存の論理表現を保持する」という建前のもとに `KsColor` / `KsFont` を独自の Double-based RGBA / フォント値型として定義し、利用者は `Theme(separatorColor: KsColor(red: 0xE6/255.0, green: 0xDA/255.0, blue: 0xB9/255.0, alpha: 1.0), ...)` のように Native 開発者にとって冗長で慣れない形で記述する必要がある。一方で `KsImage` は既に `case uiImage(UIImage)` / `class Drawable(android.graphics.drawable.Drawable)` として **プラットフォーム型を直接保持しており、Core の「論理表現のみ」契約は実態として破綻している**。

また iOS Core (Swift Package) と Android Core (`com.android.library` + Compose Runtime 依存) は同名の概念を保持してはいるが、コードを共有する仕組みは存在せず、物理的にも論理的にも別モジュールである。Theme / CellStyle / 色 / フォント / 画像のような **スタイル系の値は UI 層の関心事であり、Core に置く理由がない**。さらに後続提案 (`add-maui-bridge` / `add-maui-cells` / `add-maui-core` / `add-samples-maui`) はまだ未実装で、Bridge 経由で MAUI 側に KsColor を露出させない設計を取っているため、本変更を **MAUI 提案実装前に行うことで整合性を最初から取れる**。

## What Changes

### Core 純化

- **BREAKING** `KsColor` (Swift `struct` / Kotlin `data class`) を `KsSettingsViewCore` / `ks-settingsview-core` から削除する。
- **BREAKING** `KsFont` / `KsFontWeight` を `KsSettingsViewCore` / `ks-settingsview-core` から削除する。
- **BREAKING** `KsImage` (sealed enum / sealed interface) を Core から UI 層 (`KsSettingsViewUI` / `ks-settingsview-ui`) に移動する（型名は維持）。
- **BREAKING** `Theme` (Swift `struct` / Kotlin `data class`) を Core から UI 層に移動する。フィールド型はそれぞれ Native 型 (iOS: `UIColor` / `UIFont`、Android: `androidx.compose.ui.graphics.Color` / `androidx.compose.ui.text.TextStyle`) を直接保持する。
- **BREAKING** `CellStyle` (Swift `struct` / Kotlin `data class`) を Core から UI 層に移動する。フィールド型は Native 型を直接保持する。
- **BREAKING** `SettingsRoot` から `theme: Theme` フィールドを削除する。Theme は View 側の引数 / modifier として渡す形に統一する。
- **BREAKING** `KsCell` プロトコル / `Cell` インターフェースから `style: CellStyle` プロパティ要求を削除する。各具象 Cell が個別に `style` プロパティを持つ。
- **BREAKING** `SettingsRootDiff` から `updateTheme(Theme)` ケースを削除する。Theme 部分更新は UI 層の独立 API (`Store.applyTheme(theme)` 相当) に分離する。

### UI 層への Theme / CellStyle / KsImage 再配置

- iOS UI 層 (`KsSettingsViewUI`) に `Theme` / `CellStyle` / `KsImage` を新規定義する。フィールド型は `UIColor` / `UIFont` / `UIImage` を直接保持する。
- Android UI 層 (`ks-settingsview-ui`) に `Theme` / `CellStyle` / `KsImage` を新規定義する。フィールド型は Compose `Color` / `TextStyle` / `KsImage` を直接保持する（Compose Color は `@JvmInline value class` のため `data class` の `equals`/`hashCode` と整合する）。
- iOS / Android それぞれの UI 層に「Theme を View に渡す API」を整備する（iOS: 既存 `.theme(_:)` modifier を活用、Android: 既存 `KsSettingsView(theme = ...)` 引数を活用）。
- iOS / Android それぞれの UI 層に「Theme 部分更新 API」(`SettingsRootStore.applyTheme(_:)` / `applyTheme`) を新設する。

### 既存 theme-bridge 層の縮小

- **BREAKING** `KsColor` → `UIColor` 変換ユーティリティ (`UIColor.init(ksColor:)`)、`KsColor.toColorInt()` 拡張関数を削除する。
- `settings-view-{ios,android}-theme-bridge` capability は「実効スタイル合成（`CellStyle` → `Theme` → プラットフォーム fallback の 3 段階）」「タッチフィードバック」「`isEnabled` 描画」「`ButtonCell` の baseColor 解決」「`KsImage` 派生のアイコン解決」の責務に縮小する（変換ロジックは不要になる）。

### 各 Cell API の Native 化

- **BREAKING** `SwitchCell.accentColor` / `CheckboxCell.accentColor` / `ButtonCell.titleColor` の型を `KsColor?` から Native 型 (`UIColor?` / Compose `Color?`) に変更する。
- **BREAKING** `LabelCell.icon` / `CommandCell.icon` の型は `KsImage?` のまま（UI 層に再配置された `KsImage` を参照）。

### MAUI Bridge spec の整合（依頼ベース）

OpenSpec 規約により、本提案では他の active 変更提案 (`add-maui-bridge` / `add-maui-cells` / `add-maui-core` / `add-samples-maui`) のアーティファクトを直接変更しない。代わりに、本提案完了時に以下の修正を各担当者に **依頼** する形で整合性確保を担保する：

- 後続未実装提案 `add-maui-bridge` の `KsSettingsRootDiffUpdateThemeDTO(theme: KsThemeDTO)` を `KsSettingsRootDiffDTO` 階層から取り除き、`setTheme(_ theme: KsThemeDTO)` 単独 API として整理する（既に独立 API として記載されているため重複を解消する依頼）。
- `KsThemeDTO` の payload 定義を Native 新 `Theme`（UIColor / Compose Color 直接保持）と整合させる依頼。`KsColorDTO` のような独自 Color 型は導入しない方針（MAUI 側 `Microsoft.Maui.Graphics.Color` を Bridge 内で Native 型に直接変換する設計を `add-maui-bridge` 担当者と合意する）。

### サンプル更新

- iOS Sample (`BasicCellsDemoView.swift`) の MAUI 互換 Theme 定義を `KsColor(red: ..., green: ..., blue: ..., alpha: ...)` から `UIColor(red: ..., green: ..., blue: ..., alpha: ...)` に書き換える。
- Android Sample (`BasicCellsDemoScreen.kt`) の MAUI 互換 Theme 定義を `KsColor(...)` から `Color(0xFF...)` (Compose) に書き換える。

## Capabilities

### New Capabilities

新規 capability は導入しない。Theme / CellStyle / KsImage は既存 `settings-view-{ios,android}-style` capability に統合する。

### Modified Capabilities

- `settings-view-core`: `KsColor` / `KsFont` / `KsImage` / `Theme` / `CellStyle` の Requirement を削除。`SettingsRoot` / `Cell` 抽象 / `SettingsRootDiff` の Requirement を Theme/CellStyle 依存除去のために修正。Hashable 契約の対象から `Theme` / `CellStyle` を除外。
- `settings-view-ios-style`: `Theme` / `CellStyle` / `KsImage` の定義を新規 Requirement として追加。フィールド型は `UIColor` / `UIFont` / `UIImage` を直接保持する旨を明記。
- `settings-view-ios-theme-bridge`: 「Theme / CellStyle の UIKit 変換」Requirement から `KsColor` → `UIColor` 変換 Scenario を削除。実効スタイル合成 Scenario は Theme/CellStyle が UIColor/UIFont を直接保持する前提に書き換え。
- `settings-view-ios-host`: `SettingsRoot.theme` 依存除去（Theme は別経路で受ける）、Theme 部分更新 API (`SettingsRootStore.applyTheme(_:)`) を追加。
- `settings-view-ios-swiftui`: `KsSettingsView` への Theme 引き渡し経路を整理（`.theme(_:)` modifier）、`CellModifiers.titleColor(_:)` / `backgroundColor(_:)` の引数型を `KsColor` から `UIColor` に変更。
- `settings-view-android-style`: `Theme` / `CellStyle` / `KsImage` の定義を新規 Requirement として追加。フィールド型は Compose `Color` / `TextStyle` / `KsImage` を直接保持する旨を明記。
- `settings-view-android-theme-bridge`: 「Theme / CellStyle の Android 変換」Requirement から `KsColor` → `@ColorInt` 変換 Scenario を削除。実効スタイル合成 Scenario は Theme/CellStyle が Compose Color/TextStyle を直接保持する前提に書き換え。
- `settings-view-android-host`: `SettingsRoot.theme` 依存除去、Theme 部分更新 API (`SettingsRootStore.applyTheme(_:)`) を追加。
- `settings-view-android-compose`: `KsSettingsView` の `theme` 引数経路を整理、`CellModifiers.titleColor(_:)` / `backgroundColor(_:)` の引数型を `KsColor` から Compose `Color` に変更。
- `cell-types-basic`: 各 Cell の `style: CellStyle` プロパティの所属を Core 抽象から個別 Cell に変更。`SwitchCell.accentColor` / `CheckboxCell.accentColor` / `ButtonCell.titleColor` の型を Native 型に変更。`KsImage` Requirement を UI 層所属に修正。
- `samples-ios`: MAUI 互換 Theme 定義の Scenario を `UIColor` 直接構築に書き換え。
- `samples-android`: MAUI 互換 Theme 定義の Scenario を Compose `Color` 直接構築に書き換え。

## Impact

### コード影響範囲

- iOS Core: `KsColor.swift` / `KsFont.swift` / `KsImage.swift` / `Theme.swift` / `CellStyle.swift` / `UIColor+KsColor.swift` 削除・移動、`SettingsRoot.swift` / `KsCell.swift` / `SettingsRootDiff.swift` / `DSLCellIdentity.swift` の修正
- Android Core: `KsColor.kt` / `KsFont.kt` / `KsImage.kt` / `Theme.kt` / `CellStyle.kt` / `KsColorExt.kt` 削除・移動、`SettingsRoot.kt` / `Cell.kt` / `SettingsRootDiff.kt` / `DSLCellIdentity.kt` の修正
- iOS UI: `Theme.swift` / `CellStyle.swift` / `KsImage.swift` 新規追加、各 Cell View (`SwitchCell` / `CheckboxCell` / `ButtonCell` / `LabelCell` / `CommandCell`) の型修正、`EffectiveStyle` / `applyDiff` 経路の修正
- Android UI: `Theme.kt` / `CellStyle.kt` / `KsImage.kt` 新規追加、各 Cell ViewHolder の型修正、`EffectiveStyle` / `applyDiff` 経路の修正
- iOS SwiftUI: `CellModifiers.swift` の型修正、`KsSettingsView` の Theme 受け渡し経路
- Android Compose: `CellModifiers.kt` / `DSLHandles.kt` / `BasicCellDsl.kt` / `DSLNodes.kt` / `DSLDiffCalculator.kt` / `KsSettingsViewComposable.kt` の型修正
- iOS / Android テスト: `ThemeTests` / `CellStyleTests` / `KsImageTests` / `EffectiveStyleTests` / `ApplyDiffTests` / `BasicCellsTests` / `SettingsRootStoreTests` / `SettingsRootDiffTests` の修正・移動
- iOS / Android Sample: `BasicCellsDemoView.swift` / `BasicCellsDemoScreen.kt` の修正

### 後続 active 提案への影響（各担当者への依頼事項）

本提案では他の active 提案アーティファクトを直接変更しない。以下は各担当者への修正依頼事項として記録する：

- `add-maui-bridge`: `KsSettingsRootDiffUpdateThemeDTO` を Diff 階層から外して `setTheme` 単独 API に整理、`KsThemeDTO` の payload を Native 新 Theme 構造に再定義する依頼（spec 段階での修正、まだ実装されていないため大きな手戻りなし）
- `add-maui-cells`: `setTheme` / `addButtonCell(titleColor:)` 等の Color 経路を Native 型ベースに整合させる依頼
- `add-maui-core`: 影響極小（KsColor を直接参照する Requirement なし）。読み取り確認のみ
- `add-samples-maui`: 影響極小（MAUI Color で Theme を構築する設計は維持）。読み取り確認のみ
- `add-cell-types-input` / `add-cell-types-custom`: `Cell` 抽象から `style: CellStyle` プロパティ要求が消える点、各 Cell の Color/Font フィールド型が Native 型になる点を反映する依頼

### 利用者 API 影響

- **BREAKING** iOS / Android Native 利用者は `KsColor(red: ..., green: ..., blue: ..., alpha: ...)` を `UIColor(red: ..., green: ..., blue: ..., alpha: ...)` / `Color(0xFF...)` (Compose) に書き換える必要がある。
- **BREAKING** `SettingsRoot(sections: [...], theme: someTheme)` は `KsSettingsView(root, theme: someTheme)` 形式に変わる（iOS: `.theme(_:)` modifier、Android: `theme = ...` 引数）。
- **BREAKING** カスタム Cell 実装者は `KsCell` / `Cell` 抽象に `style: CellStyle` プロパティを実装する義務が消える（任意で持たせる）。

### Risks

- 影響範囲が広く、Core / UI / SwiftUI / Compose / Sample / テストの **同時改修** が必要。途中状態ではビルドが通らないため、Phase 内では細かい段階的コミットが取れない区間がある。
- iOS / Android で Native 型が異なるため (UIColor vs Compose Color)、両プラットフォームで同じ見た目の Theme を書く際に **「数値ベタ書きで揃える」運用** に頼る必要がある。共通化したい場合は `0xRRGGBB` 形式 hex リテラルで揃えるガイドを README で示す。
- `SettingsRoot.theme` の削除に伴い、既存の `SettingsRootDiff.full(SettingsRoot)` ケースが Theme を含まなくなる。Theme 切替を含む状態遷移をテストしている既存テストは「構造差分 + Theme 適用」の 2 段階に分割される。
- MAUI Bridge spec 修正は後続 active 提案の spec 編集を伴う。実装はまだ存在しないため手戻りは spec のみだが、各 active 提案の整合性を必ず一通り確認する必要がある。
