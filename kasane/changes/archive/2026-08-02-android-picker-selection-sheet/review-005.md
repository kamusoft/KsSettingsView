# レビュー結果: android-picker-selection-sheet (005 回目)

**日付**: 2026-08-02
**判定**: APPROVED
**スコープ**: 本レビューは **review-004 / second-opinion-005 で確定した Major (リスト高制約の解除が一度きり・再制約なし) の解消確認に絞った**。過去レビューで解消済みの論点と、蒸留へ申し送り済みの Suggestion (ADR-0005 の文言2件) は再確認していない。

## サマリー

確定 Major は解消している。制約の切り替えが `STATE_DRAGGING → 解除` / `STATE_COLLAPSED → 再制約` の対称形になり、review-004 が実測した3経路 (ハンドル 80px 下ドラッグで離す / 展開してから折り目へ戻す / dismiss 閾値未満の下ドラッグ) のいずれでも、折り目へ戻った時点でシート内容高が折り目と一致し、末尾の候補が画面内に到達する状態へ復帰することを**自分の実 MotionEvent 経路でも確認した**。状態往復・スクロール位置・`onStart` 再実行との干渉についても新たな破綻は見つからなかった。残るのはコメント文言の追随漏れ 1 件 (Suggestion) のみ。

**検証した客観事実**:

- `cd android && ./gradlew test --rerun-tasks` → BUILD SUCCESSFUL。`*/build/test-results/*/TEST-*.xml` 92 ファイルの `tests` / `failures` / `errors` / `skipped` を集計し **1188 tests / 0 failures / 0 errors / 0 skipped** を確認 (テスト実行規約に従い件数まで確認)
- 足場アーティファクト (proposal / exploration / specs / ui) は提案記録コミット以降の変更なし (`git status` で未変更)。`tasks.md` の差分はチェックボックスのみで、グループ4 (4.1 視覚照合 / 4.2 実機確認) は未チェック = 虚偽チェックなし
- 下記の実測はすべて Robolectric `w411dp-h891dp-xxhdpi` (画面 2673px 高・density 3)・候補50件で、実ダイアログ階層 (CoordinatorLayout 配下) を measure / layout し、`coordinator` へ実 `MotionEvent` のドラッグ列を送って取得した (レビュー用の一時テストは計測後に削除済み)

## 確定 Major の解消確認

### 実装の対称化

`PickerSelectionSheet.kt:206-216` の `listHeightConstraintCallback` が `STATE_DRAGGING → releaseListHeightConstraint()` / `STATE_COLLAPSED → applyCollapsedListHeight()` の2分岐になり、`:194` の `collapsedListHeight` (折り目用の高さ。制約不要なら `WRAP_CONTENT`) を唯一の情報源として往復する。review-004 が併せて指摘した「`listHeightConstrained` フラグと実 `lp.height` の食い違い」は、フラグ自体が消えたことで構造的に発生しなくなった。`applySheetHeight` (`:714-726`) も `!initialStateApplied` の初回適用と、再開時に `STATE_COLLAPSED` なら制約を戻す `else if` の2経路に整理されている。`collapsedListHeight` の算出 (`:704`) はコールバック登録 (`:715`) より前にあり、初回の `STATE_COLLAPSED` 通知で未初期化値を使う順序問題もない。

### 再現経路の実測 (画面座標。screen = 2673px)

| 操作 | state | シート内容高 | リスト `lp.height` | コンテナ top | 末尾送り後の最終行下端 | 画面外の行数 |
|---|---|---|---|---|---|---|
| 初期表示 | COLLAPSED(4) | 1336 (= peek) | 1132 | 1337 | 2619 | 0 |
| **ハンドルを下へ 40px 引いて離す** | COLLAPSED(4) | 1336 | 1132 | 1337 | 2619 | 0 |
| **ハンドルを下へ 80px 引いて離す** | COLLAPSED(4) | **1336** | **1132** | 1337 | **2619** | **0** |
| **ハンドルを下へ 200px 引いて離す** | COLLAPSED(4) | 1336 | 1132 | 1337 | 2619 | 0 |
| ハンドルを下へ 400px (dismiss 到達) | HIDDEN(5) | 2673 | WRAP_CONTENT | 2673 | — (閉じている) | — |
| 上へ 600px (全展開) | EXPANDED(3) | 2673 | WRAP_CONTENT | 0 | 2619 | 0 |
| **上へ 600px → 下へ 600px (展開して折り目へ戻す)** | **COLLAPSED(4)** | **1336** | **1132** | 1337 | **2619** | **0** |

review-004 が破綻として記録した3行目・7行目のケース (シート内容高 2673・リスト下端が画面外 4010・最終行下端 3956・画面外7行) は、いずれも初期表示と同一の値へ戻る。末尾まで送った状態で最終行の下端は 2619 で、リスト下端 2673 との差 54px はリスト下端 padding (18dp) と一致する = 末尾候補は完全に画面内にある。

`dismiss` に至った経路 (5行目) だけは `WRAP_CONTENT` のまま残るが、その時点でシートは閉じており (`isShowing=false`)、コンテナも画面外 (top=2673) にあるため表示上の影響はない。`PickerCellViewHolder.showPickerSheet` は行タップごとに `PickerSelectionSheet` を新規生成する (`PickerCellViewHolder.kt:60-76`) ため、この状態が再利用されることもない。

## 新たな問題の有無 (対称化による副作用の確認)

- **状態往復での巻き戻り**: 展開 ⇄ 折り目を3周させても、各周で `EXPANDED(3) / content=2673 / containerTop=0` と `COLLAPSED(4) / content=1336 / lp.h=1132 / 末尾行下端 2619` を正確に往復し、値のドリフトはない。**折り目へ戻したあと再び上方向ドラッグで全展開できる**ことも実測 (周1・周2 とも `EXPANDED` 到達) — 再制約で `fitToContentsOffset` が折り目位置へ戻るため「一度戻すと二度と展開できない」型の後退は起きていない。既存テスト `全展開から折り目へ戻すとリスト高が可視領域制約へ戻る` (`PickerSelectionSheetTest.kt:1085-1089`) が同じ再展開をアサートしている
- **スクロール位置の飛び**: 30番へ送った状態 (`firstVisible=24`) から展開 → 折り目復帰まで `firstVisible` は 24 のまま変化しない。制約の適用/解除は先頭可視行を基準に再レイアウトされ、位置が飛ばない
- **`onStart` 再実行との干渉**: 展開中に `dismiss()` → `show()` した場合、状態は `EXPANDED` のまま・リストは `WRAP_CONTENT` のままで、意図どおり展開状態を巻き戻さない。その後にハンドルを下へ 80px 引いて離すと `COLLAPSED / lp.h=1132 / 末尾行下端 2619` へ正しく収束する。`STATE_HIDDEN` から再表示される経路も、`super.onStart()` (Material の `BottomSheetDialog.onStart` が HIDDEN を COLLAPSED へ戻す) の後に `else if (behavior.state == STATE_COLLAPSED)` が効くため制約が戻る
- **制約が不要なシート (候補3件)**: `collapsedListHeight` が `WRAP_CONTENT` のままで、上下どちらのドラッグでも高さ・コンテナ位置が一切動かない (`content=780 / containerTop=1893` 固定)。制約機構が候補少数のシートへ副作用を持ち込んでいない
- **コールバックの多重登録・リーク**: `addBottomSheetCallback` は `!initialStateApplied` の中だけで呼ばれ、1シートにつき1回。コールバックはダイアログ自身の寿命に閉じており、除去漏れによる滞留はない
- **`setListHeight` の無駄呼び**: `:521` で現在値と一致すれば何もしないため、`STATE_COLLAPSED` が繰り返し通知されても余分な `requestLayout` は起きない
- **テストの妥当性**: 新規2テストの期待値 `constrainedListHeight` は初期状態の実測値を使う自己参照だが、同じテストが `contentRoot.height <= peek` と `lastRowBottomAfterScrollToEnd(sheet) <= peek` という絶対値のアサートも持つため、初期制約ごと退行した場合 (`WRAP_CONTENT` = -2 同士の一致) でも後段で確実に落ちる。言い訳コメントによる実質スキップや境界の欠落はない

## 指摘事項

### [🔵 Suggestion] クラス KDoc の高さ挙動の説明が「解除しっぱなし」のまま残っている

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/PickerSelectionSheet.kt:106-108`

**問題点**: クラス KDoc は「上限を超える候補数では**初期表示のあいだ**リストの表示領域を可視領域へ制約し」「利用者がシートを操作した時点でこの制約は解除され、**以降は**上方向のドラッグでコンテンツの自然高まで展開できる」と書いており、今回まさに Major として直した「一度解除したら戻らない」モデルを説明したままになっている。実際の契約は「折り目表示のあいだ制約し、折り目へ戻れば再制約する」であり、`listHeightConstraintCallback` の KDoc (`:198-204`) と `applySheetHeight` の KDoc (`:681`) は正しく更新されているため、同一ファイル内で3箇所の説明が食い違っている。クラス KDoc はこの型を最初に読む人が見る位置にあり、次に高さ周りを触る人を旧モデルへ誘導しかねない。

**推奨修正**: 「初期表示のあいだ」→「折り目表示のあいだ」、「以降は」→「解除は掴んでいるあいだで、折り目へ戻れば再び制約される」の趣旨へ改める。挙動の変更を伴わないコメントのみの修正。

## 前回指摘の解消状況

| review-004 / second-opinion-005 の指摘 | 状況 |
|---|---|
| **Major: シートを一度掴むとリスト高の制約が戻らず、折り目表示で末尾の候補が到達不能** | **解消** — `STATE_DRAGGING → 解除` / `STATE_COLLAPSED → 再制約` の対称化を確認。再現3経路すべてで折り目復帰後の内容高 = peek・末尾行下端 2619 (画面内) を実測。往復3周・スクロール位置保持・再展開可否・`onStart` 再実行にも後退なし。テストも実 `MotionEvent` の2経路 (`:1023` / `:1055`) で固定されている |
| Suggestion: ADR-0005 の追補と本文の矛盾 (選択印の描画) | 未解消 (蒸留申し送り。本レビューのスコープ外につき再評価せず) |
| Suggestion: ADR-0005 の「Material の標準挙動」の記述 | 未解消 (蒸留申し送り。同上) |

## アクションプラン

1. **(任意・低優先)** クラス KDoc (`PickerSelectionSheet.kt:106-108`) の高さ挙動の説明を、対称化後の契約へ合わせる。コメントのみの修正で、実装レビューの再周回は不要
2. **(後工程)** tasks 4.1 / 4.2 の視覚照合・実機確認は未実施のまま。実機では特に (a) ハンドルを掴んで離した直後に末尾候補まで到達できること、(b) 折り目復帰時の再制約が視覚的な跳ねを起こさないこと、(c) リスト下端 18dp とジェスチャーバーの重なり、を判定項目に含める
3. **(蒸留時)** ADR-0005 の Suggestion 2件 (追補と本文の矛盾・「Material の標準挙動」の文言) を実態へ整合させる
