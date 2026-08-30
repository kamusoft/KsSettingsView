# Verify 001: fix-dsl-header-height-diff

- 検証日: 2026-08-05
- 検証対象: 作業ツリーの未コミット変更 (HEAD = `5f7d97e`)
- 対象ドメイン: android + ios
- デルタスペック: `specs/settings-view-android-ui/spec.md` (Requirement 1 / Scenario 3)、`specs/settings-view-ios-ui/spec.md` (Requirement 2 / Scenario 5)
- 判定: **VALID**

---

## 1. 対応表: settings-view-android-ui

### Requirement: Compose DSL の headerHeight 変更の表示反映

実装本体: `android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/DSLDiffCalculator.kt`

- `compute` の preflight: `:54-58` (`containsVisibilityChange || containsHeaderHeightChange` → `listOf(Full(SettingsRoot(sections = to.sections)))`)
- `contentUpdates` の同条件早期 return: `:263-268` (→ `emptyList()`)
- 判定関数: `containsHeaderHeightChange` `:337-347` (同一 Section ID の `headerHeight` を `Double` 等値比較。正値間・`-1.0 → 正値`・`正値 → -1.0` を区別せず検出)

| Scenario | 実装 | テスト (関数名) | 状態 |
|---|---|---|---|
| headerHeight のみの変更が表示へ反映される | `DSLDiffCalculator.kt:54-58`, `:337-347` | **diff 側**: `DSLDiffCalculatorTest.kt:269` `headerHeight が正値間で変化すると Full のみ発行され contentUpdates は空` / `:280` `headerHeight が自動から固定へ変化すると…` / `:291` `headerHeight が固定から自動へ変化すると…` (遷移 3 種を個別関数で網羅。いずれも `diffs.size == 1` + `Full.root...headerHeight` + `contentUpdates == emptyList()` を検証)<br>**表示側**: `FullUpdateContentSyncTest.kt:238` `Full diff による headerHeight 変更が表示と payload 付き通知に反映される` (同一 ViewHolder 再 bind で `layoutParams.height == dpToPx(96.0)` を観測。既存テスト・本変更外) | ✅ 一致 |
| headerHeight と Cell 内容の同時変更で両方が反映される | 同上 (`Full` のみ発行 + `contentUpdates` 空) | **diff 側**: `DSLDiffCalculatorTest.kt:302` `headerHeight と Cell 内容の同時変更でも Full のみ発行され contentUpdates は空` (`diffs.size == 1`・`Full` の root に新 headerHeight と新 title の両方が載る・`contentUpdates == emptyList()`)<br>**表示側**: `FullUpdateContentSyncTest.kt:512` `Full diff で同一 id の Cell 内容変更が表示へ反映される` + `:238` (既存テスト) | ✅ 一致 |
| headerHeight が不変なら preflight は発火しない | `containsHeaderHeightChange` が `false` を返し既存経路へ抜ける | `DSLDiffCalculatorTest.kt:321` `headerHeight 不変で内容だけ変わると Full を発行せず contentUpdates で列挙される` (`compute` が空リスト・`contentUpdates` に 1 件)、補助として `:337` `containsHeaderHeightChange は同一 headerHeight で false` | ✅ 一致 |

**「`contentUpdates` は空リストを返す SHALL」の充足**:
- 実装: `contentUpdates` の早期 return が `compute` と同一の複合条件 (`containsVisibilityChange || containsHeaderHeightChange`) で書かれており、判定のずれがない (`:263-268`)
- テスト: 遷移 3 種 + 同時変更の計 4 テストすべてが `assertEquals(emptyList<Cell>(), DSLDiffCalculator.contentUpdates(old, new))` を明示的に含む → 実装・テスト双方で充足

---

## 2. 対応表: settings-view-ios-ui

### Requirement: SwiftUI DSL の headerHeight 変更の表示反映

実装本体: `ios/Sources/KsSettingsViewSwiftUI/DSLDiffCalculator.swift`

- preflight: `:75-79` (`containsHeaderHeightChange` → `preflightDiffs = [.full(...)]` を先頭に置き、`contentUpdateDiffs` を `append(contentsOf:)` で**後続**に連結して return)
- 判定関数: `containsHeaderHeightChange` `:174-190`
- 内容更新列: `contentUpdateDiffs` `:195-216` (同一 UUID の Cell を `AnyHashable` 比較し `.replaceCell(cellID: KsCellID(cell: oldCell), new: cell)` を生成)

| Scenario | 実装 | テスト (関数名) | 状態 |
|---|---|---|---|
| headerHeight のみの変更が表示へ反映される | `DSLDiffCalculator.swift:75-79`, `:174-190` | **diff 側**: `DSLDiffCalculatorTests.swift:268` `test_headerHeightが正値間で変わるとfullが発行される` / `:281` `test_headerHeightが自動から固定へ変わるとfullが発行される` / `:294` `test_headerHeightが固定から自動へ変わるとfullが発行される` (遷移 3 種を個別関数で網羅。`diffs.count == 1` + `.full` の root の当該 Section の `headerHeight` 値を検証)<br>**表示側**: `SectionAccessoryRenderingTests.swift:553` `test_fullDiffのheaderHeight変更が表示中headerの実高さに反映される` | ✅ 一致 |
| headerHeight と Cell 内容の同時変更で両方が反映される | `DSLDiffCalculator.swift:75-79` (`.full` を配列先頭に置いた上で `contentUpdateDiffs` を後続に連結) | `DSLDiffCalculatorTests.swift:307` `test_headerHeightとCell内容の同時変更でfullに続けてreplaceCellが発行される` | ✅ 一致 (**発行順序まで検証済み** — 後述) |
| headerHeight が不変なら preflight は発火しない | preflight を通過し通常の `cellLevelDiffs` 経路へ | `DSLDiffCalculatorTests.swift:335` `test_headerHeight不変で内容のみ変わるとfullは発行されずreplaceCellが発行される` (`.full` が 1 件も含まれないことを明示的に否定検証 + `diffs.count == 1` + `.replaceCell` の cellID / 新 title) | ✅ 一致 |

**「`.full` → `.replaceCell` の順」の充足**:
- 実装: 配列リテラルで `.full` を先頭に確定させた後 `append(contentsOf:)` するため、順序が構造上保証される
- テスト: `:307` は `XCTAssertEqual(diffs.count, 2)` の上で **`diffs[0]` が `.full` (かつ headerHeight が新値)**、**`diffs[1]` が `.replaceCell` (かつ cellID・新 title 一致)** を添字指定で検証している → 「両方が含まれる」ではなく**順序を固定した検証**になっている。SHALL 充足

### Requirement: Store 経由の headerHeight 変更の表示反映

実装本体: 本変更で Store / Controller 側のコード変更はなし (既存挙動の確認が Requirement の趣旨)。観測点は `ios/Tests/KsSettingsViewUITests/SectionAccessoryRenderingTests.swift`。

| Scenario | 実装 | テスト (関数名) | 状態 |
|---|---|---|---|
| replaceSection による headerHeight 変更が表示へ反映される | 既存経路 (`SettingsRootStore.replaceSection` `SettingsRootStore.swift:117-125` が `.replaceSection(sectionID:new:)` を送出 → `KsSettingsViewController.applyDiff`) | `SectionAccessoryRenderingTests.swift:517` `test_replaceSectionのheaderHeight変更が表示中headerの実高さに反映される` | ✅ 一致 |
| .full による headerHeight 変更が表示へ反映される | 既存経路 (`applyDiff(.full(...))`) | `SectionAccessoryRenderingTests.swift:553` `test_fullDiffのheaderHeight変更が表示中headerの実高さに反映される` | ✅ 一致 |

**観測点が「表示中 supplementary view の実高さ」であることの確認** (spec が要求する観測対象):
- 両テストとも `hostControllerInWindow` で controller を実 `UIWindow` に載せ、`pump()` でレイアウトを確定させている (`:379-403`)
- ヘルパ `visibleHeaderFrameHeight` (`:494-502`) は `cv.supplementaryView(forElementKind:at:)` を使う。これは **collection view が現に保持している表示中の view** を返す API であり、provider 経由の新規生成でも payload でも `controller.root` でもない。その `view.frame.height` を観測している
- さらに `layoutHeaderHeight` (`:505-513`) で layout attributes 側も併せて検証しており、二重に押さえている
- 事前条件として更新前の実高さが `40` であることを `XCTUnwrap` + `XCTAssertEqual(accuracy: 0.5)` で確認しており、`nil` 素通り (supplementary が取得できないまま素通過する) の穴がない

→ spec の「表示中 header の高さ」の要求を満たす観測になっている。

**注記 (❌ ではない)**: 2 テストの入口は `SettingsRootStore` の public API ではなく `controller.applyDiff(...)` である。ただし `SettingsRootStore.replaceSection` は `:124` で `.replaceSection(sectionID:new:)` をそのまま送出するだけなので、テスト対象の diff 値は Store が実際に流すものと同一。Store の public API から実機で通した経路は証跡 `11-ios-after-fix-store-replacesection.png` が押さえている。

---

## 3. 遷移 3 種の両 platform カバレッジ

| 遷移 | Android | iOS |
|---|---|---|
| 正値A → 正値B | `DSLDiffCalculatorTest.kt:269` (40.0 → 80.0) | `DSLDiffCalculatorTests.swift:268` (40 → 80) |
| -1 → 正値 (自動 → 固定) | `DSLDiffCalculatorTest.kt:280` (-1.0 → 80.0) | `DSLDiffCalculatorTests.swift:281` (-1 → 64) |
| 正値 → -1 (固定 → 自動) | `DSLDiffCalculatorTest.kt:291` (80.0 → -1.0) | `DSLDiffCalculatorTests.swift:294` (64 → -1) |

→ **両 platform で 3 遷移が独立したテスト関数として個別にカバーされている** (パラメタライズによる合流や 1 関数への詰め込みはない)。ADR-0018 が掲げる「対称テスト義務の初適用」は形式として成立している。

---

## 4. tasks.md の実体確認

| タスク | 実体 | 状態 |
|---|---|---|
| 1.1 Android preflight 検出 | `DSLDiffCalculator.kt:54-58` / `:263-268` / `:337-347` | ✅ |
| 1.2 Android 対称テスト (遷移 3 種) | `DSLDiffCalculatorTest.kt:269` / `:280` / `:291` | ✅ |
| 1.3 Android 同時変更テスト | `DSLDiffCalculatorTest.kt:302` | ✅ |
| 1.4 Android 退行防止テスト | `DSLDiffCalculatorTest.kt:321` (+ `:337`) | ✅ |
| 2.1 iOS preflight 検出 + `.full` 後続 `.replaceCell` | `DSLDiffCalculator.swift:75-79` / `:174-190` / `:195-216` | ✅ |
| 2.2 iOS 対称テスト (遷移 3 種) | `DSLDiffCalculatorTests.swift:268` / `:281` / `:294` | ✅ |
| 2.3 iOS 順序検証テスト | `DSLDiffCalculatorTests.swift:307` (添字指定で `.full` → `.replaceCell`) | ✅ |
| 2.4 iOS 退行防止テスト | `DSLDiffCalculatorTests.swift:335` | ✅ |
| 3.1 Store 経路の実高さ観測 XCTest | `SectionAccessoryRenderingTests.swift:517` / `:553` (表示中 supplementary の `frame.height` + layout attributes) | ✅ |
| 4.1 Android A/B 証跡 | `verification/01`〜`05` + README.md (実機 Pixel 6a / Android 16) | ✅ |
| 4.2 iOS A/B 証跡 | `verification/06`〜`11` + README.md (iPhone 17 Pro / iOS 26.1) | ✅ |

**虚偽チェックなし。**

### 証跡 (4.1 / 4.2) の実在と A/B 対比の確認 (画像を実際に開いて確認)

- `02-before-fix-after-tap-unchanged.png`: 「H高さ」タップ後もヘッダ「静的 Section」が自動高さのまま (症状の再現)
- `03-after-fix-after-tap-expanded.png`: 同一操作でヘッダ領域が明確に拡大 (解消) — **02 と 03 の対比が成立**
- `05-after-fix-height-and-content.png`: 拡大ヘッダ + `固定 Cell A（内容更新）` の両方 (Scenario 2 の表示結果)
- `07-ios-before-fix-after-tap-unchanged.png` / `08-ios-after-fix-after-tap-expanded.png`: iOS 側も同一の A/B 対比が成立
- `10-ios-after-fix-height-and-content.png`: 拡大ヘッダ + `固定 Cell A（内容更新）`
- `11-ios-after-fix-store-replacesection.png`: Store 方式デモで `PoC Section` ヘッダが拡大 (Store 経路の目視)
- README.md に「修正なし」ビルドの作り方 (`containsHeaderHeightChange` 冒頭で `false` を返す一時改変) と、タップ取りこぼしでないことの根拠が明記されている

→ **証跡は実在し、before/after の対比になっている。** 検証用の一時ボタン・一時状態は取得後に削除済み (`samples/` に作業ツリー変更なしで裏取り済み)。

---

## 5. 追加検査

- **逆流検査**: 足場アーティファクト (`proposal.md` / `exploration.md` / `specs/**`) は `5f7d97e` でコミットされて以降、作業ツリーで一切変更されていない (`git diff --stat` が空)。逆流なし
- **未記録乖離**: `deviation.md` は存在しない。対応表に ❌ が 1 件もないため、記録すべき未記録乖離もなし
- **UI 変更の brief/mock**: 本変更に `ui/` アーティファクトはない (視覚パラメータの変更ではなく既存の宣言値を表示へ届ける修正のため)。ゲート不適用
- **テスト実行** (`concepts/cross/conventions/test-execution.md` の規約に従い実行):
  - iOS: `cd ios && xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,id=<ios-simulator-udid>…'` → **`Executed 414 tests, with 0 failures`** / `** TEST SUCCEEDED **`。新規 7 テスト (SwiftUI DSL 5 + UI 2) がログ上で `passed` として実行済みを個別に確認
  - Android: `cd android && ./gradlew test --rerun-tasks` → 集計 **2014 件 (debug/release 両 variant) / failures 0**。新規 6 テストが `TEST-…DSLDiffCalculatorTest.xml` に testcase として実在し全て pass
  - 補足: 初回の `--rerun-tasks` 全件実行で `:ks-settingsview-ui:testReleaseUnitTest` が全クラス `ClassNotFoundException (initializationError)` で落ちたが、同タスクを単独で `--rerun-tasks` 再実行すると `BUILD SUCCESSFUL`。並列ビルドと Kotlin daemon の競合による一過性の事象であり (debug variant は同時に全件 green、本変更は `ks-settingsview-ui` の main ソースに触れていない)、テスト内容の失敗ではない

---

## 6. 総合判定

**VALID**

- Android 3 Scenario / iOS 5 Scenario の計 8 Scenario すべてが「✅ 一致」。❌ は 0 件
- Android の `contentUpdates` 空リスト SHALL、iOS の `.full` → `.replaceCell` **順序** SHALL は、いずれも実装の構造とテストの添字指定アサーションの双方で担保されている
- 遷移 3 種は両 platform で個別関数としてカバー済み
- iOS Store Requirement は payload / `controller.root` ではなく**表示中 supplementary view の `frame.height`** を観測するテストで担保されている
- tasks.md に虚偽チェックなし、足場の逆流なし、未記録乖離なし、テスト全件 green

### 補足所見 (判定に影響しない)

実装 diff には、本変更の Scenario と直接対応しないコメント整理 (`openspec/changes/…` 参照の除去と `core/ADR-00xx` への差し替え、Phase 番号の除去) が `DSLDiffCalculator.kt` / `.swift` / `DSLDiffCalculatorTests.swift` / `SectionAccessoryRenderingTests.swift` に含まれる。一致検証上の問題はないが、変更の粒度としてはレビュー (ksn-review) の観点で扱うべき範囲。
