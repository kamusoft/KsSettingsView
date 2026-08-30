# レビュー結果: datepickercell-color-adjust (003 回目)

**日付**: 2026-08-03
**判定**: APPROVED

**スコープ**: review-002 (APPROVED) 後の追加修正のみの確認。フルレビューの再実行ではない。
確認対象は `DatePickerColorizer.kt` (KDoc)・`DatePickerDialogIntegrationTest.kt` (isSelected 検証の追加)・`impl-notes.md`。

## サマリー

review-002 で残っていた低優先 Minor 2 件と、記述の一本化に関する Suggestion は**いずれも解消**している。
特に Minor 2 (「統合テストで確認した」と書かれた確認がテストとして残っていない) は、
文言を弱める方向ではなく**統合テストに観測を固定する**方向で解決されており、
review-002 の推奨のうち安い方かつ回帰検出力が上がる方を採っている。

修正はロジックに触れていない (`DatePickerColorizer` の本体コードに変更なし、追加はテスト 1 件と KDoc / ノートの文言のみ) ことを確認した。
新規の問題は見つからなかった。テストは自分で再実行して 589 件 / failures 0 / errors 0 / skipped 0 (統合テストが 6 → 7 件へ増えており、追加テストを含んだ結果であることを確認済み)。

## review-002 指摘の解消状況

| # | 指摘 (review-002) | 判定 |
|---|---|---|
| 指摘 1 | Minor (低): `MaterialIds` の KDoc が宣言する「両者の取り決め」を契約テストが満たしていない | **解消**。`DatePickerColorizer.kt:544-548` が「ここに集約した ID はすべて契約テストで検証する / 走査が依存しない ID はここへ置かない」に書き換わり、契約テスト側 KDoc (`DatePickerMaterialContractTest.kt:34-37`) の規律と同じことを言うようになった。契約テスト `:134` の `MaterialR.id.mtrl_picker_text_input_date` 直接参照が取り決め違反に読める状態は解消。さらに「テキスト入力欄の判定は ID ではなく `view is TextInputLayout` で行うため `mtrl_picker_text_input_date` はここに含めない」(`:547-548`) と、除外理由まで書かれており、review-001 H5 から続いた「KDoc の主張と実態のずれ」はこれで打ち止めになっている |
| 指摘 2 | Minor (低): 「統合テストで確認」と書かれた確認がテストとして残っていない | **解消**。`DatePickerDialogIntegrationTest.kt:188-204` に `実ダイアログの日付セルには isSelected が残らない` が追加された。KDoc (`DatePickerColorizer.kt:444-446`) と `impl-notes.md:73-75` の参照先テスト名も実物と一致する |
| 指摘 3 | Suggestion: TODAY / NORMAL の分岐だけ実ダイアログ経路の検証が無い | **未対応 (許容)**。任意・非ブロッキングの Suggestion であり、今回の修正スコープ外。再指摘はしない (下記「持ち越し」参照) |
| 指摘 4 | Suggestion: 既知の限界の記述を impl-notes 側へ一本化したい | **解消**。`impl-notes.md:96-111` が「**この節が本件の詳細の一本化先**」と明示し、Colorizer の KDoc (`:448-454`) と `ui/brief.md:76` は要点 + 参照に揃った。brief から本文を完全に消す形ではないが、brief に残る 1 文は他の 3 項目と同じ粒度の「部位の扱い」の要約で、機序・受容の経緯・回帰テスト対応はすべて impl-notes 側にある。正の所在が一意になったので、指摘の趣旨 (規範文書に実装側の但し書きが積まれる) は解消していると判断する |

## 追加確認

### (b) ロジックへの影響がないこと

- **本体コードの変更なし**: `resolveCalendarItemRole` (`DatePickerColorizer.kt:456-471`) の判定順 (`isEnabled` → `calendarItemRoles` キャッシュ → 塗り/枠の可視性) は review-002 時点と同一。KDoc が 2 行伸びたぶんの行番号シフトだけ
- **`isSelected` を読んでいないこと**: `ks-settingsview-ui` の main ソースを grep し、`DatePickerColorizer` に `isSelected` の実コード参照が 0 件であることを確認 (ヒットするのは KDoc 1 行のみ)。追加テストは「material 側が状態を保つようになったら気付く」ための観測であって、実装の依存を増やしていない
- **テスト側の追加のみ**: 統合テストの既存 6 件のアサーションは無変更。追加された 1 件も他テストと状態を共有しない (各テストが `showPicker()` で Activity から立て直す)

### (c) 新規テストの妥当性

- 検証点は `allDayCells()` (`month_grid` の全 TextView 子。無効日・非表示セルも含む) に対する `none { it.isSelected }` で、impl-notes の主張「選択日を含めて `isSelected == false`」と対象範囲が一致している
- 検証タイミングも妥当。主張は「`AbsListView.setupChild()` がレイアウト後に潰す」であり、`showPicker()` が `measure` / `layout` / `dispatchOnPreDraw` を 2 周回した後で読んでいる
- 「選択日のセルが列挙に含まれていること」を先に確かめる 2 段構え (`:195-196`) が入っており、列挙が空振りしたまま `none` が素通りする形になっていない。`dayCells()` ⊆ `allDayCells()` なので現状は構造的に常に成立するが、片方のヘルパだけが将来絞り込まれたときに落ちる番人として機能する
- 失敗時メッセージが「material 側が状態を保つようになったなら、描画からのロール判定を見直せる」と、次に踏んだ人の行動まで書いてある。KDoc・impl-notes・deviation.md の 1 件目 (「`isSelected` も `AbsListView.setupChild()` で潰れることを統合テストで実証」) の主張とも整合した

### ビルド / テスト

`:ks-settingsview-ui:testDebugUnitTest` を再実行 (BUILD SUCCESSFUL)。XML 集計で **589 件 / failures 0 / errors 0 / skipped 0**。
内訳のうち本変更の 3 クラスは `DatePickerColorizerTest` 31 / `DatePickerDialogIntegrationTest` **7** (前回 6 + 追加 1) / `DatePickerMaterialContractTest` 9。

### 足場の凍結

`specs/cell-types-input/spec.md` / `proposal.md` / `exploration.md` は無変更のまま (作業ツリーの変更一覧に現れない)。
今回変更されたのは実装 1 ファイルの KDoc・テスト 1 ファイル・`impl-notes.md` のみで、足場の逆流はない。
`deviation.md` の 2 件はオーナー承認済みの合意済み差分として扱い、違反として扱っていない。

## 指摘事項

なし (Critical 0 / Major 0 / Minor 0 / Suggestion 0)。

## 持ち越し (再指摘ではない、蒸留時の整理対象)

1. review-002 指摘 3 (Suggestion): 実ダイアログ経路では TODAY 分岐が検証されていない。material 既定の通常日スタイルが将来可視 stroke を持つようになった場合に統合テストが緑のまま通る。任意対応で、このまま進めてよい
2. review-001 H3 (Minor): 横向きでの選択日テキストの折り返し。Requirement「クリップされずに読める」は満たしており、brief に記録済み
3. review-001 H7: ADR-0006 の現行照合行が旧ファイル名 (`TimePickerColors.kt`) を指す → 蒸留時に `PickerDialogColors.kt` へ貼り直す
4. second-opinion-003 の Suggestion: 実ダイアログでの月送り・年選択の操作テスト (突き合わせで降格済み)

## verify について

デルタスペックの Scenario ↔ 実装の対応関係は verify-001.md (VALID) から**変化していない**。
今回の変更は KDoc・テスト 1 件の追加・ノートの整理のみで、Scenario への実装割り当ての増減はない。
よって **verify-003.md は作成しない** (ksn-verify の再実行は不要)。

## アクションプラン

なし。マージ可能。持ち越し項目 3 (ADR-0006 のファイル名) のみ蒸留時に処理する。
