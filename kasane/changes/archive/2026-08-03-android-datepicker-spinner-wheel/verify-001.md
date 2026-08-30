# 一致検証: android-datepicker-spinner-wheel (001)

**日付**: 2026-08-02
**判定**: VALID
**対象**: `specs/settings-view-android-ui/spec.md` (ADDED Requirement 9 件 / Scenario 31 件)

パス表記は以下を省略形で示す:

- `DateSelectionSheet.kt` = `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DateSelectionSheet.kt`
- `KsWheelView.kt` = `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsWheelView.kt`
- `DatePickerCellViewHolder.kt` / `DatePickerCell.kt` = 同ディレクトリ
- `DateSelectionSheetTest.kt` / `KsWheelViewTest.kt` / `InputCellsTest.kt` = `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/`
- `InputCellDsl.kt` / `InputCellDslTest.kt` = `android/ks-settingsview-compose/src/{main,test}/kotlin/jp/kamusoft/kssettingsview/compose/`

---

## 対応表

### Requirement: DatePickerCell (Spinner) 選択面の提示

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| タイトルの解決 | `DatePickerCellViewHolder.kt:127` (`pickerTitle ?: title`) | `DateSelectionSheetTest.kt:156` / `:162` | ✅ 一致 |
| 年・月・日の3系列が提示される | `DateSelectionSheet.kt:320-345` (3ホイール構築) / `:468` (列の配置) | `DateSelectionSheetTest.kt:174` | ✅ 一致 |
| 無効 Cell は選択面を提示しない | `DatePickerCellViewHolder.kt:57-66` (`isEnabled` 分岐で listener 未設定) | `DateSelectionSheetTest.kt:189` | ✅ 一致 |
| (Requirement 本文) 操作ラベルは OS 公開文字列リソース | `SheetHeaderView` 経由 (`android.R.string.ok` / `.cancel`) | `DateSelectionSheetTest.kt:167` | ✅ 一致 |

### Requirement: 日付候補の範囲と初期選択

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 未指定時の年候補の既定範囲 | `DateSelectionSheet.kt:218-221` (`DEFAULT_MIN_YEAR` 1900 / `DEFAULT_MAX_YEAR` 2100), `:232-233` | `DateSelectionSheetTest.kt:196` | ✅ 一致 |
| minDate / maxDate による年候補の制限 | `DateSelectionSheet.kt:127` (`yearCount`) / `:132-135` | `DateSelectionSheetTest.kt:204` | ✅ 一致 |
| 境界年では月候補も制限される | `DateSelectionSheet.kt:143` (`firstMonth`) / `:146` (`lastMonth`) | `DateSelectionSheetTest.kt:218` (下限側) / `:232` (上限側) | ✅ 一致 |
| 初期選択は date | `DateSelectionSheet.kt:315-317` (`selectedDate` 初期化) | `DateSelectionSheetTest.kt:243` | ✅ 一致 |
| 範囲外の date は最も近い範囲端へ丸めて提示する | `DateSelectionSheet.kt:199-208` (`resolve` の範囲端丸め) | `DateSelectionSheetTest.kt:252` | ✅ 一致 |
| minDate > maxDate では選択面を提示しない | `DateSelectionSheet.kt:234-246` (`null` 返却 + `Log.w`) | `DateSelectionSheetTest.kt:268` (警告ログも検証) | ✅ 一致 |
| 既定適用後に範囲が空になる構成では提示しない | `DateSelectionSheet.kt:236-240` (理由を分岐して `Log.w`) | `DateSelectionSheetTest.kt:281` | ✅ 一致 |
| 年候補件数が Int 上限を超える指定では提示しない | `DateSelectionSheet.kt:247-255` (64bit で算出し `Int.MAX_VALUE` 超過を弾く) | なし | ⏸️ **保留 (オーナー判断待ち)** |

> **保留の内容**: Requirement 本文の規則 (64bit で算出・`Int` 上限超過なら非提示) は実装済み。一方 Scenario の
> GIVEN (`minDate = LocalDate.MIN` / `maxDate = LocalDate.MAX`) では年候補件数が 1,999,999,999 件にとどまり
> `Int.MAX_VALUE` (2,147,483,647) を超えないため、`LocalDate` の値域では THEN の「提示されない」に到達できない。
> `tasks.md` の設計メモ「4.1 の一部が未了」に記録済みで、spec 側の見直し要否はオーナー判断待ち。
> 実装の欠落ではなく Scenario の前提の問題であるため、本項のみを理由に INVALID とはしない。

### Requirement: 年・月の変更への日候補の追随

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 31 日から日数の少ない月への変更は末日へ丸める | `DateSelectionSheet.kt:199-202` (`resolve` の末日丸め) / `:621-625` (日候補差し替え) | `DateSelectionSheetTest.kt:289` | ✅ 一致 |
| 閏年の 2 月は 29 日まで列挙される | `DateSelectionSheet.kt:171-176` (`lastDay` → `YearMonth.lengthOfMonth`) | `DateSelectionSheetTest.kt:299` | ✅ 一致 |
| 年の変更でも日が追随する | `DateSelectionSheet.kt:378-386` (年の `onSelectionChanged`) / `:607-629` | `DateSelectionSheetTest.kt:310` | ✅ 一致 |
| 年・月の変更で範囲外になった日付は範囲内の最近傍へ丸める | `DateSelectionSheet.kt:203-207` (`resolve` の範囲端丸め) | `DateSelectionSheetTest.kt:320` / 実操作経路は `:337` | ✅ 一致 |

### Requirement: 今日へのジャンプ (todayText)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 今日へジャンプする | `DateSelectionSheet.kt:637-641` (`jumpToToday`、callback 非発火) | `DateSelectionSheetTest.kt:355` / fling 競合は `:376` | ✅ 一致 |
| todayText が null または空文字なら操作を提示しない | `DateSelectionSheet.kt:358-359` (`isNullOrEmpty` で未生成) / `:411-415` (区切り線ごと除外) | `DateSelectionSheetTest.kt:399` (null) / `:404` (空文字) | ✅ 一致 |
| Material モードでは todayText は影響しない | `DatePickerCellViewHolder.kt:59-61` (`uiStyle` 分岐。Material 経路は `todayText` を参照しない) | `DateSelectionSheetTest.kt:427` | ✅ 一致 |
| 今日が範囲外なら何も変更しない | `DateSelectionSheet.kt:638-639` (`contains` で早期 return) | `DateSelectionSheetTest.kt:409` | ✅ 一致 |
| Compose DSL overload から todayText を指定できる | `InputCellDsl.kt:288` (引数) / `:307` (伝播) | `InputCellDslTest.kt:227` / 既定は `:239` | ✅ 一致 |
| (Requirement 本文) `DatePickerCell.todayText` の追加・既定 null | `DatePickerCell.kt:42` / `equals` `:69` / `hashCode` `:90` | `InputCellsTest.kt:700` / `:705` | ✅ 一致 |

### Requirement: 確定と非確定 dismiss

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 確定で選択日付を1回通知する | `DateSelectionSheet.kt:646-649` (`onConfirmed` 1回 + `dismiss`) | `DateSelectionSheetTest.kt:454` | ✅ 一致 |
| 非確定 dismiss は経路によらず callback を発火しない | `DateSelectionSheet.kt:354` (`onCancel` は `cancel()` のみ) / `:419` (外側タップ) — 発火は確定経路のみ | `DateSelectionSheetTest.kt:468` (キャンセル) / `:479` (外側タップ設定) / `:484` (Back 含む各経路) / `:508` (下方向スワイプ相当) | ✅ 一致 |

### Requirement: 選択操作の意味論

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 移動中の確定は直前にスナップ静止した候補を採用する | `KsWheelView.kt:355-366` (`commitSnappedSelection` はスナップ距離 0 のときのみ確定) | `DateSelectionSheetTest.kt:526` / 単体は `KsWheelViewTest.kt:175` `:202` `:219` | ✅ 一致 |
| 候補領域の下方向操作はシートを閉じない | `DateSelectionSheet.kt:488` (`isNestedScrollingEnabled = true` を行全体へ) | `DateSelectionSheetTest.kt:543` | ✅ 一致 |

### Requirement: 選択面の強調色

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Cell 固有値が最優先される | `PickerSelectionSheet.kt:102` (`PickerSheetStyle.from(DatePickerCell, ...)` で段階解決) | `DateSelectionSheetTest.kt:583` | ✅ 一致 |
| Theme の既定色へフォールバックする | 同上 (共通の段階解決) | `DateSelectionSheetTest.kt:606` / 中間段は `:596` | ✅ 一致 |
| androidButtonColor は確定・キャンセル操作に引き継がれる | `DatePickerCellViewHolder.kt:133` (`androidButtonColor?.toArgb() ?: accentColor`) / `DateSelectionSheet.kt:350` (ヘッダーのみ差し替え) | `DateSelectionSheetTest.kt:616` / 未指定時は `:633` | ✅ 一致 |

### Requirement: 候補表示の Locale 追随

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 日本語 Locale の表示 | `DateSelectionSheet.kt:45-106` (`DateWheelLabels`、ICU の best pattern から導出) | `DateSelectionSheetTest.kt:661` | ✅ 一致 |
| 英語 Locale の表示 | 同上 (`SKELETON_MONTH = "MMM"` で月名表記) | `DateSelectionSheetTest.kt:670` | ✅ 一致 |
| (Requirement 本文) 系列の並び順は Locale によらず年→月→日で固定 | `DateSelectionSheet.kt:471` (`layoutDirection = LAYOUT_DIRECTION_LTR`) / `:476` (追加順) | `DateSelectionSheetTest.kt:679` (LTR) / `:689` (RTL Locale での左右位置) | ✅ 一致 |
| (Requirement 本文) アクセシビリティへ公開する表示文字列にも同じ表記 | `KsWheelView.kt:270-278` (`accessibilityText` が `selectedDisplayText` を使用) | `DateSelectionSheetTest.kt:733` | ✅ 一致 |

### Requirement: 候補のアクセシビリティ状態

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 各系列の選択中候補が公開される | `KsWheelView.kt:270-278` (`seriesLabel` + 選択値) / `:433` (`onInitializeAccessibilityNodeInfo`) | `DateSelectionSheetTest.kt:733` / 単体は `KsWheelViewTest.kt:431` | ✅ 一致 |
| アクセシビリティ操作で系列ごとに候補を変更できる | `KsWheelView.kt:443` (`performAccessibilityAction`) / `:454-460` (`selectAdjacent`) | `DateSelectionSheetTest.kt:751` / 日候補追随は `:768` | ✅ 一致 |
| (Requirement 本文) 先頭・末尾ではその方向へ変更しない | `KsWheelView.kt:435-441` (端では action を addAction しない) / `:455` (`itemIndices` 判定) | `KsWheelViewTest.kt:486` / `:501` | ✅ 一致 |
| (Requirement 本文) 選択中候補が変わると公開状態も更新 | `KsWheelView.kt:369-383` (`updateSelection` で `contentDescription` 更新 + イベント送出) | `KsWheelViewTest.kt:519` / `:532` | ✅ 一致 |

---

## 追加検査

### tasks.md

- 全 21 タスク中、**4.1 のみ未チェック**。虚偽チェック (未実装なのにチェック済み) は**なし** — 他 20 項目は上記
  対応表で実体を確認した
- 4.1 は「候補範囲・初期選択・丸めの単体テスト」で、列挙された 7 項目のうち 6 項目 (既定年範囲 / min-max 制限 /
  境界月制限 / 初期=date / 範囲外丸め / minDate > maxDate・空範囲) はテストが存在する。未了は「件数上限」の
  1 項目のみで、理由が設計メモに明記されている。**未チェックのまま残すのが正しい状態**であり、虚偽ではない

### 逆流検査 (足場アーティファクトの書き換え)

- `proposal.md` / `exploration.md` / `specs/settings-view-android-ui/spec.md` は HEAD から差分なし → **逆流なし**
- 変更されているのは `tasks.md` (進捗と設計メモ) と `ui/brief.md` (照合結果の追記) のみで、いずれも実装フェーズで
  更新してよいアーティファクト

### 未記録乖離

- deviation.md は存在しない (合意済み乖離なし)
- 対応表に ❌ は 0 件。⏸️ 保留 1 件は spec 側の Scenario 前提の問題として `tasks.md` に記録済みで、
  未記録の乖離ではない

### UI 変更の検査

- `ui/brief.md` に承認モックの記録あり: `mock/variant-b-today-below-wheels.html` を採用 (`approved.png`、
  2026-08-02 オーナー承認)。不採用の対案 `variant-a-today-in-header.html` も保存されている
- 照合結果表 (5 枚 / `ui/verification/`) が記録され、brief の検証条件 5 項目すべて判定 OK
- 合意済み妥協: 0 件と明記
- 注記: brief に「オーナーによる before/after の最終承認は未取得 (実装ワーカーからは提示のみ)」とあり、
  オーナー承認は本検証の範囲外として残っている

### テスト実行

- `cd android && ./gradlew test --rerun-tasks` → **BUILD SUCCESSFUL**
- debug variant: **748 tests / failures 0 / errors 0 / skipped 0** (release variant も同数実行)
- 本変更ぶん: `DateSelectionSheetTest` 43 件 / `KsWheelViewTest` 31 件

---

## 判定

**VALID**

全 9 Requirement / 31 Scenario のうち 30 Scenario が「✅ 一致」。残る 1 件
(「年候補件数が Int 上限を超える指定では提示しない」) は Requirement 本文の規則としては実装済みで、
Scenario の GIVEN が `LocalDate` の値域では到達不能であることが判明しているためテストのみ保留 —
`tasks.md` に記録済みのオーナー判断待ち事項であり、実装の欠落・乖離ではない。

虚偽チェックなし、足場アーティファクトの逆流なし、未記録乖離なし、テスト全件成功。
