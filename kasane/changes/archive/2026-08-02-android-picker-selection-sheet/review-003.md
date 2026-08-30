# レビュー結果: android-picker-selection-sheet (003 回目)

**日付**: 2026-08-02
**判定**: CHANGES_REQUESTED

## サマリー

review-002 / second-opinion-003 で確定した4件はいずれも解消している。初期スクロールは「リストの内部スクロール」ではなく「リスト高を折り目へ制約する」形に組み替えられ、テストも実ダイアログ階層 (`layoutDialog`) を通した「選択行が peek 内にあるか」の判定へ差し替わった。タップ領域は左右とも `Rect(0, 0, slot.width, slot.height)` の全域委譲になり、テストは代理値ではなく実座標タップで検証している。ヘッダーは上下 padding を落として実測 48dp となり、モック相当に収まった。狭幅時の縮退も実測で機能している。

一方、今回導入された高さ制約の**解除トリガー**が広すぎる。候補リスト内の通常の上方向スクロール (10dp 相当) だけで制約が解除され、シートが折り目 (画面半分) から全画面へ1フレームで飛ぶ。ADR-0005 と brief が別々の挙動として定義している「内部スクロール」と「ドラッグで全展開」が実装上ひとつに潰れており、これを唯一の Major とする。あわせて候補行のタイポグラフィが style 解決を素通りしている点を Minor で挙げる。

**検証した客観事実**:

- `cd android && ./gradlew test --rerun-tasks` → BUILD SUCCESSFUL。`build/test-results/*/TEST-*.xml` の集計で **1176 tests / 0 failures / 0 errors / 0 skipped** (ui 876 + compose 152 + core 148) を確認 (テスト実行規約に従い件数まで確認)
- tasks.md のチェックはグループ1〜3のみ。未実施のグループ4 (視覚照合・実機) は未チェックで虚偽チェックなし
- 足場アーティファクト (proposal / specs / ui/brief / ui/mock) は提案記録コミット `3a8562e` 以降の変更なし。tasks.md はチェックボックスのみ
- deviation.md 記載の初期スクロールは合意済み差分として扱い、追加自体は指摘対象にしていない
- コメント規約: 新規・変更コメントに `openspec/` / `spec.md` / mock パス / `Phase`・`Decision` 通番 / `MUST`・`SHALL` の混入なし (grep 済み)
- 一致検証は `verify-001.md` (VALID、6 Requirement / 16 Scenario すべて対応、❌ ゼロ) に分離して記録した

## 指摘事項

### [🟠 Major] 候補リストを普通にスクロールしただけでシートが全画面へ飛ぶ

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/PickerSelectionSheet.kt:195-208` (`expandOnUserDragCallback`) / `:503-507` (`releaseListHeightConstraint`) / `:665-677` (制約の適用)、テスト `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/PickerSelectionSheetTest.kt:866`

**問題点**: 制約の解除は `BottomSheetCallback.onStateChanged` が `STATE_COLLAPSED` / `STATE_HIDDEN` 以外を受けた時点で走る。ところが `BottomSheetBehavior` は、**リスト内の上方向スクロール** (シートのドラッグではなく `RecyclerView` の nested scroll) を受けただけで `STATE_EXPANDED` を立てる。制約中はシート内容高 = 折り目のため `currentTop == expandedOffset` であり、`onNestedPreScroll` は `dy > 0` の1回目で無条件に `setStateInternal(STATE_EXPANDED)` を呼ぶ経路に入るためである。

実測 (Robolectric、`w411dp-h891dp-xxhdpi`、実ダイアログ階層を measure/layout し、`BottomSheetBehavior.onNestedPreScroll` に `dy = 30px`(10dp) を1回渡した):

| 時点 | state | シート内容高 | リスト高 | コンテナ top |
|---|---|---|---|---|
| 初期表示 | COLLAPSED(4) | 1336 (= peek) | 1132 | 1337 |
| 上方向スクロール 10dp 後 | EXPANDED(3) | 2673 | 2469 | **0** |

つまり `consumed[1] = 0` (スクロール量はリスト側が消費) のまま、シートだけが画面半分から全画面へ1フレームで飛ぶ。指に追従した展開ではなく、レイアウトパスによる瞬間移動になる。

これは brief と ADR が別々に定義している挙動を潰している:

- `ui/brief.md` の検証条件は「項目多数時はリストがシート内部でスクロールできる」と「上方向ドラッグで全展開でき」を別条件として並べている。前者は単独では観測できない
- `kasane/decisions/android/0005-pickercell-selection-ui-bottom-sheet.md:31` の「コンテンツ高で表示し、画面約半分を上限に内部スクロール。ドラッグで全展開可」も同じ2段構えを決めている

さらに、解除は一度きり (`:504` の early return) のため、リストを一度スクロールした後に折り目へ戻すと、リストは自然高のまま折り目より下へはみ出す — review-002 が Major としていた「選択項目が折り目の下に隠れる」状態に戻る。

テスト `上方向の操作でリスト高の制約が解除され全展開できる` (`:866`) は `behavior.state = STATE_EXPANDED` を直接代入しており、nested scroll 経路を1件も通していないため、この差を検出できていない。

**推奨修正**: 解除トリガーを「シート自体のドラッグ」に絞る。`onStateChanged` の解除条件を `STATE_DRAGGING` (ViewDragHelper が実際にシートを掴んだ時点で立つ。掴んだ瞬間に立つため展開の余地を作るには間に合う) に限定し、nested scroll 由来の `STATE_EXPANDED` では解除しない。`onSlide` 側の解除 (`:203-207`) は、制約中は `slideOffset > 0` になり得ないため実質デッドであり、上記に合わせて整理する。テストは `BottomSheetBehavior.onStartNestedScroll` / `onNestedPreScroll` を通した「リストスクロールでは折り目のまま」の検証を追加する。

なお、この挙動を Material の標準的な scroll-to-expand として許容する判断もあり得る。その場合は実装ではなく `ui/brief.md` の検証条件と ADR-0005 の記述を実態に合わせて確定させる必要があるため、いずれにせよオーナー判断を挟むこと。**制約 → 上方向操作で解除という方式そのものは裁定済みであり、ここで指摘しているのは解除トリガーの粒度とその実測結果のみである。**

### [🟡 Minor] 候補行の文字サイズだけスタイル解決を素通りしている

**該当箇所**: `PickerSelectionSheet.kt:798` (`ROW_TEXT_SIZE_SP = 16f`) / `:549` (適用箇所) / `:62-70` (`PickerSheetStyle.from`)

**問題点**: `PickerSheetStyle` は候補行の**文字色**と **Typeface** を `EffectiveStyle` から解決して受け取る (`:68-69`) 一方で、文字サイズだけ固定値 `16f` を使っている。`EffectiveStyle` には解決済みの `titleSizeSp` があり (`EffectiveStyle.kt:120-121`、既定 17sp)、Cell 行のタイトルはこれを使って描画される (`CellBaseLayout.kt:375`)。

結果として、

- 既定状態でも Cell 行のタイトル (17sp) と選択面の候補行 (16sp) で文字サイズが食い違う
- 利用者が `Theme.cellTitleFont` / `CellStyle.titleFont` でフォントサイズを指定すると、選択面には**書体と太さだけが反映されてサイズは反映されない**という半端な適用になる

デルタスペックは強調色の解決しか要求していないためスペック違反ではないが、同じ data class の中で3つのタイポグラフィ属性のうち2つだけを解決している状態は意図が読めない。

**推奨修正**: `PickerSheetStyle` に `itemTextSizeSp` を足して `effective.titleSizeSp` から解決し、`:549` で使う。意図的に固定値へ倒すのであれば、その理由 (選択面は Cell 行とは別の面である等) を `PickerSheetStyle` の KDoc に明記する — `sheetBackgroundColor` / `separatorColor` が Theme 由来である理由 (`:60`) は既にそう書かれているので、同じ書き方で揃う。

### [🔵 Suggestion] 文字サイズが承認モックと 1sp ずつずれている

**該当箇所**: `PickerSelectionSheet.kt:786-787` (`HEADER_TITLE_TEXT_SIZE_SP = 17f` / `HEADER_ACTION_TEXT_SIZE_SP = 15f`) / `:798` (`ROW_TEXT_SIZE_SP = 16f`) と `ui/mock/plan-b.html:46` (title 16px) / `:49` (btn-done 14px) / `:56` (item 15px)

**問題点**: 高さ寸法 (ドラッグハンドル 32×4 / margin 10・6、確定 pill の padding 6・16、リスト下端 18、行の実高さ、ヘッダー 48dp) はモックと一致しているのに、文字サイズだけ3箇所とも +1 されている (タイトル 17 vs 16、確定ラベル 15 vs 14、候補行 16 vs 15)。選択印のサイズ (30dp) は `RadioCellViewHolder.kt:78` と揃えた値であり、これは意図が読める。文字サイズ3件は根拠が読めない。

**推奨修正**: tasks 4.1 (視覚照合) の判定前に、モック値へ寄せるか「モックより 1sp 大きい」を意図として `ui/brief.md` に記録するかを決める。上記 Minor (候補行を `effective.titleSizeSp` にする) を採る場合は候補行だけ 17sp になるため、ヘッダー側の扱いと合わせて一度に決めるのが安全。

### [🔵 Suggestion] ADR-0005 本文と追補が矛盾したまま (review-002 から継続)

**該当箇所**: `kasane/decisions/android/0005-pickercell-selection-ui-bottom-sheet.md:26` (追補) と `:29` (Decision 箇条書き)

**問題点**: 追補は「専用ベクター drawable ではなく既存の `KsSimpleCheckView` を再利用する」と決めているが、`:29` は「チェックマークのベクター drawable を accentColor で tint して」のまま。今回のサイクルで追補が ADR へ書き込まれた結果、同一文書内に二重の決定が並んだ状態になっている。追補2件が Decision の箇条書きの途中 (`:24` と `:29` の間) に挟まっており、リストとしての読み筋も切れている。ADR は長命層で、この変更のアーカイブ後も残る。

**推奨修正**: 蒸留 (ksn-distill) の段で、追補の内容を Decision 本文へ織り込むか `:29` を追補と整合する表現へ改める。追補ブロックはリストの外 (Decision 節の末尾) へ移すと読みやすい。

## 前回指摘の解消状況

| 前回の指摘 (review-002 / second-opinion-003) | 状況 |
|---|---|
| 初期スクロールが実レイアウトでほぼ効かない (Major / 突き合わせ #4) | **解消** — リストの内部スクロールではなく「リスト高を折り目へ制約する」形に組み替え。実測でシート内容高 1336 = peek、リスト高 1132、選択行は可視領域内。テストも `layoutDialog` で実ダイアログ階層を通し「選択行が peek 内にあるか」を判定する形に差し替わった (8 / 16 / 50 件の3条件)。ただし解除トリガーに新たな問題 (上記 Major) |
| 実タップ領域の横方向が 48dp 未満 (Major / 突き合わせ #1) | **解消** — 委譲 Rect が `Rect(0, 0, slot.width, slot.height)` になり、取消側にも `TouchDelegate` が付いた。実測 (density 3) で `cancelSlot` / `confirmSlot` とも 144×144px = 48×48dp。テストは代理値をやめ、スロット左上 (1,1) / 右下 (w-1,h-1) への実座標タップで確定・取消の成立を検証している |
| ヘッダーが mock より約 18dp 高い (Minor / 突き合わせ #2) | **解消** — ヘッダーの上下 padding を 0 にしてスロット高 48dp をそのままヘッダー高にする方式。実測 (density 3) でヘッダー 144px = 48dp、承認モックの 49px 相当と一致。専用テスト (`:331`) が 44〜52dp の範囲で固定している |
| 小画面 + 大 font scale で対称スロットが画面幅超過 (突き合わせ #3) | **解消** — `resolveSlotMinWidths` (`:398-410`) で「対称幅が入らなければ各ラベルの固有幅へ縮退」。実測 (幅 320、長いラベル) で `cancelSlot=148` / `title=92` / `confirmSlot=48`、両ラベルとも希望幅を確保しヘッダー内 (16..304) に収まる |
| ADR-0005 本文と追補の文言矛盾 (Suggestion / 突き合わせ #5) | **未解消** — 蒸留申し送りのまま (上記 Suggestion) |
| リスト下端 18dp 固定とナビゲーションバー inset | **未対応** — tasks 4.2 の実機確認項目として継続 |

## 確認して問題がなかった観点

- **Scenario 対応**: 6 Requirement / 16 Scenario すべてに実装とテストの対応があり、今回の修正で欠落した Scenario はない (詳細は `verify-001.md`)
- **確定経路の閉包**: callback の発火点は候補行タップ (単一、`:592`) と確定ボタンタップ (複数、`:343`) の2箇所のみ。`cancel()` / `dismiss()` / `onBackPressed()` / `STATE_HIDDEN` 経由では発火しない (4経路すべてにテストあり)
- **`TouchDelegate` の再設定タイミング**: `addOnLayoutChangeListener` でレイアウトのたびに実寸から作り直しており、幅の縮退が起きても委譲範囲がずれない。`target` が GONE のときは委譲を張らないため、単一選択で確定スロットをタップしても複数選択の callback は発火しない (テストあり)
- **`TouchDelegate` の二重発火**: 委譲 Rect が対象の自領域を含む構成だが、子が直接消費した入力は `ViewGroup.dispatchTouchEvent` の段階で処理が終わり `View` 側の `touchDelegate` へ渡らない。二重に届く構成にはなっていない
- **Robolectric のテキスト測定限界と狭幅テストの代替条件**: Robolectric の既定 (legacy graphics) では文字幅がほぼ「1文字≒1px」に潰れ、`RuntimeEnvironment.setFontScale(2.0f)` を掛けても希望幅は変わらない (実測: `キャンセル` が font scale 2.0・density 1 で 14px)。狭幅テストが font scale ではなく長い文字列で幅を作っているのはこの制約に対する妥当な代替であり、縮退ロジック自体は「測った希望幅」で分岐するため代替条件でも機構を正しく突いている
- **モデル値の非正規化**: 範囲外 index は作業状態・確定集合・上限判定にそのまま残る。初期スクロールの対象からのみ範囲外を除外しており、これは表示位置の決定にしか影響しない
- **上限判定の分岐順序**: 解除 → 上限 → 追加の順 (`:596-608`) で、初期状態が上限超過でも解除が先に成立する
- **触覚フィードバック**: `hapticRequest` seam で戻り値を評価し `KEYBOARD_TAP` へフォールバック。要求が通る / 通らない両方のテストあり
- **アクセシビリティ**: 候補行は名前 (`contentDescription`) と状態 (`isCheckable` / `isChecked`) を1ノードで公開し、子 View は読み上げ対象外。ヘッダーの操作要素は `Button` クラスとして公開
- **区切り線**: `onDrawOver` で 1 物理 px。行背景に上書きされない。モックの `.list { border-top }` + 各行 `border-bottom` (最終行のみなし) と等価な線位置になる
- **旧実装の残骸**: `showPickerDialog` / `setSingleChoiceItems` / `setMultiChoiceItems` はコード・テストとも 0 件。未使用フィールド `currentCell` も削除済み
- **公開 API**: `PickerCell` のモデル・callback 契約は不変。新設クラスはすべて `internal`
- **Kotlin 言語面**: `!!` の使用なし、`workingSelection` 以外の可変状態は表示制御用のフラグ2つのみ、`when` は sealed 相当の enum を網羅、`catch (_: Throwable)` は触覚フィードバックの seam 1箇所に限定され選択の可否に影響しない
- **コメント規約**: 許容参照 (`android/ADR-0005`) のみで、禁止参照・履歴記述・spec キーワードの混入なし

## アクションプラン

1. **(必須)** 高さ制約の解除トリガーをシート自体のドラッグ (`STATE_DRAGGING`) に絞る。または「リストスクロールで全画面化する」を意図として `ui/brief.md` の検証条件と ADR-0005 に確定させる。いずれもオーナー判断を伴う。あわせて nested scroll 経路を通したテスト (リストスクロールでは折り目のまま / シートドラッグでは展開) を追加する
2. **(推奨)** 候補行の文字サイズを `effective.titleSizeSp` から解決するか、固定値である理由を `PickerSheetStyle` の KDoc に書く
3. **(推奨)** 文字サイズ3件のモック差分の扱いを、視覚照合 (4.1) の前に確定させる (2 と同時に決める)
4. **(任意)** ADR-0005 の本文と追補の矛盾は蒸留時に整合させる
5. **(後工程)** tasks 4.1 / 4.2 では、シート面 tint 後の角丸と影の見え方、リスト下端 18dp とジェスチャーバーの重なり、および 1 の挙動 (スクロール時のシート高の動き) を判定項目に含める
