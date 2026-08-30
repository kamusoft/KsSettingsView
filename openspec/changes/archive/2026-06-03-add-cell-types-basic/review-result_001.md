# レビュー結果 - add-cell-types-basic

**レビュー日時**: 2026年05月18日
**レビュワー**: sdd-reviewer
**変更提案ID**: add-cell-types-basic

## サマリー

本変更提案は、iOS / Android Native の両プラットフォームに基本 Cell 7 種（LabelCell / CommandCell / ButtonCell / SwitchCell / CheckboxCell / RadioCell / SimpleCheckCell）を導入し、`KsImage` 値型・一括登録 API・PoC Cell 削除・DSL 連動規約（`id` デフォルト値・`DSLReidentifiable(Cell)` / `DSLStyleModifiable(Cell)` 実装・Compose 用 DSL 拡張関数）・Sample 追加・ドキュメント整備までを網羅的に実装している。

- ビルド: iOS / Android 共に成功
- テスト: iOS 121 件全 PASS、Android `:ks-settingsview-ui:test` / `:ks-settingsview-core:test` / `:ks-settingsview-compose:test` 全 testsuite で `failures="0" errors="0"`
- `openspec validate add-cell-types-basic --strict`: PASS
- 仕様カバレッジ: `cell-types-basic` capability の全 Scenario（KsImage / 各 Cell / 登録 API / id デフォルト値 / DSL 拡張関数 / PoC Cell 削除）に対応する実装・テストが揃っている
- 規約遵守: iOS struct / Android data class 双方で `DSLReidentifiable(Cell)` / `DSLStyleModifiable(Cell)` を Core モジュール側に配置した interface / protocol に implement している（タスク 1.5.3 / 1.5.4 の前提を満たす）
- クロージャ等価性除外（Decision 2）が iOS / Android で一貫実装されている
- PoC Cell（`PoCLabelCell` / `PocLabelCell`、`VIEW_TYPE_POC` 定数）は本体 UI モジュールから完全に削除されている
- Sample 側は iOS `BasicCellsDemoView.swift` / Android `BasicCellsDemoScreen.kt` で 7 種 Cell の DSL 直置きデモを実装。トップメニューにナビゲーション導線も追加済み

重大な懸念点はなく、品質も高い。Minor / Suggestion 数件のみ指摘する。

**判定**: `APPROVED`

## 指摘事項

### 🟡 Minor

#### 🟡 Minor 1: Android LabelCell 系で icon の `systemName` 解決未完了時に空のアイコン領域が表示される

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/LabelCellViewHolder.kt:249-254`

**問題点**:

`applyLabelCellContents` 内で、`iconSystemName != null` のとき `iconView.visibility = View.VISIBLE` を設定するが、現在の実装では Material Icons 依存回避のため画像 Drawable をセットしない。結果として 24dp 四方の空の `AppCompatImageView` が左側に確保され、`title` の左にわずかな空白だけが表示される（drawable は `null` のため透明）。

```kotlin
if (iconSystemName != null) {
    iconView.visibility = View.VISIBLE   // 画像が設定されないのに領域だけ確保される
} else {
    iconView.visibility = View.GONE
    iconView.setImageDrawable(null)
}
```

仕様 `cell-types-basic/spec.md` の `KsImage` Scenario「Cell からの参照」では「UI 層は `systemName` から ... `Drawable` を解決して表示する」とあり、Android 側でリソース解決ロジックが未実装な現状では画像解決自体は本提案の責務外と読めるが、解決前の段階で領域だけ予約してしまうと「アイコンを指定したのに空白だけ表示される」ユーザー体験になる。

**推奨修正**:

Material Icons 依存を増やさずに対応する案：

1. 解決ロジック未実装時は `iconView.visibility = View.GONE` のままにする（後続提案で解決時に visibility 制御を移す）
2. または `applyLabelCellContents` の docstring の注意書きを ViewHolder の bind 結果として「systemName 指定時のアイコン解決は後続提案」と明記しつつ、当面 visibility は GONE 固定にする

```kotlin
if (iconSystemName != null) {
    // 本提案では Material Icons の解決ロジックは未実装。
    // 後続提案で解決後、ここで Drawable を設定して VISIBLE にする。
    iconView.visibility = View.GONE
} else {
    iconView.visibility = View.GONE
    iconView.setImageDrawable(null)
}
```

iOS 側は `UIImage(systemName:)` で SF Symbols が解決済みのため非対称になっているが、Android Material Icons の依存追加は別提案の責務として明示する方が現実的。

#### 🟡 Minor 2: BasicCellsDemoScreen.kt 末尾の未使用ローカル変数

**該当箇所**: `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/BasicCellsDemoScreen.kt:135-137`

**問題点**:

```kotlin
// ButtonHeading は Compose layouts の Arrangement 強制のために残す（未使用変数警告抑止）
@Suppress("UNUSED_VARIABLE")
val arrangement = Arrangement.spacedBy(8.dp)
```

`Arrangement.spacedBy` の戻り値を warning 抑止だけのために保持しており、コメントの理由（「Compose layouts の Arrangement 強制」）が技術的に意味不明である（実際には何の強制効果も無い）。実害は無いが、不要なコードでサンプルの可読性を損ねる。

**推奨修正**:

該当 3 行を削除する。`Column` 内に `verticalArrangement` を指定したい場合は `Column(... verticalArrangement = Arrangement.spacedBy(8.dp))` のように Column の引数に直接渡す。本デモは `KsSettingsView` で完結しているため、削除のみで十分。

```kotlin
// 削除：
// @Suppress("UNUSED_VARIABLE")
// val arrangement = Arrangement.spacedBy(8.dp)
```

### 🔵 Suggestion

#### 🔵 Suggestion 1: iOS / Android で `withDSLID` / `withDSLId` の case 表記が非対称

**該当箇所**:

- iOS: `LabelCell.swift:59`（`withDSLID(_:)`）
- Android: `LabelCell.kt:38`（`withDSLId(...)`）

**問題点**:

両プラットフォームで関数名の case 表記が異なる（`withDSLID` vs `withDSLId`）。これは Core モジュールの interface / protocol 側ですでにこの非対称が確定しているため本提案で書き換えるべきものではない（`add-declarative-dsl` で確定済み）が、両プラットフォームを行き来する利用者・MAUI バインディング層実装者にとって視認性が落ちる可能性がある。

**推奨**:

本提案では現状維持。`add-declarative-dsl` のオーナーレビュー後の整合化議論で扱う方が適切。`docs/conventions.md` 等で「Swift は `ID`、Kotlin は `Id` の表記」など簡単な注記が望ましい（既に書かれていれば不要）。

#### 🔵 Suggestion 2: CommandCellView の accessories 構成順序の動作差

**該当箇所**: `ios/Sources/KsSettingsViewUI/CommandCellView.swift:43-46`

**問題点**:

`applyLabelCellContents` 内で `hintText` がある場合 `accessories = [hintCustomLabel]` を設定後、`!hideArrow` のとき `accessories.append(.disclosureIndicator())` する。結果として `[hintCustomView(trailing), disclosureIndicator(trailing)]` の 2 つが trailing accessory として並ぶ。UIKit の List Cell は accessory placement に応じて自動配置するため動作上は問題ないが、配置順は spec で明示されていないため、将来の挙動変更（順序入れ替え等）に影響しやすい。

**推奨**:

現状の振る舞いを正とするなら、spec.md の「CommandCell」Requirement に「hintText が同居する場合は hintText → disclosure の順で trailing 配置」のような追記を `add-cell-types-basic` 完了後の運用ドキュメント（`docs/cell-types-basic.md`）側に書いておくと、後続変更時のリグレッション検出が容易になる。

## アクションプラン

優先度順（**本提案では `APPROVED` のためマージブロックする項目はない**）：

1. （Minor）Android LabelCellViewHolder で icon visibility を GONE 固定にし、後続提案で解決ロジックを追加するときに VISIBLE に変えるコメントを残す
2. （Minor）`BasicCellsDemoScreen.kt` 末尾の `arrangement` ローカル変数を削除
3. （Suggestion）`docs/cell-types-basic.md` に CommandCell の accessory 並び順を補記
4. （Suggestion）将来の `add-declarative-dsl` 整合化議論で `withDSLID` / `withDSLId` の case 統一を検討

## 判定結果

**ステータス**: `APPROVED`

- Critical / Major 指摘なし
- Minor 2 件 / Suggestion 2 件はいずれも本提案のマージを止める性質のものではなく、別途 follow-up または後続提案で対応可能
- iOS / Android 両プラットフォームのテストが全件 PASS、`openspec validate --strict` も PASS、全 tasks.md チェック完了、PoC Cell も完全削除されており、本提案の完了条件をすべて満たしている
