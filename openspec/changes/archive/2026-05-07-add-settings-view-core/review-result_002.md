# レビュー結果 - add-settings-view-core (Round 2)

**レビュー日時**: 2026年05月07日
**レビュワー**: sdd-reviewer
**変更提案ID**: add-settings-view-core
**前回レビュー**: review-result_001.md (APPROVED, 任意指摘 4 件)

## サマリー

review-result_001.md で記録された Minor / Suggestion 指摘 4 件への追加修正をレビューした。
（Minor 1 は別変更提案のスコープのため対象外。今回対応は Minor 2 / Suggestion 1 / Suggestion 2 / Suggestion 3 の 4 件）

- **Minor 2** (`defaultFooterBackgroundColor` 独立定数化) — iOS / Android 両方で対応完了
- **Suggestion 1** (`KsCell` に `where ID == UUID` 制約明示、`AnyCell.init` の where 句撤去) — 対応完了
- **Suggestion 2** (Kotlin `Cell.id` 一意性責務の KDoc 明示) — 対応完了
- **Suggestion 3** (`iconSize` / `iconRadius` / `cellHeight` の単位記述統一) — 対応完了（コード × 2 + docs）

すべての指摘事項に対して、過剰でも過小でもない適切な範囲で修正が行われている。後方互換性も維持（同値で初期化）されており、仕様（spec.md）の Requirement / Scenario には一切影響を与えていない。ビルド・テスト・openspec strict validate もすべてグリーン。

**判定**: ✅ **APPROVED**

---

## 各指摘事項の対応確認

### Minor 2: `defaultFooterBackgroundColor` 独立定数化

**該当箇所**:
- `ios/Sources/KsSettingsViewCore/Theme.swift:43, 66-67`
- `android/ks-settingsview-core/src/main/kotlin/jp/kamusoft/kssettingsview/core/Theme.kt:29, 43-44`

**対応内容**:

iOS 側:
```swift
/// フッタ既定背景色（現状はヘッダと同値だが、将来的に独立進化できるよう別定数として宣言）
public static let defaultFooterBackgroundColor: KsColor = defaultHeaderBackgroundColor
```
`init(...)` の `footerBackgroundColor` デフォルト引数を `Theme.defaultFooterBackgroundColor` に切り替え。

Android 側:
```kotlin
/** フッタ既定背景色（現状はヘッダと同値だが、将来的に独立進化できるよう別定数として宣言） */
val DEFAULT_FOOTER_BACKGROUND_COLOR: KsColor = DEFAULT_HEADER_BACKGROUND_COLOR
```
`data class Theme(...)` の `footerBackgroundColor` デフォルト引数を `DEFAULT_FOOTER_BACKGROUND_COLOR` に切り替え。

**評価**: ✅ 推奨修正どおり。同値で初期化することで後方互換を維持しつつ、API 表現上は独立進化可能になった。コメントも設計意図を明示している。`ThemeTests` の「デフォルト値」テストも変更不要で通過しており、退行なし。

---

### Suggestion 1: `KsCell` に `where ID == UUID` 明示

**該当箇所**:
- `ios/Sources/KsSettingsViewCore/KsCell.swift:17`
- `ios/Sources/KsSettingsViewCore/AnyCell.swift:27`

**対応内容**:

```swift
public protocol KsCell: Hashable, Identifiable where ID == UUID {
    var id: UUID { get }
    var style: CellStyle { get }
}
```

`AnyCell.init` から `where Cell.ID == UUID` を撤去:

```swift
public init<Cell: KsCell>(_ cell: Cell) {
    self.id = cell.id
    self.style = cell.style
    self._box = AnyHashable(cell)
}
```

**評価**: ✅ 推奨修正どおり。`KsCell` プロトコル本体で型制約が明示されたことで、利用側コードの可読性が上がった。`AnyCellTests` 全 5 件含めテストすべてパス、Sendable 等の他の制約とも干渉なし。

---

### Suggestion 2: Kotlin `Cell.id` 一意性責務の KDoc 明示

**該当箇所**: `android/ks-settingsview-core/src/main/kotlin/jp/kamusoft/kssettingsview/core/Cell.kt:13-19`

**対応内容**:

```kotlin
 * @property id 一意な ID（Bridge 境界では String として扱われる）。
 *   **一意性は呼び出し側の責務**であり、Core 層では値域チェックを行わない
 *   （`KsColor` 等と同方針）。具象 Cell を構築する側は、少なくとも
 *   同一 [Section] 内で `id` が衝突しないこと、および空文字列でないことを
 *   保証すること。`UICollectionViewDiffableDataSource` / `DiffUtil` 等の
 *   差分検出は `id` の一意性を前提とする。
```

**評価**: ✅ 推奨修正どおり。後続 `add-cell-types-*` 提案で具象 Cell 実装者が参照すべき責務が明確になった。Core ロジック自体は変更されておらず、テスト退行なし。`[Section]` の KDoc リンクも適切。

---

### Suggestion 3: `iconRadius` / `cellHeight` の単位明記

**該当箇所**:
- `ios/Sources/KsSettingsViewCore/CellStyle.swift:25-29`
- `android/ks-settingsview-core/src/main/kotlin/jp/kamusoft/kssettingsview/core/CellStyle.kt:22-27`
- `docs/core-model.md:66-68`

**対応内容**:

iOS（3 フィールド統一）:
```
/// アイコンサイズ（幅・高さの 1 辺。論理単位、iOS では points として UI 層で解釈する。`nil` は Theme 既定）
/// アイコン角丸半径（論理単位、iOS では points として UI 層で解釈する。`nil` は Theme 既定）
/// Cell 高さ（論理単位、iOS では points として UI 層で解釈する。`nil` は Theme 既定）
```

Android（3 フィールド統一）:
```
/** アイコンサイズ（幅・高さの 1 辺。論理単位、Android では dp 相当として UI 層で解釈する。null は Theme 既定） */
/** アイコン角丸半径（論理単位、Android では dp 相当として UI 層で解釈する。null は Theme 既定） */
/** Cell 高さ（論理単位、Android では dp 相当として UI 層で解釈する。null は Theme 既定） */
```

docs/core-model.md（表内 3 行統一）:
```
| `iconSize` | `Double?` | アイコンサイズ（1 辺、論理単位。iOS は points、Android は dp 相当として UI 層で解釈する） |
| `iconRadius` | `Double?` | アイコン角丸半径（論理単位。iOS は points、Android は dp 相当として UI 層で解釈する） |
| `cellHeight` | `Double?` | Cell 高さ（論理単位。iOS は points、Android は dp 相当として UI 層で解釈する） |
```

**評価**: ✅ 推奨修正どおり。3 フィールドすべて統一フォーマットで「論理単位 + プラットフォーム解釈」が明記された。docs と Swift / Kotlin の文言も整合しており、後続 UI 層実装時の単位解釈ミスのリスクが軽減された。

---

## 仕様整合性チェック

修正は以下の観点で spec.md の Requirement / Scenario に違反していないことを確認した。

| Requirement | 影響評価 |
| --- | --- |
| SettingsRoot ドメインモデル | 変更なし、影響なし |
| Section ドメインモデル | 変更なし、影響なし |
| Cell 抽象（Swift / Kotlin） | iOS は `where ID == UUID` 明示で「`var id: UUID { get }` を要求する」要件は完全充足。Kotlin は KDoc 追加のみで仕様側影響なし |
| AnyCell 型消去（iOS） | `where` 句の置き場所変更のみで API 公開シグネチャは互換維持、`Hashable` 要件も変更なし |
| Theme 型 | `defaultFooterBackgroundColor` 定数追加のみ。フィールド集合・デフォルト値は不変（同値で初期化）。「デフォルト値」Scenario に退行なし |
| CellStyle 型 | コメントのみ追加、フィールド構造に変更なし |
| Hashable / equals 契約 | 変更なし、影響なし |
| ユニットテストの存在 | 変更なし、テスト全件パス維持 |

tasks.md の 34 タスクはすべて完了状態を維持。proposal.md / design.md にも矛盾は発生していない。

---

## 検証ログ

- **iOS ビルド**: `swift build` → `Build complete!` (0.21s)
- **iOS テスト**: `swift test` → `Executed 22 tests, with 0 failures`
  - `AnyCellTests` 5 件 / `CellStyleTests` 5 件 / `SectionTests` 4 件 / `SettingsRootTests` 4 件 / `ThemeTests` 4 件
- **Android テスト**: `./gradlew :ks-settingsview-core:test --rerun-tasks` → `BUILD SUCCESSFUL`（18 件）
- **OpenSpec**: `openspec validate add-settings-view-core --strict` → `Change 'add-settings-view-core' is valid`

---

## アクションプラン

なし。すべての指摘事項に対する追加修正は完了。

引き続き残る論点（本変更提案外）:
- **Minor 1**（`docs/conventions.md` の Gradle モジュール命名規約食い違い）: 別変更提案で扱う前提。本変更提案ではスコープ外。

---

## 判定結果

**ステータス**: ✅ **APPROVED**

- review-result_001.md で挙げた 4 件の任意指摘すべてに対し、過不足のない適切な修正が行われている
- 修正に伴う仕様（spec.md）違反・タスク退行・テスト失敗は一切なし
- ビルド・テスト・`openspec validate --strict` すべてグリーン
- 後方互換性も維持（`defaultFooterBackgroundColor` を同値で初期化）

本変更提案は Round 1 で既に APPROVED であり、Round 2 で品質がさらに向上した。マージ・アーカイブ可能な状態。
