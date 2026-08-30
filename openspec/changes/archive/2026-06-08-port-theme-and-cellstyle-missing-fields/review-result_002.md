# レビュー結果 - port-theme-and-cellstyle-missing-fields (Re-Review)

**レビュー日時**: 2026年06月08日
**レビュワー**: sdd-reviewer
**変更提案ID**: port-theme-and-cellstyle-missing-fields
**前回レビュー結果**: `review-result_001.md` （CHANGES_REQUESTED）

## サマリー

前回 `CHANGES_REQUESTED` で指摘した Critical 1 件 / Major 1 件 / Minor 数件 / Suggestion について、すべての項目で適切に対応された修正が確認できた。

- **iOS Core テスト**: 83 PASS（前回同等）
- **iOS UI テスト**: 214 PASS（前回 205 → 9 件追加で改善）
- **Android Unit テスト**: 258 PASS（前回 247 → 11 件追加で改善）
- **`openspec validate port-theme-and-cellstyle-missing-fields --strict`**: PASS
- **iOS ビルド**: 成功（`swift build`）
- **Android ビルド**: 成功（`./gradlew :ks-settingsview-ui:test`）

判定: **APPROVED**（Critical / Major / 主要 Minor すべて解消、追加テストが描画反映 Scenario をカバー）。

## 前回指摘の解消確認

### ✅ Critical: `Theme.headerHeight` / `Theme.headerFont` / `Theme.footerFont` / `Theme.headerFontSize` / `Theme.footerFontSize` の描画反映が未実装 — **解消**

**iOS 確認**:
- `KsSettingsViewController.makeHeaderBoundaryItem` のシグネチャに `theme: Theme? = nil` が追加され、`section.headerHeight <= 0 && theme.headerHeight > 0` のとき `.absolute(CGFloat(theme.headerHeight))` を返す経路が実装されている（`KsSettingsViewController.swift:461-490`）。
- `sectionAccessoryView` / `rootAccessoryView` 経路で Header/Footer に対し `EffectiveStyle.effectiveHeaderFont(theme:)` / `effectiveFooterFont(theme:)` を介した font 反映が行われている（`KsSettingsViewController.swift:783-815`）。
- `applyAccessoryToListCell` / `applyAccessoryLabel` で UILabel.font に注入される（`KsSettingsViewController.swift:1452-1460`）。
- 旧来の estimated(20) fallback は `section.headerHeight <= 0 && theme.headerHeight <= 0` のときのみに退避。

**Android 確認**:
- `SectionTextAccessoryViewHolder.bind` で `Section.headerHeight = -1.0` のとき `Theme.headerHeight > 0` を fallback として `layoutParams.height = (theme.headerHeight * density).toInt()` に反映（`SectionAccessoryViewHolders.kt:79-98`）。
- `applyHeaderFooterFont` 経由で `Theme.headerFont/footerFont` → Typeface 変換、`headerFontSize/footerFontSize > 0` のとき `TypedValue.COMPLEX_UNIT_SP` で textSize 上書き（`SectionAccessoryViewHolders.kt:108-123`）。
- `RootTextAccessoryViewHolder` も同経路を使用（`SectionAccessoryViewHolders.kt:204-218`）。

**テスト確認**:
- iOS: `test_makeHeaderBoundaryItem_section未指定_themeHeaderHeight指定で_absoluteになる` / `test_makeHeaderBoundaryItem_section明示はthemeより優先される` / `test_makeHeaderBoundaryItem_section未指定_theme未指定なら従来estimated20` の 3 シナリオが追加（`KsSettingsViewControllerTests.swift:395-449`）。
- Android: `SectionAccessoryRenderingTest` に「Section.headerHeight=-1 → Theme.headerHeight fallback」「Section 明示が Theme より優先」「Theme.headerFont の Typeface/textSize 反映」「headerFontSize/footerFontSize による textSize 反映」など 5 シナリオが追加。

### ✅ Major: `EffectiveStyle.effectiveButtonTitleColor` ヘルパと `ButtonCellView.resolvedBaseColor` の二重実装、既定色不整合 — **解消**

**iOS 確認**:
- `ButtonCellView.resolvedBaseColor` は削除され、本番描画は `EffectiveStyle.effectiveButtonTitleColor(buttonCellTitleColor:cellStyle:theme:)` を直接呼ぶ（`ButtonCellView.swift:62-66`）。
- 4 段目の既定が `Theme.defaultButtonTitleColor = .systemBlue` に統一（`Theme.swift:269`, `EffectiveStyle.swift:251`）。テストの期待値も `.systemBlue` に更新済み（`test_effectiveButtonTitleColor_全てnilならsystemBlue`）。

**Android 確認**:
- `EffectiveStyle.Companion.effectiveButtonTitleColorArgb(view, ...)` が新設され、View 系の 4 段目は `MaterialColors.getColor(view, R.attr.colorPrimary, SYSTEM_BLUE_ARGB)` で動的解決する（`EffectiveStyle.kt:402-417`）。
- `ButtonCellViewHolder.bind` は 1 行呼び出し（`ButtonCellViewHolder.kt:37-42`）に統一。
- Compose 経路向け `effectiveButtonTitleColor(...)` は 4 段目 `Theme.DEFAULT_BUTTON_TITLE_COLOR = Color(0xFF007AFF)` （iOS `.systemBlue` 等価）（`EffectiveStyle.kt:380-389`）。

### ✅ Minor: Android `EffectiveStyle` のロジック二重実装 — **解消**

- `EffectiveStyle.from()` 内の値解決はすべて Companion アクセサ（`effectiveTitleColor` / `effectiveBackgroundColor` / `effectiveAccentColor` / `effectiveValueTextColor` / `effectiveValueTextFont` / `effectiveDescriptionColor` / `effectiveDescriptionFont` / `effectiveTitleFont` / `effectiveCellHeightDp` 等）に置換され、SoT が Companion 側に集約された（`EffectiveStyle.kt:81-158`）。
- `from()` は「Compose 論理型 → Android View プラットフォーム型（ARGB Int / Typeface / sp Float）変換」のみを担う責務分離が達成されている。

### ✅ Minor: `applyViewBackgroundColor` リネーム — **解消**

- iOS の private メソッド名は `applyBackgroundColor(theme:)` に変更され、呼び出し元 3 箇所も更新されている（`KsSettingsViewController.swift:233, 275, 988`）。
- 残存する `viewBackgroundColor` の語は「旧名からのリネーム経緯を記録するコメント／spec 言及／テスト Header コメント」のみで、コード本体の参照は無し。

### ✅ Suggestion: `effectiveHeaderFont` / `effectiveFooterFont` アクセサ追加 — **実装**

- iOS: `EffectiveStyle.effectiveHeaderFont(theme:) -> UIFont` / `effectiveFooterFont(theme:) -> UIFont` を追加。「`headerFont ?? defaultHeaderFooterFont` をベースに `headerFontSize > 0` のとき `withSize` で上書き」を 1 箇所に集約（`EffectiveStyle.swift:263-280`）。
- Android: `EffectiveStyle.Companion.effectiveHeaderFont(theme: Theme) -> TextStyle` / `effectiveFooterFont(theme: Theme) -> TextStyle` / 共通ラッパ `effectiveHeaderOrFooterFont` を追加（`EffectiveStyle.kt:431-462`）。
- 両プラットフォームで 6 ケースの解決テストを追加。

### ➡️ Suggestion: `Theme.rowHeight` (Int) と `Theme.headerHeight` (Double) の型不揃い — **スコープ外保持（妥当）**

本 change のスコープ外として保持される旨が報告されており、Open Question として残されている（design.md の Open Questions 節に該当する内容）。妥当な判断。

---

## 追加品質確認

### 仕様遵守

- spec delta（`specs/settings-view-ios-style/spec.md` / `specs/settings-view-android-style/spec.md`）の MUST/SHALL 要件は全て描画パスまで通っていることを iOS / Android 両方で確認。
- `Theme.headerHeight` の Section fallback、`headerFont` / `headerFontSize` 優先順位、ButtonCell 4 段解決のいずれも spec の Scenario と一致。
- `openspec validate --strict` PASS。

### テスト品質

- スタブではなく実コンポーネント（XCTest の `UIWindow + makeKeyAndVisible`、Android の Robolectric ベース ViewHolder ライフサイクル）を経由した assertion になっている。
- テストカバレッジは前回比で iOS UI +9 / Android +11 件、いずれも本 change で導入された描画反映パスを対象としている。
- 「コメントで言い訳した実質スキップ」「単純な保持確認のみ」のアンチパターンはなし。

### 設計上の細部確認

- Android の `effectiveButtonTitleColorArgb` が `View` を引数に取り `MaterialColors.getColor(view, ...)` で `colorPrimary` を動的解決する設計は、Material3 テーマ前提のアプリと整合しており妥当。Material テーマ未設定アプリでは `SYSTEM_BLUE_ARGB` (#FF007AFF) にフォールバックする保険も入っており、堅牢。
- Compose 経路用 `effectiveButtonTitleColor`（Color を返す）と View 系 `effectiveButtonTitleColorArgb`（Int を返す）の役割分担は KDoc に明記。
- `EffectiveStyle.effectiveHeaderOrFooterFont(theme:isHeader:)` というラッパ関数の存在で Section / Root の H/F 双方から重複なく呼び出せる構造になっており、保守性も良好。

### 残るリスク（実装上の判断、ブロッカーではない）

- **`effectiveButtonTitleColorArgb` の単体テスト** は Compose ヘルパ `effectiveButtonTitleColor` のみが直接テストされ、View 引数版は `ButtonCellViewHolder` 経由の統合テストで間接的に検証される構造。`MaterialColors.getColor` ベースの 4 段目動的解決ロジック自体の直接単体テストはないが、ButtonCell 4 段優先テスト 4 ケースは Compose ヘルパで spec の解決順序 MUST を満たすため、本 change の合格基準上は許容範囲。今後ブランチ間で `colorPrimary` 取得が破綻した場合に検知できないリスクが残るが、Minor 以下の改善余地（将来 PR で補強可能）。

---

## 指摘事項

### 🔵 Suggestion: `effectiveButtonTitleColorArgb` の Material `colorPrimary` 解決パスの直接単体テスト追加（将来改善）

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EffectiveStyle.kt:402-417`

**現状**:
本 change で View 系 4 段目を Material `colorPrimary` 動的解決に変更したが、`EffectiveStyleResolutionTest` ではこの View 引数版を直接アサートしていない（Compose 版 `effectiveButtonTitleColor` の `DEFAULT_BUTTON_TITLE_COLOR` 期待のみ）。

**改善案（任意・将来）**:
Robolectric ベースの単体テストで、Material3 テーマを当てた `View` を渡して `effectiveButtonTitleColorArgb(...)` が `colorPrimary` を返すこと、テーマ未設定時に `SYSTEM_BLUE_ARGB` にフォールバックすることをアサートするテストを 1〜2 件追加すると、4 段目の挙動が将来回帰しても気付ける。

**重要度**: Suggestion（spec MUST には影響しない・本 change の判定には影響しない）。

---

## アクションプラン

- なし（本 change としては解消済み）。
- 上記 Suggestion は本 change のフォローアップ／次回以降の余力時に検討。

---

## 判定結果

**ステータス**: `APPROVED`

**理由**:
- 前回 Critical 指摘（Header/Footer Font/Height の描画反映未実装）は iOS / Android 両方で完全に解消され、3+5 件の描画反映 Scenario テストでカバーされている。
- 前回 Major 指摘（ButtonCell 4 段解決の二重実装）は iOS / Android で SoT 一本化されている。
- 前回 Minor 指摘（Android `EffectiveStyle.from()` の二重実装、`applyViewBackgroundColor` リネーム）はいずれも対応済み。
- Suggestion（`effectiveHeaderFont` / `effectiveFooterFont` アクセサ追加）も実装済み。
- 全テスト（iOS Core 83 / iOS UI 214 / Android 258）が PASS。`openspec validate --strict` も PASS。
- spec delta の MUST/SHALL 要件は全て実装に反映されており、テスト品質も良好。
- 残る Suggestion（View 系 ButtonCell 4 段の直接単体テスト）は spec 要件外であり、本 change の判定には影響しない。

マージ可能。
