# レビュー結果: proofread-skills-wording (001 回目)

**日付**: 2026-09-05
**判定**: CHANGES_REQUESTED

## サマリー

41 ファイルの用語統一は、置換の網羅性・据え置き判断のどちらも概ね正確だった。Cell を指さない用例 (Grid の `*` 行 / `Auto` 行、選択面の候補行、title と同じ行、複数行テキスト、Compose の `Row`、`hasUnevenRows` の言い換え、「行う」「実行」の複合語) はすべて残っており、逆に取り残された「行」/"row" も見当たらない。「箱」「ボーダー」は skills/ja と concepts から完全に消え、公開 API 名・既定値・コード例・契約の意味は変わっていない。

一方で、機械置換の副作用として**過剰置換が 1 件**混入し (`PickerCell` の Cell をタップ)、**同語反復で読みにくくなった導入文が 6 ファイル**残っている。用語統一そのものが本 change の成果物である以上、これらは成果物内の欠陥として扱う。加えて、ja だけで実施した見出しの言い換えにより **ja と en の見出し対応が崩れている** (en maui には ja で「伝わりにくい」と判断した表現がそのまま残る) 点と、concepts の新語 **Container がコード識別子 `SectionBoxMetrics` および en の "box" から橋渡しなしに乖離した**点を指摘する。

## 照合した規約

- `kasane/handbook/index.md` → `cross/index.md` (作業ドメインは cross。core / ios / android / maui の domain index は本 change の触る範囲 (`skills/**`・`kasane/concepts/**`) に当たる文書を持たない)
- `cross/user-skill-api-listing.md` — きっかけ「`skills/` を触るとき」。**適合**: 公開 API 名の削除・追加はなく (`CustomCell rows` → `CustomCell` は掲載 API 名ではなく修飾語の削除)、除外リストにも触れていない。skills/ 配下のコード例は本 change で変更されていない
- `cross/comment-policy.md` — always だが対象は「コメント構文を持つ全ソースコード」。本 change は Markdown 散文のみで**非該当**
- ksn-core `references/concepts.md` (用語規約・可読性規約)、`references/doc-structure.md` (log.md は構造 lint の適用対象外) — 指摘 4 / 所見 1 の根拠
- `kasane/lessons/code-review.md` — 重点観点 L-001 (ミューテーション実測) はコード変更を伴わない本 change に非該当。「指摘しないこと」は昇格済みルールなし

**ビルド・テスト**: ドキュメントのみの変更でコード・テストに差分がないため実行対象なし。代わりに `scripts/doc-structure-lint.py` を HEAD と作業ツリーで比較実行した (結果は所見 1)。

## 指摘事項

### [🟠 Major] 過剰置換で意味の壊れた文が 1 件残っている

**該当箇所**: `skills/ja/kssettingsview-maui/references/cells.md:113`

**問題点**: 「`PickerCell` の Cell をタップすると選択面が開く」となっている。`PickerCell` 自身が Cell なので「PickerCell が持つ Cell」を指すかのように読め、置換前の「`PickerCell` の行をタップすると」より意味が悪化している。同じ文の他 platform 版は正しく直っており (`skills/ja/kssettingsview-ios/references/cells.md` の「`PickerCell` は Cell のタップで選択画面を開く」、`skills/ja/kssettingsview-android/references/cells.md` の「`PickerCell` は Cell のタップでボトムシートを開く」)、en 版も `skills/en/kssettingsview-maui/references/cells.md:113` が "Tapping a `PickerCell` opens a selection surface." と正しい。ja maui だけが取り残された形で、確定サマリの「意味を変えていない」という到達状態から外れている。

**推奨修正**: 「`PickerCell` をタップすると選択面が開く」または他 platform に揃えて「`PickerCell` は Cell のタップで選択面を開く」とする。

### [🟡 Minor] 機械置換で導入文が同語反復になった (ja / en 各 3 ファイル)

**該当箇所**:
- `skills/ja/kssettingsview-ios/references/custom-cells.md:3` — 「組み込み Cell で足りない Cell のためのレシピ。まず `CustomCell` を試し、共通 Cell レイアウトと style 解決に参加する Cell が必要なときだけ独自 Cell 型を定義する。」
- `skills/ja/kssettingsview-android/references/custom-cells.md:3` — 同型
- `skills/ja/kssettingsview-maui/references/custom-cells.md:3` — 「組み込み Cell では表せない Cell のためのレシピ。」
- `skills/en/kssettingsview-ios/references/custom-cells.md:3` — "Recipes for cells the built-in cells do not cover."
- `skills/en/kssettingsview-android/references/custom-cells.md:3` — 同型
- `skills/en/kssettingsview-maui/references/custom-cells.md:3` — "Recipes for cells the built-in cells cannot express."
- `skills/en/kssettingsview-maui/references/custom-cells.md:49` — 見出し "## Package a cell as a reusable cell type"

**問題点**: 置換前は「組み込み Cell で足りない**行**のための」「Recipes for **rows** the built-in cells do not cover」で、修飾する側 (Cell 種別) と修飾される側 (画面上の項目) が語で分かれていた。両方を Cell にしたため「Cell で足りない Cell」「cells the built-in cells do not cover」という自己言及になり、1 文に Cell が 4 回現れるファイルもある。skills は利用者と利用者のエージェントが最初に読む導入文であり、可読性の劣化は本 change の目的 (伝わりやすくする) と逆向きになる。

**推奨修正**: 修飾される側を Cell 以外の語に逃がす。ja は「組み込み Cell で表せない内容のためのレシピ」「組み込み Cell が用意していない見た目のためのレシピ」等、en は "Recipes for what the built-in cells do not cover" 等。見出し `:49` は "Package a CustomCell as a reusable cell type" のように主語を具体化すると重複が消える。

### [🟡 Minor] 見出しの言い換えが ja だけに入り、en との対応が崩れた

**該当箇所**:
- `skills/en/kssettingsview-maui/references/styling.md:106` — "## Switch between the flat (Classic) and the boxed (Modern) appearance"
- `skills/en/kssettingsview-ios/references/styling.md:161` / `skills/en/kssettingsview-android/references/styling.md:177` — "## Switch between Classic and Modern list appearance"
- 対応する ja: `skills/ja/kssettingsview-maui/references/styling.md:106` / `skills/ja/kssettingsview-ios/references/styling.md:161` / `skills/ja/kssettingsview-android/references/styling.md:177` — いずれも「## Section の区切り方を切り替える (Classic の罫線 / Modern の角丸 Container)」

**問題点**: ja は 3 platform で同形に揃った一方、en は手つかずで、(a) ja と en が同じ節を別の切り口で名乗る状態になり、(b) en 側の 3 platform 不揃い (maui だけ "flat / boxed") がそのまま残った。特に en maui の "the flat (Classic) and the boxed (Modern) appearance" は、ja で「伝わりにくい」と判断して置き換えた「フラットな外観 (Classic) と箱の外観 (Modern)」の直訳そのもので、同じ欠点が en に残っている。確定サマリは「en は ja を正として追従」を採用方針として挙げているため、この節だけ追従していないのは方針との不整合でもある。

**推奨修正**: en の 3 見出しを ja と同じ切り口に揃える (例: "## Choose how sections are separated (Classic rules / Modern rounded boxes)")。en 本文の "box" 据え置きは合意済みなので、見出しでも box 語彙を使ってよい。en を触らない判断をするなら、その理由をサマリの据え置き一覧に明記して、次回の docs-refresh が漏れとして再検出しないようにする。

### [🟡 Minor] concepts の「Container」がコード語彙と en から橋渡しなしに乖離している

**該当箇所**: `kasane/concepts/core/styling/list-appearance.md:24` (見出し「## Modern の Section Container」)、`kasane/concepts/core/styling/list-appearance.md:65` (同じ節が `SectionBoxMetrics` を参照)

**問題点**: ksn-core `references/concepts.md` の用語規約は「概念名・見出し・本文の用語はコードと既存文書の語彙をそのまま使う。concepts 側で新しい訳語・言い換え語を発明しない」と定める。Border は実装側の `sectionBorderWidth` / `sectionBorderColor` と一致するので問題ないが、Container は実装側の語彙が Box (`SectionBoxMetrics`、iOS の `SectionBoxDecorationTests` 等) であり、en skills も "section box" のままである。結果として同一の描画要素が「Container (concepts / ja skills)」「box (en skills)」「Box (コード)」の 3 通りで呼ばれる。`:65` は同じ節の中で `SectionBoxMetrics` を挙げているのに、その Box と見出しの Container が同じものだという説明がなく、コードを grep する読者が接地に失敗する。用語の採用自体はユーザー確定事項なので覆すべきものではないが、橋渡しが要る。

**推奨修正**: `list-appearance.md` の「Modern の Section Container」節の冒頭か「用語」相当の位置に 1 行足す (例: 「Container は実装では `SectionBoxMetrics` / `SectionBoxDecoration*` の Box、en skills では section box と呼ぶ同じものを指す」)。これで新語の導入と用語規約の要求 (新語は用語節で定義する) が両立する。

### [🔵 Suggestion] ja 3 platform で本文の詳しさと隣接見出しに差が残っている

**該当箇所**: `skills/ja/kssettingsview-maui/references/styling.md:108`・`:118` / `skills/ja/kssettingsview-ios/references/styling.md:163`・`:174` / `skills/ja/kssettingsview-android/references/styling.md:179`・`:189`

**問題点**: 見出しは 3 platform で同形になったが、直後の本文は maui だけが「`Classic` は Cell と Section の境界を罫線で引くだけで、Cell は画面の全幅に並ぶ。`Modern` は … Section Header / Footer はその Container の外側に置く」と具体化され、ios / android は「区切り / まとめる」の一文にとどまる。また隣の節見出しが maui だけ「## Section の Container を調整する」で、ios / android は「## Modern の Section Container を調整する」と `Modern の` の有無が割れている (置換前からの差がそのまま残った形)。

**推奨修正**: 必須ではない。揃えるなら ios / android の本文にも全幅・Header / Footer の位置を 1 句足し、隣接見出しを「## Modern の Section Container を調整する」に統一する。

## 所見 (指摘ではない)

- 確定サマリの検証欄にある「doc-structure の concepts 配下の違反数は HEAD と同数で新規違反なし」は、実測では `kasane/concepts/log.md` が 158 件 → 159 件で +1 だった (`kasane/concepts/log.md:382` 以降の追記エントリが 1 項目 200 字超のため)。ただし ksn-core `references/doc-structure.md` の適用対象表は `log.md` を対象外 (履歴は項目の列挙が正しい形) と定めており、既存 158 件も同型なので**内容の是正は不要**。訂正が要るのは報告の記述だけで、実害はない。他の 5 ファイルは HEAD と同数 (list-appearance 0 / number-picker 1 / android-native-host 2 / maui-facade 30 / ios-native-host 0) で新規違反なし。
- `kasane/concepts/log.md` の追記は形式 (日付見出し + `- updated:` 接頭辞、末尾追記、既存 79 件の `updated:` 用例) に沿っており、重複した日付見出しもない。timestamp 据え置きの判断 (契約の意味・既定値が不変でコードとの再検証ではない) も `references/concepts.md` の「timestamp = 最終検証日」に整合する。
- 据え置き判断はすべて妥当だった。en に残る "row" は Grid の row / `Auto` row / Compose の `Row(` / 候補行 / title row / 選択面の 2 行目 / `hasUnevenRows` の言い換えのみ、ja に残る「行」は Grid の `*` 行 / 候補行 / title と同じ行 / 単一行・複数行テキスト / 対応表の行 / 「行う」「実行」の複合語のみで、Cell を指す用例の取り残しはない。
- `skills/README.md` と 3 platform の en description の "CustomCell rows holding any …" → "CustomCell holding any …" は英文として自然で、ja description の「CustomCell」単独とも揃っている。
- concepts の意味変更は起きていない。`android-native-host.md:122` の「Section H/F 行は Container の外に置く」、`list-appearance.md:67` の「Section Header / Footer 行にも適用する」のように Cell を指さない「行」を残した判断も一貫している。

## アクションプラン

1. `skills/ja/kssettingsview-maui/references/cells.md:113` の過剰置換を直す (Major)
2. custom-cells の導入文 6 ファイルと en maui の見出し 1 件から同語反復を解く (Minor)
3. en の Classic / Modern 見出し 3 件を ja に追従させる。追従しないなら据え置き理由をサマリへ記録する (Minor)
4. `kasane/concepts/core/styling/list-appearance.md` に Container ⇔ `SectionBoxMetrics` / en "box" の橋渡しを 1 行足す (Minor)
5. 余力があれば ja 3 platform の本文の詳しさと隣接見出しを揃える (Suggestion)
