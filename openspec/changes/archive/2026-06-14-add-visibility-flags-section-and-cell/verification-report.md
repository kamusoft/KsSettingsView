# 検証レポート - add-visibility-flags-section-and-cell

**検証日時**: 2026年06月14日
**検証者**: openspec-verify-change
**変更提案ID**: add-visibility-flags-section-and-cell

---

## Summary

| Dimension    | Status                                           |
|--------------|--------------------------------------------------|
| Completeness | 88/89 tasks (12.3 のみ未チェック・ユーザー目視対象) / 6 specs 全 Req 実装済み |
| Correctness  | 全 MUST/SHALL/Scenario が実装に反映されている     |
| Coherence    | design.md の全 Decision に従っている              |

---

## Issues by Priority

### CRITICAL（なし）

### WARNING（なし）

### SUGGESTION（なし）

---

## 検証詳細

### Completeness

**タスク完了状況**:

- tasks.md の 89 項目中 88 項目が `[x]` チェック済み。
- 未完了は `12.3 iOS / Android sample アプリで「条件付き非表示」サンプルが期待通りに動作することを目視確認` のみ。
  - タスク本体に「実機/シミュレータでの目視確認はユーザー実施対象。ビルドは両プラットフォームで成功確認済み」と明記されており、自動検証の対象外である。
  - ビルド成功は `.build` ディレクトリ内のアーティファクト（`SectionVisibilityTests.swift.o` タイムスタンプ 2026-06-14）および Android テスト結果 XML（`testReleaseUnitTest`、タイムスタンプ 2026-06-14）で確認済み。

**Spec Coverage**:

6 つの delta spec（`settings-view-core` / `cell-types-basic` / `settings-view-ios-host` / `settings-view-android-host` / `settings-view-ios-swiftui` / `settings-view-android-compose`）の全 Requirement が実装に反映されていることを確認。

| Spec | Requirements | 確認状況 |
|------|-------------|----------|
| settings-view-core | 表示状態同期の三層分離（RENAMED）/ Section ドメインモデル（isVisible 追加） | 実装確認 |
| cell-types-basic | 全 Cell 共通の isVisible / VisibilityAware 抽象 | 実装確認 |
| settings-view-ios-host | visible projection の二重管理 / 部分 Diff の index 規約 / ReplaceCell/ReplaceSection 防御 / layout mode 再評価 | 実装確認 |
| settings-view-android-host | visible projection flatten 規約 / 部分 Diff index 規約 / ReplaceCell/ReplaceSection 防御 | 実装確認 |
| settings-view-ios-swiftui | DSL preflight / isVisible 引数 | 実装確認 |
| settings-view-android-compose | DSL preflight + contentUpdates 空 / isVisible 引数 | 実装確認 |

### Correctness

**実装確認項目**:

- `ios/Sources/KsSettingsViewCore/Section.swift`: `isVisible: Bool = true` 追加、`==` / `hash(into:)` に `isVisible` 含む。
- `android/ks-settingsview-core/.../Section.kt`: `val isVisible: Boolean = true` 追加（data class 自動 equals/hashCode）。
- `ios/Sources/KsSettingsViewUI/VisibilityAware.swift`: `public protocol VisibilityAware { var isVisible: Bool { get } }` を新規作成。
- `android/ks-settingsview-ui/.../VisibilityAware.kt`: `interface VisibilityAware { val isVisible: Boolean }` を新規作成。
- iOS 7 Cell（LabelCell / CommandCell / ButtonCell / SwitchCell / CheckboxCell / RadioCell / SimpleCheckCell）: `isVisible: Bool = true` + `VisibilityAware` 準拠 + `Hashable` + `withDSLID` / `withStyle` 経路で保持。確認済み。
- Android 7 Cell: `isVisible: Boolean = true` + `VisibilityAware` 準拠。data class `copy()` 経由で保持。確認済み。
- `KsSettingsViewController.swift`: `computeVisibleSections(from:)` で `Section.isVisible` / `VisibilityAware.isVisible` フィルタを実装。visible projection が snapshot / layout / separator / supplementary view の参照元。部分 Diff の index は model 配列基準で解釈。hidden 対象は no-op。`applyReplaceCell` で visibility 切替を snapshot チェックより先に検出し Full フォールバック。`applyReplaceSection` は常に Full 経路。layout mode 再評価も visible projection 基準。
- `KsSettingsView.kt`: `flatten()` で `Section.isVisible` / `VisibilityAware.isVisible` フィルタを実装。`applyReplaceCell` で `internalRoot` から旧 Cell を取得し visibility 切替を先に検出して Full フォールバック。`applyReplaceSection` は Full 経路。
- `ios/.../DSLDiffCalculator.swift`: `containsVisibilityChange(from:to:)` が冒頭で preflight 実施。可視性変化時 `.full(newRoot)` のみ発行。
- `android/.../DSLDiffCalculator.kt`: `containsVisibilityChange(from, to)` が `compute` / `contentUpdates` 冒頭で preflight 実施。可視性変化時 `Full(newRoot)` のみ発行、`contentUpdates` は空リスト。
- SwiftUI DSL `SectionBuilder.swift`: Section / 7 Cell ヘルパに `isVisible: Bool = true` 引数追加。
- Compose DSL `BasicCellDsl.kt` / `DSLScope.kt`: Section / 7 Cell ヘルパに `isVisible: Boolean = true` 引数追加。

**テスト確認**:

- Android: `SectionVisibilityTest` / `CellVisibilityTest` / `FlattenVisibilityTest` / `VisibilityApplyDiffTest` / `DSLVisibilityPreflightTest` の全件が failures=0 errors=0（testReleaseUnitTest 結果 XML 確認済み）。
- iOS: `.build` ディレクトリに `SectionVisibilityTests` / `CellVisibilityTests` / `VisibilityProjectionTests` / `DSLVisibilityPreflightTests` のコンパイル済みオブジェクトが存在（2026-06-14 タイムスタンプ）。tasks.md の 12.1 `[x]` チェック済み。

### Coherence

**design.md 遵守**:

- Decision 1（`isVisible` は Core の Section / UI 層の Cell の両方に配置）: Section.swift / Section.kt / 7 Cell すべてで確認。
- Decision 2（`VisibilityAware` を UI 層 opt-in 抽象として配置、Core 抽象 `KsCell` / `Cell` には追加しない）: `KsSettingsViewUI/VisibilityAware.swift` / `ks-settingsview-ui/.../VisibilityAware.kt` に配置、Core 側には追加なし。
- Decision 3（preflight で可視性変化を Full 経路へ）: DSLDiffCalculator 両プラットフォームで実装済み。
- 破壊的変更なし: 既定値 `true` により既存呼び出しは互換維持。

**ソースコメント整合（Suggestion 1 対応済み）**:

- `ios/Sources/` 内のソースコメントに「二層分離」表記なし（`三層分離` に置換済み）。
- Android ソースコメントも同様。
- archive 済み `refactor-display-state-sync` の文言（tasks.md / review-result / verification-report）は変更されていない。

**proposal.md の「What Changes」との対応**:

- Section / 7 Cell への `isVisible: Bool` 追加: 実装済み。
- `VisibilityAware` opt-in 抽象の新規追加: 実装済み。
- 可視性意味論の仕様化: ホスト層 / DSL 層に実装済み。
- `isEnabled` との独立性: セル非描画時に `isEnabled` 視覚効果不発生はテストで検証済み。
- 「三層分離」への rename: delta spec で定義、live spec への反映は `openspec apply` 時に実施。
- DSL diff 算出ロジックの可視性検出規約: 実装済み（preflight）。
- `ReplaceCell` / `ReplaceSection` での visibility 切替 MUST NOT + 防御挙動: 実装済み。
- 部分 Diff の `index` 規約: 実装済み（model 配列基準）。
- DSL に `isVisible` 引数追加: 実装済み。
- 破壊的変更なし: 確認済み。

**Sample アプリ**:

- `samples/ios/KsSettingsViewSample/VisibilityDemoView.swift` 存在確認。
- `samples/android/app/src/main/kotlin/.../VisibilityDemoScreen.kt` 存在確認。

---

## Final Assessment

CRITICAL なし・WARNING なし・SUGGESTION なし。

タスク未完了は `12.3`（実機/シミュレータ目視確認）のみで、これは tasks.md 自体が「ユーザー実施対象」と明示しており、自動検証の対象外。ビルド・自動テストは両プラットフォームで全件成功済み。

**判定: VALID**

全 MUST/SHALL/Scenario の実装が確認され、テストは全件成功、Suggestion 対応（ソースコメント整合）も完了している。archive 可能な状態にある。
