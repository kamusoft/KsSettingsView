# Live Summary: proofread-skills-wording

## 最終状態 (何がどうなったか)

利用者向け Agent Skills (`skills/`) とその正本である concepts の用語を、次の 3 点で統一した。公開 API 名・コード例の識別子・契約の意味は変えていない。

1. **Cell の呼称**: 設定画面の 1 項目を指す「行」(ja) / "row(s)" (en) を「Cell」/ "cell(s)" に統一。Cell を指さない用例 (選択画面の候補行・title と同じ行・Grid の row・複数行テキスト・改行・「実行」等の複合語) は据え置き
2. **Section 装飾の呼称**: Section の Cell 群を囲む領域を「箱」から「Container」、その縁線を「ボーダー」から「Border」に統一 (ja と concepts)。en 側の "box" は自然な英語のため据え置き
3. **Classic / Modern の切り替え見出し**: 「フラットな外観 (Classic) と箱の外観 (Modern) を切り替える」のような伝わりにくい見出しを「Section の区切り方を切り替える (Classic の罫線 / Modern の角丸 Container)」に改め、ja の 3 platform で同形に揃えた。en も同じ切り口 "Choose how sections are separated (Classic separators / Modern rounded boxes)" に統一し、隣接見出し (ja「Modern の Section Container を調整する」/ en "Tune the Modern section box") も 3 platform で揃えた。本文も `kasane/concepts/core/styling/list-appearance.md` の記述を根拠に、Classic (罫線で区切り Cell が全幅に並ぶ) と Modern (Section の Cell を角丸 Container にまとめ Header / Footer はその外) の違いを具体化した (ja / en とも 3 platform で同じ詳しさ)

## 採用値と根拠 (却下試行の要点)

- 「Cell」への統一はユーザー指示 (ja ベースで校正、行/Cell の表記揺れを Cell に統一)。en は ja を正として追従
- 「Container / Border」の採用: ユーザー提示の候補 (Box / Container / Border) から、領域は Container・縁線は Border と役割で使い分けた。Box は ja では使わない (en の "box" は据え置き)
- concepts 側も同じ用語に揃えたのはユーザー指示 (skills と正本の語彙ずれを解消するため)。`kasane/concepts/log.md` に 2026-09-05 の用語統一エントリを追記。各ファイルの frontmatter timestamp は据え置き (契約の意味・既定値は不変で、コードとの再検証ではないため)
- 差し戻し 1 件: ja maui styling.md の言い換え文に「行」が再登場したため「Cell」に修正済み
- 据え置き (判断保留のまま確定): ja の「選択画面の候補行」「title と同じ行の末尾」「対応表の行」、en の "leaves uneven rows enabled" (`hasUnevenRows` の言い換え)。いずれも Cell を指さない、または識別子との対応を保つため
- レビュー指摘で修正: maui ja の「`PickerCell` の Cell をタップ」(過剰置換) を「`PickerCell` をタップ」に、custom-cells の導入文の同語反復 (ja / en 各 3 ファイル) と en maui の見出し "Package a CustomCell as a reusable cell type" を整理、concepts list-appearance.md に Container と実装識別子 Box (`SectionBoxMetrics`) / en "section box" の対応を 1 文追加
- 2 周目レビュー指摘で修正: ja migration SKILL.md の「Section の枠」→「Section の Container」、en maui styling.md の Classic / Modern 本文を en ios / android と同じ詳しさに
- 据え置き (concepts): picker 系概念の「行タップ」は触っていない兄弟概念と同語で揃っているため今回は据え置き (docs-refresh / drift で再検出されたら同じ理由で判断する)
- 置換でなく削除: README と 3 platform SKILL.md の description "CustomCell rows holding any" → "CustomCell holding any" (cells にすると重複。ja の「CustomCell」単独に合わせた)。ios cells.md の 1 文は重複回避で "because the cell is drawn from its latest value at draw time" に語順整理

## 触ったファイル

- `skills/ja/kssettingsview-{ios,android,maui}/SKILL.md` と `references/{cells,custom-cells,styling,updates}.md`、`skills/ja/kssettingsview-aiforms-migration/SKILL.md` と `references/api-mapping.md` (17 ファイル)
- `skills/en/` の同構成 17 ファイルと `skills/README.md`
- `kasane/concepts/core/styling/list-appearance.md`、`kasane/concepts/core/cells/number-picker-selection-surface.md`、`kasane/concepts/ios/api/ios-native-host.md`、`kasane/concepts/android/api/android-native-host.md`、`kasane/concepts/maui/api/maui-facade.md`、`kasane/concepts/log.md`

## 検証

- ドキュメントのみの変更でコード・テストは触っていない。既存 lint: `scripts/local-path-lint.py` / `identity-lint.py` / `doc-structure-lint.py` / `readme-example-lint.py` を実行 (結果は完了報告に記載。doc-structure は触った concepts 5 ファイルで HEAD と同数・新規違反なし。`kasane/concepts/log.md` は追記エントリ 1 件分だけ件数が増えるが、log.md は構造 lint の適用対象外で既存エントリと同型)

## 決定事項 / ADR 候補

- 用語統一 (Cell / Container / Border) は利用者向け文書と concepts の語彙の決定であり、ADR 候補には該当しない (覆すコストが低く、log.md の記録で足りる)
