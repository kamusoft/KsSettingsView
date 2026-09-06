# レビュー結果: proofread-skills-wording (002 回目)

**日付**: 2026-09-05
**判定**: APPROVED

## サマリー

review-001 の 5 件 (Major 1 / Minor 3 / Suggestion 1) はすべて解消を確認した。過剰置換は修正され、custom-cells の同語反復は ja / en 6 ファイルとも修飾される側を「内容」/ "content" に逃がして解け、en の Classic / Modern 見出しは ja と同じ切り口で 3 platform に揃い、隣接見出しも ja「Modern の Section Container を調整する」/ en "Tune the Modern section box" で統一された。concepts には Container ⇔ `SectionBoxMetrics` ⇔ en "section box" の橋渡し 1 文が入り、用語規約 (新語は用語節で定義する) の要求を満たしている。

差分全体を機械的に照合した結果、598 の変更行ペアのうち置換語彙 (行/row → Cell/cell、箱 → Container、ボーダー → Border) だけでは説明できない変更は 96 ペアで、いずれも見出しの言い換えと同語反復の解消、および置換に伴う語順整理にとどまる。コードブロック内の行は 1 行も変わっておらず、公開 API 名・既定値・契約の意味の変更もない。

残るのは優先度の低い Minor 2 件 (同義語の取り残し 1 箇所、en maui の本文だけ他 2 platform より薄い) で、いずれも到達状態を損なうものではない。

## 照合した規約

- `kasane/handbook/index.md` → `cross/index.md` (作業ドメインは cross。core / ios / android / maui の domain index には `skills/**`・`kasane/concepts/**` に当たる文書がない)
- `cross/user-skill-api-listing.md` — きっかけ「`skills/` を触るとき」。**適合**: 掲載 API 名の削除・追加なし (`CustomCell rows holding any` → `CustomCell holding any` は掲載 API 名ではなく修飾語の削除で、`skills/README.md` と 3 platform の en description で同型)。除外リストにも触れていない。「コード例のコメント」節も非該当 (コード例は 1 行も変更されていない)
- `cross/comment-policy.md` — always だが対象は「コメント構文を持つ全ソースコード」。本 change は Markdown 散文のみで**非該当**
- ksn-core `references/concepts.md` (用語規約・可読性規約・timestamp = 最終検証日)、`references/doc-structure.md` (log.md は構造 lint の適用対象外)
- `kasane/lessons/code-review.md` — 重点観点 L-001 (ミューテーション実測) はコード変更のない本 change に非該当。「指摘しないこと」は昇格済みルールなし

**ビルド・テスト**: コード・テストに差分がないため実行対象なし。代わりに `scripts/local-path-lint.py` / `scripts/identity-lint.py` / `scripts/readme-example-lint.py` を実行し、いずれも指摘なし (readme-example は「README の最小例 4 件が消費者検証のソースと一致する」)。doc-structure は指揮側の報告どおり concepts 配下で新規違反なしとして受け入れた。

## 前回指摘の解消確認

| # | 前回指摘 | 状態 | 確認箇所 |
|---|---|---|---|
| 1 | 🟠 過剰置換「`PickerCell` の Cell をタップ」 | 解消 | `skills/ja/kssettingsview-maui/references/cells.md:113` が「`PickerCell` をタップすると選択面が開く」。en:113 の "Tapping a `PickerCell` opens a selection surface." と対応 |
| 2 | 🟡 custom-cells 導入文の同語反復 (6 ファイル + 見出し 1) | 解消 | ja 3 ファイル `:3` は「組み込み Cell では表せない内容のためのレシピ」、en 3 ファイル `:3` は "Recipes for content the built-in cells do not cover / cannot express"。`skills/en/kssettingsview-maui/references/custom-cells.md:49` は "Package a CustomCell as a reusable cell type" |
| 3 | 🟡 en の Classic / Modern 見出しが ja に追従していない | 解消 | en 3 platform とも `## Choose how sections are separated (Classic separators / Modern rounded boxes)` (`skills/en/kssettingsview-{maui:106,ios:161,android:177}/references/styling.md`) |
| 4 | 🟡 concepts の Container がコード語彙・en から乖離 | 解消 | `kasane/concepts/core/styling/list-appearance.md:26` に「実装の識別子およびプラットフォーム文書ではこの Container を Box と呼ぶ (`SectionBoxMetrics` 等。en の利用者向け文書では section box)。」 |
| 5 | 🔵 ja 3 platform の本文の詳しさ・隣接見出しの不揃い | 解消 | ja 3 platform とも本文に「Cell は画面の全幅に並ぶ」「Section Header / Footer はその Container の外側に置く」が入り、隣接見出しは 3 platform とも「## Modern の Section Container を調整する」 |

## 指摘事項

### [🟡 Minor・低優先] 用語統一の第 3 の同義語「枠」が、同じ change で編集した行に残っている

**該当箇所**: `skills/ja/kssettingsview-aiforms-migration/SKILL.md:25`

**問題点**: 「画面全体のスタイル (`Cell*` 既定値・Header / Footer・Cell の高さ・Section の枠) を移す」。この行は本 change で「行高さ」→「Cell の高さ」に編集されているが、同じ括弧内の「Section の枠」は手つかずで残った。指しているのは Section 装飾 4 属性 (`SectionMargin` / `SectionCornerRadius` / `SectionBorderWidth` / `SectionBorderColor`) であり、リンク先の `skills/ja/kssettingsview-aiforms-migration/references/api-mapping.md:286` は本 change で「箱」→「角丸の Container」に直っている。結果として、同じ Skill の中で同じ描画要素が「枠」(SKILL.md) と「Container」(api-mapping.md) の 2 通りで呼ばれる。確定サマリの到達状態 2 (箱・ボーダーを Container・Border に統一) の趣旨から漏れた 1 箇所で、他の「枠」(アイコン枠・入力欄の枠) は Section 装飾を指さないため本件だけが該当する。en の対応行 `skills/en/kssettingsview-aiforms-migration/SKILL.md:25` は "section borders" で Border 語彙と揃っている。

**推奨修正**: 「Section の Container」または「Section の Container と Border」に直す。

### [🟡 Minor・低優先] en maui の Classic / Modern 本文だけが他 2 platform より薄い

**該当箇所**: `skills/en/kssettingsview-maui/references/styling.md:108`

**問題点**: "`Classic` draws flat cells with separator lines; `Modern` wraps the cells of each section in a rounded box, with the header and footer outside the box." に対し、`skills/en/kssettingsview-ios/references/styling.md:163` と `skills/en/kssettingsview-android/references/styling.md:179` は "…and cells span the full width of the screen." と "with the **section** header and footer outside the box" を持つ。差は 2 点で、(a) Classic の全幅の言及が maui だけ無く、対応する ja (`skills/ja/kssettingsview-maui/references/styling.md:108`) には「Cell は画面の全幅に並ぶ」がある、(b) "section" を落とした "the header and footer" は、同じページが Root の header / footer も扱うため、どちらの Header / Footer が Container の外なのか読み取れない。確定サマリは本文について「ja / en とも 3 platform で同じ詳しさ」を到達状態として挙げており、この 1 ファイルだけ満たしていない。また "flat cells" は ja 側で「伝わりにくい」として捨てた「フラットな外観」の名残でもある。

**推奨修正**: en ios / android と同じ形に揃える (例: "`Classic` only draws separator lines between cells and sections, and cells span the full width of the screen. `Modern` wraps just the cells of each section in a rounded box, with the section header and footer outside the box.")。

## 所見 (指摘ではない)

- **concepts の「行タップ」は skills の「Cell のタップ」と揃っていない**。skills は本 change で「`PickerCell` は Cell のタップで選択画面を開く」(`skills/ja/kssettingsview-ios/references/cells.md:138` 等) に統一された一方、concepts は `kasane/concepts/core/cells/number-picker-selection-surface.md:29`・`:72`、`kasane/concepts/core/cells/picker-selection-surface.md:28`・`:78` などで「行タップ」を保っている。触っていない兄弟概念 (time-picker / date-picker / input-cells) も同語で揃っているため、2 ファイルだけ直すと concepts 内が割れる — **今回据え置いた判断は妥当**と見る。ただし確定サマリの据え置き一覧に載っていないので、次の docs-refresh / ksn-drift が漏れとして再検出しうる。記録する価値はある。
- **`kasane/concepts/log.md:384` の記述が maui-facade について実際より広い**。エントリは「あわせて Cell を指す『行』を『Cell』へ整えた」と書き、対象 5 ファイルを並べているが、`kasane/concepts/maui/api/maui-facade.md` の差分は 1 行 (箱 → Container / ボーダー → Border) だけで、同ファイルには Cell を指す「行」(`:104` 「任意の View を行の内容にする」、`:106`・`:108`・`:110`・`:111`) がそのまま残っている。log は append-only の履歴なので**訂正の必要はない**が、記述としては範囲を広く読ませる。他 4 ファイルでは「Cell 行」という重複した複合語だけを解消しており、その方針自体は一貫している (concepts 全体に「Cell 行」は 1 件も残っていない)。
- 据え置き判断は前回同様すべて妥当だった。ja に残る「行」は Grid の `*` / `Auto` 行、選択面の候補行、title と同じ行、単一行・複数行テキスト、対応表の行、「行う」「実行」の複合語のみ。en に残る "row" は grid row、candidate row、title row、選択面の 2 行目、Compose の `Row(`、`hasUnevenRows` の言い換え ("uneven rows") のみ。Cell を指す用例の取り残しはない。
- 「箱」「ボーダー」は `skills/ja/` と `kasane/concepts/` の本文から完全に消えた。残るのは `kasane/concepts/log.md:260`・`:263` の過去エントリのみで、append-only の履歴として正しく手つかず。
- コードブロックは 1 行も変わっていない (差分行のうちコード様の行は 0 件)。公開 API 名・既定値・契約の意味も不変で、`skills/README.md` と 3 platform の en description から消えたのは "rows" の修飾語だけ。
- 各 concepts ファイルの frontmatter timestamp を据え置いた判断は、`references/concepts.md` の「timestamp = 最終検証日」に整合する (コードとの再検証を伴わない語彙変更のため)。

## アクションプラン

1. `skills/ja/kssettingsview-aiforms-migration/SKILL.md:25` の「Section の枠」を Container 語彙に直す (Minor・低優先)
2. `skills/en/kssettingsview-maui/references/styling.md:108` を en ios / android と同じ詳しさに揃える (Minor・低優先)
3. concepts の「行タップ」据え置きを確定サマリの据え置き一覧に 1 行記録する (所見。次回の再検出を防ぐため)

いずれも APPROVED を妨げない。1・2 は本 change 内で直すのが自然だが、直さずアーカイブしても到達状態は成立する。
