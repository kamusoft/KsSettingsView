# レビュー結果 - refine-basic-cells-sample-layout (Phase 17)

**レビュー日時**: 2026年06月07日  
**レビュワー**: sdd-reviewer  
**変更提案ID**: refine-basic-cells-sample-layout  
**対象フェーズ**: Phase 17 — iOS Cell.cellHeight 反映修正  
**判定結果**: **APPROVED**

---

## サマリー

Phase 17 は「個別 Cell の `CellStyle.cellHeight` 指定値が iOS で実 frame に反映されない」というオーナー三次実機目視（Image #12/#13）の核心指摘に対する正式対応である。

**実装方針の妥当性**: AiForms.Maui.SettingsView オリジナル `SettingsTableSource.GetHeightForRow`（`UITableView.heightForRowAt` 経由で `cell.Height` を CGFloat 直接返却）の意味論を `UICollectionView` + `UICollectionLayoutListConfiguration` 環境で再現するため、`UICollectionViewListCell.preferredLayoutAttributesFitting(_:)` を override する選択は iOS 公式 API として正しい。`UIListContentConfiguration` の intrinsic（priority 1000）に `contentView.heightAnchor` 制約（priority 999）が負ける問題に対し、レイアウト属性そのものを補正することで priority 競合を回避する設計は妥当。

**共通基底クラス `KsListCellBase` の導入**: 7 種 Cell View にそれぞれ `preferredLayoutAttributesFitting` と `installSelectedColorHandler` を重複実装するのを避け、保守性を確保している。継承元変更に伴うサブクラス側の `init` 簡略化・`init?(coder:)` の重複削除も適切。

**テスト**: `measuredCellHeight(for:indexPath:containerSize:)` ヘルパで `UICollectionView.layoutIfNeeded()` 後の `cellForItem(at:)?.frame.height` を実測する 2 ケース（80pt / 120pt）を追加。80pt 単独だと「偶然 intrinsic ≈ 80」だった場合の擬陽性を排除できないが、120pt の追加で任意指定値の反映を回帰保証している点が良い。

**ビルド・テスト・openspec 検証結果**:
- iOS swift test (macOS): 154 tests passed
- iOS xcodebuild test (iPhone 17 / iOS 26.1 Simulator): **172 tests passed, 0 failures**
- Android `./gradlew :ks-settingsview-core:test :ks-settingsview-ui:test :ks-settingsview-compose:test`: BUILD SUCCESSFUL
- `openspec validate refine-basic-cells-sample-layout --strict`: Change is valid

**仕様ドキュメントの整合性**: spec.md / design.md / tasks.md / proposal.md の記述は実装と一致しており、Phase 17 で追加された Requirement / Scenario も網羅されている。Phase 16 の `KsAccessoryReusableView` 機構との並列維持の方針も明示されている。

---

## 指摘事項

### 🔵 Suggestion-1: `applyEffectiveHeight` 末尾のコメントが意図を伝えていない

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsCellViewSupport.swift:135-137`

**問題点**:

```swift
listCell.setNeedsLayout()
// preferredLayoutAttributesFitting で参照するために高さを再記録（applyEffectiveHeight 直後は
// 上の `s.lastHeight = newHeight` で更新済み。ここでは明示的なメモを残す目的）
```

このコメントは「再記録のためのメモ」とだけ書かれているが、コード上は再記録処理が無く、`s.lastHeight = newHeight` が既に上で実行済みである旨を改めて述べているだけの「メモ書きのメモ」になっており、読者は「次に何かが続く？」と一瞬迷う。意図を明示するなら、

> `s.lastHeight` は上の処理で更新済みなので、ここでは `setNeedsLayout()` で `preferredLayoutAttributesFitting(_:)` の再評価を促すのみとする。

のように `setNeedsLayout()` を中心に説明し、残骸的なコメントを残さない方が良い。

**推奨修正**:

```swift
// 高さ制約を更新したので、次回 layout 時に preferredLayoutAttributesFitting(_:) が
// 新しい lastHeight に基づいて attributes を補正できるよう layout pass を促す。
listCell.setNeedsLayout()
```

優先度: Low（コードの正しさには影響しない）。

---

### 🔵 Suggestion-2: 固定高さモード（`hasUnevenRows == false`）の視覚的セル高さ検証テストが不足

**該当箇所**: `ios/Tests/KsSettingsViewUITests/KsSettingsViewControllerTests.swift:521-567`

**問題点**:

Phase 17 で追加された 2 ケースは両方とも `Theme(hasUnevenRows: true)`（可変高さモード）。spec.md の "cellHeight 80 指定時にセルの描画高さが 80pt 以上になる" Scenario は可変モード前提で記述されているが、`adjustedLayoutAttributes` には固定モード（`lastIsFixedHeight == true` → 厳密に `desired` に揃える）の分岐も実装されている。固定モードの分岐は `Theme(hasUnevenRows: false)`（既定）で動作するが、Phase 17 のテストではカバーされていない。

`hasUnevenRows == false` 時の固定モード分岐（`size.height = desired`）が回帰した場合、本テストでは検出できない。

**推奨修正**: 以下のような固定モード版テストを 1 つ追加するとカバレッジが完結する。

```swift
func test_視覚的セル高さ_hasUnevenRowsFalse時_cellHeight指定値で固定される() {
    let theme = Theme(hasUnevenRows: false)
    let section = Section(
        cells: [
            CommandCell(style: CellStyle(cellHeight: 100), title: "Fixed Row")
        ]
    )
    let root = SettingsRoot(sections: [section], theme: theme)
    guard let measured = measuredCellHeight(for: root, indexPath: IndexPath(item: 0, section: 0)) else {
        XCTFail("セル frame.height が取得できない")
        return
    }
    // 固定モードは「厳密に」desired に揃える契約
    XCTAssertEqual(measured, 100, accuracy: 0.5,
        "hasUnevenRows = false で cellHeight = 100 指定時、frame.height は厳密に 100pt（許容誤差 ±0.5pt）")
}
```

優先度: Low（既存テスト 2 つで主要回帰は捕捉できる。固定モード回帰のセーフティネットとして任意追加）。

---

### 🔵 Suggestion-3: `super.preferredLayoutAttributesFitting` 呼び出しの根拠コメントを 1 行追加すると親切

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsListCellBase.swift:46-51`

**問題点**:

`super.preferredLayoutAttributesFitting(layoutAttributes)` を呼んでから補正する順序は Apple 推奨パターンだが、Phase 17 で導入された API なので「なぜ super 呼び出しが必要か」を将来の保守者向けに 1 行入れておくと安心。実装は問題ない。

**推奨修正**:

```swift
override func preferredLayoutAttributesFitting(
    _ layoutAttributes: UICollectionViewLayoutAttributes
) -> UICollectionViewLayoutAttributes {
    // まず super に intrinsic-based fitting を計算させる（Apple 推奨）。
    // その結果（proposed）の size.height を adjustedLayoutAttributes で補正する。
    let base = super.preferredLayoutAttributesFitting(layoutAttributes)
    return KsCellViewSupport.adjustedLayoutAttributes(self, proposed: base)
}
```

優先度: Very Low（補足情報。実装は正しい）。

---

## レビュー観点別評価

### 1. AiForms オリジナル参照の妥当性 ✅
`SettingsTableSource.cs:113-135` の `GetHeightForRow` 引用は正確で、`UITableView.heightForRowAt` 経路に対する `UICollectionView` 系の同等経路として `preferredLayoutAttributesFitting(_:)` を選択しているのは正しい。Apple 公式 API の用途とも整合する。

### 2. `KsListCellBase` の設計 ✅
- `UICollectionViewListCell` 直系で、`init(frame:)` で `installSelectedColorHandler` を呼ぶことで重複を排除している。
- `preferredLayoutAttributesFitting(_:)` の override は `super` を最初に呼ぶ Apple 推奨パターンに従っている。
- `init?(coder:)` を `@available(*, unavailable)` で基底に集約。

### 3. `adjustedLayoutAttributes` のロジック ✅
- `lastHeight == nil` → proposed をそのまま返す（render 前の初回 layout で安全）。
- `lastIsFixedHeight == true` → 厳密固定。
- `lastIsFixedHeight == false` → `max(proposed, lastHeight)` で下限保証。
- `applyEffectiveHeight` の意味論（固定 vs 最低高さ保証）と整合している。
- `proposed.copy()` で attributes の他フィールド（indexPath / zIndex / center / frame 等）を保持しているのも正しい。

### 4. 7 種 Cell View の継承元変更 ✅
- `LabelCellView` / `CommandCellView` / `CheckboxCellView` / `RadioCellView` / `SimpleCheckCellView` は `init(frame:)` を持たず、基底クラスから継承するのみ。
- `ButtonCellView.init(frame:)` は `super.init(frame:)` 後に `titleLabel` 配置のみ追加。`installSelectedColorHandler` は基底で実行済み。
- `SwitchCellView.init(frame:)` は `super.init(frame:)` 後に `toggle.addTarget` のみ追加。
- 重複削除されたコードはすべて基底クラスでカバーされており、副作用の漏れはない。

### 5. `installSelectedColorHandler` の呼び出しタイミング ✅
`UICollectionViewListCell.init(frame:)` → `KsListCellBase.init(frame:)` → `super.init(frame:)` の直後に `installSelectedColorHandler(self)` を呼ぶ。`configurationUpdateHandler` の closure は `[weak listCell]` で循環参照を避けつつ、closure 内で都度 `state(listCell)` を引くため、サブクラス特有の init（`ButtonCellView` の `titleLabel` 配置等）と競合しない。

### 6. 視覚的高さ検証テスト ✅
- `measuredCellHeight` ヘルパで `UICollectionView.layoutIfNeeded()` を 2 回呼んでいる（`setNeedsLayout` 挟みで `applyEffectiveHeight` の `setNeedsLayout` をフラッシュするため）のは現実的な対応。
- 80pt 単独だと「偶然 intrinsic ≈ 80」を排除できないが、120pt の追加で任意値の反映を保証する設計は妥当。
- `>= 80 - 0.5` の許容誤差は可変モードの「最低高さ保証」契約と整合する。

### 7. Phase 16 機構との整合性 ✅
- Phase 16 の `KsAccessoryReusableView`（supplementary view 用、`Section.headerHeight` 反映）と Phase 17 の `KsListCellBase`（Cell 本体用、`CellStyle.cellHeight` 反映）は経路が完全に分離されている。
- `Section.headerHeight = 80` の維持と、Sample の `cellHeight = 80` の維持は両立している。
- 思想（priority 999 → authoritative size 反映）も揃っている。

### 8. テスト全件 PASS ✅
- iOS swift test (macOS): 154 tests passed
- iOS xcodebuild test (iPhone 17 / iOS 26.1 Simulator): **172 tests passed, 0 failures**
- Android `./gradlew :ks-settingsview-core:test :ks-settingsview-ui:test :ks-settingsview-compose:test`: BUILD SUCCESSFUL

### 9. `openspec validate --strict` valid ✅
`Change 'refine-basic-cells-sample-layout' is valid`

### 10. プロジェクトルール遵守 ✅
- 日本語コメント: OK（すべての追加・修正箇所で日本語コメントを使用）。
- Theme.Material3.\*: Android 側変更なしのため対象外。
- onDrawOver: Android 側変更なしのため対象外。
- replaceCells: 該当しない。

---

## アクションプラン

すべて Low / Very Low 優先度の Suggestion であり、APPROVED に支障はない。マージ後の継続改善として任意で取り組める。

1. **任意**: `KsCellViewSupport.applyEffectiveHeight` 末尾コメントを `setNeedsLayout()` 中心の説明にリファクタ（Suggestion-1）
2. **任意**: 固定モード（`hasUnevenRows == false`）の視覚的セル高さ検証テストを 1 件追加（Suggestion-2）
3. **任意**: `KsListCellBase.preferredLayoutAttributesFitting` に super 呼び出し根拠の 1 行コメントを追加（Suggestion-3）
4. **必須（フェーズ外）**: tasks.md の `10.4` / `11.5` / `13.1` / `13.2` の実機目視確認をオーナー側で実施（Phase 17 完了後の Image #14 等での再確認）。

---

## 判定結果

**ステータス**: **APPROVED**

- Critical / Major / Minor の指摘なし。
- Suggestion 3 件はすべて Low / Very Low 優先度で、コードの正しさやテスト網羅性に致命的な影響なし。
- AiForms オリジナル参照の妥当性、`preferredLayoutAttributesFitting` パターンの正確さ、共通基底クラス導入の保守性、Phase 16 機構との整合性、テスト網羅性、ビルド・テスト全件 PASS、openspec valid をすべて確認済み。
- Phase 17 はオーナー三次実機目視（Image #12/#13）の核心指摘に対する正式対応として、設計・実装・テスト・ドキュメントの一貫性を保ちつつ完結している。

マージ可能。
