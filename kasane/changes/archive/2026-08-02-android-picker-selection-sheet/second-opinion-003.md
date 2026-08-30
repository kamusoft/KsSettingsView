# セカンドオピニオン: android-picker-selection-sheet (code-review / 対応ホスト側レビュー: review-002)
**相方**: codex / **日付**: 2026-08-02 / **対象**: 修正サイクル2反映後の実装 diff
---
# 再レビュー結果: android-picker-selection-sheet

**判定**: **CHANGES_REQUESTED**  
**件数**: Critical 0 / Major 2 / Minor 1 / Suggestion 0

ホスト報告の `./gradlew test --rerun-tasks` BUILD SUCCESSFUL、1162件成功を前提とし、テストは再実行していません。初期スクロールは deviation.md 記録済み、`KsSimpleCheckView` 再利用はADR追補で承認済みとして扱いました。

## 前回指摘の解消状況

| 前回指摘 | 状況 | 確認結果 |
|---|---|---|
| haptic fallbackが`false`を扱わない | **解消** | 戻り値を判定して`KEYBOARD_TAP`へfallbackし、成功・失敗両方のテストが追加されています。 |
| 長いタイトル・ロケールで操作ラベルが切れる | **部分解消** | ラベルの希望幅を確保しタイトルを先に縮める構造になりました。ただし小画面＋大きなfont scaleでは対称スロット自体が画面幅を超えます。 |
| 操作要素の48dp領域・button semantics不足 | **部分解消** | button semanticsと縦方向48dpは追加されましたが、実際の横方向タップ領域は48dpを保証していません。 |
| 下スワイプ経路を`dismiss()`で代用 | **解消** | `BottomSheetBehavior.STATE_HIDDEN`への遷移とdismiss、callback不発火を検証しています。外側タップ設定も明示されました。 |
| `KsSimpleCheckView`がADRと不一致 | **解消** | ADR追補によるオーナー承認済みのため指摘対象外です。 |

## 現在の指摘事項

### [🟠 Major] 大きなfont scaleでは対称スロットが画面幅を超える

**該当箇所**: [PickerSelectionSheet.kt:314](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/PickerSelectionSheet.kt:314)、[PickerSelectionSheetTest.kt:271](android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/PickerSelectionSheetTest.kt:271)

**問題点**: 左右を常に`max(cancelWidth, confirmWidth)`の対称幅にするため、320dp程度の画面でfont scaleを大きくすると、`左右スロット×2 + 32dp padding`がヘッダー幅を超えます。その場合、タイトルが0幅になった後も不足分が残り、右側操作が画面外へクリップされます。

追加テストの狭幅は常に`左右スロット幅×2 + padding`より広く生成されており、font scaleや長いロケールで「操作領域だけで画面幅を超える」条件を検証していません。

**推奨修正**: 通常時だけ対称幅とし、収まらない場合は左右を各ラベルの固有幅へ戻す、タイトルを別段へ移すなどのレスポンシブなfallbackを設けてください。320dp、font scale 2.0、長いOSラベルの組み合わせで、両操作が画面内に収まることをテストしてください。

### [🟠 Major] 48dpと判定しているスロット全体がタップ可能ではない

**該当箇所**: [PickerSelectionSheet.kt:322](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/PickerSelectionSheet.kt:322)、[PickerSelectionSheet.kt:392](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/PickerSelectionSheet.kt:392)、[PickerSelectionSheetTest.kt:228](android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/PickerSelectionSheetTest.kt:228)

**問題点**: テストは`cancelSlot`の幅を確認していますが、クリック可能なのは`cancelView`だけです。取消ラベルが短いロケールでは、スロットが48dpでも実際の横方向タップ領域は48dp未満になります。

確定側の`TouchDelegate`も範囲が`target.left..target.right`であり、スロット全幅ではなく縦方向だけを拡張しています。取消側には`TouchDelegate`自体がありません。

**推奨修正**: 両操作Viewへ`minWidth = 48dp`を設定するか、両スロットで次のように全領域を委譲してください。

```kotlin
val bounds = Rect(0, 0, slot.width, slot.height)
slot.touchDelegate = TouchDelegate(bounds, target)
```

テストもスロット寸法ではなく、実際のクリック対象または`TouchDelegate`範囲が48×48dp以上であることを確認してください。

### [🟡 Minor] タッチ領域修正でヘッダーがmockより大幅に高くなる

**該当箇所**: [PickerSelectionSheet.kt:244](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/PickerSelectionSheet.kt:244)、[PickerSelectionSheet.kt:328](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/PickerSelectionSheet.kt:328)

**問題点**: スロットを48dp高にしたうえで、ヘッダーに上6dp・下12dpのpaddingを追加するため、ヘッダー全体は最低66dpになります。承認mockは操作要素の自然高に同じpaddingを加えた構成であり、修正前より約18dp高くなることがコード上確定しています。「寸法を変えずにタップ領域を確保する」というコメントとも一致しません。

**推奨修正**: 48dpをヘッダー全体の操作領域として扱い、既存paddingを含めた総高で確保してください。見た目の子Viewはmock寸法に保ち、親ヘッダーから`TouchDelegate`で領域を拡張する方法が適しています。

## 新規変更の確認

初期スクロールは、有効な単一選択index／複数選択の最小有効indexを使用し、範囲外値を無視しています。deviation.mdと実装・テストは整合しており、この追加による新たな状態管理上の問題は確認できませんでした。

指定どおりファイルへの書き込みは行っていません。

## 突き合わせ結果 (ホスト側判定: 2026-08-02)

ホスト側 = review-002.md (CHANGES_REQUESTED, Major 1 / Minor 2 / Suggestion 1)。

| # | 指摘 | 出典 | 採否 |
|---|---|---|---|
| 1 | 実タップ領域の横方向が 48dp 未満 (取消に TouchDelegate なし / 確定は縦のみ拡張。テストは代理値) | 双方一致 | **確定** (Major — 相方の高い方) |
| 2 | ヘッダーが mock より約 18dp 高い | 双方一致 (実装ワーカー申し送りとも一致) | **確定** (Minor)。修正方針は相方推奨の「子 View を mock 寸法に保ち親スロット全域を TouchDelegate 委譲」を採る — #1 と同時解決 |
| 3 | 小画面 + 大 font scale で対称スロットが画面幅超過 | 相方のみ | **採用** (根拠強: 320dp × fontScale 2.0 で算術的に成立) |
| 4 | 初期スクロールが実レイアウトでほぼ効かない (deviation 合意挙動が成立していない) | ホストのみ | **確定** (Major — 実ダイアログ階層の実測付き) |
| 5 | ADR-0005 本文と追補の文言矛盾 | ホストのみ (Suggestion) | **採用 (蒸留時対応)** — ksn-distill への申し送り |

前回指摘の解消状況は両者一致: haptic フォールバック / STATE_HIDDEN 経路 / シート面 tint / onStart 巻き戻し / 実 bind 経路 / 小掃除 = 解消。タップ領域のみ部分解消で継続。
