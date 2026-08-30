# レビュー結果: timepickercell-color-adjust (002 回目)

**日付**: 2026-08-02
**判定**: APPROVED

## サマリー

修正サイクル 1 の結果を再確認した。review-001 の Major (コメント規約違反 10 箇所) は 5 ファイル全体を再走査した結果すべて解消しており、残る外部参照は許容形式の `android/ADR-0006` 5 箇所のみ。second-opinion-002 指摘 #3 (半透明アクセントのコントラスト判定) の修正は、合成ロジック・派生色の期待値・境界判定のいずれも手計算で追跡して正しいことを確認した。review-001 Minor-2 / Minor-3 のテストは推奨内容 (既定/フォーカスの枠色・塗り・キャレット・入力文字、2 回目走査の冪等性、遅延生成 View への追随) をすべて満たしている。

新たな退行は見つからなかった。残る指摘は Minor 1 件 (長命層 ADR-0006 と実装の乖離 — 蒸留フェーズの処理対象でコード修正不要) と Suggestion 2 件のみのため APPROVED とする。

### 実行した客観確認

| 項目 | 結果 |
|---|---|
| `cd android && ./gradlew test :ks-settingsview-ui:lintDebug` | BUILD SUCCESSFUL |
| テスト結果 XML の集計 | debug 634 件 / release 634 件、failures=0 errors=0 skipped=0 (前回 621 → +13) |
| lint (`lintDebug`) | 成功。`PrivateResource` 指摘なし |
| 足場凍結 | `proposal.md` / `specs/` は無変更。`tasks.md` はチェックボックスのみ、`ui/brief.md` は追記のみ — 逆流修正なし |
| deviation.md | 不在 |
| 実機証跡 | `ui/verification/` に 5 枚が存在 (前回レビューで画素値まで確認済み。今回は再修正が導出ロジックのみで実機描画に影響しないため再撮影は不要と判断) |
| diff 範囲 | `android/ks-settingsview-ui/` 配下 5 ファイル (`TimePickerCellViewHolder.kt` 変更 + 新規 4 ファイル)。他のディレクトリへの波及なし |

## 前回指摘の解消確認

### review-001 [🟠 Major] コメント規約違反 10 箇所 — **解消**

5 ファイル全文のコメントを `concepts/cross/conventions/comment-policy.md` に照らして再走査した (禁止パターン `openspec` `kasane/` `changes/` `spec.md` `brief` `mock` `proposal` `Phase [0-9]` `Decision [0-9]` `Round [0-9]` `Major-` `Minor-` `review-` `MUST` `SHALL` `SHOULD` `Scenario` `Requirement` `論点` を grep + 目視)。

- 残存する外部参照は `android/ADR-0006` 5 箇所のみで、すべて許容形式 (ドメイン付き)。裸の `ADR-0006` は消えている
- `TimePickerCellViewHolder.kt` は本 diff 以前から存在した `openspec/changes/.../spec.md` 参照 (前回「対象外」とした箇所) まで併せて除去されている
- 値の根拠を外部参照に委ねていた箇所は自己完結説明に置き換わっている。`INTERMEDIATE_SURFACE_ALPHA` / `ACCENT_TINT_ALPHA` / `SELECTED_STROKE_WIDTH_DP` はいずれも「デザイン確定値 + 上げ下げしたときに何が壊れるか」という形式で、参照なしで判断できる
- `applyStaticRole` の「部位対応表を正とする」は「下の `when` が対応そのもの / ここに現れない部位は Material 既定のまま」へ書き換わっており、推奨どおり
- 規範表現も自然な日本語 (「必須」「使わない」) に収まっており、delta spec キーワードの混入なし
- テストコード 2 ファイルにも違反なし (規約の適用範囲に含まれる)

### second-opinion-002 [🟠 Major] 半透明アクセントのコントラスト判定 — **解消 (合成ロジックは正しい)**

`TimePickerColors.kt` の新しい導出系を、実装を読むだけでなく期待値を手計算して検証した。

- `onAccent` の判定対象が `accent` から `accentSurface` (= `compositeOver(base = background, top = accent)`) に変わっており、指摘の失敗例 (白背景に `0x40000000`) は実効面 RGB ≒ (191,191,191)・相対輝度 ≒ 0.52 となって黒が選ばれる。テスト `明るい背景に載る半透明の黒アクセントには黒が選ばれる` がこれを固定している
- `blend` は `top` 固有アルファと `topAlpha` の積を重ね強度に使う形に直っており、`compositeOver` は `blend(base, top, 1.0f)` として矛盾なく定義されている。`accent` が不透明なら `accentSurface == accent` となり、既存の実機証跡が撮られた条件での挙動は変わらない (退行なし)
- テストの期待値はすべて手計算と一致した。`blend(0xFFF2EFE6, 0x80FFBF00, 1.0f)` → (248,214,114) = `0xFFF8D672`、`accentTint` (16%) → `0xFFF4E7C1`、半透明アクセントの `accentTint` (16%×50%=8%) → `0xFFF3EBD3`、`intermediateSurface` (黒 5.5%) → `0xFFE4E1D9` / 暗背景 (白 5.5%) → `0xFF1D1D1D`
- 境界判定も追跡した。`sqrt(0.05×1.05) − 0.05 ≒ 0.17913` に対し `#757575` の相対輝度 ≒ 0.1779 (白)、`#767676` ≒ 0.18113 (黒) で、テスト `判定が切り替わる境界近傍のグレー` の 117/118 の割り当ては正しい
- 誤った仕様を固定していた「アルファ非依存」テストは削除され、`不透明アクセントの onAccent は背景に依らない` (不透明時の背景独立性) に置き換わっている。これは正しい性質の固定であり、置換として妥当
- 半透明前提の扱いが `TimePickerColors` / `ColorRoles.blend` / `contrastingBlackOrWhite` / `intermediateSurface` の各 KDoc に明記され、「`base` は不透明な面色を前提とする」「背景が半透明ならその後ろは分からないので RGB をそのまま使う」という近似の境界も宣言されている。近似であること自体が読み手に伝わる形になっており妥当

### review-001 [🟡 Minor-2] キーボード入力モードのテスト — **解消**

`キーボード入力欄の枠と塗りとキャレットを着色する` / `キーボード入力欄の文字と選択ハイライトを着色する` の 2 件が追加され、`boxStrokeColor` (= フォーカス時の枠色) = accent、`boxBackgroundColor` = accentTint、`cursorColor` = accent、`EditText.currentTextColor` = text、`highlightColor` = accent を検証している。推奨した 5 項目のうち「非フォーカス時 = TRANSPARENT」だけが直接の getter を持たないため未固定 (下記 Suggestion-2 を参照)。

### review-001 [🟡 Minor-3] pre-draw 冪等再適用のテスト — **解消 (推奨以上)**

- `静的な部位は 2 回目の走査で再着色されない` (計測用 `CountingTextView` で `setTextColor` 呼び出し 0 回)
- `入力欄の枠は 2 回目の走査で再設定されない` (`CountingTextInputLayout` で `setBoxStrokeColorStateList` 0 回) — 推奨した「スパイで静的適用の 1 回性を直接固定する」まで実施されている
- `文字盤の数字は走査のたびに再着色され shader も落とし直される` (2 回目に 1 回だけ再適用され shader が null に戻る)
- `後から追加された View は次の走査で着色される` (`ViewStub` 遅延生成の代替として `TextView` + `TextInputLayout` を後から追加)

「静的 1 回 / 動的毎回」という実機計測由来の設計上の要点が回帰テストで固定された。

## 指摘事項

### [🟡 Minor] ADR-0006 の Decision 5 が実装と食い違ったまま (蒸留フェーズ向け・コード修正不要)

**該当箇所**: `kasane/decisions/android/0006-timepicker-dialog-runtime-coloring-via-view-traversal.md` (Decision 項目 5 / Consequences 末尾) ↔ `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/TimePickerColorizer.kt:324-339`

**問題点**: ADR-0006 は accepted 状態で「キャレットは API 29 の `textCursorDrawable` tint を使う (EntryCellViewHolder に実装済み前例あり)」と決めているが、実装は `TextInputLayout.cursorColor` を使っている。理由 (`TextInputLayout` が状態変化のたびに `cursorColor` で `textCursorDrawable` を塗り直すため、EditText 側の tint は上書きされる) は実機で確認され、コードコメントと `ui/brief.md` に記録済みであり、**実装側が正しい**。問題は長命層である ADR 本文が誤った機構を述べたまま残っている点で、`tasks.md 3.6` のチェック済み項目も括弧内の機構が実態と異なる。

同じく Consequences 末尾の「針の ColorFilter 描画・pre-draw のちらつき・入力欄枠の駆動 state の 3 点は机上確定のみで実機未検証」も、スパイク (tasks 1.1〜1.4) で実証済みとなり記述が古い。

これは本サイクルが持ち込んだ退行ではなく、コード側の修正も不要。ただし長命層の乖離を放置すると次に ADR-0006 を読む人が誤った機構を再実装するため、蒸留の対象として明示的に残す。

**推奨修正**: 本サイクルでは何もしない。蒸留フェーズ (ksn-distill) で ADR-0006 に実機検証の結果を反映する — Decision 5 をキャレット = `TextInputLayout.cursorColor`、枠の駆動源 = 内部 EditText のフォーカス (`state_selected` ではない) に補正し、Consequences の「机上確定のみで実機未検証」を実証済みへ更新する。`ui/brief.md` の「実機で判明した material-components の挙動」節がそのまま材料になる。

### [🔵 Suggestion] `boxStrokeCsl` のコメントが挙げる不変条件が実際の依存とずれている

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/TimePickerColorizer.kt:66-80`

**問題点**: コメントは「`TextInputLayout` は CSL から `state_focused` の色を取り出して保持する実装のため、**focused を先頭に置く**」と、配列の先頭性を守るべき条件として説明している。material 1.12.0 の `TextInputLayout.setBoxStrokeColorStateList` (`TextInputLayout.java:1178-1195`) を確認すると実際は次のとおりで、先頭性は要件ではない。

- `focusedStrokeColor = getColorForState({state_focused, state_enabled}, -1)` — 状態集合で引くため、`STATE_SELECTED` が前にあっても `{focused, enabled}` には一致せず結果は変わらない
- `defaultStrokeColor = getDefaultColor()` — `ColorStateList.getDefaultColor()` は**最後に現れる空 state spec** の色を返すため、`intArrayOf()` を末尾に置いていることが「非フォーカス時に枠を出さない」を成立させている**実際の**不変条件

つまり守るべきは「`{focused, enabled}` に一致する spec が accent を返すこと」と「空 spec が末尾にあること」であり、コメントを読んで「先頭さえ守れば良い」と判断した将来の変更が、空 spec を途中に差し込んで既定枠色を壊す余地が残る。動作は現状正しいので緊急性はない。

**推奨修正**: コメントの理由部分を実際の不変条件へ書き直す (例: 「`{state_focused, state_enabled}` に一致する spec がフォーカス時の枠色になり、末尾の空 spec が非フォーカス時の既定色になる。既定を透明にするため空 spec は末尾に置く」)。あわせて、既定枠色には公開 getter が無いためテストで直接固定できないことを踏まえると、この不変条件はコメントで支えるしかない点も込みで書けるとよい。

### [🔵 Suggestion] 文字盤ノブ上の数字だけ、`onAccent` の下地の仮定が厳密には一致しない

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/TimePickerColors.kt:32,40` / `TimePickerColorizer.kt:58,86`

**問題点**: `onAccent` は「アクセントを `background` へ重ねた面」を下地と仮定している。AM/PM トグル (`periodTextCsl`) はダイアログ背景の上に直接アクセントが載るためこの仮定と一致するが、文字盤の選択中の数字 (`clockNumberCsl`) が載るノブは、`intermediateSurface` (背景を 5.5% 暗く/明るくした文字盤の円) の上にアクセントが載る。アクセントが不透明なら両者とも `accentSurface == accent` で完全に一致するので、差が出るのは半透明アクセントかつ実効輝度が黒/白の判定境界 (相対輝度 ≒ 0.179) のごく近傍に落ちた場合に限られる。

**推奨修正**: 現状のままでよい。生じ得る差が上記の狭い条件に限られる一方、部位ごとに下地を持たせると `TimePickerColors` の「3 色だけを入力とする境界」が崩れ、割に合わない。気になるなら `onAccent` の KDoc に「下地はダイアログ背景を仮定する。文字盤のノブは中間面の上に載るため厳密には微差がある」と一行添えるだけで十分。

## 申し送り (本 diff 外・指摘ではない)

- `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/TimePickerCell.kt:11,14-15` に同型のコメント規約違反が残っている (`（Decision 1）` / `仕様: openspec/changes/add-cell-types-input/specs/cell-types-input/spec.md` / `"TimePickerCell" Requirement。`)。本 change の diff 外のため指摘対象にしていないが、`TimePickerCellViewHolder.kt` 側は今回きれいになったので、対になるファイルとして別途整える価値はある
- 回転 (Activity 再生成) 問題はオーナー決定により `kasane/changes/fix-picker-dialog-recreation/` へ切り出し済み (`exploration.md` の存在を確認)。本レビューでは指摘対象にしていない
- `ui/brief.md` の承認状態は 2026-08-02 のオーナー承認を反映済みとして扱い、指摘対象にしていない

## アクションプラン

1. 本サイクルでのコード修正は不要 (APPROVED)
2. Suggestion-1 (`boxStrokeCsl` のコメント理由の書き直し) は、着手するなら 1 行の書き換えで済む。見送っても可
3. Minor-1 (ADR-0006 の Decision 5 / Consequences の補正) を蒸留フェーズの入力として引き継ぐ
4. Suggestion-2 は対応不要
