# レビュー結果: add-scroll-control (005 回目)

**日付**: 2026-09-30
**判定**: APPROVED

## サマリー

対象は、オーナー指示で足したスコープ 5 件 (deviation.md の「スコープの拡張 (オーナー指示)」と (1)〜(5) の行) の修正サイクルの後の状態。前回の突き合わせ (second-opinion-code-004.md 末尾) で確定・採用した 4 件は、いずれも解消した。確定 Major は「再レイアウトの後に中央・下端の合わせがずれる」問題で、直近の実行で最後に位置を決めた行き先を控え、新しい表示範囲で解き直す形に改まっている。これが既存のスクロール制御 (命令の順序・一方向の着地・位置の復元) を壊していないことも確かめた。新しい指摘は、この change で足したテストの待機ヘルパが超過時に実測値を出さない点 (優先度の低い Minor 1 件) だけで、Critical / Major は無い。

### 前回の指摘の解消状況

| 前回の指摘 (second-opinion-code-004 の採否) | 状況 | 確認した内容 |
|---|---|---|
| 🟠 Major (確定): iOS の再レイアウトの後に `center` / `end` の合わせがずれる | **解消** | `flushScrollEntriesIfPossible()` は、実行の前に `lastSettledScroll` を空にする。そのうえで、行き先が求まった `performScroll` ごとに (行き先, 合わせる位置) を上書きする。これで「その実行で最後に位置を決めた命令」が残る (対象の無い命令は上書きしないので、最終位置を決めた命令と一致する)。`scheduleAncestorRelayout()` は、それが Cell・Section・先頭・末尾なら新しい表示範囲で `performScroll(..., animated: false)` で解き直し、戻し (`isAnchor`) か空なら従来どおり上端の要素とずれを保つ (`ios/Sources/KsSettingsViewUI/KsSettingsViewController+ScrollControl.swift:198-227`)。テストは Cell・Section × 中央・下端と、末尾の 5 件 (`ios/Tests/KsSettingsViewUITests/ScrollControlTests.swift:1231-1289`)。親の表示領域が 500 から 552 へ伸びる構成で、新しい表示範囲の中央・下端に合うことを確かめている。前回の上端を保つだけの形に戻すと、中央 26pt・下端 52pt ずれて落ちる (deviation のスコープの拡張 (2) の見直しの行の実測と、静的な読みが一致する)。判定が割れていた `.end` も、テストで確かめられている |
| 🟡 Minor (確定): 「戻し直さない」2 条件を守るテストが無い | **解消** | `test_再レイアウトの前に届いた命令があるときは再レイアウトの後に位置を解き直さない` の仕組み: 再レイアウトの予約 (`main.async`) の後に次の命令を出すと、その命令の `main.async` は予約より後に並ぶ。そのため、再レイアウトの時点では次の命令が `incomingScrollEntries` に残る。次の命令のアニメーションの出発点 (`startOffset`) が命令を出した時点の位置と一致することで、解き直していないことを確かめている。条件を外すと中央の解き直しで 26pt 動き、出発点がずれて落ちる。`test_最初のレイアウトで始めた命令のアニメーションは祖先の再レイアウトで打ち切られない` は、途中の位置を 3 つ以上経て末尾に着くことを見る。条件を外すと、再レイアウトの時点でアニメーションなしの解き直しが末尾へ飛ばし、途中の位置が 1 つ以下になって落ちる。どちらも前提 (処理件数・予約済み・領域が伸びた) を先に確かめている |
| 🔵 Suggestion (採用): Sample の `MainActor.assumeIsolated` が公開 doc に無い前提に頼る | **解消** | `ios/Sources/KsSettingsViewCore/KsCell.swift:34-40` に「通知のクロージャが呼ばれるスレッド」の節があり、公開 Cell の通知の閉包 16 個 (12 Cell) すべての doc からそこを参照している (grep で漏れが無いことを確かめた)。節で名指しする `onItemSelected` は `PickerCell+ItemProjection.swift` に実在する。呼び出し元も抜き取りで確かめた。`PickerCellView` / `DatePickerCellView` の選択面の閉包は、いずれも `@MainActor` の Cell の view の中から呼ばれる。型は変えていない。公開 doc に内部用語は無い |
| 🔵 Suggestion (採用): `ComposeRootAccessoryTextTest` の両方渡しのテストが行の配置を待たない | **解消** | `findRootViewRows(layout).size == 2` (Composable の Root 行が 2 つ配置される) を条件ベースで待ってから、文字列の行の不在を確かめる形になった (`android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/compose/ComposeRootAccessoryTextTest.kt:140-145`)。待つ条件は、検証したい意味 (差分の反映が済んだこと) を直接表す行の配置で、代理の値ではない |

### 新鮮な目で確かめた観点

- **既存のスクロール制御への影響**: 解き直しは `readyScrollEntries` / `incomingScrollEntries` が空で、アニメーションが進行中でないときだけ行う。そのため「後から出した命令が最終位置を決める」順序の保証は変わらない。解き直しは同じ行き先への `animated: false` の送り直しなので、アニメーションの一方向の着地にも関わらない。命令の実行がレイアウトの外 (snapshot の完了・遅延の `main.async`) で起きたときは再レイアウトを予約しないので、従来の経路は変わらない。再レイアウトで寸法が変わらない場合、解き直しは同じ位置への送り直しになり、目に見える変化は無い。位置の復元の経路 (`isAnchor` と `lastSettledScroll == nil`) は、前回 APPROVED の形のまま残っている
- **`lastSettledScroll` が古くなる経路**: 値を書くのは `performScroll` だけで、再レイアウトを予約するのは `viewDidLayoutSubviews` の実行の直後に限られる。その実行の冒頭で値を空にするので、前の実行の行き先が紛れ込む経路は無い。再レイアウトの時点で snapshot の適用が進行中のことはありうるが、適用の後のレイアウト属性で同じ行き先を解き直すだけで、命令の意味は保たれる
- **MAUI iOS の再確認**: `evidence/maui-ios-reconnect-band-after-recheck-{1-A-scrolled-to-end,1-B-after-reconnect,3-B-after-reconnect}.png` を見た。末尾へ送った状態 (1-A) から 3 回目の再接続の後 (3-B) まで、一覧は画面の下端まで届き、白い帯は無い。末尾の Root Footer も見えたまま保たれている。bin/obj を捨てたフルビルドであることは deviation に記録されている (lessons process L-003)
- **(3) 宣言の差分**: `planReorder` (Android `DSLDiffCalculator.kt:183-218`) を読み直した。削除を適用した後の並びの最長増加部分列を動かさず、追加は直前の配置済みの項目の直後、残りは新しい順に直前の項目の直後へ移す。適用後の並びは宣言と一致し、index は適用時点の並びで出ている。`indexOf` の繰り返しで O(n²) になるが、設定画面の 1 Section の項目数では問題にならない
- **(1)(4)(5)**: 修正サイクルでの変更は無い。前回の確認 (版の引き上げの理由のコメント・Composable を優先する扱い・Sample の書き方の統一) から変わっていないことを確かめた
- **足場**: tasks.md の追加スコープの項目の書き換えは無く、足場 (proposal / design / specs) も書き換えられていない。修正サイクルで触ったファイルは、いずれも deviation.md の行に箇所として現れている (スコープの拡張 (2) の見直しの行・Cell の公開 doc の付随修正の行・`ComposeRootAccessoryTextTest` の付随修正の行。lessons process L-009)
- **コメント規約**: 修正で入ったコメント (`scheduleAncestorRelayout()`・`lastSettledScroll`・`isAnchor`・テストの区切り) は、いずれも単独で意味が通る。作業文書の番号・履歴記述・デルタスペックの構文キーワードは無い

### 実行したテスト

Simulator / Emulator は起動していない。

- Android: `./gradlew :kssettingsview:testDebugUnitTest --tests 'jp.kamusoft.kssettingsview.compose.*' --rerun-tasks` を実行し、180 tests / 0 failures (うち `ComposeRootAccessoryTextTest` 6、`DSLDiffCalculatorTest` 27)
- iOS: Simulator を使わない `xcodebuild build-for-testing -scheme KsSettingsView -destination 'generic/platform=iOS Simulator'` で、本体とテストがコンパイルできることを確かめた (`TEST BUILD SUCCEEDED`)。全件の結果は、ホストの報告 (1201 tests / 0 failures、Swift 6 言語モードの error 0) を採った
- MAUI: 修正サイクルでの変更は無い。ホストの報告 (624 / 0) を採った

## 照合した規約

- cross/comment-policy.md (always): 修正で足したコメントと公開 doc コメント (Cell の通知の閉包のスレッド) に、禁止参照・内部用語・履歴記述が無いこと
- cross/test-execution.md (テスト実行・結果報告): 条件ベースの待機の 3 条件 (実時間の deadline・実行機会の譲渡・超過時の実測値付き fail)、待機条件の選び方、件数の確認
- cross/runtime-behavior-verification.md (実行時挙動の不具合修正の完了判定): (2) の見直しの後の MAUI iOS での再確認の証跡
- ios/swift6-language-mode-check.md (`ios/Sources/` を触る変更の完了判定): ホストの報告 (error 0) と、ビルドの成功を確認
- lessons: code-review.md (L-001。テストの検出力は、条件を外したときの動きを静的に追って確かめた)、process.md (L-003・L-005・L-006・L-009)

## 指摘事項

### 🟡 Minor `ComposeRootAccessoryTextTest` の待機ヘルパが、期限を過ぎたときに実測値を出さない

**該当箇所**: `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/compose/ComposeRootAccessoryTextTest.kt:287-301`
**問題点**: `awaitCondition` は期限を過ぎると `fail("$description まで $timeoutMillis ms 以内に到達しなかった")` で落ちるが、その時点の実測値を載せていない。cross/test-execution の「収束を待つアサーション」は、超過時に実測値をメッセージに載せることを 3 条件の 1 つにしている。実測値が無いと、「実装が壊れた」と「待機が足りない」を失敗メッセージから区別できない。同じパッケージの他の待機ヘルパ (`ComposeScrollControlTest`・`DSLTimePickerHourCycleRenderingTest` など) は、実測値を載せている。今回の修正で足した「Composable の Root 行が 2 つ配置される」待機もこのヘルパを通るので、落ちたときに配置された行の数が分からない。
**推奨修正**: `awaitCondition` に実測値を返す引数 (例: `actual: () -> String`) を足して fail のメッセージに含める。あるいは既定で、配置済みの行の ViewHolder の種類の一覧と `rootHeader` / `rootFooter` を載せる。この change で新設したテストファイルの中で数行で閉じる。

## アクションプラン

1. (Minor・優先度低) `ComposeRootAccessoryTextTest.awaitCondition` の超過時のメッセージに実測値を載せる

## 追記: Minor の修正確認 (2026-09-30)

**解消**。`ComposeRootAccessoryTextTest.awaitCondition` は、観測 (`observe`) と判定 (`until`) を分ける形になった。期限を過ぎると、最後の観測値を `describe` で文字列にして `fail` のメッセージへ載せる。cross/test-execution の 3 条件を満たしている (実時間の deadline・ループ内の `Thread.sleep(1)`・超過時の実測値付き `fail`)。

呼び出し元は 6 か所 (Root Footer の消滅・Composable の Root 行の配置・style の到達・`awaitLayouts`・`awaitLayout`・`awaitRootTextRow`)。いずれも観測値は条件の意味そのもの (行の配置・Host の値) で、framework の代理の値ではない。判定の式は修正前と同じなので、検出力は変わらない。`awaitRootTextRow` は、条件を満たした同じ観測の中から行を返す。

実行: `./gradlew :kssettingsview:testDebugUnitTest --tests 'jp.kamusoft.kssettingsview.compose.*' --rerun-tasks` で 180 tests / 0 failures (うち `ComposeRootAccessoryTextTest` 6)。判定は APPROVED のまま。
