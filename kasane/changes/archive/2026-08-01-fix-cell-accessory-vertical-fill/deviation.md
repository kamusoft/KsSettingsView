# 実装乖離メモ: fix-cell-accessory-vertical-fill

デルタスペック・承認済みモックと実装の差分のうち、オーナー指示により合意したもの。

- **valueText と Cell 級アクセサリの間隔**: 承認済み mock (`ui/mock/plan-a.html` / `approved.png`) は
  アクセサリ列の gap を 16px として描いており、`stackH.spacing = 16` をそのまま使うと CommandCell /
  Picker 系で valueText とアクセサリの間隔が構造変更前の約 6pt から約 16pt へ広がる
  (`settings-view-ios-host` の Requirement は「spacing・margin 等の視覚パラメータは本 spec の対象外 —
  `ui/mock/` と AiForms 原典参照が正」と規定) → **オーナー指示により約 6pt を維持する**。
  理由: 構造変更前の見た目を保つ (アクセサリ列の導入は配置関係の修正が目的であり、既存の間隔を
  変える意図はない)。実装は `stackH` の `stackV` 直後にのみ custom spacing を当て、icon と stackV の
  間隔 (16pt) は変更しない (2026-08-01)
