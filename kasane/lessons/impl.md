---
scope: impl
timestamp: 2026-08-28
---

# lessons: impl

ソースコメント規約 (`concepts/cross/conventions/comment-policy.md`) は実装ワーカーの読み込み対象に入っておらず、規約の存在自体が届いていなかった。L-001 は書き込み前 hook (`.claude/hooks/comment-policy-check.py`) と重複するが、hook は書いた後に止めるだけで書き直しの往復が発生するため、最初から正しく書くための導線として残す。L-002 は機械判定できない類型を受け持つ。L-003 はコメント規約とは別軸で、横断修正の対象を数え切ったと判断してよい条件を扱う。L-004 は記録文書 (deviation・証跡・完了報告) に書く原因分析の裏取り条件を扱う。L-005 はレビュー指摘への応じ方 — 指摘の正しさと推奨修正コードの正しさを分けて扱う条件を扱う。

- [L-001] ソースコメントに、変更提案・レビュー文書への参照 (`kasane/changes/…` `openspec/…` `spec.md` `design.md` `review-001`)、議論の通番 (`Decision 5` `Phase 18` `Major-1` `論点 3`)、デルタスペックの語 (`MUST` `SHOULD` `Requirement` `Scenario`) を書かない。対応する ADR があれば `<domain>/ADR-NNNN` を参照し、無ければそのコメントだけで意味が通る日本語の説明に書き直す (書き換えのために新規 ADR は起票しない)。実装を終えた時点で、触ったファイルに対する `python3 scripts/comment-policy-lint.py <path>` の禁止件数が 0 になっている。(昇格: 2026-08-03、出典: fix-cell-accessory-vertical-fill / ios-picker-selection-parity / timepickercell-color-adjust / add-cell-types-custom)
- [L-002] コメントに時間軸を持ち込まない。「〜で新規追加」「全面刷新」「旧実装は〜」「〜から移植」のような経緯・過去仕様の説明は書かず、現在の仕様を現在形で書く (経緯は git 履歴の責務)。移植元 AiForms との互換仕様だけは、現在形の仕様説明として書いてよい。(昇格: 2026-08-03、出典: fix-cell-accessory-vertical-fill / ios-picker-selection-parity / timepickercell-color-adjust / add-cell-types-custom)
- [L-003] 一括置換・横断修正で対象を洗い出すときは、単一の検索式の結果を網羅と判定しない。互いに独立した原理の検索軸を2つ以上重ね (呼び出し形式・対象シンボル・API 名の語彙など)、すべてが同一の結論に収束することを確認してから件数を確定する。対象がコメント・ドキュメントの散文中の語句である場合は、行単位の grep が改行をまたげないことを軸の一つに数え、ファイル全体テキストに対し語間の改行・コメント記号を許す走査を必ず併用する。収束しなければ差分の理由を説明できるまで数えたことにしない。経緯は [details/exhaustiveness-judged-by-single-search-axis.md](details/exhaustiveness-judged-by-single-search-axis.md)。(昇格: 2026-08-11、出典: consolidate-robolectric-wait-helpers / unify-inline-looper-idle-calls / colorPrimary 宣言元参照への是正)
- [L-004] deviation.md や証跡記録に「本体 API の制約で実現できない」「環境要因でできない」と書くときは、根拠となる本体コードの該当箇所を実際に読み、ファイルパスと行を確認したうえで原因を記述する。読んでいない推測を原因として書かない。ツールチェーンのエラーを「環境要因」と断定する場合も同じ — エラーメッセージの額面を真因とせず、同一マシンで動いている構成 (兄弟プロジェクトの global.json 等のバージョン解決設定) と突き合わせてから記録する。経緯は [details/deviation-cause-written-without-reading-source.md](details/deviation-cause-written-without-reading-source.md)。(昇格: 2026-08-19、出典: align-sample-parity / add-maui-native-bridge / add-accessory-visibility-toggle)
- [L-005] レビューが推奨する具体的な修正コードは検証済みの正解ではなく仮説として扱い、採用前に周辺機構 (同値スキップ・通知経路・原典の対応挙動等) との相互作用を確認する。推奨と異なる設計を採る場合は、その理由を KDoc / deviation.md 等に残し、推奨形への改変を検出するガードテストを足す。指摘そのもの (問題の発見) の正しさと、推奨修正の正しさは独立に評価する。経緯は [details/review-recommended-fix-treated-as-hypothesis.md](details/review-recommended-fix-treated-as-hypothesis.md)。(昇格: 2026-08-28、出典: add-maui-custom-cell / fix-adapter-not-restored-on-reattach / add-maui-native-bridge / add-maui-basic-input-cells / restore-pickercell-object-items)
