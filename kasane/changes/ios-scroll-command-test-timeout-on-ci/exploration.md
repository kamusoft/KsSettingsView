# Exploration: ios-scroll-command-test-timeout-on-ci

## 課題 / 動機

iOS の SwiftUI DSL のスクロール制御のテスト `test_ForEachのkeyでSectionを指せる` (`ios/Tests/KsSettingsViewSwiftUITests/DSLScrollControlTests.swift`) が、develop の検証 CI で 1 度失敗した。再実行で通るかどうかに頼る状態は、リリース前の事前確認 (develop の CI が緑であること) を不安定にする。

発見の文脈: リリース 0.1.0-beta.8 の事前確認中、develop の検証 CI (ci.yml) の `ios / verify` の step「Run tests on iOS Simulator」で失敗した。

- run: https://github.com/kamusoft/KsSettingsView/actions/runs/36677242463 (attempt 1、commit 4d06111)
- 失敗の内容
  - `DSLScrollControlTests.swift:288`: 「命令 1 件の処理」が deadline 3.000 秒以内に成立せず、経過 4.084 秒の時点でも `processed=0`
  - `DSLScrollControlTests.swift:289`: 続く位置の検査も連鎖して失敗 (見出しの上端 2345.0、期待値 0.0)
- 同じ iOS コードでの前回 run (commit 77e90ac、run 36676536425) では同テストが 0.448 秒で通っている。4d06111 の変更は MAUI のテスト 1 ファイルだけで、iOS には触れていない
- 失敗した回はこのテスト 1 件に 11.5 秒かかっており、シミュレータが重かった
- テストの `host(_:)` は、SwiftUI 配下の `KsSettingsViewController` が見つかり、最初の snapshot が反映されるまで待ってから返す。スクロール命令のハンドルは `KsSettingsView` の `makeUIViewController` の中で接続される (`ios/Sources/KsSettingsViewSwiftUI/KsSettingsView.swift`)。そのため「接続前に出した命令が捨てられた」わけではなさそう
- スクロール制御 (add-scroll-control) で足したばかりのテストで、CI での実行回数はまだ少ない

### 探索で確かめた事実 (コードと CI ログ)

命令の経路 (DSL 方式): ハンドル → 受け口 `DSLScrollCommandResolver` が ID を宣言ツリーの最終 ID へ引き直す (`main.async` 1 回) → Host の待ち行列 (`main.async` 1 回) → 実行できる状態なら実行して `processedScrollCommandCount` を進める。

- 実行できる状態の条件は 4 つ: 最初の snapshot 反映済み・適用中の snapshot が 0 件・window に載っている・高さがある (`canExecuteScrollEntries`、`ios/Sources/KsSettingsViewUI/KsSettingsViewController+ScrollControl.swift`)。満たさないときの再試行の合図は、snapshot 適用の完了 (`applySnapshot` の完了処理) と Host の `viewDidLayoutSubviews` の 2 つ
- 待機ヘルパ `awaitCondition` は毎回のループで渡した view (テストでは hosting の view) のレイアウトしか走らせず、Host 自身の `viewDidLayoutSubviews` を必ずしも起こさない (`ios/Tests/KsSettingsViewTestSupport/ConditionWait.swift` の `settleLayout`)。命令が待ち行列で止まった場合、テストの側からは救済されず、実装側の合図だけが頼り
- 受け口が ID を引き直せなかったときの警告 (Debug ビルドで出る) は、同じ run の他のテストでは出ているが、このテストでは出ていない → 「key 42 が見つからず捨てた」線は外れる
- 経過 4.084 秒は deadline 3 秒を 1 秒超過しており、待機ループの 1 回が 1 秒以上止まっている。同じジョブの他のスイートも壁時計が重い (例: テスト本体 0.258 秒に対し 10.357 秒)
- CI 履歴: このテストは c5e5fec (add-scroll-control) で入り、`ios / verify` で 4 回実行されて 1 回失敗 (attempt 2 と次の run 36678973786 は成功)
- 手元の負荷なしの 1 回の計測 (観測値を足した使い捨てのコピーで取得): 命令から処理まで 0.03〜0.05 秒、`host(_:)` の所要 0.03〜0.2 秒。`host(_:)` を抜けた時点で宣言ツリーの世代は 1 (make の後に update が 1 回走っている)

通常なら 2 回の `main.async` は待機ループ (10 ミリ秒刻み) の 2 周で済むため、4 秒間 0 件は「遅いだけ」では説明しにくい。原因の候補:

| 候補 | 中身 |
|---|---|
| (a) 待ち行列で止まった | 命令は Host に届いたが実行の条件がそろわず、再試行の合図が 4 秒来なかった (例: `host(_:)` 後に走ったアニメーション付き snapshot 適用の完了が、重い環境で遅れた) |
| (b) main キュー自体が止まった | main スレッドが 4 秒の大半で止まり、2 回の遅延が回らなかった |
| (c) 想定外の経路 | 命令がテストの見ている Host とは別の Host に届いた等 |

今の失敗報告は処理済み件数しか出さないため、(a)(b)(c) を区別できない。

## 検討した選択肢 (却下案と理由を含む)

論点 1 (原因の切り分け) の進め方として検討した:

- **A: 手元で負荷をかけて反復実行し再現を試す** — 最初に採った。CPU 負荷のプロセスを立ててテストの束を反復実行したが、手元マシン全体に影響するためユーザーの指示で中止した (束 1 回分で停止、失敗なし)。手元で CPU 負荷をかけた再現は以後行わない
- **負荷なしで手元で反復実行する** — 却下。負荷なしでは命令の処理が 0.05 秒程度で deadline 3 秒に遠く、再現の見込みが低い割にマシンを数十分使う
- **B: 失敗報告に観測値を足し、次に CI で起きるのを待つ** — 採用。手元への負荷がなく、次の発生で (a)(b)(c) を区別できる。代償は原因の確定が次の発生まで持ち越しになり、その間 CI の揺れが残ること
- **C: deadline を延ばして収める** — 却下。CI は最速で落ち着くが、候補 (a) (命令が待ち行列で止まる経路が実装にある) だった場合に本番の不具合を隠す

## 決定事項

- 論点 1 の切り分けは B で進める: 命令の処理を待つテストの失敗報告に観測値を足し、次の CI での発生を待つ
- スコープ: 同じ待ち方 (処理済み件数の変化で待ち、失敗時に処理済み件数だけを出す) をしている iOS のスクロール制御テスト 3 ファイルすべて
  - `ios/Tests/KsSettingsViewSwiftUITests/DSLScrollControlTests.swift`
  - `ios/Tests/KsSettingsViewUITests/ScrollControlTests.swift`
  - `ios/Tests/KsSettingsViewBridgeTests/KsBridgeScrollControlTests.swift`
- 失敗報告に足す観測値 (候補を区別する材料)
  - Host の待ち行列: 遅延前の件数 (`incomingScrollEntries`)・遅延後の件数 (`readyScrollEntries`)・遅延の予約中か (`isScrollDeferralScheduled`)
  - 実行の条件: 最初の反映済みか・適用中の snapshot の件数・window の有無・高さ
  - 進行中のアニメーションの有無 (既存)
  - DSL 方式のみ: 受け口が引き直しを行ったか (`lastResolvedGenerations`)・宣言ツリーの世代・受け口の Host がテストの見ている Host と同じか
  - 命令を出してからの経過時間と、命令の直後に積んだ `main.async` が回ったか (main キューの消化の確認)
- テストの合否条件・deadline・実装 (`ios/Sources/`) は変えない。直す先 (論点 3) は原因がはっきりしてから決める
- Android / MAUI のスクロール制御テストは対象外 (失敗は iOS のみで、待ち方の仕組みも platform ごとに異なる)

## ADR 候補

なし (テストの観測値を足すだけで、覆すコスト・境界・将来の制約のいずれにも当たらない)

## 未決の論点

- 論点 1: なぜ 4 秒経っても `processedScrollCommandCount` が 0 のままだったのか — 候補 (a)(b)(c) のどれかは、観測値を足した後の次の発生で判定する
- 論点 2: 同じファイルの他のテスト (`awaitProcessed` を使うもの) にも同じ揺れの余地があるか — 論点 1 の答えに従う
- 論点 3: 直す先がテストの待ち合わせ (deadline・待つ条件) なのか、実装側の命令処理の経路なのか — 論点 1 の答えに従う。(a) なら実装側 (重い端末でスクロール命令が効かない不具合になりうる)、(b) なら待ち合わせ側の見直し

## UI 素材

## 変更級の推奨: S (ユーザー確定)

テストの失敗報告だけの変更で、公開 API・実装・データに触れない。iOS の 1 能力 (スクロール制御) のテスト 3 ファイルに閉じ、可逆で局所的。原因が判明して直す段階は、その時点で別途級を判定する (候補 (a) なら実装の修正になる)。
