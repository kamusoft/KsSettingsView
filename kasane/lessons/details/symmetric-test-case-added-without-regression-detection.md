# 対称に足したテストケースが回帰検出力を持たない (L-002 の経緯)

inbox パターン `symmetric-test-case-added-without-regression-detection` (pain, count 3) から 2026-08-13 に昇格。

## 観測

- 2026-08-09 harden-update-accessory-unknown-id (review-001 Minor): 両 OS 対称の操作契約表に足した Android 側2ケースが Store の diffs を観測せず、ガードを無効化するミューテーションでも通過する空振りだった。iOS 側の同名ケースは検出力あり。観測点が「実描画行テキスト + Adapter 通知件数」のみで、未知 ID Diff の発行 (本変更の回帰) を検出できなかった。後続操作の到達を見る `assertDiffDeliveryAlive` probe の追加で解消 (review-002 で落ちることを実測確認)。
- 2026-08-13 fix-ios-replace-cell-type-change (review-001 Minor): 単発/バッチ対称の「同型行は同一実体維持」ケースのうちバッチ側の `===` assert が、バッチを常時 reload に退行させるミューテーションでも通過する空振りだった (reuse プールが同一インスタンスを返し得るため)。単発側の同名ケースは検出力あり。実害なしの Minor として未修正のまま既知事項化。
- 2026-08-13 align-maui-accessory-placement-guard (review-001 Minor): content 側と対称に足した guard 解除 2 行とバッチ内数えあげに、除去しても落ちるテストが 1 本も無かった。レビューがミューテーション実測で無防備を証明し、解除 2 箇所を各々固定するテスト追加で解消。「対称だから」を根拠に足したコードは、対称の相手と同じ検出力のテストを伴っているかまで確認しないと無防備のまま残る。
