# セカンドオピニオン: android-picker-selection-sheet (code-review / 対応ホスト側レビュー: review-004)
**相方**: codex / **日付**: 2026-08-02 / **対象**: 修正サイクル4反映後の実装 diff
---
# 最終レビュー結果

**判定: CHANGES_REQUESTED**

Critical 0 / Major 1 / Minor 0 / Suggestion 0。

nested scroll遮断方式そのものは妥当です。RecyclerViewのローカルなタッチスクロール・flingは維持され、リスト操作からシートへの伝播だけが止まります。ハンドル／ヘッダー起点の直接ドラッグと下スワイプdismissも遮断されていません。

ただし、折り畳みへ戻った場合の高さ管理に問題があります。

### 🟠 Major: 直接ドラッグ後にCOLLAPSEDへ戻ると、候補末尾へ到達できなくなる

**該当箇所**: [PickerSelectionSheet.kt:201](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/PickerSelectionSheet.kt:201)、[PickerSelectionSheet.kt:527](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/PickerSelectionSheet.kt:527)

**問題点**: `STATE_DRAGGING` に入った時点でリスト高を `WRAP_CONTENT` に戻し、その後は制約を復元しません。そのため、次の経路でシートが折り畳み状態なのにRecyclerViewだけが展開時の高さを保持します。

1. ハンドルを短くドラッグし、展開・dismissせず `COLLAPSED` に戻る
2. 一度展開したシートを直接ドラッグで `COLLAPSED` に戻す
3. 下方向ドラッグがdismiss閾値に届かず `COLLAPSED` へ戻る

この状態ではRecyclerViewが画面外まで続く大きなviewportとして測定されますが、実際に見えるのはpeek部分だけです。RecyclerViewの最大スクロール量は自身の大きな測定高を基準に計算されるため、下側の候補を可視領域まで持ち上げられなくなる可能性があります。「リストは常に内部スクロール」という裁定を満たせません。

**推奨修正**: 初期制約高を保持し、状態遷移を次のように対称化してください。

```kotlin
STATE_DRAGGING -> リスト高制約を解除
STATE_COLLAPSED -> 初期制約高を再適用
```

再度直接ドラッグされた場合は、その都度制約を解除します。併せて以下の実 `MotionEvent` テストを追加するのが安全です。

- 短い上下ドラッグで `COLLAPSED` に戻った後も末尾候補までスクロールできる
- `EXPANDED → COLLAPSED` 後もリスト高がpeek用制約へ戻る
- ハンドル起点の十分な下ドラッグでは `HIDDEN` へ遷移し、callbackを発火せずdismissする

候補行の実効文字サイズ適用とヘッダー文字サイズのmock合わせには、新たな問題は確認できません。報告された全1182件成功および実MotionEvent実測結果は採用し、テストは再実行していません。

## 突き合わせ結果 (ホスト側判定: 2026-08-02)

- 双方 CHANGES_REQUESTED で、**Major は同一指摘** (解除一度きり・COLLAPSED 復帰時の再制約なし → 折り目下の候補へ内部スクロールで到達不能)。ホスト側は「ハンドル 80px 下ドラッグで発生・末尾約7候補が選択不能」の実測付き → **確定 (Major)**
- 推奨修正も同一: STATE_DRAGGING で解除 / STATE_COLLAPSED で再制約の対称化
- 前回指摘 (展開トリガー / 文字サイズ) の解消は双方一致
- ホストのみの Suggestion 2件 (ADR-0005 の追補矛盾・「Material の標準挙動」文言) は蒸留申し送りで継続
