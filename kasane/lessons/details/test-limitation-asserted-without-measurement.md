# test-limitation-asserted-without-measurement (impl L-010 の経緯)

inbox パターンとして pain 3 件で閾値到達し、2026-09-19 にオーナー承認で `impl.md` L-010 へ昇格した (強制力ルーティングは lessons: 機械検出できる性質ではなく、設計判断・記述でもないため)。

## ルール文 (昇格時)

「テスト環境では X を検証できない」という制約をコメント・証跡・報告に書き、それを理由に検証を弱める (アサーションを間接化する・実機に委ねる・禁忌の待機に戻す) ときは、その制約を最小の実験コードで実測してから書く。実測せずに書いた「できない」は検証範囲の穴として固定化される。

## 経緯

- 2026-08-01 fix-android-cell-width-allocation: 実装ワーカーが「`isSingleLine` の TextView は `Layout` 幅が `VERY_WIDE` になるため Robolectric では実描画位置を検証できない」と KDoc / ui/brief.md に断定で記録し、オーナー要求「aux あり + CENTER の title 位置検証」を前提チェックのみの弱いテストで実装した。review-002 が調査テストで実測した結果、`dispatchOnPreDraw()` を 1 行呼べば `scrollX` 補正が再現され px 単位で検証可能と判明 (前半の `VERY_WIDE` は正しく、後半の「補正を再現しない」だけが誤り)。修正はレビュー 2 周を要し、確立した検証手法は `kasane/handbook/cross/test-execution.md` へ蒸留された。
- 2026-09-01 fix-ios-tapnotifyingrenderer-actor-isolation: 実装ワーカーではなくオーケストレーター自身が、Simulator の実行時証跡メモに「押下を保持したままの静止画を撮る手段が無い」と書き、それを理由に押下ハイライトの視覚確認を「範囲外」に落とした。独立レビューが「この不可能性は検証された形跡がない」と指摘。実際には押下を保持するtouch path を送りながら別プロセスから `xcrun simctl io <device> screenshot` を時間差で撮れば取得でき、押下行だけが押下色で塗られたフレームが残せた。**書き手が実装ワーカーでも指揮側でも同じ形で起きる**。
- 類似パターン: [deviation-cause-written-without-reading-source](deviation-cause-written-without-reading-source.md) (未読の推測を原因として記録)。どちらも「未検証の断定を証跡に書く」だが、こちらは断定がテストの検証範囲を直接狭める点で影響が異なる。
- 2026-09-19 picker-selection-after-dismiss: レビュー指摘への修正で実装ワーカーが「Activity ホスト下では `awaitMainLooperCondition` が戻らない」と報告し、handbook の禁忌である `idle()` 待機で追加テストを書いた。review-002 が同 package の姉妹テスト `DateCalendarDialogTest` に同じヘルパの成立例があることを示して差し戻し、再試行で成立。ハングの正体は `--tests` によるクラス単体指定の既存問題で、ワーカーは実行モードと待機ヘルパを切り分けずに「戻らない」と断定していた。レビュー 1 周を消費。**制約の断定が実行モードの条件と混ざったまま報告される形**。count 3 で pain の閾値到達 — 昇格候補。
