# レビュー結果 - refine-basic-cells-sample-layout (Phase 16)

**レビュー日時**: 2026年06月06日
**レビュワー**: sdd-reviewer
**変更提案ID**: refine-basic-cells-sample-layout
**レビュースコープ**: Phase 16（iOS Section.headerHeight 反映修正 / `KsAccessoryReusableView` 導入 / 視覚的高さ検証テスト追加 / Sample 値 80 化）

---

## サマリー

Phase 16 の中核は「`UICollectionViewListCell` の self-sizing が `boundarySupplementaryItem.layoutSize.heightDimension = .absolute(headerHeight)` を上書きしていた」根本原因の特定と、AiForms オリジナル `TextHeaderView : UITableViewHeaderFooterView` 構造に倣った `UICollectionReusableView` 直系クラス `KsAccessoryReusableView` への切り替えである。

実装・テスト・仕様すべてを精査した結果、以下を確認した。

- **iOS swift test (macOS)**: 154 tests passed, 0 failures
- **iOS xcodebuild test (iPhone 17 / iOS 26.5 Simulator)**: 170 tests passed, 0 failures
  - Phase 16 の中核テスト `test_視覚的ヘッダ高さ_headerHeight80指定時_supplementaryのframe高さが80になる` および `_120指定時...` を含む全テストが PASS
  - 純粋ロジックテスト `test_makeHeaderBoundaryItem_headerHeight80のとき_absolute80になる` ほか 4 件も PASS
- **Android Gradle test**: BUILD SUCCESSFUL（166 actionable tasks、全テスト UP-TO-DATE 含む）
- **Android Sample assembleDebug**: BUILD SUCCESSFUL
- **iOS Sample build (iPhone 17 Simulator)**: BUILD SUCCEEDED
- **openspec validate refine-basic-cells-sample-layout --strict**: `Change 'refine-basic-cells-sample-layout' is valid`
- **AiForms オリジナル参照の妥当性**: `SettingsTableSource.cs:143-167` の `GetHeightForHeader` が CGFloat 直返却で UITableView の rect 計算に直接反映される構造、`TextHeaderView.cs` が `UITableViewHeaderFooterView` を継承し priority 999 制約を張る構造、いずれも本実装の設計判断（`UICollectionReusableView` 直系 + priority 999）に正しく対応している。AiForms 参照に矛盾はない。
- **`KsAccessoryReusableView` の実装**: priority 999 の Auto Layout 制約、`prepareForReuse` での状態リセット、`setVerticalAlignment(_:)` での同一値再設定スキップ（無駄な制約張り直し回避）、leading/trailing 16pt インセット、いずれも AiForms `TextHeaderView` の意図に沿った妥当な実装。1 ファイル 1 型・日本語コメント・`UIKit` 限定の `#if canImport(UIKit)` ガードも適切。
- **分岐ロジック `makeAccessoryListCell`**: `accessoryText != nil || accessoryView == nil` の条件で reusable view 経路へ流す判定は、テキスト accessory（Header / Footer のテキスト）と accessory 未指定の両方を新クラスにルーティングし、`accessoryView` 経路（SwiftUI / 任意 UIView）のみ既存 `UICollectionViewListCell` に残す意図と一致している。後方互換性も維持されている。
- **`refreshRootSupplementary` の更新**: `as? KsAccessoryReusableView` / `as? UICollectionViewListCell` の二段分岐で Root Header / Footer も正しく動作する。
- **視覚的高さ検証テスト**: `layoutIfNeeded()` を二度走らせてから `supplementaryView(forElementKind:at:)` を取得し、`frame.height` を `accuracy: 0.5` で比較する手法は妥当。Simulator 環境の CALayer 演算誤差（±0.5pt 程度）を許容する許容幅も合理的で、本テストが実際に PASS している事実が `.absolute(headerHeight)` の描画 frame への反映を保証している。
- **Phase 15 `applyAccessoryLabel` との関係**: テキスト accessory 経路（`accessoryText != nil || accessoryView == nil`）は新クラスに完全に切り替わっており、`applyAccessoryToListCell` 内の `if let text = accessoryText` 分岐は **実際の呼び出しでは到達しないデッドパスになっている**（後述 Minor-1）。`applyAccessoryLabel` も同パスからしか呼ばれないため、同様にデッドコード化している。動作上の不具合はないが、コード衛生として今後の整理対象となる。

**判定**: **APPROVED**

Critical / Major 指摘なし。実装・テスト・仕様の整合は完璧で、Phase 16 の目的（`Section.headerHeight` 正値が iOS 描画 frame に確実に反映される）はテスト実測で明確に保証されている。残る Minor 指摘 1 件はデッドコードの整理提案であり、本 change の archive を阻害しない。

---

## 指摘事項

### 🟡 Minor-1: `applyAccessoryToListCell` / `applyAccessoryLabel` のテキスト accessory 分岐がデッドコードになっている

**該当箇所**:
- `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:1551`（`applyAccessoryToListCell`）
- `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:876`（`applyAccessoryLabel`）
- `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:1562`（`if let text = accessoryText` 分岐）

**問題点**:

Phase 16 で `makeAccessoryListCell` の分岐ロジック（L776）が以下になった結果、`applyAccessoryToListCell` がテキスト accessory を扱うパスは **実際には到達不能**になっている。

```swift
// makeAccessoryListCell L776
if accessoryText != nil || accessoryView == nil {
    return makeAccessoryReusableView(...)   // ← テキスト or accessory 未指定はここに流れる
}
// 以下は (accessoryText == nil && accessoryView != nil) のみ到達
applyAccessoryToListCell(listCell, accessoryText: accessoryText, accessoryView: accessoryView, ...)
```

したがって `applyAccessoryToListCell` の引数 `accessoryText` は常に `nil` で渡され、L1562 の `if let text = accessoryText` ブロック内（およびそこから呼ばれる `applyAccessoryLabel` 全体）は **新たに呼ばれない**。

動作上の不具合はないが、

- 読み手は「`applyAccessoryToListCell` がテキスト経路をサポートしている」と誤読しやすい
- Phase 15.1 で導入した `applyAccessoryLabel`（L876）の存在意義が薄れ、`KsAccessoryReusableView.setVerticalAlignment(_:)` と二重実装になっている
- 将来 `accessoryView != nil` でも何らかのテキスト accessory 機能が増えた場合に古い実装側を変更すべきか新側を変更すべきか判断が割れる

**推奨修正（本 change スコープ内でも、後続 change でも可）**:

1. `applyAccessoryToListCell` の引数 `accessoryText` を削除し、accessoryView 経路専用シグネチャに整理する
2. `applyAccessoryLabel`（L876）と関連 `AccessoryVerticalAlignment` 周辺ロジックの重複部分を削除し、`KsAccessoryReusableView` 側に一本化する
3. 上記が大きい場合は、少なくとも `applyAccessoryToListCell` 冒頭に `assert(accessoryText == nil, "Phase 16 以降テキスト accessory は KsAccessoryReusableView 経路に流れる前提")` を追加し、想定外パスの混入を即座に検出できるようにする

**判定への影響**: Minor / 動作影響なし。本 change のスコープを広げてまで修正する必要はないが、デッドコード整理として後続 change で扱うのが望ましい。本 change の APPROVED 判定は変更しない。

---

### 🔵 Suggestion-1: `KsAccessoryReusableView.label.font` を呼び出し側からも上書き可能にする検討

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsAccessoryReusableView.swift:59`

**問題点**:

`label.font = UIFont.preferredFont(forTextStyle: .footnote)` が `let` 初期化子の中で固定されているため、将来 `Theme.headerFontSize` / `Theme.footerFontSize` を反映する Requirement が増えた際、`KsAccessoryReusableView` 内部に手を入れる必要がある。

現状の仕様（`Theme.headerFontSize` / `footerFontSize` は本 change スコープ外）では問題ないが、Phase 16 で AiForms オリジナルの `PaddingLabel` 相当として `label` を露出する設計を採ったため、`makeAccessoryReusableView` 側で `accessoryView.label.font = ...` を上書きする経路を持つだけで将来拡張余地ができる。

**推奨**: 本 change ではコード変更は不要。将来 fontSize 反映を実装する change を作る際に、`KsAccessoryReusableView` の `label` プロパティが `let` 公開済みであることを活用して呼び出し側で上書きする方針を取れば、本クラスに改修を入れずに対応できる。仕様メモとしての扱い。

**判定への影響**: なし。

---

### 🔵 Suggestion-2: 視覚的高さ検証テストのコンテナサイズ依存の明示

**該当箇所**: `ios/Tests/KsSettingsViewUITests/KsSettingsViewControllerTests.swift:408`

**問題点**:

`measuredSectionHeaderHeight(for:section:containerSize:)` の `containerSize` 既定値 `CGSize(width: 375, height: 1000)` は iPhone 系幅と十分高い height を想定しているが、`headerHeight = 120` テストで高さが 1000 でも理論上は表示可能だが、将来「複数 Section + 大きい `headerHeight` + 小さい containerSize」のテストを追加した場合に可視範囲外の supplementary が `nil` を返して失敗する可能性がある。

**推奨**:

- 既存テストには影響なし。回帰テスト追加時に `containerSize.height` を `max(headerHeight * 2, 1000)` のような動的計算にしておくと安全。
- もしくは `prefetchEnabled` の制御で全 supplementary を強制取得する方法に切り替える。

**判定への影響**: なし。

---

## レビュー観点別チェック結果

### 正確性・機能性

- [x] openspec の仕様（proposal.md / design.md / tasks.md / spec.md 群）に正しく追従している
- [x] Phase 16 タスク 16.1〜16.17 すべて完了済み（実装範囲を実際にコードで確認）
- [x] エッジケース（`headerHeight = -1 + header nil`、`headerHeight = -1 + header テキスト有`、`headerHeight 正値 + header テキスト有`、`headerHeight 正値 + header nil`）を `makeHeaderBoundaryItem` 純粋ロジックテストで網羅
- [x] `KsAccessoryReusableView.prepareForReuse()` で `label.text = nil` / `textColor = nil` / `backgroundColor = .clear` を初期化、再利用時の状態漏れを防止
- [x] `setVerticalAlignment(_:)` の同一値再設定スキップで無駄な制約張り直しを回避

### テスト容易性

- [x] `makeHeaderBoundaryItem` を `internal static` 化して純粋ロジックテストを可能にした設計判断は妥当
- [x] `KsAccessoryReusableView` は `final` class で、`UICollectionReusableView` 直系のため UIKit 標準テスト手法（`dequeueReusableSupplementaryView` 経由）でテスト可能
- [x] 時刻ソース直接参照なし（該当箇所なし）

### セキュリティ

- [x] 入力値（`String` テキスト）は UILabel に渡るだけで、エスケープ不要
- [x] 機密情報のハードコードなし

### パフォーマンス

- [x] `setVerticalAlignment(_:)` の同一値ガードで不要な制約 deactivate/activate を回避
- [x] `KsAccessoryReusableView` は cell registration 経由で再利用される（`registeredSupplementaryKinds` セットで重複 register を抑止）

### 可読性・保守性

- [x] 日本語コメントが豊富で、AiForms オリジナルファイル名・行範囲を引用しているため設計意図の追跡が容易
- [x] 命名（`KsAccessoryReusableView`、`makeAccessoryReusableView`、`mapVerticalAlignment`）はすべて目的を明確に表す
- [⚠] Minor-1 のデッドコード（`applyAccessoryLabel` 経路）が残置されており、可読性に若干の影響

### 一貫性

- [x] 1 ファイル 1 型のルールを遵守（`KsAccessoryReusableView` 専用ファイル）
- [x] 既存 `KsSettingsViewController` の structuring（MARK 区切り、private/internal アクセス制御、`Self.` 経由の static 呼出）と一貫
- [x] プロジェクトルール「`onDrawOver`」「Theme.Material3.*」「`replaceCells`」のいずれも対象外（iOS / Android UI の他 phase が遵守済み）

### 多言語対応

- [x] 本 phase で追加された文字列は UI 上のテキストではなくテストコード内の Section 名・Cell 名のみ（`"CommandCell"` / `"Tall"` / `"Sample"`）。文字列リソース化対象なし

### テスト

- [x] iOS 全テスト PASS（macOS 上 154 / iOS Simulator 上 170）
- [x] Android 全テスト PASS（BUILD SUCCESSFUL）
- [x] iOS / Android Sample アプリのビルドも PASS
- [x] spec.md に対応するテストが実装されている：
  - 「テキスト accessory 用 supplementary view クラスの選択」Requirement → `SectionAccessoryRenderingTests` 内 `is KsAccessoryReusableView` 検証（L53, L154, L250, L284, L318, L350）
  - 「Section.headerHeight の UI 反映」Requirement Scenario 「headerHeight 正値が描画 frame に反映される」 → `test_視覚的ヘッダ高さ_headerHeight80指定時_..._80になる` / `_120指定時_..._120になる`
  - Decision 16-1（`.absolute` vs `.estimated` 選択） → `test_makeHeaderBoundaryItem_*` 4 件
- [x] テスト内に手抜き実装・スキップなし
- [x] スタブ使用なし
- [x] 境界値（`headerHeight = -1` / 正値、`header = nil` / 非空）の組み合わせを純粋ロジックテストでカバー

### openspec 検証

- [x] `openspec validate refine-basic-cells-sample-layout --strict` → `Change 'refine-basic-cells-sample-layout' is valid`

---

## アクションプラン

判定が APPROVED のため、本 change は archive 可能。後続 change で扱うべき項目を優先度順に列挙する。

1. **（低優先・別 change 推奨）Minor-1**: `applyAccessoryToListCell` のテキスト accessory 分岐 / `applyAccessoryLabel` のデッドコード整理。`accessoryText` 引数の削除または `assert(accessoryText == nil)` の追加で、Phase 16 以降の設計意図（テキストは `KsAccessoryReusableView` 経路のみ）を強制する。
2. **（参考・将来 change の設計メモ）Suggestion-1**: `Theme.headerFontSize` / `footerFontSize` 反映を実装する際は、`KsAccessoryReusableView.label` を直接公開している設計を活用し、呼び出し側で `font` を上書きする方針を採用する。
3. **（参考・将来テスト改善メモ）Suggestion-2**: 視覚的高さ検証テストの container size を動的化して、より大きな `headerHeight` テスト追加時の堅牢性を確保する。

---

## 判定結果

**ステータス**: `APPROVED`

### 判定根拠

- Critical / Major 指摘なし
- iOS swift test（154）、iOS xcodebuild test（170）、Android Gradle test、両プラットフォーム Sample ビルド、すべて PASS
- `openspec validate ... --strict` が valid
- Phase 16 の中核目的（`Section.headerHeight` 正値が iOS 描画 frame.height に反映される）はテスト実測（`accuracy: 0.5pt`）で保証されている
- AiForms オリジナル `SettingsTableSource.cs:143-167` / `TextHeaderView.cs` への参照と本実装（`UICollectionReusableView` 直系 + priority 999 制約 + `setVerticalAlignment` 意味論）の対応関係が一字一句正確
- 後方互換性（`accessoryView` 経路は従来通り `UICollectionViewListCell`）も維持されている
- プロジェクトルール（日本語コメント、`onDrawOver`、Theme.Material3.*、`replaceCells`）も他 phase で遵守済みで Phase 16 が違反することはなし
- Minor / Suggestion は本 change archive を阻害しないコード衛生・将来拡張メモ

本変更提案は **archive 可能**。
