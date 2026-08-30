---
scope: spec-review
kind: pain
severity: normal
count: 1
first-seen: 2026-08-03
last-seen: 2026-08-03
evidence:
  - fix-picker-dialog-recreation (ADR-0011 が position ベース tag を「別 Cell へ値を書き込む」ため却下したのに、デルタスペックの Requirement は GIVEN が「Activity 再生成後」に限定されており、同じ誤書き込みが再生成と無関係な attach/detach 経路から再来した。code-review の Major で捕捉)
---

## ルール文

ADR の Alternatives に「この failure mode が起きるから却下した」と書いた選択肢がある変更では、デルタスペックに **その failure mode を GIVEN 非依存で禁じる Requirement** (「〜は決して起きない MUST NOT」) を立てる。Scenario の GIVEN を今回の主経路 (例:「Activity 再生成後」) に絞ったままにすると、同じ failure mode が別経路から再来しても spec の網にかからず、verify も VALID のまま通過する。spec-review では「却下した理由が、選んだ設計でも別の入口から成立しないか」を1問として明示的に当てる。

## 経緯

- 2026-08-03 fix-picker-dialog-recreation: android/ADR-0011 は Fragment tag を position ベースにする案を「再生成後に別の Cell の `onValueChanged` へ値を書き込む誤対応が起きる」ため却下し、cell.id ベースを採用した。デルタスペックはこれを受けて「対応付け不能時の dismiss フォールバック」Requirement に「別の Cell への値の書き込みは、対応付けの成否によらず発生しない SHALL」と書いたが、**所属する Requirement の適用文脈が「Activity 再生成後」に限定**されていた。

  実装された `runRestoreScan()` は `FragmentManager` 上の tag が decode できる `DialogFragment` を無条件に処理対象とし、「再生成で復元された Fragment」と「今まさに表示中の生きた Fragment」を区別しなかった。結果、再生成とは無関係な経路 — 画面 A のダイアログ表示中に A が detach され、画面 B が attach + root 反映される — で、A のダイアログの確定値が B の Cell に書き込まれる状態になった。Compose DSL の既定 id が構造由来 (位置ベース) で画面間衝突しやすいため、到達性も低くない。

  デルタスペックの Scenario 対応表としては欠落ではないため verify-001 は VALID のまま通り、提案フェーズの spec-review (自己レビュー + 相方 spec-review) も検出しなかった。捕捉したのは実装後の code-review (review-001 の Major)。**却下した failure mode が、採用した設計でも別の入口から成立し得る**という観点が、spec を書く側にもレビューする側にも無かったのが原因。
