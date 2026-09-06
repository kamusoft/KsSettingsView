# 経緯: 未実測の推定を長命層・spec の前提に断定形で書く (impl L-007)

inbox パターンとして pain 3 件で閾値到達し、2026-09-06 にオーナー承認で `impl.md` L-007 へ昇格した。

## 観測 (3 change)

- customcell-android-maui-perf (exploration.md で「〜と推定」だった iOS 側の機構説明が、concepts へ起こす過程で留保を失い断定形になった。review-001 Major-1 が指摘 — しかも断定内容は本プロジェクトの iOS 検証経路 (Simulator = JIT 実行) には当てはまらず、機構としても不正確だった)
- add-spm-distribution (付随修正で handbook cross/test-execution.md に追記した「複数バンドル時の件数集計手順」が、書いた文言どおりに実行されないまま確定した。review-002 Major が実測と突き合わせて捕捉 — 旧文言に従うと 1000 件の実行を 2998 件と報告する。review-003 で新文言どおりの集計が実測 1000 と一致することを確認して解消)
- fix-default-colors-dark-appearance (design Risks が「同梱テーマの `textColorPrimary` と 2 セットの値 [黒 / 白] は実質同じ」と実測なしに断定し、spec Scenario「ライトの既定は本 change 前と同じ (Cell title の色も本 change 前の実効値と一致)」の前提にした。実際の Material3 同梱テーマの `textColorPrimary` は #1D1B20 で、実装後の light 既定 #000000 と一致しない。8.4 のライト A/B で Android 8 面すべての title / valueText 差分として露呈し、オーナー裁定が必要になった)

## 経緯

- 2026-09-01 add-spm-distribution: handbook test-execution.md への件数集計手順の追記 (規範層の改訂) が、実出力での検証なしに確定した。`Executed` 行の全合算という文言は中間サマリ行も拾って二重計上する。規範が governs する当の領域 (件数確認) で誤った手順を指示する形になり、CHANGES_REQUESTED の根拠となった。

- 2026-08-28 customcell-android-maui-perf: 「iOS は Debug でも AOT 混在で動くため乖離が小さい」を concepts に断定形で記載。実測表に iOS の行は無く、レビューが「実測と未計測の推定が同じ確度で並び、後続が iOS 側の調査をこの一文で打ち切るリスク」と指摘。さらに実機 Debug には当てはまるが Simulator (JIT・Mac の性能) 経路には成り立たない不正確さも判明し、未計測の明記 + 経路依存の注記へ書き直した。
- 類似パターン: [test-limitation-asserted-without-measurement](test-limitation-asserted-without-measurement.md) (未実測の断定を証跡に書きテストを弱める)。どちらも「未検証の断定の固定化」だが、こちらは長命層 (ksn-drift のディープ検証対象) に入る点で腐り方が長期化する。
- 2026-09-05 fix-default-colors-dark-appearance: 足場 (design) の Risks 節に「実質同じ」と書かれた推定が、spec の Scenario (ライトの title は本 change 前と同一) の前提になった。長命層ではなく足場でも、Scenario の前提になる推定は 1 回の実測 (`resolveAttribute(textColorPrimary)` の値を読む) で確定できた。実測しない断定が spec の検証条件に昇格し、テストも推定側の値 (パレット) と比較して緑になり、視覚 A/B まで露呈しなかった。

観測の共通構造: 推定は足場 (exploration / design の Risks) では留保付きで書かれていても、長命層や spec へ転記される時点で留保が脱落し、実測と同じ確度の断定として固定される。3 例目は足場内 (design → spec) の転記でも同じ形で起き、1 回の実測 (属性値を読む) で確定できる推定がテストの期待値になってしまった。
