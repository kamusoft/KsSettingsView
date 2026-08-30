# 検証レポート: refine-basic-cells-sample-layout（Phase 18 完了時点）

検証日: 2026-06-07
検証対象 Phase: Phase 18（Phase 16 機構の revert / B 案）

---

## サマリースコアカード

| 次元           | 状態                                          |
| -------------- | --------------------------------------------- |
| Completeness   | Phase 18 タスク 18.1〜18.14 全 [x] / 未完了は実機目視確認のみ |
| Correctness    | 全 Requirement に対応する実装を確認 / 乖離なし |
| Coherence      | design.md の全 Decision と実装が一致           |

---

## 観点別検証結果

### 観点 1: revert 漏れの確認

**KsAccessoryReusableView.swift の削除**

`ios/Sources/KsSettingsViewUI/` ディレクトリに `KsAccessoryReusableView.swift` が存在しないことを確認した（ls 出力に当該ファイルなし）。

**KsSettingsViewController.swift の Phase 16 由来分岐削除**

- `makeAccessoryReusableView` 関数: 存在しない（grep で 0 件）
- `mapVerticalAlignment` 関数: 存在しない（grep で 0 件）
- `KsAccessoryReusableView` への実体参照: grep 検索でコメント内経緯記録のみ（実体参照 0 件）

**applyAccessoryLabel / applyAccessoryToListCell の Phase 15.1 同等復元**

- `applyAccessoryToListCell(_:accessoryText:accessoryView:textColor:verticalAlignment:)` シグネチャが存在（`KsSettingsViewController.swift:1424`）
- `applyAccessoryLabel(_:text:textColor:verticalAlignment:)` が存在（`KsSettingsViewController.swift:1474`）
- UILabel + AutoLayout 制約（priority 999、Header = bottomAnchor / Footer = topAnchor）が実装されていることをコード内容から確認
- `AccessoryVerticalAlignment` enum が維持されている（`KsSettingsViewController.swift:803`）

**SectionAccessoryRenderingTests の UICollectionViewListCell 経路復元**

- `is UICollectionViewListCell` 検証が存在（`SectionAccessoryRenderingTests.swift:50`）
- `listCell.contentView.subviews.compactMap { $0 as? UILabel }.first?.text` 形式の検証が存在（`SectionAccessoryRenderingTests.swift:156`）
- `is KsAccessoryReusableView` の記述なし（grep で 0 件）

### 観点 2: Decision 16-2 / 16-3 の「revert 済み」付記

- `design.md` の Decision 16-2 見出しに「— **Phase 18 で revert 済み**」が付記されている（`design.md:398`）
- `design.md` の Decision 16-3 見出しに「— **Phase 18 で revert 済み**」が付記されている（`design.md:458`）
- 両 Decision 本体に `> **Phase 18 で revert**:` 書き出しの説明文が追記されている

### 観点 3: B 案副次改善の維持

**makeHeaderBoundaryItem の internal static 化**

`KsSettingsViewController.swift:420` で `internal static func makeHeaderBoundaryItem(for:original:)` が定義されていることを確認。

**純粋ロジックテスト 4 件の維持**

`KsSettingsViewControllerTests.swift` に以下の 4 件が存在することを確認:
- `test_makeHeaderBoundaryItem_headerHeight80のとき_absolute80になる`（行 322）
- `test_makeHeaderBoundaryItem_headerHeight未指定_header非空のとき_estimated20になる`（行 344）
- `test_makeHeaderBoundaryItem_headerHeight未指定_header_nilのとき_nilを返す`（行 362）
- `test_makeHeaderBoundaryItem_headerHeight40_header_nilでも_absolute40になる`（行 375）

**Decision 16-1 の維持**

`design.md` に Decision 16-1「iOS の Header heightDimension 選択ロジック（.absolute vs .estimated）」が記述されており、revert 対象外として維持されている。

### 観点 4: Phase 17 機構の無傷確認

**KsListCellBase.swift の存在**

`ios/Sources/KsSettingsViewUI/KsListCellBase.swift` が存在し、`preferredLayoutAttributesFitting(_:)` の override が実装されている（`KsListCellBase.swift:46-50`）。

**全 7 種 Cell View の KsListCellBase 継承**

- `LabelCellView: KsListCellBase` 確認
- `CommandCellView: KsListCellBase` 確認
- `ButtonCellView: KsListCellBase` 確認
- `SwitchCellView: KsListCellBase` 確認
- `CheckboxCellView: KsListCellBase` 確認
- `RadioCellView: KsListCellBase` 確認
- `SimpleCheckCellView: KsListCellBase` 確認

**視覚的セル高さ検証テストの維持**

- `test_視覚的セル高さ_cellHeight80指定時_セルのframe高さが80になる`（行 430）
- `test_視覚的セル高さ_cellHeight120指定時_セルのframe高さが120になる`（行 456）

**Phase 16 視覚的ヘッダ高さ検証テストの削除**

- `test_視覚的ヘッダ高さ_headerHeight80指定時_supplementaryのframe高さが80になる`: 存在しない（grep で 0 件）
- `test_視覚的ヘッダ高さ_headerHeight120指定時_supplementaryのframe高さが120になる`: 存在しない（grep で 0 件）
- `measuredSectionHeaderHeight(for:section:containerSize:)` ヘルパ: 存在しない（grep で 0 件）

### 観点 5: tasks.md の Phase 18（18.1〜18.14）完了確認

Phase 18 の全タスク（18.1〜18.14）が `[x]` になっていることを確認した。

未完了タスクは以下 5 件のみだが、いずれも実機目視確認（自動検証不可）のタスクであり、Phase 18 の作業範囲外:
- 7.4（Android Switch 実機目視）
- 10.4（iOS Sample シミュレータ目視）
- 11.5（Android Sample エミュレータ目視）
- 13.1（iOS 実機目視確認）
- 13.2（Android 実機目視確認）

これらはコメント欄に「実機目視確認（13.x）で対応」と明記されており、コード実装は完了済みである。

### 観点 6: openspec validate --strict

```
openspec validate refine-basic-cells-sample-layout --strict
> Change 'refine-basic-cells-sample-layout' is valid
```

valid を返すことを確認した。

### 観点 7: テスト・ビルド全件 PASS の記録

`review-result_008.md` および `tasks.md` に以下の記録がある:

| 検証項目 | 結果 |
| -------- | ---- |
| iOS Swift Package test（macOS）| 154 tests passed |
| iOS `xcodebuild test`（Simulator iPhone 17） | Executed 170 tests, with 0 failures / TEST SUCCEEDED |
| Android `:ks-settingsview-*:test` | BUILD SUCCESSFUL |
| Android Sample `:app:assembleDebug` | BUILD SUCCESSFUL |
| iOS Sample `xcodebuild build` | BUILD SUCCEEDED |

iOS Simulator 170 tests に Phase 17 の視覚的セル高さ検証テスト 2 件と Phase 16 副次改善の純粋ロジックテスト 4 件が含まれており、Phase 18 の revert が Phase 17 機構を破壊していないことが実測で保証されている。

### 観点 8: Sample CommandCell の headerHeight = 60（iOS / Android）と spec の整合

**iOS Sample**

`samples/ios/KsSettingsViewSample/BasicCellsDemoView.swift:85` に `Section("CommandCell", headerHeight: 60)` が存在する。

**Android Sample**

`samples/android/.../BasicCellsDemoScreen.kt:71` に `Section(header = "CommandCell", headerHeight = 60.0)` が存在する。

**specs との整合**

- `specs/samples-ios/spec.md` の「Section.headerHeight 明示指定のサンプル」Scenario に `headerHeight = 60` の記述が存在（Phase 18 で 80 から 60 に戻した旨の注記あり）
- `specs/samples-android/spec.md` の同 Scenario に `headerHeight = 60.0` の記述が存在（同様の注記あり）
- `specs/settings-view-ios-ui/spec.md` の「Section.headerHeight の UI 反映」Requirement の AutoLayout 下端揃え両立 Scenario に `headerHeight: 60` が存在（Phase 18 で 80 から 60 に変更済み）

3 箇所すべてで spec と実装が一致している。

### 観点 9: プロジェクトルール遵守確認

- 日本語コメント: `KsSettingsViewController.swift` のコメントが日本語で記述されていることを確認
- `Theme.Material3.*`: Android 側の要件（settings-view-android-ui のテーマ要件）は本 Phase の revert 範囲外であり変更なし
- `onDrawOver`: `ClassicSectionDecoration.kt` の `onDrawOver` は本 Phase の revert 範囲外であり変更なし
- `replaceCells`: Android バッチ更新は本 Phase の revert 範囲外であり変更なし

---

## 問題一覧

### CRITICAL

なし

### WARNING

なし

### SUGGESTION

なし

---

## 最終判定

**VALID**

Phase 18 で要求された全 revert（`KsAccessoryReusableView.swift` 削除、Phase 16 由来分岐削除、`applyAccessoryLabel` / `applyAccessoryToListCell` 復元、視覚的ヘッダ高さ検証テスト削除、`SectionAccessoryRenderingTests` 復元）が完全に実施されており、B 案で維持すべき副次改善（`makeHeaderBoundaryItem` の `internal static` 化、純粋ロジックテスト 4 件、Decision 16-1）も実装に残っている。Phase 17 機構（`KsListCellBase` / `preferredLayoutAttributesFitting`）は無傷で維持されており、iOS Simulator 170 tests PASS により実測で保証されている。Sample の headerHeight は iOS / Android ともに 60 で一致し、全 delta spec と実装が整合している。`openspec validate --strict` も valid を返した。
