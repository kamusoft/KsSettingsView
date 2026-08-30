# レビュー結果: android-datepicker-spinner-wheel (002 回目)

**日付**: 2026-08-02
**判定**: APPROVED

## サマリー

前回サイクルで確定した指摘 4 件 (突き合わせ表 #1〜#4) はいずれも解消されている。修正で新たに入ったコード
— `KsWheelView` の `stopScroll()` と通知抑止フラグ、`DateSelectionSheet` の列コンテナ LTR 固定、コメントの
書き直し、sample footer の差し戻し — も、いずれも局所的で意図が自己完結しており、副作用の検討もコメントに
残されている。特に `stopScroll()` は「打ち切り → `SnapHelper` の再スナップ → その打ち切り」という
`RecyclerView` の内部挙動を正しく踏まえており、中間位置を選択として確定しないための
`isStoppingScroll` の導入まで含めて筋がよい。

テストは `./gradlew test --rerun-tasks` で全件再実行し 748 tests / failures 0 / errors 0 / skipped 0。
本変更ぶんは `DateSelectionSheetTest` 43 件 (前回 41 → +2)、`KsWheelViewTest` 31 件 (前回 28 → +3) で、
追加分は 4 件の指摘それぞれに対応する回帰テストになっている。ブロッキングの指摘なし。

## 前回指摘の解消確認

| # | 指摘 (確定重要度) | 状態 | 確認内容 |
|---|---|---|---|
| 1 | プログラム的スクロールが進行中の fling を止めない (Major) | ✅ 解消 | `KsWheelView.kt:330` に `stopScroll()` を新設し、`setCandidates` (`:293`) / `setSelectedIndex` (`:314`) / `selectAdjacent` (`:456`) の3経路すべてから呼んでいる。回帰テスト3件を確認 |
| 2 | comment-policy 違反 (Major) | ✅ 解消 | 変更対象ファイル全件に hook のパターンを適用して検出 0 件。指摘の 6 箇所 + `DatePickerCellViewHolder` の履歴記述に加え、diff 外だった `KsWheelView` の既存同型コメント・`DatePickerCell` / `DatePickerCellViewHolder` の `openspec/` 参照まで掃かれている |
| 3 | RTL Locale で列順が反転 (Major) | ✅ 解消 | `DateSelectionSheet.kt:471` で列コンテナに `layoutDirection = View.LAYOUT_DIRECTION_LTR` を指定。回帰テストを確認 |
| 4 | sample footer の片側先行変更 (Minor) | ✅ 解消 | Android / iOS とも footer は「ホイール形式で日付を選択するデモ。」で一致。`todayText = "今日"` も両プラットフォームで揃っている |

なお突き合わせ表 #5 (非 ISO 暦 Locale) / #6 (横向き固定高) は対応不要と確定済みのため、本レビューでは扱わない。

## 確認した観点

- **ビルド / テスト**: `android/` で `./gradlew test --rerun-tasks` を実行し BUILD SUCCESSFUL。debug variant 748 tests / failures 0 / errors 0 / skipped 0 (release variant も同数実行)
- **#1 の修正の正しさ**: `RecyclerView.stopScroll()` は `setScrollState(IDLE)` → `stopScrollersInternal()` の順で動く。`setScrollState(IDLE)` の時点で `SnapHelper` の scroll listener が発火して `snapToTargetExistingView()` → `smoothScrollBy()` を始めるため、1回では state が `SETTLING` のまま残る。実装はこれを踏まえて2回目を条件付きで呼んでおり、`SnapHelper` 内部の `mScrolled` が 1 回目で false に落ちるため2回で収束する (無限ループにならない)。打ち切り中の `IDLE` 通知で `commitSnappedSelection` が中間位置を確定しないよう `isStoppingScroll` (`:349`) で抑止しているのも正しい
- **#1 のテストの質**: `慣性移動中にプログラム的に選択を移すと移動先に留まる` (`KsWheelViewTest.kt:334`) は `smoothScrollBy` 直後に `scrollState == SETTLING` を assert して「移動中の再現になっている」ことを先に確認しており、テストが空振りしない作りになっている。`慣性移動を打ち切った中間位置は選択として通知されない` (`:359`) は候補位置へ整列した状態から打ち切る意地の悪いケースを突いていて、`isStoppingScroll` が無ければ落ちる
- **#3 のテストの質**: `RTL Locale でも3列は左から年 月 日の順に配置される` (`DateSelectionSheetTest.kt:689`) は、構成の `layoutDirection` が RTL であること・列コンテナの**親**まで RTL が届いていること (`:703`) を assert したうえで `left` の大小を比較している。LTR 固定を外せば落ちるテストになっており、`indexOfChild` だけを見ていた既存テスト (`:679`) の穴を正しく塞いでいる。`FLAG_SUPPORTS_RTL` をテスト側で立てる必要がある理由もコメントで説明されている
- **LTR 固定の副作用**: 列コンテナへの `layoutDirection` 指定は各行の `TextView` まで伝播するが、行は `gravity = Gravity.CENTER` (`KsWheelView.kt:245`) で中央寄せのため、RTL Locale でも表示位置は変わらない。帯 (`DateSelectionSheet.kt:495`) の `marginStart` / `marginEnd` は同値のため反転の影響なし
- **足場アーティファクト**: `proposal.md` / `exploration.md` / `specs/settings-view-android-ui/spec.md` は HEAD から差分なし (凍結違反なし)。書き換えは `tasks.md` / `ui/brief.md` のみ
- **視覚照合の陳腐化**: `ui/verification/` の5枚は 23:11 生成で、今サイクルの修正 (23:35〜23:45) より前。ただし修正は ja / LTR の撮影条件下で描画に影響しない (LTR では `LAYOUT_DIRECTION_LTR` の明示は既定と同一、コメント書き直しと sample footer の差し戻しは選択面の描画に無関係、`stopScroll` は fling 競合時のみ挙動が変わる) ため、照合結果は有効なままと判断した
- **既存挙動の回帰**: `stopScroll()` は `selectAdjacent` にも入ったが、`NumberSelectionSheet` / `PickerSelectionSheet` 系のテストを含め全件 green

---

## 指摘事項

### [🔵 Suggestion] 起点以外の系列へ、選択が変わらない場合でも移動を指示している

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DateSelectionSheet.kt:611-613`

**問題点**:
`applySelection` は起点以外の系列に対して無条件に `yearWheel.setSelectedIndex(...)` を呼ぶ。`setSelectedIndex` は
今回の修正で先頭に `stopScroll()` が入ったため、**渡した index が現在の選択と同じでも進行中の移動を打ち切る**
ようになった。年の選択は丸めによって変わらないことが保証されている (コメント `:604` のとおり) ので、月の
スナップ静止のたびに年ホイールへ「同じ位置への移動」を指示していることになる。

実害が出るのは「年ホイールを弾いたまま、別の指で月ホイールを操作してスナップさせる」ような同時操作に
限られ、そのとき年の慣性移動が途中で止まる。修正前は逆に年の移動が続いて別の年へ着地していた
(それが #1 の不具合) ので、現状は明確に改善されており、退行ではない。

**推奨修正**:
必須ではない。対応するなら `setSelectedIndex` の冒頭を「移動不要なら何もしない」に寄せるのが素直:

```kotlin
internal fun setSelectedIndex(index: Int) {
    if (index !in itemIndices) return
    // 既に選択中でスクロールも止まっているなら、打ち切りも再配置も要らない。
    if (index == selectedIndex && listView.scrollState == RecyclerView.SCROLL_STATE_IDLE) return
    stopScroll()
    ...
}
```

ただし「同じ index でも中央へ再整列させたい」という現在の暗黙の期待 (候補差し替え直後など) を壊さないか
確認が要るため、この change で急いで入れる必要はない。

---

## アクションプラン

1. 追加対応なし。前回指摘 #1〜#4 はすべて解消済みで、ブロッキングの指摘はない
2. [Suggestion] `setSelectedIndex` の早期 return は、次に `KsWheelView` へ触れる変更のついでで足りる

---

# 追記: 提示上限の spec 更新への追随確認 (2026-08-03)

**対象**: オーナー決定 (年候補件数の上限を `Int` 表現上限 → 提示上限 1,000,000 件へ引き直し) に伴う
spec 更新への追随部分のみ
**判定**: **APPROVED 維持** (Critical 0 / Major 0 / Minor 1 / Suggestion 0 — いずれも非ブロッキング)

## 確認内容

| 項目 | 状態 | 確認内容 |
|---|---|---|
| spec 更新の内容 | ✅ | Requirement 本文が「提示上限 1,000,000 件」へ、Scenario が「年候補件数が提示上限を超える指定では提示しない」へ更新。GIVEN も `1,999,999,999 件 > 1,000,000 件` と到達可能な記述に直っている |
| 実装の追随 | ✅ | `DateSelectionSheet.kt:229` に `MAX_YEAR_CANDIDATE_COUNT: Long = 1_000_000L` を新設し、`:259` の判定を `Int.MAX_VALUE` 比較から置換。64bit 算出 (`:258` の `Long`) は維持されており、spec 本文の「64bit 整数で算出し」を満たす |
| 警告ログ | ✅ | `:263` に `limit=$MAX_YEAR_CANDIDATE_COUNT` を追加。件数・上限・min・max がすべて出るため、上限に当たった構成を logcat だけで切り分けられる |
| ガードの順序 | ✅ | `min.isAfter(max)` の判定が先、件数判定が後。`LocalDate.MIN`/`MAX` は前者を通り抜けて後者で弾かれるため、Scenario の GIVEN が意図どおり件数ガードに到達する |
| テスト追加 | ✅ | `DateSelectionSheetTest.kt:287`。選択面が提示されないこと (`assertNull`) と警告ログに `too many year candidates: 1999999999` が出ることの両方を検証。件数はハードコードした期待値ではなく実際の算出結果と一致している (`999999999 - (-999999999) + 1`) |
| tasks.md 4.1 | ✅ | チェック済みに変更され、設計メモの「4.1 の保留」節が「保留は解消済み (提示上限 1,000,000 件への引き直し)」へ書き替えられている。経緯 (なぜ旧上限では到達不能だったか) も残っており、虚偽チェックではない |
| コメント規約 | ✅ | 変更対象ファイル全件に hook のパターンを再適用して検出 0 件 |
| テスト全件 | ✅ | `cd android && ./gradlew test` → BUILD SUCCESSFUL。debug variant 749 tests / failures 0 / errors 0 / skipped 0 (`DateSelectionSheetTest` 43 → 44 件、追加はこの1件のみ) |

## 指摘事項

### [🟡 Minor] 上限の根拠コメントが破綻水準を実際より3桁小さく書いている

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DateSelectionSheet.kt:224-228`

**問題点**:
`MAX_YEAR_CANDIDATE_COUNT` の doc コメントは、上限を選んだ根拠をこう書いている:

```
ホイールのスクロール範囲が破綻する水準（1万件規模）に対して2桁の余裕を持たせた防御上限で、
```

実際の破綻水準は「1万件規模」ではない。破綻するのは `LinearLayoutManager` の縦スクロール範囲
(`itemCount × rowHeightPx`) が `Int` を溢れる点で、`ROW_HEIGHT_DP = 44f` から算出すると:

| 密度 | 行高 (px) | 破綻件数 |
|---|---|---|
| xhdpi (2.0) | 88 | 約 2,440 万 |
| xxhdpi (3.0) | 132 | 約 1,627 万 |
| xxxhdpi (4.0) | 176 | 約 1,220 万 |

つまり破綻水準は **1,200〜2,400 万件規模** で、コメントの「1万件規模」とは約3桁ずれている。
また上限 1,000,000 件との比は xxhdpi で約 16 倍 (≒1桁) なので、「2桁の余裕」も実態と合わない。

**選んだ上限 1,000,000 件そのものは妥当**である (破綻水準より 1 桁以上低く、実用的な年範囲
— 既定 201 件 — より 3 桁以上高い)。問題は根拠の数字だけで、挙動には影響しない。
ただしこのコメントは「なぜこの値なのか」を将来の読み手に伝えるための唯一の記述なので、
誤った前提から上限を再導出されると、上げ過ぎ / 下げ過ぎの改変を招きうる。

**推奨修正**:
数値を実測に合わせる。例:

```kotlin
/**
 * 年候補として提示できる件数の上限。
 *
 * ホイールの縦スクロール範囲（件数 × 行高）が Int を溢れる水準（行高 44dp では
 * 1,200 万件規模）より1桁以上低く抑えた防御上限で、実用的な年範囲（既定は 201 件）が
 * これに触れることはない。
 */
```

**参考 (オーナー判断向け・spec は書き換えない)**:
更新後の spec 本文にも同じ趣旨の括弧書き「(ホイールのスクロール範囲が破綻する水準に対して2桁の
余裕を持つ防御上限)」がある。こちらも実測とは合わない (実際は約16倍 ≒ 1桁) が、spec は
オーナー承認済みの足場アーティファクトのため、本レビューでは書き換えを求めない。
規範部分 (上限 1,000,000 件) は妥当であり、括弧書きは非規範的な補足なので実装への影響もない。
文言を直すかどうかはオーナーの判断に委ねる。

### [nit] tasks.md 2.2 の文言が旧上限のまま

`tasks.md` の 2.2 は「年候補件数の Int 上限超過」という旧表現のまま残っている (4.1 と設計メモは
更新済み)。実装・spec とも「提示上限」へ移っているので、次に tasks.md へ触れる際に揃えると読みやすい。
アーカイブされる作業資料であり、判定には影響しない。

## 追記時点のアクションプラン

1. [Minor] `MAX_YEAR_CANDIDATE_COUNT` の doc コメントの破綻水準を実測値へ直す (1行の書き換え)
2. [nit] `tasks.md` 2.2 の「Int 上限超過」→「提示上限超過」
3. spec 本文の括弧書きの数字をどうするかはオーナー判断 (規範部分に影響なし)

### 上記3点の対応確認 (2026-08-03)

3点とも対応済みで、数値は実測と整合する。`MAX_YEAR_CANDIDATE_COUNT` の KDoc の「端末密度により約 1,200 万〜2,400 万件」は
実測 (xxxhdpi 約 1,220 万 / xxhdpi 約 1,627 万 / xhdpi 約 2,440 万) と一致し、最悪ケース 1,220 万に対して上限
1,000,000 件は 12.2 倍低いため「1桁以上低く抑えた」も正しい。spec 本文の括弧書き (「破綻する水準より1桁以上低い防御上限」) と
`tasks.md` 2.2 の「提示上限超過」も整合しており、設計メモ 62 行付近が旧上限のまま残っているのは Scenario 改称前の経緯の
記述として正しい。`./gradlew :ks-settingsview-ui:compileDebugKotlin :ks-settingsview-compose:compileDebugKotlin` は
BUILD SUCCESSFUL (`:ks-settingsview-ui:compileDebugKotlin` は実行された)。**APPROVED 維持**、未処理の指摘なし。

あわせて `ui/brief.md:73` にオーナー最終承認 (2026-08-02) が記録されたことを確認した。verify-002 で「本検証の範囲外として
残っている」とした事項も解消済み。
