---
scope: spec-review
kind: pain
severity: normal
count: 3
first-seen: 2026-08-01
last-seen: 2026-09-05
evidence:
  - align-sample-parity (実装フェーズで、承認済み mock の「通知種別」初期値「メール, アプリ内」が spec の「パラメータは現行を維持する」= 現行「メール, SMS」と食い違い、オーナー裁定が必要になった)
  - fix-cell-accessory-vertical-fill (mock のアクセサリ列 gap 16px に実装が合った結果、valueText とアクセサリの間隔が現行の約 6pt から約 16pt へ広がり、オーナーが 6pt への差し戻しを指示。deviation.md 記録)
  - add-sample-dark-mode-toggle (承認モック plan-a.html / approved.png の基本 Cell 7 種デモに「無効なボタン」の行が描かれていたが、現行の 3 面 [iOS / Android / MAUI] のどれにも存在しない行だった。加えて ButtonCell の文言もモックは「登録」で現行 3 面の「ログアウト」と不一致。値ではなく行そのものと文言の創作。4 実行面の照合ワーカーは黙ってこの行を除いて「一致」と判定し、MAUI ワーカーは逆に「iOS / Android にはある行が MAUI に無い」と誤った parity 指摘を上げた [grep で 3 面とも不在を確認]。実害は無かったが、mock の構造が現行実装と一致しているかを spec-review で照合していれば起きない)
---

## ルール文

提案レビューでは、spec が「現行を維持する」と書いたパラメータ (初期値・選択肢・上限・単位・間隔などの寸法値) について、mock 内の該当値が現行実装のコードと一致しているか照合する。一致しない値を mock が描いている場合は、承認前に mock を現行値へ修正させる。「生値は非規範」と brief に書くだけでは防げない — 実装が mock の生値に引きずられた実例があるため、値そのものを合わせる。

## 経緯

- 2026-08-01 align-sample-parity: mock (plan-b.html / approved.png) が PickerCell 複数選択の初期値を「メール, アプリ内」と描いていたが、現行 iOS 実装は `[0, 2]` = 「メール, SMS」。spec は差分を列挙したうえで「上記の変更を除きパラメータは現行を維持する」と規定していたため、「見た目の正 = mock」と「挙動の正 = spec」が真っ向から食い違った。実装ワーカーが判断を上げ、オーナーが spec 優先で裁定 (deviation.md に記録)。mock 作成時に現行実装の値を写経していれば発生しなかった。
- 2026-08-01 fix-cell-accessory-vertical-fill: mock (plan-a.html / approved.png) がアクセサリ列の gap を 16px で描いており、実装が `stackH.spacing = 16` をそのまま使った結果、CommandCell / Picker 系の valueText とアクセサリの間隔が現行の約 6pt から約 16pt へ広がった。spec は「spacing・margin 等の視覚パラメータは本 spec の対象外」、brief も「spacing・寸法の生値は非規範で、現行実装のトークン解決値を維持する」と**防御を書いていた**が、実装・レビュー・相方レビューの誰も「非規範だから現行値へ戻す」とは判断せず、全員が「mock と一致 = 問題なし」と評価した。オーナーが 6pt への差し戻しを指示 (deviation.md に記録)。**防御的な文言では実装を現行値に引き戻せない**ことがこのケースの学び — mock 側の値を現行に合わせるのが確実。
- 2026-09-05 add-sample-dark-mode-toggle: mock が現行に無い行 (「無効なボタン」) を描いていた。パラメータ値だけでなく Section / 行の構成も、spec が「現行を維持」とする画面については mock 作成時に現行実装から写すべき対象。
