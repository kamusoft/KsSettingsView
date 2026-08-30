# 一致検証: android-datepicker-spinner-wheel (002 — 最終)

**日付**: 2026-08-03
**判定**: VALID
**対象**: `specs/settings-view-android-ui/spec.md` (ADDED Requirement 9 件 / Scenario 31 件)

verify-001 で唯一「⏸️ 保留 (オーナー判断待ち)」だった Scenario が、オーナー決定 (年候補件数の上限を
`Int` 表現上限 → 提示上限 1,000,000 件へ引き直し) と spec / 実装 / テストの追随によって解消されたため、
全 31 Scenario を再検証して最終判定を出す。

パス表記の省略形:

- `DateSelectionSheet.kt` / `KsWheelView.kt` / `DatePickerCellViewHolder.kt` / `DatePickerCell.kt` = `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/`
- `DateSelectionSheetTest.kt` / `KsWheelViewTest.kt` / `InputCellsTest.kt` = `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/`
- `InputCellDsl.kt` / `InputCellDslTest.kt` = `android/ks-settingsview-compose/src/{main,test}/kotlin/jp/kamusoft/kssettingsview/compose/`

---

## 対応表

### Requirement: DatePickerCell (Spinner) 選択面の提示

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| タイトルの解決 | `DatePickerCellViewHolder.kt:127` | `DateSelectionSheetTest.kt:156` / `:162` | ✅ 一致 |
| 年・月・日の3系列が提示される | `DateSelectionSheet.kt:329-354` / `:477` | `DateSelectionSheetTest.kt:174` | ✅ 一致 |
| 無効 Cell は選択面を提示しない | `DatePickerCellViewHolder.kt:57-66` | `DateSelectionSheetTest.kt:189` | ✅ 一致 |
| (本文) 操作ラベルは OS 公開文字列リソース | `SheetHeaderView` 経由 (`android.R.string.ok` / `.cancel`) | `DateSelectionSheetTest.kt:167` | ✅ 一致 |

### Requirement: 日付候補の範囲と初期選択

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 未指定時の年候補の既定範囲 | `DateSelectionSheet.kt:218-224` / `:241-242` | `DateSelectionSheetTest.kt:196` | ✅ 一致 |
| minDate / maxDate による年候補の制限 | `DateSelectionSheet.kt:127` / `:132-135` | `DateSelectionSheetTest.kt:204` | ✅ 一致 |
| 境界年では月候補も制限される | `DateSelectionSheet.kt:143` / `:146` | `DateSelectionSheetTest.kt:218` / `:232` | ✅ 一致 |
| 初期選択は date | `DateSelectionSheet.kt:324-326` | `DateSelectionSheetTest.kt:243` | ✅ 一致 |
| 範囲外の date は最も近い範囲端へ丸めて提示する | `DateSelectionSheet.kt:199-208` (`resolve`) | `DateSelectionSheetTest.kt:252` | ✅ 一致 |
| minDate > maxDate では選択面を提示しない | `DateSelectionSheet.kt:243-255` | `DateSelectionSheetTest.kt:268` | ✅ 一致 |
| 既定適用後に範囲が空になる構成では提示しない | `DateSelectionSheet.kt:245-249` | `DateSelectionSheetTest.kt:281` | ✅ 一致 |
| **年候補件数が提示上限を超える指定では提示しない** | `DateSelectionSheet.kt:229` (`MAX_YEAR_CANDIDATE_COUNT = 1_000_000L`) / `:258-266` (64bit 算出 + 上限判定 + `Log.w`) | `DateSelectionSheetTest.kt:287` | ✅ **一致 (verify-001 の保留を解消)** |

> **保留解消の確認**: spec の Requirement 本文が「`Int` の表現上限 (2^31 − 1)」から「提示上限 1,000,000 件」へ、
> Scenario 名と GIVEN が「年候補件数 1,999,999,999 件が提示上限 1,000,000 件を超える」へ更新された。
> `LocalDate.MIN`/`MAX` の年差は `999999999 − (−999999999) + 1 = 1,999,999,999` 件で新上限を超えるため、
> Scenario の GIVEN が**到達可能**になった。実装は 64bit (`Long`) 算出を維持したまま上限比較へ置換され、
> テストは選択面の非提示 (`assertNull`) と警告ログ (`too many year candidates: 1999999999`) の双方を検証している。

### Requirement: 年・月の変更への日候補の追随

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 31 日から日数の少ない月への変更は末日へ丸める | `DateSelectionSheet.kt:199-202` / `:630-634` | `DateSelectionSheetTest.kt:299` | ✅ 一致 |
| 閏年の 2 月は 29 日まで列挙される | `DateSelectionSheet.kt:171-176` (`YearMonth.lengthOfMonth`) | `DateSelectionSheetTest.kt:309` | ✅ 一致 |
| 年の変更でも日が追随する | `DateSelectionSheet.kt:387-395` / `:616-638` | `DateSelectionSheetTest.kt:320` | ✅ 一致 |
| 年・月の変更で範囲外になった日付は範囲内の最近傍へ丸める | `DateSelectionSheet.kt:203-207` | `DateSelectionSheetTest.kt:330` / 実操作経路は `:347` | ✅ 一致 |

### Requirement: 今日へのジャンプ (todayText)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 今日へジャンプする | `DateSelectionSheet.kt:646-650` (`jumpToToday`) | `DateSelectionSheetTest.kt:365` / fling 競合は `:386` | ✅ 一致 |
| todayText が null または空文字なら操作を提示しない | `DateSelectionSheet.kt:367-368` / `:420-424` | `DateSelectionSheetTest.kt:409` / `:414` | ✅ 一致 |
| Material モードでは todayText は影響しない | `DatePickerCellViewHolder.kt:59-61` | `DateSelectionSheetTest.kt:437` | ✅ 一致 |
| 今日が範囲外なら何も変更しない | `DateSelectionSheet.kt:647-648` (`contains` で早期 return) | `DateSelectionSheetTest.kt:419` | ✅ 一致 |
| Compose DSL overload から todayText を指定できる | `InputCellDsl.kt:288` / `:307` | `InputCellDslTest.kt:227` / 既定は `:239` | ✅ 一致 |
| (本文) `DatePickerCell.todayText` の追加・既定 null | `DatePickerCell.kt:42` / `:69` / `:90` | `InputCellsTest.kt:700` / `:705` | ✅ 一致 |

### Requirement: 確定と非確定 dismiss

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 確定で選択日付を1回通知する | `DateSelectionSheet.kt:655-658` | `DateSelectionSheetTest.kt:464` | ✅ 一致 |
| 非確定 dismiss は経路によらず callback を発火しない | `DateSelectionSheet.kt:363` (`onCancel`) / `:428` (外側タップ) — 発火は確定経路のみ | `DateSelectionSheetTest.kt:478` / `:489` / `:494` / `:518` | ✅ 一致 |

### Requirement: 選択操作の意味論

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 移動中の確定は直前にスナップ静止した候補を採用する | `KsWheelView.kt:355-366` (`commitSnappedSelection`) | `DateSelectionSheetTest.kt:536` / 単体は `KsWheelViewTest.kt:175` `:202` `:219` | ✅ 一致 |
| 候補領域の下方向操作はシートを閉じない | `DateSelectionSheet.kt:497` (`isNestedScrollingEnabled`) | `DateSelectionSheetTest.kt:553` | ✅ 一致 |

### Requirement: 選択面の強調色

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Cell 固有値が最優先される | `PickerSelectionSheet.kt:102` | `DateSelectionSheetTest.kt:593` | ✅ 一致 |
| Theme の既定色へフォールバックする | 同上 | `DateSelectionSheetTest.kt:616` / 中間段は `:606` | ✅ 一致 |
| androidButtonColor は確定・キャンセル操作に引き継がれる | `DatePickerCellViewHolder.kt:133` / `DateSelectionSheet.kt:359` | `DateSelectionSheetTest.kt:626` / 未指定時は `:643` | ✅ 一致 |

### Requirement: 候補表示の Locale 追随

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 日本語 Locale の表示 | `DateSelectionSheet.kt:45-106` (`DateWheelLabels`) | `DateSelectionSheetTest.kt:671` | ✅ 一致 |
| 英語 Locale の表示 | 同上 (`SKELETON_MONTH = "MMM"`) | `DateSelectionSheetTest.kt:680` | ✅ 一致 |
| (本文) 系列の並び順は Locale によらず年→月→日で固定 | `DateSelectionSheet.kt:480` (`LAYOUT_DIRECTION_LTR`) / `:485` | `DateSelectionSheetTest.kt:689` (LTR) / `:699` (RTL Locale) | ✅ 一致 |
| (本文) アクセシビリティへ公開する表示文字列も同じ表記 | `KsWheelView.kt:270-278` | `DateSelectionSheetTest.kt:743` | ✅ 一致 |

### Requirement: 候補のアクセシビリティ状態

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 各系列の選択中候補が公開される | `KsWheelView.kt:270-278` / `:433` | `DateSelectionSheetTest.kt:743` / 単体は `KsWheelViewTest.kt:431` | ✅ 一致 |
| アクセシビリティ操作で系列ごとに候補を変更できる | `KsWheelView.kt:443` / `:454-460` | `DateSelectionSheetTest.kt:761` / 日候補追随は `:778` | ✅ 一致 |
| (本文) 先頭・末尾ではその方向へ変更しない | `KsWheelView.kt:435-441` / `:455` | `KsWheelViewTest.kt:486` / `:501` | ✅ 一致 |
| (本文) 選択中候補が変わると公開状態も更新 | `KsWheelView.kt:369-383` | `KsWheelViewTest.kt:519` / `:532` | ✅ 一致 |

---

## 追加検査

### tasks.md

- **全 21 タスクがチェック済み**。対応表と突き合わせて**虚偽チェックなし**を確認した
- verify-001 時点で唯一未チェックだった 4.1 (候補範囲・初期選択・丸めの単体テスト) は、最後まで欠けていた
  「件数上限の非提示」のテストが `DateSelectionSheetTest.kt:287` に追加されたことでチェック済みへ移行。
  実体を確認済みで妥当
- 設計メモの「4.1 の保留」節は「4.1 の保留は解消済み (提示上限 1,000,000 件への引き直し)」へ書き替えられ、
  旧上限では Scenario が到達不能だった経緯も残っている
- **nit** (判定に影響なし): 2.2 の本文が「年候補件数の Int 上限超過」という旧表現のまま。spec / 実装は
  「提示上限」へ移っているため文言が揃っていない

### 足場アーティファクトの変更 (逆流検査)

- `specs/settings-view-android-ui/spec.md` が**更新されている**。ただしこれは実装都合の逆流ではなく、
  **オーナー決定に基づきオーケストレーターが実施した spec 側の修正**である旨が本タスクの依頼で明示されている。
  変更内容は Requirement 本文の上限値と対応 Scenario の名前 / GIVEN のみで、他の Requirement / Scenario に
  波及していないことを diff で確認した → **逆流ではない**
- `proposal.md` / `exploration.md` は HEAD から差分なし
- その他の更新は `tasks.md` (進捗・設計メモ) と `ui/brief.md` (照合結果) のみ

### 未記録乖離

- deviation.md は存在しない (合意済み乖離なし)
- 対応表に ❌ 0 件、⏸️ 保留 0 件 → **未記録乖離なし**

### UI 変更の検査

- `ui/brief.md` に承認モックの記録あり (`mock/variant-b-today-below-wheels.html` を採用、`approved.png`、
  2026-08-02 オーナー承認)。不採用の対案も保存されている
- 照合結果表 (5 枚 / `ui/verification/`) が記録され、brief の検証条件 5 項目すべて判定 OK。合意済み妥協 0 件
- 今回の増分 (件数上限の引き直し) は提示できない構成のガードのみで、選択面の描画には一切影響しないため、
  視覚照合の再取得は不要
- 注記: brief に「オーナーによる before/after の最終承認は未取得 (実装ワーカーからは提示のみ)」とあり、
  オーナー承認は本検証の範囲外として残っている

### テスト実行

- `cd android && ./gradlew test` → **BUILD SUCCESSFUL**
- debug variant: **749 tests / failures 0 / errors 0 / skipped 0** (release variant も同数実行)
- 本変更ぶん: `DateSelectionSheetTest` 44 件 (verify-001 時点 43 件 → 今回の追加1件)、`KsWheelViewTest` 31 件

---

## 判定

**VALID**

全 9 Requirement / 31 Scenario が「✅ 一致」。verify-001 で保留していた「年候補件数が提示上限を超える
指定では提示しない」は、spec の上限引き直しにより GIVEN が到達可能になり、実装・テストとも追随済みで解消した。

虚偽チェックなし、未記録乖離なし、足場の逆流なし (spec 更新はオーナー決定に基づく正当な修正)、テスト全件成功。
**アーカイブ可能な状態**と判断する。

なお review-002 の追記で、`MAX_YEAR_CANDIDATE_COUNT` の根拠コメントが破綻水準を実際より3桁小さく
書いている点を Minor として指摘している。挙動・仕様一致には影響しないため本検証の判定には含めない。
