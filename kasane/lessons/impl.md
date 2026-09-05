---
scope: impl
timestamp: 2026-09-05
---

# lessons: impl

L-001 / L-002 (ソースコメント規約の写し) は 2026-09-01 に撤去した — 規約本文が `kasane/handbook/cross/comment-policy.md` (index で常時指定) としてワーカーの必読導線に載ったため。欠番のまま振り直さない (経緯は `rejected.md`)。L-003 は横断修正の対象を数え切ったと判断してよい条件を扱う。L-004 は記録文書 (deviation・証跡・完了報告) に書く原因分析の裏取り条件を扱う。L-005 はレビュー指摘への応じ方 — 指摘の正しさと推奨修正コードの正しさを分けて扱う条件を扱う。L-006 は契約変更後に旧契約を語る既存の説明文を掃く条件を扱う。

- [L-003] 一括置換・横断修正で対象を洗い出すときは、単一の検索式の結果を網羅と判定しない。互いに独立した原理の検索軸を2つ以上重ね (呼び出し形式・対象シンボル・API 名の語彙など)、すべてが同一の結論に収束することを確認してから件数を確定する。対象がコメント・ドキュメントの散文中の語句である場合は、行単位の grep が改行をまたげないことを軸の一つに数え、ファイル全体テキストに対し語間の改行・コメント記号を許す走査を必ず併用する。収束しなければ差分の理由を説明できるまで数えたことにしない。経緯は [details/exhaustiveness-judged-by-single-search-axis.md](details/exhaustiveness-judged-by-single-search-axis.md)。(昇格: 2026-08-11、出典: consolidate-robolectric-wait-helpers / unify-inline-looper-idle-calls / colorPrimary 宣言元参照への是正)
- [L-004] deviation.md や証跡記録に「本体 API の制約で実現できない」「環境要因でできない」と書くときは、根拠となる本体コードの該当箇所を実際に読み、ファイルパスと行を確認したうえで原因を記述する。読んでいない推測を原因として書かない。ツールチェーンのエラーを「環境要因」と断定する場合も同じ — エラーメッセージの額面を真因とせず、同一マシンで動いている構成 (兄弟プロジェクトの global.json 等のバージョン解決設定) と突き合わせてから記録する。経緯は [details/deviation-cause-written-without-reading-source.md](details/deviation-cause-written-without-reading-source.md)。(昇格: 2026-08-19、出典: align-sample-parity / add-maui-native-bridge / add-accessory-visibility-toggle)
- [L-005] レビューが推奨する具体的な修正コードは検証済みの正解ではなく仮説として扱い、採用前に周辺機構 (同値スキップ・通知経路・原典の対応挙動等) との相互作用を確認する。推奨と異なる設計を採る場合は、その理由を KDoc / deviation.md 等に残し、推奨形への改変を検出するガードテストを足す。指摘そのもの (問題の発見) の正しさと、推奨修正の正しさは独立に評価する。経緯は [details/review-recommended-fix-treated-as-hypothesis.md](details/review-recommended-fix-treated-as-hypothesis.md)。(昇格: 2026-08-28、出典: add-maui-custom-cell / fix-adapter-not-restored-on-reattach / add-maui-native-bridge / add-maui-basic-input-cells / restore-pickercell-object-items)
- [L-006] 観察可能な契約 (挙動・値・公開可視性) を変更する実装では、旧契約を記述する既存のコメント・doc を、変更対象の capability 外の層と他 platform を含むリポジトリ全体で契約の特徴語 (旧値・旧規則の語句・降格した型名) で grep し、現行契約へ追随させてから完了とする。grep 結果を「内部機構の説明だから残してよい」と読むときは、その行が公開メンバーの doc コメントで利用者から見えない型・値を案内していないかを判定してから残す。コンパイルもテストも旧契約の記述を検出せず、誤った契約説明は将来の設計判断を誤らせる。経緯は [details/old-contract-comments-in-other-layers-not-swept.md](details/old-contract-comments-in-other-layers-not-swept.md)。(昇格: 2026-09-05、出典: harden-update-accessory-unknown-id / adjust-section-spacing / ios-effectivestyle-visibility)
