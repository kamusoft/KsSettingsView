# レビュー結果 - add-settings-view-ios-ui

**レビュー日時**: 2026年05月09日  
**レビュワー**: sdd-reviewer  
**変更提案ID**: add-settings-view-ios-ui  

## サマリー

`add-settings-view-ios-ui` は iOS Native の UI 基盤（`KsSettingsViewUI` / `KsSettingsViewSwiftUI` モジュール）を `UICollectionView` + `UICollectionViewDiffableDataSource` + `UICollectionLayoutListConfiguration` で構築し、SwiftUI ラッパ + DSL を同梱する大型変更である。提案・設計・タスク・スペックは互いによく整合しており、実装も全 35 タスク完了、ビルド成功、`swift test`（macOS 50 件）/ `xcodebuild test`（iOS Simulator: Core 48 + UI 29 + SwiftUI 9 = 86 件）すべて成功を確認した。

実装はおおむね設計どおりであり、PoC 動作・Cell レジストリ・Renderer・Theme/CellStyle 合成・スタイル切替・SwiftUI ラッパ・DSL いずれも機能する。Decision 1〜6 の方針が反映されている。

ただし、以下に示すように **Spec で要求された「装飾領域の中身更新で再描画される」シナリオが、現実装ではそもそも再描画パスを通らない可能性が高く、テストもそれを実検証していない**点が Major な懸念として残る。これは将来 `add-cell-types-custom` で SwiftUI/UIView 装飾を本格利用する際に表面化するリスクであり、Phase 1 で潰しておくべき問題である。

**判定**: `CHANGES_REQUESTED`

---

## 指摘事項

### #### 🟠 Major: 装飾領域（Section H/F・Root H/F）の `view` ケース中身更新で再描画されない可能性

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:43-55, 467-494`

**問題点**:

Spec の「Section H/F の view 形式ヘッダの中身更新（差分検出非対応）」と「Root Header の中身更新（差分検出非対応）」シナリオは以下を要求している:

> `KsAnyView` は差分検出に参加しないため supplementary view 自体の生成・破棄は走らないが、`contentConfiguration` の再構成によって `Counter(value: 2)` の中身が再描画される

しかし実装の挙動は以下のとおり:

- `root` の `didSet` で `rebuildLayout` を呼ぶのは Root H/F が **`nil` ↔ 非 `nil`** に切り替わったときのみ。同型内（`.text → .text` 別文字列、`.view → .view` 別中身）では `applySnapshot(animated: true)` のみが呼ばれる。
- DataSource の section ID は `UUID` 単独、item ID は `KsCellID`（Cell の中身ハッシュ）。装飾領域の `KsAnyView` は `Hashable` 非参加で Section/Root の中身ハッシュにも反映されないため、内容を変えただけでは snapshot の差分が発生せず、結果として `applySnapshot` は実質 no-op。
- snapshot が no-op の場合、`supplementaryViewProvider` は再呼出されないため、Section H/F・Root H/F の supplementary view は古い `contentConfiguration` のまま残る可能性が高い。

つまり「同じスロットに別 `KsAnyView` を持つ root を代入して中身が更新される」というシナリオは、実装上**動かない可能性が高い**。

テスト `SectionAccessoryRenderingTests.test_view形式の中身差し替えで再描画される` は「supplementary view が引き続き取得できることを確認する」止まりで、コメントにも以下のとおり明記されている:

```swift
// SectionAccessory の `==` は `view` 同士なら ケース一致のみで等価扱いになる仕様。
// よって snapshot 上の差分は発生しない（item は同一）。
// ここではエラーが出ないこと、および supplementary view が引き続き取得できることを確認する。
```

これは「再描画される」ことの検証としては不十分で、Spec の Scenario の意図を満たしていない。

ソース `KsSettingsViewController.swift:489` のコメントにも認識されている:

```swift
// root.theme / accessory 変更による supplementary 再描画は reload で促す（Hashable 等価でも refresh する）
// ただし「同一 root を 2 回連続代入」では snapshot が空操作となるため、reload は呼ばない。
// ここではシンプルに `apply` のみ行う方針とし、accessory 変化は呼び出し側の didSet で吸収する。
```

「呼び出し側の didSet で吸収する」と書きながら、didSet は `nil` ↔ 非 `nil` の rebuildLayout しか発火しないため、吸収できていない。

**推奨修正**:

1. `root` の didSet で前後の `header` / `footer` の Equatable を比較し、**変化があったとき**（= `oldValue.header != root.header` 等）に `collectionView` の **boundary supplementary を強制リフレッシュ**する処理を入れる。例:

   ```swift
   // root の didSet 内
   let rootHeaderPresenceChanged = (oldValue.header == nil) != (root.header == nil)
   let rootFooterPresenceChanged = (oldValue.footer == nil) != (root.footer == nil)
   if rootHeaderPresenceChanged || rootFooterPresenceChanged {
       rebuildLayout()
       return
   }
   applySnapshot(animated: true)
   // accessory が「同型内で変化」した場合は supplementary を明示的に再構成する
   if oldValue.header != root.header || oldValue.footer != root.footer {
       refreshRootSupplementaryViews()
   }
   if !sectionsAccessoriesEqual(oldValue.sections, root.sections) {
       refreshSectionSupplementaryViews()
   }
   ```

2. Section H/F・Root H/F の supplementary 再構成は、可視 supplementary view を直接取得して `setNeedsUpdateConfiguration()` を呼ぶ、もしくは `var snapshot = dataSource.snapshot(); snapshot.reloadSections([…]); dataSource.apply(snapshot, animatingDifferences: false)` で section 単位の再描画を促すなどで実現できる。

3. `KsAnyView` 同型内の差し替えは `==` で「等しい」と判定される（`SectionAccessory` / `RootAccessory` の Equatable が view ケース同士を等価扱いしている）ため、`oldValue.header != root.header` だけでは検出できない。**`KsAnyView` の差し替えは「内容 hashable 不能だが新インスタンス」として強制 refresh する**シグナルを別途立てる必要がある（例: 毎回 `view` ケースなら無条件に refresh）。

4. 上記修正に対応するテストを追加すること。例:

   - `.text("A")` → `.text("B")` で supplementary view の `UIListContentConfiguration.text` が "B" に更新されていること
   - `.view(KsAnyView.swiftUI { Text("v1") })` → `.view(KsAnyView.swiftUI { Text("v2") })` で `contentConfiguration` が新規インスタンスに置き換わっていること（identical 比較で別オブジェクトであること）

---

### #### 🟡 Minor: `view.subviews` から `UICollectionView` を取り出す Spec シナリオの文言と実装の乖離

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:113-121`

**問題点**:

Spec「UICollectionView のレイアウト」の Scenario:

> WHEN `view.subviews` に含まれる `UICollectionView` のレイアウトを取得する  
> THEN 取得したレイアウトは `UICollectionViewCompositionalLayout` であり、内部設定は List ベースである

実装は `loadView()` 内で `self.view = cv`（UICollectionView 自身をルートビューとして採用）としているため、`view.subviews` には UICollectionView 自身は含まれない（`view.subviews` で取得できるのは UICollectionView の内部子ビューになる）。

実テスト（`KsSettingsViewControllerTests.test_UICollectionViewはCompositionalLayoutで構成される`）では `controller.internalCollectionView.collectionViewLayout` 経由で検証しており、Spec 文面の `view.subviews` 取得方法とはルートが違う。

**推奨修正**:

どちらかを選択:

1. 設計どおりルート view を別 UIView にし、UICollectionView を `view.addSubview(cv)` する形に変更する。SafeArea inset などのカスタマイズ余地が広がるメリットあり。
2. Spec の Scenario 文面を「`controller.view as? UICollectionView` のレイアウト、または内部 UICollectionView のレイアウト」に書き換えて整合させる（ただし `openspec/changes/<id>/specs/` の編集は本変更提案完了前なら可、既に PR マージ済みのため次の変更提案で訂正する形）。

**注**: 実害は小さい。実装は Spec の意図（`UICollectionViewCompositionalLayout` で List 構成されている）を満たしており、テストも代替経路で検証している。Spec 文言と完全一致させたいなら 1 を採るのが望ましい。

---

### #### 🟡 Minor: Spec Scenario「`UICollectionLayoutListConfiguration.appearance` が `.plain` / `.insetGrouped` に設定」の検証回避

**該当箇所**: `ios/Tests/KsSettingsViewUITests/KsSettingsViewStyleTests.swift:6-12`

**問題点**:

Spec の `classic スタイルの Appearance` / `modern スタイルの Appearance` Scenario は appearance の値検証を要求している。テストではコメントで「`UICollectionLayoutListConfiguration.appearance` を直接読み取る公開 API は存在しない」として、`.classic` / `.modern` の保持と layout インスタンス差し替えだけで満足させている。

UIKit 公開 API では確かに `NSCollectionLayoutSection` から listConfiguration を逆引きする手段はないが、`KsSettingsViewController` 内部の `appearance(for:)` ヘルパは型変換ロジックそのものなので、**`internal` でも `appearance(for:)` を露出させて単体テストすれば**、変換マッピングを完全に検証できる。

**推奨修正**:

`KsSettingsViewController` 内の `private func appearance(for:)` を `internal static func appearance(for style: KsSettingsViewStyle) -> UICollectionLayoutListConfiguration.Appearance` に昇格し、`@testable import` から直接テスト。例:

```swift
func test_classicはplain() {
    XCTAssertEqual(KsSettingsViewController.appearance(for: .classic), .plain)
}
func test_modernはinsetGrouped() {
    XCTAssertEqual(KsSettingsViewController.appearance(for: .modern), .insetGrouped)
}
```

これで Spec の Scenario テキストと検証内容が一致する。

---

### #### 🔵 Suggestion: `KsCellID.contentHash` に `Hasher` シードランダム化の影響範囲を明記

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsCellID.swift:30-33`

**指摘**:

`AnyHashable(cell).hashValue` を `contentHash: Int` に保存しているが、これは `KsCellID` を `Hashable` 自動合成する際の `id` + `contentHash` の合成材料に組み込まれている。プロセス内で一貫しているので動作するが、`contentHash` 衝突時に「id 一致 + contentHash 衝突 → 等価扱い」となり、内容が違っていても snapshot 差分なしと誤判定されるレアケースが残る。

`UUID`（`KsCell.id`）と `contentHash` のペアでは衝突確率は実質ゼロに近いが、Cell 設計者が `id` を固定値で再利用するパターン（同じ Cell スロットを更新するつもりで `id` を流用）で hash 衝突した場合に「内容変更が検出されない」というバグが顕在化する。

**推奨**:

ドキュメント上に「同一 `id` で内容を変更する運用を推奨する場合、衝突確率は約 1 / 2^63 程度」「衝突回避の確実性が必要なら `id` を必ず変えるか、新たに contentVersion フィールドを Cell に持たせる」等の注意書きを追加。あるいは `KsCellID` の `Equatable` を `id` のみで判定する設計に切り替え、内容変更には `reconfigureItems` を使うパターンを採用する。本変更提案では現状維持で良いが、後続の `add-cell-types-*` で考慮すべき。

---

### #### 🔵 Suggestion: PoCLabelCell の自動登録による KsCellRegistry.shared 汚染

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:91`

**指摘**:

`KsSettingsViewController.init` 内で常に `registry.register(cellType: PoCLabelCell.self, ...)` を呼ぶ。`registry: KsCellRegistry = .shared` がデフォルトのため、テストで独立 registry を渡さない場合は `shared` に PoCLabelCell が常に残る（`removeAll` で消えるが、その後別テストで Controller を生成すれば再登録される）。

PoC は本変更提案完了後 `add-cell-types-basic` で削除予定なので、Phase 1 では許容できる。ただし `registry` を DI で注入できる設計があるのに「常時 shared に登録する」のは設計意図と若干矛盾する。

**推奨**:

`registry == .shared` のときだけ自動登録する、もしくは引数フラグ `autoRegisterPoC: Bool = true` を設けてテストで切れるようにする。本変更提案では Suggestion レベルで OK。

---

## アクションプラン

### 優先度: 必須（Major / Critical）

1. **Section/Root H/F の中身更新で再描画されないバグ修正**
   - `KsSettingsViewController.root` の didSet で「装飾領域の中身が同型内で変わったとき」に supplementary view を強制 refresh する処理を追加
   - `KsAnyView` ケースの場合は等価判定で検出できないため、毎回 refresh する戦略でよい
   - 対応するテストを `SectionAccessoryRenderingTests` / `RootAccessoryRenderingTests` に追加（テキスト更新検証 + view ケースの contentConfiguration 差し替え検証）

### 優先度: 推奨（Minor）

2. Spec「UICollectionView のレイアウト」シナリオの `view.subviews` 文言と実装の整合性向上（実装変更 or 後続変更提案で spec 文言修正）
3. `appearance(for:)` ヘルパを `internal static` に昇格し、Spec の Appearance 値検証を直接テスト

### 優先度: 任意（Suggestion）

4. `KsCellID.contentHash` の hash 衝突リスクをドキュメントに明記
5. `PoCLabelCell` の自動登録ロジックを DI 戦略と整合する形に調整（`registry == .shared` 時のみ等）

---

## 良い点（評価）

- **タスク・実装・テストが網羅的に対応**: 全 35 タスク完了、すべてのチェックボックスが正しくマーク済み。テスト件数も 86 件（macOS 50 + iOS Simulator 上で UI 29 + SwiftUI 9 + Core 48）と十分。
- **設計判断（Decision 1〜6）に忠実**: `UICollectionViewCompositionalLayout` + `.list(using:)`、`KsCellRegistry` シングルトン + DI 併用、`UIViewControllerRepresentable` ラッパ、`@resultBuilder` DSL いずれも設計通り実装されている。
- **`KsAnyView.backing` の公開**: Decision 5c で要求された UI 層からの switch 用に Core 側で `Backing` 型と `backing` プロパティを `public` 化（コミット履歴で確認）。Core 仕様変更は最小限。
- **メモリリーク対策**: `deinit` で DataSource / Delegate / index を明示解放しており、`MemoryLeakTests` で `weak var` が `nil` になることを検証。
- **DSL 曖昧性回避の対策**: `import SwiftUI` 時の `Section` 名衝突を `KsSection` typealias と `ksSection(_:footer:cells:)` トップレベル関数で二重に救済。実用性が高い。
- **PoC Cell の責務切り出し**: `PoCLabelCell` が `internal` 公開で、本変更提案完了後 `add-cell-types-basic` で差し替え可能な設計に整理されている。
- **テスト容易化フック**: SwiftUI 側で `Context` 直接生成不可問題を `makeController()` / `applyUpdate(to:coordinator:)` の純粋関数化で回避し、まともにテスト可能にしている。
- **ドキュメント `docs/ios-ui.md`**: 利用方法・スタイル切替・装飾領域・DSL・独自 Cell 追加手順・テーマ合成・テスト方針が網羅的にまとめられている。

---

## 判定結果

**ステータス**: `CHANGES_REQUESTED`

理由:
- Spec の「Section H/F view 形式ヘッダの中身更新（差分検出非対応）」「Root Header の中身更新（差分検出非対応）」Scenario が、実装上動かない可能性が高い（Major #1）。
- 当該シナリオに対応するテストは "supplementary view が取得できる" 止まりで、Spec の意図する「中身が再描画される」を実検証していない。
- 上記は Phase 1 完成段階で「装飾領域に View を埋め込んで動的更新」という主要ユースケースのコア部分にあたるため、後続変更提案 (`add-cell-types-custom` 等) の前提として確実に動く状態にしておく必要がある。
- Minor / Suggestion 群は本判定に直接影響しないが、Major 修正と合わせて対応するのが望ましい。

修正完了後、再レビューを推奨。
