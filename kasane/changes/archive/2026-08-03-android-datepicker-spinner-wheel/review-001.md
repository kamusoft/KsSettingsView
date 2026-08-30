# レビュー結果: android-datepicker-spinner-wheel (001 回目)

**日付**: 2026-08-02
**判定**: CHANGES_REQUESTED

## サマリー

デルタスペックの 8 Requirement / 27 Scenario に対し、実装・テストともに高い網羅度で対応している。`DateCandidates` による候補範囲の記述子化 (候補列を実体化せず index 解決) と、`KsWheelView.setCandidates` / `setSelectedIndex` の通知契約の分離 (差し替えでは通知せず、静止・a11y・プログラム移動では通知する) は設計として筋がよく、再入防止も `isSyncing` と二重に固めてある。丸めロジック (末日丸め → 範囲端丸め) は境界年・境界月・閏年のいずれも正しく、`resolve` が年・月を動かさないことも候補の制限によって保証されている。テストは 743 件すべて green (failures=0 / errors=0、`--rerun-tasks` で再実行して確認)。

一方で、機械 lint 化済みのソースコメント規約に新規コメント 6 箇所が抵触している (Major)。加えて、候補位置へのプログラム的スクロールが進行中の fling を止めないため、慣性移動中に「今日」ジャンプ等を行うと選択が上書きされうる経路が残っている (Minor)。いずれも修正は局所的で、設計の作り直しは不要。

## 確認した観点

- ビルド / テスト: `:ks-settingsview-ui` `:ks-settingsview-compose` `:ks-settingsview-core` の `testDebugUnitTest` を `--rerun-tasks` で実行し BUILD SUCCESSFUL。合計 743 tests / skipped 0 / failures 0 / errors 0。うち `DateSelectionSheetTest` 41 件・`KsWheelViewTest` 28 件が本変更の新規・追加分
- 足場アーティファクト: `proposal.md` / `specs/settings-view-android-ui/spec.md` は未変更 (diff なし)。書き換えは `tasks.md` / `ui/brief.md` のみで凍結違反なし
- tasks.md の虚偽チェック: なし。4.1 のみ未チェックで、理由が設計メモに明記されている (他 20 項目は実体を確認)
- 視覚照合: `ui/verification/` の 5 枚は本日 23:11 生成で、`brief.md` の照合結果表と対応している (陳腐化なし)
- 既存挙動の回帰: `updateSelection` のシグネチャ変更 (`notify` 追加) と `showsBand` / `seriesLabel` の追加はいずれも既定値で従来動作を保存しており、`NumberSelectionSheetTest` / `PickerSelectionSheetTest` も green
- 公開 API: `todayText` の挿入位置は `uiStyle` 直後で proposal の宣言どおり。`equals` / `hashCode` / DSL overload すべてに反映済み

---

## 指摘事項

### [🟠 Major] 新規コメントがソースコメント規約の禁止参照に抵触する

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DateSelectionSheet.kt:412`, `:651`, `:654`, `:657`, `:663`, `:666`

**問題点**:
`kasane/concepts/cross/conventions/comment-policy.md` は、アーカイブされる作業資料 (変更配下の `ui/mock/` を含む) への参照をコメントに書くことを禁じている。この規約は `.claude/hooks/comment-policy-check.py` で機械検査化されており (`lessons/rejected.md` の 2026-08-02 エントリ参照)、新規ファイルに対して実際に 6 件が検出される:

```
// 「今日」を出さないときは区切り線ごと構成から外す（承認モックの右フレーム）。
/** 内容下の余白（dp）。承認モックの 14dp。 */
/** 「今日」chip の左右 padding（dp）。承認モックの 20dp。 */
/** 「今日」chip の上下 padding（dp）。承認モックの 6dp。 */
/** 「今日」行の上 padding（dp）。承認モックの 10dp。 */
/** 「今日」行の下 padding（dp）。承認モックの 4dp。 */
```

「承認モックの N dp」は、モック画像がアーカイブされた後に検証不能になる参照であり、そのファイルだけを読む人にとって意味が閉じない (規約の最低条件を満たさない)。

**推奨修正**:
規約の「書き換え時の判断基準」1 (定型句型) に従い、参照句を落として自己完結した説明にする。値そのものは定数名と数値で自明なので、由来の記述は不要:

- `/** 内容下の余白（dp）。承認モックの 14dp。 */` → `/** 内容下端とシート下端のあいだの余白（dp）。 */`
- `// 「今日」を出さないときは区切り線ごと構成から外す（承認モックの右フレーム）。` → `// 「今日」を出さないときは区切り線ごと構成から外す。`

なお `KsWheelView.kt:488` 付近の同型コメント (「承認モックの 44dp」等) は本変更の diff 外の既存記述であり、必須ではないが同じファイルに触れている以上、ついでに掃くのが望ましい (規約の適用契機「既存コメントに触れる実装をするとき」)。

---

### [🟡 Minor] 候補位置へのプログラム的スクロールが進行中の慣性移動を止めない

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsWheelView.kt:283-306` (`setCandidates` / `setSelectedIndex`)

**問題点**:
両メソッドとも `wheelLayoutManager.scrollToPositionWithOffset(index, 0)` を **LayoutManager に対して直接**呼んでいる。`RecyclerView.scrollToPosition(int)` は内部で `stopScroll()` を呼んでから LayoutManager へ委譲するが、LayoutManager を直接叩く経路ではこれが行われない。`scrollToPositionWithOffset` は pending position を立てて `requestLayout()` するだけで、`ViewFlinger` が駆動中の慣性移動 (fling) は停止しない。

このため、ある系列が慣性移動中に別経路で選択を移すと、ジャンプ後に fling が続行して別の候補へ着地し、静止時の `commitSnappedSelection` が `onSelectionChanged` を発火して選択を上書きする。到達経路として現実的なのは:

- 年ホイールを勢いよく弾いて慣性移動している最中に「今日」chip をタップする (chip は別 View なので、その ACTION_DOWN では年ホイールの fling は止まらない)。`jumpToToday` → `applySelection` で3列が今日へ移るが、その後に年ホイールだけが流れて着地し、`selectedDate` の年が今日以外になる

デルタスペック「今日へのジャンプ (todayText)」の「3系列の選択中がデバイスの現在日付になる」が、この競合下では満たされない。日付が壊れることはない (`resolve` が常に有効範囲へ丸める) が、利用者の意図しない日付が確定候補になる。

**注記**: これはコードパスからの指摘であり、実機での再現は行っていない。修正前に fling 中のタップで再現確認をとることを勧める。

**推奨修正**:
両メソッドの先頭で fling を止める。1 行で済む:

```kotlin
internal fun setSelectedIndex(index: Int) {
    if (index !in itemIndices) return
    listView.stopScroll()   // 進行中の慣性移動を打ち切ってから位置を移す
    wheelLayoutManager.scrollToPositionWithOffset(index, 0)
    updateSelection(index, notify = true)
}
```

`setCandidates` も同様。テストは「慣性移動中に `setSelectedIndex` を呼び、Looper を進めても選択が移動先に留まる」形で足せる (`KsWheelViewTest` の `smoothScrollBy` + `idleFor` の既存パターンが流用できる)。

---

### [🔵 Suggestion] 非 ISO 暦・非 ASCII 数字の Locale で OS の表記慣行と食い違う

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DateSelectionSheet.kt:82-87` (`formatterFor`)

**問題点**:
ICU の `getBestDateTimePattern` から得たパターンを `DateTimeFormatter.ofPattern(pattern, locale)` に渡しているが、`DateTimeFormatter` の暦は既定で `IsoChronology`、数字は `DecimalStyle.STANDARD` (ASCII) のまま。ICU が返すのはパターン文字列だけで、Locale の既定暦や数字体系は反映されない。結果として:

- `th-TH` (既定暦が仏暦): ホイールは 2026、OS の日付 UI は 2569 と表示され食い違う
- `fa-IR` (既定暦がペルシャ暦) も同様
- `ar` 系: OS はアラビア数字 (٢٠٢٦) を使う場面でホイールは ASCII 数字

デルタスペックの Scenario は ja / en しか規定しておらず契約違反ではないが、Requirement 本文の「端末 Locale の日付表記慣行から導出する」の趣旨からはずれる。

**推奨修正**:
必須ではない。対応するなら `android.icu.text.DateFormat` / `android.icu.util.Calendar` 側で整形して Locale の既定暦・数字体系に乗せる方式が素直だが、`java.time` からの離脱と `DateWheelLabels` の作り直しを伴うため、別 change として扱うのが妥当。当面は「ISO 暦・ASCII 数字で表示する」と `DateWheelLabels` の doc コメントに明記して、意図的な割り切りであることを残すだけでもよい。

---

### [🔵 Suggestion] シート内容が固定高で、横向き・特大フォント時に下端が切れうる

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DateSelectionSheet.kt:422-433` (`onStart`)

**問題点**:
`contentRoot` は非スクロールの `LinearLayout` で、`isFitToContents = true` + `STATE_EXPANDED` で内容高のまま提示する。ハンドル + ヘッダー + ホイール 5 行 (220dp) + 「今日」行 + 下余白でおよそ 370dp 前後になり、横向きの一般的な端末高 (システムバー控除後 ~330dp) を上回る。ヘッダーが上端にあるため確定・取消は残るが、「今日」chip が画面外へ出る可能性がある。フォントスケール最大時も同様。

`NumberSelectionSheet` も同じ器・同じ `onStart` の作りで、この性質は本変更で新たに生じたものではない (「今日」行の分だけ悪化する)。

**推奨修正**:
必須ではない。対応するなら `contentRoot` をヘッダー固定 + 本体スクロールにするか、`BottomSheetBehavior.maxHeight` を意識して可視行数を縮める。`NumberSelectionSheet` と共通の器の問題なので、両者まとめて別 change で扱うのが自然。

---

## 参考情報 (判定には含めない)

### 保留中の 4.1 / 「年候補件数が Int 上限を超える指定」Scenario について

`tasks.md` の設計メモにあるとおり、この Scenario は `LocalDate` の値域では到達不能 (最大 1,999,999,999 件 < `Int.MAX_VALUE`)。オーナー判断待ちの spec 側の課題として扱われているため、本レビューの CHANGES_REQUESTED 理由には含めていない。

判断材料として、コード側から見た実際の限界を記しておく:

- 実装上の律速は年候補の**件数**ではなく、`RecyclerView` / `LinearLayoutManager` の縦スクロール範囲 (`itemCount × rowHeightPx`) が `Int` を溢れる点にある。`ROW_HEIGHT_DP = 44f` は xxhdpi で約 132px なので、**約 1,600 万件 (= 1,600 万年)** を超えたあたりでスクロール範囲の計算が破綻する
- つまり `Int.MAX_VALUE` を閾値にしたガード (`DateSelectionSheet.kt:247-255`) は、到達不能であると同時に、**実際に壊れる水準より 2 桁ゆるい**
- spec を直すなら「`Int` の表現上限」ではなく、提示可能な年数の上限を具体値で規定する (例: 1 万年) 方が、到達可能かつ実害に即した契約になる

なお `DateCandidates.of` のガード自体は防御として無害であり、削除を求めるものではない。

---

## アクションプラン

1. **[Major]** `DateSelectionSheet.kt` の 6 箇所のコメントから「承認モック」参照を除き、自己完結した説明に書き直す。書き込み後に `.claude/hooks/comment-policy-check.py` が発火しないことを確認する。ついでに `KsWheelView.kt` の既存同型コメントも掃く (任意)
2. **[Minor]** `KsWheelView.setSelectedIndex` / `setCandidates` の先頭に `listView.stopScroll()` を入れ、慣性移動中のプログラム的移動が上書きされないようにする。回帰テストを `KsWheelViewTest` に1件追加する
3. **[Suggestion]** 非 ISO 暦 Locale の割り切りを `DateWheelLabels` の doc に明記する (対応自体は別 change)
4. **[Suggestion]** シート高の横向き対応は `NumberSelectionSheet` と共通の課題として別 change へ切り出す
5. 保留中の spec Scenario については、上記「参考情報」を添えてオーナー判断を仰ぐ
