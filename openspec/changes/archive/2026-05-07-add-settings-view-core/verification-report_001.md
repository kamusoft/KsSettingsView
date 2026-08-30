## Verification Report: add-settings-view-core（Decision 5b 追加後）

> 本レポートは Decision 5b（SectionAccessory による header/footer の sum type 化）追加実装後の状態を検証したものである。
> 以前のレポート（`verification-report.md`）は Section 8 タスク（SectionAccessory 化）追加前の内容であり、本レポートに差し替える。

### Summary

| Dimension    | Status                                          |
|--------------|-------------------------------------------------|
| Completeness | 45/45 tasks complete（Section 1〜8 全完了）、8 requirements 全カバー |
| Correctness  | 8/8 requirements 実装済み、17/17 scenarios 充足 |
| Coherence    | Decision 1〜5b 全て遵守、コードパターン一貫 |

---

## Completeness

### Task Completion

tasks.md の全チェックボックスを確認。Section 1〜8 全タスクが `[x]` 完了状態。

| Section | 内容 | 完了数 |
|---------|------|--------|
| 1 | iOS Core モジュール初期設定 | 3/3 |
| 2 | iOS Core 型定義 | 8/8 |
| 3 | iOS Core ユニットテスト | 6/6 |
| 4 | Android Core モジュール初期設定 | 4/4 |
| 5 | Android Core 型定義 | 7/7 |
| 6 | Android Core ユニットテスト | 5/5 |
| 7 | ドキュメント | 1/1 |
| 8 | SectionAccessory 化（再修正） | 11/11 |

**合計: 45/45 タスク完了**

### Spec Coverage

spec.md に記載された 8 つの Requirement すべてについて実装ファイルの存在を確認した。

| Requirement | iOS 実装 | Android 実装 |
|-------------|---------|-------------|
| SettingsRoot ドメインモデル | `SettingsRoot.swift` | `SettingsRoot.kt` |
| Section ドメインモデル | `Section.swift`（`SectionAccessory?` 型） | `Section.kt`（`SectionAccessory?` 型） |
| SectionAccessory 型 | `SectionAccessory.swift` | `SectionAccessory.kt` |
| Cell 抽象 | `KsCell.swift` | `Cell.kt` |
| AnyCell 型消去（iOS） | `AnyCell.swift` | 該当なし（Android は不要） |
| Theme 型 | `Theme.swift` | `Theme.kt` |
| CellStyle 型 | `CellStyle.swift` | `CellStyle.kt` |
| Hashable / equals 契約 | Swift struct により自動 | Kotlin data class により自動 |

---

## Correctness

### Requirement 実装マッピング

**SettingsRoot ドメインモデル**

- iOS: `ios/Sources/KsSettingsViewCore/SettingsRoot.swift` — `public struct SettingsRoot: Hashable, Sendable` で `sections: [Section]` と `theme: Theme` を保持。デフォルト値あり。
- Android: `android/ks-settingsview-core/src/main/kotlin/jp/kamusoft/kssettingsview/core/SettingsRoot.kt` — `data class SettingsRoot(val sections: List<Section> = emptyList(), val theme: Theme = Theme())`
- 仕様の「複数の Section と全体 Theme を保持」「値等価性」を両プラットフォームで充足。

**Section ドメインモデル（Decision 5b 対応済み）**

- iOS: `Section.swift` — `public struct Section: Hashable, Identifiable` で id（UUID）・`header: SectionAccessory?`・`footer: SectionAccessory?`・`cells: [AnyCell]` を保持。
- Android: `Section.kt` — `data class Section(val id: String, val header: SectionAccessory? = null, val footer: SectionAccessory? = null, val cells: List<Cell> = emptyList())`
- 仕様の「文字列のみならず任意 Cell も格納できる `SectionAccessory?`」を両プラットフォームで充足。

**SectionAccessory 型（Decision 5b）**

- iOS: `SectionAccessory.swift` — `public enum SectionAccessory: Hashable, Sendable { case text(String); case custom(AnyCell) }` — 仕様と完全一致。
- Android: `SectionAccessory.kt` — `sealed interface SectionAccessory { data class Text(val value: String) : SectionAccessory; data class Custom(val cell: Cell) : SectionAccessory }` — 仕様と完全一致。

**Cell 抽象**

- iOS: `KsCell.swift` — `public protocol KsCell: Hashable, Identifiable where ID == UUID { var id: UUID { get }; var style: CellStyle { get } }` — 仕様と完全一致。
- Android: `Cell.kt` — `sealed interface Cell { val id: String; val style: CellStyle }` — 仕様と完全一致。

**AnyCell 型消去（iOS）**

- iOS: `AnyCell.swift` — `public struct AnyCell: Hashable, Identifiable, Sendable` — 内部に `AnyHashable` を保持し、`unwrap(as:)` で具象型取り出し可能。
- 仕様の「Hashable、異種 Cell をコレクション格納可能」を充足。

**Theme 型**

- iOS / Android 両方で仕様必須フィールド 8 個（`separatorColor`・`cellBackgroundColor`・`selectedColor`・`headerTextColor`・`headerBackgroundColor`・`footerTextColor`・`footerBackgroundColor`・`scrollIndicatorVisible`）を完全実装。
- 全フィールドが `KsColor` / `Bool`（Android: `Boolean`）のプラットフォーム非依存型で定義されており、`UIColor` / `android.graphics.Color` を含まない。
- デフォルト値あり（旧 AiForms.Maui.SettingsView 標準スタイルに準拠）。

**CellStyle 型**

- iOS / Android 両方で仕様必須フィールド 9 個（`titleColor`・`titleFont`・`descriptionColor`・`descriptionFont`・`iconSize`・`iconRadius`・`cellHeight`・`hintTextColor`・`hintTextFont`）を完全実装。
- 全フィールドが Optional / nullable（デフォルトは nil / null）であり、「未指定 = Theme から継承」の構造を正しく表現。

**Hashable / equals 契約**

- iOS: 全型が `struct` または `enum` であり Swift が Hashable を自動合成（`SectionAccessory` は `Hashable` を明示宣言）。
- Android: 全型が `data class` であり equals / hashCode が自動実装。`sealed interface SectionAccessory` の各サブタイプも `data class` であり契約を満たす。

### Scenario Coverage

spec.md の全 17 シナリオ（SectionAccessory 関連 3 シナリオを含む）についてテストの存在を確認した。

| Scenario | iOS テスト | Android テスト |
|----------|-----------|--------------|
| SettingsRoot の構築 | `SettingsRootTests.swift:test_構築_sections_と_theme_を保持する` | `SettingsRootTest.kt:build_holds_sections_and_theme` |
| 等価性（SettingsRoot） | `SettingsRootTests.swift:test_等価性_同フィールドのインスタンスは等しい` | `SettingsRootTest.kt:equality_same_fields_are_equal` |
| Section の構築（文字列ヘッダ） | `SectionTests.swift:test_構築_文字列ヘッダで全フィールドを保持する` | `SectionTest.kt:build_with_text_header_holds_all_fields` |
| Section の構築（カスタムヘッダ） | `SectionTests.swift:test_構築_カスタムヘッダで元の_Cell_を取り出せる` | `SectionTest.kt:build_with_custom_header_returns_original_cell` |
| 空セクション | `SectionTests.swift:test_空_cells_で構築でき_isEmpty_が真` | `SectionTest.kt:empty_cells_can_be_built` |
| Swift enum 定義（SectionAccessory） | `SectionAccessoryTests.swift:test_text_ケースの構築とケース別取り出し` | N/A |
| Kotlin sealed interface 定義（SectionAccessory） | N/A | `SectionAccessoryTest.kt:text_build_and_extract` |
| 等価性（SectionAccessory） | `SectionAccessoryTests.swift:test_text_等価性_同じ文字列は等しい` / `test_custom_等価性_同じ_Cell_なら等しい` / `test_text_と_custom_は別ケースとして区別される` | `SectionAccessoryTest.kt:text_equality_*` / `custom_equality_*` / `text_and_custom_are_distinct` |
| Swift プロトコル定義（KsCell） | `DummyCells.swift` でコンパイル時検証 | N/A |
| Kotlin sealed 抽象（Cell） | N/A | `SectionTest.kt:heterogeneous_cells_can_be_stored` |
| 異種 Cell の格納（AnyCell） | `AnyCellTests.swift:test_異種_Cell_を同コレクションに格納できる` | N/A（Android は `sealed interface` で不要） |
| Theme のデフォルト値 | `ThemeTests.swift:test_デフォルト値_引数なしで構築できる` | `ThemeTest.kt:default_values_can_be_built_with_no_args` |
| プラットフォーム型を持たない | 型定義上 `UIColor` 等を含まない（コンパイル時保証） | `android.graphics` 等を含まない（コンパイル時保証） |
| CellStyle のデフォルト値 | `CellStyleTests.swift:test_デフォルト値_全フィールドが_nil` | `CellStyleTest.kt:default_all_fields_are_null` |
| Theme との継承関係（nullable） | `CellStyleTests.swift` で Optional フィールドを検証 | `CellStyleTest.kt` で nullable フィールドを検証 |
| 同一フィールドのインスタンスは等しい | 各テストファイルの等価性テスト | 各テストファイルの等価性テスト |
| フィールド変更後は等しくない | 各テストファイルの不等価テスト | 各テストファイルの不等価テスト |

---

## Coherence

### Design Adherence

design.md の Decision 1〜5b 全てを実装との照合で検証した。

**Decision 1: 値型ファースト**

- iOS: 全型が `struct`（`SettingsRoot`・`Section`・`AnyCell`・`Theme`・`CellStyle`・`KsColor`・`KsFont`）または `enum`（`SectionAccessory`・`KsFontWeight`）
- Android: 全型が `data class`（`SectionAccessory` サブタイプ含む）または `enum class`（`KsFontWeight`）
- 準拠: 完全に従っている。

**Decision 2: Cell 抽象は protocol / sealed interface**

- iOS: `public protocol KsCell: Hashable, Identifiable`
- Android: `sealed interface Cell`
- 準拠: 完全に従っている。

**Decision 3: AnyCell 型消去（iOS のみ）**

- iOS: `AnyCell` を `Section.cells: [AnyCell]` として実装。内部 `AnyHashable` で等価性・ハッシュを委譲。`unwrap(as:)` で具象型取り出し可能。
- `SectionAccessory.custom(AnyCell)` にも AnyCell を採用。
- Android: `sealed interface` のため AnyCell 相当は実装なし。
- 準拠: 完全に従っている。

**Decision 4: Theme と CellStyle の継承**

- `CellStyle` の全フィールドが Optional / nullable で定義。合成ロジックは Core に含まれない。
- 準拠: 完全に従っている。

**Decision 5b: SectionAccessory による header/footer の sum type 化**

- iOS: `Section.header` / `Section.footer` が `SectionAccessory?` 型に変更済み。`SectionAccessory` は `case text(String)` / `case custom(AnyCell)` の 2 ケース。
- Android: `Section.header` / `Section.footer` が `SectionAccessory?` 型に変更済み。`SectionAccessory` は `Text` / `Custom` の 2 サブタイプ。
- `docs/core-model.md` の Section 説明が `SectionAccessory?` 型で更新され、`SectionAccessory` 専用章を含む。
- 準拠: 完全に従っている。

**Decision 5: 論理スタイル型 KsColor / KsFont**

- `KsColor`: iOS `struct KsColor(red: Double, green: Double, blue: Double, alpha: Double)`、Android `data class KsColor(val red: Double, val green: Double, val blue: Double, val alpha: Double = 1.0)`
- `KsFont`: iOS `struct KsFont(family: String?, size: Double, weight: KsFontWeight)`、Android `data class KsFont(...)`
- 準拠: 完全に従っている。

### Code Pattern Consistency

- iOS: `Sources/` と `Tests/` のディレクトリ構成、ファイル命名（PascalCase.swift）は SwiftPM 慣習に沿っている。
- Android: `src/main/kotlin/` と `src/test/kotlin/` のディレクトリ構成、パッケージ ID `jp.kamusoft.kssettingsview.core` は Gradle + Kotlin 慣習に沿っている。
- `docs/core-model.md` に `SectionAccessory` の章（`### SectionAccessory`）が追加されており、task 8.11 の完了条件を満たす。

---

## テスト実行結果

### iOS: `swift test`

実行日: 2026-05-07

```
Test Suite 'All tests' passed
  AnyCellTests:         5 tests, 0 failures
  CellStyleTests:       5 tests, 0 failures
  SectionAccessoryTests: 8 tests, 0 failures
  SectionTests:         6 tests, 0 failures
  SettingsRootTests:    4 tests, 0 failures
  ThemeTests:           4 tests, 0 failures
合計: 32 tests, 0 failures
```

### Android: `./gradlew :ks-settingsview-core:test`

実行日: 2026-05-07

```
CellStyleTest:         5 tests, 0 failures
SectionAccessoryTest:  8 tests, 0 failures
SectionTest:           7 tests, 0 failures
SettingsRootTest:      4 tests, 0 failures
ThemeTest:             4 tests, 0 failures
合計: 28 tests, 0 failures
BUILD SUCCESSFUL
```

### OpenSpec Validation: `openspec validate add-settings-view-core --strict`

```
Change 'add-settings-view-core' is valid
```

---

## 完了条件チェック

| 条件 | 結果 |
|------|------|
| 全タスクのチェックボックスが完了（Section 1〜8 全 45 タスク） | 完了 |
| `settings-view-core` capability の全 Scenario が通る | 通過（17/17 Scenario） |
| iOS ユニットテストが全成功（`swift test`） | 成功（32/32 tests） |
| Android ユニットテストが全成功（`./gradlew :ks-settingsview-core:test`） | 成功（28/28 tests） |
| `docs/core-model.md` が存在し `SectionAccessory` 章を含む | 存在・含む |
| `openspec validate add-settings-view-core --strict` がグリーン | グリーン |

---

## Issues

CRITICAL: なし

WARNING: なし

SUGGESTION: なし

---

## Final Assessment

すべてのチェックが通過した。Decision 5b（SectionAccessory による header/footer の sum type 化）を含む全実装が仕様・設計と完全に一致している。CRITICAL・WARNING・SUGGESTION のいずれも検出されなかった。アーカイブ可能な状態である。
