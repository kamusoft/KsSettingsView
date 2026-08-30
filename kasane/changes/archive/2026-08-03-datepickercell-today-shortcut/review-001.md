# レビュー結果: datepickercell-today-shortcut (001 回目)

**日付**: 2026-08-03
**判定**: APPROVED

## サマリー

デルタスペックの 3 Requirement / 13 Scenario はすべて実装とテストに対応が取れており、`./gradlew test lintDebug` を再実行して BUILD SUCCESSFUL・failures 0 (ks-settingsview-ui 668 件) を確認した。ADR-0010 の Decision 1〜5 (経路 A / リフレクション不採用 / 経路 D フォールバック / 押下時の日単位範囲比較 / MaterialIds + 契約テスト) にすべて従っており、単発実行の門 (`isJumping`) と世代番号による post キャンセルの組み合わせは「連打 = 1 回」「移動完了前の dismiss で再提示しない」の 2 Scenario を実際に成立させている。無断の仕様逸脱・虚偽のタスクチェック・足場アーティファクト (proposal.md / specs/spec.md) の書き換えは無い。指摘は Minor 3 件と Suggestion 2 件で、いずれも本変更のマージを止める性質のものではない。

## 確認した観点 (指摘なしの範囲)

- **仕様充足**: ADDED 3 Requirement / MODIFIED 1 Requirement の全 Scenario に対応するテストが `DatePickerTodayShortcutTest` に存在する。範囲外判定が `jumpToToday` の最初にあるため、代替表示状態 (年選択 / テキスト入力) でも「今日が範囲外なら何も変更しない」が自動的に成立する
- **足場の凍結**: 変更されている足場は `tasks.md` (チェック更新) と `ui/brief.md` (照合結果の追記のみ、削除行 0) だけで、正である `specs/settings-view-android-ui/spec.md` と `proposal.md` は無変更
- **停止性**: `driveJump` の世代不一致による早期 return は、必ず `generation` を進めた側 (`onViewDestroyed` / `requestRebuild`) が `isJumping = false` も同時に落とすため、ボタンが恒久的に無反応になる経路は無い
- **正規クリック経路**: `performItemClick(cell, dayPosition, adapter.getItemId(dayPosition))` は position を渡す形で、material 側リスナは view ではなく position を使うため、`getChildAt` が想定外の子を返しても選択日は狂わない
- **RTL**: `marginStart` + index 0 挿入のため、RTL では「今日」が行の start (右) に来る。`gravity="end"` のコンテナと整合する
- **iOS コメント**: `handleToday()` の実装 (startOfDay 比較で範囲外は return、`setDate(todayStart)` で表示月・選択日とも移動、通知なし) と新しいコメントの記述が一致している
- **コメント規約**: 本変更が追加したコメントに `.claude/hooks/comment-policy-check.py` の禁止パターンの該当は無い

## 指摘事項

### [🟡 Minor] 構成変更後の復元に「今日」ボタンが乗ったこと、および世代付き tag が既存の切り出し変更へ申し送られていない

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DatePickerCellViewHolder.kt:145-146`, `:171`

**問題点**:
`DatePickerColorizer.attach` は `showMaterialDatePicker` の中でしか呼ばれないため、画面回転等で Activity が再生成されると復元済みダイアログにフックが張り直されない。本変更で「今日」ボタンの差し込みも同じフックに相乗りしたので、**復元後はボタンごと消える**という新しい症状が加わった。この構造問題自体は `kasane/changes/fix-picker-dialog-recreation/` として切り出し済み (オーナー決定) なので先送りは合意済みだが、以下 2 点はどこにも記録されていない:

1. 復元時に張り直すべき対象が「着色 + 値確定リスナ」から「+ 今日ボタンの差し込み」へ増えたこと
2. 経路 D の作り直しで tag が `DatePickerCell.material.<pos>.r1` のように世代サフィックスを持つようになったこと。`fix-picker-dialog-recreation/exploration.md` が最小修正案として挙げている `fm.findFragmentByTag(tag)` は、素の `bindingAdapterPosition` 由来 tag しか見ないと作り直し後のダイアログを取り逃す

**推奨修正**:
コード修正は不要。蒸留 (ksn-distill) の時点で上記 2 点を `fix-picker-dialog-recreation` の探索メモへ申し送るか、本変更に既知の限界として記録する。`concepts/core/cells/date-picker-selection-surface.md` の「適用範囲はダイアログの表示セッション内」という記述も、着色だけでなく差し込んだ操作にも及ぶことを含める余地がある (記載の要否は蒸留時の価値 lint で判断)。

### [🟡 Minor] フルスクリーン構成のフォールバック先に伸縮スペーサが無条件で挿入され、テストも合成階層でしか通していない

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DatePickerTodayShortcut.kt:95-101` / `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/DatePickerTodayShortcutTest.kt:107-121`

**問題点**:
スペーサは「`date_picker_actions` が `gravity="end"` なので取消 / 確定を右端へ押し出す」ための仕掛けだが、挿入条件が「コンテナが横並びの LinearLayout であること」だけなので、フォールバック先 (`confirm_button` の親) にも入る。material 1.12.0 の `mtrl_picker_header_fullscreen.xml` では `confirm_button` の親は `[confirm_button][mtrl_picker_header_toggle]` を包む `wrap_content` の LinearLayout であり、余剰幅が 0 なので weight=1 のスペーサは幅 0 のまま無害ではあるが、意図と結びつかない挿入が残る。material 側のレイアウトが変わったときに、この無条件挿入がヘッダを崩す事故になり得る。

あわせて、フォールバック配置のテストは Button 1 個だけの LinearLayout を手で組んだ合成階層で、実レイアウトを通していない。契約テストが `MaterialR.layout.mtrl_picker_dialog` / `mtrl_calendar_horizontal` を inflate できている以上、`mtrl_picker_header_fullscreen` も同じ手段で通せる。

**推奨修正**:
スペーサの挿入を `container.id == MaterialIds.DATE_PICKER_ACTIONS` のときに限定する。テストは `mtrl_picker_header_fullscreen` を inflate した階層に差し替えると、合成階層による代理検証を外せる。なお brief.md はフルスクリーン配置を mock 照合の対象外としているため、配置の見た目そのものは指摘していない。

### [🟡 Minor] ヘッダ追随のアサートが「変化したこと」しか見ていない

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/DatePickerTodayShortcutTest.kt:153`

**問題点**:
`assertTrue(shown.headerSelectionText() != headerBefore)` は、ヘッダが**今日以外の日付**へ動いた場合でも通る。brief.md の検証条件は「ヘッダの選択日テキストも更新される」であり、実機照合では「2026年8月3日」と具体値を確認しているのに、自動テスト側は差分の有無しか固定していない。`@Config(qualifiers = "ja")` で表示文字列は決定的なので、直接値を突ける。

**推奨修正**:
`headerSelectionText()` が今日の日付を表す文字列であること (例: `contains("8")` と `contains("3")` ではなく、`DateStrings` 相当の期待値との一致、あるいは最低でも `FIXED_TODAY.dayOfMonth` を含むこと) をアサートする。`selectedDayCell()` を使った検証はテキスト入力側のテストで既に行っているので、そちらの手法を流用してもよい。

### [🔵 Suggestion] 待ち合わせ中にモード切替されたときのキャンセル (未再現・机上の指摘)

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DatePickerTodayShortcut.kt:196-226`

**問題点**:
`generation` を進めるのはダイアログ View の破棄と作り直し要求だけで、**ダイアログを開いたままカレンダー → テキスト入力へ切り替えた**場合は進まない。「今日」を押した直後 (post した待ち合わせが走る前、およそ 1 フレーム) にモードを切り替えると、`driveJump` が目的月の日グリッドを見つけられないまま上限まで回り、最後に `requestRebuild` へ倒れてダイアログが閉じて開き直る (= ユーザーのモード切替が巻き戻る)。結果としての表示月・選択日は spec どおり今日になるため仕様違反ではなく、タイミング窓も極めて狭い。実機・Robolectric とも再現は取っておらず、コード読解による机上の指摘であることを明記しておく。

**推奨修正**:
必須ではない。気にするなら、`driveJump` の待ち合わせ中に日グリッドの器 (`MaterialIds.MONTHS`) が階層から消えたことを検出した時点で、作り直しへ倒さず静かに畳む (`isJumping = false` して return) 選択肢がある。ただし「表示中かつ範囲内での no-op は許容しない」という tasks.md 3.4 の方針とはトレードオフになるため、変えるなら方針の再確認が要る。

### [🔵 Suggestion] 本変更の範囲外: DatePickerColorizer.kt に既存の comment-policy 違反が残っている

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DatePickerColorizer.kt:52`, `:328`, `:465`, `:600`

**問題点**:
`.claude/hooks/comment-policy-check.py` のパターンを本変更の対象ファイル全体に流したところ、DatePickerColorizer.kt に 4 件の該当が残っている (承認モックへの参照 3 件、アーカイブ作業文書のパス参照 1 件 — `:465` の `kasane/changes/datepickercell-color-adjust/impl-notes.md`)。**いずれも本変更の diff 範囲外の既存記述**であり、本変更が持ち込んだものではない (本変更が追加したコメントに該当は無い)。hook は Edit/Write の書き込み内容だけを見るため、触っていない行は検出されない。

**推奨修正**:
本変更では対応不要。同ファイルを次に触るときのついでの是正候補として記録するに留める。

## アクションプラン

1. (蒸留時) Minor-1: 復元対象に「今日」ボタンが加わったことと、経路 D の世代付き tag を `fix-picker-dialog-recreation` へ申し送る
2. (任意・低優先) Minor-3: ヘッダ追随のアサートを具体値に強める
3. (任意・低優先) Minor-2: スペーサ挿入を `date_picker_actions` に限定し、フォールバックのテストを実レイアウト inflate に置き換える
4. (対応不要) Suggestion 2 件は記録のみ
