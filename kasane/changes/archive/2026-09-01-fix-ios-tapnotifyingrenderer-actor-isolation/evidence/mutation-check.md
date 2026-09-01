# 押下解除テストの回帰検出力 (ミューテーション実測)

作り替えた押下往復テストが、解除側の欠陥を実際に検出できるかを実測した (kasane/lessons/code-review.md L-001)。

## 手順

ios/Sources/KsSettingsViewUI/KsCellViewSupport.swift の押下判定を一時的に壊し、
`if s.isEnabled && isPressed {` を `if s.isEnabled {` に置き換えて (押下状態に関わらず押下色を塗る)
KsSettingsViewUITests の該当 2 クラスを Simulator で実行した。実測後にソースは復元済み。

## 結果 (Executed 60 tests, with 2 failures)

| テスト | 結果 | 意味 |
|---|---|---|
| KsCellViewSupportTests.test_押下中は選択色になり解除後は平常時の実効背景色へ戻る | **失敗** | 解除側を検出できている。失敗メッセージ: 期待値 (1 1 0 = 平常時の背景) に対し実測値 (1 0 1 = 押下色) のまま収束せず |
| SectionBoxDecorationTests.test_押下背景も箱形状に収まる | 通過 | 押下側だけを見る既存テストは壊れない (作り替え前はこちらしか無かった) |
| SectionBoxDecorationTests.test_描画結果で箱とボーダーと下地が観察できる | 失敗 | 平常時の Cell が押下色で塗られる副作用を描画結果から検出 |
| 上記以外の 57 件 | 通過 | |

## 追加実測: 有効判定側 (isEnabled)

同じ押下判定の `isEnabled` 側を壊し、`if s.isEnabled && isPressed {` を `if isPressed {` に置き換えて
(無効 Cell でも押下色を塗る) `KsCellViewSupportTests` を実行した。実測後にソースは復元済み。

結果 (Executed 6 tests, with 1 failure):

| テスト | 結果 | 意味 |
|---|---|---|
| KsCellViewSupportTests.test_無効Cellは押下しても選択色を塗らない | **失敗** | 有効判定側を検出できている |
| KsCellViewSupportTests.test_押下中は選択色になり解除後は平常時の実効背景色へ戻る | 通過 | 有効 Cell の往復は壊れないため |

これで押下判定の 2 つの項 (押下状態・有効状態) それぞれに、落ちるテストが 1 件ずつ対応することを実測で確認した。
