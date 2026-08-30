# レビュー結果 - refine-basic-cells-sample-layout (Phase 14.2 / 14.5 再修正)

**レビュー日時**: 2026年06月06日
**レビュワー**: sdd-reviewer
**変更提案ID**: refine-basic-cells-sample-layout

## サマリー

Phase 14.2（iOS Header / Footer 余白問題）および Phase 14.5（Android Switch オフ時色問題）の **再修正** をレビューした。

### 全体所見

- 前回の不十分な修正（前回 PR `applyAccessoryToListCell` 内 `directionalLayoutMargins` だけの対応）と同じ漏れが起きないように、Phase 14.2 では **3 段構え**（`headerTopPadding = 0` + `.estimated(20)` + `contentInsets = .zero`）で原因を 1 つずつ潰しており、根本対応として妥当。
- Phase 14.5 は前回「Track / Thumb 両方とも `colorOutline`」という **明確なバグ** を、Material 3 標準の「オフ Track = `colorSurfaceContainerHighest`、オフ Thumb = `colorOutline`」配色に修正しており、Material 3 仕様にも合致する。
- 仕様（spec.md）への MUST 文言追記が実装の意味論と整合しており、新規 Scenario も実装で検証される範囲をカバーしている。
- 自動テストで検証可能な箇所（`headerTopPadding == 0`、`backgroundColor == .clear`、オフ時 Track 色 != Thumb 色）はすべてテストが追加されており、PASS している。
- ビルド・テストすべて成功：
  - iOS swift test: 154 PASS
  - iOS xcodebuild test (iPhone 17): 162 PASS
  - iOS Sample build: SUCCEEDED
  - Android core/ui/compose test: BUILD SUCCESSFUL
  - Android Sample assembleDebug: BUILD SUCCESSFUL
  - `openspec validate refine-basic-cells-sample-layout --strict`: valid
- 既存 Phase 1〜14 の他項目（罫線インセット規則、罫線太さ、`Section.headerHeight = 40` の `.absolute(40)` 反映、Footer 文字色など）と矛盾していない。`makeHeaderBoundaryItem` の `.absolute(headerHeight)` パスと `.estimated(20)` パスのいずれも、`contentInsets = .zero` / `pinToVisibleBounds = false` が同じ呼び出し階層（line 366-368）で一律適用される設計になっており、Phase 14.9 の固定高さ 40pt 指定とも整合する。

### 自動テストでカバーしきれないリスク

実機目視確認（Phase 13.1 / 13.2）が **依然として未完** であり、本再修正で「実機目視で確実に直っているか」は **オーナーレビュー側で確認が必要**。
ただし、自動テストで検証可能な部分（API 設定値）は網羅されており、コード読解からも 4 経路（Root Header / Root Footer / Section Header / Section Footer）すべてに `.estimated(20)` / `contentInsets = .zero` の漏れがないことを確認した。

**判定**: `APPROVED`

## 指摘事項

### 🔵 Suggestion 1: `.estimated(20)` / `contentInsets = .zero` の直接的なテストカバレッジ

**該当箇所**: `ios/Tests/KsSettingsViewUITests/KsSettingsViewControllerTests.swift`

**問題点**:

`test_makeListConfig_headerTopPaddingは0` / `test_makeListConfig_backgroundColorはclear` で `UICollectionLayoutListConfiguration` 側の検証は十分にできているが、Phase 14.2 で最も重要な変更点である `NSCollectionLayoutBoundarySupplementaryItem` の `heightDimension = .estimated(20)` と `contentInsets = .zero` の検証が、直接的なユニットテストではカバーされていない。
sectionProvider クロージャ内部の動的決定であるため確かに直接検証は難しいが、4 経路（Root Header / Root Footer / Section Header / Section Footer）のいずれかで漏れがあった場合、自動テストでは捕捉できない。実機目視確認のみが頼りとなる。

**推奨修正**:

将来的な再発防止のため、`makeLayout(for:)` を呼んだ後の `boundarySupplementaryItems` を取り出して `heightDimension` / `contentInsets` を検証するヘルパテストの追加を検討する。たとえば以下のような形式：

```swift
func test_makeLayout_rootHeaderItemHeightIs20AndContentInsetsZero() {
    let vc = KsSettingsViewController(root: SettingsRoot(), style: .classic, ...)
    vc.rootHeader = .text("Root Header")
    let layout = vc.makeLayout(for: .classic) as? UICollectionViewCompositionalLayout
    let boundaryItems = layout?.configuration.boundarySupplementaryItems ?? []
    let rootHeaderItem = boundaryItems.first { $0.elementKind == KsSettingsViewController.rootHeaderElementKind }
    XCTAssertNotNil(rootHeaderItem)
    // .estimated(20) を直接比較するのは API 上難しいが、size の調査は可能
    XCTAssertEqual(rootHeaderItem?.contentInsets, .zero)
    XCTAssertFalse(rootHeaderItem?.pinToVisibleBounds ?? true)
}
```

ただし、`NSCollectionLayoutDimension` の `.estimated(20)` を直接比較する公開 API はないため、`hashValue` 比較や `description` 確認等のテクニックが必要になる。Section Header / Section Footer の sectionProvider クロージャ実行結果のテストは更に難しい（`NSCollectionLayoutEnvironment` の mock が要る）。本指摘は強い修正要請ではなく、改善余地として提示する。

### 🔵 Suggestion 2: `applyAccessoryToListCell` の SwiftUI / UIKit accessoryView 経路にも上下マージン縮小の必要性

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:1402-1419`

**問題点**:

`applyAccessoryToListCell` の `accessoryText` 経路では `directionalLayoutMargins = (2, 16, 2, 16)` が適用されるが、`KsAnyView.swiftUI` / `KsAnyView.uiKit` 経路では `UIHostingConfiguration` や `addSubview + Auto Layout` を使っており、上下マージンの縮小が行われていない。
RootHeader / RootFooter にカスタム View を渡された場合、その上下にシステム既定の余白が残る可能性がある。本変更提案の Sample コードでは text ベースの Header / Footer しか使われていないため実機影響は無いが、ライブラリ利用者が View ベースの RootHeader を使った場合、本 Requirement「Header / Footer 周辺の上下余白合計 8pt 以下」を満たさない可能性がある。

**推奨修正**:

`UIHostingConfiguration` には `.margins(.all, 2)` または `.margins(.vertical, 2)` を、UIKit 経路にも上下 2pt の制約に変更する。ただし本 change のスコープは Sample/Demo 用途であり、利用者カスタム View はまだ実需が無いため、別 change での対応でも構わない。

### 🔵 Suggestion 3: Phase 14.5 オフ時 Track 色のフォールバック確実性

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SwitchCellViewHolder.kt:104-108`

**問題点**:

`MaterialColors.getColor` が `colorSurfaceContainerHighest` を解決できなかった場合、フォールバックは `Color.LTGRAY` (`#CCCCCC`)。`colorOutline` のフォールバックは `Color.GRAY` (`#888888`)。両者の Material デフォルト解決が同時に失敗した場合でも `LTGRAY != GRAY` で **異なる色** になるためテストはパスする。
ただし、テーマによっては `colorSurfaceContainerHighest` が `colorOutline` と非常に近い色に解決される可能性が完全に排除できない（あくまで Material 3 ガイダンスに沿う実装側の責務）。`assertNotEquals` テスト（line 1166-1183）が `intArrayOf(-android.R.attr.state_checked)` でのテーマ解決結果に依存するため、テーマ次第ではテスト Robotic レベル（Robolectric 既定 Material 3 テーマ）でしか保証されない可能性がある。

**推奨修正**:

現状は問題なし（Robolectric Material 3 テーマで PASS、実機 Image #7 想定通り）。ただし、将来 Material ライブラリのバージョンアップで `colorSurfaceContainerHighest` のセマンティクスが変わった場合に備え、テスト時に `Color.LTGRAY != Color.GRAY` という **絶対的なフォールバック保証** がコードコメントで読み取れる旨を明文化する程度の改善余地はある。本指摘は将来リスク防止のための注意喚起であり、現時点での修正要請ではない。

## アクションプラン

判定が `APPROVED` のため、必須対応はなし。以下はオーナー判断による将来改善余地：

1. **（任意）** Suggestion 1: 4 経路（Root Header / Footer / Section Header / Footer）の `boundarySupplementaryItem.contentInsets / heightDimension` を直接検証するテストを追加し、将来の再発を機械的に防ぐ。
2. **（任意）** Suggestion 2: ライブラリ利用者が `RootHeader.view(...)` でカスタム View を使った際にも 8pt 以下密度を維持するため、`UIHostingConfiguration` / `addSubview` 経路にも上下マージン縮小を導入する。
3. **必須（自動チェック不可）** Phase 13.1 / 13.2 の **実機目視確認** をオーナー側で実施する：
   - iOS: Header / Footer 余白が Android と同等密度（≦ 8pt）になっていることを確認する。
   - Android: Switch オフ時に Track（薄いグレー）と Thumb（中間グレー）が **視覚的に分離** していることを確認する。
   - Image #7（Google Play 通知設定）と並べて比較すると判定しやすい。

## 判定結果

**ステータス**: `APPROVED`

### 判定理由

- 修正の根本原因分析が網羅的（`headerTopPadding` + `boundarySupplementaryItem.heightDimension` + `contentInsets` の 3 軸）であり、Material 3 標準（Image #7）に揃えた配色変更も明確。
- 4 経路（Root Header / Root Footer / Section Header / Section Footer）すべてで `.estimated(20)` / `contentInsets = .zero` / `pinToVisibleBounds = false` が一律適用されており、前回のような「片方だけ漏れる」リスクは無い。
- spec.md の MUST 文言と実装が一致し、新規 Scenario もテストか実機目視で検証できる範囲になっている。
- iOS swift test (154) / xcodebuild test (162) / Android test / 両 Sample build / `openspec validate --strict` がすべて PASS。
- Phase 14.5 のオフ時 Track / Thumb 色分離は `assertNotEquals` 直接テストで担保されており、Material 3 仕様（Image #7）と整合。
- 既存 Phase 1〜14 の他項目（罫線インセット、罫線太さ、`headerHeight = 40` の固定高さ反映、Footer 文字色など）と機能的衝突なし。

**Critical / Major レベルの指摘なし**。Suggestion レベル 3 件はいずれも将来改善余地であり、本 change のマージを阻害するものではない。

実機目視確認（Phase 13.1 / 13.2）はオーナー側で実施する必要があるが、自動チェックで担保可能な範囲は十分にカバーされているため、コードレビュー観点では承認とする。
