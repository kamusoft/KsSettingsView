# レビュー結果: picker-selection-after-dismiss (003 回目)

**日付**: 2026-09-19
**判定**: APPROVED

## サマリー

review-002 の Minor (追加 2 本の待機が `idle()` で、待機規約と `cross/ADR-0027` のどちらにも沿っていない) は**閉じた**。推奨どおり 2 行が置き換わっており、肯定側は条件ベース待機 (`awaitMainLooperCondition`)、否定側は名前で意図が判別できる固定量待機 (`drainMainLooperForUnchangedCheck`) になった。姉妹テスト `DateCalendarDialogTest.kt:262` / `:273` と同形である。

本サイクルの差分は該当テストファイルの 2 行のみで、他のファイル・足場アーティファクトに手は入っていない。Android 全件はクリーンな `--rerun-tasks` 実行で 2984 件 / 0 失敗。新たな問題は持ち込まれていない。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always) — 本サイクルの差分にコメントの追加・変更なし。`scripts/comment-policy-lint.py` は禁止 0 件 / 検査対象 811 ファイルで exit 0
- `kasane/handbook/cross/test-execution.md` (テストを実行するとき・テスト結果を報告するとき) — 「収束を待つアサーション」節の 3 条件と負の検証の例外規定に照合。適合を確認 (下記)
- `kasane/decisions/cross/0027-negative-verification-fixed-wait-exception.md` (accepted) — 負の検証の待機を名前で判別可能にする決定に照合。適合を確認 (下記)
- 前回までに照合済みで今サイクルの差分が触れない規約 (`cross/runtime-behavior-verification.md` / `maui/integration-host-verification.md` / `ios/swift6-language-mode-check.md`) は再照合の対象外とした

## 検証したこと (指摘に至らなかった確認)

- **指摘の是正が入っていること**: `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/DateCalendarRecreationTest.kt:172` が `awaitMainLooperCondition(diagnostics = { "受け取った通知: $events" }) { events.size == 2 }`、`:185` が `drainMainLooperForUnchangedCheck()`。ファイル内に残る `idle()` は 410 / 412 / 419 / 421 の 4 箇所だけで、いずれも本 change の diff 外 (既存の `launch` / `recreate` ヘルパ)
- **肯定側が待機規約の 3 条件を満たすこと**: `KsSettingsViewTestSupport.kt:36-56` の `awaitMainLooperCondition` は (a) `System.nanoTime()` ベースの実時間 deadline (既定 5000 ms)、(b) ループ内で `looper.runOneTask()` / `Thread.sleep(1)` による実行機会の譲渡、(c) 超過時に `diagnostics()` の実測値を載せた `fail()` の 3 つをすべて備える。`Thread.yield()` ではなく `sleep` を使う点も規約の但し書きどおり
- **待機が空振りしないこと**: 待機の直前に `assertEquals(listOf("changed:$PICKED_DATE"), events)` があるため、待機に入る時点で `events.size == 1` が確定している。条件 `events.size == 2` が最初から成立していて待たずに通る形にはなっていない
- **否定側が ADR-0027 の線引きに合うこと**: 取消は「何も起きないこと」の確認であり待つべき正の完了条件がない。`drainMainLooperForUnchangedCheck` (同 `KsSettingsViewTestSupport.kt:65-72`) は呼び出し名から不変性確認の待機と判別でき、上限 1000 件で打ち切るため、この change 自身が足したヘルパを負の検証で使う形に揃った。review-002 が指摘した内部矛盾は解消している
- **テスト再実行 (完了判定)**: `cd android && ./gradlew test --rerun-tasks` で **BUILD SUCCESSFUL in 2m 27s**、JUnit XML 集計で `kssettingsview` debug 1315 / release 1315、`kssettingsview-bridge` debug 177 / release 177 の**合計 2984 件 / 0 失敗 / 0 エラー**。実装者の報告と一致する。追加 2 本の所要は肯定側 0.066 秒・否定側 0.073 秒で、待機が時間切れまで引っ張る形にはなっていない (deadline 5000 ms に対して十分小さい)
- **足場の凍結**: `proposal.md` / `design.md` / `specs/` / `deviation.md` / `tasks.md` に本サイクルの差分なし。作業ツリー全体の tracked 差分は 47 ファイル / 1904 insertions / 86 deletions で review-002 時点と同一 — 2 行の置換が行数に影響しないことと整合し、他ファイルへの手入れが無いことの裏取りになる
- **deviation の追記要否**: 今サイクルの差分は待機の書き方の是正のみで、仕様に対する新たな乖離ではないため追記不要 (review-002 のアクションプラン 2 と同じ判断)
- **既知の先送り**: tasks 6.1 / 7.3 の iOS 実機分はオーナー担当として本レビューの対象外

## 指摘事項

### [🔵 Suggestion / 別 change 相当] `launch` / `recreate` ヘルパの `idle()` が、既知の単体クラス指定ハングの候補として残る

**該当箇所**: `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/DateCalendarRecreationTest.kt:410` / `:412` / `:419` / `:421`

**問題点**: handbook `cross/test-execution.md` の Android 節は、Compose の `DatePicker` を載せたカレンダーの選択面を提示した後の `shadowOf(Looper.getMainLooper()).idle()` が `PopupLayout.pollForLocationOnScreenChange` に main looper を占有されて戻らず、テストが無言で固まると記している。`recreate` ヘルパは選択面が提示され直した直後に `idle()` を 2 回呼んでおり、この禁忌の形に当たる。review-002 が実測した「`--tests '*DateCalendarRecreationTest'` の単体クラス指定が戻らない」性質の候補になり得る。

ただしこれは**本 change の diff 外の既存コード**であり、review-002 が追加テストを取り除いた状態でも同じハングを実測しているとおり、本サイクルの是正が持ち込んだものではない。モジュール単位以上の実行では顕在化せず全件 0 失敗であるため、判定には影響させない。

**推奨修正**: 本 change では手を入れない。単体クラス指定でも回せるようにするなら、別 change で `recreate` / `launch` の `idle()` を条件ベース待機へ置き換える題材として起票する (review-002 のアクションプラン 4 と同じ扱い)。

## アクションプラン

1. **対応不要**。review-002 の Minor は閉じており、Critical / Major はない。次工程 (検証済みであれば蒸留) へ進んでよい
2. 上記 Suggestion は本 change のスコープ外。単体クラス指定のハングを直す価値があると判断する場合のみ、別 change として起票する
3. 既知の先送り (tasks 6.1 / 7.3 の iOS 実機分) は本レビューの対象外として扱った
