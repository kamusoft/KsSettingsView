# レビュー結果 - refine-basic-cells-sample-layout (Phase 18)

**レビュー日時**: 2026年06月07日
**レビュワー**: sdd-reviewer
**変更提案ID**: refine-basic-cells-sample-layout
**レビュースコープ**: Phase 18（Phase 16 機構の revert / B 案：副次改善のみ維持）

## サマリー

オーナーの「Phase 16 で間違ってしなくて良い修正を入れたなら戻して欲しい」との明確な指示に基づき、B 案（機構は revert、副次改善は維持）で実施された Phase 18 のレビュー結果を以下にまとめる。

### 全体評価

提示されたレビュー観点 1〜9 のすべてについて、想定通りの状態が達成されていることを確認した。

- Phase 16 機構（`KsAccessoryReusableView` / 経路分岐 / 視覚的ヘッダ高さ検証テスト 2 件 / `measuredSectionHeaderHeight` ヘルパ）が**完全に削除**されており、ソース・テスト・delta spec のいずれにも実体参照は残っていない（残っているのはコメント内の経緯記録のみ）。
- `applyAccessoryLabel` / `applyAccessoryToListCell` が Phase 15.1 と同等のシグネチャ・実装（priority 999 の UILabel + AutoLayout、Header = 下端 / Footer = 上端）で確実に復活している。
- B 案で残すべき副次改善（`makeHeaderBoundaryItem` の `internal static` 化、純粋ロジックテスト 4 件）はソース・テスト・Decision 16-1 のいずれにも維持されている。
- Phase 17 機構（`KsListCellBase` + `preferredLayoutAttributesFitting`、Cell View 7 種の継承、`test_視覚的セル高さ_*` 2 件）は無傷で、本 Phase の revert と独立して機能している。
- delta spec の整合性: `specs/settings-view-ios-ui/spec.md` から「テキスト accessory 用 supplementary view クラスの選択」Requirement は完全削除、「Section.headerHeight の UI 反映」内の「headerHeight 正値が描画 frame に反映される」Phase 16 Scenario も削除済み。Phase 15 由来の「Section Header / Footer の垂直配置」Requirement は復元前提を満たして残っている。
- Sample 値 `headerHeight = 60` / `60.0`（iOS / Android）に確実に戻されている。
- iOS Swift Package 154 tests / iOS Simulator 170 tests / Android `:ks-settingsview-{core,ui,compose}:test` / iOS Sample / Android Sample すべて PASS / BUILD SUCCEEDED。
- `openspec validate refine-basic-cells-sample-layout --strict` = `valid`。
- プロジェクトルール（日本語コメント、`trash` 使用、Theme.Material3.*、`onDrawOver`、`replaceCells`）への違反なし。

**判定**: ✅ **APPROVED**

## 指摘事項

Critical / Major 指摘なし。以下は実装の確認結果と軽微な観察事項。

### 確認結果（指摘なし、参考記録）

#### 観点 1: revert の網羅性

- `KsAccessoryReusableView.swift` ファイル: 削除済み（`ls` で No such file or directory、`git status` で working tree clean）。
- `KsAccessoryReusableView` / `makeAccessoryReusableView` / `mapVerticalAlignment` / `measuredSectionHeaderHeight` の参照を `ios/Sources/` / `ios/Tests/` に対して `grep` した結果、**実体参照は 0 件**。残っているのは下記のコメント内経緯記録のみで、いずれも「Phase 18（revert）」を明記した解説コメント。
  - `KsSettingsViewController.swift:415`（副次改善維持の補足コメント）
  - `KsSettingsViewController.swift:766`（`makeAccessoryListCell` 統一の経緯記録）
  - `KsSettingsViewController.swift:1375`（`refreshRootSupplementary` の経緯記録）
  - `KsSettingsViewController.swift:1416`（`applyAccessoryToListCell` の経緯記録）
  - `SectionAccessoryRenderingTests.swift:307`（テスト復元の経緯記録）
- 経緯記録としてコメントを残す方針は、Phase 16 / 17 / 18 という複雑な経緯を後追いするうえで妥当。

#### 観点 2: `applyAccessoryLabel` / `applyAccessoryToListCell` 復元の正しさ

- `applyAccessoryToListCell(_:accessoryText:accessoryView:textColor:verticalAlignment:)` のシグネチャは Phase 15.1 と一致（`accessoryText: String?` / `accessoryView: KsAnyView?` / `textColor: UIColor` / `verticalAlignment: AccessoryVerticalAlignment`）。
- `applyAccessoryLabel` 実装は AiForms オリジナル `TextHeaderView.cs` の priority 999 制約方式に揃い、Header = `bottomAnchor`、Footer = `topAnchor` の下端 / 上端揃えで `contentView` に張り付く。`leading/trailing` 16pt インセット、`UIFont.preferredFont(forTextStyle: .footnote)` 既定 font も `BasicCellsTests` の期待値と整合。
- `makeAccessoryListCell` は `KsAccessoryReusableView` 分岐が完全に撤廃され、テキスト / SwiftUI / UIKit すべて `UICollectionViewListCell` 経路に統一されている（`KsSettingsViewController.swift:754〜796`）。
- `refreshRootSupplementary` も `UICollectionViewListCell` 経路のみに整理されている（`KsSettingsViewController.swift:1377〜1399`）。

#### 観点 3: B 案副次改善の維持

- `KsSettingsViewController.makeHeaderBoundaryItem(for:original:)` は `internal static` で公開され（`KsSettingsViewController.swift:420`）、`section.headerHeight > 0` → `.absolute(CGFloat(headerHeight))`、`section.headerHeight == -1 && header != nil` → `.estimated(20)`、`section.headerHeight == -1 && header == nil` → `nil` の純粋ロジック分岐を保持。
- 純粋ロジックテスト 4 件はすべて `KsSettingsViewControllerTests.swift` に維持されている：
  - `test_makeHeaderBoundaryItem_headerHeight80のとき_absolute80になる` (line 322)
  - `test_makeHeaderBoundaryItem_headerHeight未指定_header非空のとき_estimated20になる` (line 344)
  - `test_makeHeaderBoundaryItem_headerHeight未指定_header_nilのとき_nilを返す` (line 362)
  - `test_makeHeaderBoundaryItem_headerHeight40_header_nilでも_absolute40になる` (line 375)
- `design.md` Decision 16-1（`.absolute` vs `.estimated` 選択ロジック）は維持され、Decision 18-1 の B 案維持対象テーブルにも記載されている。

#### 観点 4: Phase 17 機構の無傷確認

- `KsListCellBase.swift` は存在し、`UICollectionViewListCell` 継承＋`preferredLayoutAttributesFitting` override が維持されている。
- Cell View 7 種（`LabelCellView` / `CommandCellView` / `ButtonCellView` / `SwitchCellView` / `CheckboxCellView` / `RadioCellView` / `SimpleCheckCellView`）すべてが `KsListCellBase` を継承（`grep` で確認）。
- 視覚的セル高さ検証テスト 2 件は `KsSettingsViewControllerTests.swift` に維持：
  - `test_視覚的セル高さ_cellHeight80指定時_セルのframe高さが80になる` (line 430)
  - `test_視覚的セル高さ_cellHeight120指定時_セルのframe高さが120になる` (line 456)
- `KsCellViewSupport.adjustedLayoutAttributes` も `KsCellViewSupport.swift:154` で維持されている。
- iOS Simulator 170 tests 全 PASS により、Phase 17 機構が壊れていないことを実測でも確認済み。

#### 観点 5: delta spec の整合性

- `specs/settings-view-ios-ui/spec.md`:
  - 「テキスト accessory 用 supplementary view クラスの選択」Requirement は **完全削除**（`grep` で `テキスト accessory 用` / `KsAccessoryReusableView` ともに 0 件）。
  - 「Section.headerHeight の UI 反映」Requirement は維持しつつ、Phase 16 で追加された「headerHeight 正値が描画 frame に反映される」Scenario は削除済み（残っているのは「正値による固定高さ」「正値が AutoLayout 下端揃えと両立する（headerHeight=60）」「-1 + header テキスト有りの自動高さ」「-1 + header 空時の supplementary 非生成」の 4 Scenario）。
  - 「Section Header / Footer の垂直配置」Requirement は Phase 15.1 由来として維持され、復元された `applyAccessoryLabel` ロジックに対応する Scenario が正しく記述されている。
- `specs/samples-ios/spec.md` / `specs/samples-android/spec.md`:
  - `headerHeight = 60` / `60.0` に戻り、Phase 16 で `80` に増量した記述は削除済み。
  - 経緯コメント（「Phase 16 で `80` に増量したが、Phase 18 で revert して `60` に戻した」）が併記され、変更履歴が追跡可能。

#### 観点 6: Sample 値の確認

- `samples/ios/KsSettingsViewSample/BasicCellsDemoView.swift:85` → `Section("CommandCell", headerHeight: 60)`
- `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/BasicCellsDemoScreen.kt:71` → `Section(header = "CommandCell", headerHeight = 60.0)`
- 両プラットフォームとも Phase 15.2 / 15.5 の値に確実に戻っている。

#### 観点 7: テスト・ビルド全件 PASS

| カテゴリ                    | 結果                                                    |
| --------------------------- | ------------------------------------------------------- |
| iOS `swift test`            | `Executed 154 tests, with 0 failures` PASS              |
| iOS `xcodebuild test`（Simulator iPhone 17） | `Executed 170 tests, with 0 failures` `** TEST SUCCEEDED **` |
| Android `:ks-settingsview-*:test` | `BUILD SUCCESSFUL` (166 actionable tasks UP-TO-DATE) |
| Android Sample `:app:assembleDebug` | `BUILD SUCCESSFUL` (94 actionable tasks UP-TO-DATE)  |
| iOS Sample `xcodebuild build`  | `** BUILD SUCCEEDED **`                                  |

iOS Simulator 170 tests に Phase 17 の視覚的セル高さ検証テスト 2 件と Phase 16 副次改善の純粋ロジックテスト 4 件が含まれており、本 Phase の revert が他 Phase の機構を破壊していないことが実測で保証されている。

#### 観点 8: `openspec validate --strict`

- `Change 'refine-basic-cells-sample-layout' is valid`

#### 観点 9: プロジェクトルール

- 日本語コメント: `KsListCellBase.swift` / `KsSettingsViewController.swift` の Phase 18 コメントはすべて日本語で、経緯と意図が明確に記述されている。違反なし。
- `trash` 使用: tasks 18.1 で「`trash` 経由」と明記、`KsAccessoryReusableView.swift` は実際に削除済み（`ls` で存在しないことを確認）。違反なし。
- `Theme.Material3.*` / `onDrawOver` / `replaceCells`: Phase 18 は iOS 側の revert + Sample 値変更が中心で、Android 側のテーマ / Decoration / バッチ更新には変更を加えていない。既存挙動に違反なし。

### 軽微な観察事項（指摘ではなく備考）

#### 🔵 Suggestion 1: 経緯コメントの量

**該当箇所**: `KsSettingsViewController.swift` の Phase 18 関連コメント、`design.md` Decision 16-2 / 16-3 の「Phase 18 で revert 済み」付記、`SectionAccessoryRenderingTests.swift:307` の Phase 18 経緯コメント。

**観察**: Phase 16 → 17 → 18 という複雑な経緯を追跡可能にするための経緯コメントは妥当だが、将来本 change がアーカイブされた後の長期的視点では、これらの「Phase X で…」コメントは古びる可能性がある。アーカイブ時に `openspec archive` で完了仕様が `openspec/specs/` に同期される段階で、Phase 番号への参照を「履歴的記録」として整理するか、`openspec/changes/archive/refine-basic-cells-sample-layout/` のドキュメントに集約してソースコメントを簡素化することを検討してもよい。

**推奨**: 任意。現状でもレビュー観点上の問題はない。判断はオーナーに委ねる。

#### 🔵 Suggestion 2: Phase 18 コメントの整合（参考）

**該当箇所**: `KsSettingsViewController.swift:415`

**観察**: 当該コメントは「Phase 18 で Phase 16 の `KsAccessoryReusableView` 機構は revert されたが、本副次改善は維持する（純粋ロジックとしては正しく、回帰防止に有用）」と明記しており、Decision 18-1 の B 案趣旨と完全に整合している。読み手が `makeHeaderBoundaryItem` の `internal static` 化が「副次改善として残された理由」を即座に把握できる、優れたインラインドキュメントになっている。

## アクションプラン

Critical / Major 指摘がないため、アクションは不要。

任意で検討可能な項目（優先度：低）:

1. （Suggestion 1）アーカイブ時の経緯コメント整理方針をオーナーと相談する。

## 判定結果

**ステータス**: ✅ **APPROVED**

理由:

- オーナー指示「B 案：機構は戻すが、副次的な改善は残す」と完全に一致した revert が実施されている。
- revert の網羅性が極めて高い（実体参照 0 件、`KsAccessoryReusableView.swift` ファイル削除、経路分岐撤廃、視覚的ヘッダ高さテスト削除、`measuredSectionHeaderHeight` ヘルパ削除がすべて確認できた）。
- `applyAccessoryLabel` / `applyAccessoryToListCell` が Phase 15.1 と同等で正しく復元されている。
- B 案で残すべき副次改善（`makeHeaderBoundaryItem` の `internal static` 化、純粋ロジックテスト 4 件、Decision 16-1）が確実に維持されている。
- Phase 17 機構（`KsListCellBase` + `preferredLayoutAttributesFitting`、視覚的セル高さ検証テスト）が無傷で、Phase 17 でオーナーの本来の指摘（`Cell.cellHeight` 反映）が解決された状態が保たれている。
- delta spec の Requirement / Scenario 整理が一貫しており、Phase 16 由来の Requirement / Scenario は完全削除、Phase 15 由来の AutoLayout 下端揃え両立 Scenario の `headerHeight` も 60 に戻されている。
- Sample 値 60 への revert、テスト・ビルド全件 PASS、`openspec validate --strict` valid、プロジェクトルール遵守、すべて達成されている。
- design.md Decision 18-1 が B 案採用理由、revert / 維持対象テーブル、Phase 17 機構との関係、実機検証方法を完全に文書化しており、本 Phase の意図と範囲が明確。

Critical / Major 指摘なし。Minor も実質的になく、Suggestion はいずれも任意の改善案にとどまる。実装範囲・delta spec・ドキュメント・テスト・ビルドすべてが要求を満たしているため、マージ可能と判断する。
