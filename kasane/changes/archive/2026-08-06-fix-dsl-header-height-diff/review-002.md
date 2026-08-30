# レビュー結果: fix-dsl-header-height-diff (002 回目)

**日付**: 2026-08-05
**判定**: APPROVED

## サマリー

前回サイクルで修正を指示された 5 項目はいずれも解消しており、修正による退行も確認できなかった。iOS の preflight 2 分岐 (headerHeight 変化時は `.full` + `.replaceCell`、可視性のみは `.full` のみ) は spec の要求どおりに成立し、Section 追加/削除・可視性・headerHeight の三重併発でも取りこぼしは生じない。Store 経由テストは `KsSettingsViewController(store:)` → `connectStore` → `diffPublisher.sink` の実経路を通っており、しかも DSL 実適用経路 (`KsSettingsView.applyDiffToStore` が `store.replaceAll` → `store.replaceCell` を呼ぶ) と同一形になったため、proxy としての妥当性も高い。新規指摘は Minor 1 / Suggestion 3 で、いずれも本 change のアーカイブを妨げない。

## 実施した検証

- **Android**: `cd android && ./gradlew test --rerun-tasks` → **2014 tests / 0 failures / 0 errors** (debug + release 両 variant、`build/test-results/**/TEST-*.xml` 集計)。`:ks-settingsview-compose:test --rerun-tasks` 単独も BUILD SUCCESSFUL
- **iOS**: `cd ios && xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,id=<ios-simulator-udid>' (iPhone 17 Pro / iOS 26.1)` → **Executed 415 tests, with 0 failures**
- 新規/変更テスト 11 件がシミュレータで実際に実行され pass することをテスト名で個別確認 (`DSLDiffCalculatorTests` 8 件 + `SectionAccessoryRenderingTests` 3 件)
- `python3 scripts/comment-policy-lint.py <touched 5 files>` → **禁止 0 件**。`--selftest` 全件 OK (検査が無音化していないことを確認)
- 触った 5 ファイルを `refactor-display-state-sync|purify-core|add-partial-update|add-visibility-flags|add-cell-types|Phase [0-9]|Decision [0-9]|Round [0-9]|review-result|openspec/` で grep → **0 件** (lint が機械検出できない kebab-case change-id / 拡張子なし裸参照も残っていない)
- 検証用の一時ミューテーション (`containsHeaderHeightChange` 先頭の `return false`) が残っていないことをコードで確認。`samples/` に未コミット変更が無いことを `git status` で確認 (検証用の一時ボタン・一時状態の削除が実際に済んでいる)
- `KsSettingsViewController.applyFullSnapshot` が `self.root = root` + `rebuildModelIndexes()` を行うことを確認 (`.full` 直後の `.replaceCell` が可視性切替の誤検出でフルフォールバックしないことの根拠)

## 前回指摘 1〜5 の解消確認

### 1. [Major] iOS preflight の順序による `.replaceCell` 欠落 → **解消**

`ios/Sources/KsSettingsViewSwiftUI/DSLDiffCalculator.swift:73-85` で headerHeight 分岐 (`.full` + `contentUpdateDiffs`) が先、可視性のみ分岐 (`.full` のみ) が後になった。指示どおり「headerHeight 変化時は可視性変化が同時でも `.replaceCell` を保持」「可視性のみは `.full` のみ」の 2 分岐が成立している。可視性のみの分岐は変更されておらず、core/ADR-0010 と Non-Goal には踏み込んでいない。

テストで両分岐が固定されている:
- `DSLDiffCalculatorTests.swift` `test_別Sectionの可視性変更とheaderHeight変更の併発でもfullに続けてreplaceCellが発行される` (相方が要求した回帰テストそのもの)
- `test_可視性のみの変更ではfullのみが発行される` (可視性側の退行防止)

**新たな併発ケースの検討** (破綻しないことを確認済み):
- **Section 追加/削除 + 可視性 + headerHeight の三重併発**: headerHeight 分岐が `.full(新 sections)` を先頭に置くため構造は snapshot 再構築で吸収され、`contentUpdateDiffs` は新旧双方に存在する ID の Cell だけを対象にするので、追加された Cell へ誤った `.replaceCell` は出ない。削除された Cell は新ツリーに無いので走査対象外
- **Cell の Section 間移動 + headerHeight**: `.full` で構造が確定した後に ID 指定の `.replaceCell` が届くため、移動先で reconfigure される
- **旧ツリーに無い Section ID**: `containsHeaderHeightChange` は `guard let ... else { continue }` で読み飛ばす。insert として新規描画されるため検出しないのが正しい
- **可視性が hidden → visible に転じた Cell の内容変更**: `.full` 適用時点で `root` / `cellIndex` が新ツリーに更新されるため (`applyFullSnapshot:1151,1162`)、後続の `applyReplaceCell` が読む `oldCell` は既に新値。`oldVisible != newVisible` の可視性切替フォールバックには入らない
- **Root H/F の同時変化**: 早期 return で `updateAccessory` が落ちる点は前回 review-001 で確認済み (DSL 経路の Root H/F は wrapper が毎回直接適用するため表示上の取りこぼしなし)。本修正でこの性質は変わっていない

### 2. [Major] 「Store 経由」テストが Store を通っていない → **解消**

`SectionAccessoryRenderingTests.swift:398-415` の `hostStoreConnectedControllerInWindow(store:)` は `KsSettingsViewController(store:)` を使う。これは `KsSettingsViewController.swift:145-162` の public convenience init で、`connectStore(store)` → `store.diffPublisher.sink { self?.applyDiff($0) }` (`:286-292`) を張る。テストの更新操作は `store.replaceSection(...)` / `store.replaceAll(...)` / `store.replaceCell(...)` の公開 API に置き換わっており、**Store → Publisher → Controller の経路を実際に通る**。

さらに `KsSettingsView.swift:403-407` の `applyDiffToStore` は `.full` を `store.replaceAll`、`.replaceCell` を `store.replaceCell` に写す。つまり新テストの操作列は DSL 経路の実適用列そのものであり、proxy として妥当。

### 3. [Minor] 同時変更 Scenario の THEN が表示結果を要求しているのに diff 列しか見ていない → **解消**

`test_Store経由のfull直後のreplaceCellでheader高さとCell内容の両方が表示へ反映される` (`:634-663` 付近) が、表示中 header の実 frame 高さ (40 → 90) と表示中 Cell の実 title (`旧タイトル` → `新タイトル`) を前後対で観測する。

回帰検出力も確認した: `applyFullSnapshot` の `reloadSections` 条件は `oldSection.header != section.header || oldSection.footer != section.footer` (`:1180-1182`) であり、本テストは header accessory を据え置くため reload されない。したがって `.replaceCell` を落とせば Cell は `旧タイトル` のまま残り、テストは落ちる。トートロジーではない。

### 4. [Minor] 証跡 README の実機シリアル・Simulator UUID → **解消**

`verification/README.md` の記載は `実機: Pixel 6a / Android 16` と `シミュレータ: iPhone 17 Pro / iOS 26.1` のみ。シリアル・UUID とも残っていない。

### 5. [Minor/Suggestion] Android の preflight 条件二重定義とコメント債務 → **解消**

`DSLDiffCalculator.kt:286-289` に `private fun requiresFullRefresh(from:to:)` が切られ、`compute:48` と `contentUpdates:255` の双方がこれを呼ぶ。抽出前後の条件式の等価性を確認した:

- 抽出前 (前サイクルの実装): 両所とも `containsVisibilityChange(from, to) || containsHeaderHeightChange(from, to)`
- 抽出後: `requiresFullRefresh = containsVisibilityChange(from, to) || containsHeaderHeightChange(from, to)` を両所が呼ぶ

引数の受け渡し・短絡評価の順序・戻り値の使われ方 (`compute` は `Full` 単発 return、`contentUpdates` は `emptyList()` return) いずれも変わっておらず、**完全に等価**。単一定義になったことでコメントの口約束 (`[compute] 側の preflight 判定と条件を一致させる必要がある`) も不要になった。

コメント債務は上記「実施した検証」のとおり lint / grep とも 0 件。

## 指摘事項

### [🟡 Minor] 新規 Store 経由テスト 3 件が controller の強参照を捨てており、検証経路が ARC の未規定な延命に依存している

**該当箇所**: `ios/Tests/KsSettingsViewUITests/SectionAccessoryRenderingTests.swift:550`, `:587`, `:625`

**問題点**: 3 件とも `let (_, cv, window) = hostStoreConnectedControllerInWindow(store: store)` と書かれ、controller を `_` で捨てている。同一ファイルの既存テスト 4 箇所 (`:442` `:467` `:492` `:664`) はいずれも `controller` に束縛しており、新テストだけが例外。

Store → Controller の経路を保持しているのは controller が持つ `storeSubscription` だけである (`KsSettingsViewController.swift:121`)。そして本リポジトリは「controller は強参照が消えたら deinit される」ことを明示的な不変条件としており、**Store 接続経路も含めて** `MemoryLeakTests.swift:34-58` (`test_Store経由でもControllerがdeinitされStore購読が解除される`) がそれを固定している。実際 controller 内の Combine sink・layout provider は全て `[weak self]` で、UIWindow は subview の view controller を retain しない。

つまり controller の唯一の強参照は破棄されたタプル要素であり、いま 3 件が pass しているのは -Onone でのスタック一時変数の寿命に依存している可能性が高い。ARC は `_` に対する延命を保証しないため、最適化条件や toolchain が変われば `deinit` → `storeSubscription.cancel()` が走り、3 件が同時に落ちる。**幸い落ち方は「静かに検証されなくなる」ではなく assert 失敗なので false-green にはならないが、修正 2 で得た「経路を通す」保証の耐久性が損なわれる。**

**推奨修正**: `let (controller, cv, window) = hostStoreConnectedControllerInWindow(store: store)` と名前付きで束縛し、検証区間を `withExtendedLifetime(controller) { ... }` で包むか、`defer { withExtendedLifetime(controller) {} }` を置く。最小でも `defer { _ = controller }` で寿命を明示する。

### [🔵 Suggestion] iOS 実装の「可視 Cell 限定」の narrowing が change アーティファクトのどこにも記録されていない

**該当箇所**: `ios/Sources/KsSettingsViewSwiftUI/DSLDiffCalculator.swift:203-216` (`contentUpdateDiffs` の `for section in new where section.isVisible` と `guard (cell as? VisibilityAware)?.isVisible ?? true`)

**問題点**: iOS デルタスペックの SHALL は「同一再評価内で同一 ID の Cell の内容も変わっている場合、`.full(newRoot)` に続けて当該 Cell の `.replaceCell` を発行する」と無条件に書かれている。実装は新ツリーで可視な Cell のみに絞っており、文言の厳密な読みでは narrowing にあたる。`deviation.md` も無い。

**評価**: 表示結果は同値であり、実装として正しいと判断する。根拠:
- 絞り込み条件は Controller の `computeVisibleSections` (`KsSettingsViewController.swift:252-256`) と完全に同一 (Section の `isVisible` と Cell の `VisibilityAware.isVisible` の両方) であり、`.full` 適用後の snapshot に載る集合と一致する。**除外された Cell は snapshot に存在しないので `.replaceCell` の reconfigure 対象を持たない** — 本来必要な `.replaceCell` を落としてはいない
- 逆向き (hidden → visible で内容変化) は新ツリーで可視なので対象に含まれる。取りこぼしはない
- 絞らなくても `applyReplaceCell` の `:1566-1571` が「snapshot に無い場合は自然な no-op」として扱うため、絞り込みは最適化であって挙動変更ではない
- `test_headerHeight変更時に非表示CellのreplaceCellは発行されない` が非表示 Cell・非表示 Section 内 Cell の双方で固定している

**推奨修正**: 本 change 内での修正は不要。蒸留時に「iOS の `.replaceCell` 追随は visible projection に限る」を concepts (`core/architecture/display-state-synchronization.md`) 側の記述として拾い上げること。spec 文言の追随判断はオーナー/蒸留の領分なので、レビューからの spec 書き換え指示はしない。

### [🔵 Suggestion] Android 全件テストの間欠失敗が前回より悪化している (本 diff 非関与)

**該当箇所**: `:ks-settingsview-ui:testReleaseUnitTest` (本 diff が触っていないモジュール)

**問題点**: 本レビュー中の実測:

| 実行 | 結果 |
|---|---|
| `./gradlew test --rerun-tasks` 1 回目 | **FAILED** — 14 テストクラスが `initializationError: java.lang.ClassNotFoundException` |
| `:ks-settingsview-ui:testReleaseUnitTest --rerun-tasks` 単独 1 回目 | **FAILED** — 33 件が同じ `ClassNotFoundException` |
| 同 単独 2 回目 | SUCCESS (792 tests / 0 failures) |
| `./gradlew test --rerun-tasks` 2 回目 | SUCCESS (2014 tests / 0 failures) |

`.class` ファイル自体は `ks-settingsview-ui/build/tmp/kotlin-classes/releaseUnitTest/` に存在しており、テスト worker のクラスパス解決が壊れる形の失敗。アサーション失敗ではない。review-001 Suggestion 6 では「単独実行では常に成功」とされていたが、**今回は単独実行でも 1 回失敗した**ため、切り分けの前提が変わっている。

**評価**: 本 diff の Android 側変更は `ks-settingsview-compose` に閉じており、`ks-settingsview-ui` は依存方向的にも本変更の影響を受けない (compose → ui の一方向)。**本変更起因ではない**と判断する。ただし `test-execution.md` の「実行件数の確認までが検証」という規律が、この不安定さで実質的に「何度か回して green を拾う」運用になっている。

**推奨修正**: 本 change では対応不要。review-001 Suggestion 6 の起票に「単独実行でも再現した」「症状は `ClassNotFoundException` によるテストクラスのロード失敗」を追記して別途調査する。

### [🔵 Suggestion] 触っていない周辺ファイルに同型のコメント債務が残っている

**該当箇所**: `ios/Sources/KsSettingsViewSwiftUI/KsSettingsView.swift:394`、`ios/Tests/KsSettingsViewUITests/MemoryLeakTests.swift:5`、`ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:1148`, `:1512`

**問題点**: それぞれ `purify-core-extract-style-to-ui-layer Decision 5` / `本提案 add-partial-update-native` / 拡張子なしの裸 Requirement 参照 / `add-visibility-flags-section-and-cell` が残る。`comment-policy.md` の禁止参照だが、lint の BLOCKING パターンでは kebab-case change-id を機械検出できないため 0 件と出る。

**評価**: ラチェット方式では既存債務であり本変更の違反ではない (本変更は触ったファイルの同型債務を完済している)。特に `KsSettingsView.swift:394` は本 change が扱った `DSLDiffCalculator.swift` の直接の呼び出し側であり、対になる 2 ファイルで片方だけ残る形になっている。

**推奨修正**: 本 change の範囲外。drift 側の棚卸し対象として扱う。

## 退行チェック (前回 APPROVED 部分が壊れていないこと)

- Android: `compute` の可視性 preflight の挙動は `requiresFullRefresh` 抽出で変わらず、既存テスト含め 2014 件全 pass
- iOS: `containsVisibilityChange` 本体・`cellLevelDiffs` の内容比較・Section H/F 経路はいずれも無変更。既存テスト `test_fullDiffでheader不変ならCellは再構成されない` / `test_fullDiffのtextヘッダ変更が表示中のsupplementaryに反映される` を含む 415 件全 pass
- 足場アーティファクト: `proposal.md` / `specs/` は無変更。`tasks.md` の差分はチェックボックス 11 個のみで、1.1〜4.2 の全項目に対応する実装・テスト・証跡の実体を確認済み (虚偽チェックなし)
- `Section` は iOS / Android とも `headerHeight` を等価判定に含むため、`compute` 冒頭の完全一致早期 return が headerHeight 差を飲み込むことはない (`ios/Sources/KsSettingsViewCore/Section.swift:95`)
- 一時的なミューテーション用コードの残置なし

## アクションプラン

1. (任意・本 change 内) Minor 1 — 新規 Store テスト 3 件で controller を名前付き束縛し `withExtendedLifetime` で寿命を明示する。修正コスト極小・退行リスクなし
2. (蒸留申し送り) Suggestion 1 — 「iOS の `.replaceCell` 追随は visible projection に限る」を concepts へ拾う
3. (別 change 起票の追記) Suggestion 2 — Android 全件テスト間欠失敗の切り分けに今回の実測 (単独でも再現・`ClassNotFoundException`) を追加
4. (drift) Suggestion 3 — 周辺ファイルのコメント債務

Critical / Major はなく、1 を未対応のままでも本 change はアーカイブ可能と判断する。
