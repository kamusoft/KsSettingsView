# Verification Report: add-partial-update-core

生成日: 2026-05-13

## Summary

| Dimension    | Status                                       |
|--------------|----------------------------------------------|
| Completeness | 80/80 tasks, 5 requirements all covered      |
| Correctness  | 全 Requirement 実装確認済み、全 Scenario カバー済み |
| Coherence    | 設計決定 7 件すべて遵守                          |

---

## CRITICAL

なし

---

## WARNING

なし

---

## SUGGESTION

なし

---

## 詳細

### Completeness

**タスク完了状況**: 80/80 チェックボックスすべて `[x]`

**Spec カバレッジ**（delta spec: `specs/settings-view-core/spec.md`）:

| Requirement | 実装ファイル | 状態 |
|---|---|---|
| SettingsRoot ドメインモデル（header/footer 削除） | `ios/Sources/KsSettingsViewCore/SettingsRoot.swift`, `android/.../SettingsRoot.kt` | OK |
| Hashable / equals 契約 | 上記 + 各新規型ファイル | OK |
| SettingsRootDiff 型 | `ios/Sources/KsSettingsViewCore/SettingsRootDiff.swift`, `android/.../SettingsRootDiff.kt` | OK |
| AccessoryTarget 型 | `ios/Sources/KsSettingsViewCore/AccessoryTarget.swift`, `android/.../AccessoryTarget.kt` | OK |
| SettingsAccessory 型 | `ios/Sources/KsSettingsViewCore/SettingsAccessory.swift`, `android/.../SettingsAccessory.kt` | OK |
| SettingsRootDiff のユニットテスト | `ios/Tests/KsSettingsViewCoreTests/SettingsRootDiffTests.swift`, `android/.../SettingsRootDiffTest.kt` | OK |

---

### Correctness

**Requirement 別 実装確認**:

- **SettingsRoot ドメインモデル**: `SettingsRoot.swift` は `sections` / `theme` のみを保持し `header` / `footer` は存在しない。Hashable 手動実装で `sections` + `theme` のみを使用。Kotlin 側は `data class SettingsRoot(val sections: List<Section>, val theme: Theme)` で同様。Core モジュール内に旧 `header` / `footer` 参照なし（grep 確認）。

- **SettingsRootDiff 型（Swift）**: `public enum SettingsRootDiff: Hashable, Sendable` として定義、11 ケース全実装済み。`insertCell` / `replaceCell` では `any KsCell` の existential 制約により手動 `Hashable` 実装を実施（`AnyHashable` 経由）。spec に定義された 11 ケースすべてが実装されている。

- **SettingsRootDiff 型（Kotlin）**: `sealed interface SettingsRootDiff`、各ケースは `data class` として 11 ケース全実装済み。

- **AccessoryTarget 型（Swift）**: `public enum AccessoryTarget: Hashable, Sendable`。`rootHeader` / `rootFooter` / `sectionHeader(sectionID: UUID)` / `sectionFooter(sectionID: UUID)` の 4 ケース全実装。

- **AccessoryTarget 型（Kotlin）**: `sealed interface AccessoryTarget`、`data object RootHeader` / `data object RootFooter` / `data class SectionHeader(val sectionId: String)` / `data class SectionFooter(val sectionId: String)` の 4 サブタイプ全実装。

- **SettingsAccessory 型（Swift）**: `public enum SettingsAccessory: Hashable, Sendable` に `case root(RootAccessory)` / `case section(SectionAccessory)` の 2 ケース実装済み。

- **SettingsAccessory 型（Kotlin）**: `sealed interface SettingsAccessory` に `data class Root(val accessory: RootAccessory)` / `data class Section(val accessory: SectionAccessory)` の 2 サブタイプ実装済み。

**Scenario カバレッジ**:

spec.md の全 Scenario に対応するテストが存在することを確認:

- SettingsRoot の構築 → `SettingsRootTests.swift:test_構築_sections_と_theme_を保持する` / `SettingsRootTest.kt:build_holds_sections_and_theme`
- SettingsRoot の等価性（同一/異なる） → `SettingsRootTests.swift` / `SettingsRootTest.kt` に複数テスト
- SettingsRootDiff の全 11 ケース生成・payload 取り出し → `SettingsRootDiffTests.swift` / `SettingsRootDiffTest.kt`
- SettingsRootDiff の等価性 → 同上テストファイルに等価性テスト群
- AccessoryTarget の全 4 ケース等価性・hashValue → `AccessoryTargetTests.swift` / `AccessoryTargetTest.kt`
- SettingsAccessory の等価性・ケース別判定・view ケース中身無視 → `SettingsAccessoryTests.swift` / `SettingsAccessoryTest.kt`
- updateAccessory による Root H/F 更新（text） → `test_updateAccessory_RootHeader_text_生成と_payload_取り出し` / `updateAccessory_root_header_text`
- updateAccessory による Section H/F 削除（nil） → `test_updateAccessory_SectionHeader_nil_削除を表現できる` / `updateAccessory_section_header_null`
- moveCell の生成（Section 内移動） → `test_moveCell_生成と_payload_取り出し` / `moveCell_construct_and_extract`
- KsAnyView 中身違いは等価 → `SettingsAccessoryTests.swift:test_root_view_ケースは中身無視で等価` / `test_section_view_ケースは中身無視で等価`

---

### Coherence

**設計決定の遵守確認**:

| Decision | 内容 | 実装の遵守状況 |
|---|---|---|
| Decision 1 | SettingsRoot から header/footer を削除し UI 層責務化 | 遵守。SettingsRoot に header/footer なし |
| Decision 2 | SettingsRootDiff は Swift enum / Kotlin sealed interface | 遵守。各言語の慣用的定義に従っている |
| Decision 3 | updateAccessory は target 引数で Root/Section を統一表現 | 遵守。`updateAccessory(target: AccessoryTarget, accessory: SettingsAccessory?)` の 1 ケース |
| Decision 4 | RootAccessory / SectionAccessory を区別保持 | 遵守。SettingsAccessory はラッパのみで既存型を統合・置換していない |
| Decision 5 | moveCell は toIndex のみ指定（Section 内のみ） | 遵守。Swift `case moveCell(cellID: KsCellID, to: Int)` / Kotlin `data class MoveCell(val cellId: String, val toIndex: Int)` |
| Decision 6 | moveSection は from / to の両方を Int で指定 | 遵守。Swift `case moveSection(from: Int, to: Int)` / Kotlin `data class MoveSection(val from: Int, val to: Int)` |
| Decision 7 | Diff の Hashable / equals 契約 | 遵守。Swift は手動 Hashable 実装（`AnyHashable` 経由）、Kotlin は data class による自動 equals/hashCode |

**iOS / Android ケース構成の一致確認**:

- `SettingsRootDiff`: 11 ケース一致（full/insertSection/removeSection/moveSection/replaceSection/insertCell/removeCell/replaceCell/moveCell/updateAccessory/updateTheme）
- `AccessoryTarget`: 4 ケース一致（rootHeader/rootFooter/sectionHeader/sectionFooter）
- `SettingsAccessory`: 2 ケース一致（root/section）

---

## Final Assessment

CRITICAL なし / WARNING なし / SUGGESTION なし。

すべてのチェックが合格しました。アーカイブ準備が整っています。

**判定: VALID**
