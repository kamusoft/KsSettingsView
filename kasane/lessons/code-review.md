---
scope: code-review
timestamp: 2026-08-04
---

# lessons: code-review

## 重点観点

- [L-001] 「このアサーションはトートロジーではないか / 回帰検出力があるか」が争点になったレビューでは、静的読解で議論を続けず、実装へ一時的なミューテーションを入れてテストが実際に落ちることを実測する。前提アサーションが通過し争点のアサーションだけが落ちるミューテーションを構成できれば、検出力の証明として決定的。レビュー推奨の修正案の正誤検証にも同じ手法が使える。使った一時変更は backup との shasum 一致で原状復帰を確認する。実測例は [details/mutation-probe-proves-assertion-power.md](details/mutation-probe-proves-assertion-power.md)。(昇格: 2026-08-04、出典: fix-android-cell-width-allocation / android-picker-selection-sheet / fix-picker-dialog-recreation / fix-adapter-not-restored-on-reattach / fix-decoration-theme-not-applied-on-initial-bind / fix-ios-separator-color-not-applied)

## 指摘しないこと

(まだ昇格済みルールなし)
