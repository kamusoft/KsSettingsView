---
scope: ui-impl
timestamp: 2026-08-03
---

# lessons: ui-impl

ソースコメント規約 (`concepts/cross/conventions/comment-policy.md`) は UI 実装ワーカーの読み込み対象に入っておらず、規約の存在自体が届いていなかった。内容は `lessons/impl.md` の L-001 / L-002 と同一で、scope が開示の単位であるため両方に置く (片方を直したらもう片方も直す)。

- [L-001] ソースコメントに、変更提案・レビュー文書への参照 (`kasane/changes/…` `openspec/…` `spec.md` `design.md` `review-001`)、議論の通番 (`Decision 5` `Phase 18` `Major-1` `論点 3`)、デルタスペックの語 (`MUST` `SHOULD` `Requirement` `Scenario`)、承認モックへの参照 (`承認モック` `approved.png`) を書かない。対応する ADR があれば `<domain>/ADR-NNNN` を参照し、無ければそのコメントだけで意味が通る日本語の説明に書き直す (書き換えのために新規 ADR は起票しない)。実装を終えた時点で、触ったファイルに対する `python3 scripts/comment-policy-lint.py <path>` の禁止件数が 0 になっている。(昇格: 2026-08-03、出典: fix-cell-accessory-vertical-fill / ios-picker-selection-parity / timepickercell-color-adjust / add-cell-types-custom)
- [L-002] コメントに時間軸を持ち込まない。「〜で新規追加」「全面刷新」「旧実装は〜」「〜から移植」のような経緯・過去仕様の説明は書かず、現在の仕様を現在形で書く (経緯は git 履歴の責務)。移植元 AiForms との互換仕様だけは、現在形の仕様説明として書いてよい。(昇格: 2026-08-03、出典: fix-cell-accessory-vertical-fill / ios-picker-selection-parity / timepickercell-color-adjust / add-cell-types-custom)
