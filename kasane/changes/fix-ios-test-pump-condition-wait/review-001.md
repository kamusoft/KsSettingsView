# レビュー結果: fix-ios-test-pump-condition-wait (001 回目)

**日付**: 2026-09-01
**判定**: CHANGES_REQUESTED
**兼務**: verify (デルタスペック一致検証) — 判定 **INVALID** (❌ 1 件。詳細は「一致検証 (verify 兼務)」節)

## サマリー

固定時間待機の全数置換は、用途 3 分離・共有ターゲットへの単一定義集約・旧定義 20 個の完全撤去まで到達しており、`ios/Tests/` 配下に残る `RunLoop.current.run(until:)` は待機ヘルパ 2 定義のみである。述語の質も全体に高く、「更新前から真の不変条件を述語にしない」という spec の規則は個々の置換でよく守られている (`KsBridgeCustomCellTests` のトークン変更で `attachCount` の増加を待つ、`KsBridgeThemeTests` の modern 切替で `minX > 0` を待つ、等)。

一方で、**Host を組み立てる setup ヘルパの初期反映述語が、直後の assert が読む対象より狭い**箇所が 5 つの setup 経路に残っている。行 (Cell) の実体化までしか待たないのに、呼び出し側は supplementary・2 番目以降の Section・item 0 以外の行を即座に参照する。同じ change の中に既に広い述語版 (`awaitInitialAccessoryRender`) があり、3 ファイルではそちらが使われているため、規律の問題ではなく適用漏れと読める。これは proposal が Impact に挙げた「述語の選び違いでテストの検証内容が弱まる」に当たるため Major とした。

## 照合した規約

| 文書 | 適用のきっかけ |
|---|---|
| `kasane/handbook/cross/comment-policy.md` | always (コメント構文を持つ全ソース) — 機械検査の対象外である新規 7 ファイルを含め、規約本文の禁止類型で人手照合した |
| `kasane/handbook/cross/test-execution.md` | テストの実行と結果の報告 |
| `kasane/handbook/cross/runtime-behavior-verification.md` | 実行時挙動 (フレーム間タイミング) が絡む不具合の完了判定 |
| `kasane/handbook/cross/public-identifiers.md` | `ios/Package.swift` を触る |
| `kasane/lessons/code-review.md` | 重点観点 L-001 (トートロジー疑いはミューテーションで実測) |

照合結果 (節ごと):

- **comment-policy** — 許容する外部参照: `cross/ADR-0027` のみで形式適合 (`ios/Tests/KsSettingsViewTestSupport/NegativeVerificationWait.swift:14,23`)。禁止する参照 (change-id 裸参照 / Phase・Decision 通番 / タスク通番 / アーカイブ文書のパス / 拡張子なし裸参照) は grep と目視でいずれも 0 件。禁止する記述類型のうち **lint が検出しない履歴記述**を人手で確認した — 新規 7 ファイルと変更差分のコメントはすべて現在形で、「旧 pump は〜」「〜へ移行した」の類は無い。`ios/Tests/KsSettingsViewUITests/AccessoryViewLiveProbeTests.swift:5-10` のファイルヘッダ書き換えも現在の仕様の説明になっている。デルタスペック構文キーワード (SHALL / MUST 等) の混入 0 件。**適合**
- **test-execution** — 完了判定に絞り込みなしの全件 Simulator 実行を使い、実行件数付きで記録している (`evidence/full-suite-after-fix.md`、997 件 / 0 failures)。`swift test` は使っていない。追記された「負の検証だけが例外」節は、実装 (`waitForNegativeVerification` の命名と doc) と一致している。**適合**
- **runtime-behavior-verification** — ① 修正前の実環境再現 = CI run の attempt 1/2 反転を記録 (`evidence/ci-flaky-before-fix.md`)、② 修正後の同一手順での解消確認 = 反復実行 10 回 (`evidence/repeat-run-after-fix.md`) + 全件実行、③ 証跡は change 配下。**適合**。なお flaky が現れた環境 (CI 混雑時) での再確認は commit 後にしか取れないため、この点は本 change の範囲で閉じない (指摘ではない)
- **public-identifiers** — 新設 target `KsSettingsViewTestSupport` は SwiftPM の PascalCase 規則に沿い、`products` に載せていないため配布座標に影響しない。**適合**
- **lessons/code-review L-001** — 争点になり得たのは「ヘルパ自身のテストが待機の失敗経路まで到達しているか」だが、`ConditionWaitTests.swift:73` の `XCTAssertGreaterThanOrEqual(elapsed, delay)` と `:72` の `messages.isEmpty` が同時に置かれているため、早期 return する待機でも成立を待たない待機でも必ず落ちる構造になっており、静的読解で決着した。オーケストレーターが同一ビルドツリーで実行中の可能性があるため、実装への一時ミューテーションは行っていない

## 指摘事項

### [🟠 Major] 初期反映の述語が、直後の assert が読む対象より狭い

**該当箇所**:
- `ios/Tests/KsSettingsViewUITests/AccessoryMeasureInvalidationTests.swift:56`
- `ios/Tests/KsSettingsViewUITests/AccessoryViewLiveProbeTests.swift:57`
- `ios/Tests/KsSettingsViewUITests/AccessoryViewDetachDiagnosticTests.swift:51`
- `ios/Tests/KsSettingsViewUITests/HostViewLoadRestoreTests.swift:37`
- `ios/Tests/KsSettingsViewBridgeTests/KsBridgeTestHost.swift:46` (述語の定義は `:55-78`)

**問題点**:

`awaitInitialRender` の述語は「visible projection の Section 数へ追いつく」+「**行を持つ先頭 Section の item 0** の Cell が実体化する」の 2 点しか見ない (`ios/Tests/KsSettingsViewUITests/KsUITestWait.swift:40-44`、`ios/Tests/KsSettingsViewBridgeTests/KsBridgeTestHost.swift:72-77`)。ところが上記 5 経路の呼び出し側は、setup 直後に**述語が観測していない対象**をそのまま参照する:

| 呼び出し側 | setup 直後に読む対象 | 述語が見ているか |
|---|---|---|
| `ios/Tests/KsSettingsViewUITests/AccessoryMeasureInvalidationTests.swift:89` | `XCTUnwrap(headerHeight(...))` = header supplementary の frame | 見ていない |
| `ios/Tests/KsSettingsViewUITests/AccessoryViewLiveProbeTests.swift:124`, `:147` | `XCTUnwrap(headerFrameHeight(...))` = 同上 | 見ていない |
| `ios/Tests/KsSettingsViewUITests/AccessoryViewDetachDiagnosticTests.swift:294` | `shared.window` (header supplementary へ載った view accessory) | 見ていない |
| `ios/Tests/KsSettingsViewUITests/HostViewLoadRestoreTests.swift:200` | `visibleHeaderLabel(cv, section: 0)?.text` | 見ていない |
| `ios/Tests/KsSettingsViewBridgeTests/KsBridgeAccessoryViewTests.swift:212-213`, `:248-250` | `headerAccessoryView` / `headerText` | 見ていない |
| `ios/Tests/KsSettingsViewBridgeTests/KsBridgeHostReleaseTests.swift:61-62` | 2 Section 分の `renderedTitles` と `headerText` | 先頭 Section の item 0 のみ |
| `ios/Tests/KsSettingsViewBridgeTests/KsBridgeHostTests.swift:37`、`ios/Tests/KsSettingsViewBridgeTests/KsBridgeOperationContractTests.swift:452`、`ios/Tests/KsSettingsViewBridgeTests/KsBridgeCellConversionTests.swift:431` | 全 Section・全 item の `renderedTitles` | 先頭 Section の item 0 のみ |

`KsBridgeTestHost.renderedTitles` は未実体化の Cell を `""` に落とす (`ios/Tests/KsSettingsViewBridgeTests/KsBridgeTestHost.swift:147-155`) ため、実体化が追いつかない回は空文字との比較で落ちる。supplementary 側は `XCTUnwrap` が nil で落ちる。いずれも「収束前の状態を検証して落ちる」= 本 change が閉じにいった flaky の型そのものである。

分類台帳の該当行は、待つ遷移として明示的に supplementary を挙げている (`triage.md` の A 分類: `AccessoryMeasureInvalidationTests.swift:55` 「初期 Section accessory supplementary が生成され、実 frame が取得できる」、`AccessoryViewLiveProbeTests.swift:55` 同、`HostViewLoadRestoreTests.swift:36` 「Cell / supplementary が実描画される」、`KsBridgeTestHost.swift:45` 「初期 Cell / supplementary が実描画される」)。台帳が指定した遷移証拠まで述語が届いていない。

置換前の 0.05 秒固定待機より弱くなったわけではない (置換は改善方向) が、**広い述語版が同じ change の中に既にあり (`ios/Tests/KsSettingsViewUITests/KsUITestWait.swift:53`、3 ファイルで使用)、切り替えが 1 行で済む**にもかかわらず、台帳が supplementary を挙げた経路で狭い版が使われている点を Major と判断した。deviation.md にこの選択の記録は無い。

**推奨修正**:
- UITests 側: `AccessoryMeasureInvalidationTests` / `AccessoryViewLiveProbeTests` / `AccessoryViewDetachDiagnosticTests` / `HostViewLoadRestoreTests` の setup を `awaitInitialAccessoryRender` へ切り替える
- Bridge 側: `KsBridgeTestHost.awaitInitialRender` の述語を、① 先頭 Section の accessory supplementary (領域があるとき)、② 全 Section の期待行数ぶんの Cell 実体化、まで広げる (`KsUITestWait.swift:80-88` の `isAccessoryRendered` と同型で書ける)
- 上記が過剰と判断する場合は、実測 (supplementary が Cell と同一レイアウト周回で必ず生成されること) を根拠に添えて deviation.md へ記録し、台帳との差分を合意済みにする

### [🟡 Minor] `awaitCondition` の `actual` に既定値があり、実測値なしで fail できてしまう

**該当箇所**: `ios/Tests/KsSettingsViewTestSupport/ConditionWait.swift:30`

**問題点**: デルタスペックは「deadline 超過時は黙って戻らず**その時点の実測値をメッセージに載せて**テストを fail させる」を SHALL としている (`specs/ios-test-support/spec.md`、Requirement: 条件ベース待機)。実装は `actual` に `{ "(観測値の指定なし)" }` という既定値を置いているため、引数を省略した呼び出しは「実測: (観測値の指定なし)」というメッセージで失敗する — Scenario「deadline 超過は実測値付きで fail する」を満たさない失敗メッセージが作れる。

現時点の呼び出しはすべて `actual:` を渡しており違反は 0 件 (`ios/Tests/` 配下で `actual:` を省略した `awaitCondition` はヘルパ自身のテスト `ConditionWaitTests.swift:47` のみ、これは成立済み条件の早期 return 検証で失敗経路を通らない)。ただし既定値が残る限り、後から書かれるテストが契約から静かに外れる。

**推奨修正**: `actual` を既定値なしの必須引数にする。`awaitNonNil` は現在どおり `{ "nil のまま" }` を明示的に渡せばよい。

### [🟡 Minor] `CustomCellTests.host()` の分岐で A 分類の一部が実質 B に落ちており、deviation.md に記録がない

**該当箇所**: `ios/Tests/KsSettingsViewUITests/CustomCellTests.swift:79-93` (定義)、`:451`, `:498`, `:515`, `:583`, `:618`, `:625`, `:643` (identifier を渡さない呼び出し)

**問題点**: `triage.md` は `CustomCellTests.swift:81` (= `host()` 内の `pump(cell)`) を **A 分類**とし、待つ遷移を「standalone SwiftUI-hosted content が view tree に現れる」としている。実装は `renderedIdentifier` の有無で分岐させ、渡された 6 箇所は条件ベース待機、渡さない 7 箇所は `layoutNow` (= B 相当) にした。doc コメントの理由付け (「`Text` / `Image` だけの content は UIView を一切生やさないため待てる遷移が無い」) は妥当だが、これは台帳の分類の変更であり、他の 3 件と違って deviation.md に記録が無い。

特に `:618` / `:625` / `:643` は SwiftUI `Text` の描画結果を **snapshot 画像で比較する** assert の直前であり、「待てる遷移が無い」とは言い切りにくい (待てる遷移は「描画結果が変わる」)。この 3 箇所については、`XCTAssertNotEqual(mutatedImage, plainImage)` の mutation probe が「両方とも空画像」を弾くため検出力自体は保たれているが、描画完了のタイミング依存は残る。

**推奨修正**: 分類変更を deviation.md に記録する (実装の変更は必須ではない)。snapshot 比較の 3 箇所については、`layoutNow` のままにするなら「なぜ描画完了を待つ必要がないか」をコメントに残す。

### [🟡 Minor] レイアウト実行ヘルパの「時間待機なし」判定が置換前の固定待機より緩い

**該当箇所**: `ios/Tests/KsSettingsViewTestSupportTests/LayoutRunTests.swift:36`

**問題点**: `XCTAssertLessThan(elapsed, KsTestWait.negativeVerificationDuration, "時間待機が発生している")` の閾値は 0.2 秒。置換前の固定待機は 0.05 秒だったため、**`layoutNow` の中に 0.05 秒の RunLoop 待機が戻ってきてもこのテストは通る**。tasks 1.5 が求めた「レイアウト実行が時間待機しない」の回帰検出力が、守りたい値のスケールに届いていない。加えて、負の検証用の待機時間という意味的に無関係な定数へ結合しており、その定数を将来短くするとテストの意味が変わる。

**推奨修正**: 閾値をリテラル (0.02 秒程度) にし、コメントで「置換前の固定待機 0.05 秒より小さい値を選ぶ」意図を書く。

### [🔵 Suggestion] 述語成立後の再レイアウトが、確立した条件を崩しうる

**該当箇所**: `ios/Tests/KsSettingsViewTestSupport/ConditionWait.swift:40-43`, `:50-53`

**問題点**: 述語が成立した後にもう一度 `settleLayout(view)` を実行してから戻る。「消える / 回収される」型の述語 — `ios/Tests/KsSettingsViewBridgeTests/KsBridgeAccessoryViewTests.swift:277-283` (画面外へ出た header の回収)、`ios/Tests/KsSettingsViewBridgeTests/KsBridgeCustomCellTests.swift:378-393` (リサイクル後に内容がどの表示中の行にも残っていないこと)、`ios/Tests/KsSettingsViewUITests/AccessoryViewDetachDiagnosticTests.swift:296-305` — では、この追加レイアウトが直後の assert が依存する状態を作り直す可能性がある。実測上は 10 回の反復で問題が出ていない (contentOffset が動いていないため対象が再表示されない) が、待機の契約としては「述語が真の状態で戻る」ことを保証していない。

**推奨修正**: 成立後の `settleLayout` を落とすか、実行後に述語を再判定してから戻る。

### [🔵 Suggestion] `KsTestWait.failureReporter` が public な可変グローバル

**該当箇所**: `ios/Tests/KsSettingsViewTestSupport/KsTestWait.swift:36`

**問題点**: 差し替え可能にした意図 (超過経路の検証) は doc に書かれており妥当だが、`public static var` のため任意のテストターゲットから恒久的に差し替えられる。復元を忘れた差し替えは、以後の全待機の失敗を無言で握り潰す — 本 change が消しにいった「黙って戻る待機」と同じ結果になる。

**推奨修正**: `withFailureReporter(_:during:)` のようなスコープ関数に閉じ、`var` 自体は `internal` へ落とす。

### [🔵 Suggestion] 初期反映ヘルパが 2 ターゲットに重複定義されている

**該当箇所**: `ios/Tests/KsSettingsViewUITests/KsUITestWait.swift:18-46` と `ios/Tests/KsSettingsViewBridgeTests/KsBridgeTestHost.swift:55-78`

**問題点**: 述語もコメントもほぼ同一の定義が 2 つある。共有ターゲットが `KsSettingsViewUI` に依存していないための分割だが、本 change が「private コピーの散在を解消する」ことを目的の 1 つに置いていた以上、同じ形が再び 2 コピーになるのは方向が逆に見える。`UICollectionView` と期待 Section 構造だけを引数に取る形なら共有ターゲット側に置ける。

**推奨修正**: 上の Major の対応で述語を広げるとき、あわせて共有ターゲットへ寄せられないか検討する (依存が要るなら現状維持でよい。その場合は判断を残す)。

---

## 一致検証 (verify 兼務)

**判定**: INVALID (❌ 1 件)

### Requirement / Scenario 対応表

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| **条件ベース待機** (deadline 契約: 単調時計) | `ios/Tests/KsSettingsViewTestSupport/KsTestWait.swift:49-57` (`DispatchTime` 基準) | — (契約は下の 3 Scenario で検証) | ✅ |
| 同 (共通既定値・呼び出しごと上書き可) | `ios/Tests/KsSettingsViewTestSupport/KsTestWait.swift:20` (3.0 秒)、`ConditionWait.swift:29` | `ConditionWaitTests.swift:47`, `:83` | ✅ 置換前の最長明示待機 1.0 秒を上回る |
| 同 (置換前の `seconds:` 明示値を引き継がない) | — | — | ✅ `ios/Tests/` の呼び出しに `deadline:` 上書き 0 件 (ヘルパ自身のテストのみ) |
| Scenario: 条件成立で待機が終わる | `ConditionWait.swift:38-43` | `ConditionWaitTests.swift:43-53` (deadline 5.0 に対し elapsed < 0.5) | ✅ |
| Scenario: 遅延して成立する述語でも deadline 内なら成功する | `ConditionWait.swift:44-47` (ループ内で RunLoop を `pollInterval` 分回す) | `ConditionWaitTests.swift:57-75` (0.3 秒後に成立、elapsed ≥ 0.3 と失敗 0 件を同時に要求) | ✅ 早期 return も無限ループも落ちる形 |
| Scenario: deadline 超過は実測値付きで fail する | `ConditionWait.swift:49-61` | `ConditionWaitTests.swift:78-94`, `:97-106` (description / 実測 / deadline / 経過 の 4 要素をメッセージで確認) | ✅ (`actual` 既定値の穴は Minor として別記) |
| **待機なしのレイアウト実行** / Scenario: レイアウト確定だけが必要なテスト | `ios/Tests/KsSettingsViewTestSupport/LayoutRun.swift:15-18` | `ios/Tests/KsSettingsViewTestSupportTests/LayoutRunTests.swift:26-37` | ✅ (閾値の緩さは Minor として別記) |
| **負の検証のための意図明示の固定待機** (名前で判別できる) | `ios/Tests/KsSettingsViewTestSupport/NegativeVerificationWait.swift:25-38` (`waitForNegativeVerification`、doc に `cross/ADR-0027`) | `ConditionWaitTests.swift:123-130` | ✅ |
| Scenario: no-op の確認 | 例: `ios/Tests/KsSettingsViewBridgeTests/KsBridgeUpdateTests.swift:154`, `ios/Tests/KsSettingsViewUITests/ContentUpdateBatchTests.swift:105` | 同左 (呼び出し箇所がテスト) | ✅ |
| Scenario: 不達の確認 | `ios/Tests/KsSettingsViewBridgeTests/KsBridgeHostReleaseTests.swift:81`, `ios/Tests/KsSettingsViewBridgeTests/KsBridgeLifecycleTests.swift:43`, `ios/Tests/KsSettingsViewUITests/StoreDisconnectionTests.swift:65` | 同左 | ✅ |
| 同 SHALL NOT (収束待ちに使わない) | 全 29 呼び出しを個別確認 | — | ✅ 全箇所とも直後の assert が不変性の確認 |
| **収束待ちの全数条件ベース化** (旧パターンが残っていない) | — | — | ✅ `ios/Tests/` の `RunLoop.current.run` は `ConditionWait.swift:45` / `NegativeVerificationWait.swift:33` の 2 定義のみ。`func pump` は 0 件 (撤去する定義 20 個が消えている) |
| Scenario: 収束待ち箇所が条件ベースで書かれている | ターゲット別に置換済み (下の分類別内訳) | 各テスト | ❌ 未記録の分類変更 1 件 (`CustomCellTests.swift:81`) |
| Scenario: flaky が観測されたテストの安定化 | `ios/Tests/KsSettingsViewBridgeTests/KsBridgeCustomCellTests.swift:369-393` (回収完了を述語で待つ) | `evidence/repeat-run-after-fix.md` (10/10)、`evidence/full-suite-after-fix.md` (997 / 0 failures) | ✅ |

### 分類別の突き合わせ (triage.md 対比)

| 分類 | 台帳 | 実装後の観測 | 差分の説明 |
|---|---:|---:|---|
| A (収束待ち) | 160 | 条件ベース待機 (`awaitCondition` / `awaitEqual` / `awaitNonNil` と各ファイルの薄い派生ヘルパ) へ置換済み | deviation.md「置換の粒度」のとおり呼び出し数は call site 数と 1:1 にならない (統合 2 件・分割 3 件)。B→A 3 件を取り込み、A→C 1 件を送出 |
| B (レイアウト実行のみ) | 16 | `layoutNow` 呼び出し 16 (ヘルパ定義・自テスト・空 Section 分岐の 4 箇所を除く) | B→A 3 件 (deviation) を差し引いて 13、Root header テストの 4 段化で +2 (deviation)、`CustomCellTests.host()` 分岐で +1 (**未記録**) → 16 |
| C (負の検証) | 30 | `waitForNegativeVerification` 呼び出し 29 | −1 (`KsBridgeUpdateTests.swift:319` 撤去、deviation)、+1 (`SectionAccessoryRenderingTests.swift:559` A→C、deviation)、−1 (`AccessoryViewLiveProbeTests.swift:299` は Root header テスト書き直しで消滅、deviation) → 29 で辻褄が合う |
| 撤去する定義 | 20 | 0 件残存 | ✅ |

### 追加検査

- [x] **tasks.md**: 全 16 タスクが `[x]`。虚偽チェックなし — 5.2 の証跡 `evidence/full-suite-after-fix.md` の存在と内容 (997 件 / 0 failures、バンドル別の実行件数) を確認した
- [x] **逆流検査**: `proposal.md` / `specs/ios-test-support/spec.md` / `triage.md` はいずれも未変更 (作業ツリーの変更一覧に現れない)。実装期間中に書き換えられた足場は tasks.md のチェックのみで、これは正常
- [x] **付随修正**: deviation.md 記載の 2 件 (Root accessory 追従テストの書き直し / `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:351-361` の doc コメント修正) を確認。後者は本務で触った領域の説明が本 change の handbook 更新によって古くなったことへの追随で、実行コードは変更しておらず、ksn-core「付随修正」の同梱条件 (①同じ能力内 ②公開 API・スキーマ・ADR に触れない ③局所的 ④既存テストで担保 ⑤ユーザー判断の分岐なし) の内側。オーナー判断も記録済み。**スコープ外としての指摘はしない**
- [x] **テスト全件成功**: `evidence/full-suite-after-fix.md` で 997 件 / 0 failures。レビュアー側でも `-only-testing:KsSettingsViewTestSupportTests` を Simulator (iPhone 17) で実行し、7 件 / 0 failures / `** TEST SUCCEEDED **` を確認した
- [x] **UI 変更**: なし (`ui/` アーティファクト無し。妥当)

### ❌ の一覧と見立て

| ❌ | 内容 | 見立て |
|---|---|---|
| 1 | `triage.md` が A 分類とした `CustomCellTests.swift:81` の 7 呼び出しが `layoutNow` (B 相当) になっており、deviation.md に記録が無い | **deviation として合意すべき** (実装の修正は不要)。`Text` / `Image` のみの content には待てる遷移が無いという理由付けは実装の doc コメントに書かれており妥当。snapshot 比較の 3 箇所だけは、描画完了を待たない根拠をコメントに残すと閉じる |

Major 指摘 (初期反映述語の狭さ) は台帳が挙げた遷移証拠に届いていない点で乖離の性質を持つが、`awaitInitialRender` 自体は条件ベース待機であり Scenario「収束待ち箇所が条件ベースで書かれている」の文面は満たすため、対応表上は ❌ に数えていない。判定は review 側の Major として扱う。

## アクションプラン

1. **[Major]** `awaitInitialAccessoryRender` への切り替え (UITests 4 ファイル) と `KsBridgeTestHost.awaitInitialRender` の述語拡張 (supplementary + 全 Section の行数)。過剰と判断するなら根拠付きで deviation.md へ記録する
2. **[verify ❌ / Minor]** `CustomCellTests.host()` の A→B 分岐を deviation.md に記録する
3. **[Minor]** `awaitCondition` の `actual` を必須引数にする
4. **[Minor]** `LayoutRunTests` の閾値をリテラル 0.02 秒程度へ下げる
5. **[Suggestion]** 述語成立後の再レイアウトの扱い / `failureReporter` のスコープ化 / 初期反映ヘルパの重複 — 対応は任意。見送るなら判断だけ残す
6. 1〜4 の修正後は、`ios` ディレクトリで全件 Simulator 実行を再度通し、実行件数を `evidence/full-suite-after-fix.md` に更新する
