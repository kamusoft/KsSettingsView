# レビュー結果: android-numberpicker-modern-ui (002 回目)

**日付**: 2026-08-02
**判定**: APPROVED

## サマリー

review-001.md および second-opinion-002.md 末尾「突き合わせ結果」で確定した指摘 5 件 (Major 3 / Minor 2) と Suggestion 3 件は、いずれも成果物上で解消されている。修正はコメント掃除にとどまらず、スナップ静止判定の厳密化 (`calculateDistanceToFinalSnap` による整列確認) と候補列の index ベース遅延生成という設計変更を伴っているが、新たな退行は検出されなかった。

ビルド・テストとも成功 (`android`: 1308 件・失敗 0 / `samples/android`: `:app:compileDebugKotlin` 成功)。デルタスペックの 6 Requirement / 25 Scenario は verify-001.md の対応表どおり実装・テストの裏付けを保っており、今回の修正で対応が失われた Scenario はない。むしろ 5 件のテストが新規追加され、行間停止からの補正スクロール・上限ちょうどの候補件数・64bit 候補値算出・アクセシビリティイベント送出という、従来カバーされていなかった経路が塞がれた。

Critical / Major はなく、残るのは記録の鮮度と任意のテスト追加に関する Suggestion 3 件のため APPROVED とする。

## 前回指摘の解消状況

| # | 指摘 (review-001 / 突き合わせ結果) | 確定重要度 | 状態 | 根拠 |
|---|---|---|---|---|
| 1 | comment-policy 禁止参照の残存 | Major | ✅ 解消 | `NumberPickerCell.kt` / `InputCellDsl.kt` / `InputCellDslTest.kt` / `InputCellsDemoScreen.kt` の 4 ファイルすべてで削除済み。本 change で触れた全ファイルに対する `openspec/` `kasane/` `Decision [0-9]` `仕様:` `設計:` `MUST` `SHALL` の grep がゼロ件 |
| 2 | SnapHelper 補正移動開始時の IDLE で選択が確定される | Major | ✅ 解消 | `KsWheelView.kt:245-253` (`commitSnappedSelection`) が `calculateDistanceToFinalSnap` の残距離 0 を条件に加えた |
| 3 | 有効候補数での eager List 生成 | Major | ✅ 解消 | `NumberPickerCellViewHolder.kt:113-180` の `NumberCandidates` が「先頭値・刻み・件数」記述子となり、`KsWheelView` は `itemCount` と `(Int) -> String` だけを受け取る形に変わった |
| 4 | `bindRow` フックが実バインド経路と別コード | Minor | ✅ 解消 | `KsWheelView.kt:358-360` の private `bindRow(row, position)` を、検証用フック (`:346-350`) と `onBindViewHolder` (`:371-373`) の双方が呼ぶ |
| 5 | 選択変更時の AccessibilityEvent 非送出 | Minor | ✅ 解消 | `KsWheelView.kt:262` で選択が実際に変わったときだけ `TYPE_VIEW_SELECTED` を送出 |
| 6 | `PickerSheetStyle.from` の 2 オーバーロード重複 | Suggestion | ✅ 解消 | 2 つの公開オーバーロードが `private fun from(cellAccentColor, theme, effective)` へ委譲する形になった |
| 7 | `SelfContainedRecyclerView` の不要な `open` | Suggestion | ✅ 解消 | `SheetChrome.kt:322` は `internal class` |
| 8 | 候補の遅延生成 | Suggestion | ✅ 解消 | #3 と同一の修正で対応 |

## 修正が持ち込んだ問題の確認 (今回の重点)

### スナップ静止判定 (`KsWheelView.kt:245-253`)

「残距離 0 のときだけ確定する」への変更は、**確定しなくなる方向の退行**が最も怖い箇所のため、算術と経路の両面で確認した。

- **算術**: `listView` は `clipToPadding = false` のため `LinearSnapHelper` の中央は `height / 2`、行の中央は `top + rowHeight / 2` で求まる。行が選択位置に整列した状態は `top = 2 * rowHeight` なので残距離は `2r + r/2 - 5r/2`。`r` の偶奇によらず整数除算の結果は常に 0 になる (`r = 2k` → `5k - 5k`、`r = 2k+1` → `(5k+2) - (5k+2)`)。密度によって残距離が 1px ずれて永久に確定しなくなる、という懸念は成立しない
- **listener 順序**: `addOnScrollListener(WheelScrollListener())` が `snapHelper.attachToRecyclerView` より先に登録されている (`:177` と `:179`) ため、行間停止の IDLE では自前 listener が先に走り、残距離が 0 でないので確定をスキップする。その後 SnapHelper が補正を開始し、完了後の IDLE で残距離 0 → 確定という順序になる。仮に登録順が逆でも残距離条件が効くため、順序への依存はない
- **スクロール端**: 上下に `(可視行数 - 1) / 2` 行分の padding があり、先頭・末尾候補もスクロール限界で中央に来られる。端で残距離が 0 にならず確定できないケースは無い
- **候補 1 件時**: コンテンツ高 = 可視行数の高さと一致してスクロール不能になるが、初期 index がそのまま選択中として保たれるため問題ない
- **テスト**: `KsWheelViewTest.kt` の「行間で指を離した時点では選択中候補を更新しない」「補正スクロールが完了して候補位置へ整列すると選択中候補が更新される」が、実 MotionEvent (タッチスロップ上乗せ・離す直前の静止で fling 速度 0) で両状態を分けて検証している。前者は行が中央からずれていること自体もアサートしており、「そもそも行間で止まっていない」偽陽性を排除している

### 候補列の遅延生成 (`NumberPickerCellViewHolder.kt:113-180`)

- `valueAt(index)` は `first.toLong() + index.toLong() * step` を経由するため、`min` が負・範囲が Int 全域でもオーバーフローしない。有効 index の上限は `count - 1` で、その値は必ず `max` 以下になるため `toInt()` は安全
- `indexOf(value)` は `offset < 0` / `offset % step != 0` / `index >= count` の 3 経路で先頭候補 `0` へ落ちる。範囲外・step 非整合・候補外のいずれも spec の「先頭候補」に一致
- `count` は必ず 1 以上になるため、`KsWheelView.NO_SELECTION` はこの経路からは発生しない (ホイール側に残る `-1` 分岐は部品としての防御であり、死にコードではない)
- **テスト**: 「候補件数が Int 上限ちょうどでも候補列を実体化せずに提示する」は、eager 実装なら OOM か長時間停止で落ちるため、遅延化の証明として機能している。「Int 全域にまたがる候補列でも候補値は 64bit で算出される」が先頭 `Int.MIN_VALUE` / 末尾 `Int.MAX_VALUE` / 確定値の 3 点を押さえている

### アクセシビリティイベント (`KsWheelView.kt:256-265`)

- 送出は `index != selectedIndex` のときだけで、端の候補で動かなかった場合に無駄な通知が出ない。これも「選択中候補が変わらないときはアクセシビリティイベントを送出しない」で裏が取られている
- `contentDescription` の代入自体が内部的に content-change 通知を伴うため、`TYPE_VIEW_SELECTED` は追加通知になる。重複読み上げのリスクはあるが、`info.className` を `NumberPicker` として公開している以上スピナーとしての選択変化通知は妥当で、review-001 が提示した選択肢の範囲内

## 指摘事項

### [🔵 Suggestion] 「移動中の確定」がシート経路のテストになっていない

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/KsWheelViewTest.kt:201-230`

**問題点**: Scenario「移動中の確定は直前にスナップ静止した候補を採用する」の検証は、ホイール側の `selectedIndex` が補正前後で変わることの確認と、シート側が `wheelView.selectedIndex` を読むことの確認に分解されている。`confirmSelection` が読む値そのものを押さえているため実質的な穴はないが、「行間で止めた直後に確定ボタンを押すと旧値が通知される」という**利用者の操作列そのもの**を通る経路は 1 件も無い。second-opinion-002 が求めていた形もこちらだった。

**推奨修正 (任意)**: `NumberSelectionSheetTest` に、ホイールを行間まで実ドラッグ → `sheet.confirmView.performClick()` で旧値が通知される、という 1 件を足す。`KsWheelViewTest.dragWheelBy` と同等のヘルパーは既に `NumberSelectionSheetTest.drag` にあるため、追加は小さい。

---

### [🔵 Suggestion] verify-001.md の実装参照が今回の修正で陳腐化している

**該当箇所**: `kasane/changes/android-numberpicker-modern-ui/verify-001.md:24,35,36,38,39,47,48,49,73,74,75`

**問題点**: 対応表が指す行番号・式が修正前のコードを指している (例: 「選択面の候補表示にも同じフォーマットを適用する」の実装欄が `NumberPickerCellViewHolder.kt:63` の `candidates.map { ... }` になっているが、現在の同ファイル `:63` は `displayTextAt = candidates::displayTextAt`)。Scenario とテストの対応自体は全件維持されているため一致検証の結論は変わらないが、アーカイブされる証跡としては現物と噛み合わない。

**推奨修正 (任意)**: verify-002.md を出して対応表を現行コードで引き直す。判断は指揮側 (verify の再実行要否) に委ねる。

---

### [🔵 Suggestion] 解消済みの archived deviation が残っている

**該当箇所**: `kasane/changes/archive/2026-08-01-align-sample-parity/deviation.md:15`

**問題点**: 「Android は `unit` 相当の公開 API を持たないため Picker UI 側に suffix が付かない」という合意済み差分が記録されているが、本 change で `unit` が入り、サンプルの footer 文言「Picker UI と Cell の valueText に "px" suffix が付く。」が iOS と同様に成立する状態になった (`InputCellsDemoScreen.kt:175,185` と `samples/ios/KsSettingsViewSample/InputCellsDemoView.swift:175,183` が一致)。deviation は解消済みだが記録は残ったままになる。

**推奨修正 (任意)**: アーカイブ済み文書は凍結対象のため本サイクルでは触らず、蒸留 (ksn-distill) で「この deviation は android-numberpicker-modern-ui で解消」と扱えるよう申し送る。

## 確認したが指摘に至らなかった観点

- **ビルド・テスト**: `cd android && ./gradlew test` 成功 (JUnit XML 集計で 1308 件・failures 0・errors 0)。`cd samples/android && ./gradlew :app:compileDebugKotlin` 成功
- **仕様充足**: 6 Requirement / 25 Scenario の対応は verify-001.md の対応表どおり維持。修正で削除・改名されたテストは無く、5 件が純増 (行間停止 / 補正完了 / 上限ちょうどの件数 / 64bit 候補値 / アクセシビリティイベント 2 件)
- **足場凍結**: `proposal.md` / `specs/settings-view-android-ui/spec.md` は未変更。変更されている足場は `tasks.md` (全項目のチェック) と `ui/brief.md` (照合結果の追記) のみで、いずれも実装フェーズで書いてよい種類。tasks.md に虚偽チェックは無く、2.3 の「移動中確定の採用値」も対応テストが実在する
- **コメント規約**: 本 change で触れた全ファイル (新規 3 + 変更 6) を `openspec/` `kasane/changes/` `Decision [0-9]` `Requirement` `Scenario` `review-[0-9]` `仕様:` `設計:` `MUST` `SHALL` `SHOULD NOT` `MAY` で grep してゼロ件。書き換え後の KDoc (`InputCellDsl.kt:24-25`・サンプルの `unit` 説明) も自己完結しており、外部参照は `android/ADR-0005` / `android/ADR-0007` 形式のみ
- **境界値**: `min > max` / 候補件数 Int 上限超過 / 上限ちょうど / `Int` 全域 + step 3 / `max` 付近の step 加算 / `step <= 0` / `value` が候補外・範囲外 / 端候補でのアクセシビリティアクション / `selectedIndex + 1` のオーバーフロー可能性 (最大 index は `Int.MAX_VALUE - 1` のため発生しない) を確認。いずれも安全側
- **リソース・性能**: 候補列が実体化されなくなったことで、旧実装 (`List<Int>` + `List<String>` の全件生成) より割り当てが減っている。`KsWheelView` が保持するのは件数と関数だけで、行 View は `RecyclerView` の再利用に乗る。`itemIndices` の `IntRange` 生成はホットパスではない
- **既存挙動の非退行**: `PickerSelectionSheet` 側の変更は `PickerSheetStyle.from` の委譲化と `SelfContainedRecyclerView` の `open` 除去のみで、解決規則・生成される値は同一。既存の `PickerSelectionSheetTest` 全件が通っている
- **サンプル**: `unit = "px"` への置き換えで `valueText` 手組みが不要になり、iOS サンプル (`InputCellsDemoView.swift:182-183`) と引数構成・footer 文言が一致した
- **視覚照合**: `ui/brief.md` の照合結果 (`verification/` 4 枚・検証条件 9 項目 OK・合意済み妥協 0 件・未カバー 1 件の明記) は前回レビュー時点のもの。今回の修正のうち見た目に影響するものは無い (スナップ静止判定は「いつ確定するか」の変更で、静止後の描画結果は同一) ため、再照合は不要と判断した

## アクションプラン

1. 追加対応は必須ではない。Suggestion 3 件はいずれも任意で、実装コードの修正を伴うものは 1 件目のテスト追加のみ
2. 2 件目 (verify-002 の要否) と 3 件目 (archived deviation の申し送り) は指揮側・蒸留側の判断事項として引き継ぐ
