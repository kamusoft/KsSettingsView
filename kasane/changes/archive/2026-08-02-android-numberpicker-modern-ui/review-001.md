# レビュー結果: android-numberpicker-modern-ui (001 回目)

**日付**: 2026-08-02
**判定**: CHANGES_REQUESTED

## サマリー

デルタスペックの 6 Requirement / 25 Scenario はすべて実装とテストで裏付けられており (詳細は `verify-001.md`)、ビルド・テストとも成功 (1294 件・失敗 0)。ホイール部品の切り出しとシート意匠の共通化 (`SheetChrome.kt`) は ADR-0007 の「DatePicker へ再利用する」意図に沿った妥当な設計で、境界値 (min > max / Int 上限 / max 付近のオーバーフロー) の堅牢化も丁寧に行われている。

一方で、本 change が編集した KDoc ブロック内に `concepts/cross/conventions/comment-policy.md` の禁止参照が残っており (同種の参照は `NumberPickerCellViewHolder.kt` では正しく除去されているため対応が不揃い)、これを Major として差し戻す。加えてテストフックの経路乖離とアクセシビリティ通知の欠落を Minor として挙げる。

## 指摘事項

### [🟠 Major] 編集した KDoc ブロックにコメント規約の禁止参照が残っている

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/NumberPickerCell.kt:10-13`

**問題点**:

`NumberPickerCell` のクラス KDoc は本 change で編集されている (`:19` に `@property unit` を追加、`:22` の `@property valueText` を書き換え)。しかし同じ KDoc ブロックの冒頭に、`comment-policy.md` が「禁止する参照」として明示している類型がそのまま残っている。

```
 * 仕様: openspec/changes/add-cell-types-input/specs/cell-types-input/spec.md
 *   "NumberPickerCell" Requirement / ...
 * 設計: openspec/changes/add-cell-types-input/design.md Decision 3 / Decision 8。
```

- アーカイブ文書のパス (`openspec/changes/.../spec.md` / `design.md`)
- Decision 番号 (`Decision 3` / `Decision 8`)

規約の「適用契機」は「新規コメントを書くとき・**既存コメントに触れる実装をするとき**・コードレビューのとき」であり、編集した当のブロックはこれに該当する。実装は `NumberPickerCellViewHolder.kt` では同種の参照 (`仕様: openspec/changes/add-cell-types-input/...`) を正しく削除しており、判断基準は理解されているのに `NumberPickerCell.kt` だけ取り残された形になっている。

**推奨修正**: `comment-policy.md` の「定型句型」の扱いに従い `仕様:` / `設計:` の 2 行 (`:10-13`) を削除する。残す価値のある設計根拠があれば `android/ADR-0007` の許容形式で書き直す。

**同種の残存 (本 change で触れた他ファイル。同一サイクル内で併せて掃除することを推奨)**:

- `android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/InputCellDsl.kt:24-28`
- `android/ks-settingsview-compose/src/test/kotlin/jp/kamusoft/kssettingsview/compose/InputCellDslTest.kt:25`
- `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/InputCellsDemoScreen.kt:35,38`

いずれも本 change で編集済みのファイル内に閉じており、削除は数行で済む。範囲を「触れたファイルの内側」に限れば本サイクルで完結できる。

---

### [🟡 Minor] テストフック `bindRow` が実バインド経路を共有していない

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsWheelView.kt:328-332` と `:348-350`

**問題点**:

検証用フック `bindRow(index)` は `createRow()` に `row.text = displayItems[index]` を**自前で**代入しており、実際の描画経路である `ItemsAdapter.onBindViewHolder` (`row.text = displayItems[position]`) とは別のコードになっている。

候補表示に関わる Scenario (「選択面の候補表示にも同じフォーマットを適用する」「valueText 明示指定は候補表示に影響しない」「候補は step 刻みで昇順に列挙される」ほか) の文字列アサーションは、`NumberSelectionSheetTest.kt:79-80` の `candidateTexts` 経由で**すべてこのフック側**を通っている。`KsWheelViewTest.kt:112-114` も同様。実レイアウト済みの行から文字列を読むテスト (`rowViewAt(i).text`) は 1 件も無いため、`onBindViewHolder` 側だけが壊れた場合に検出できない。

同じリポジトリの先行実装 `PickerSelectionSheet.kt` では、検証用フック (`:531-533`) が本番の private `bindRow(row, position)` (`:542`) を呼び、`onBindViewHolder` (`:561`) も同じ関数を呼ぶ形で経路を共有している。`KsWheelView` だけがこの確立済みパターンから外れている。

**推奨修正**: `private fun bindRow(row: TextView, position: Int)` を切り出し、`onBindViewHolder` と検証用フックの双方から呼ぶ (`PickerSelectionSheet` と同じ構造にする)。あるいは最低 1 件、`rowViewAt(index)?.text` で実レイアウト後の行文字列を検証するテストを足す。

---

### [🟡 Minor] 選択中候補が変わってもアクセシビリティイベントを送出しない

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsWheelView.kt:242-248` (`updateSelection`)

**問題点**:

選択が変わったとき `contentDescription` は更新されるが、`AccessibilityEvent` を一切送出していない。加えて `listView` は `IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS` (`:123`) のため、`RecyclerView` が本来出す `TYPE_VIEW_SCROLLED` も抑止される。結果として、利用者がホイールを回して候補が切り替わっても支援技術側に変化の通知が届かず、フォーカスを当て直さない限り新しい値が読み上げられない可能性が高い。旧実装の `android.widget.NumberPicker` は値変更時に自前でイベントを送っていたため、体験の後退にあたる。

デルタスペックの文言 (「公開される状態も更新される」) は満たしているため spec 違反ではないが、Requirement がアクセシビリティを明示的に契約している以上、実効性を伴わせるべき箇所と考える。

**推奨修正**: `updateSelection` で選択が変わったときに `sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED)` (または `TYPE_WINDOW_CONTENT_CHANGED` + `CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION`) を送る。検証は Robolectric の `ShadowAccessibilityManager` / `getSentAccessibilityEvents` 相当で可能。

---

### [🔵 Suggestion] 候補の eager 生成は現実的な上限が緩い

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/NumberPickerCellViewHolder.kt:100` と `:63`

**問題点**: `List(count.toInt()) { ... }` はボックス化された `List<Int>` を全件生成し、続く `candidates.map { NumberPickerCell.format(...) }` が全件分の `String` を生成する。デルタスペックが定める提示拒否の閾値は `Int` の表現上限 (2^31 − 1) なので、例えば `min = 0, max = 5_000_000, step = 1` は閾値を通過したうえで数百 MB 規模の割り当てを起こしうる。

ただし **これは spec が定めた契約どおりの実装**であり、旧実装 (`generateSequence(...).toList()` + `map { it.toString() }.toTypedArray()`) も同じ eager 生成だったため退行ではない。閾値の見直しは spec 側の判断になるため、ここでは指摘に留める。

**推奨修正 (任意)**: 候補列を遅延化する (index → 値 / 表示文字列の関数を `KsWheelView` に渡し、`getItemCount` を件数だけで表現する) と、`Int` 上限まで安全に扱えるようになる。閾値そのものを下げたい場合は spec の改訂が必要なため、変更フローで扱う。

---

### [🔵 Suggestion] `PickerSheetStyle.from` の 2 オーバーロードが完全重複している

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/PickerSelectionSheet.kt:80-95` と `:99-114`

**問題点**: `PickerCell` 版と `NumberPickerCell` 版で、本体 8 行が `cell.accentColor` を読む以外まったく同一。片方だけ更新される劣化に弱い。

**推奨修正 (任意)**: `private fun from(cellAccent: Color?, theme: Theme, effective: EffectiveStyle)` を用意し、2 つの公開オーバーロードから委譲する。

---

### [🔵 Suggestion] `SelfContainedRecyclerView` の `open` は不要

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SheetChrome.kt:322`

**問題点**: 抽出時に `internal open class` になっているが、リポジトリ内に派生クラスは存在しない (`PickerSelectionSheet` / `KsWheelView` とも直接インスタンス化している)。継承前提でない型は閉じておくのが Kotlin の既定挙動に沿う。

**推奨修正 (任意)**: `open` を外す。将来 DatePicker ホイール版で派生が必要になった時点で開ける。

## 確認したが指摘に至らなかった観点

- **仕様充足**: 6 Requirement / 25 Scenario すべてに実装とテストの対応あり (`verify-001.md`)。tasks.md に虚偽チェックなし。足場 (proposal / spec / mock) の逆流書き換えなし。deviation.md 無しと整合し、未記録乖離も検出せず
- **Non-Goals の遵守**: `PickerSelectionSheet` は 349 行が削られているが、内容はドラッグハンドル・ヘッダー・`SelfContainedRecyclerView` の `SheetChrome.kt` への機械的な移設であり、挙動を変える差分は見つからなかった (初期化順序も `workingSelection` → `headerView` の順で保たれており、確定 lambda はクリック時に作業状態を読む形のまま)。既存の `PickerSelectionSheetTest` 全件が通っている
- **境界値**: `min > max` / 候補件数 Int 上限超過 / `max` 付近の step 加算オーバーフロー / `step <= 0` / `value` が候補外・範囲外 / 候補 0 件時の `selectedIndex = -1` (`coerceIn(0, -1)` を踏まない分岐) をコード上で確認。いずれも安全側
- **スタイル解決**: 強調色が「Cell 固有値 → CellStyle → Theme」の既存契約に乗っており、3 分岐すべてに実レイアウト後の文字色アサーションがある
- **dismiss 経路**: 確定は `confirmView` のみ、その他 4 経路 (キャンセル / 外側タップ / Back / 下スワイプ) はいずれも callback を通らない構造。`STATE_HIDDEN` への settle まで含めた実挙動テストあり
- **候補領域のスクロール伝播**: `SelfContainedRecyclerView` により nested scroll の開始と fling 伝播を止める設計で、`BottomSheetBehavior` の `findScrollingChild` は `KsWheelView` 配下の `listView` に到達する (帯・フェードは非スクロール View)。実 MotionEvent ドラッグのテストで裏付けあり
- **視覚照合**: `ui/brief.md` に承認モックの記録と 2026-08-02 の照合結果 (`verification/` 4 枚・検証条件 9 項目 OK・合意済み妥協 0 件) が残っている。「unit 未指定の見た目はサンプルに該当 Cell が無く実機未照合」という限界も明記されており、証跡として誠実
- **コメント (新規分)**: 新規追加されたコメントは `<domain>/ADR-NNNN` 形式のみを外部参照に使っており、spec 構文キーワード・変更提案パス・レビュー通番の混入なし。「承認モックの 44dp」等の表現は先行 change (`PickerSelectionSheetTest`) で既に確立している書き方のため指摘しない
- **サンプル**: `samples/android` は別 composite build のため `android` 側の `./gradlew test` に含まれない。別途 `:app:compileDebugKotlin` を実行して成功を確認した

## アクションプラン

1. **[Major]** `NumberPickerCell.kt:10-13` の禁止参照を削除する (規約の「定型句型」処理)
2. **[Major の付随]** 同じく本 change で触れた `InputCellDsl.kt:24-28` / `InputCellDslTest.kt:25` / `InputCellsDemoScreen.kt:35,38` の禁止参照も併せて削除する
3. **[Minor]** `KsWheelView` の行バインドを `onBindViewHolder` と検証用フックで共有させる (または実レイアウト後の行文字列アサーションを追加する)
4. **[Minor]** 選択変更時にアクセシビリティイベントを送出し、テストで裏付ける
5. **[Suggestion]** 候補生成の遅延化 / `PickerSheetStyle.from` の重複解消 / `open` の除去 — 任意。1〜4 と同時に入れても差分は小さい

修正後は `cd android && ./gradlew test` に加え、`cd samples/android && ./gradlew :app:compileDebugKotlin` の再実行を推奨する。
