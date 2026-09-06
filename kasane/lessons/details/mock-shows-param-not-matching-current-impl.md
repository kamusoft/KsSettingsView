# mock が現行実装と一致しない値・行・文言を描く

`lessons/spec-review.md` L-003 の経緯。

## ルール文

提案レビューで、spec が「現行を維持する」と定める画面・パラメータ (初期値・選択肢・寸法値・行構成・文言) について、mock が描く該当値を現行実装のソースと 1 つずつ照合し、一致しない値・行・文言は承認前に mock を現行へ修正させる。brief に「生値は非規範」と書くだけでは実装が mock の生値に引きずられた実例があるため、mock 側の値を合わせる。事後判定: brief.md の承認記録に、現行との照合を行った対象 (画面・値) が書かれている。

## 経緯

- 2026-08-01 align-sample-parity: mock (plan-b.html / approved.png) が PickerCell 複数選択の初期値を「メール, アプリ内」と描いていたが、現行 iOS 実装は `[0, 2]` = 「メール, SMS」。spec は差分を列挙したうえで「上記の変更を除きパラメータは現行を維持する」と規定していたため、「見た目の正 = mock」と「挙動の正 = spec」が真っ向から食い違った。実装ワーカーが判断を上げ、オーナーが spec 優先で裁定 (deviation.md に記録)。mock 作成時に現行実装の値を写経していれば発生しなかった。
- 2026-08-01 fix-cell-accessory-vertical-fill: mock (plan-a.html / approved.png) がアクセサリ列の gap を 16px で描いており、実装が `stackH.spacing = 16` をそのまま使った結果、CommandCell / Picker 系の valueText とアクセサリの間隔が現行の約 6pt から約 16pt へ広がった。spec は「spacing・margin 等の視覚パラメータは本 spec の対象外」、brief も「spacing・寸法の生値は非規範で、現行実装のトークン解決値を維持する」と防御を書いていたが、実装・レビュー・相方レビューの誰も「非規範だから現行値へ戻す」とは判断せず、全員が「mock と一致 = 問題なし」と評価した。オーナーが 6pt への差し戻しを指示 (deviation.md に記録)。防御的な文言では実装を現行値に引き戻せない — mock 側の値を現行に合わせるのが確実。
- 2026-09-05 add-sample-dark-mode-toggle: 承認モック (plan-a.html / approved.png) の基本 Cell 7 種デモに、現行の 3 面 (iOS / Android / MAUI) のどこにも無い「無効なボタン」の行が描かれ、ButtonCell の文言もモックは「登録」で現行 3 面の「ログアウト」と不一致だった (値ではなく行そのものと文言の創作)。4 実行面の照合ワーカーは黙ってこの行を除いて「一致」と判定し、MAUI ワーカーは逆に「iOS / Android にはある行が MAUI に無い」と誤った parity 指摘を上げた。文言差分は 3 周目の相方レビュー (second-opinion-code-003) で初めて表面化し、承認ゲートの扱いが NEEDS_DISCUSSION になってオーナー裁定 (モックの規範範囲を配色と外観 UI に限定、行構成・文言は合意済み差分) が必要になった。spec が「現行を維持」とする画面については、パラメータ値だけでなく Section / 行の構成と文言も mock 作成時に現行実装から写す対象。
