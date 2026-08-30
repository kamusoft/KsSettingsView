# レビュー結果 - add-settings-view-core (Round 3)

**レビュー日時**: 2026年05月07日
**レビュワー**: sdd-reviewer
**変更提案ID**: add-settings-view-core
**ラウンド**: Round 3
**レビュー対象**: Decision 5b（SectionAccessory による header/footer の sum type 化、tasks.md セクション 8）
**前回レビュー**: review-result_001.md (APPROVED) / review-result_002.md (APPROVED)

---

## サマリー

design.md に新たに追加された **Decision 5b** に基づき、`Section.header` / `Section.footer` の型を `String?` から `SectionAccessory?` に変更し、`SectionAccessory` を sum type（iOS は `enum`、Android は `sealed interface`）として導入する追加実装をレビューした。

- **iOS 側**:
  - 新規 `SectionAccessory.swift`（`public enum SectionAccessory: Hashable, Sendable { case text(String); case custom(AnyCell) }`）
  - `Section.swift` の `header` / `footer` を `SectionAccessory?` に変更
  - 新規 `SectionAccessoryTests.swift`（8 ケース）
  - `SectionTests.swift` を `.text(...)` / `.custom(...)` ベースに書き換え、カスタムヘッダ等価性ケースを追加（計 6 ケース）
  - `SettingsRootTests.swift` も `.text("general")` 等に追従
- **Android 側**:
  - 新規 `SectionAccessory.kt`（`sealed interface SectionAccessory { data class Text(val value: String); data class Custom(val cell: Cell) }`）
  - `Section.kt` の `header` / `footer` を `SectionAccessory?` に変更
  - 新規 `SectionAccessoryTest.kt`（8 ケース）
  - `SectionTest.kt` 拡張（計 7 ケース、カスタムヘッダ＋既存の異種 Cell 格納検証を保持）
  - `SettingsRootTest.kt` も `SectionAccessory.Text("General")` 等に追従
- **ドキュメント**:
  - `docs/core-model.md` の Section 章を更新し、`SectionAccessory` 章を新規追加（プラットフォーム別表現と意味論を整理）
- **OpenSpec ドキュメント**:
  - proposal.md / design.md / spec.md / tasks.md は Decision 5b の意図に沿って整合的に更新済み（`SectionAccessory 型` Requirement / Scenario が spec.md に追加され、tasks.md セクション 8 が新設）

### 検証結果

- **iOS テスト**: `swift test` → 32 件すべて成功（`SectionAccessoryTests` 8 件 / `SectionTests` 6 件 / `SettingsRootTests` 4 件 / `AnyCellTests` 5 件 / `CellStyleTests` 5 件 / `ThemeTests` 4 件）
- **Android テスト**: `./gradlew :ks-settingsview-core:test --rerun-tasks` → BUILD SUCCESSFUL、計 28 件成功
  - `SectionAccessoryTest` 8 件 / `SectionTest` 7 件 / `SettingsRootTest` 4 件 / `CellStyleTest` 5 件 / `ThemeTest` 4 件
- **OpenSpec**: `openspec validate add-settings-view-core --strict` → `Change 'add-settings-view-core' is valid`
- **タスク**: tasks.md セクション 8（8.1〜8.11）すべて `[x]`、未実装フラグの誤チェックなし

仕様 (`spec.md`) に追加された `SectionAccessory 型` Requirement の全 Scenario（Swift enum 定義 / Kotlin sealed interface 定義 / 等価性）は実装・テストでカバーされ、`Section ドメインモデル` Requirement の更新分（文字列ヘッダ / カスタムヘッダ Scenario）も両プラットフォームで検証済み。Round 1 / Round 2 で APPROVED された既存実装（KsColor / KsFont / Theme / CellStyle / KsCell / AnyCell）への退行も確認できなかった。

クリティカル / メジャーな指摘なし。Suggestion レベルの所見が 1 件あるが本質的成立を妨げない。

**判定**: ✅ **APPROVED**

---

## 指摘事項

### 🔵 Suggestion 1: `SectionAccessory.custom` の等価性テストが「同一 AnyCell インスタンス」依存

**該当箇所**:
- `ios/Tests/KsSettingsViewCoreTests/SectionAccessoryTests.swift:60-66`（`test_custom_等価性_同じ_Cell_なら等しい`）
- `android/ks-settingsview-core/src/test/kotlin/jp/kamusoft/kssettingsview/core/SectionAccessoryTest.kt:60-67`（`custom_equality_same_cell_are_equal`）

**問題点（提案）**:

iOS / Android とも `custom` ケースの「同じ Cell なら等しい」テストが同一インスタンスの再利用で検証されている。

```swift
// 現状
let anyCell = AnyCell(DummyLabelCell(id: UUID(), title: "h"))
let a: SectionAccessory = .custom(anyCell)
let b: SectionAccessory = .custom(anyCell)
XCTAssertEqual(a, b)
```

これだと `enum SectionAccessory` の Hashable 自動合成が「同じ関連値（同じ `AnyCell`）」に対して等価と判定するかしか検証されておらず、本来重要な「内容（id / style / title）が同じだが別インスタンスの AnyCell を `.custom` で包んだとき等価になるか」（＝ Hashable / equals 契約のフルチェイン）がやや弱い。

実用上は `AnyCellTests` 側で「内容が同じ別インスタンスは等しい」が検証されており、SectionAccessory 側のテストはケース別取り出しと「text と custom の区別」が主目的とも読めるため、機能上の欠陥ではない。

**推奨修正（任意・後続提案で対応可）**:

別インスタンスバージョンを 1 ケース追加するとより堅牢。

```swift
let cellId = UUID()
let a: SectionAccessory = .custom(AnyCell(DummyLabelCell(id: cellId, title: "h")))
let b: SectionAccessory = .custom(AnyCell(DummyLabelCell(id: cellId, title: "h")))
XCTAssertEqual(a, b)
XCTAssertEqual(a.hashValue, b.hashValue)
```

```kotlin
val a = SectionAccessory.Custom(DummyLabelCell(id = "h1", title = "h"))
val b = SectionAccessory.Custom(DummyLabelCell(id = "h1", title = "h"))
assertEquals(a, b)
assertEquals(a.hashCode(), b.hashCode())
```

なお Section レベルの `test_等価性_カスタムヘッダ同士_同じ_Cell_なら等しい` も同様に同一 `headerCell` 再利用パターンのため、Hashable 自動合成のフルチェインがやや弱い検証になっている。同じく Suggestion レベル。

---

## 仕様整合性チェック

| Requirement | Scenario | iOS テスト | Android テスト | 充足 |
| --- | --- | --- | --- | --- |
| SectionAccessory 型 | Swift enum 定義 | `SectionAccessory.swift` 型定義 + `SectionAccessoryTests` | — | ✅ |
| SectionAccessory 型 | Kotlin sealed interface 定義 | — | `SectionAccessory.kt` 型定義 + `SectionAccessoryTest` | ✅ |
| SectionAccessory 型 | 等価性（text / custom 区別、内部値一致） | `test_text_等価性_同じ文字列は等しい` / `test_custom_等価性_同じ_Cell_なら等しい` / `test_text_と_custom_は別ケースとして区別される` | `text_equality_same_value_are_equal` / `custom_equality_same_cell_are_equal` / `text_and_custom_are_distinct` | ✅ |
| Section ドメインモデル | 構築（文字列ヘッダ） | `test_構築_文字列ヘッダで全フィールドを保持する` | `build_with_text_header_holds_all_fields` | ✅ |
| Section ドメインモデル | 構築（カスタムヘッダ） | `test_構築_カスタムヘッダで元の_Cell_を取り出せる` | `build_with_custom_header_returns_original_cell` | ✅ |
| Section ドメインモデル | 空セクション | `test_空_cells_で構築でき_isEmpty_が真` | `empty_cells_can_be_built` | ✅ |
| Hashable / equals 契約 | 同一フィールドのインスタンスは等しい | `test_等価性_同フィールドは等しい`（Section） + Set 重複排除 | `equality_same_fields_are_equal`（Section） + `hashset_distinguishes_cases` | ✅ |
| Hashable / equals 契約 | フィールド変更後は等しくない | `test_等価性_id_が異なれば等しくない` 等 | `equality_different_id_are_not_equal` 等 | ✅ |

spec.md で要求されたすべての Scenario について、iOS / Android のいずれか（あるいは両方）でテスト実装と成功が確認できた。

---

## 観点別レビュー結果

### 1. 正確性・機能性

- iOS の `enum SectionAccessory: Hashable { case text(String); case custom(AnyCell) }` は spec.md "Swift enum 定義" Scenario と完全一致。`Sendable` の追加合成は仕様超過だが、Swift 6 strict concurrency 対応として妥当な追記であり、`AnyCell` も Sendable のため伝播も健全 ✅
- Android の `sealed interface SectionAccessory { data class Text(val value: String) : SectionAccessory; data class Custom(val cell: Cell) : SectionAccessory }` は spec.md "Kotlin sealed interface 定義" Scenario と完全一致 ✅
- `Section.header` / `Section.footer` を `SectionAccessory?` に置換、`nil` / `null` でヘッダ／フッタ非表示の意味論を維持 ✅
- 仕様・タスクを書き換えるような不整合なし

### 2. テスト容易性

- 値型 / data class / sealed interface のため、外部依存なくユニットテスト単独で完結 ✅
- DateTime.Now 等の不純物は皆無

### 3. セキュリティ / パフォーマンス

- 該当なし（純粋データ層）

### 4. 可読性・保守性

- 命名は spec.md / design.md と完全に一致しており文書間で迷子にならない
- Swift 側 `case text(String)` / `case custom(AnyCell)` は `lowerCamelCase`、Kotlin 側 `data class Text` / `data class Custom` は `PascalCase`、それぞれの言語慣習どおり ✅
- KDoc / DocC コメントに spec.md / design.md の章へのリンクが付いており、後続開発者が経緯を追える ✅
- 過剰な抽象化や未使用コードは無し

### 5. 一貫性

- iOS と Android で「text / Text」「custom / Custom」のケース名が論理同型に揃っている ✅
- Section.kt と Section.swift のフィールド構成が論理同型を維持（`header: SectionAccessory? = null` / `header: SectionAccessory? = nil`）✅
- Round 1 / Round 2 で確立した命名規約・ファイル分割パターン（型ごとに 1 ファイル、ファイル冒頭に仕様参照コメント）を踏襲 ✅

### 6. テスト品質

- iOS `SectionAccessoryTests` 8 件、Android `SectionAccessoryTest` 8 件で text / custom 両ケースの構築・取り出し・等価性・区別・HashSet 格納を網羅 ✅
- スタブやスキップなし、コメントで言い訳した実質スキップなし ✅
- `Set<SectionAccessory>` / `setOf<SectionAccessory>` での重複排除検証によって Hashable / equals 契約の実用パスを検証 ✅
- 既存テスト（SectionTests / SettingsRootTests）も型変更に追従し、退行はゼロ
- Suggestion 1 で挙げた「別インスタンス内容一致」検証は弱いが、AnyCellTests 側で間接的にカバーされており致命的ではない

### 7. ドキュメント

- `docs/core-model.md` の Section 章は header/footer 型を `SectionAccessory?` に更新し、`SectionAccessory` 章を新規追加。
- プラットフォーム別表現（iOS enum / Android sealed interface）と意味論（`text` 経路と `custom` 経路の使い分け、UI 層での `UICollectionLayoutListConfiguration` / `UIHostingConfiguration` への対応）を簡潔に整理。Decision 5b への参照リンクも整備されており、過不足のない記述 ✅

### 8. 全体ルール

- 全コメント・KDoc が日本語 ✅
- ファイル削除は本変更で不要のため `trash` は該当なし

---

## 既存実装への影響確認（Round 1 / Round 2 範囲）

| 既存型 | 影響 | 検証方法 | 結果 |
| --- | --- | --- | --- |
| `KsColor` / `KsFont` / `KsFontWeight` | 影響なし（Section に非依存） | 既存テスト維持（変更なし） | ✅ |
| `Theme` | 影響なし | `ThemeTests` / `ThemeTest` 各 4 件パス | ✅ |
| `CellStyle` | 影響なし | `CellStyleTests` / `CellStyleTest` 各 5 件パス | ✅ |
| `KsCell` (iOS) / `Cell` (Android) | 影響なし（abstract のまま） | 既存テスト維持 | ✅ |
| `AnyCell` (iOS) | 影響なし（API 変更なし） | `AnyCellTests` 5 件パス | ✅ |
| `Section` | 型変更（`header`/`footer`） | iOS `SectionTests` 6 件 / Android `SectionTest` 7 件 すべてパス | ✅ |
| `SettingsRoot` | 影響なし（Section 経由で間接的に追従） | iOS `SettingsRootTests` 4 件 / Android `SettingsRootTest` 4 件 すべてパス | ✅ |
| `TestSupportCells.kt`（Android） | 影響なし（Round 1 の既存物、`SectionAccessory.Custom(cell)` のテスト用にも再利用） | 全テスト依存先として正常稼働 | ✅ |

Round 1 / Round 2 で確立されたコードベースに退行は検出されなかった。

---

## アクションプラン

優先度順:

1. （任意・後続提案で対応可）**Suggestion 1**: `SectionAccessory` / `Section` の「カスタムヘッダ等価性」テストを別インスタンスでも検証するケースを追加し、Hashable / equals 契約のフルチェインをより堅牢に検証する。

本変更提案のマージ・アーカイブを止める性質の指摘は無し。

---

## 検証ログ

### iOS

```
$ cd ios && swift test
...
Test Suite 'KsSettingsViewPackageTests.xctest' passed at 2026-05-07 22:59:38.478.
   Executed 32 tests, with 0 failures (0 unexpected) in 0.004 (0.006) seconds
Test Suite 'All tests' passed at 2026-05-07 22:59:38.478.
```

内訳:
- `AnyCellTests` 5 / `CellStyleTests` 5 / `SectionAccessoryTests` 8 / `SectionTests` 6 / `SettingsRootTests` 4 / `ThemeTests` 4 = 32

### Android

```
$ cd android && ./gradlew :ks-settingsview-core:test --rerun-tasks
> Task :ks-settingsview-core:test
BUILD SUCCESSFUL in 2s
4 actionable tasks: 4 executed
```

JUnit XML 内訳（test-results/test/*.xml より）:
- `ThemeTest` 4 / `SectionAccessoryTest` 8 / `SettingsRootTest` 4 / `SectionTest` 7 / `CellStyleTest` 5 = 28

### OpenSpec

```
$ openspec validate add-settings-view-core --strict
Change 'add-settings-view-core' is valid
```

### tasks.md チェック整合性

- セクション 1〜7 のタスク（Round 1 / Round 2 範囲）: 既存どおり全 [x]、未着手項目への誤チェックなし
- セクション 8（Decision 5b 対応、本 Round 3 スコープ）: 8.1〜8.11 すべて [x]、実装・テスト・ドキュメントの実態と整合

---

## 判定結果

**ステータス**: ✅ **APPROVED**

- Critical / Major 指摘なし
- iOS 32 件 / Android 28 件すべてのテストが成功
- spec.md に追加された `SectionAccessory 型` Requirement / 更新された `Section ドメインモデル` Requirement の全 Scenario をテストで網羅
- ビルド成功、`openspec validate add-settings-view-core --strict` も valid
- tasks.md セクション 8（8.1〜8.11）完了、未実装の誤チェックなし
- Round 1 / Round 2 で確立した既存実装への退行ゼロ
- ドキュメント（`docs/core-model.md`）も実装と完全整合

Suggestion 1 は将来の品質向上余地として記録するが、本変更提案のマージ・アーカイブ可否を左右しない。本変更提案 `add-settings-view-core` は Round 3 でも APPROVED であり、後続変更提案（`add-settings-view-ios-ui` / `add-settings-view-android-ui` / `add-cell-types-custom` 等）を進めて問題ない状態と判定する。
