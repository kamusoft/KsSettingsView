# レビュー結果: fix-ios-test-pump-condition-wait (002 回目)

**日付**: 2026-09-01
**判定**: CHANGES_REQUESTED
**兼務**: verify (デルタスペック一致検証) — 判定 **INVALID** (❌ 1 件。詳細は「一致検証 (verify 兼務)」節)

## サマリー

前回 (review-001 + second-opinion-code-001) の 10 件 (ホスト側 Major 1 / Minor 3 / Suggestion 3、相方 3 件) はすべて実物で解消を確認した。特に Major の対応は、狭い述語を 1 行差し替えるのではなく **共有ターゲットに `awaitCollectionRender` を新設して 2 ターゲットの重複定義ごと集約する**形を取っており、Suggestion「初期反映ヘルパの重複」も同時に閉じている。述語は「全 Section の期待行数 + 可視領域にかかる Cell + 可視領域にかかる section header / footer supplementary」まで広がり、review-001 が表で挙げた読み取り対象をすべて覆っている。全件 Simulator 実行をレビュアー側でも再現し、997 件 / 0 failures / `** TEST SUCCEEDED **` を確認した。

一方で、広げた述語には **Root accessory (`ks-root-header` / `ks-root-footer` の boundary supplementary) が入っていない**。加えて `awaitCollectionRender` は可視 Section が 0 件の構成で **待機を一切せずレイアウト実行だけで戻る** (`CollectionRenderWait.swift:36-40`)。この 2 つが重なる `SectionBoxDecorationTests.hostWithRootAccessories(root: SettingsRoot(sections: []), rootHeader:)` の呼び出しでは、置換前の 0.05 秒の固定待機より**待ちが短くなっている** (RunLoop を 1 度も回さない) にもかかわらず、直後に Root accessory の実体を読む。デルタスペックの「setup ヘルパが内包する待機も同様に、初期反映の完了述語を待つ形にする SHALL」に対する未充足であり、Major かつ verify ❌ とした。

なお実装ワーカーが自己申告した「手元では述語を常に真にしても全件通る」は**正しい**が、**待機を広げた修正には意味がある**と判断する (根拠は「検出力の主張の評価」節)。

## 照合した規約

| 文書 | 適用のきっかけ |
|---|---|
| `kasane/handbook/cross/comment-policy.md` | always (コメント構文を持つ全ソース)。lint 対象外の未追跡新規ファイル 8 本を含め、規約本文の禁止類型で人手照合した |
| `kasane/handbook/cross/test-execution.md` | テストの実行と結果の報告 / 本 change が追記する文書そのもの |
| `kasane/handbook/cross/runtime-behavior-verification.md` | 実行時挙動 (フレーム間タイミング) が絡む不具合の完了判定 |
| `kasane/handbook/cross/public-identifiers.md` | `ios/Package.swift` を触る |
| `kasane/lessons/code-review.md` | 重点観点 L-001 (検出力の争点はミューテーションで実測) |

照合結果 (節ごと):

- **comment-policy** — 許容する外部参照は `cross/ADR-0027` のみで形式適合 (`ios/Tests/KsSettingsViewTestSupport/NegativeVerificationWait.swift:14,23`)。禁止する参照 (change-id 裸参照 / Phase・Round・Decision 通番 / タスク通番 / アーカイブ文書のパス / 拡張子なし裸参照) と、**lint が検出しない履歴記述**を新規 8 ファイルと差分行の全コメントで確認して 0 件。今回書き足された `CollectionRenderWait.swift` の doc、`ConditionWait.swift:40-41` の「ここで再度レイアウトを走らせると〜」、`LayoutRunTests.swift:36-37` の閾値の意図はいずれも現在形で、「旧 pump では」「〜へ移行した」の類は無い。デルタスペック構文キーワード (SHALL / MUST 等) の混入 0 件。**適合**
- **test-execution** — 完了判定に絞り込みなしの全件 Simulator 実行を使っている。追記された「例外は負の検証だけ」節と適用実例の表は、実装 (`waitForNegativeVerification` の命名と doc、共有ターゲット 1 本への集約) と一致する。追記中の ADR 参照はリンク形式で規約側の書式に沿う。**適合**
- **runtime-behavior-verification** — ① 修正前の実環境再現 (`evidence/ci-flaky-before-fix.md`)、② 修正後の同一手順での解消確認 (`evidence/repeat-run-after-fix.md` 10/10 + `evidence/full-suite-after-fix.md`)、③ 証跡は change 配下。今回追加の `evidence/initial-render-predicate-detection.md` は述語の検出力の実測であり、限界 (手元では差が出ない) を隠さず書いている。**適合**
- **public-identifiers** — 新設 target `KsSettingsViewTestSupport` / `KsSettingsViewTestSupportTests` は SwiftPM の PascalCase 規則に沿い、`products` に載せていないため配布座標に影響しない。**適合**
- **lessons/code-review L-001** — 争点 (「`hostWithRootAccessories` から戻った時点で Root header の factory は未実行ではないか」) をレビュアー側でスクラッチにコピーした複製ツリー上でミューテーション実測した。結果は「手元では実行済み」で、私の当初の見立て (アサーションが誤って通る) は**手元では成立しない**。指摘の根拠を「観測された誤通過」から「契約と置換前より短い待機」へ限定した。レビュー対象のツリーには一切書き込んでいない

## 前回指摘の解消状況

| # | 出典 / 重要度 | 指摘 | 状態 | 確認した実物 |
|---|---|---|---|---|
| 1 | review-001 🟠 Major | 初期反映の述語が、直後の assert が読む対象より狭い (5 setup 経路) | **解消** (残件あり → 新規 Major) | `ios/Tests/KsSettingsViewTestSupport/CollectionRenderWait.swift` を新設し、`ios/Tests/KsSettingsViewUITests/KsUITestWait.swift:17-30` と `ios/Tests/KsSettingsViewBridgeTests/KsBridgeTestHost.swift:54-63` の両方が委譲。述語は全 Section の期待行数 + 可視 Cell + 可視 section header/footer supplementary。`awaitInitialAccessoryRender` は grep 0 件で消滅し、5 経路すべてが同一の広い述語を通る |
| 2 | review-001 🟡 Minor | `awaitCondition` の `actual` に既定値があり実測値なしで fail できる | **解消** | `ios/Tests/KsSettingsViewTestSupport/ConditionWait.swift:30` が既定値なしの必須引数。`awaitNonNil` は `:104` で `"nil のまま"` を明示的に渡す |
| 3 | review-001 🟡 Minor | `CustomCellTests.host()` の A→B 分岐が deviation.md に未記録 | **解消** | `deviation.md`「置換の粒度」4 番目の項に、15 利用箇所が 8 (A) / 7 (B) に分かれた理由まで記録済み。根拠は `ios/Tests/KsSettingsViewUITests/CustomCellTests.swift:71-77` の doc にも残る |
| 4 | review-001 🟡 Minor | `LayoutRunTests` の閾値が置換前の固定待機 0.05 秒より緩い | **解消** | `ios/Tests/KsSettingsViewTestSupportTests/LayoutRunTests.swift:38` が `0.02` のリテラル。`:36-37` に閾値の取り方の意図コメント。無関係な定数への結合も解消 |
| 5 | review-001 🔵 Suggestion | 述語成立後の再レイアウトが確立した条件を崩しうる | **解消** | `ConditionWait.swift:38-42` がループ内で「レイアウト → 判定 → 成立なら即 return」の順になり、成立後の追加レイアウトが無い。`:40-41` に理由コメント |
| 6 | review-001 🔵 Suggestion | `KsTestWait.failureReporter` が public な可変グローバル | **解消** | `ios/Tests/KsSettingsViewTestSupport/KsTestWait.swift:36` は `internal static var` へ降格。差し替えは `:45-53` の `withFailureReporter(_:during:)` に閉じ、`defer` で必ず復元 |
| 7 | review-001 🔵 Suggestion | 初期反映ヘルパが 2 ターゲットに重複定義 | **解消** | 述語本体は共有ターゲットの `CollectionRenderWait.swift` 1 箇所。各ターゲットに残るのは `computeVisibleSections` から期待構造を作るだけの薄いラッパ |
| 8 | 相方 🟡 Minor | `InputCellsTests` のコメントが旧実装の待機方式を説明している | **解消** | `ios/Tests/KsSettingsViewUITests/InputCellsTests.swift:1070` が「`becomeFirstResponder()` は MainActor 上の Task 経由で走るため、成立そのものを待つ」に更新 |
| 9 | 相方 🟡 Minor | 新規テストで GCD による遅延実行を使用している | **解消** | `ios/Tests/KsSettingsViewTestSupportTests/ConditionWaitTests.swift:49-55` が `Task { @MainActor in ... Task.sleep(for:) }`。`DispatchQueue` は全新規ファイルで 0 件 |
| 10 | 相方 🔵 Suggestion | 診断文字列の数値整形が C 形式 | **解消** | `KsTestWait.swift:58-62` が `.formatted(.number.precision(.fractionLength(3)).locale(...))`。`String(format:` は全新規ファイルで 0 件 |

(review-001 の verify ❌ 1 件は #3 と同一事象で、`deviation.md` への記録により解消。)

## 指摘事項

### [🟠 Major] 初期反映の述語に Root accessory が入っておらず、可視 Section 0 件の構成では待機が消える

**該当箇所**:
- `ios/Tests/KsSettingsViewTestSupport/CollectionRenderWait.swift:36-40` (可視 Section 0 件の早期 return)、`:69-71` (待つ supplementary の kind が section header / footer のみ)
- `ios/Tests/KsSettingsViewUITests/SectionBoxDecorationTests.swift:73-92` (`hostWithRootAccessories`)、呼び出し `:478`, `:524`, `:596` (可視 Section 0 件)

**問題点**:

`awaitCollectionRender` の述語が待つ supplementary は `UICollectionView.elementKindSectionHeader` / `elementKindSectionFooter` の 2 kind に限られる (`CollectionRenderWait.swift:69`)。Root accessory は `KsSettingsViewController.rootHeaderElementKind` (`ks-root-header`) / `rootFooterElementKind` の **layout 全体の boundary supplementary** であり、この 2 kind には含まれないため待機の対象外になっている。

さらに `expectedItemCounts` が空 (可視 Section 0 件) の場合、`:36-40` の guard が `layoutNow` だけを実行して戻る。コメントは「表示すべき Section が無い構成には待つべき遷移が無いため」と理由付けているが、**Root accessory を持つ構成ではこれは成り立たない** — `visibleSupplementaryViews(ofKind: rootHeaderElementKind)` は空から非空へ遷移する、待てる完了条件である。

具体的に、次の 3 呼び出しが可視 Section 0 件で `awaitInitialRender` を通り、RunLoop を 1 度も回さずに戻る:

| 呼び出し | setup 直後に読む対象 | 述語が見ているか |
|---|---|---|
| `SectionBoxDecorationTests.swift:478` (`host`) | `cv.contentInset` | (レイアウトで確定。実害なし) |
| `SectionBoxDecorationTests.swift:524` (`hostWithRootAccessories`) | `:529` `rootAccessoryContentFrame(cv, kind: rootHeaderElementKind)` — nil なら `XCTFail("Root Header の内容が取得できない")` | 見ていない |
| `SectionBoxDecorationTests.swift:596` (`hostWithRootAccessories`) | `:609` `let baseline = counter.count` (Root header の `KsAnyView` factory 実行回数) | 見ていない |

置換前は同じ経路に `pump(cv, seconds: 0.05)` (レイアウト → RunLoop 0.05 秒 → レイアウト) が入っていた。**この 3 箇所は置換によって待機が短くなっている** — 本 change が閉じにいった方向と逆である。`:596` は、baseline を初期 factory 実行前に取ると後続の `counter.count > baseline` が「作り直し」ではなく「初期生成」で成立してしまい、アサーションが誤って通る形になる。

可視 Section が 1 件以上ある `hostWithRootAccessories` の呼び出し (`:405`, `:410`, `:447`, `:452`, `:560`, `:643`, `:659`, `:664`, `:685`, `:757`) も、Cell と section supplementary は待つが Root accessory は待たずに `rootAccessoryContentFrame` を読む。こちらは Cell の待機が実質的な猶予になるため優先度は下がる。

なお**手元での実測では誤通過は起きない** — レビュアー側でスクラッチへ複製したツリーに `XCTAssertGreaterThan(baseline, 0)` を差し込んで `SectionBoxDecorationTests` を実行したところ 54 件 / 0 failures で、`hostWithRootAccessories` から戻った時点で factory は実行済みだった。ただし handbook `cross/test-execution.md` が「手元で通ることは、この形で書けている根拠にならない」と定めるとおり、これは指摘を取り下げる根拠にはならない。

**推奨修正** (いずれか):
- `awaitCollectionRender` に Root accessory の期待有無を渡せるようにし、`isCollectionRendered` の supplementary 判定へ `rootHeaderElementKind` / `rootFooterElementKind` を含める。あわせて `:36-40` の早期 return を「期待する Root accessory があるならそれを待つ」形に変える (Section 0 件でも待つべき遷移が残る)
- 共有ターゲットは `KsSettingsViewUI` に依存していないため kind 定数を直接参照できない。kind 文字列を引数で受ける形にするか、`SectionBoxDecorationTests` 側の `hostWithRootAccessories` に `awaitCollectionRender` の後段として Root accessory の述語を足す。後者なら `ios/Tests/KsSettingsViewUITests/RootAccessoryThemeRefreshTests.swift:59-67` の `hasRootSupplementary` を使った待機がそのまま雛形になる
- 過剰と判断する場合は、実測 (Root の boundary supplementary が Cell と同一レイアウト周回で必ず生成されること) を根拠に添えて `deviation.md` へ記録し、可視 Section 0 件の構成で待機が消えることを合意済みにする

### [🔵 Suggestion] `awaitCollectionRender` の失敗が setup 段で起きたとき、どのテストの setup かが読み取りにくい

**該当箇所**: `ios/Tests/KsSettingsViewTestSupport/CollectionRenderWait.swift:33-34`, `ios/Tests/KsSettingsViewUITests/KsUITestWait.swift:19-20`, `ios/Tests/KsSettingsViewBridgeTests/KsBridgeTestHost.swift:54`

**問題点**: `KsUITestWait.awaitInitialRender` は `file` / `line` を引き回しているが、`KsBridgeTestHost.awaitInitialRender` は引数を持たず、失敗は `KsBridgeTestHost.swift:58` に固定で出る。Bridge 側は `attach` が 67 箇所から呼ばれるため、deadline 超過時にどのテストの setup が落ちたかがメッセージからは分からない (テスト名は XCTest の出力から追えるので実害は小さい)。

**推奨修正**: `KsBridgeTestHost.attach` / `awaitInitialRender` にも `file: StaticString = #filePath, line: UInt = #line` を通す。

### [🔵 Suggestion] `hostWithRootAccessories` は Root accessory を待たない点が呼び出し側から見えない

**該当箇所**: `ios/Tests/KsSettingsViewUITests/SectionBoxDecorationTests.swift:71-73`

**問題点**: doc コメントは「Root Header / Footer を載せた controller を window に置いて実レイアウトを走らせる」とだけ書かれており、`awaitInitialRender` が何を待つ (何を待たない) かは呼び出し側から読み取れない。上の Major を「待たない」で閉じる選択をした場合、この doc に「Root accessory の実体化は待たない」ことを書かないと、後から追加されるテストが同じ穴に落ちる。

**推奨修正**: Major の対応方針が決まったら、その結論を doc コメントに 1 行残す。

## 検出力の主張の評価 (実装ワーカーの自己申告について)

`evidence/initial-render-predicate-detection.md` の 2 つの実験と、その解釈を評価した。

**実験 1 (述語を常に真にしても全件 0 failures) の主張は正しい。** レビュアー側でも同じ結論に達する根拠が 2 つある。① `SectionBoxDecorationTests` は可視 Section 0 件 + Root accessory の構成で**すでに待機なし** (`layoutNow` のみ) で通っている (上の Major 参照)。つまり手元では window 掲載 + `layoutIfNeeded()` の時点で supplementary の生成まで同期的に済む環境である。② 私が行った `baseline > 0` のミューテーション実測も同じことを示した。よって「手元では待機の有無が pass / fail に出ない」は実証済みと見なせる。

**それでも待機を広げた修正には意味がある**と判断する。理由:

1. **証明すべき命題が違う。** 本 change が閉じにいったのは「手元で落ちるテスト」ではなく「CI (macos-26、混雑時) で間欠的に落ちるテスト」であり、その存在は `evidence/ci-flaky-before-fix.md` の同一 commit・attempt 1 失敗 / attempt 2 成功で実測済みである。手元の pass / fail で差が出ないことは、修正が無意味である証拠ではなく、**手元の実行がこの種の欠陥に対する検査になっていない**ことの証拠にすぎない。これは handbook `cross/test-execution.md` が明文化している立場そのもの
2. **広げた述語は単調に強い。** `isCollectionRendered` は旧述語 (Section 数 + 先頭 Section の item 0) の論理積を含む真部分集合ではなく強化であり、成立した瞬間に抜ける構造は変わらない。通常時の実行時間は増えず (実測: 全件 997 件が Bridge 7.0 秒 / UITests 7.9 秒)、遅延時にだけ余分に待つ。つまり**副作用なしに保証が増える**変更で、価値を pass / fail の差分で測る必要がない
3. **実験 2 が「黙って戻らない」ことを担保している。** 期待 Section 構造を 1 つ増やすと 7/7 が deadline 超過で落ち、失敗メッセージが Section ごとの行の実体化数と header / footer の領域有無・view 有無まで出す。述語がトートロジーでないこと、および**失敗時に「何を待っていて実際はどうだったか」が読める**ことは、これで実証されている。デルタスペックの Scenario「deadline 超過は実測値付きで fail する」の要求はこの水準を求めており、実験 2 は十分な証明

**ただし、この評価は「手元で差が出ない」ことの帰結として 1 つ注意を残す。** 述語を広げる方向の修正は手元の緑では検証できないので、**逆に述語を狭めた / 待機を消した箇所も手元の緑では検出できない**。上の Major (可視 Section 0 件で待機が消える) が全件緑のまま残っているのは、まさにこの非対称性による。この change の完了判定は「全件緑」ではなく「述語が読み取り対象を覆っているかの読解」に依存しており、今回の残件はその読解の穴である。

---

## 一致検証 (verify 兼務)

**判定**: INVALID (❌ 1 件)

### Requirement / Scenario 対応表

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| **条件ベース待機** (deadline 契約: 単調増加時計) | `ios/Tests/KsSettingsViewTestSupport/KsTestWait.swift:68-76` (`KsTestMonotonicClock` / `DispatchTime.uptimeNanoseconds`) | 下の 3 Scenario で検証 | ✅ 壁時計非依存 |
| 同 (共通既定値 1 箇所・呼び出しごと上書き可) | `KsTestWait.swift:20` (3.0 秒)、`ConditionWait.swift:29` | `ConditionWaitTests.swift:65`, `:101`, `:117` | ✅ 置換前の最長明示待機 1.0 秒を上回る |
| 同 (置換前の `seconds:` 明示値を引き継がない) | — | — | ✅ `ios/Tests/` の呼び出しに `deadline:` 上書きは 0 件 (ヘルパ自身のテストのみ) |
| Scenario: 条件成立で待機が終わる | `ConditionWait.swift:38-42` | `ConditionWaitTests.swift:61-71` (deadline 5.0 に対し elapsed < 0.5) | ✅ |
| Scenario: 遅延して成立する述語でも deadline 内なら成功する | `ConditionWait.swift:43-46` (ループ内で `pollInterval` 分 RunLoop を回す) | `ConditionWaitTests.swift:75-93` (0.3 秒後に成立。`elapsed ≥ 0.3` と失敗 0 件を同時に要求) / `:127-146` (`awaitNonNil`) | ✅ 早期 return も無限ループも落ちる形 |
| Scenario: deadline 超過は実測値付きで fail する | `ConditionWait.swift:48-57`。`actual` は既定値なしの必須引数 (`:30`) | `ConditionWaitTests.swift:96-112` (description / 実測 / deadline / 経過 の 4 要素)、`:115-124` (`awaitEqual` の期待値 + 実測) | ✅ 前回 Minor 解消 |
| **待機なしのレイアウト実行** / Scenario: レイアウト確定だけが必要なテスト | `ios/Tests/KsSettingsViewTestSupport/LayoutRun.swift:15-18` | `ios/Tests/KsSettingsViewTestSupportTests/LayoutRunTests.swift:26-39` (レイアウト 1 回実行 + elapsed < 0.02 秒) | ✅ 前回 Minor 解消 |
| **負の検証のための意図明示の固定待機** (名前で判別できる) | `ios/Tests/KsSettingsViewTestSupport/NegativeVerificationWait.swift:25-38` (`waitForNegativeVerification`、doc に `cross/ADR-0027`) | `ConditionWaitTests.swift:149-156` | ✅ |
| Scenario: no-op の確認 | 例: `ios/Tests/KsSettingsViewBridgeTests/KsBridgeUpdateTests.swift:154`、`ios/Tests/KsSettingsViewUITests/ContentUpdateBatchTests.swift:105` | 同左 (呼び出し箇所がテスト) | ✅ |
| Scenario: 不達の確認 | `ios/Tests/KsSettingsViewBridgeTests/KsBridgeHostReleaseTests.swift:81`、`ios/Tests/KsSettingsViewUITests/StoreDisconnectionTests.swift:65` | 同左 | ✅ |
| 同 SHALL NOT (収束待ちに使わない) | 全 29 呼び出しを個別確認 | — | ✅ 全箇所とも直後の assert が不変性の確認 |
| **収束待ちの全数条件ベース化** (旧パターンが残っていない) | — | — | ✅ `ios/Tests/` の `RunLoop.current.run` は `ConditionWait.swift:44` / `NegativeVerificationWait.swift:33` の 2 定義のみ。`func pump` / `pumpEntry` は 0 件 |
| Scenario: 収束待ち箇所が条件ベースで書かれている | `awaitCollectionRender` (`CollectionRenderWait.swift`) へ集約。ただし可視 Section 0 件の setup は無待機 | 各テスト | ❌ `SectionBoxDecorationTests.swift:478`, `:524`, `:596` の setup が「初期反映の完了述語を待つ」を満たさない |
| Scenario: flaky が観測されたテストの安定化 | `ios/Tests/KsSettingsViewBridgeTests/KsBridgeCustomCellTests.swift:369-393` (回収完了を述語で待つ) | `evidence/repeat-run-after-fix.md` (10/10)、`evidence/full-suite-after-fix.md` (997 / 0 failures) | ✅ |

### 分類別の突き合わせ (triage.md 対比)

| 分類 | 台帳 | 実装後の観測 | 差分の説明 |
|---|---:|---:|---|
| A (収束待ち) | 160 | 条件ベース待機 (`awaitCondition` / `awaitEqual` / `awaitNonNil` / `awaitCollectionRender` と各ファイルの薄い派生) へ置換済み | `deviation.md`「置換の粒度」のとおり呼び出し数は call site 数と 1:1 にならない (統合 2 件・分割 3 件・`CustomCellTests.host()` 分岐 1 件)。B→A 3 件を取り込み、A→C 1 件を送出 |
| B (レイアウト実行のみ) | 16 | `layoutNow` 呼び出し 16 (ヘルパ定義・自テスト・可視 Section 0 件分岐を除く) | 内訳は review-001 と同じ。今回の修正で増減なし |
| C (負の検証) | 30 | `waitForNegativeVerification` 呼び出し 29 | −1 (`KsBridgeUpdateTests.swift:319` 撤去)、+1 (`SectionAccessoryRenderingTests.swift:559` A→C)、−1 (Root header テスト書き直しで消滅) → 29 で辻褄が合う。いずれも `deviation.md` 記載済み |
| 撤去する定義 | 20 | 0 件残存 | ✅ |

### 追加検査

- [x] **テスト全件成功**: レビュアー側で `xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,name=iPhone 17'` を絞り込みなしで実行。Bridge 166 / Core 88 / SwiftUI 94 / TestSupport 7 / UITests 642 = **997 件・0 failures**、`** TEST SUCCEEDED **`、warning 0 件。`evidence/full-suite-after-fix.md` の記録と一致
- [x] **lint**: `local-path-lint.py` / `identity-lint.py` / `comment-policy-lint.py` いずれも 0 件。`comment-policy-lint.py` が走査しない未追跡新規ファイル 8 本は規約本文で人手照合 (上記「照合した規約」)
- [x] **tasks.md**: 全 16 タスクが `[x]`。虚偽チェックなし。1.1 の受け入れ条件「共有 (単一定義)」は `ios/Package.swift` の `KsSettingsViewTestSupport` 1 target + 3 テストターゲットの依存で成立
- [x] **逆流検査**: `proposal.md` / `specs/ios-test-support/spec.md` / `triage.md` はいずれも未変更。作業ツリーで変更されている足場は `tasks.md` (チェック) と `deviation.md` (追記) のみで、これは正常
- [x] **付随修正**: `deviation.md` 記載の 2 件 (Root accessory 追従テストの書き直し / `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:351-361` の doc コメント修正)。後者の diff を再確認し、実行コードは 1 行も変わっておらず、追記された 2 段落は `AccessoryViewLiveProbeTests.swift:267-347` が実測した挙動と一致する。ksn-core「付随修正」の同梱条件の内側。**スコープ外としての指摘はしない**
- [x] **Non-Goal の遵守**: Android 側 (`android/`) の変更 0 件。`ios/Sources/` の変更は上記 doc コメントのみ。CI workflow の変更 0 件
- [x] **UI 変更**: なし (`ui/` アーティファクト無し。妥当)
- [x] **指摘しなかった観点** (確認済み・実害なし): ① `KsTestMonotonicClock` が `ContinuousClock` ではなく `DispatchTime` を使う点 — ios の実装スキルが禁じるのは GCD のキュー操作であり時計読み出しではなく、spec の「単調増加時計」要求は満たす ② `withFailureReporter` の `body` が非 throwing で `try` を含むテストを包めない点 — 現行の失敗経路テストは throwing を要さない ③ `HostViewLoadRestoreTests.swift:271-274` の `XCTAssertNil(visibleRootHeaderText(cv))` が待機を伴わない負のアサーションである点 — triage の C 分類 30 箇所に含まれず、本 change が作った形でもない

### ❌ の一覧と見立て

| ❌ | 内容 | 見立て |
|---|---|---|
| 1 | `awaitCollectionRender` が可視 Section 0 件の構成で待機せず戻り、Root accessory を kind として持たないため、`SectionBoxDecorationTests` の 3 setup が「初期反映の完了述語を待つ」を満たさない (置換前の 0.05 秒固定待機より短い) | **実装で閉じるのが望ましい** (推奨修正の 1 つ目か 2 つ目)。Root accessory が Cell と同一レイアウト周回で必ず生成されるという実測を取れるなら、`deviation.md` へ記録して合意済みにする道もある |

## アクションプラン

1. **[Major / verify ❌]** `awaitCollectionRender` の述語に Root accessory を含め、可視 Section 0 件でも期待する Root accessory があれば待つ形にする (`CollectionRenderWait.swift:36-40`, `:69-71`)。共有ターゲットから kind 定数を参照できないため、kind を引数で受けるか `SectionBoxDecorationTests.hostWithRootAccessories` 側に後段の待機を足す。雛形は `RootAccessoryThemeRefreshTests.swift:59-67`
2. **[Suggestion]** `KsBridgeTestHost.attach` / `awaitInitialRender` への `file` / `line` の引き回し
3. **[Suggestion]** 1 の結論を `hostWithRootAccessories` の doc コメントへ 1 行残す
4. 1 の修正後は `ios` で全件 Simulator 実行を再度通し、実行件数を `evidence/full-suite-after-fix.md` に更新する (件数は変わらない見込み)
