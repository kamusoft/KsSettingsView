## Verification Report: refactor-accessory-and-root-hf

Date: 2026-05-08

### Summary

| Dimension    | Status                                    |
|--------------|-------------------------------------------|
| Completeness | 28/28 tasks, 4 requirements (ADDED/MODIFIED all covered) |
| Correctness  | 4/4 requirements implemented              |
| Coherence    | Design decisions followed (7/7 decisions) |

---

### Dimension 1: Completeness

**Task Completion**: 28/28 完了 (100%)

全タスクが完了している。

**Spec Coverage** (delta specs: `specs/settings-view-core/spec.md`):

| Requirement | Status |
|---|---|
| KsAnyView 型消去ラッパ（ADDED） | 実装済み |
| RootAccessory 型（ADDED） | 実装済み |
| SettingsRoot ドメインモデル（MODIFIED） | 実装済み |
| Section ドメインモデル（MODIFIED） | 実装済み |
| SectionAccessory 型（MODIFIED） | 実装済み |
| Hashable / equals 契約（MODIFIED） | 実装済み |
| AnyCell 型消去（REMOVED） | 削除済み |

---

### Dimension 2: Correctness

**Requirement: KsAnyView 型消去ラッパ**

- Swift: `ios/Sources/KsSettingsViewCore/KsAnyView.swift` に `public struct KsAnyView: @unchecked Sendable` として定義。`swiftUI` / `uiKit` の二択 backing を `Backing` enum で保持。`Hashable` / `Equatable` に意図的に準拠しない。仕様完全一致。
- Kotlin: `android/ks-settingsview-core/src/main/kotlin/.../KsAnyView.kt` に `sealed interface KsAnyView` として定義。`Compose` / `AndroidView` の二択 backing。`equals` / `hashCode` を独自実装しない。仕様完全一致。
- テスト: `KsAnyViewTests.swift` / `KsAnyViewTest.kt` で構築・非 Hashable 検証済み。

**Requirement: RootAccessory 型**

- Swift: `ios/Sources/KsSettingsViewCore/RootAccessory.swift` に `public enum RootAccessory: Hashable` として定義。`text(String)` / `view(KsAnyView)` の 2 ケース。`Hashable` 手動実装で `view` ケースの中身を判定対象外。仕様完全一致。
- Kotlin: `android/ks-settingsview-core/src/main/kotlin/.../RootAccessory.kt` に `sealed interface RootAccessory` として定義。`data class Text` / `class View` の 2 サブタイプ。`View` は `equals` / `hashCode` 手動実装でクラス一致のみで等価。仕様完全一致。
- `SectionAccessory` との別型保証: コンパイル時・実行時両方で検証済み。
- テスト: `RootAccessoryTests.swift` / `RootAccessoryTest.kt` で全 Scenario 検証済み。

**Requirement: SectionAccessory 型（MODIFIED: cell 概念排除）**

- Swift: `ios/Sources/KsSettingsViewCore/SectionAccessory.swift` に `case text(String)` / `case view(KsAnyView)` の 2 ケース。旧 `.custom(AnyCell)` ケースは完全削除済み。`Hashable` 手動実装で `view` ケース中身を判定対象外。仕様完全一致。
- Kotlin: `android/ks-settingsview-core/src/main/kotlin/.../SectionAccessory.kt` に `data class Text` / `class View(KsAnyView)` の 2 サブタイプ。旧 `Custom(Cell)` ケースは完全削除済み。仕様完全一致。
- テスト: `SectionAccessoryTests.swift` / `SectionAccessoryTest.kt` で全 Scenario 検証済み。

**Requirement: SettingsRoot ドメインモデル（MODIFIED: header / footer 追加）**

- Swift: `ios/Sources/KsSettingsViewCore/SettingsRoot.swift` に `header: RootAccessory?` / `footer: RootAccessory?` を追加。デフォルト値 `nil`。`Hashable` 手動実装で `view` ケース中身を判定対象外。仕様完全一致。
- Kotlin: `android/ks-settingsview-core/src/main/kotlin/.../SettingsRoot.kt` に `header: RootAccessory?` / `footer: RootAccessory?` を追加。デフォルト値 `null`。`equals` / `hashCode` 手動実装で `View` ケース中身を判定対象外。仕様完全一致。
- テスト: `SettingsRootTests.swift` / `SettingsRootTest.kt` で H/F なし同士・text 同士・view 同士・nil/非 nil の全 Scenario 検証済み。

**Requirement: AnyCell 型消去（REMOVED）**

- Swift: `AnyCell.swift` ファイルは存在しない。`SectionAccessory` / `Section` ソースに `AnyCell` の型定義・ケース参照なし。コメント内の参照のみ残存（廃止の経緯説明として正当）。仕様完全一致。
- Kotlin: Android 側には元々 `AnyCell` 概念がなく、変更不要。

**Scenario Coverage 評価**: delta spec に記載された全 Scenario がテストでカバーされている。

---

### Dimension 3: Coherence

**Design Adherence**:

| Decision | 実装状態 |
|---|---|
| Decision 1: Cell 概念排除（`.custom` → `.view`） | SectionAccessory.swift / .kt で `.view(KsAnyView)` に完全置換済み |
| Decision 2: RootAccessory を SectionAccessory と別型として新設 | 別型として定義、相互代入不可 |
| Decision 3: KsAnyView は差分検出に参加しない | Hashable / equals を一切実装せず、各コンテナも手動実装で中身を除外 |
| Decision 4: iOS は `boundarySupplementaryItem` で Root H/F を実装 | 本提案の core スコープ外（ios-ui 提案で実装予定）、delta spec 対象外 |
| Decision 5: Android は `ConcatAdapter` で Root H/F を実装 | 本提案の core スコープ外（android-ui 提案で実装予定）、delta spec 対象外 |
| Decision 6: MAUI 公開 API は BindableProperty のみ | 本提案の core スコープ外（maui-bindings 提案で実装予定）、delta spec 対象外 |
| Decision 7: cell-types-custom から H/F スコープを切り出す | 本提案の探索段階で当該提案を書き換え済み（tasks.md 4.2 で確認済み） |

**Code Pattern Consistency**: iOS / Android 両プラットフォームで命名規則・ファイル構造・コメントスタイルが統一されている。

---

### Issues

**CRITICAL**: なし

**WARNING**: なし

**SUGGESTION**: なし

---

### openspec validate --strict

`openspec validate refactor-accessory-and-root-hf --strict` の実行結果: **PASS**（"Change 'refactor-accessory-and-root-hf' is valid"）

---

### Final Assessment

CRITICAL・WARNING・SUGGESTION いずれも検出されなかった。全チェックが通過した。アーカイブ可能な状態。

**判定: VALID**
