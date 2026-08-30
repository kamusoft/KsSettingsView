# Verification Report: refine-basic-cells-sample-layout (Phase 16)

Date: 2026-06-06

## Summary

| Dimension    | Status                                                                        |
|--------------|-------------------------------------------------------------------------------|
| Completeness | Phase 16: 19/19 tasks [x]。全体: 85/90（残 5 件は実機目視確認のみ）。全 Requirement 実装済み |
| Correctness  | 全 Phase 16 Requirement 実装済み。Scenario すべてテストカバー済み               |
| Coherence    | Decision 16-1 / 16-2 / 16-3 すべて実装に正しく反映。パターン一貫性あり          |

---

## Issues

### CRITICAL

なし

### WARNING

なし

### SUGGESTION

なし

---

## 検証観点別詳細

### 1. spec の Requirement / Scenario と実装の一致

**「テキスト accessory 用 supplementary view クラスの選択」Requirement**（16.14 追加）

- `KsSettingsViewController.makeAccessoryListCell` の条件分岐（`accessoryText != nil || accessoryView == nil`）によりテキスト accessory と accessory 未指定を `KsAccessoryReusableView` 経路へルーティング。`accessoryView` 経路のみ `UICollectionViewListCell` を維持。仕様の MUST / MAY と一致。
- テスト: `SectionAccessoryRenderingTests.test_textヘッダのsupplementaryが表示される`（L53）で `is KsAccessoryReusableView` 検証済み。

**「Section.headerHeight の UI 反映」Requirement の Phase 16 追加 Scenario**

- Scenario「headerHeight 正値が AutoLayout 下端揃えと両立する」: `makeHeaderBoundaryItem` が `section.headerHeight > 0` のとき `.absolute(CGFloat(section.headerHeight))` を確実に返すことを `test_makeHeaderBoundaryItem_headerHeight80のとき_absolute80になる` で検証済み。
- Scenario「headerHeight 正値が描画 frame に反映される」: `test_視覚的ヘッダ高さ_headerHeight80指定時_supplementaryのframe高さが80になる` および `_120指定時` で `layoutIfNeeded()` 後の `frame.height` を `accuracy: 0.5` で実測検証済み。

### 2. design.md Decision の実装反映

**Decision 16-1（heightDimension 選択ロジック）**

`KsSettingsViewController.makeHeaderBoundaryItem(for:original:)` が `internal static` に変更済み。`section.headerHeight > 0` → `.absolute(CGFloat(section.headerHeight))`、`headerHeight == -1 && header != nil` → `.estimated(20)`、`headerHeight == -1 && header == nil` → `nil` の三分岐が実装されており、Decision に記載の疑似コードと完全一致。

**Decision 16-2（KsAccessoryReusableView 採用）**

`ios/Sources/KsSettingsViewUI/KsAccessoryReusableView.swift` が新規作成済み。`UICollectionReusableView` 直系サブクラスとして実装され、priority 999 の Auto Layout 制約（AiForms `TextHeaderView.cs` lines 38-46 準拠）、`setVerticalAlignment(_:)`、`prepareForReuse()` をすべて備えている。accessoryView 経路は従来通り `UICollectionViewListCell` を維持（Decision の MAY 条件と一致）。

**Decision 16-3（デッドコード削除）**

- `applyAccessoryLabel` 関数: 実装ファイルに一切存在しない（完全削除済み）。
- `applyAccessoryToListCell` のシグネチャ: `(_ listCell: UICollectionViewListCell, accessoryView: KsAnyView?)` の2引数専用形式に整理済み（`accessoryText` / `verticalAlignment` / `textColor` パラメータなし）。
- `AccessoryVerticalAlignment` enum: `makeAccessoryListCell` → `makeAccessoryReusableView` 経路および `refreshRootSupplementary` のテキスト accessory 経路で `mapVerticalAlignment` を介して引き続き使用されており、残置理由（テキスト accessory 経路での利用継続）が実装で確認できる。

### 3. tasks.md Phase 16.1〜16.19 の完了状況

tasks.md を精査した結果、Phase 16 の 16.1〜16.19 全 19 項目すべてが `[x]` 状態であることを確認。

全体 85/90 タスク完了。残 5 件（#29 / #40 / #45 / #51 / #52）はすべて実機目視確認タスクであり、実装・テスト・ビルドに関わる項目ではない。Phase 16 の成果物（仕様・実装・テスト）の検証には影響しない。

### 4. applyAccessoryLabel / applyAccessoryToListCell テキスト分岐の完全削除

- `KsSettingsViewController.swift` において `applyAccessoryLabel` という文字列はゼロ件（完全削除確認）。
- `applyAccessoryToListCell` は定義 1 件（L1486）、呼び出し 2 件（L805 accessoryView 経路、L1459 refreshRootSupplementary 経路）のみ。すべて `accessoryView` 単引数で呼ばれており、テキスト分岐コードは存在しない。
- テスト `test_view形式ヘッダの差し替えでapplyAccessoryToListCellが新しいcontentConfigurationを設定する` は新シグネチャ（`accessoryView:` のみ）で更新済み。

### 5. AccessoryVerticalAlignment enum の残置妥当性

`AccessoryVerticalAlignment` は `KsSettingsViewController.swift` L864 に定義。以下の 3 箇所で実際に使用されている:

- L760: `makeAccessoryListCell` の `verticalAlignment` パラメータ既定値
- L823: `makeAccessoryReusableView` の `verticalAlignment` パラメータ
- L1450: `refreshRootSupplementary` 内 `let verticalAlignment: AccessoryVerticalAlignment = isFooter ? .top : .bottom`

`mapVerticalAlignment` を介して `KsAccessoryReusableView.VerticalAlignment` に変換される構造。Decision 16-3 の「内部表現と KsAccessoryReusableView 側 API の責務分離」方針通りであり、残置理由は妥当。

### 6. openspec validate --strict の結果

```
Change 'refine-basic-cells-sample-layout' is valid
```

（検証時点での実行結果）

### 7. iOS / Android テスト・ビルドの PASS 記録

review-result_006.md に記録:

- iOS swift test（macOS）: 154 tests passed, 0 failures
- iOS xcodebuild test（iPhone 17 / iOS 26.5 Simulator）: 170 tests passed, 0 failures
  - `test_視覚的ヘッダ高さ_headerHeight80指定時_supplementaryのframe高さが80になる` PASS
  - `test_視覚的ヘッダ高さ_headerHeight120指定時_supplementaryのframe高さが120になる` PASS
  - `test_makeHeaderBoundaryItem_*` 4 件 PASS
- Android Gradle test: BUILD SUCCESSFUL
- Android Sample assembleDebug: BUILD SUCCESSFUL
- iOS Sample build（iPhone 17 Simulator）: BUILD SUCCEEDED

---

## Final Assessment

CRITICAL なし、SUGGESTION なし。

Phase 16（iOS Section.headerHeight 反映修正 + KsAccessoryReusableView 導入 + Minor-1 デッドコード削除）の仕様と実装は完全に一致している。

**判定: VALID**
