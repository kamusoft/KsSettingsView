# レビュー結果: android-picker-selection-sheet (001 回目)

**日付**: 2026-08-02
**判定**: CHANGES_REQUESTED

## サマリー

デルタスペックの 6 Requirement / 16 Scenario はすべて実装とテストで対応が取れており、`AlertDialog` からボトムシートへの差し替えは公開 API を変えずに完了している。作業状態と確定 callback の分離、範囲外 index の非正規化、上限到達時の haptic、区切り線の `onDrawOver` 描画など、既存の規約・過去教訓に沿った実装で、コメントも `android/ADR-0005` 参照へ移行済みでソースコメント規約に適合する。

一方でヘッダーの確定 / 取消ボタンのタップ領域が 48dp 未満であり、本変更が候補行のアクセシビリティを明示要件に掲げている中で、モーダルの主操作だけが指針を外れている。見た目を変えずに修正できるため、これを唯一の必須修正として CHANGES_REQUESTED とする。残りは視覚照合 (tasks 4.1) / 実機確認 (4.2) の観点に回せる低優先の指摘。

**検証した客観事実**: `./gradlew :ks-settingsview-ui:testDebugUnitTest` を再実行し、`build/test-results/*/TEST-*.xml` の集計で **1134 tests / 0 failures** (ui 417×2 + core 74×2 + compose 76×2) を確認した (テスト実行規約に従い件数まで確認)。tasks.md のチェックはグループ 1〜3 のみで、未実施のグループ 4 は未チェックのままであり虚偽チェックはない。足場アーティファクト (proposal / spec / brief / mock) は実装期間中に書き換えられていない。

## 指摘事項

### [🟡 Minor] ヘッダーの確定 / 取消ボタンのタップ領域が 48dp 未満

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/PickerSelectionSheet.kt:236-247` (cancelView) / `:261-287` (confirmView) / 定数 `:579-587`

**問題点**: `cancelView` は `HEADER_ACTION_TEXT_SIZE_SP = 15sp` + 上下 `HEADER_ACTION_PADDING_V_DP = 6dp`、`confirmView` は同 15sp + 上下 `CONFIRM_PADDING_V_DP = 6dp` で、いずれも実高さが約 30〜32dp にとどまり、Android のタップ領域指針 48dp を下回る。候補行は `ROW_MIN_HEIGHT_DP = 48dp` を満たしている (`:351`) ため、モーダル内で主操作だけが指針を外れている状態になる。本変更は「候補行のアクセシビリティ状態」を Requirement に持ち、確定操作は複数選択で唯一の確定手段であるため、影響は小さくない。

**推奨修正**: 見た目 (mock の pill / テキストボタンの寸法) を変えずに当たり判定だけを広げる。

- `cancelView`: 背景を持たないため `minHeight = dp(48)` + `gravity = START or CENTER_VERTICAL` を足すだけで視覚変化なく 48dp を確保できる
- `confirmView`: `minHeight` を足すと pill 背景が伸びるため、`endSlot` (`:300-310`) 側に `TouchDelegate` を設定するか、`startSlot` / `endSlot` の `minimumHeight` を 48dp にして pill を中央配置する

### [🟡 Minor] 長いタイトルとロケール依存の操作ラベルでヘッダー左右のボタンが潰れうる

**該当箇所**: `PickerSelectionSheet.kt:249-259` (titleView の `maxWidth`) / `:289-320` (weight 1 の左右スロット)

**問題点**: ヘッダーは「weight 1 のスロット + WRAP_CONTENT のタイトル + weight 1 のスロット」で構成され、`LinearLayout` は WRAP_CONTENT のタイトルを先に測ってから残りを左右へ等分する。タイトルは `HEADER_TITLE_MAX_WIDTH_RATIO = 0.6` まで伸びるため、狭い画面では左右スロットが約 20% ずつ (360dp 幅で 60dp 前後) しか残らない。操作ラベルは OS リソース由来でロケールにより長さが変わる (日本語「キャンセル」、ドイツ語 "Abbrechen" 等) ため、この幅を超えると `isSingleLine = true` かつ `ellipsize` 未設定の `cancelView` が黙って切り詰められる。極端なケースではスロット幅が 0 になり操作ラベルが見えなくなる。

**推奨修正**: 左右スロットを WRAP_CONTENT、タイトルを weight 1 (+ `ellipsize = END`) にして「先に縮むのはタイトル」という優先順位にする。mock の `1fr auto 1fr` と同じ中央揃えは、左右スロットへ同じ `minWidth` を与えるか、タイトルに `gravity = CENTER` を保った上で左右の実測幅の大きい方を両側に適用することで維持できる。

### [🟡 Minor] シートコンテナ背景の透明化で elevation の外形が角丸と食い違う

**該当箇所**: `PickerSelectionSheet.kt:470-489` (`applySheetHeight` 内の `sheet.setBackgroundColor(Color.TRANSPARENT)`)

**問題点**: `design_bottom_sheet` の既定背景 (Material の `MaterialShapeDrawable`) を `ColorDrawable` で置き換えているため、(1) View の outline が角丸を失い矩形になる — elevation による影が出る構成では、角丸の `contentRoot` の背後に矩形の影が覗く可能性がある、(2) `BottomSheetBehavior` が全展開時に行う角丸の補間 (corner interpolation) が効かなくなる。mock (`plan-b.html:36`) はシートに `box-shadow` を置いており、影の有無・形は視覚照合の判定対象になる。

**推奨修正**: コンテナ背景を潰さず、`?attr/bottomSheetStyle` / `shapeAppearanceOverlay` かコンテナの `MaterialShapeDrawable` の fill 色を `theme.cellBackgroundColor` に差し替える形にすると、角丸・影・展開時の補間を Material 標準のまま保てる。少なくとも tasks 4.1 の視覚照合で角の影の見え方を確認対象に加えること。

**副次**: この背景設定はメソッド名 `applySheetHeight` の責務 (高さ) と一致しない。高さ適用と面の見た目の適用は分けるとよい。

### [🔵 Suggestion] `onStart()` ごとに `STATE_COLLAPSED` へ戻る

**該当箇所**: `PickerSelectionSheet.kt:170-173`, `:483-488`

**問題点**: `applySheetHeight()` を `onStart()` で呼び、毎回 `state = STATE_COLLAPSED` を代入している。マルチウィンドウの再開・画面消灯からの復帰など、シート表示中に `onStart` が再度走る局面でユーザーが上方向ドラッグで全展開した状態が初期高さへ戻る。

**推奨修正**: 状態の初期化は初回のみに限定する (フラグを持つか、`state` の代入を `peekHeight` 設定と分ける)。

### [🔵 Suggestion] 高さ・タイトル幅の計算に画面幅を使っている

**該当箇所**: `PickerSelectionSheet.kt:258` / `:475-481`

**問題点**: `contentRoot.measure(...)` の幅に `displayMetrics.widthPixels` を、タイトルの `maxWidth` にも同じ値を使っているが、Material のボトムシートは横幅の広い画面で最大幅 (Material 3 既定 640dp) に制限される。タブレット・横向きではシート実幅と測定幅が食い違い、peekHeight とタイトル上限幅が想定とずれる。

**推奨修正**: 実幅が確定してから測る (`design_bottom_sheet` の実測幅、または `contentRoot` の `doOnLayout`) か、最大幅の制約値を考慮した幅で測る。

### [🔵 Suggestion] RecyclerView の実 bind 経路がテストで一度も通っていない

**該当箇所**: `PickerSelectionSheet.kt:499-506` (`bindRow(index)` / `currentWorkingSelection()` の検証用フック)、`PickerSelectionSheetTest.kt` 全体

**問題点**: 候補行の検証はすべて `sheet.bindRow(index)` の直接生成経路を通っており、`ItemsAdapter.onCreateViewHolder` → `onBindViewHolder` の実経路 (`:522-531`) はテストで一度も実行されない (`adapter?.itemCount` の確認のみ)。bind ロジック自体は共有されているため現時点の破綻はないが、adapter の配線・LayoutManager 設定・行の実レイアウトが壊れてもテストは緑のままになる。

**推奨修正**: 1 ケースだけでも `listView.measure(...)` / `layout(...)` を回して `listView.getChildAt(0)` から表示名と選択印を確認する経路を足すと、フックの前提ずれを検出できる。Robolectric の描画限界に触れる部分 (実 ellipsize 等) は含めない範囲で十分。

### [🔵 Suggestion] 非確定 dismiss のテストが「経路が有効であること」を確認していない

**該当箇所**: `PickerSelectionSheetTest.kt:137-159`

**問題点**: 3 経路とも `cancel()` / `onBackPressed()` / `dismiss()` を直接呼んでいるため、実質「callback は click listener からしか発火しない」ことの再確認になっている。外側タップ経路については、そもそも外側タップで閉じられる設定になっているか (`setCanceledOnTouchOutside` の既定) を確認していないため、将来これが `false` に変わっても検知できない。

**推奨修正**: `sheet.isCanceledOnTouchOutside` 相当 (Robolectric なら `shadowOf(sheet)` の cancelable / canceledOnTouchOutside) を 1 件アサートしておくと、Scenario「非確定 dismiss は経路によらず callback を発火しない」の前提そのものが担保される。

### [🔵 Suggestion] コメント内のアーカイブ資料への裸参照

**該当箇所**: `PickerSelectionSheet.kt:217`

**問題点**: 「（承認モックの `1fr auto 1fr` グリッドと同型）」は `kasane/changes/.../ui/mock/plan-b.html` への裸参照であり、アーカイブ後に指す先を追えない (ソースコメント規約の「拡張子なしの裸参照」「アーカイブ文書への参照」に該当)。直前の文が配置意図を自己完結して説明しているため、参照句は装飾になっている。

**推奨修正**: 参照句を削除する (定型句型の書き換え)。

### [🔵 Suggestion] 単一選択で選択中項目が初期表示に入らない

**該当箇所**: `PickerSelectionSheet.kt:324-338` (リスト構築)

**問題点**: シート高さは画面の約半分が上限のため、候補が多いと選択中の項目が初期表示範囲の外になる。差し替え前の `AlertDialog.setSingleChoiceItems(items, checkedItem, ...)` は checkedItem の位置までスクロールした状態で開いていたため、この点だけは体験が後退する。

**留保**: デルタスペック・brief のいずれにも初期スクロール位置の要求はなく、行レイアウトの正である iOS の `PickerListViewController` も選択行へスクロールしていない。**プラットフォーム間の同質性を優先するなら現状のままが正**であり、修正は仕様判断を伴う。実機確認 (tasks 4.2) で体感を見た上でオーナー判断とするのが妥当。

### [🔵 Suggestion] 実機確認で navigation bar 下の余白を見ること

**該当箇所**: `PickerSelectionSheet.kt:328-330` (`LIST_PADDING_BOTTOM_DP = 18dp` 固定)

**問題点**: リスト下端の余白は mock 由来の固定値であり、system bar の inset ではない。利用者アプリが edge-to-edge (targetSdk 35 では既定で強制) の場合、最終行やタップ領域がジェスチャーバーと重なる可能性がある。

**推奨修正**: tasks 4.2 の実機確認にジェスチャーナビゲーション端末での下端の重なり確認を含める。重なるようなら固定パディングに navigation bar inset を加算する。

### [🔵 Suggestion] 使われていないフィールド

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/PickerCellViewHolder.kt:21`, `:24`, `:93`

**問題点**: `currentCell` は代入とクリアのみで読み出しがない (本変更の前から未使用)。「タップ時に内容を取り出すため」という doc コメントは実態と合っておらず、シート提示は `bind` の引数を直接束縛している。

**推奨修正**: 同ファイルを触っている変更なので、フィールドごと削除する (本変更のスコープ外と判断するなら据え置きでよい)。

## 確認して問題がなかった観点

- **Scenario 対応**: タイトル解決 / 全件列挙 + formatter / OS ラベル解決 / キャンセル・非確定 dismiss の callback 不発火 / 単一選択の即時確定 / 複数選択の確定・破棄 / 上限到達時の無視 + haptic / 上限到達時の解除 / 強調色 3 段解決 / アクセシビリティ状態とトグル後更新 / 範囲外 index 保持 / 初期上限超過 / items 空 — 全 16 Scenario に実装とテストの対応がある
- **強調色の解決順序**: `cell.accentColor` → `EffectiveStyle.accentColor` (= `CellStyle.accentColor` → `Theme.cellAccentColor`) で spec の 3 段解決と一致 (`PickerSelectionSheet.kt:59-67`)
- **モデル値の非正規化**: 範囲外 index は `workingSelection` に保持され、確定集合にも `size` による上限判定にも残る。単一選択の範囲外 `selectedIndex` は選択印なし
- **確定経路の閉包**: callback の発火点は候補行タップ (単一) と `confirmView` タップ (複数) の 2 箇所のみで、`cancel()` / `dismiss()` 経路からは到達しない
- **区切り線**: `onDrawOver` で 1 物理 px 描画 (行背景に上書きされない)、先頭行の上に線・最終行の下に線なしで mock (`list` の `border-top` + `item` の `border-bottom` + `:last-child` 無し) と一致
- **チェック表示**: `KsSimpleCheckView` を 30dp で使用し、既存の `RadioCellViewHolder` / `SimpleCheckCellViewHolder` と寸法・描画が揃っている
- **アクセシビリティの構成**: 行コンテナが名前 (`contentDescription`) と状態 (`isSelected` / `isCheckable` + `isChecked`) を 1 ノードで公開し、子の TextView / チェック View は読み上げ対象から外している。ドラッグハンドルも装飾として除外
- **RecyclerView のリサイクル整合**: トグルは `workingSelection` を正として `onBindViewHolder` で常に再適用されるため、スクロールで表示状態が壊れない
- **ソースコメント規約**: 新規・変更コメントに `openspec/` / `spec.md` / `Decision N` / `MUST` 等の禁止参照はなく、旧 `PickerCellViewHolder` の `openspec/...` 参照は `android/ADR-0005` へ置換済み (残る 1 件は上記 Suggestion)
- **足場凍結**: proposal / spec / brief / mock に実装中の書き換えなし。tasks.md はチェックボックスのみの更新

## アクションプラン

1. **(必須)** ヘッダーの確定 / 取消のタップ領域を 48dp 以上にする — 見た目を変えない方法 (`minHeight` / `TouchDelegate`) で対応する
2. **(推奨)** ヘッダーの幅配分をタイトル優先縮小に変え、ロケール依存のラベル長で操作ボタンが潰れないようにする
3. **(推奨)** シートコンテナ背景の透明化を見直すか、tasks 4.1 の視覚照合に「角の影の形」を判定項目として加える
4. **(任意)** `onStart` ごとの状態リセット・画面幅ベースの測定・RecyclerView 実経路のテスト・コメントの裸参照・未使用フィールドを整理する
5. **(任意 / オーナー判断)** 単一選択時の初期スクロール位置は実機確認の上で現状維持か追随かを決める
