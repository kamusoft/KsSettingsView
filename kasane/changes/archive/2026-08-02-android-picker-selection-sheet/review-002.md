# レビュー結果: android-picker-selection-sheet (002 回目)

**日付**: 2026-08-02
**判定**: CHANGES_REQUESTED

## サマリー

前回 (review-001) と相方レビュー (second-opinion-002) で確定した指摘は、haptic の `false` 戻り値フォールバック・ヘッダーの幅配分・Button semantics・シート面の tint 化・`onStart` の状態巻き戻し・実 bind 経路のテスト・`STATE_HIDDEN` 経路のテスト・裸参照コメント・未使用フィールドまで、ほぼすべて筋のよい形で解消されている。デルタスペックの 6 Requirement / 16 Scenario の対応も維持されている。

一方、今回の修正サイクルで追加された**初期スクロール**は、実際のシートのレイアウト条件下では意図した効果を持たない。候補の総高が画面高に収まる範囲 (= 半分上限を超えるが全画面には届かない、最も一般的な件数域) では `RecyclerView` にスクロール余地がないため、選択中の項目は折り目の下に隠れたままになる。テストはリストを実在しない高さ制約 (`AT_MOST 600px`) で測っているためこの穴を検出できていない。加えて、前回の唯一の必須指摘だったタップ領域は縦 48dp は満たしたが、確定ボタンの**横**が約 33dp のままで指針を下回っている。この2点を修正対象として CHANGES_REQUESTED とする。

**検証した客観事実**:

- `cd android && ./gradlew test --rerun-tasks` → BUILD SUCCESSFUL。`build/test-results/*/TEST-*.xml` の集計で **1162 tests / 0 failures / 0 errors / 0 skipped** (ui 862 + compose 152 + core 148) を確認 (テスト実行規約に従い件数まで確認)
- tasks.md のチェックはグループ1〜3のみ。未実施のグループ4 (視覚照合・実機) は未チェックで虚偽チェックなし
- 足場アーティファクト (proposal / specs / ui/brief / ui/mock) に実装期間中の書き換えなし。tasks.md はチェックボックスのみの変更
- deviation.md 記載の初期スクロール追加は合意済み差分として扱い、追加自体は指摘対象にしていない (下記 Major は「追加された挙動が実レイアウトで成立していない」という実装上の指摘)
- コメント規約: 新規・変更コメントに `openspec/` / `spec.md` / mock パス / `Phase`・`Decision` 通番 / `MUST`・`SHALL` の混入なし (grep 済み)

## 指摘事項

### [🟠 Major] 初期スクロールが実際のシートのレイアウトではほとんど効かない

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/PickerSelectionSheet.kt:405-409` (`initialScrollPosition`) / `:416` (`scrollToPosition`) / `:565-583` (`applySheetHeight`)、テスト `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/PickerSelectionSheetTest.kt:669-726`

**問題点**: 初期スクロールは `RecyclerView` の内部スクロールとして実装されているが、シートは `RecyclerView` を**折り畳み高さ (peek) ではなく自然高 (最大で画面高) で**レイアウトする。`contentRoot` は `WRAP_CONTENT`、`design_bottom_sheet` も `wrap_content` で、折り畳み状態は「シートの上端 peek 分だけを見せる」平行移動にすぎない。したがってリストのビューポートは折り目よりずっと下まで伸びており、

- **リストの内容が自然高に収まる場合はスクロール余地が 0** で `scrollToPosition` は無効化される (`LinearLayoutManager` が `fixLayoutStartGap` で先頭へ戻す)
- スクロール余地がある場合も、送り先はリスト**ビューポートの上端**であって折り目の上ではないため、末尾付近の項目は依然として折り目の下に落ちる

実測 (Robolectric、`w411dp-h891dp-xxhdpi`、実際のダイアログ階層を measure/layout してから `findFirstVisibleItemPosition` を取得):

| 候補数 | 選択 index | peek | リスト高 | リストのスクロール範囲 | 先頭表示 | 選択項目は折り目の上か |
|---|---|---|---|---|---|---|
| 8 | 7 | 1336 | 1446 | 1392 | 0 | ✗ (行7 の上端 ≈1476 > 1336) |
| 12 | 11 | 1336 | 2142 | 2088 | 0 | ✗ |
| 16 | 15 | 1336 | 2415 | 2784 | 2 (クランプ) | ✗ |
| 50 | 30 | 1336 | 2415 | 8700 | 30 | ✓ |

つまり効くのは「候補の総高が画面高を超え、かつ選択項目が末尾付近でない」場合だけで、選択面として最も一般的な 8〜16 件程度では初期スクロールが一切効かない。deviation で合意した挙動 (選択中の項目が見える位置で開く) とクラスの KDoc の記述 (`:102`) が実態と一致していない。

なお `applySheetHeight` の `contentRoot.measure(...)` (`:569`) が `RecyclerView` の measure 経路を先に走らせ、pending scroll position をこの時点で消費する点も、この経路を分かりにくくしている。

**推奨修正**: 「リストの内部スクロール」ではなく「折り目の上に見せる」問題として解き直す。いずれも deviation の意図を満たす:

- 対象位置が折り目より下になる場合はシートを展開状態 (`STATE_EXPANDED`) で開く
- または `peekHeight` 確定後に、折り目までの可視領域を基準に `scrollToPositionWithOffset` でオフセットを与える (リストのビューポートが折り目より下まで伸びていることを前提に計算する)

**テスト側の修正も必要**: `firstVisiblePositionOf` (`:669-677`) はリストを `AT_MOST 600px` で測っており、この制約は実際のシートでは発生しない。実レイアウト (ダイアログの CoordinatorLayout を measure/layout する。`:163-171` の下スワイプテストが同じ手順を既に使っている) を通し、「選択項目の行が peek の範囲内にあること」を判定条件にすること。現行のテストは通っても実挙動を担保しない。

### [🟡 Minor] 確定ボタンの実タップ領域が横 33dp にとどまる (前回必須指摘の残り)

**該当箇所**: `PickerSelectionSheet.kt:392-397` (`delegateTouchToChild`) / `:338-355` (`confirmSlot`)、テスト `PickerSelectionSheetTest.kt:248-255`

**問題点**: `TouchDelegate` の範囲が `Rect(target.left, 0, target.right, slot.height)` で、**縦だけ**を slot 高さ (48dp) へ広げ、横は pill の実幅のままになっている。実測 (density 3、ja ロケール) では slot = 144×144px (48×48dp) に対し pill = `[46,33,144,110]` すなわち有効タップ領域は **98×144px = 約 32.7×48dp**。Android の最小タップ領域 48×48dp を横方向で下回っており、前回の必須指摘 (48dp 未満) は縦のみの解消にとどまっている。

テスト `取消と確定のタップ領域は48dp以上ある` は `confirmSlot.measuredWidth` (= 144px) を見ているため、実際の当たり判定ではなく容器の幅という代理値を検証しており、この差を検出できない。

**副次**: コード上の説明も実態とずれている。`:344` の「スロット全体（最小 48dp）を確定操作の当たり判定にする」と `delegateTouchToChild` の KDoc `:388`「[slot] の領域全体を [target] のタップ領域として委譲する」は、横方向には成り立っていない。

**推奨修正**: 委譲範囲を `Rect(0, 0, slot.width, slot.height)` にする (pill は `Gravity.END` 配置のままなので見た目は変わらず、slot の左側の余白も確定操作の当たり判定になる)。テストは `confirmSlot.touchDelegate` の実 Rect か、slot 左端付近へのタップで確定が発火することを検証する形に変える。単一選択モードでは `confirmView` が GONE で `left == right` となり委譲 Rect が空になるため、この変更後は「単一選択で slot をタップしても確定 callback が発火しない」ことも併せて確認すること。

### [🟡 Minor] ヘッダーが承認モックより約 18dp 高くなっている

**該当箇所**: `PickerSelectionSheet.kt:268` (`cancelView.minHeight`) / `:328` `:345` (slot の `minimumHeight`) / `:683-684` (ヘッダー paddings)

**問題点**: タップ領域の修正で行の実高さが 48dp になり、上下 padding 6dp + 12dp と合わせてヘッダーは約 66dp になる。承認モック (`ui/mock/plan-b.html`) のヘッダーは padding 6/12px + 中身約 31px の計 49px 相当で、実装は約 35% 高い。タスク 1.1 の「承認 mock 準拠」の判定と、これから行う視覚照合 (tasks 4.1) の基準に直接影響する。

**推奨修正**: アクセシビリティ要件 (48dp) を優先するのは妥当なので、寸法を戻すのではなく、(a) ヘッダーの上下 padding を詰めて全体高をモックに近づける、(b) モックとの差分を意図的なものとして ui/ 側の判定基準に明記する、のいずれかで扱いを確定させる。どちらもオーナー確認を伴うため、視覚照合の前に決めておくのが安全。

### [🔵 Suggestion] ADR-0005 本文と追補が矛盾したまま

**該当箇所**: `kasane/decisions/android/0005-pickercell-selection-ui-bottom-sheet.md:26` (追補) と `:29` (Decision 箇条書き)

**問題点**: 追補は「専用ベクター drawable ではなく既存の `KsSimpleCheckView` を再利用する」と決めているが、直後の箇条書き `:29` は「チェックマークのベクター drawable を accentColor で tint して」のままで、同じ文書内で決定が二重になっている。追補が箇条書きの途中 (`:24` のヘッダー項目と `:29` のリスト項目の間) に挿入されているため、リストとしての読み筋も切れている。ADR は長命層で、この変更のアーカイブ後も残る。

**推奨修正**: 蒸留 (ksn-distill) の段で、追補の内容を Decision 本文へ織り込むか、`:29` の記述を追補と整合する表現へ改める。追補ブロックはリストの外 (Decision 節の末尾) へ移すと読みやすい。

## 前回指摘の解消状況

| 前回の指摘 (review-001 / second-opinion-002) | 状況 |
|---|---|
| タップ領域 48dp 未満 (必須 / C3) | **部分解消** — 縦 48dp ✓、Button semantics ✓ (`publishAsButton` + テスト)。確定の横 33dp が残る (上記 Minor) |
| ヘッダー幅配分 (Major 昇格 / C2) | 解消 — 左右 slot を実測幅 + 48dp の対称 `minimumWidth`、タイトルを weight 1 + `ellipsize END` に変更。テストで「先に縮むのはタイトル」「左右対称」を検証 |
| シート背景の透明化 (Minor 3) | 解消 — `setBackgroundColor` を廃し `backgroundTintList` へ。Material の角丸・elevation 外形・展開時の角丸補間を保つ形になった (見え方自体の確認は 4.1) |
| haptic の `false` 戻り値フォールバック (C1) | 解消 — `hapticRequest` seam で戻り値を評価し `KEYBOARD_TAP` へフォールバック。呼ばれる / 呼ばれない両方のテストあり |
| `onStart` ごとの状態巻き戻し | 解消 — `initialStateApplied` で初回のみ `STATE_COLLAPSED` |
| 画面幅ベースの測定 | 解消 — `effectiveSheetWidth` で実測幅優先、未レイアウト時は `behavior.maxWidth` で丸め |
| RecyclerView 実 bind 経路のテスト | 解消 — `listView` を measure/layout して `getChildAt` から表示名・選択印を検証 |
| 非確定 dismiss の前提未担保 (C4) | 解消 — `isCancelableOnTouchOutside` の assert と `STATE_HIDDEN` 遷移経由の dismiss テストを追加 |
| コメントの裸参照 | 解消 — mock への参照句を削除 |
| 未使用フィールド `currentCell` | 解消 — 削除 |
| 選択印の drawable 方針 (C5) | 解消 — ADR 追補でオーナー承認済み (ただし本文との矛盾は上記 Suggestion) |
| 単一選択の初期スクロール | deviation で採用済み。実装が意図どおり効いていない (上記 Major) |
| リスト下端 18dp 固定とナビゲーションバー inset | 未対応 — tasks 4.2 の実機確認項目として残る |

## 確認して問題がなかった観点

- **Scenario 対応**: 6 Requirement / 16 Scenario すべてに実装とテストの対応があり、今回の修正で欠落した Scenario はない
- **確定経路の閉包**: callback の発火点は候補行タップ (単一) と `confirmView` タップ (複数) の2箇所のみ。`cancel()` / `dismiss()` / `STATE_HIDDEN` 経由では発火しない
- **モデル値の非正規化**: 範囲外 index は作業状態・確定集合・上限判定にそのまま残る。初期スクロールの対象からは範囲外 index を除外しており、これは表示位置の決定にのみ影響する妥当な絞り込み
- **`TouchDelegate` の重複発火**: 委譲 Rect が対象の自領域を含む構成だが、これは Android 公式が案内する当たり判定拡大の標準形であり、子が直接消費した入力が二重に届く構成にはなっていない
- **単一選択時の確定 slot**: `confirmView` が GONE のとき `left == right` で委譲 Rect が空になり、slot をタップしても複数選択の確定 callback は発火しない
- **アクセシビリティ**: 候補行は名前 (`contentDescription`) と状態 (`isSelected` / `isCheckable` + `isChecked`) を1ノードで公開し、子は読み上げ対象外。ヘッダーの操作要素は `Button` クラスとして公開 (テストあり)
- **区切り線**: `onDrawOver` で 1 物理 px。行背景に上書きされず、先頭行の上に線・最終行の下に線なしでモックと一致
- **コメント規約**: 許容参照 (`android/ADR-0005`) のみで、禁止参照・履歴記述・spec キーワードの混入なし
- **公開 API**: `PickerCell` のモデル・callback 契約は不変。新設クラスはすべて `internal`
- **足場凍結**: proposal / specs / ui は無変更。ADR への追補はオーナー承認済みの決定記録として妥当

## アクションプラン

1. **(必須)** 初期スクロールを折り目 (peek) 基準で成立させる — 展開状態で開くか、peek 確定後に `scrollToPositionWithOffset` でオフセットを与える。あわせてテストを実レイアウト経由の「選択行が peek 内にあること」の判定へ差し替える
2. **(必須)** 確定ボタンの委譲 Rect を slot 全幅へ広げ、48×48dp を実際の当たり判定で満たす。テストは代理値 (slot 幅) ではなく委譲 Rect または実タップで検証する
3. **(推奨)** ヘッダー高さがモックより約 18dp 高い件の扱いを、視覚照合 (4.1) の前に確定させる (padding で寄せる / 差分を意図として記録する)
4. **(任意)** ADR-0005 の本文と追補の矛盾は蒸留時に整合させる
5. **(後工程)** tasks 4.1 / 4.2 では、シート面の tint 後の角丸と影の見え方、リスト下端とジェスチャーバーの重なりを判定項目に含める
