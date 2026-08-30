# セカンドオピニオン: datepickercell-today-shortcut (002 回目 — review-001 対応の code-review)
**相方**: codex / **日付**: 2026-08-03 / **対象**: 実装 diff (89bac6a 以降の未コミット変更) + 変更アーティファクト
---
# レビュー結果: datepickercell-today-shortcut

**判定**: CHANGES_REQUESTED  
**件数**: Critical 0 / Major 1 / Minor 2 / Suggestion 0

## 指摘事項

### [🟠 Major] Fragment 復元後に「今日」ショートカットが失われる

**該当箇所**: [android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DatePickerCellViewHolder.kt:148](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DatePickerCellViewHolder.kt:148)、[DatePickerColorizer.kt:129](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DatePickerColorizer.kt:129)

**問題点**:  
「今日」ボタンは、Picker を新規表示するときだけ生成される一時的な `DatePickerTodayShortcut` と `FragmentLifecycleCallbacks` によって注入されています。画面回転などで Activity と FragmentManager が再生成されると、`MaterialDatePicker` 自体は FragmentManager から復元されますが、ショートカットのラベル・処理・ライフサイクルコールバックは Fragment state に保存されず、新しい FragmentManagerにも再登録されません。

その結果、開いたまま回転したカレンダーでは `todayText` が指定されていても「今日」操作が消え、提示 Requirement を満たさなくなります。現在の20件のテストには Activity 再生成ケースがありません。

**推奨修正**:  
復元された Picker に対しても現在の Cell からフックと操作を再接続できる所有構造にしてください。例えば、安定した Cell ID による Fragment tag と復元可能な表示設定を使い、ViewHolder の再 bind 時に既存 Picker を検出して再 attach する方法が考えられます。少なくとも、Picker を開いた状態で Activity を再生成し、ボタンの再提示・今日への移動・確定 callback を検証する回帰テストを追加してください。

### [🟡 Minor] フルスクリーン用フォールバックを実際のレイアウトで検証していない

**該当箇所**: [android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/DatePickerTodayShortcutTest.kt:106](android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/DatePickerTodayShortcutTest.kt:106)

**問題点**:  
テストは `LinearLayout` と確定ボタンだけの合成 View でフォールバック先を確認しています。実際の `mtrl_picker_fullscreen` では確定ボタン、入力モードトグル、縦横 orientation が組み合わさるため、配置、レイアウトパラメータ、クリック後の移動経路までは保証できません。フルスクリーン対応は proposal/tasks で明示された内部構造依存ですが、契約テストもダイアログ用レイアウトだけを対象にしています。

**推奨修正**:  
material 1.12.0 の実際のフルスクリーンレイアウト、可能なら実 `MaterialDatePicker` を使い、ボタンの提示・アクセシビリティ・今日への移動を確認してください。

### [🟡 Minor] Sample コメントが新しい対応範囲と矛盾している

**該当箇所**: [samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/InputCellsDemoScreen.kt:222](samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/InputCellsDemoScreen.kt:222)

**問題点**:  
Spinner の説明に「Spinner モード限定」と残っていますが、本変更により `todayText` は Material モードでも操作を提示します。直後のカレンダー例と矛盾し、利用例として誤解を招きます。

**推奨修正**:  
「Spinner モードでは3連ホイールを今日へ移動する」など、その例固有の表示方法を説明する文言へ修正してください。

## サマリー

通常表示、範囲境界、年選択、テキスト入力、single-flight、確定 callback の実装とテストは、デルタスペックおよび android/ADR-0010 に概ね一致しています。足場の変更もタスク完了チェックと視覚照合結果の追記に限定されています。

ただし、Androidの通常ライフサイクルである Fragment 復元後にショートカットが消える設計上の欠落があるため、現時点では承認できません。指定どおりファイル作成とテスト再実行は行っていません。


## 突き合わせ結果 (2026-08-03、ホスト review-001 との照合)

| # | 指摘 | ホスト側 | 採否 |
|---|---|---|---|
| Major 1 | Fragment 復元 (回転) 後に今日ボタンが消える | Minor 1 で同箇所を指摘 (既知の先送り fix-picker-dialog-recreation と同型、tag 世代サフィックスとの相互作用も指摘) | **双方一致 → 確定**。ただし既知の切り出し済み構造問題 (OK リスナー・Colorizer 着色も同時に消える) の一部のため、本変更で直すか先送り先へ申し送るかは**オーナー判断待ち** |
| Minor (相方) | フルスクリーンフォールバックが合成階層のみで未検証 | Minor 2 で同趣旨 | **双方一致 → 確定 (Minor)**。実レイアウト inflate のテストへ強化する |
| Minor (相方) | Sample の「Spinner モード限定」コメントが陳腐化 | 言及なし | **採用** (根拠強・修正容易) |
| Minor 3 (ホスト) | ヘッダ追随アサートが `!= before` で弱い | 相方言及なし | **採用** (アサーション強化) |
| Suggestion (ホスト) | 待ち合わせ中モード切替のキャンセル (机上・未再現) / Colorizer の diff 範囲外 comment-policy 違反 4 件 | — | **降格** (今回は対処しない。後者は本変更と無関係の既存事項) |

確定・採用 4 (うちオーナー判断待ち 1) / 降格 2。
