# 検証レポート - refine-basic-cells-style（Phase 17 最終形）

**変更提案**: refine-basic-cells-style  
**検証対象**: Phase 17（Theme.titleColor / Theme.titleFont 追加）を含む最終形  
**検証日**: 2026-06-03  
**前回レポート**: verification-report_001.md（Phase 1〜16 VALID 確認済み）

---

## Summary

| 次元 | 状態 |
|------|------|
| Completeness | Phase 17 全タスク（17.1〜17.15）すべて `[x]`、全 Phase（1〜17）完了 |
| Correctness | 全 Requirement の実装が確認済み。フラグ判定式・優先順位・テストカバレッジ一致 |
| Coherence | design.md Decision 12 に設計意図が明記。実装と整合している |

---

## 検証内容

### 1. タスク完了確認

tasks.md の Phase 17（17.1〜17.15）全タスクが `[x]` であることを確認した。Phase 1〜16 も `[x]` のままであることを確認した。

### 2. 仕様変更の実装整合性

#### (a) Theme.titleColor / Theme.titleFont の追加

- **仕様**: `settings-view-core/spec.md` の "Theme 型" Requirement に `titleColor: KsColor?`（既定 `nil`）と `titleFont: KsFont?`（既定 `nil`）の MUST 追加。
- **iOS実装**: `ios/Sources/KsSettingsViewCore/Theme.swift:77-83` に `titleColor: KsColor?` / `titleFont: KsFont?` を末尾追加。デフォルト `nil`。既存呼び出しのシグネチャ互換を維持。
- **Android実装**: `android/ks-settingsview-core/.../Theme.kt:91-99` に同等フィールドを末尾追加。デフォルト `null`。
- **判定**: 仕様と実装が一致している。

#### (b) EffectiveStyle の 3 段階優先順位

- **仕様**: `settings-view-ios-ui/spec.md` および `settings-view-android-ui/spec.md` の "Theme / CellStyle の UIKit 変換" / "Android 変換" Requirement にて「CellStyle.titleColor → Theme.titleColor → プラットフォーム既定」の 3 段階優先順位 MUST。
- **iOS実装**: `ios/Sources/KsSettingsViewUI/EffectiveStyle.swift:87-93` で `if let kc = cellStyle.titleColor { ... } else if let kc = theme.titleColor { ... } else { UIColor.label }` の 3 段階実装。titleFont も同等（line 94-100）。
- **Android実装**: `android/ks-settingsview-ui/.../EffectiveStyle.kt:84-88` で `when { cellStyleTitleColor != null → ... themeTitleColor != null → ... else → resolveDefaultTitleColor(context) }` の 3 段階実装。titleFont も同等（line 96-98）。
- **判定**: 仕様と実装が一致している。

#### (c) titleColorIsExplicit フラグ

- **仕様**: `settings-view-ios-ui/spec.md` および `settings-view-android-ui/spec.md` で「`cellStyle.titleColor != nil || theme.titleColor != nil` のとき `true`」の MUST。
- **iOS実装**: `EffectiveStyle.swift:102` の `self.titleColorIsExplicit = (cellStyle.titleColor != nil) || (theme.titleColor != nil)` が仕様の判定式と完全一致。
- **Android実装**: `EffectiveStyle.kt:89-90` の `titleColorIsExplicit: Boolean = cellStyleTitleColor != null || themeTitleColor != null` が仕様と一致。
- **判定**: 仕様と実装が完全一致している。

#### (d) ButtonCell の 4 段階 baseColor 解決

- **仕様**: `settings-view-ios-ui/spec.md` の "ButtonCell の baseColor 解決順序" Requirement：「Cell 個別 titleColor → CellStyle.titleColor → Theme.titleColor → UIColor.systemBlue」の 4 段階 MUST。
- **iOS実装**: `ios/Sources/KsSettingsViewUI/ButtonCellView.swift:123-131`（`resolvedBaseColor`）で `cell.titleColor != nil → UIColor(ksColor:)` / `effective.titleColorIsExplicit → effective.titleColor` / `else → .systemBlue` の 3 分岐で 4 段階を実現。CellStyle と Theme の判定は `titleColorIsExplicit` に集約されており仕様の設計意図（design.md Decision 12 参照）に沿っている。
- **Android実装**: `ButtonCellViewHolder.kt:43-51` で `cell.titleColor != null → cell.titleColor.toColorInt()` / `effective.titleColorIsExplicit → effective.titleColor` / `else → MaterialColors.getColor(...colorPrimary...)` の 3 分岐で 4 段階を実現。
- **判定**: 仕様と実装が一致している。

#### (e) disabled 時の disabledTextColor が baseColor より優先される

- **仕様**: `settings-view-ios-ui/spec.md` の "ButtonCell の baseColor 解決順序" Requirement「isEnabled = false のときは disabledTextColor を用いる MUST」。Phase 6 で実装された挙動が Phase 17 でも保たれているか確認。
- **iOS実装**: `ButtonCellView.swift:65` の `titleLabel.textColor = btn.isEnabled ? baseColor : effective.disabledTextColor` が Phase 17 修正後も維持されている。
- **Android実装**: `ButtonCellViewHolder.kt:53` の `val color = if (cell.isEnabled) baseColor else effective.disabledTextColor` が維持されている。
- **判定**: Phase 6 の挙動が Phase 17 でも保たれている。

### 3. 全 7 種 Cell での Theme.titleColor 反映

- **仕様**: `cell-types-basic/spec.md` の "全 Cell 共通の Theme.titleColor / Theme.titleFont 反映" Requirement 「全 7 種 Cell で MUST」。
- **テストカバレッジ**:
  - iOS `BasicCellsTests.swift`（line 520〜633）: LabelCell / CommandCell / SwitchCell / CheckboxCell / RadioCell / SimpleCheckCell の 6 種で Theme.titleColor 反映テストを直接確認。ButtonCell は同ファイル line 485〜499 の `test_ButtonCellView_baseColor_Theme_titleColor優先` で担保。全 7 種。
  - Android `BasicCellsTest.kt`（line 877〜963）: 同等の 7 種テストが全件存在。
- **テスト結果**: Android `BasicCellsTest` が tests=66、failures=0（タイムスタンプ 2026-06-03T11:04:53）で PASS 確認済み。
- **判定**: 全 7 種 Cell で MUST 条件が担保されている。

### 4. ButtonCell 4 段階優先順位の単体テスト網羅

- **iOS**: `BasicCellsTests.swift:460-516` の 5 テストで「Cell 個別 titleColor 優先」「CellStyle.titleColor 優先」「Theme.titleColor 優先（Phase 17 新規）」「全て未指定は systemBlue」「isEnabled=false で disabledTextColor 優先」を網羅。
- **Android**: `BasicCellsTest.kt:813-872` の 5 テストで同等の網羅確認。
- **判定**: 4 段階の各段階がすべて単体テストで担保されている。

### 5. Theme.titleColor / Theme.titleFont のデフォルト値（nil / null）

- **iOS Core テスト**: `ThemeTests.swift:132-163` に「titleColor 既定は nil」「titleFont 既定は nil」「明示指定できる」「等価性確認」の 4 テストが存在。
- **Android Core テスト**: `ThemeTest.kt`（testReleaseUnitTest）が tests=18、failures=0（タイムスタンプ 2026-06-03T10:50:08）でPASS。`titleColor: 既定は null` / `titleFont: 既定は null` のテストケースを含む。
- **判定**: デフォルト値 nil / null が実装・テストともに確認できる。

### 6. EffectiveStyle の 4 シナリオテスト

- **仕様要件**: spec の Scenario「Theme.titleColor のみ指定」「CellStyle.titleColor のみ指定」「両方指定（CellStyle 優先）」「両方未指定（fallback）」の 4 ケースと `titleColorIsExplicit` フラグ。
- **iOS**: `EffectiveStyleTests.swift:150-228` に 4 ケース全て + titleFont の合成テストが存在。
- **Android**: `EffectiveStyleTest.kt:204-266` に 4 ケース全て + titleFont テストが存在。tests=17、failures=0（タイムスタンプ 2026-06-03T11:04:54）でPASS。
- **判定**: 全シナリオが担保されている。

### 7. Sample での titleColor 反映

- **iOS Sample**: `samples/ios/.../BasicCellsDemoView.swift` に `titleColor: mauiTitleText`（MAUI TitleColor 相当、`#CC9900`）を Theme に設定する記述を確認（line 80）。
- **Android Sample**: `samples/android/.../BasicCellsDemoScreen.kt` に `titleColor = MAUI_TITLE_TEXT`（line 237）を確認。
- **判定**: 17.13 の要件が満たされている。

### 8. design.md Decision 12 の存在

- `design.md:183-207` に Decision 12「Theme.titleColor / Theme.titleFont の追加とフォールバック順序」が追加されており、3 段階優先順位・4 段階 ButtonCell baseColor・`titleColorIsExplicit` フラグの設計根拠が明記されている。
- **判定**: 設計文書が実装と整合している。

### 9. Android Compose flaky テスト（Major M-1）の取り扱い

- 問題: `KsSettingsViewComposeTest > DSL 方式で外部 state を 2 回連続更新しても 2 回目の追加が反映される`。
- ソース確認: 当該テストには `titleColor` / `titleFont` / `EffectiveStyle` / `ButtonCellView` への参照が存在しない。commit `142e6ad` 由来の既存テストであり、Phase 17 では当該ファイルを一切変更していない。
- テスト結果: 直近の testDebugUnitTest（2026-06-03T19:50）では tests=6、failures=0 で PASS している（flaky のため常時再現するわけではない）。
- **判定**: Phase 17 のコード変更とは独立しており、Phase 17 の品質評価には影響しない。別 change での対応が適切である（review-result_005.md の方針と一致）。

### 10. tasks.md 全 Phase 完了確認

- Phase 1〜17 の全タスクが `[x]` であることを実際に tasks.md を読んで確認した。

---

## Issues

### CRITICAL

なし

### WARNING

なし

### SUGGESTION

なし

---

## 最終判定

**VALID**

CRITICAL / WARNING / SUGGESTION のいずれも検出されなかった。Phase 17 で追加されたすべての仕様変更（Theme.titleColor / titleFont の追加・3 段階優先順位・4 段階 ButtonCell baseColor・titleColorIsExplicit フラグ・全 7 種 Cell への反映）が iOS / Android 両プラットフォームで正しく実装され、十分なテストカバレッジで担保されていることを確認した。

Android Compose flaky テスト（Major M-1）は Phase 17 のスコープ外の既存問題であり、Phase 17 の VALID 判定の妨げにはならない。アーカイブへ進める状態にある。
