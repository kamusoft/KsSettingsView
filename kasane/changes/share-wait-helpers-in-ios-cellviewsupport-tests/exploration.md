# Exploration: share-wait-helpers-in-ios-cellviewsupport-tests

fix-ios-test-pump-condition-wait の develop マージ時 (2026-09-01) に受け皿として簡易起票。

## 課題 / 動機

`ios/Tests/KsSettingsViewUITests/KsCellViewSupportTests.swift` が、共有ターゲット `KsSettingsViewTestSupport` の待機ヘルパを使わず、private な待機ヘルパを 2 つ自前で持っている:

- `waitForFirstCell(in:timeout:file:line:)` (`:44-65`) — 先頭 Cell の生成を待つ
- `waitForBackgroundColor(_:of:in:timeout:file:line:)` (`:67-91`) — 背景色の収束を待つ

**待機規約 (`kasane/handbook/cross/test-execution.md`) には違反していない。** どちらも実時間 deadline で区切り、ループ内で RunLoop を短く回して実行機会を譲り、超過時は実測値付きで `XCTFail` する形で書かれている (条件ベース待機として正しい)。

問題は 2 点:

1. **共有ターゲットへの単一定義集約が崩れる方向にある。** fix-ios-test-pump-condition-wait は「private コピーの散在を解消し、待機ヘルパを共有ターゲット 1 つに集約する」ことを受け入れ条件として達成した。同 change と並行して develop 側で書かれた本ファイルが、同じ役割の private コピーを新たに 2 つ作っている
2. **`waitForFirstCell` の述語が狭い。** 待つのは「先頭 Section の item 0 の Cell」だけで、共有ヘルパ `awaitInitialRender` が持つ広い述語 (全 Section の行数一致・可視領域にかかる全行・全 Section の header / footer supplementary・Root accessory) を持たない。これは fix-ios-test-pump-condition-wait のレビューで Major として 3 サイクルかけて解消した「共有 setup ヘルパの述語が、呼び出し側が直後に読む対象より狭い」型と同じ ([[shared-setup-wait-predicate-narrower-than-callers-read]])

現状確認: 2026-09-01 に develop マージ後のツリーで両ヘルパの存在をコードで確認済み (行番号は同日時点)。全件テストは 1000 件 0 failures で通っている。

出典: `kasane/changes/fix-ios-test-pump-condition-wait/` の実装完了後、develop (`e93fafe fix(ios): 行タップ通知と押下フィードバックを Swift 6 の actor 分離に適合させる`) をマージした際に発見。

## 検討した選択肢 (未検討)

- 案A: 2 つの private ヘルパを共有ターゲットのヘルパ (`awaitNonNil` / `awaitEqual` / `awaitInitialRender`) の呼び出しへ置き換える
- 案B: `waitForBackgroundColor` のように「特定の Cell の特定プロパティが期待値になる」型が今後も要るなら、共有ターゲット側に便利関数として追加してから寄せる

どちらが妥当かは、共有ヘルパで置き換えたときに検証内容が変わらないかを実物で確かめてから決める。

## 決定事項

(未定 — 探索前)
