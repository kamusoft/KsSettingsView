# Verify 002: fix-dsl-header-height-diff (修正サイクル 1 周目後の再検証)

- 検証日: 2026-08-05
- 検証対象: 作業ツリーの未コミット変更 (HEAD = `5f7d97e`)
- 対象ドメイン: android + ios
- デルタスペック: `specs/settings-view-android-ui/spec.md` (Requirement 1 / Scenario 3)、`specs/settings-view-ios-ui/spec.md` (Requirement 2 / Scenario 5)
- 前回: `verify-001.md` (VALID)。その後 review-001 / second-opinion-002 の指摘で実装・テストが変更されたため、対応表を全 8 Scenario 分作り直した
- 判定: **VALID**

---

## 0. 前回からの差分 (再検証の必要が生じた箇所)

| # | 修正内容 | 影響する Scenario |
|---|---|---|
| 1 | iOS `DSLDiffCalculator.swift`: preflight を「headerHeight 変化あり」→「可視性のみ変化」の順へ入れ替え。`contentUpdateDiffs` を新ツリーで可視な Cell のみへ限定 | iOS DSL 全 3 |
| 2 | iOS `SectionAccessoryRenderingTests.swift`: Store テストを `SettingsRootStore` の公開操作 (`replaceSection` / `replaceAll` / `replaceCell`) 経由へ差し替え、表示中 header の実高さ + 表示中 Cell の title を観測するテストを追加 | iOS Store 2 / iOS DSL Scenario 2 |
| 3 | iOS `DSLDiffCalculatorTests.swift`: 併発・可視性のみ・非表示 Cell 除外の 3 テストを追加 | iOS DSL 全 3 |
| 4 | Android `DSLDiffCalculator.kt`: preflight 条件を `requiresFullRefresh` へ抽出 (振る舞い不変) | Android 全 3 |
| 5 | `verification/README.md`: 一意識別子 (実機シリアル) の削除と記述更新 | (証跡整備) |

---

## 1. 対応表: settings-view-android-ui

### Requirement: Compose DSL の headerHeight 変更の表示反映

実装本体: `android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/DSLDiffCalculator.kt`

- `compute` の preflight: `:50-52` (`requiresFullRefresh` → `listOf(Full(SettingsRoot(sections = to.sections)))`)
- `contentUpdates` の同条件早期 return: `:257-259` (→ `emptyList()`)
- **単一定義の判定**: `requiresFullRefresh` `:289-291` (`containsVisibilityChange || containsHeaderHeightChange`)。前回は `compute` / `contentUpdates` に同じ複合条件が二重定義されていたが、抽出により両者が同一の判定を通ることが構造上保証された (振る舞いは不変)
- headerHeight 判定関数: `containsHeaderHeightChange` `:340-350` (同一 Section ID の `headerHeight` を `Double` 等値比較。正値間・`-1.0 → 正値`・`正値 → -1.0` を区別せず検出)

| Scenario | 実装 | テスト (関数名) | 状態 |
|---|---|---|---|
| headerHeight のみの変更が表示へ反映される | `DSLDiffCalculator.kt:50-52`, `:289-291`, `:340-350` | **diff 側**: `DSLDiffCalculatorTest.kt:269` `headerHeight が正値間で変化すると Full のみ発行され contentUpdates は空` / `:280` `…自動から固定へ…` / `:291` `…固定から自動へ…` (遷移 3 種を個別関数で網羅。いずれも `diffs.size == 1` + `Full.root…headerHeight` + `contentUpdates == emptyList()`)<br>**表示側**: `FullUpdateContentSyncTest.kt:238` `Full diff による headerHeight 変更が表示と payload 付き通知に反映される` (既存テスト) | ✅ 一致 |
| headerHeight と Cell 内容の同時変更で両方が反映される | 同上 (`Full` のみ + `contentUpdates` 空) | **diff 側**: `DSLDiffCalculatorTest.kt:302` `headerHeight と Cell 内容の同時変更でも Full のみ発行され contentUpdates は空` (`Full` の root に新 headerHeight と新 title の両方が載ることを検証)<br>**表示側**: `FullUpdateContentSyncTest.kt:238` + `:512` `Full diff で同一 id の Cell 内容変更が表示へ反映される` (既存テスト) | ✅ 一致 |
| headerHeight が不変なら preflight は発火しない | `requiresFullRefresh` が `false` → 既存経路へ抜ける | `DSLDiffCalculatorTest.kt:321` `headerHeight 不変で内容だけ変わると Full を発行せず contentUpdates で列挙される` (`compute` が空リスト・`contentUpdates` に 1 件)、補助として `:337` `containsHeaderHeightChange は同一 headerHeight で false` | ✅ 一致 |

**`requiresFullRefresh` 抽出後も同じテストで担保されているかの確認 (今回の検証点)**:
- 抽出は `compute` / `contentUpdates` の呼び出し先を差し替えただけで、条件式 (`containsVisibilityChange || containsHeaderHeightChange`) も分岐の戻り値も不変。**テストファイル `DSLDiffCalculatorTest.kt` は前回から一切変更されていない** (`git diff` 上、Android テストの差分は前回検証時と同一)
- 上表 4 テスト (遷移 3 種 + 同時変更) がすべて `assertEquals(emptyList<Cell>(), DSLDiffCalculator.contentUpdates(old, new))` を含むため、抽出で `contentUpdates` 側の分岐が壊れれば全件が落ちる。抽出の退行検出力は担保されている
- 既存の可視性 preflight 側も `ks-settingsview-compose` の全件テスト green で退行なし

---

## 2. 対応表: settings-view-ios-ui

### Requirement: SwiftUI DSL の headerHeight 変更の表示反映

実装本体: `ios/Sources/KsSettingsViewSwiftUI/DSLDiffCalculator.swift`

- **headerHeight preflight (先)**: `:73-77` — `[.full(SettingsRoot(sections: new.sections))]` を先頭に確定させ、`contentUpdateDiffs` を `append(contentsOf:)` で後続に連結して return
- **可視性 preflight (後)**: `:83-85` — headerHeight 不変時のみ到達し、`.full` のみを返す
- 判定関数: `containsHeaderHeightChange` `:180-195`
- 内容更新列: `contentUpdateDiffs` `:207-228` (新ツリーで**可視な** Section / Cell に限定し、`AnyHashable` 比較で `.replaceCell` を生成)

| Scenario | 実装 | テスト (関数名) | 状態 |
|---|---|---|---|
| headerHeight のみの変更が表示へ反映される | `DSLDiffCalculator.swift:73-77`, `:180-195` | **diff 側**: `DSLDiffCalculatorTests.swift:268` `test_headerHeightが正値間で変わるとfullが発行される` / `:281` `…自動から固定へ…` / `:294` `…固定から自動へ…` (遷移 3 種を個別関数で網羅。`diffs.count == 1` + `.full` の当該 Section の `headerHeight` 値を検証)<br>**表示側**: `SectionAccessoryRenderingTests.swift:580` `test_Store経由のreplaceAllのheaderHeight変更が表示中headerの実高さに反映される` (DSL preflight が発行する `.full` の適用先そのもの) | ✅ 一致 |
| headerHeight と Cell 内容の同時変更で両方が反映される | `DSLDiffCalculator.swift:73-77` (`.full` 先頭 + `contentUpdateDiffs` 後続) | **diff 側**: `DSLDiffCalculatorTests.swift:307` `test_headerHeightとCell内容の同時変更でfullに続けてreplaceCellが発行される` (`diffs[0]` = `.full` / `diffs[1]` = `.replaceCell` を添字指定で順序固定)<br>**表示側 (今回追加)**: `SectionAccessoryRenderingTests.swift:617` `test_Store経由のfull直後のreplaceCellでheader高さとCell内容の両方が表示へ反映される` (表示中 header の `frame.height` 40 → 90 と表示中 Cell の `titleLabel.text` 旧 → 新の**両方**を観測) | ✅ 一致 |
| headerHeight が不変なら preflight は発火しない | headerHeight preflight を通過 → 可視性 preflight (`:83-85`) も通過 → 通常の `cellLevelDiffs` 経路 | `DSLDiffCalculatorTests.swift:335` `test_headerHeight不変で内容のみ変わるとfullは発行されずreplaceCellが発行される` (`.full` が 1 件も含まれないことを明示否定 + `diffs.count == 1` + `.replaceCell` の cellID / 新 title) | ✅ 一致 |

**修正 1 (preflight 順序入れ替え) が Scenario 1 / 3 を壊していないかの確認 (今回の検証点)**:

| 入力の組み合わせ | 到達する分岐 | 期待どおりか | 固定しているテスト |
|---|---|---|---|
| headerHeight のみ変化 | `:73` headerHeight 側。`contentUpdateDiffs` は内容不変で空 → `.full` 1 件 | ✅ Scenario 1 の THEN どおり | `:268` / `:281` / `:294` (いずれも `diffs.count == 1` を検証しており、順序入れ替えで `.replaceCell` が余計に付けば落ちる) |
| headerHeight 変化 + 内容変化 | `:73` headerHeight 側 → `.full` + `.replaceCell` | ✅ Scenario 2 の THEN どおり | `:307` |
| headerHeight 変化 + 可視性変化 + 内容変化 (併発) | `:73` headerHeight 側が**先**に発火 → `.replaceCell` が落ちない | ✅ second-opinion-002 Major-1 の解消 | `:369` `test_別Sectionの可視性変更とheaderHeight変更の併発でもfullに続けてreplaceCellが発行される` |
| headerHeight 不変 + 可視性変化 | `:73` を素通り → `:83` 可視性側 → `.full` のみ | ✅ 従来挙動 (ADR-0010) の維持 | `:395` `test_可視性のみの変更ではfullのみが発行される` (`diffs.count == 1` + `.full` を検証) |
| headerHeight 不変 + 内容のみ変化 | 両 preflight を素通り → 通常経路 | ✅ Scenario 3 の THEN どおり | `:335` |

→ **順序入れ替えによる Scenario 1 / 3 の担保の破壊はない。** 5 通りの組み合わせがそれぞれ独立したテスト関数で固定されており、どの分岐へ倒れるかが観測されている。特に Scenario 1 の 3 テストは `diffs.count == 1` を主張しているため、`contentUpdateDiffs` の連結が誤って発火すれば必ず落ちる。

**`contentUpdateDiffs` の可視 Cell 限定について (注記 — ❌ ではない)**:
- Requirement 本文の SHALL は「同一再評価内で同一 ID の Cell の内容も変わっている場合、`.full` に続けて当該 Cell の `.replaceCell` を発行する」と無条件に書かれているのに対し、実装は**新ツリーで可視な Cell のみ**へ限定している (`:207-228`、`DSLDiffCalculatorTests.swift:418` `test_headerHeight変更時に非表示CellのreplaceCellは発行されない` で明示的に固定)
- ただしこの限定は**観測結果を変えない**ことをコードで確認した: `applyReplaceCell` (`KsSettingsViewController.swift:1517-1583`) は、snapshot に載らない hidden Cell を「自然な no-op」として扱い (`:1565-1571`)、表示への反映は起こさない。model 側は先行する `.full` が新 root ごと運ぶため (`:1533-1556` を経ずとも `applyFullSnapshot` が root を差し替える)、非表示のまま内容が変わった Cell は再表示時に新しい内容で構成される
- Requirement 本文の目的節「これにより header の高さと Cell の内容の**表示**へ反映される」および Scenario 2 の GIVEN/WHEN/THEN (可視 Cell が前提) はいずれも充足しており、Scenario 単位では乖離なし。**見立て**: 実装を直す必要はない。Requirement 本文の文言と実装スコープの差を厳密に残したい場合のみ、蒸留時に「非表示 Cell は `.full` の model 反映に委ねる」を concepts へ書き下すか deviation として記録すればよい (本 change の受け入れを妨げない)

### Requirement: Store 経由の headerHeight 変更の表示反映

実装本体: 本変更で Store / Controller のプロダクションコード変更はなし (既存挙動の確認が Requirement の趣旨)。観測点は `ios/Tests/KsSettingsViewUITests/SectionAccessoryRenderingTests.swift`。

| Scenario | 実装 (通る経路) | テスト (関数名) | 状態 |
|---|---|---|---|
| replaceSection による headerHeight 変更が表示へ反映される | `SettingsRootStore.replaceSection` (`SettingsRootStore.swift:117-124`) → `diffSubject.send(.replaceSection(...))` → `KsSettingsViewController.connectStore` の購読 (`KsSettingsViewController.swift:286-292`) → `applyDiff` | `SectionAccessoryRenderingTests.swift:543` `test_Store経由のreplaceSectionのheaderHeight変更が表示中headerの実高さに反映される` | ✅ 一致 |
| .full による headerHeight 変更が表示へ反映される | `SettingsRootStore.replaceAll` (`SettingsRootStore.swift:76-78`) → `diffSubject.send(.full(root))` → 同上 | `SectionAccessoryRenderingTests.swift:580` `test_Store経由のreplaceAllのheaderHeight変更が表示中headerの実高さに反映される` | ✅ 一致 |

**GIVEN「Store 接続で表示されている」の再現 (今回の最重要検証点)**:
- 新設ヘルパ `hostStoreConnectedControllerInWindow(store:)` (`:398-416`) が **`KsSettingsViewController(store:)` の public convenience init** (`KsSettingsViewController.swift:145-163`) で controller を生成する。この init は `connectStore(store)` を呼び、`store.diffPublisher` / `contentUpdateBatchPublisher` / `$theme` を購読する統合経路を確立する (`:286-307`)
- その controller の `view` を実 `UIWindow` に載せ、`layoutIfNeeded()` + `pump()` でレイアウトを確定させている。**GIVEN「Store 接続で表示されている」は文字どおり再現されている**
- 前回 verify-001 が「注記」に留めた `controller.applyDiff(...)` 直呼びは 3 テストすべてから消えている (`git diff` 上、`applyDiff` を呼ぶ新規テストは 0 件)

**WHEN「`replaceSection` する」/「`.full` を適用する」の再現**:
- WHEN の入口はそれぞれ `store.replaceSection(sectionID:new:)` (`:566`) と `store.replaceAll(_:)` (`:603`) の **Store 公開 API**。テストは diff 値を自作せず Store に作らせている
- **`replaceAll` が spec の `.full` に相当することの妥当性**: `SettingsRootStore.replaceAll` の実装は `:76-78` で `diffSubject.send(.full(root))` を送るだけであり、`SettingsRootDiff.full` を発行する **Store の唯一の公開操作**である (`grep` で `diffSubject.send(.full` は当該 1 箇所のみ)。したがって「Store 接続で `.full` を適用する」を Store の公開 API で表現する手段は `replaceAll` 以外に存在せず、対応は妥当。加えて iOS DSL の headerHeight preflight が発行する `.full` の適用先も同一の `applyDiff(.full(...))` であり、DSL 側 Scenario 1 の表示担保も兼ねる

**観測点が「表示中 header の高さ」であることの確認** (spec の THEN):
- `visibleHeaderFrameHeight` (`:515-523`) は `cv.supplementaryView(forElementKind:at:)` — **collection view が現に保持している表示中の view** を返す API — の `frame.height` を観測する。provider 経由の新規生成でも payload でも `controller.root` でもない
- `layoutHeaderHeight` (`:531-539`) で layout attributes 側も併せて検証し、二重に押さえている
- 事前条件として更新前の実高さが `40` であることを `XCTUnwrap` + `XCTAssertEqual(accuracy: 0.5)` で確認しており、`nil` 素通りの穴がない (前後対を取っているためトートロジーにもならない)

---

## 3. 遷移 3 種の両 platform カバレッジ (ADR-0018 の対称テスト義務)

| 遷移 | Android | iOS |
|---|---|---|
| 正値A → 正値B | `DSLDiffCalculatorTest.kt:269` (40.0 → 80.0) | `DSLDiffCalculatorTests.swift:268` (40 → 80) |
| -1 → 正値 (自動 → 固定) | `DSLDiffCalculatorTest.kt:280` (-1.0 → 80.0) | `DSLDiffCalculatorTests.swift:281` (-1 → 64) |
| 正値 → -1 (固定 → 自動) | `DSLDiffCalculatorTest.kt:291` (80.0 → -1.0) | `DSLDiffCalculatorTests.swift:294` (64 → -1) |

→ 両 platform で 3 遷移が独立したテスト関数として個別にカバーされている (前回から不変)。

ADR-0018 の 4 象限:

| 象限 | 自動テスト |
|---|---|
| Android Store | `FullUpdateContentSyncTest.kt:195` (replaceSection) / `:238` (Full) — 既存 |
| Android DSL | `DSLDiffCalculatorTest.kt:269` / `:280` / `:291` — 本変更 |
| iOS Store | `SectionAccessoryRenderingTests.swift:543` (replaceSection) / `:580` (replaceAll = `.full`) — 本変更で **Store 公開 API 経由へ格上げ** |
| iOS DSL | `DSLDiffCalculatorTests.swift:268` / `:281` / `:294` — 本変更 |

---

## 4. tasks.md の実体確認

| タスク | 実体 | 状態 |
|---|---|---|
| 1.1 Android preflight 検出 | `DSLDiffCalculator.kt:50-52` / `:257-259` / `:289-291` / `:340-350` | ✅ |
| 1.2 Android 対称テスト (遷移 3 種) | `DSLDiffCalculatorTest.kt:269` / `:280` / `:291` | ✅ |
| 1.3 Android 同時変更テスト | `DSLDiffCalculatorTest.kt:302` | ✅ |
| 1.4 Android 退行防止テスト | `DSLDiffCalculatorTest.kt:321` (+ `:337`) | ✅ |
| 2.1 iOS preflight 検出 + `.full` 後続 `.replaceCell` | `DSLDiffCalculator.swift:73-77` / `:180-195` / `:207-228` | ✅ |
| 2.2 iOS 対称テスト (遷移 3 種) | `DSLDiffCalculatorTests.swift:268` / `:281` / `:294` | ✅ |
| 2.3 iOS 順序検証テスト | `DSLDiffCalculatorTests.swift:307` (添字指定で `.full` → `.replaceCell`) + 表示結果 `SectionAccessoryRenderingTests.swift:617` | ✅ |
| 2.4 iOS 退行防止テスト | `DSLDiffCalculatorTests.swift:335` (+ 可視性退行防止 `:395`) | ✅ |
| 3.1 Store `replaceSection` / `.full` の実高さ観測 XCTest (「目視をテストの代替にしない」) | `SectionAccessoryRenderingTests.swift:543` / `:580` — **Store の公開 API から Publisher 経由で controller へ届く経路**を通し、表示中 supplementary の `frame.height` + layout attributes を観測。目視 (`11-ios-…png`) には依存していない | ✅ |
| 4.1 Android A/B 証跡 | `verification/01`〜`05` + README.md (Pixel 6a / Android 16) | ✅ |
| 4.2 iOS A/B 証跡 | `verification/06`〜`11` + README.md (iPhone 17 Pro / iOS 26.1) | ✅ |

**虚偽チェックなし。** 特に 3.1 は前回「テストの入口が Store public API でない」ことを注記していたが、今回はタスク文言 (「Store `replaceSection` および `.full` で」) と実体が完全に一致している。

証跡 (`verification/`) は前回検証で画像を実見して A/B 対比の成立を確認済み。今回の変更点は README の記述のみで、**実機シリアル等の一意識別子は削除されており** (Android は `Pixel 6a / Android 16`、iOS は `iPhone 17 Pro / iOS 26.1` のみ)、README 末尾には Store 経路と `.full` → `.replaceCell` の自動テスト化が追記されている (証跡の主張とテストの実体が整合)。

---

## 5. 追加検査

- **逆流検査**: `git status` / `git diff HEAD` により、足場アーティファクト `proposal.md` / `exploration.md` / `specs/**` は `5f7d97e` から一切変更されていないことを確認。`kasane/changes/` 配下の tracked な差分は `tasks.md` のみで、内容は**チェックボックス 11 個の `[ ]` → `[x]`** だけ (本文の書き換えなし)。**逆流なし**
- **未記録乖離**: `deviation.md` は存在しない。対応表に ❌ は 0 件のため、記録すべき未記録乖離もなし (§2 の「可視 Cell 限定」は Scenario 単位では乖離なし。見立ては当該節に記載)
- **UI 変更の brief/mock**: 本変更に `ui/` アーティファクトはない (視覚パラメータの変更ではなく既存の宣言値を表示へ届ける修正)。ゲート不適用
- **テスト実行** (`concepts/cross/conventions/test-execution.md` に従い実行):
  - **iOS**: `cd ios && xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,id=<ios-simulator-udid>…' (iPhone 17 Pro)` → **`Executed 415 tests, with 0 failures`** / `** TEST SUCCEEDED **`。新規/変更 10 テスト (SwiftUI DSL 7 + UI 3) がログ上で個別に `passed` として実行済みであることを確認
  - **Android**: `cd android && ./gradlew test --rerun-tasks` → XML 集計 **2014 件 (debug/release 両 variant) / failures 0 / errors 0**。新規 6 テストが `TEST-…DSLDiffCalculatorTest.xml` の testcase として実在し全て pass
  - **注記 (前回と同一の既知事象)**: 全件 `--rerun-tasks` 実行で `:ks-settingsview-ui:testReleaseUnitTest` が 30 クラス `ClassNotFoundException (initializationError)` で落ちた。同 variant の `testDebugUnitTest` は同一実行内で成功しており、同タスクを単独で `--rerun-tasks` 再実行すると `BUILD SUCCESSFUL`。**本変更の Android 側は `ks-settingsview-compose` に閉じており、`ks-settingsview-ui` のプロダクション/テストコードには 1 行も触れていない**ため、本変更起因ではない。review-001 Suggestion 6 (7 回中 2 回再現) と同じ環境起因の間欠事象で、別 change として起票済みの扱い。テスト内容の失敗ではないため ❌ としない

---

## 6. 総合判定

**VALID**

- Android 3 Scenario / iOS 5 Scenario の計 8 Scenario すべてが「✅ 一致」。❌ は 0 件
- **iOS Store Requirement の 2 Scenario は Store の公開 API を実際に通るテストで担保されている**。GIVEN は `KsSettingsViewController(store:)` + `UIWindow` 実装で再現、WHEN は `store.replaceSection` / `store.replaceAll`、THEN は表示中 supplementary の `frame.height` (+ layout attributes)。`replaceAll` は `SettingsRootDiff.full` を発行する Store 唯一の公開操作であり、spec の `.full` への対応は妥当
- **iOS DSL Scenario 2 の THEN「表示は header の高さと Cell の内容の両方が新しくなる」は表示結果を観測するテストで担保された** (`SectionAccessoryRenderingTests.swift:617` が header の実 frame 高さと Cell の label テキストの両方を観測)。前回の「diff 列の形だけ」状態は解消
- **preflight 順序の入れ替えは iOS DSL Scenario 1 / 3 の担保を壊していない**。5 通りの入力組み合わせ (headerHeight のみ / +内容 / +可視性併発 / 可視性のみ / 内容のみ) がそれぞれ独立したテストで分岐先まで固定されている
- **Android 3 Scenario は `requiresFullRefresh` 抽出後も同じテストで担保されている** (テストファイルは前回から無変更、抽出は条件式・戻り値ともに不変で、`contentUpdates` 空の主張が 4 テストで維持されている)
- tasks.md に虚偽チェックなし、足場の逆流なし、未記録乖離なし
- テスト: iOS 415 件 / 0 failures、Android 2014 件 / 0 failures。Android の `:ks-settingsview-ui:testReleaseUnitTest` 間欠失敗は本変更非関与の既知環境事象 (§5 参照)

### 補足所見 (判定に影響しない)

- iOS Requirement 本文の SHALL (「同一 ID の Cell の内容も変わっている場合… `.replaceCell` を発行する」) に対し、実装は可視 Cell に限定している。`applyReplaceCell` が hidden Cell を自然な no-op として扱うため観測結果は同一で、Scenario 単位の乖離はない。蒸留時に concepts (`display-state-synchronization.md`) へ「非表示 Cell の内容更新は `.full` の model 反映に委ねる」を書き下すと、この限定が知識として残る
- 実装 diff には本変更の Scenario と直接対応しないコメント整理 (旧 openspec 変更 ID 参照の `core/ADR-00xx` 置換、Phase 番号の除去) が 5 ファイルすべてに含まれる。一致検証上の問題はなく、ksn-review が扱う範囲
