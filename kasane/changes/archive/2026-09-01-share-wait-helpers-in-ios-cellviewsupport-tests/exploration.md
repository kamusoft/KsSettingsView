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

## 検討した選択肢 (却下案と理由を含む)

- **案A (採用): 2 つの private ヘルパを既存共有ヘルパの呼び出しへ置き換える。** `waitForFirstCell` → host 内の `awaitInitialRender(controller)` (前例: `SectionBoxDecorationTests.host`) + `cellForItem` の guard 取得、`waitForBackgroundColor` → `awaitCondition` に背景色述語を直接渡す形 (前例: `SectionBoxDecorationTests.swift:1066-1069`)
- **案B (却下): 共有ターゲットに「特定 Cell の背景色が期待値になるまで待つ」便利関数を追加してから寄せる。** UIColor の比較は色空間の都合で `isEqual` を使う必要があり `awaitEqual` (Equatable `==`) に寄せられないため、専用関数は「isEqual 比較の色待ち」という薄い層にしかならない。利用箇所は 2 ファイルで割に合わず、前例側 (`SectionBoxDecorationTests` の 2 箇所) も書き換えないと不揃いになる

## 決定事項

- 案A を採用 (2026-09-01 探索で確定)。根拠:
  - 対象テストは「1 Section・1 Cell・Root accessory なし」の構成で、`awaitInitialRender` の広い述語に置き換えても検証内容は変わらない (待つ対象が「先頭 Cell だけ」から「テストが直後に読む全部」へ広がるだけ)
  - 置き換え先はすべて既存の形で揃っており、新規 API の追加は不要
- ADR は不要 (テスト 1 ファイルの内部書き換え。覆すコスト低・境界を越えない・将来を制約しない)

## ADR 候補

なし (決定事項に記載のとおり選別基準に該当しない)

## 未決の論点

なし

## 変更級の推奨: S (確定)

テストファイル 1 つの内部書き換えのみ・公開 API 変更なし・完全可逆・UI なし。デルタスペック不要、直接実装 (置き換え + Simulator 全件テスト) で進める。
