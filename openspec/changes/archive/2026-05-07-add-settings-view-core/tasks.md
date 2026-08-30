## 1. iOS Core モジュール初期設定

- [x] 1.1 `ios/Sources/KsSettingsViewCore/` ディレクトリを作成
- [x] 1.2 `ios/Package.swift` に `KsSettingsViewCore` ターゲットを追加（platforms iOS 16+）
- [x] 1.3 `ios/Tests/KsSettingsViewCoreTests/` ディレクトリを作成し、`Package.swift` にテストターゲットを追加

## 2. iOS Core 型定義

- [x] 2.1 `KsColor.swift` を作成し、`public struct KsColor` を RGBA Double で実装
- [x] 2.2 `KsFont.swift` を作成し、`public struct KsFont` および `public enum KsFontWeight` を実装
- [x] 2.3 `Theme.swift` を作成し、`public struct Theme: Hashable` を仕様の最低フィールドで実装、デフォルト値を持つ
- [x] 2.4 `CellStyle.swift` を作成し、`public struct CellStyle: Hashable` を実装（フィールドは Optional）
- [x] 2.5 `KsCell.swift` を作成し、`public protocol KsCell: Hashable, Identifiable` を実装
- [x] 2.6 `AnyCell.swift` を作成し、`public struct AnyCell: Hashable` 型消去ラッパを実装
- [x] 2.7 `Section.swift` を作成し、`public struct Section: Hashable, Identifiable` を実装
- [x] 2.8 `SettingsRoot.swift` を作成し、`public struct SettingsRoot: Hashable` を実装

## 3. iOS Core ユニットテスト

- [x] 3.1 `SettingsRootTests.swift` で構築・等価性・空 sections の Scenario をテスト
- [x] 3.2 `SectionTests.swift` で構築・空 cells・等価性をテスト
- [x] 3.3 `AnyCellTests.swift` で異種 Cell 格納・キャスト復元をテスト（テスト用ダミー Cell を使用）
- [x] 3.4 `ThemeTests.swift` で デフォルト値・等価性・Hashable をテスト
- [x] 3.5 `CellStyleTests.swift` で デフォルト値・Optional フィールド・等価性をテスト
- [x] 3.6 `swift test` でテスト全成功を確認

## 4. Android Core モジュール初期設定

- [x] 4.1 `android/ks-settingsview-core/` を Gradle サブプロジェクトとして作成し、`android/settings.gradle.kts` の `include(...)` に追加
- [x] 4.2 `android/ks-settingsview-core/build.gradle.kts` を作成（kotlin("jvm") もしくは Android library、minSdk 29、JDK 17）
- [x] 4.3 `src/main/kotlin/jp/kamusoft/kssettingsview/core/` パッケージを作成
- [x] 4.4 `src/test/kotlin/jp/kamusoft/kssettingsview/core/` を作成し、JUnit 5 を依存に追加

## 5. Android Core 型定義

- [x] 5.1 `KsColor.kt` を作成し、`data class KsColor(val red: Double, val green: Double, val blue: Double, val alpha: Double)` を実装
- [x] 5.2 `KsFont.kt` を作成し、`data class KsFont` および `enum class KsFontWeight` を実装
- [x] 5.3 `Theme.kt` を作成し、`data class Theme(...)` を仕様の最低フィールドで実装、デフォルト値を持つ
- [x] 5.4 `CellStyle.kt` を作成し、`data class CellStyle(...)` を実装（フィールドは nullable）
- [x] 5.5 `Cell.kt` を作成し、`sealed interface Cell` を実装
- [x] 5.6 `Section.kt` を作成し、`data class Section(val id: String, val header: String?, val footer: String?, val cells: List<Cell>)` を実装
- [x] 5.7 `SettingsRoot.kt` を作成し、`data class SettingsRoot(val sections: List<Section>, val theme: Theme)` を実装

## 6. Android Core ユニットテスト

- [x] 6.1 `SettingsRootTest.kt` で構築・等価性・空 sections の Scenario をテスト
- [x] 6.2 `SectionTest.kt` で構築・空 cells・等価性をテスト
- [x] 6.3 `ThemeTest.kt` でデフォルト値・等価性をテスト
- [x] 6.4 `CellStyleTest.kt` でデフォルト値・nullable フィールド・等価性をテスト
- [x] 6.5 `./gradlew :ks-settingsview-core:test` でテスト全成功を確認

## 7. ドキュメント

- [x] 7.1 `docs/core-model.md` を作成し、Core 型のフィールド一覧と論理スタイル → プラットフォーム型の変換ルール概要を記載

## 8. SectionAccessory 化（再修正）

> 本セクションは Decision 5b 追加に伴う既存実装の改修タスクである。アーカイブ前に対応する。

- [x] 8.1 iOS: `SectionAccessory.swift` を新規作成し `public enum SectionAccessory: Hashable { case text(String); case custom(AnyCell) }` を実装
- [x] 8.2 iOS: `Section.swift` の `header: String?` / `footer: String?` を `header: SectionAccessory?` / `footer: SectionAccessory?` に変更
- [x] 8.3 iOS: `SettingsRootTests.swift` / `SectionTests.swift` を `SectionAccessory.text(...)` ベースに書き換え、`.custom(...)` ケースの構築・等価性テストを追加
- [x] 8.4 iOS: `SectionAccessoryTests.swift` を新規作成し、`text` / `custom` の等価性、Hashable、ケース別取り出しを検証
- [x] 8.5 iOS: `swift test` でテスト全成功を確認
- [x] 8.6 Android: `SectionAccessory.kt` を新規作成し `sealed interface SectionAccessory { data class Text(val value: String) : SectionAccessory; data class Custom(val cell: Cell) : SectionAccessory }` を実装
- [x] 8.7 Android: `Section.kt` の `header: String?` / `footer: String?` を `header: SectionAccessory?` / `footer: SectionAccessory?` に変更
- [x] 8.8 Android: `SectionTest.kt` を `SectionAccessory.Text(...)` ベースに書き換え、`Custom(...)` ケースの構築・等価性テストを追加
- [x] 8.9 Android: `SectionAccessoryTest.kt` を新規作成
- [x] 8.10 Android: `./gradlew :ks-settingsview-core:test` で全成功を確認
- [x] 8.11 `docs/core-model.md` の Section 説明を更新し、`SectionAccessory` の章を追加

## 依存関係

- 先行：`add-monorepo-foundation`
- 後続：`add-settings-view-ios-ui`、`add-settings-view-android-ui`、`add-cell-types-basic`、`add-cell-types-input`、`add-cell-types-custom`、`add-maui-bindings`

## 完了条件

- 全タスクのチェックボックスが完了している
- `settings-view-core` capability の全 Scenario が通る
- iOS / Android 両モジュールでユニットテストが全成功する
- `docs/core-model.md` が存在する
