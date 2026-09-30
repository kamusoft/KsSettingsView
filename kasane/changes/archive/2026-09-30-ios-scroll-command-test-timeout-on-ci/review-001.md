# レビュー結果: ios-scroll-command-test-timeout-on-ci (001 回目)

**日付**: 2026-09-30
**判定**: APPROVED

## サマリー

3 ファイルとも、命令の処理を待つ待機の失敗報告に exploration.md「決定事項」が列挙した観測値をそろえて足している。合否条件・deadline・待機条件 (`until:`)・`ios/Sources/` は変わっていない。scratch コピーの実装に「命令を実行しない」ミューテーションを入れて失敗させ、報告が意図した形で出ることを 3 ファイルで確かめた。命令からの経過時間と main キューの消化の読み取りに関する低優先度の指摘が 2 件あるが、どちらも次の CI での切り分けを妨げるものではない。

## 照合した規約

- cross/comment-policy.md (always)
- cross/test-execution.md (テストを実行するとき・テスト結果を報告するとき)
- 該当しないため本文照合を省いたもの: ios/swift6-language-mode-check.md (`ios/Sources/` を触る変更ではない)、cross/runtime-behavior-verification.md (スクロール制御の実装は変えていない)
- 関連 ADR: cross/ADR-0027 (負の検証の固定待機) — 負の検証の待機には手が入っておらず抵触なし。core/ADR-0037 (スクロール命令のハンドルを Host へつなぐ) — 実装に触れないため影響なし
- lessons/code-review.md の重点観点 L-001 (ミューテーションで実測する) に従い、失敗報告の中身をミューテーションで確かめた (下記)

ロードしたスキル: ksn-review (コンテキストパッケージの解決結果どおり、追加の code-review スキルなし)

## 確認したこと

**ビルドとテスト** (専用シミュレータ ksn-scroll-probe、iOS Simulator の全件を 1 回実行)
- `xcodebuild test -scheme KsSettingsView` で TEST SUCCEEDED。バンドル集計行の合算で 1201 tests / 0 failures (786 + 189 + 131 + 88 + 7)
- 変更した 3 ファイルからのコンパイル警告なし。`scripts/comment-policy-lint.py --advisory` の検出なし

**失敗報告の中身の実測 (ミューテーション)**
- scratch にコピーした `ios/` だけで `canExecuteScrollEntries` が常に false を返すよう変え、各ファイル 2 件ずつ計 6 件を 1 回だけ実行した (作業ツリーの `ios/Sources/` には触れていない。コピーは実行後に trash 済み)
- 6 件とも待機が deadline で落ち、報告に次の形で観測値が並んだ (DSL 方式の例):
  `processed=0 / 受け口: 引き直し=世代 1 → 1 宣言ツリーの世代=1 受け口の Host が同一=true / 待ち行列: 遅延前=0 遅延後=1 遅延の予約中=false / 実行の条件: 最初の反映済み=true 適用中の snapshot=0 window=true 高さ=600.0 / アニメーション中=false / 命令から 3.006 秒 命令の直後の main.async=回った`
- Store 方式の DSL テストでは `受け口: Host へ直接接続`、UIKit / Bridge では受け口の項目を除いた同じ並びになった。候補 (a) に当たる状態 (遅延後に 1 件が残り、予約はなく、main キューは回っている) が報告から読み取れる

**観測値の網羅 (決定事項との照合)**

| 観測値 | UIKit | Bridge | DSL |
|---|---|---|---|
| 待ち行列: 遅延前・遅延後・遅延の予約中 | あり | あり | あり |
| 実行の条件: 最初の反映済み・適用中の snapshot・window・高さ | あり | あり | あり |
| 進行中のアニメーション | あり | あり | あり |
| 受け口の引き直し・宣言ツリーの世代・受け口の Host が同一か | 対象外 | 対象外 | あり |
| 命令からの経過時間・命令の直後の `main.async` | あり | あり | あり |

`canExecuteScrollEntries` の条件のうち `internalDataSource != nil` だけは報告に出ないが、最初の反映済みが true なら data source はあるので、切り分けの材料としては足りている。

**記録の置き場所 (処理済み件数で待つ待機との対応)**
- 3 ファイルの `awaitScrollSettled` / `awaitProcessed` / `awaitExpandedAndSettled` / 処理済み件数を `until:` に持つ `awaitCondition` をすべて洗い出した。どの待機も、同じテストの経路で直前に出した命令の後に `markIssued` がある。前の命令の記録が残ったまま別の命令を待つ箇所はない (`beginManualAnimation` / `attachWithCommand` / `scrollHost` / `captureAnchorAtCell` の各ヘルパの中も含む)
- Host の作り直しで積まれる位置の戻し (Bridge の `makeHost` と UIKit の `restoreScrollAnchor`) も、戻しを積んだ直後に記録している
- 命令を続けて出すテストでは最後の命令の後に記録しており、経過時間は最後の命令から数える。報告の意味として一貫している

**検証の意味を変えていないこと**
- diff の中で待機に関わる変更は `actual:` の差し替えだけ。`until:`・deadline・assert は同一。`awaitCondition` は `actual()` を deadline 超過時にしか評価しない (`ios/Tests/KsSettingsViewTestSupport/ConditionWait.swift`) ため、観測用の状態参照が通る実行の順序に影響する余地はない
- `markIssued` が積む `main.async` はフラグを立てるだけで、UIKit の状態にもテストの状態にも触れない。順序に意味のあるテストも確かめた
  - `ScrollControlTests` の「進行中の反映がある間は…」: Host の遅延 → 記録 → `observedAtDeferral` の順になる。Host の遅延より前に割り込まないため、観測する時点は変わらない
  - DSL の「Host へ渡った後の未実行の命令も…」: 受け口の引き直し → テストの割り込みブロック → (Host の遅延, 記録) の順。割り込みが Host の遅延より前に入るという前提は保たれる
- `IssuedCommand` が DSL のハンドルを強参照で持ち続けるが、`KsScrollController` は受け口を weak で持つため、Host や宣言ツリーまで生き残ることはない

**読みやすさ・重複**
- `IssuedCommand` / `markIssued` / `scrollCommandState` が 3 つのテストターゲットにほぼ同じ形で重複している。ただし観測する内部メンバーは `@testable import KsSettingsViewUI` でしか読めず、共有ターゲット `KsSettingsViewTestSupport` は `KsSettingsViewUI` に依存していない。まとめるには `Package.swift` の依存を変える必要があり、S 級のテスト変更の範囲を超える。約 40 行 × 3 の重複は妥当と判断した
- コメントはファイル単体で意味が通り、作業文書や change への参照はない

## 指摘事項

### [🟡 Minor] 割り込みブロックの中だけで記録しているため、main キューが止まったときの報告が「記録なし」になる
**該当箇所**: `ios/Tests/KsSettingsViewSwiftUITests/DSLScrollControlTests.swift:511-521`
**問題点**: `first.scrollToEnd` の後に積んだ `main.async` ブロックの中でだけ `markIssued(second)` を呼んでいる。待っているのはそのブロック内で出す位置の戻しなので、ブロック内での記録自体は正しい。しかし候補 (b) (main キューが止まる) の状態ではブロックが実行されないため、報告は「受け口: 命令の記録なし」「命令の記録なし」になる。この表示は記録の漏れと同じ見え方で、(b) を示す「命令の直後の main.async=未実行」と経過時間が出ない。このファイルの他の待機では (b) が報告から直接読み取れるのに、ここだけ読み手の推理が必要になる。
**推奨修正**: `first.scrollToEnd(animated: false)` の直後 (ブロックの外) で `markIssued(first)` も呼ぶ。ブロックが走れば内側の `markIssued(second)` で上書きされるので、通常の報告は今と変わらない。ブロックが走らなければ、最初の命令からの経過時間と「未実行」が出る。

### [🔵 Suggestion] 命令から待機までの間に別の待機を挟むテストでは、main キューの消化の観測値が必ず「回った」になる
**該当箇所**: 例 `ios/Tests/KsSettingsViewUITests/ScrollControlTests.swift:451-461` (記録と待機の間に負の検証の固定待機)、`ScrollControlTests.swift:433-436`・`:946-949` ほか、`ios/Tests/KsSettingsViewBridgeTests/KsBridgeScrollControlTests.swift:361-364` ほか (記録と待機の間に `present(_:)` の最初の反映待ち)
**問題点**: 命令の時点で記録するという決定事項どおりの置き場所だが、間に挟まる待機が RunLoop を回すので、記録用の `main.async` は処理済み件数の待機が始まる前に必ず回る。このため、これらのテストでは「命令の直後の main.async=回った」が (b) の判定材料にならない。「命令から N 秒」にも間の待機の時間が含まれる。`awaitCondition` 自身の「経過」も並んで出るので、報告が誤読を招くほどではない。
**推奨修正**: 必須ではない。これらのテストで CI の失敗が起きたときは、main キューの消化を「待機の経過が deadline を大きく超えているか」で読む、と論点 1 の判定時に意識しておけば足りる。必要になったら、待機の開始時にも印を積んで、待機中の消化を別に出す。

## アクションプラン

1. (任意・低優先) Minor: DSL の割り込みテストで、ブロックの外でも `markIssued(first)` を呼ぶ
2. (任意) Suggestion: 論点 1 の判定時に、present や負の検証を挟むテストでは main キューの消化の観測値を割り引いて読む
