# レビュー結果: ios26-5-supplementary-recycle-test-timeout (002 回目)

**日付**: 2026-09-26
**判定**: APPROVED

## サマリー

差分は、再決定した論点1 (案 D) のとおりに実装されている。画面外は位置で判定し、画面外にある間にテスト自身の操作で先頭 header を作り直させ、戻したときに「スクロール前と別の supplementary 実体に同じ view が載る」ことを待つ。この形を Bridge テストと UI 診断テストの両方に当てている。review-001 の Major 2 件と Suggestion 1 件は解消した。再設定経路を壊すミューテーションは、両テストとも毎回検出した (Bridge 3/3 と 1/1、UI 3/3)。ほかに優先度の低い Minor が 1 件ある。UI 診断テストの遷移証拠は、作り直しの時点で UIKit が header をまだ保持していることを前提にしている。

## 照合した規約

- `cross/comment-policy.md` (always): 追加・変更したコメントを節ごとに照合した。許容参照の範囲に収まっており、禁止参照 (作業文書のパス・裸参照・ローカル通番)、履歴記述、構文キーワードは無い。`scripts/comment-policy-lint.py --advisory` でも対象 2 ファイルの検出は 0 件。適合
- `cross/test-execution.md` (テスト実行・結果の報告): 「収束を待つアサーション」の 3 条件、「何を待機条件に選ぶか」、iOS の実行方法 (Simulator 実行・バンドル集計行による件数確認) を照合した。3 条件は共有の `awaitCondition` が満たしている。待機条件の選び方は指摘 1 を除いて適合
- lessons `code-review.md` L-001 (ミューテーションで検出力を実測する): 下の「実測」で実施した
- lessons `test.md` L-001 (保証したい性質そのものを実経路で検証する): 再バインドが実際に supplementary の提供処理を通ることを、ミューテーションの発火位置で確かめた

## 実行したテスト

- 全件実行 (作業ツリー、iPhone Air / iOS 26.5、バンドル集計行を合算): **1116 tests / 0 failures** (Bridge 173 / Core 88 / SwiftUI 112 / TestSupport 7 / UI 736)
- 対象 2 テストの反復実行 (`-test-iterations 25`、作業ツリーと同じ内容の複製): **Bridge 25 / 0 failures、UI 25 / 0 failures**

## 実測

`ios/` を scratchpad へ複製し、複製側だけに一時的なミューテーションと観測コードを入れた。実行機は iPhone Air (iOS 26.5)。作業ツリーの対象 2 ファイルは、実測の前後で shasum が一致した。

| 測定 | 結果 |
|---|---|
| M1: `applyAccessoryToListCell` の UIKit 分岐で、既に別の親に載っている view を載せ直さない | UI 診断テストは **3/3 で失敗**した。失敗したのは「別の supplementary 実体で new を載せて再表示される」の待機だけで、前段のアサーションはすべて通過した。Bridge テストは通過した。Bridge 側は factory が取り付けの前に親から切り離すため、M1 の条件に当たらない |
| M2: Bridge の `KsBridgeAccessoryView.anyView` で、取り付け済みの view を渡されたときは別の空 view を返す (同じ view を再バインドできない回帰を模す) | Bridge テストは **3/3 で失敗**した (待機が deadline を超過し、「同一 view が再バインドで再表示される」も失敗)。同じクラスのほか 13 件は通過した。失敗時の実測では、戻したあとの supplementary はスクロール前と別の実体になっていた |
| M3 (review-001 と同じ型): スクロール開始以降に呼ばれた section 0 header の supplementary 提供で、view accessory を渡さない | Bridge テストは **1/1 で失敗**した。M3 が発火したのは先頭へ戻したときの提供処理で、作り直しの経路が supplementary の提供処理を通るという doc コメントの主張と一致する。UI 診断テストでは M3 が一度も発火しなかった (Section が 1 つで、画面外にある間に提供処理が呼ばれない) ため、UI 側の検出力は M1 で確かめた |
| E4: 画面外の判定と作り直しの間に「UIKit が header を手放すまで待つ」段を挟む (UIKit が作り直しより前に header を回収した状況を模す) | Bridge テストは 5/5 で通過した。**UI 診断テストは 5/5 で失敗**した。戻したときに、再利用プールからスクロール前と同じ実体が取り出され、そこへ new が正しく載っている。つまり製品側は正しく再バインドしているのに、「別の実体」の条件が成立しない (指摘 1) |

## review-001 の指摘の解消状況

| review-001 の指摘 | 状況 | 根拠 |
|---|---|---|
| 🟠 Major: Bridge テストが再バインドの経路を検証しなくなった | **解消** | 画面外にある間に `updateAccessoryView` で作り直しを起こし、戻したときに別の実体へ probe が載ることを待つ (`ios/Tests/KsSettingsViewBridgeTests/KsBridgeAccessoryViewTests.swift:343-358`)。M2・M3 を検出した |
| 🟠 Major: UI 診断テストの「再表示される」待機が操作前から成立していた | **解消** | 待機条件が「スクロール前と別の実体 かつ その実体に new が載る」になった (`ios/Tests/KsSettingsViewUITests/AccessoryViewDetachDiagnosticTests.swift:200-211`)。作り直しの前は、保持中の実体がスクロール前と同じなので成立しない。`awaitCondition` の契約 (操作前には成立しない遷移証拠) に適合する。M1 を検出した。行コメントとアサーション文言は、作り直された supplementary を経由する実態に合っている |
| 🔵 Suggestion: 「hidden のまま」が事象と合っていない | **解消** | 理由コメントが「しばらく返し続け、手放す時期は実行機の速さで変わる」に改められた (Bridge :304-308、UI :42-46) |

## 指摘事項

### 🟡 Minor UI 診断テストの「別の実体」は、作り直しの時点で UIKit が header を保持していることを前提にしている
**該当箇所**: `ios/Tests/KsSettingsViewUITests/AccessoryViewDetachDiagnosticTests.swift:180-211`
**問題点**: この fixture は Section が 1 つだけである。作り直しより前に UIKit が先頭 header を回収していると、戻したときにスクロール前と同じ実体が再利用プールから取り出される。その場合、new が正しく載っていても `current !== supplementaryBeforeScroll` が成立しない。E4 の実測では 5/5 で deadline を超過した。現状は、位置の判定がレイアウト 1 回で成立して RunLoop を回さずに作り直しへ進むため、回収が割り込む余地はなく、実行機の速さにも依存しない (反復 25/25 で通過)。ただし、同期レイアウトの中で UIKit が画面外の supplementary を手放すかどうかは UIKit の保持方針が決める事柄である。行コメント (:181-183) は「手放す時期は実行機の速さで決まるため」別の実体を待つと説明しており、逆向きの前提 (作り直しの時点ではまだ手放していない) には触れていない。将来 UIKit の保持方針が変わると、製品が正しくても「別の実体で new を載せて再表示される」の timeout として落ち、原因を読み違えやすい。Bridge テストは Section が 12 あってプールに別の実体があるため、E4 でも通過した。
**推奨修正**: 作り直しの直前に、前提を明示するアサーションを置く (例: `XCTAssertTrue(headerSupplementary(cv) === supplementaryBeforeScroll, "前提: 作り直しの時点で UIKit が先頭 header を保持している (...)")`)。あわせて、行コメントにこの前提を一文足す。この class は UIKit の振る舞いの変化を観測するための診断テストなので、前提が崩れたときは前提アサーションとして落ちるのが本来の形になる。蒸留時の handbook 追記 (既知の先送り) でも、「実体の同一性を遷移証拠にするときは、再利用プールから同じ実体が戻る可能性を考える」を候補に含められる。

## 確認した観点 (指摘なし)

- 合意スコープ: 変更はテスト 2 ファイルに閉じており、製品コードには触れていない。deadline は既定の 3.0 秒のまま。exploration.md の決定事項 (2026-09-26 に追加した 3 項目を含む) をすべて満たしている。deviation.md の 2 項目は合意済みの差分として扱った
- 位置の判定: `layoutAttributesForSupplementaryElement` が nil のときは「画面外ではない」側へ倒しており、誤って成立する方向には崩れない。UI 診断テストのスクロール量 400 は内容高より大きいが、`contentOffset` を直接書き換えているので位置判定は成り立つ (失敗時の実測で、戻したあとの frame と可視矩形を確認した)
- 経路の主張: Bridge の doc コメントにある「作り直しの経路は … 同じ supplementary の提供処理を通る」は、M3 が先頭へ戻したときの提供処理で発火したことで裏付けられた (提供処理 → `makeAccessoryListCell` → `applyAccessoryToListCell` → factory)
- 失敗時の観測値: header の frame、可視矩形、保持中の supplementary の実体と hidden、スクロール前の実体、載っている view を出している。ミューテーションと E4 の失敗メッセージだけで、何が起きたかを切り分けられた
- Bridge 側の末尾のアサーション (:357-361) は待機条件と重なるが、待機の timeout 後に個別の失敗理由を出す役割があり、害はない
- コメント規約: 公開 doc コメントの変更は無い (テストの internal メンバーのみ)

## アクションプラン

1. (任意・低優先) UI 診断テストの作り直しの直前に、「UIKit が header を保持している」ことの前提アサーションを置き、行コメントにその前提を足す (指摘 1)
2. 蒸留時の handbook 追記 (既知の先送り) で、review-001 の申し送りに加え、指摘 1 の「実体の同一性を遷移証拠にするときの再利用プールの考慮」も候補に含める

## Minor 修正の確認 (2026-09-26 追記)

**判定: APPROVED のまま** (指摘 1 は解消。任意の Suggestion を 1 件足す)

### 確認した修正
`ios/Tests/KsSettingsViewUITests/AccessoryViewDetachDiagnosticTests.swift:195-201` に、作り直しの直前の前提アサーション (UIKit がスクロール前の supplementary を保持している) と、その理由を書いた行コメント 3 行が入った。コメントは自己完結しており、「Section が 1 つだけ」「再利用プールから同じ実体が取り出される」という前提の根拠をファイル単体で読める。コメント規約 lint の advisory でも、対象ファイルの検出は 0 件だった。失敗メッセージは、同じファイルにある他の「前提:」アサーションと同じ書き方で、header の位置・可視矩形・保持中とスクロール前の実体を載せている。

### 実行したテスト
- 全件実行 (作業ツリー、iPhone Air / iOS 26.5): **1116 tests / 0 failures** (Bridge 173 / Core 88 / SwiftUI 112 / TestSupport 7 / UI 736)
- `AccessoryViewDetachDiagnosticTests` を 25 回反復 (複製側、修正後のファイルと同じ内容): **150 tests / 0 failures**。前提アサーションが通常の実行で誤って落ちることはない

### E4 の再実験
複製側で、前提アサーションの直前に「UIKit が header を手放すまで待つ」段を挟んで 5 回実行した。

| 結果 | 回数 |
|---|---|
| 前提アサーション (作業ツリーでは :198。複製側は観測コードの 3 行分ずれて :201) が「前提: 作り直しの前に UIKit が header を手放している」で失敗 | 5/5 |
| 続けて、後段の「別の supplementary 実体で new を載せて再表示される」の待機も deadline 超過で失敗 | 5/5 |

UIKit の保持方針が変わったときに、最初に報告される失敗が前提アサーションになり、原因を直接読める。これで指摘の意図は満たしている。作業ツリーの対象 2 ファイルは、実験の前後で shasum が一致した。

### 🔵 Suggestion 前提が崩れたとき後段の timeout も続けて出る
**該当箇所**: `ios/Tests/KsSettingsViewUITests/AccessoryViewDetachDiagnosticTests.swift:198-201`
**問題点**: `XCTAssertTrue` は失敗してもテストを止めない (`continueAfterFailure` は既定の true)。そのため E4 では、前提アサーションの失敗に続いて後段の待機が 3 秒待ってから timeout を出した。最初の失敗が前提アサーションなので読み違えは起きにくいが、失敗は 2 件報告され、3 秒余計にかかる。
**推奨修正** (任意): 前提が崩れた時点でテストを打ち切る。たとえば `guard headerSupplementary(cv) === supplementaryBeforeScroll else { XCTFail("前提: ..."); return }` にする。同じファイルのほかの前提は `XCTUnwrap` で打ち切っているので、それとも揃う。
