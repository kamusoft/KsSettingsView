# レビュー結果: android-picker-selection-sheet (004 回目)

**日付**: 2026-08-02
**判定**: CHANGES_REQUESTED

## サマリー

review-003 の2件はいずれも解消している。全展開のトリガーは `STATE_DRAGGING` へ絞られ、あわせて候補リストが nested scroll の開始と fling 伝播を返さない `SelfContainedRecyclerView` になったことで、リストの内部スクロールでシートが動く経路は塞がれた。候補行の文字サイズも `EffectiveStyle.titleSizeSp` から解決され、専用テスト2件で固定されている。

一方、**制約の解除が一度きりで再制約されない**点が残っており、これが今回の Major になる。解除トリガーが狭くなったことで「リストを普通にスクロールして誤発火する」経路は消えたが、シートを掴む操作自体は上下どちらでも解除を起こすため、**ドラッグハンドルを 27dp 下へ引いて離す (折り目へ戻る) だけで、シート内容高 (2673px) が折り目 (1336px) を超えたまま固定される**。この状態ではリストの下半分が画面外へ描かれ、末尾までスクロールしても最後の7行が画面内に来ない = 選択できない。deviation に記録された合意 (トリガーの限定・リスト面下スワイプ dismiss の喪失) はこの状態を含んでいないため、合意済み差分としては扱わなかった。

**検証した客観事実**:

- `cd android && ./gradlew test --rerun-tasks` → BUILD SUCCESSFUL。`*/build/test-results/*/TEST-*.xml` の `tests` / `failures` 属性を集計し **1182 tests / 0 failures / 0 errors / 0 skipped** (ui 441×2 + compose 76×2 + core 74×2) を確認 (テスト実行規約に従い件数まで確認)
- 足場アーティファクト (proposal / specs / ui/brief / ui/mock) は提案記録コミット `3a8562e` 以降の変更なし。`tasks.md` の差分はチェックボックスのみ、グループ4 (視覚照合・実機) は未チェックで虚偽チェックなし
- コメント規約: 新規・変更コメントに `openspec/` / `spec.md` / mock パス / `Phase`・`Decision`・`Round` 通番 / `MUST`・`SHALL`・`SHOULD` の混入なし (grep 済み)。許容参照 `android/ADR-0005` のみ
- 今回の差分は「展開トリガー」と「候補行の文字サイズ」に限定され、Requirement / Scenario の対応関係を動かしていない (`verify-001.md` の対応表は有効なまま)
- 下記の実測はすべて Robolectric `w411dp-h891dp-xxhdpi` (画面 2673px 高・density 3)・候補50件で、実ダイアログ階層 (CoordinatorLayout 配下) を measure / layout し、`coordinator` へ実 `MotionEvent` のドラッグ列を送って取得した (検証用の一時テストは計測後に削除済み)

## 指摘事項

### [🟠 Major] シートを一度掴むとリスト高の制約が戻らず、折り目表示で末尾の候補が到達不能になる

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/PickerSelectionSheet.kt:527-531` (`releaseListHeightConstraint` の一度きり解除) / `:201-209` (`expandOnUserDragCallback`) / `:689-701` (制約の適用が `!initialStateApplied` の中にしかない)

**問題点**: 解除は `STATE_DRAGGING` で走るが、`STATE_DRAGGING` は**上方向の展開ドラッグだけでなく、シートを掴む操作すべて**で立つ。`peekHeight` は初回 `applySheetHeight` で `1336` に固定される一方、解除後のシート内容は自然高 (`2673`) のままになるため、折り目に戻ったシートは内容が折り目より `1337px` はみ出した状態で固定される。リストは `RecyclerView` の内部スクロール範囲を保つが、**ビューポート自体が画面外まで伸びている**ため、末尾までスクロールしても最終行はビューポート下端 = 画面外に留まる。

実測 (候補50件・行高 174px):

| 操作 | state | シート内容高 | リスト `lp.height` | コンテナ top | リスト下端 (画面座標) |
|---|---|---|---|---|---|
| 初期表示 | COLLAPSED(4) | 1336 (= peek) | 1132 | 1337 | 2673 (画面下端と一致) |
| ハンドルを上へ 40px ドラッグ | COLLAPSED(4) | 1336 | 1132 | 1337 | 2673 (解除されない = 正しい) |
| **ハンドルを下へ 80px 引いて離す** | **COLLAPSED(4)** | **2673** | **WRAP_CONTENT** | 1337 | **4010** (画面外へ 1337px) |
| ハンドルを上へ 80px 以上ドラッグ | EXPANDED(3) | 2673 | WRAP_CONTENT | 0 | 2673 |
| 上へ 600px → 下へ 600px (展開して折り目へ戻す) | COLLAPSED(4) | 2673 | WRAP_CONTENT | 1337 | 4010 |

破綻状態 (上表の3行目・5行目) で末尾までスクロールした結果:

- 最終行 (index 49) の下端 = 画面座標 **3956** (画面高 2673) → **画面外**
- レイアウト済み14行のうち **7行が画面下端より完全に下**
- `scrollOffset=6285 / range=8700` で、リストはスクロール上限に達している (これ以上送れない)

つまり折り目表示のまま最後の約7候補を**選択できない**。ハンドルを下へ 27dp 引いて離すという、dismiss しようとして途中でやめる程度の操作で入る状態であり、視覚的な手掛かりもない (シートの下端は画面下端と一致して見える)。復帰にはもう一度上へドラッグして全展開する必要がある。

これは deviation.md の合意 (全展開トリガーをシート直接ドラッグに限定・その帰結としてリスト面からの下スワイプ dismiss を失う) の射程外である。合意は「いつ展開するか」を定めたもので、「折り目に戻ったときリストが折り目内に収まること」を放棄してはいない。初期表示でその不変条件を満たすために導入されたのが今回の制約機構であり、解除が非対称なことでその不変条件が事後に壊れている。

**推奨修正**: 解除と再制約を対称にする。`applySheetHeight` で算出した可視領域高 (`visibleListHeight`) をフィールドに保持し、`onStateChanged` が `STATE_COLLAPSED` を受けた時点で `setListHeight(visibleListHeight)` に戻す (`releaseListHeightConstraint` の一度きり early return を落とす)。折り目より下の不可視領域での伸縮なので、見えるジャンプにはならない。あわせて `listHeightConstrained` フラグと実際の `lp.height` が食い違う経路も畳める — 現状 `:679` の `setListHeight(WRAP_CONTENT)` は `initialStateApplied` の分岐の外にあり、`onStart` が再度走るとフラグが `true` のまま実高だけ制約なしになる。

テストは「ハンドルを下へ小さく引いて離す」「上へ展開してから折り目へ戻す」の2経路で、(1) シート内容高 ≦ `peekHeight`、(2) 末尾までスクロールしたとき最終行が可視領域 (画面内) に入ること、を固定するとよい。現在のテストは折り目→展開の一方向しか通っていないため、この差を検出できていない。

### [🔵 Suggestion] ADR-0005 の本文と追補が矛盾したまま (review-002 から継続)

**該当箇所**: `kasane/decisions/android/0005-pickercell-selection-ui-bottom-sheet.md:26` (追補) と `:29` (Decision 箇条書き)

**問題点**: 追補は「専用ベクター drawable ではなく既存の `KsSimpleCheckView` を再利用する」と決めているが、箇条書き `:29` は「チェックマークのベクター drawable を accentColor で tint して」のまま残っており、同一文書内に二重の決定が並んでいる。追補2件が Decision の箇条書きの途中 (`:24` と `:29` の間) に挟まっている点も読み筋を切っている。ADR は長命層で、この変更のアーカイブ後も残る。

**推奨修正**: 蒸留 (ksn-distill) の段で追補の内容を Decision 本文へ織り込むか `:29` を追補と整合する表現へ改める。追補ブロックは Decision 節の末尾へ移すと読みやすい。

### [🔵 Suggestion] ADR-0005 の高さ挙動の記述が実装の方針と食い違う

**該当箇所**: `kasane/decisions/android/0005-pickercell-selection-ui-bottom-sheet.md:31` (「高さ: コンテンツ高で表示し、画面約半分を上限に内部スクロール。ドラッグで全展開可 (Material の標準挙動)」)

**問題点**: 実装は Material 標準の scroll-to-expand (リストの nested scroll でシートが展開する) を**意図的に採らない**方針へ確定し (deviation.md の2件目、オーナー裁定)、そのために `SelfContainedRecyclerView` で nested scroll の開始と fling 伝播を遮断している。ADR の「(Material の標準挙動)」という括弧書きは、この確定した方針と逆の読みを残す。deviation.md は変更と一緒にアーカイブされるため、この裁定を長命層に残すには ADR 側の記述が正になる。

**推奨修正**: 蒸留の段で、`:31` を「全展開はシート面の直接ドラッグに限り、候補リストのスクロールは常に内部で完結する (Material 標準の scroll-to-expand は採らない)」の趣旨へ改める。上の Suggestion と同じ作業で片付く。

## 前回指摘の解消状況

| review-003 / second-opinion-004 の指摘 | 状況 |
|---|---|
| Major: 候補リストの上方向スクロール1回でシートが全画面へ瞬間移動 | **解消** — 解除トリガーが `STATE_DRAGGING` に限定され、さらに `SelfContainedRecyclerView` (`:422-434`) が `startNestedScroll` / `dispatchNestedPreFling` / `dispatchNestedFling` を返さないため、`BottomSheetBehavior.onNestedPreScroll` 経路そのものが成立しない。実測でリスト面を 400px 上へドラッグしても `STATE_COLLAPSED` のまま・シート内容高 = peek・リスト `lp.height` 不変で、リストだけが内部スクロールする (テスト `候補リストのスクロールではシートを展開しない` が実 `MotionEvent` で固定)。ハンドル起点では 80px 以上のドラッグで `STATE_EXPANDED` に達する |
| Minor: 候補行の文字サイズが `EffectiveStyle` を素通りして固定 16f | **解消** — `PickerSheetStyle.itemTextSizeSp` (`:54`) を追加して `effective.titleSizeSp` から解決 (`:74`)、候補行に適用 (`:573`)。KDoc も「候補行のタイポグラフィは Cell 行のタイトルと同じ実効値を使う」と意図を明記。テストは `Theme(cellTitleFontSize = 22.0)` の反映と「Cell 行タイトルの `textSize` と一致」の2本 |
| Suggestion: 文字サイズがモックと 1sp ずつずれている | **一部反映** — ヘッダーはモック値と一致 (タイトル 16sp / 取消 15sp / 確定 14sp = mock の 16px / 15px / 14px)。候補行はモックの 15px ではなく Cell タイトルの実効値 (既定 17sp) を優先する判断が申し送り済みで、視覚照合 (4.1) の確認対象 |
| Suggestion: ADR-0005 本文と追補の文言矛盾 | **未解消** — 蒸留申し送りのまま (上記 Suggestion) |
| リスト下端 18dp 固定とナビゲーションバー inset | **未対応** — tasks 4.2 の実機確認項目として継続 |

## 確認して問題がなかった観点

- **nested scroll 遮断方式の妥当性**: `isNestedScrollingEnabled` を `true` のまま保つ判断は正しい。`BottomSheetBehavior.findScrollingChild` はこのフラグで「スクロールする子」を認識し、その子の領域で始まったドラッグをシートが横取りしないよう働くため、`false` にするとリスト上のドラッグがシートのドラッグになる。実測でも、リスト面のドラッグはシートを動かさずリストの内部スクロールになり (`computeVerticalScrollOffset > 0`、state は COLLAPSED のまま)、ハンドル/ヘッダー起点のドラッグは従来どおりシートを動かす。`startNestedScroll` が常に `false` を返すことで nested scroll の親が解決されず、`dispatchNestedPreScroll` も自動的に無効化される (テストが両方 `false` を実測)
- **解除トリガーの誤発火余地**: ハンドルを 40px (13dp) 引いた程度ではタッチスロープに届かず解除は起きない。解除は実際にシートを掴んだ操作でのみ発生しており、トリガーの粒度自体は裁定どおり (問題は解除後に戻らないこと)
- **展開後の機能維持**: 全展開中もヘッダーは表示され続け (`headerView.bottom <= contentRoot.height`)、確定・取消とも成立する。展開でリストのスクロール位置 (選択行) が飛ばない
- **Scenario 対応**: 今回の差分は展開トリガーと文字サイズに限定され、6 Requirement / 16 Scenario の対応を変えていない。callback の発火点は候補行タップ (単一) と確定ボタン (複数) の2箇所のみで、`cancel()` / `dismiss()` / `onBackPressed()` / `STATE_HIDDEN` の4経路すべてに不発火テストがある
- **`PickerCellViewHolder` の差し替え**: `AlertDialog` 経路は残骸なし。`isEnabled = false` では `setOnClickListener(null)` + `isClickable = false` で提示されず、`reset()` も同じ初期状態へ戻す。公開 API (`PickerCell` のモデル・callback) は不変で、新設クラスはすべて `internal`
- **`InputCellsTest` の差し替え**: `ShadowAlertDialog` → `ShadowDialog` + `is PickerSelectionSheet` の型検査で、提示経路の検証としてむしろ具体的になっている。選択値の検証は専用テストファイル側が担う
- **Kotlin 言語面**: `!!` なし、可変状態は `workingSelection` と表示制御フラグ2つのみ、`when` は enum を網羅、`catch (_: Throwable)` は触覚フィードバックの seam 1箇所に限定
- **リスト面からの下スワイプ dismiss の喪失**: deviation 記録済みの合意事項として指摘対象にしていない。ただし候補が少ないシートではドラッグ可能領域がハンドル + ヘッダー (約 68dp) だけになるため、実機確認 (4.2) の判定項目に入れておくと安全

## アクションプラン

1. **(必須)** リスト高の制約を「解除しっぱなし」から「折り目へ戻ったら再制約」へ対称化する。あわせて `listHeightConstrained` フラグと実 `lp.height` の一貫性を保つ。テストは「下へ小さく引いて離す」「展開してから折り目へ戻す」の2経路で、シート内容高 ≦ `peekHeight` と最終行の到達可能性を固定する
2. **(後工程)** tasks 4.1 / 4.2 では、(a) 1 の挙動を実機で確認 (ハンドルを掴んで離した後にリスト末尾へ到達できるか)、(b) 候補行 17sp とヘッダータイトル 16sp の大小関係を含む視覚照合、(c) シート面 tint 後の角丸と影、(d) リスト下端 18dp とジェスチャーバーの重なり、(e) ハンドル/ヘッダーのみがドラッグ可能であることの操作感、を判定項目に含める
3. **(蒸留時)** ADR-0005 の追補と本文の矛盾 (選択印の描画) と、高さ挙動の記述 (「Material の標準挙動」) を実態に合わせて整合させる
