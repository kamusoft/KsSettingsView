---
scope: test
kind: pain
severity: normal
count: 1
first-seen: 2026-09-26
last-seen: 2026-09-26
evidence:
  - ios-appearance-test-observe-rendered-values (追加した CustomCell の外観切替テストの検出力確認で、描画時の背景設定だけを固定の light 値にする一時改変を入れたところテストが緑のままだった。外観切替で UIKit が行の構成更新 [`configurationUpdateHandler`] を呼び、押下色の後始末 [`KsCellViewSupport.restoreNormalColor`] が保存済みの平常色を背景設定へ入れ直して改変を打ち消していた。改変先を保存される平常色そのもの [`setRenderState` に渡す値] へ移して赤を確認した)
---

## ルール文 (候補)

ミューテーションでテストの検出力を確かめるとき、改変は値の**保存元** (後から再設定に使われる値) に入れ、画面へ反映する**設定先**だけには入れない。改変を入れたのにテストが緑なら、「テストに検出力が無い」と結論する前に、観測までの間にその値を設定し直す経路 (framework のコールバック・構成更新・再 bind) が無いかを調べる。事後判定: 検出力の記録に、改変した箇所がどの値を壊したか (保存元か設定先か) が書かれている。

## 経緯

- 2026-09-26 ios-appearance-test-observe-rendered-values: 1 回目の改変 (描画時の設定先) は再設定経路に打ち消されて緑。そのまま「テストに検出力が無い」と読めば、観測方法を描画値へ置き換える方向へ判断が戻るところだった。原因を読んで改変先を保存元へ移し、新テストだけが赤になることを確認した (独立レビューでも同じ改変で再現)
