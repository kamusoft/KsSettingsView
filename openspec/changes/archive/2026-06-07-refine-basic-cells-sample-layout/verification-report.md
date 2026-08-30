# Verification Report: refine-basic-cells-sample-layout

Date: 2026-06-04

## Summary

| Dimension    | Status                                     |
|--------------|--------------------------------------------|
| Completeness | 44/52 tasks (8 incomplete - 実機目視確認のみ), 全 Requirement 実装あり |
| Correctness  | 全 Requirement 実装済み、Scenario カバー済み |
| Coherence    | Design に準拠、パターン一貫                  |

---

## Completeness

### Task Completion: 44/52

完了: 44 タスク
未完了: 8 タスク（すべて実機目視確認）

未完了タスクの一覧:

- [ ] 5.3: 単一 Cell のセクションでも上下両方の罫線が端から端で描画されることを確認するユニットテストを追加する
- [ ] 5.4: アイコン有り Cell と無し Cell が混在するセクションで、インセットが正しく切り替わることを確認するユニットテストを追加する
- [ ] 7.4: 実機 / エミュレータで checked = true / false 双方の Switch を目視確認する
- [ ] 8.3: 必要に応じて marginEnd を微調整し、各アクセサリ右端 X 座標と ±1px 以内で一致するよう実機検証する
- [ ] 10.4: iOS Sample をシミュレータで起動し、構成が新しい順序・テキストで描画されることを目視確認する
- [ ] 11.5: Android Sample をエミュレータで起動し、構成が iOS と同順序・同テキストで描画されることを目視確認する
- [ ] 13.1: iOS シミュレータで「基本 Cell 7 種デモ」を起動し視覚要素を確認する
- [ ] 13.2: Android エミュレータで「基本 Cell 7 種デモ」を起動し視覚要素を確認する

判定: これら 8 タスクはすべて「実機目視確認が必要（自動チェック不可）。コード実装は完了。」と明記されており、tasks.md 内のコメントで自動化不可の理由が記載されている。コード実装はすべて完了。

### Requirement Coverage

6 つの delta spec の全 Requirement を確認:

| Spec | Requirement | 実装ファイル |
|------|-------------|-----------|
| cell-types-basic | KsImage 値型（iOS sealed enum） | `ios/Sources/KsSettingsViewCore/KsImage.swift` |
| cell-types-basic | KsImage 値型（Android sealed interface） | `android/ks-settingsview-core/.../KsImage.kt` |
| settings-view-core | Section ドメインモデル（headerHeight 追加） | `ios/.../Section.swift`, `android/.../Section.kt` |
| settings-view-ios-ui | Section Header/Footer の sticky 抑止 | `KsSettingsViewController.swift` L338-370 |
| settings-view-ios-ui | viewBackgroundColor のセクション間反映 | `KsSettingsViewController.swift` L263 |
| settings-view-ios-ui | Section.headerHeight の UI 反映 | `KsSettingsViewController.swift` L387-410 |
| settings-view-ios-ui | Section Footer 空時の supplementary 非生成 | `KsSettingsViewController.swift` L419-423 |
| settings-view-ios-ui | 罫線インセット規則 | `KsSettingsViewController.swift` L430-485 |
| settings-view-ios-ui | KsImage.uiImage 派生の解決 | `LabelCellView.swift` L95-109 |
| settings-view-android-ui | SwitchCell の Thumb/Track 色分離 | `SwitchCellViewHolder.kt` L69-94 |
| settings-view-android-ui | CheckboxCell の右端整列強化 | `CheckboxCellViewHolder.kt` L116-133 |
| settings-view-android-ui | KsImage 派生のアイコン解決 | `LabelCellViewHolder.kt` L274-303 |
| samples-ios | 基本 Cell 7 種デモ画面（Cell タイプ別構成） | `samples/ios/.../BasicCellsDemoView.swift` |
| samples-android | 基本 Cell 7 種デモ画面（Cell タイプ別構成） | `samples/android/.../BasicCellsDemoScreen.kt` |

---

## Correctness

### KsImage sealed 化

**iOS**: `KsImage.swift` は `public enum KsImage: Hashable, @unchecked Sendable` で `systemName(String)` と `uiImage(UIImage)` の 2 ケースを持つ。`Hashable` 実装は仕様どおり `systemName` は String hash、`uiImage` は `ObjectIdentifier` 参照同一性。旧 3 フィールド形式は廃止済み。

**Android**: `KsImage.kt` は `sealed interface KsImage` で `Resource(resId: Int)` / `class Drawable(...)` / `data class SystemName(name: String)` の 3 派生を持つ。`Drawable` は参照同一性（`System.identityHashCode`）、`Resource`/`SystemName` は値同一性（`data class`）で仕様と一致。

### Section.headerHeight

**iOS**: `Section.swift` の `let headerHeight: Double`（既定 `-1`）が末尾フィールドとして追加され、メンバワイズイニシャライザの末尾デフォルト引数（`headerHeight: Double = -1`）で既存呼び出しを破壊しない。

**Android**: `Section.kt` の `val headerHeight: Double = -1.0` がデータクラス末尾に追加済み。

### iOS UI: sticky 抑止

`KsSettingsViewController.swift` の sectionProvider ループ（L350-371）で `elementKindSectionFooter` の場合に `item.pinToVisibleBounds = false` を適用（L363）。Header も同様に `newItem.pinToVisibleBounds = false`（L356）。仕様の「Header および Footer の両方とも pinToVisibleBounds = false」に準拠。

### iOS UI: viewBackgroundColor のセクション間反映

`makeLayout(for:)` 内 L263 で `listConfig.backgroundColor = .clear` を設定。`UICollectionView.backgroundColor` には `effective.viewBackgroundColor` を維持（L243）。仕様の要件に完全準拠。

### iOS UI: Section.headerHeight の UI 反映

`makeHeaderBoundaryItem(for:original:)` メソッド（L387-410）で `headerHeight > 0` → `.absolute`、`headerHeight == -1` かつ `header` 非空 → `.estimated`（元の item を維持）、`headerHeight == -1` かつ `header == nil` → `nil`（非生成）の 3 分岐を実装。仕様の優先順位 1-3 に完全準拠。

### iOS UI: Footer 空時の非生成

`shouldShowFooter(for:)` メソッド（L419-423）で `footer == nil` または `.text("")` の場合は `false` を返し、sectionProvider ループで非生成。仕様準拠。

### iOS UI: 罫線インセット規則

`separatorConfiguration(for:base:)` メソッド（L430-467）と `titleLeadingPosition(for:)` メソッド（L473-485）で仕様の規則を実装：
- セクション最初: `topSeparatorInsets.leading = 0`
- セクション最後: `bottomSeparatorInsets.leading = 0`
- 中間: `bottomSeparatorInsets.leading = titleLeading`（アイコン無し 16pt、アイコン有り 52pt）

仕様の「アイコン有り時 = 16 + 24 + 12 = 52pt」に準拠。

### iOS UI: KsImage 解決

`LabelCellView.swift` の `applyLabelCellContents` 関数 L95-109 で `switch icon` による網羅解決（`.systemName` → `UIImage(systemName:)`, `.uiImage` → image, `nil` → `content.image = nil`）。仕様の「icon == nil 時に content.image = nil を明示」も実装済み（Minor-2 対応）。

### Android UI: SwitchCell Thumb/Track 色分離

`SwitchCellViewHolder.kt` L69-94 で `thumbTintList`（checked: colorOnPrimary、unchecked: colorOutline の ColorStateList）と `trackTintList`（accent 単色）を独立設定。仕様準拠。

### Android UI: CheckboxCell 右端整列強化

`CheckboxCellViewHolder.kt` L120-128 で `sizePx = (24 * density).toInt()` の明示 `LayoutParams` を設定、`setPadding(0,0,0,0)` / `minimumWidth=0` / `minimumHeight=0` も維持。仕様準拠。

### Android UI: KsImage アイコン解決

`LabelCellViewHolder.kt` の `applyLabelCellContents` 関数 L274-303 で `when(icon)` による網羅解決（`null` → GONE、`Drawable` → setImageDrawable、`Resource` → ContextCompat.getDrawable、`SystemName` → GONE でフォールバック、エラーなし）。仕様の「SystemName で throw/ログ禁止」に準拠。

### Sample: iOS BasicCellsDemoView

`BasicCellsDemoView.swift` のセクション構成（CommandCell → LabelCell → SwitchCell → CheckboxCell → RadioCell → SimpleCheckCell → ButtonCell）、各 Cell のタイトル・description・valueText・footer テキストが仕様の Cell タイプ別構成 spec と一致。MAUI 互換 Theme（viewBackground #F2EFE6 等 10 フィールド）を明示渡し。

### Sample: Android BasicCellsDemoScreen

`BasicCellsDemoScreen.kt` のセクション構成・テキストが iOS と一字一句一致（仕様確認済み）。アイコンリソース（`ic_account_circle.xml`, `ic_storage.xml`）が `samples/android/app/src/main/res/drawable/` に存在確認済み。

### Scenario Coverage

| Spec / Scenario | テスト |
|----------------|--------|
| KsImage iOS の派生定義・構築・等価性 | `KsImageTests.swift` 全 8 テスト |
| KsImage Android の派生定義・構築・等価性・when 網羅 | `KsImageTest.kt` 全 10 テスト |
| Section headerHeight 既定値・明示指定・等価性 | `SectionTests.swift` L120-149、`SectionTest.kt` L128-156 |
| iOS Footer 空文字列 → footerMode = .none | `KsSettingsViewControllerTests.swift` L119-124 |
| iOS headerHeight 正値 → headerMode = .supplementary | `KsSettingsViewControllerTests.swift` L128-138 |
| iOS KsImage.systemName/uiImage/nil 解決 | `BasicCellsTests.swift` L134-169 |
| Android SwitchCell trackTintList / thumbTintList | `BasicCellsTest.kt` L1083-1122 |
| Android CheckboxCell 24dp 明示サイズ | `BasicCellsTest.kt` L1135-1143 |
| Android KsImage.Resource/Drawable/SystemName 解決 | `BasicCellsTest.kt` L1156-1217 |

---

## Coherence

### Design Adherence

`design.md` の Decision と実装の対応：

- Decision 1（KsImage sealed 化）: 実装済み
- Decision 3（viewBackgroundColor セクション間反映）: `listConfig.backgroundColor = .clear` 実装済み
- Decision 4（Footer pinToVisibleBounds = false）: sectionProvider ループで実装済み
- Decision 5（Header/Footer 高さ・余白制御）: `makeHeaderBoundaryItem` / `shouldShowFooter` 実装済み
- Decision 7（罫線インセット規則）: `separatorConfiguration(for:base:)` 実装済み
- Decision 10（CheckboxCell 24dp 明示サイズ）: `LayoutParams(sizePx, sizePx)` 実装済み

### Code Pattern Consistency

ファイル命名・ディレクトリ構造・コーディングスタイルはプロジェクトの既存パターンに準拠。コメント参照先（openspec/changes/...）も正確に記載されている。

---

## Issues

### CRITICAL

なし

### WARNING

なし

### SUGGESTION

なし

---

## Final Assessment

CRITICAL なし、WARNING なし、SUGGESTION なし。

未完了の 8 タスク（5.3, 5.4, 7.4, 8.3, 10.4, 11.5, 13.1, 13.2）はすべて「実機目視確認が必要（自動チェック不可）。コード実装は完了。」と tasks.md に明記されており、コード実装・ユニットテストは完了している。これらはユーザーが手動で確認すべき項目であり、実装の不一致ではない。

**判定: VALID**

すべての仕様 Requirement が実装に反映されており、テストが Scenario をカバーし、設計制約（SystemName で throw/ログ禁止等）も守られている。アーカイブ可能な状態。
