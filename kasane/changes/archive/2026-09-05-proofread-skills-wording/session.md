# Live Session: proofread-skills-wording
対象: skills/ (利用者向け Agent Skills、en/ja) の表現校正。ユーザーが指摘する箇所を1件ずつ修正する
開始: 2026-09-05

## 試行ログ (append-only)
- 環境メモ: ワーカーへの継続送信 (SendMessage) が本セッションでは使えないため、指示ごとにワーカーを都度起動するフォールバックで進める
- ja 側で「行」と「Cell」の表記揺れを解消、Cell を指す「行」を「Cell」に統一 → skills/ja/ 配下 17 ファイル・264 行 (README_ja.md は該当なし)。「実行」「改行」「複数行のテキスト」等の非 Cell 用例は据え置き → 採用 (ユーザー確認待ち)
  - 保留 (選択画面の候補行 / Cell 内のテキスト行と読めるため未置換): ios/references/cells.md:169,271 / android/references/cells.md:335 / maui/references/cells.md:139,236 / aiforms-migration/references/api-mapping.md:3,181
  - 付随: en 側は "row" のまま (日英の語対応がずれる) / 「1 行だけ」→「Cell 1 つだけ」の見出し表現が platform 間で微妙に不揃い / ios styling.md の表で rowHeight と hasUnevenRows のラベルが同じ「Cell の高さ」になった
- 「箱」表現を廃止 (maui styling.md:106 の見出しが伝わりにくい指摘 + 他の「箱」も合わせて) → ja 側で Section の Cell 群を囲む領域は「Container」、縁線は「Border」(「ボーダー」も置換) に統一。maui styling.md:106 見出し「フラットな外観 (Classic) と箱の外観 (Modern) を切り替える」→「Section の区切り方を切り替える (Classic の罫線 / Modern の角丸 Container)」、ios:161 / android:177 の同趣旨見出しも同形に。本文は concepts/core/styling/list-appearance.md を根拠に Classic / Modern の違いを具体化 → 採用 (ユーザー確認待ち)
  - 付随: concepts/core/styling/list-appearance.md は「箱」のまま (正本側との用語ずれ、別判断) / コード例の識別子 boxedTheme は保持 / en 側の "box" は未着手
  - 差し戻し: maui styling.md:108 の新文に「行は画面の全幅に並ぶ」と「行」が再登場 → 「Cell」に直す
- maui styling.md:108「行は画面の全幅に並ぶ」→「Cell は画面の全幅に並ぶ」。ja 側に Cell を指す「行」の再登場なしを確認 → 採用
- concepts 側 (正本) も「箱」→「Container」「ボーダー」→「Border」に揃え、Cell を指す「行」も「Cell」に → core/styling/list-appearance.md (20 箇所)、ios/api/ios-native-host.md:62、android/api/android-native-host.md:122、maui/api/maui-facade.md:72、core/cells/number-picker-selection-surface.md (「行」4 箇所、同一課題として同梱)。concepts/log.md に 2026-09-05 の用語統一エントリを追記。frontmatter timestamp は据え置き (契約の意味は不変)。doc-structure-lint: 新規違反 0 (既存債務 33 件は HEAD と同数) → 採用 (ユーザー確認待ち)
  - 据え置き: log.md 過去エントリの「箱」(append-only 履歴) / 識別子 SectionBoxMetrics / 「Section Header / Footer 行」等の非 Cell 用例
- ユーザー確認: ja 側 (「行」→Cell / 「箱」→Container・Border) はこれで大丈夫 → 採用確定
- en 側で Cell を指す "row(s)" を "cell(s)" に統一 (ja が正。"box" は据え置き) → skills/en/ 17 ファイル + skills/README.md、268 箇所。Grid の row / 選択画面の候補行 (ios cells.md:169, maui cells.md:236, api-mapping.md:236) / title row (ios cells.md:271, android cells.md:335) / Compose の Row は据え置き → 採用 (ユーザー確認待ち)
  - 保留: ios custom-cells.md:58 "leaves uneven rows enabled" (hasUnevenRows の言い換えのため据え置き)
  - 置換でなく削除: README / 3 platform SKILL.md の description "CustomCell rows holding any" → "CustomCell holding any" (cells だと重複) / ios cells.md:134 語順整理 "because the cell is drawn from its latest value at draw time"
- 完了指示 → summary.md 作成。lint (local-path / identity / doc-structure / readme-example) 全通過。独立レビュー review-001.md: CHANGES_REQUESTED (Major 1 / Minor 3 / Suggestion 1)
- レビュー指摘の修正 (意味を変えないもの): maui ja cells.md:113「`PickerCell` の Cell をタップ」→「`PickerCell` をタップ」(過剰置換) / custom-cells.md:3 の同語反復を ja・en 各 3 ファイルで解消 (「組み込み Cell では表せない内容のためのレシピ」/ "Recipes for content the built-in cells…") / en maui custom-cells.md:49 見出し "Package a CustomCell as a reusable cell type" / concepts list-appearance.md:26 に Container ↔ Box (SectionBoxMetrics・en "section box") の橋渡し 1 文を追加 → 採用
- ユーザー判断待ち: (a) en 側の見出し追従 (en maui styling.md:106 "flat / boxed" の見出しと en 3 platform の不揃い) — ユーザーは en の box を据え置きと判断済みのため未対応 / (b) ja 3 platform で Classic/Modern 本文の詳しさ (maui のみ具体化) と隣接見出し「Modern の」の有無の不揃い (Suggestion)
- ユーザー判断: (a)(b) とも「揃える」→ en 3 platform の Classic/Modern 見出しを "Choose how sections are separated (Classic separators / Modern rounded boxes)" に統一 / ja ios・android の本文 (styling.md:163 / :179) と en の対応本文を maui と同じ詳しさ (Classic は罫線のみで Cell が全幅、Modern は Section の Cell を角丸 Container にまとめ Header / Footer は外側) に統一 / 隣接見出しを ja「Modern の Section Container を調整する」・en "Tune the Modern section box" で統一 → 採用
- review-002.md: APPROVED (低優先 Minor 2)。修正: ja migration SKILL.md:25「Section の枠」→「Section の Container」(リンク先 api-mapping.md:286 の 4 プロパティ一式に対応) / en maui styling.md:108 の Classic/Modern 本文を en ios・android と同じ詳しさに → 採用。concepts の「行タップ」(picker 系) は兄弟概念と同語で揃っているため据え置き
- review-003.md: APPROVED (修正 2 件の確認、新規指摘 0)。lint 全通過。change 完成
## 決定事項
- 用語: 設定画面の 1 項目は「Cell」/ "cell"、Section の Cell 群を囲む領域は「Container」(en は "box" のまま、実装識別子は Box)、縁線は「Border」
- ADR 候補なし (覆すコストが低く concepts/log.md の記録で足りる)
## エスカレーション・スコープ外の発見
