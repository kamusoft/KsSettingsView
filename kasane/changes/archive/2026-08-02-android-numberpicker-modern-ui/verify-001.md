# 一致検証結果: android-numberpicker-modern-ui (001 回目)

**日付**: 2026-08-02
**判定**: VALID

## 検証範囲

- デルタスペック: `specs/settings-view-android-ui/spec.md` (ADDED 6 Requirement / 25 Scenario)
- tasks.md (4 節・17 タスク)
- deviation.md: 無し (乖離記録なし)
- ui/brief.md + ui/mock/approved.png + ui/verification/ (4 枚)
- テスト実行: `cd android && ./gradlew test --rerun-tasks` → **BUILD SUCCESSFUL / tests=1294 failures=0 errors=0 skipped=0**
- サンプル (別 composite build) のコンパイル: `cd samples/android && ./gradlew :app:compileDebugKotlin` → **BUILD SUCCESSFUL**

## 対応表

### Requirement: NumberPickerCell の unit と表示値の生成

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| unit 指定時の自動表示 | `NumberPickerCell.kt:48-53` (`effectiveValueText`) / `NumberPickerCell.kt:89-97` (`format`) / `NumberPickerCellViewHolder.kt:24` | `InputCellsTest.kt` 「bind で unit 指定時は単位付き文字列を表示」「format は unit 非空なら半角スペース区切りで連結する」 | ✅ 一致 |
| unit 未指定時の自動表示 | 同上 | `InputCellsTest.kt` 「bind で unit 未指定なら数値のみを表示」「format は unit が空なら数値のみを返す」「既定の unit は空文字」 | ✅ 一致 |
| valueText 明示指定は unit より優先される | `NumberPickerCell.kt:53` (`valueText ?: format(...)`) | `InputCellsTest.kt` 「bind で valueText 明示指定は unit より優先される」 | ✅ 一致 |
| 選択面の候補表示にも同じフォーマットを適用する | `NumberPickerCellViewHolder.kt:63` (`candidates.map { NumberPickerCell.format(it, cell.unit) }`) | `NumberSelectionSheetTest.kt` 「候補表示には unit が適用される」 | ✅ 一致 |
| valueText 明示指定は候補表示に影響しない | 同上 (候補は `valueText` を参照しない) | `NumberSelectionSheetTest.kt` 「valueText の明示指定は候補表示に影響しない」 | ✅ 一致 |
| Compose DSL overload から unit を指定できる | `InputCellDsl.kt:210,226` | `InputCellDslTest.kt` 「NumberPickerCell DSL で unit を指定できる」「unit 既定値は空文字」 | ✅ 一致 |

補足: `unit` は `equals` / `hashCode` にも反映済み (`NumberPickerCell.kt:69,88`)、`InputCellsTest.kt`「equals は unit の差を区別する」で検証。iOS 側 `NumberPickerCell.format(value:unit:)` (`ios/Sources/KsSettingsViewUI/NumberPickerCell.swift:135-140`) と規則が一致することを確認した。

### Requirement: NumberPickerCell 選択面の提示

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| タイトルの解決 | `NumberPickerCellViewHolder.kt:62` (`pickerTitle ?: title`) | `NumberSelectionSheetTest.kt` 「タイトルは pickerTitle を優先して解決する」「pickerTitle が null のときタイトルは title を使う」 | ✅ 一致 |
| step 刻みの候補列挙 | `NumberPickerCellViewHolder.kt:100` | `NumberSelectionSheetTest.kt` 「候補は step 刻みで昇順に列挙される」 | ✅ 一致 |
| step が 0 以下なら 1 へ fallback する | `NumberPickerCellViewHolder.kt:117` (`effectiveStepOf`) | `NumberSelectionSheetTest.kt` 「step が 0 以下なら 1 へ fallback する」 | ✅ 一致 |
| min > max では選択面を提示しない | `NumberPickerCellViewHolder.kt:81-89` (警告ログ + `null`) | `NumberSelectionSheetTest.kt` 「min が max より大きいときは選択面を提示せず警告ログを残す」 | ✅ 一致 |
| 候補件数が Int 上限を超える指定では提示しない | `NumberPickerCellViewHolder.kt:91-99` (`count: Long` 算出 + 警告ログ) | `NumberSelectionSheetTest.kt` 「候補件数が Int 上限を超える指定では選択面を提示せず警告ログを残す」 | ✅ 一致 |
| max 付近の step 加算でも列挙が終端する | `NumberPickerCellViewHolder.kt:100` (先頭 + index × step を 64bit で算出) | `NumberSelectionSheetTest.kt` 「max 付近の step 加算でも候補の列挙は終端する」 | ✅ 一致 |
| 無効 Cell は選択面を提示しない | `NumberPickerCellViewHolder.kt:40-46` | `NumberSelectionSheetTest.kt` 「無効 Cell の行タップでは選択面を提示しない」 | ✅ 一致 |
| (操作ラベルは OS 公開リソース) | `SheetChrome.kt:198,227` (`android.R.string.cancel` / `android.R.string.ok`) | `NumberSelectionSheetTest.kt` 「操作ラベルは OS の公開文字列リソースから解決される」 | ✅ 一致 |

### Requirement: 選択候補の初期状態と選択操作

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 初期選択は現在値 | `NumberPickerCellViewHolder.kt:109-114` (`initialIndexOf`) / `KsWheelView.kt:112-114` | `NumberSelectionSheetTest.kt` 「初期選択は現在値の候補になる」 / `KsWheelViewTest.kt` 「初期選択は指定された index の候補になる」「初期選択の候補は中央の選択位置へ置かれる」 | ✅ 一致 |
| 現在値が候補に含まれない場合は先頭候補 | `NumberPickerCellViewHolder.kt:110,113` | `NumberSelectionSheetTest.kt` 「現在値が候補に含まれない場合は先頭候補が選択中になる」「範囲外の現在値でも先頭候補が選択中になる」 | ✅ 一致 |
| 移動中の確定は直前にスナップ静止した候補を採用する | `KsWheelView.kt:362-373` (IDLE 時のみ `commitSnappedSelection`) / `NumberSelectionSheet.kt:134-140` (`wheelView.selectedIndex` を採用) | `KsWheelViewTest.kt` 「移動中は選択中候補を更新しない」「静止すると中央にスナップした候補が選択中になる」 + `NumberSelectionSheetTest.kt` 「確定で選択中の候補値を1回通知して閉じる」 | ✅ 一致 (下記注) |
| 候補領域の下方向操作はシートを閉じない | `SheetChrome.kt:322-334` (`SelfContainedRecyclerView`) / `KsWheelView.kt:102` | `NumberSelectionSheetTest.kt` 「候補領域の下方向操作では選択面を閉じず候補が遷移する」 (実 MotionEvent ドラッグ) | ✅ 一致 |
| (選択中は常に1件・判別可能) | `KsWheelView.kt:256-272` (`applyRowAppearance`) | `KsWheelViewTest.kt` 「選択中行だけが強調色の太字で描画される」「中央ハイライト帯は選択位置に強調色の淡色で敷かれる」「中央から離れるほど行はフェードして縮小する」 | ✅ 一致 |

注: 「移動中の確定」は単一の end-to-end テストではなく、(a) 移動中は `selectedIndex` が更新されない (b) 確定は `selectedIndex` を採用する、の2テストの合成で担保されている。確定経路が読む値が (a) で固定される値そのものであるため、契約としては充足と判定した。

### Requirement: 確定と非確定 dismiss

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 確定で選択値を1回通知する | `NumberSelectionSheet.kt:134-140` / `NumberPickerCellViewHolder.kt:66` | `NumberSelectionSheetTest.kt` 「確定で選択中の候補値を1回通知して閉じる」 (`assertEquals(listOf(75), received)` で発火回数も検証) | ✅ 一致 |
| 非確定 dismiss は経路によらず callback を発火しない | `NumberSelectionSheet.kt:66` (`onCancel = { cancel() }`) / `:92-93` (`setCanceledOnTouchOutside(true)`) | `NumberSelectionSheetTest.kt` 「キャンセルボタンでは callback を発火しない」「選択面は外側タップで閉じられる設定になっている」「非確定 dismiss はどの経路でも callback を発火しない」(cancel / onBackPressed / dismiss)「下方向スワイプ相当の非表示遷移で閉じても callback を発火しない」(`STATE_HIDDEN`) | ✅ 一致 |

### Requirement: 選択面の強調色

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Cell 固有値が最優先される | `PickerSelectionSheet.kt:99-101` (`cell.accentColor?.toArgb() ?: effective.accentColor`) | `NumberSelectionSheetTest.kt` 「強調色は Cell 固有値を最優先で解決する」「Cell 固有値が無いとき CellStyle へフォールバックする」 | ✅ 一致 |
| Theme の既定色へフォールバックする | 同上 (`EffectiveStyle` が CellStyle → Theme を解決) | `NumberSelectionSheetTest.kt` 「Cell 固有値も CellStyle も無いとき Theme へフォールバックする」 | ✅ 一致 |

### Requirement: 候補のアクセシビリティ状態

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 選択中候補が公開される | `KsWheelView.kt:126,245,290-301` (`contentDescription` / `onInitializeAccessibilityNodeInfo`) | `KsWheelViewTest.kt` 「選択中候補の表示文字列を公開する」「ホイールはスピナー相当のコントロールとして公開される」 | ✅ 一致 |
| アクセシビリティ操作で候補を変更できる | `KsWheelView.kt:303-319` (`performAccessibilityAction` / `selectAdjacent`) | `KsWheelViewTest.kt` 「次候補へのアクセシビリティ操作で選択中が進む」「前候補へのアクセシビリティ操作で選択中が戻る」 (公開状態と強調表示の両方を検証) | ✅ 一致 |
| 端の候補ではその方向へ変更されない | `KsWheelView.kt:295-300,315` | `KsWheelViewTest.kt` 「末尾候補では次候補へ変更されない」「先頭候補では前候補へ変更されない」 (アクション非提供も検証) | ✅ 一致 |

## 追加検査

| 項目 | 結果 |
|---|---|
| tasks.md 全タスク完了 | ✅ 17/17 が `[x]`。対応表と突き合わせて**虚偽チェックなし**。4.2 (視覚照合) も brief.md への記録という成果物で裏付けあり |
| 逆流検査 (足場の書き換え) | ✅ `git status` 上、`proposal.md` / `specs/settings-view-android-ui/spec.md` / `exploration.md` / `second-opinion-001.md` / `ui/mock/` はいずれも未変更。変更されている足場は `tasks.md` (チェック更新) と `ui/brief.md` (照合結果の追記 — ui 規約が brief.md に記録すると定める項目) のみ |
| 未記録乖離 | ✅ ❌ 判定は 0 件。deviation.md が無いことと整合 |
| UI 変更の証跡 | ✅ brief.md に承認モックの記録 (`mock/plan-a.html` 採用・`approved.png`・2026-08-02 オーナー承認) と照合結果 (2026-08-02、`verification/` 4 枚、検証条件 9 項目すべて OK)、**合意済み妥協 0 件**を記載。`ui/verification/` に画像 4 枚が実在 |
| テスト全件成功 | ✅ `--rerun-tasks` で強制再実行し 1294 件成功・失敗 0・スキップ 0 |
| 旧実装の残骸 | ✅ `AlertDialog` / `widget.NumberPicker` を用いた選択 UI・`ShadowAlertDialog` 期待はいずれも残存なし (`android.widget.NumberPicker` の参照は `KsWheelView` のアクセシビリティ className 用途のみ) |

## 判定理由

デルタスペックの 6 Requirement / 25 Scenario すべてに実装とテストの対応があり、❌ は 0 件。tasks.md に虚偽チェックはなく、足場アーティファクトへの逆流もない。テストは強制再実行で全件成功し、サンプル (別ビルド) のコンパイルも通る。よって **VALID**。

なお品質面の指摘 (コメント規約違反の残存・テストフックの経路乖離ほか) は `review-001.md` に分離して記載した。一致検証の軸では INVALID 要因にあたらない。
