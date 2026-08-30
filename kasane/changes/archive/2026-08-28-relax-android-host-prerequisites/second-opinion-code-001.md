# セカンドオピニオン: relax-android-host-prerequisites (code-001)
**相方**: codex / **label**: so-code-relax-android-host-prerequisites / **日付**: 2026-08-27 / **対象**: コミット 68ac115 以降の作業ツリー全差分 (untracked 含む)
---
# レビュー結果: relax-android-host-prerequisites

**日付**: 2026-08-27  
**判定**: **CHANGES_REQUESTED**

## サマリー

主要なテーマ分離、Fragment 依存撤廃、日付選択状態の復元、MAUI 側の依存整合は仕様と概ね一致しています。一方、時刻シートの Activity 破棄時クリーンアップが実装されておらず、その不備を現在のテストが検出できません。

指摘件数: Critical 0 / Major 1 / Minor 2 / Suggestion 0

## 指摘事項

### [🟠 Major] TimeSelectionSheet が Activity の破棄に追随して閉じられない

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/TimePickerCellViewHolder.kt:57`  
**関連箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/TimeSelectionSheet.kt:271`  
**テスト**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/TimeSelectionSheetTest.kt:99`, `:354`

**問題点**: `TimeSelectionSheet` は生成直後に `show()` されるだけで、ViewTree の `LifecycleOwner` や `Activity.onDestroy` と結び付けられていません。表示中に Activity が再生成された場合、正常な `dismiss()` を経ず WindowManager の強制回収に依存するため、`WindowLeaked` や破棄済み Activity の保持につながります。

同じ問題を避けるため、`DateCalendarDialog.showAnchoredTo()` は `ON_DESTROY` で明示的に `dismiss()` していますが、時刻シートには相当する処理がありません。

回転テストも次の理由でこの不備を検出できません。

- `shownSheetCount()` は「現在表示中」ではなく累積作成数を数えているため、古いシートが `isShowing == true` のままでも結果は1件です。
- ViewHolder のルートを Activity の View 階層へ追加していないため、正しい lifecycle-anchor 実装に変更してもその経路を検証できません。
- 再生成前のシートに対する `isShowing == false` の確認がありません。

このため、`tasks.md` で完了扱いになっている回転シナリオは十分に担保されていません。

**推奨修正**: `DateCalendarDialog.showAnchoredTo()` と同様に、`TimeSelectionSheet` にアンカー View を受け取る表示経路を設け、ホストの `ON_DESTROY` で `dismiss()`、通常 dismiss 時に observer を解除してください。テストは実際の Activity View 階層へ行を取り付け、再生成前のシートについて `assertFalse(sheet.isShowing)`、新規シートが表示されていないこと、callback 無発火をそれぞれ検証してください。

### [🟡 Minor] 削除済み DatePicker 実装の ID と説明が残っている

**該当箇所**: `android/ks-settingsview-ui/src/main/res/values/ids.xml:3`

**問題点**: `ks_date_picker_today_button` は、削除された `DatePickerTodayShortcut` と色走査用の ID ですが、現在のコードから参照されていません。コメントも「今日ボタンの差し込みと着色走査に使用する」と、現在存在しない処理を説明しています。

**推奨修正**: `ks_date_picker_today_button` と対応コメントを削除してください。

### [🟡 Minor] テストコメントが旧実装の履歴に依存している

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/DateCalendarDialogTest.kt:47`  
**関連箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/TimeSelectionSheetTest.kt:39`

**問題点**: コメントが「旧経路」「MaterialTimePicker = DialogFragment」と実装履歴を説明しており、コメント規約の「現在のコードの Why を記述し、履歴は ADR / Git に置く」に反します。

**推奨修正**: 「Fragment に依存せず、ComponentActivity のみのホストでも提示できることを検証する」など、現在保証する条件だけを記述してください。

## アクションプラン

1. `TimeSelectionSheet` をホスト lifecycle に結び付け、Activity 破棄時に明示的に閉じる。
2. 回転テストを実際の View 階層と `isShowing` の検証へ修正する。
3. 未使用 ID と履歴コメントを整理する。
4. 修正後、提示済みの Android 全テストに加え、時刻シート表示中の `Activity.recreate()` を再確認する。

依頼どおり静的レビューのみを行い、ビルド・テストは再実行していません。また、レビュー結果ファイルも作成していません。

## 突き合わせ結果 (2026-08-27)

ホスト側 review-001.md (CHANGES_REQUESTED: Major 1 / Minor 4 / Suggestion 3) との突き合わせ。

| 指摘 | 状況 | 採否 |
|---|---|---|
| TimeSelectionSheet のホスト破棄追随なし + 回転テストの検出力不足 | 双方一致 (相方 Major / ホスト Minor-3) | **確定** — 高い方 (Major) を採用。DateCalendarDialog と同じ lifecycle アンカー方式で修正 |
| 未使用 id ks_date_picker_today_button の残存 | 双方一致 (Minor) | **確定** |
| テストコメントの実装履歴依存 (DateCalendarDialogTest / TimeSelectionSheetTest) | 相方のみ・根拠強 (comment 規約の実在) | **採用** |
| カレンダー選択面のバックグラウンド遷移喪失 (onSaveInstanceState の無条件 dismiss) | ホストのみ (Major) | 採用 (ホスト側指摘) |
| CustomCell テストの本番配線不通 | ホストのみ (Minor) | 採用 |
| 旧証跡の併存 (媒体規約) | ホストのみ (Minor) | 採用 |

採用 6 (うち双方一致 2) / 降格 0 / 未解決・矛盾 0。
