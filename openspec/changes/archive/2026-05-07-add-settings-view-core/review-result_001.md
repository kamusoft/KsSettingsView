# レビュー結果 - add-settings-view-core

**レビュー日時**: 2026年05月07日
**レビュワー**: sdd-reviewer
**変更提案ID**: add-settings-view-core

## サマリー

KsSettingsView の全プラットフォーム共通ドメインモデルを Native 言語ごとに正準ソースとして確立する変更提案の実装をレビューした。

- **iOS Swift モジュール `KsSettingsViewCore`**: 8 型 (`KsColor` / `KsFont` / `KsFontWeight` / `Theme` / `CellStyle` / `KsCell` / `AnyCell` / `Section` / `SettingsRoot`) を実装。Package.swift に target / testTarget を追加。
- **Android Kotlin モジュール `ks-settingsview-core`**: 同等型を `data class` / `sealed interface` で実装。`kotlin("jvm") 2.0.21` + JUnit 5 で構築。
- **ドキュメント**: `docs/core-model.md` に型一覧・変換ルール・スタイル合成意味論を整理。
- **テスト**: iOS 22 件・Android 18 件すべて成功（実測）。`openspec validate add-settings-view-core --strict` も `valid` で通過、`openspec list` でも当該変更が `Complete` 表示。
- **タスク**: 34 件すべて `[x]` で完了し、未実装でチェックされたタスクはなし。

仕様 (`spec.md`) に列挙された Requirement / Scenario はすべて実装・テストでカバーされており、設計判断（値型ファースト、型消去 AnyCell、論理 KsColor/KsFont、UI 非依存）は `design.md` の決定通りに実装されている。コード品質も高く、コメントは仕様への参照付きで十分に整備されている。

クリティカルな欠陥は検出されなかった。Minor / Suggestion レベルの所見が数点あるが、いずれも本変更提案の本質的成立を妨げない。

**判定**: ✅ **APPROVED**

---

## 指摘事項

#### 🟡 Minor 1: `docs/conventions.md` 上の Gradle モジュール名規約と実モジュール名の食い違い

**該当箇所**:
- `docs/conventions.md:26`（`kssettingsview-core`、`kssettingsview-android-ui` と記載）
- `android/settings.gradle.kts:29`（`include(":ks-settingsview-core")`）
- `openspec/changes/add-settings-view-core/proposal.md:14`、`tasks.md:29`（`ks-settingsview-core` と命名）

**問題点**:
`docs/conventions.md` の Kotlin 行は Gradle モジュール名を「`kssettingsview-core`」（`ks` と `settingsview` を連結）と例示しているのに対し、本変更提案で実装されたモジュール名は「`ks-settingsview-core`」（ハイフン区切り）。実装は変更提案 (`proposal.md` / `tasks.md`) に厳密に従っているため本変更提案としては問題ないが、規約ドキュメント側との不整合が残り、後続提案 `add-settings-view-android-ui` 等で混乱を招く可能性がある。

> ※ 禁止事項により本変更提案で `conventions.md` 等の規約ドキュメントに対する書き換え指摘は行いません。あくまで「後続変更提案で扱う論点」としての記録です。

**推奨修正**:
本変更では対応不要。後続変更提案または別途整合性整理用の change を立て、規約ドキュメント側を `ks-settingsview-core` 前提に揃えるか、もしくは Gradle モジュール命名を提案文どおり追認する旨を明記する。

---

#### 🟡 Minor 2: `Theme` の `footerBackgroundColor` デフォルトが `defaultHeaderBackgroundColor` を流用

**該当箇所**:
- `ios/Sources/KsSettingsViewCore/Theme.swift:43`
- `android/ks-settingsview-core/src/main/kotlin/jp/kamusoft/kssettingsview/core/Theme.kt:29`

**問題点**:
`footerBackgroundColor` のデフォルト値が `Theme.defaultHeaderBackgroundColor` / `DEFAULT_HEADER_BACKGROUND_COLOR` を直接参照している。意味論的には「ヘッダとフッタのデフォルト背景色は同色」という設計意図が読み取れるが、専用の `defaultFooterBackgroundColor` 定数が存在しないため、将来「フッタだけ背景色を変えたい」というニーズが出た際に誤用リスクがある（フッタ既定値を変えたつもりでヘッダも変わる、あるいは逆）。

**推奨修正**:
（任意・後続提案で対応可）`defaultFooterBackgroundColor` を別定数として宣言し、現状値は `defaultHeaderBackgroundColor` と同値で初期化する。これにより API 表現上は独立で進化できる。

```swift
public static let defaultFooterBackgroundColor: KsColor = defaultHeaderBackgroundColor
```

---

#### 🔵 Suggestion 1: `KsCell` プロトコルに `where ID == UUID` 制約を明示してもよい

**該当箇所**: `ios/Sources/KsSettingsViewCore/KsCell.swift:15-20`

**問題点（提案）**:
`KsCell` は `Identifiable` を継承し `var id: UUID { get }` を要求するため Swift の型推論で `ID == UUID` となる。`AnyCell.init<Cell: KsCell>(_ cell: Cell) where Cell.ID == UUID` 側で改めて制約しているのは現状で動作するが、プロトコル本体に `associatedtype ID = UUID` 相当の明示制約があれば、`AnyCell` 側の `where 句` を省略できコメント可読性が上がる。

**推奨修正（任意）**:
プロトコル定義側で次のいずれかを検討。

```swift
public protocol KsCell: Hashable, Identifiable where ID == UUID {
    var id: UUID { get }
    var style: CellStyle { get }
}
```

これにより `AnyCell.init` を `where` 句なしで書けて、ドキュメントとして意図が明確になる。

---

#### 🔵 Suggestion 2: Android 側 `Cell` の `id` に対する一意性要件のドキュメント化

**該当箇所**: `android/ks-settingsview-core/src/main/kotlin/jp/kamusoft/kssettingsview/core/Cell.kt:13-19`

**問題点（提案）**:
仕様 `Cell 抽象` Requirement は「一意な `id`」を求めるが、Kotlin 側 `Cell` の `id: String` は文字列なら何でも受け付ける。具象 Cell を定義する後続変更提案 (`add-cell-types-*`) で id 衝突や空文字 id が発生する可能性がある。Core では値域チェックを行わない方針 (`KsColor` と同様) で問題ないが、KDoc に「呼び出し側責務として一意性を保証すること」を明示しておくと後続実装の指針が明確になる。

**推奨修正（任意）**:
KDoc に注意書きを追記（Core ロジック自体は変更不要）。

---

#### 🔵 Suggestion 3: `iconSize` / `iconRadius` / `cellHeight` の単位を Doc で明示

**該当箇所**:
- `ios/Sources/KsSettingsViewCore/CellStyle.swift:25-29`（`iconSize` には「ポイント単位」と明記済、`iconRadius` / `cellHeight` には単位記述なし）
- `android/ks-settingsview-core/src/main/kotlin/jp/kamusoft/kssettingsview/core/CellStyle.kt:23-27`（同様）

**問題点（提案）**:
iOS と Android で「ポイント / dp」の単位差があるため、Core 仕様としては論理単位扱いになるはず。`iconSize` のみ「ポイント単位」と書かれているが、`iconRadius` / `cellHeight` には単位記述がない。後続 UI 層の変換時に「dp なのか px なのか」を間違えるリスクを下げるため、各フィールドに単位（または「論理単位、UI 層で適切に変換」の旨）を統一して明記したい。

**推奨修正（任意）**:
3 フィールドの KDoc / DocC に統一した単位指定を追記。

---

## アクションプラン

優先度順:

1.（任意・後続）**Minor 1**: `docs/conventions.md` の Gradle モジュール名例と実装の整合性を別変更提案で整理する。
2.（任意・後続）**Minor 2**: `Theme.defaultFooterBackgroundColor` の独立定数化を後続変更で検討する。
3.（任意）**Suggestion 1**: `KsCell` プロトコルに `where ID == UUID` を明示し、`AnyCell.init` の `where` 句を撤去。
4.（任意）**Suggestion 2 / 3**: KDoc / DocC コメントの追記（Cell.id 一意性、CellStyle 数値フィールドの単位）。

いずれも本変更提案のマージを止める性質のものではない。

---

## 検証ログ要約

- **iOS ビルド**: `swift build` → `Build complete!`（エラーなし）
- **iOS テスト**: `swift test` → `Executed 22 tests, with 0 failures`
  - `AnyCellTests` 5 件 / `CellStyleTests` 5 件 / `SectionTests` 4 件 / `SettingsRootTests` 4 件 / `ThemeTests` 4 件
- **Android テスト**: `./gradlew :ks-settingsview-core:test --rerun-tasks` → `BUILD SUCCESSFUL` / 18 件すべて成功
  - `CellStyleTest` 5 件 / `SectionTest` 5 件 / `SettingsRootTest` 4 件 / `ThemeTest` 4 件
- **OpenSpec**: `openspec validate add-settings-view-core --strict` → `valid`
- **タスクチェック**: `tasks.md` の 34 タスクすべて `[x]`、未完成項目は確認されず

## 仕様 Scenario 充足マッピング

| Requirement | Scenario | iOS テスト | Android テスト |
| --- | --- | --- | --- |
| SettingsRoot ドメインモデル | 構築 | ✅ `test_構築_sections_と_theme_を保持する` | ✅ `build_holds_sections_and_theme` |
| SettingsRoot ドメインモデル | 等価性 | ✅ `test_等価性_同フィールドのインスタンスは等しい` | ✅ `equality_same_fields_are_equal` |
| Section ドメインモデル | 構築 | ✅ `test_構築_全フィールドを保持する` | ✅ `build_holds_all_fields` |
| Section ドメインモデル | 空セクション | ✅ `test_空_cells_で構築でき_isEmpty_が真` | ✅ `empty_cells_can_be_built` |
| Cell 抽象 | Swift プロトコル定義 | ✅ プロトコル定義 + `AnyCellTests` で間接確認 | — |
| Cell 抽象 | Kotlin sealed 抽象 | — | ✅ `Cell.kt` 定義 + `heterogeneous_cells_can_be_stored` |
| AnyCell 型消去 | 異種 Cell 格納 | ✅ `test_異種_Cell_を同コレクションに格納できる` / `test_unwrap_で具象型に復元できる` | N/A (Kotlin は sealed) |
| Theme | デフォルト値 | ✅ `test_デフォルト値_引数なしで構築できる` | ✅ `default_values_can_be_built_with_no_args` |
| Theme | プラットフォーム型を持たない | ✅ 型レベルで保証（KsColor / KsFont のみ） | ✅ 同上 |
| CellStyle | デフォルト値 | ✅ `test_デフォルト値_全フィールドが_nil` | ✅ `default_all_fields_are_null` |
| CellStyle | Theme との継承関係（Optional 表現） | ✅ 型定義で保証 + `test_等価性_片方が_nil_もう片方が値の場合は等しくない` | ✅ 同上 |
| Hashable / equals 契約 | 同一フィールド | ✅ 各 `_等価性_同フィールド` テスト + `test_Hashable_Set_に格納できる` | ✅ 各 `equality_same_fields_are_equal` + `hashcode_set_dedup` |
| Hashable / equals 契約 | フィールド変更後 | ✅ 各 `_等価性_フィールドが異なれば等しくない` | ✅ 各 `equality_different_fields_are_not_equal` |
| ユニットテストの存在 | iOS 実行 | ✅ `swift test` 成功 | — |
| ユニットテストの存在 | Android 実行 | — | ✅ `./gradlew :ks-settingsview-core:test` 成功 |

仕様で要求されたすべての Scenario について、iOS / Android のいずれか（あるいは両方）でテスト実装と成功が確認できた。

---

## 判定結果

**ステータス**: ✅ **APPROVED**

- Critical / Major 指摘なし
- すべてのテストが成功
- 仕様 Requirement / Scenario をテストで網羅
- ビルド成功、`openspec validate --strict` も通過
- タスク 34/34 完了、未実装フラグ立て検知なし

Minor / Suggestion 指摘は将来の品質向上余地として記録するが、本変更提案のマージ可否を左右しない。後続変更提案 (`add-settings-view-ios-ui` / `add-settings-view-android-ui` / `add-cell-types-*` / `add-maui-bindings`) を進めて問題ない状態と判定する。
