# レビュー結果: ios-appearance-test-observe-rendered-values (001 回目)

**日付**: 2026-09-26
**判定**: APPROVED

## サマリー

合意済みスコープ (exploration.md の「決定事項」範囲 1・2) を過不足なく満たしている。冒頭コメントから未検証の主張 (画素に現れない) が除かれ、保持色の観測で足りる根拠と取りこぼす 2 種類が自己完結した文で書かれている。CustomCell の外観切替テストは実経路 (window の外観切替) を通り、一時改変で赤になることをレビュー側でも再現した。全件実行も成功している。Critical / Major / Minor の指摘は無く、Suggestion 1 件のみ。

## 照合した規約

- cross/comment-policy.md (always)
- cross/test-execution.md (テストを実行するとき・テスト結果を報告するとき)
- lessons: code-review.md (重点観点 L-001 に従い一時改変で検出力を実測)、test.md (L-001: 実経路での検証。L-002 は対称テスト表への追加ではないため該当なし)
- core/ADR-0030 (コメントが根拠として参照する決定。記述と矛盾しないことを確認)

ios/ の handbook (swift6-language-mode-check.md) は `ios/Sources/` を触る変更の完了判定が適用のきっかけで、本変更は `ios/Sources/` を触らないため対象外。

## 確認した内容

**ビルド・テスト**
- 対象クラスの絞り込み実行 (iPhone 17 Simulator): 5 tests / 0 failures
- 絞り込みなしの全件実行 (iPhone 17 Simulator、バンドル集計行の合算): 1117 tests / 0 failures (Bridge 173 / Core 88 / SwiftUI 112 / TestSupport 7 / UI 737)。`** TEST SUCCEEDED **`

**検出力の実測 (lessons code-review L-001)**
- deviation.md 2 件目と同じ一時改変 (`ios/Sources/KsSettingsViewUI/CustomCellView.swift` の `setRenderState` に渡す `effectiveBackgroundColor` を `resolvedColor(with: traitCollection)` で描画時点の固定値にする) を入れ、対象クラスを実行した。新テスト `test_表示中に外観をダークへ切り替えるとCustomCellの行背景がdark既定になる` だけが落ち (5 tests / 1 failure)、既存 4 本は通過した
- 改変は `git checkout` で戻し、改変前に取った shasum との一致と `git diff -- ios/Sources` が空であることを確認した

**仕様充足 (合意スコープ)**
- 範囲 1: `ios/Tests/KsSettingsViewUITests/ThemeDarkAppearanceRenderingTests.swift:12-17` が「なぜ足りるか (UIKit の描画時の解決と同じ計算)」「検出する壊れ方 3 種」「検出しない 2 種 (上塗りで隠れる・UIKit が描き直さない)」を書いており、決定事項の ①② と対応する
- 範囲 2: `ThemeDarkAppearanceRenderingTests.swift:240-268` がライト表示 → ダーク切替で CustomCell の行背景 `#1C1C1E` と行の identity 維持を観測する。観測は既存の `appliedCellBackgroundColor` を流用しており決定事項どおり。先頭行が `CustomCellView` であることを前提として確かめており、別種の行を観測して空振りする形になっていない
- 旧 3.4 / 3.6 の追加・旧 3.7 の観測置き換えは行われていない (決定事項どおり)
- deviation.md の 2 件 (製品に `configurationUpdateHandler` 経由の再設定経路がある事実の訂正、一時改変の改変先の変更) は合意済みの差分として扱った。1 件目の訂正は、新しい冒頭コメントの記述 (dynamic 色を設定し解決は UIKit に任せる) と矛盾しない — 入れ直すのも同じ dynamic 色であり、`ios/Sources/KsSettingsViewUI/KsCellViewSupport.swift` の `applyRenderedBackgroundColor` / `installSelectedColorHandler` の実装で確認した
- 足場: exploration.md の差分は探索結果 (2026-09-26 の決定事項) の記入であり、実装中の書き換えではない。実測で誤りと判明した事実は exploration.md を直さず deviation.md に記録されており、規律どおり

**コメント規約**
- 外部参照は `core/ADR-0030` の ID 形式のみ (許容参照)。作業文書のパス・change 名・通番・履歴記述・デルタスペック構文キーワードは無い。`scripts/comment-policy-lint.py --advisory` で当該ファイルの検出 0 件
- 追加したテストの doc コメントは非公開のテストメソッドで、公開 doc コメントの制約は該当しない

**その他**
- `import SwiftUI` は `CustomCell` の builder に渡す `Text` のため。同じターゲットの `CustomCellTests.swift` 等と同じ書き方
- 待機は既存ハーネス (`awaitInitialRender` / `layoutNow`) を使い、固定秒数の待機を持ち込んでいない

## 指摘事項

### 🔵 Suggestion 追加テストの doc コメントが「描き直される」と観測範囲を超えて書いている
**該当箇所**: `ios/Tests/KsSettingsViewUITests/ThemeDarkAppearanceRenderingTests.swift:240`
**問題点**: doc コメントが「未指定の背景が dark 既定の値で描き直される」と書くが、このテストが観測するのは保持色を行自身の trait で解決した値で、描き直し (描画結果) そのものではない。同じファイルの冒頭コメント (12-17 行) は「UIKit が保持中の色を描き直さない場合は検出しない」と明記しており、同じファイル内で記述が食い違って読める。テスト名 (`…行背景がdark既定になる`) は観測範囲に合っている。
**推奨修正**: 「CustomCell の行も、表示中の外観切替で未指定の背景が dark 既定の値に解決される。」のように、観測している事実に合わせた動詞にする。既存テスト `test_表示中に外観をダークへ切り替えると既定色が描き直される` の名前は今回の範囲外なので触らなくてよい。

## アクションプラン

1. (任意) 上記 Suggestion の doc コメントの言い回しを直す
2. 蒸留時の申し送り (レビュー判定には影響しない): deviation.md 1 件目の「外観切替で `configurationUpdateHandler` が呼ばれ、`restoreNormalColor` が平常色を入れ直す」経路は、描画時の設定だけを壊す一時改変がテストで検出されない理由になる。検出力を実測する次の作業者が同じ空振りに当たらないよう、長命層へ残す価値があるかを ksn-distill で判断する

## 再確認 (2026-09-26)

**判定**: APPROVED (変更なし)

- Suggestion「追加テストの doc コメントが観測範囲を超えて書いている」は解消した。`ios/Tests/KsSettingsViewUITests/ThemeDarkAppearanceRenderingTests.swift:240` は「CustomCell の行も、表示中の外観切替で未指定の背景がその行の trait で dark 既定の値に解決される。」になり、観測している事実 (保持色を行自身の trait で解決した値) と一致する。冒頭コメント (12-17 行) とも食い違わない
- 修正後の diff を読み直し、変わったのはこの doc コメント 1 行だけで、テストの本体・冒頭コメント・`import` に変化が無いことを確認した。`ios/Sources` の差分も空
- `scripts/comment-policy-lint.py --advisory` で当該ファイルの検出は 0 件。外部参照・履歴記述・内部用語の混入も無い
- 変わったのがコメント 1 行だけなのでテストは再実行していない。前回の結果 (全件 1117 tests / 0 failures、一時改変で新テストが赤) をそのまま有効とする
- 新たな指摘は無い
