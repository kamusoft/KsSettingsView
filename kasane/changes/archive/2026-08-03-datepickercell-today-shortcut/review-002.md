# レビュー結果: datepickercell-today-shortcut (002 回目)

**日付**: 2026-08-03
**判定**: APPROVED
**対象**: 89bac6a 以降の未コミット変更 (`git diff HEAD` + 未追跡の新規ファイル)
**主眼**: review-001 / second-opinion-002 の突き合わせで採用された 3 件の修正の反映確認と、新たな問題の混入確認

## サマリー

採用済み 3 件の修正はいずれも正しく反映されている。①スペーサ挿入はコンテナ同一性の判定で `date_picker_actions` に限定され、フルスクリーンのフォールバックは material 1.12.0 の実レイアウト `mtrl_picker_fullscreen` を inflate したテスト 3 件 (縦・横・クリック経路) に置き換わった。②ヘッダ追随のアサートは「今日を初期選択にした picker から採った期待値との一致」へ強化され、期待値が移動前の表示と異なることを事前 assert して空振りを封じている。③Sample の陳腐化コメントは修正され、あわせてカレンダーモードの Cell に `todayText` デモが追加された。`android/` で `./gradlew test lintDebug` を再実行して BUILD SUCCESSFUL・failures 0 (テスト結果 XML 集計で 1676 件、うち `DatePickerTodayShortcutTest` 22 件) を確認した。足場の逆流 (proposal.md / spec.md の書き換え) は無く、修正で新たに持ち込まれた欠陥も見つからなかった。指摘は Minor 1 件と Suggestion 1 件で、いずれもマージを止める性質ではない。

## 修正の反映確認

### ① フルスクリーンフォールバックの実レイアウト検証 + スペーサの限定 → 反映済み

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DatePickerTodayShortcut.kt:96-105` / `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/DatePickerTodayShortcutTest.kt:109-171`, `:466-474`

スペーサ挿入の条件が `container === actionRow && container is LinearLayout && container.orientation == HORIZONTAL` になった。`actionRow` は `findFirstById(MaterialIds.DATE_PICKER_ACTIONS)` の結果そのものなので、参照同一性による限定は ID 比較と等価であり、フォールバック先には入らない。差し込み先が `date_picker_actions` のときも「横並びの LinearLayout」であることを毎回確認してから入れる形で、置き方の前提が崩れたら静かに素通りする。

テストは合成階層を捨て、`ThemeOverlay_Material3_MaterialCalendar_Fullscreen` を載せて `MaterialR.layout.mtrl_picker_fullscreen` を inflate する `inflateFullscreen()` に置き換わった。material 1.12.0 の aar を展開して裏を取った結果は以下のとおりで、テストの前提は実物と一致している。

- `mtrl_picker_fullscreen.xml` は `mtrl_picker_header_fullscreen` と `mtrl_calendar_frame` だけを持ち、`date_picker_actions` を含まない → T:114-117 の `assertNull` は実物の性質を突いている
- `mtrl_picker_header_fullscreen.xml` の `confirm_button` の親は `orientation="@integer/mtrl_calendar_header_orientation"` を持つ `LinearLayout` → フォールバック先が LinearLayout であることが保証され、`buttonLayoutParams` が返す `LinearLayout.LayoutParams` と型が整合する (親の型が違えば measure 時に ClassCastException になり得た経路)
- 同 integer は `values/` = 1 (縦並び) / `values-land/` = 0 (横並び) → 横向きだけスペーサが問題になる構図で、それを突くテスト (T:137 `@Config(qualifiers = "+land")`) が入っている

さらに T:161 で、フォールバック先に置いたボタンの押下が経路 D の要求まで結線されていることを見ており、「差し込めたが押せない」を排除している。契約テスト側にも `date_picker_actions` の型・向き・取消/確定の親子関係を固定する 1 件が加わった (`DatePickerMaterialContractTest.kt:70-90`)。

### ② ヘッダ追随アサートの期待値一致化 → 反映済み

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/DatePickerTodayShortcutTest.kt:197-205`, `:539-540`

`assertTrue(text != headerBefore)` が消え、`todayHeaderSelectionText()` — 今日を初期選択にした picker を別に開いてそこに出る文字列を採る — との `assertEquals` になった。material の `DateStrings` は公開 API から組み立てられないため、同じ material に作らせた文字列を期待値にするのは妥当な取り方で、`@Config(qualifiers = "ja")` により決定的。加えて `assertNotEquals(headerBefore, headerForToday)` を移動前に置いて「初期値と今日のヘッダ表示が偶然一致していて assert が空振りする」構図も塞いでいる。指摘の意図 (「今日以外の日付へ動いても通る」を潰す) を満たしている。

### ③ Sample コメントの陳腐化解消 → 反映済み

**該当箇所**: `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/InputCellsDemoScreen.kt:222-223`, `:244-246`

「(Spinner モード限定)」が「(Spinner では 3 連ホイールを今日へ動かす)」に置き換わり、`todayText` がモード共通のオプトインで表示方法だけが違う、という現在の事実と整合した。あわせてカレンダーモードの Cell に `todayText = "今日"` とその説明コメントが入り、直後のカレンダー例との矛盾も解消している (この追加は brief.md の照合結果の補足に恒久変更として記録済み)。

## 確認した観点 (指摘なしの範囲)

- **ビルドとテスト**: `android/` で `./gradlew test lintDebug` → BUILD SUCCESSFUL。テスト結果 XML の集計で 1676 件 (debug/release の 2 variant)・failures 0・errors 0・skipped 0。lint 違反なし
- **足場の凍結**: `kasane/changes/datepickercell-today-shortcut/` で変更されているのは `tasks.md` (全行 `[ ]`→`[x]` のみ) と `ui/brief.md` (照合結果の追記のみ、削除行 0)。正である `proposal.md` と `specs/settings-view-android-ui/spec.md` は 89bac6a から無変更
- **申し送りの実行**: review-001 のアクションプラン 1 (復元対象への今日ボタン追加と、経路 D の世代付き tag) は `kasane/changes/fix-picker-dialog-recreation/exploration.md` へ追記済みで、最小修正案の `findFragmentByTag(tag)` が固定 tag 前提では取り逃す点まで書かれている
- **着色の分岐順序**: `DatePickerColorizer.applyStaticRole` の `when` で `ks_date_picker_today_button` の枝は末尾の `view is TextView` (通常文字ロール) より前にあり、差し込んだボタンが通常文字色で塗り潰される経路は無い (`DatePickerColorizer.kt:272-275`)
- **停止性 / 再帰**: `showMaterialDatePicker` の作り直しは押下起点でしか起きず、作り直し後の picker は今日の月を表示中のため経路 A が成立する。`rebuildCount` が自動で増え続ける経路は無い
- **コメント規約**: `.claude/hooks/comment-policy-check.py` のパターンを本変更の対象ファイル (新規 2 ファイル + `ids.xml` を含む) 全体に流したところ、該当は `DatePickerColorizer.kt:52` `:328` `:465` `:600` の 4 件のみで、いずれも review-001 で「diff 範囲外の既存記述・今回は対処しない」と降格済みのもの。今回の修正が新たに持ち込んだ違反は無い
- **修正による副作用**: ②で追加された `todayHeaderSelectionText()` は Robolectric の Activity をもう 1 つ立てるが、`Shown` は自分の Activity の FragmentManager だけを見るため対象 picker には干渉しない。①の限定は `todayText` 未指定時の構成不変 (T:71 / T:80 のボタン行子数 2) にも影響していない

## 指摘事項

### [🟡 Minor] Spinner 側テストの名称とコメントが本変更で陳腐化した

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/DateSelectionSheetTest.kt:437-439`

**問題点**:
`Material モードでは todayText は選択 UI に影響しない` というテスト名と、直下の「3 連ホイールの選択面 (と「今日」操作) は提示されない。todayText の有無で経路は変わらない」というコメントは、本変更の前は正しかったが、いま `todayText` は Material モードの選択 UI に確かに影響する (カレンダーのボタン行に操作が増える)。assert しているのは `tapRow` が Spinner の選択面を返さないことだけなので**テストは正しく通り続けており、誤った assertion ではない**。しかし名称・コメントだけを読むと、ADDED Requirement (カレンダーモードの今日ショートカットの提示) と正面から矛盾する記述に見える。採用済みの修正③ (Sample コメントの陳腐化解消) とまったく同じ型の陳腐化で、原因も同じくこの変更である。

**推奨修正**:
テスト名を検証内容に合わせる (例: `Material モードでは 3 連ホイールの選択面を提示しない`) 。コメントも「Material モードの「今日」操作はカレンダー側に出るため、ここでは 3 連ホイールが開かないことだけを見る」の趣旨へ書き直す。assert 本体の変更は不要。

#### 確認結果 (2026-08-03、オーケストレーターによる直接修正の独立確認) — **解消**

`git diff HEAD -- .../DateSelectionSheetTest.kt` を独立に取って確認した。差分は 4 行 (テスト名 1 行 + コメント 2 行 → 3 行) のみ。

1. **名称・コメントと実態の一致**: 新しい名称 `Material モードでは todayText があっても Spinner の選択面は開かない` は、`todayText` 指定あり / なしの 2 パターンで `tapRow(...)` が `null` (= Spinner の選択面が開かない) を返すという assert 内容そのものを述べており、実態と一致する。「todayText は選択 UI に影響しない」という現在は成り立たない主張は消えた。コメントも「3連ホイールの選択面は提示されない」と対象を 3 連ホイールに限定し、Material 側の「今日」操作の担当先を `DatePickerTodayShortcutTest` として明示したため、ADDED Requirement と矛盾して読める箇所は残っていない。テストクラス名はコード内の識別子であり、`comment-policy-check.py` の禁止参照 (外部文書 ID への依存) には当たらない — 同ファイル全体にパターンを流して該当 0 件を確認した
2. **assert への影響**: 差分に `assertNull` / `tapRow` / `DatePickerCell(...)` の行は 1 行も含まれず、検証内容は無変更。`./gradlew :ks-settingsview-ui:testDebugUnitTest --tests "*DateSelectionSheetTest*" --rerun-tasks` をキャッシュ無効で再実行し、44 件 / failures 0 / errors 0 / skipped 0、当該テストが新名称で結果 XML に存在することを確認した
3. **同種の陳腐化の残存**: `todayText` / 「今日」に言及するコメント・KDoc・テスト名を `android/` `samples/` 全体で洗い出したが、「Spinner 限定」「Material では出ない」の趣旨で陳腐化したものは他に残っていない。`DateSelectionSheet.kt:279` `:283` `:293-294` は Spinner の選択面クラス自身の KDoc、`DatePickerCell.kt:21` はモード非依存の記述、`InputCellsDemoScreen.kt:222` `:244-245` は今回の修正③で更新済み。iOS 側 (`DatePickerCell.swift:12` 等) は Wheels / Calendar 双方に言及しており本変更の影響を受けない

以上より Minor 1 は解消。判定 (APPROVED) は変わらない。

### [🔵 Suggestion] フルスクリーン縦向きでは「今日」が確定ボタンの上に縦積みになる

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DatePickerTodayShortcut.kt:87-96`

**問題点**:
フォールバック先である `mtrl_picker_header_fullscreen.xml` の確定ボタンの親 LinearLayout は `orientation="@integer/mtrl_calendar_header_orientation"` で、この integer は material 1.12.0 で `values-land/` = 0 (横並び) / `values/` = 1 (**縦並び**)。したがってフルスクリーン**縦向き**では、差し込んだ「今日」が確定ボタンとモード切替アイコンの上に縦に積まれる形になる。しかも親のさらに外側 `fullscreen_header` は `layout_height="@dimen/mtrl_calendar_header_height_fullscreen"` の固定高で、列に 3 つ目が入ることで下端が切れる可能性がある。

ただし、これは今回の修正が持ち込んだものではない (縦向きでは修正前もスペーサは入らず、積まれ方は同じ)。また `ui/brief.md` は「フルスクリーン表示などアクション行が無い構成のフォールバック配置は実装判断で、mock 照合の対象外。ボタンの提示と挙動のみを検証する」と明示しており、**承認済みの合意の範囲内**である。実機での見え方は未確認 (Robolectric テストは inflate と子数の確認までで、measure/layout を通していない)。

**推奨修正**:
本変更では不要。将来フルスクリーンの見た目を扱う気になったときの材料として、`install` の分岐で親が縦並びのときはヘッダではなく別の置き場を探す (あるいはカレンダー本体の上部へ置く) 選択肢を残しておく。今の情報だけで作り込むのは、mock 照合の対象外という合意を実装側だけで動かすことになるので勧めない。

## アクションプラン

1. ~~(任意・低優先) Minor: `DateSelectionSheetTest.kt:437` のテスト名とコメントを、Material モードで検証している内容に合わせて書き直す~~ → **2026-08-03 対応済み・独立確認済み** (上記「確認結果」)
2. (対応不要) Suggestion: フルスクリーン縦向きの縦積みは brief.md の合意範囲内。記録のみ

**未対応の指摘は残っていない。**
