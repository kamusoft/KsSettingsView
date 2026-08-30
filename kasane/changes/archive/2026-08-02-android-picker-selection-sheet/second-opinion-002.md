# セカンドオピニオン: android-picker-selection-sheet (code-review / 対応ホスト側レビュー: review-001)
**相方**: codex / **日付**: 2026-08-02 / **対象**: 実装 diff (PickerSelectionSheet.kt ほか4ファイル)
---
# レビュー結果: android-picker-selection-sheet

**日付**: 2026-08-02  
**判定**: **CHANGES_REQUESTED**  
**件数**: Critical 0 / Major 3 / Minor 2 / Suggestion 0

## サマリー

主要な選択状態遷移、非正規化、スタイル解決、候補行のアクセシビリティ状態はデルタスペックと概ね整合しています。一方、対応 OS 範囲で haptic が発生しない経路と、ヘッダー操作のレイアウト・アクセシビリティに修正必須の問題があります。

ホスト報告の `./gradlew test --rerun-tasks` BUILD SUCCESSFUL、1134件成功を前提とし、テストは再実行していません。

## 指摘事項

### [🟠 Major] haptic のフォールバックが通常の失敗時に実行されない

**該当箇所**: [PickerSelectionSheet.kt:450](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/PickerSelectionSheet.kt:450)

**問題点**: `performHapticFeedback()` は未対応の feedback type などでは例外ではなく `false` を返します。現在は例外だけを捕捉しているため、`REJECT` が実行されなかった場合も `KEYBOARD_TAP` を試しません。特に minSdk 29 を含む対応範囲で、上限到達時に実際の触覚フィードバックが得られない可能性があり、明示要件に反します。

**推奨修正**:

```kotlin
if (!view.performHapticFeedback(HapticFeedbackConstants.REJECT)) {
    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
}
```

戻り値が `false` の経路を再現できるテスト seam を設け、fallback が呼ばれることも検証してください。

### [🟠 Major] 長いタイトルやロケールによって操作ラベルが切り詰められる

**該当箇所**: [PickerSelectionSheet.kt:257](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/PickerSelectionSheet.kt:257)

**問題点**: タイトルを画面幅の60%まで許容した後、残り幅を左右スロットへ等分しています。左右スロットは Cancel/OK の実測幅を考慮しないため、長い `pageTitle`、狭い画面、大きな font scale、長い OS ローカライズ文字列の組み合わせで、特にキャンセルラベルがクリップされます。「タイトルと OS ロケールの操作ラベルを表示する」という契約を有効な入力範囲で満たせません。

**推奨修正**: 実際のシート幅を基準に操作ラベルを先に測定し、左右スロットへ `max(cancelWidth, confirmWidth, 48dp)` の対称幅を確保したうえで、残り幅だけをタイトルへ割り当てて ellipsize してください。小画面・長いタイトル・fontScale 2.0・長いロケール文字列のレイアウトテストも追加してください。

### [🟠 Major] キャンセル・確定操作が最小タッチ領域とボタン semantics を満たさない

**該当箇所**: [PickerSelectionSheet.kt:236](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/PickerSelectionSheet.kt:236)、[PickerSelectionSheet.kt:261](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/PickerSelectionSheet.kt:261)

**問題点**: raw `TextView` 自体をクリック対象にしており、縦 padding は6dpだけで `minimumHeight` もありません。通常 font scale では48×48dpの最小タッチ領域を下回ります。またアクセシビリティ上のクラスは `TextView` のままで、操作ボタンとしての role が公開されません。

**推奨修正**: 見た目を維持できる `MaterialButton` の text/filled variantを使うか、少なくとも48dpの最小領域、Ripple、`Button` 相当の accessibility class/actionを付与してください。測定後サイズと `AccessibilityNodeInfo` を検証するテストも必要です。

### [🟡 Minor] 下方向スワイプのテストが実際の BottomSheet 経路を通っていない

**該当箇所**: [PickerSelectionSheetTest.kt:138](android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/PickerSelectionSheetTest.kt:138)、[tasks.md:18](kasane/changes/android-picker-selection-sheet/tasks.md:18)

**問題点**: 「下方向スワイプ」の分岐は単に `swipe.dismiss()` を呼んでおり、`BottomSheetBehavior` の `STATE_HIDDEN` 遷移や、スワイプによる dismiss が有効であることを検証していません。外側タップも `cancel()` の直接呼び出しです。この状態で「全経路のテスト」を完了扱いにするのは過大です。

**推奨修正**: 少なくとも behavior を `STATE_HIDDEN` へ遷移させ、dialog の dismissalと callback 不発火を確認してください。Robolectricで再現できない経路は、タスクの完了範囲を実態に合わせて実機検証側へ残してください。

### [🟡 Minor] ADRで決定された vector drawable ではなく Canvas View を使用している

**該当箇所**: [PickerSelectionSheet.kt:387](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/PickerSelectionSheet.kt:387)、[ADR-0005:27](kasane/decisions/android/0005-pickercell-selection-ui-bottom-sheet.md:27)

**問題点**: ADR・proposal・tasks は「vector/custom drawable を accentColor で tint」と決定していますが、実装は `KsSimpleCheckView` による Canvas 直接描画です。見た目が近くても、deviation.md がない状態では記録されていない設計逸脱であり、tasks 1.2 の完了表明とも一致しません。

**推奨修正**: ADRどおり vector drawable + tint へ変更してください。既存 Canvas View の再利用を正式に選ぶのであれば、足場を書き換えず deviation として判断を記録し、合意を得てください。

## アクションプラン

1. haptic の戻り値処理を修正し、fallback テストを追加する。
2. ヘッダーを操作ラベルの実測幅・48dpタッチ領域・ボタン semantics に対応させる。
3. 実際の BottomSheet dismiss 経路をテストする。
4. 選択印の drawable 方針をADRへ合わせるか、乖離として合意・記録する。

指定どおりレビュー結果ファイルへの書き込みは行っていません。

## 突き合わせ結果 (ホスト側判定: 2026-08-02)

ホスト側 = review-001.md (CHANGES_REQUESTED, Minor 3 / Suggestion 7)。

| # | 相方の指摘 | 採否 | 判定根拠 |
|---|---|---|---|
| C1 | haptic フォールバックが false 戻り値で実行されない (Major) | **採用** | 相方のみ + 根拠強。`performHapticFeedback` は失敗時 false を返し例外ではない。`REJECT` は API 30 追加のため旧 API で無音になる実害シナリオが具体的 |
| C2 | 長タイトル・ロケールで操作ラベルが切り詰め (Major) | **確定** (ホスト Minor 2 と一致。相方が Major 主張のため Major) | 双方一致 |
| C3 | タップ領域 48dp 未満 + Button semantics 欠如 (Major) | **確定** (ホスト Minor 1 = 必須修正と一致。semantics 分は相方の追加根拠として統合) | 双方一致 |
| C4 | 下スワイプテストが実経路を通らない (Minor) | **確定** (ホスト Suggestion「dismiss テストの前提未担保」と同根。STATE_HIDDEN 遷移 + canceledOnTouchOutside 検証を追加) | 双方一致 (部分) |
| C5 | ADR の vector drawable でなく Canvas View (Minor) | **未解決 → オーナー判断へ** | ホスト側は「既存チェック表現と揃い問題なし」、相方は「未記録の設計逸脱」— 見解が割れた。実装の物ではなく記録の問題のため、再提示ではなくオーナー裁定に直接倒す |

ホスト側のみの指摘 (シート背景透明化による elevation 外形不一致 Minor 3、onStart 状態リセット・画面幅測定・実 bind 経路テスト・裸参照コメント・未使用フィールド等の Suggestion) はホスト側判定のまま修正対象に含める。単一選択の初期スクロール位置 (ホスト Suggestion・留保) はオーナー判断事項。
