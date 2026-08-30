# 代理値アサート・実経路を通らないテスト (L-001 の経緯)

`lessons/test.md` L-001 の詳細。inbox パターン (count 3、2026-08-05 昇格) から移設。

## 経緯

- 2026-08-02 android-picker-selection-sheet: (1) タップ領域テストが `confirmSlot.measuredWidth` を見て実タップ領域の横 33dp を見逃す (review-002)。(2) 初期スクロールのテストが実際には発生しない `AT_MOST 600px` 制約で測り、実レイアウトで効かないことを検出できず (review-002)。(3) 全展開テストが `behavior.state = STATE_EXPANDED` を直接代入し、誤発火する nested scroll 経路を1件も通さず (review-003)。(4) 折り目→展開の一方向のみで、復帰時の非対称破綻を検出できず (review-004)。いずれも実 `MotionEvent` / 実ダイアログ階層の実測レビューで発覚した。
- 2026-08-02 android-numberpicker-modern-ui: (1) `KsWheelView` の検証フック `bindRow(index)` が `onBindViewHolder` と別コードで文字列を組み立てており、候補表示の Scenario アサーションが全てフック経由 — 実描画側だけが壊れても検出不能 (review-001 Minor。先行 `PickerSelectionSheet` の経路共有パターンから逸脱していた)。(2)「移動中の確定は直前にスナップ静止した候補を採用する」が実ドラッグ→確定ボタンの操作列でなく、(a) 移動中は selectedIndex 不変 (b) 確定は selectedIndex 採用、の2テスト合成のまま (review-002 Suggestion)。
- 2026-08-05 fix-android-accessory-header-refresh: ヘルパ `bindSectionText` / `bindCellTitle` が新規 ViewHolder を作って `onBindViewHolder` を直接呼ぶ設計のため、「新しい ViewHolder に bind すれば新しい値が出る」ことは通知機構と無関係に常に成り立ち、Scenario 1:1 対応の positive テスト 2 件が修正前コード (HEAD) でも green だった (review-001 Major。code-review L-001 のミューテーション実測で発覚)。保証の実体は payload 付き変更通知の発行であり、`NotificationRecorder` の `ChangeRecord` 照合を追加して解消。
