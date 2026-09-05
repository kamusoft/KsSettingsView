# レビュー結果: proofread-skills-wording (003 回目)

**日付**: 2026-09-05
**判定**: APPROVED

## サマリー

review-002 の低優先 Minor 2 件の修正のみを確認した。2 件とも指摘を解消しており、新たな意味変更・用語ずれ・ja/en の不一致は持ち込んでいない。全体は review-002 で APPROVED 済みのため再レビューは行っていない。

## 照合した規約

- `kasane/handbook/cross/user-skill-api-listing.md` — きっかけ「`skills/` を触るとき」。**適合**: 今回の 2 箇所とも掲載 API 名の追加・削除・改名はなく、修飾語と説明文の変更のみ。コード例は不変
- `kasane/handbook/cross/comment-policy.md` (always) — Markdown 散文のみのため非該当
- ksn-core `references/concepts.md` (用語規約) — 用語「Container」は `kasane/concepts/core/styling/list-appearance.md` で定義済み (実装識別子 Box / en "section box" との橋渡しも review-002 の修正で入っている)

**lint**: `scripts/local-path-lint.py` / `identity-lint.py` / `readme-example-lint.py` を実行、いずれも指摘なし。コード・テストに差分がないためビルド／テストは実行対象なし。

## 前回指摘の解消確認

| # | 前回指摘 | 状態 | 確認箇所 |
|---|---|---|---|
| 1 | 🟡 「Section の枠」が Container 語彙から取り残されている | 解消 | `skills/ja/kssettingsview-aiforms-migration/SKILL.md:25` が「画面全体のスタイル (`Cell*` 既定値・Header / Footer・Cell の高さ・Section の Container) を移す」。リンク先 `skills/ja/kssettingsview-aiforms-migration/references/api-mapping.md:286` の「角丸の Container」と同語になった。`skills/ja/` に残る「枠」はアイコン枠 3 件・入力欄の枠 1 件・スニペットの枠 1 件のみで、Section 装飾を指す用例は無い |
| 2 | 🟡 en maui の Classic / Modern 本文だけ薄い | 解消 | `skills/en/kssettingsview-maui/references/styling.md:108` が "…`Classic` only draws separator lines between cells and sections, and cells span the full width of the screen. `Modern` wraps just the cells of each section in a rounded box, with the section header and footer outside the box." (a) 全幅の言及が入り、(b) "the header and footer" → "the section header and footer" で Root 側と区別され、(c) ja で捨てた "flat rows" の名残も消えた |

## 新規指摘

なし。

確認した観点:

- **意味変更の有無**: 2 箇所とも公開 API 名・既定値・契約の意味は不変。en maui の追記内容 (Classic の全幅・Modern の Container 内包・Header / Footer は外) は `kasane/concepts/core/styling/list-appearance.md:71`・`:79`・`:92` の記述と一致し、根拠のない断定は足していない
- **ja/en の対応**: `skills/ja/kssettingsview-maui/references/styling.md:108` と語順・情報量が対応する。最終文 "Switching does not change the content or the identity of anything." は今回も未編集で、ja の「切り替えても内容と identity は変わらない」と対応 (ios は identifiers / 識別子、android は ids / ID と、platform ごとの語で揃っている)
- **3 platform の平仄**: en の分離子の語は maui / ios が "separator lines"、android が "hairlines" で、それぞれ ja の「罫線」「細線」に対応する。android だけ機械的に写したのではなく ja を正として追従している
- **過剰置換の再発**: 今回の 2 箇所ともコードブロック・識別子には触れていない

## 所見 (指摘ではない)

- `skills/en/kssettingsview-aiforms-migration/SKILL.md:25` は "section borders" のままで、ja が "Container" になったことで同じ表の行の指す範囲が ja (Container = 装飾 4 属性全体) と en (borders = Border 系 2 属性寄り) で少しずれた。ただしこの en 行は本 change で Border 語彙側に整っているものとして review-002 が既に是と判断しており、en では Container を "box" と呼ぶ据え置き方針 (確定サマリ 2) とも衝突しない。今回の修正が持ち込んだ不一致ではないため指摘にはしない
- review-002 のアクションプラン 3 (concepts の「行タップ」据え置きを確定サマリに記録) も `kasane/changes/proofread-skills-wording/summary.md` の据え置き一覧に 1 行入っており、次回の再検出リスクは下がっている

## アクションプラン

なし。アーカイブ可能。
