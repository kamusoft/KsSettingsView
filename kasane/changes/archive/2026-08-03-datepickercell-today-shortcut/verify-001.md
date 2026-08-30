# 検証結果: datepickercell-today-shortcut (001 回目)

**日付**: 2026-08-03
**判定**: VALID
**対象**: `specs/settings-view-android-ui/spec.md` (ADDED 3 Requirements / MODIFIED 1 Requirement、計 15 Scenario) と 89bac6a 以降の未コミット変更

---

## 対応表

パスはリポジトリルートからの相対。行番号は検証時点のもの。

- 実装 A = `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DatePickerTodayShortcut.kt`
- 実装 B = `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DatePickerCellViewHolder.kt`
- 実装 C = `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DatePickerColorizer.kt`
- テスト T = `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/DatePickerTodayShortcutTest.kt`

### ADDED / Requirement: カレンダーモードの今日ショートカットの提示

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| todayText 指定時に操作が提示される | 実装 B:147-165 (`todayText` 非空で `DatePickerTodayShortcut` を生成し Colorizer のフックへ相乗り)、実装 C:134-138 (`onViewCreated` で差し込み)、実装 A:86-106 (`install`) | T:62 `todayText 指定でカレンダーに今日ボタンが提示される`、T:88 (左端配置)、T:110 / T:137 (ボタン行が無い構成) | ✅ 一致 |
| todayText が null または空文字なら提示しない | 実装 B:147 `cell.todayText?.takeIf { it.isNotEmpty() }` (null なら shortcut 自体を生成しない) | T:71 (null)、T:80 (空文字)。いずれもボタン非在に加えボタン行の子数 2 を固定し「構成が現行と変わらない」を検証 | ✅ 一致 |
| 操作のラベルがアクセシビリティに公開される | 実装 A:114-129 (`Button` に `text = label`)、実装 A:124 (専用 ID) | T:174 `createAccessibilityNodeInfo()` の `text` と `isClickable` を検証 | ✅ 一致 |

補助: ラベルの色ロール (accent) は実装 C:272-275 / 実装 A:126、テストは T:183。

### ADDED / Requirement: カレンダーの今日への移動

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 表示月と選択日が今日になる | 実装 A:163-190 (`jumpToToday`)、A:200-230 (`driveJump` = 経路 A) | T:192 (表示月・選択日・ヘッダ選択日テキスト・callback 0 回) | ✅ 一致 |
| 値の確定は確定操作の責務のまま | 実装 A:221 (`performItemClick` で material 側の選択更新のみ)、実装 B:136-142 (`addOnPositiveButtonClickListener` が唯一の通知点) | T:210 (確定で 1 回)、T:223 (取消で 0 回)、T:413 (再 build 経路でも 1 回) | ✅ 一致 |
| 今日が範囲外なら何も変更しない | 実装 A:151-155 (`isSelectable`)、A:164-165 (押下時に最初に判定して早期 return) | T:237 (表示月・選択日とも不変、callback 0 回) | ✅ 一致 |
| min/max 当日は有効 (日単位比較) | 実装 A:151-155 (`LocalDate` 比較のため時刻成分を持たない) | T:256 (min = 今日)、T:271 (max = 今日)、T:286 (min = max = 今日) | ✅ 一致 |
| 移動の完了前に閉じられたら再提示しない | 実装 A:68-72 (`onViewDestroyed` で世代を進める)、A:208 (世代不一致で早期 return)、実装 C:150-154 (`onFragmentViewDestroyed` からの通知) | T:303 (押下直後に dismiss → picker 数 0・callback 0 回) | ✅ 一致 |
| (Requirement 本文) 連続実行の結果は 1 回と同じ | 実装 A:62 / A:166 (`isJumping` の門) | T:319 (3 連打で picker 数 1・表示月と選択日は今日・callback 0 回) | ✅ 一致 |
| (Requirement 本文) 選択日の表示・確定操作の状態も追随 | 実装 A:221 (material の通知チェーンに乗せる) | T:192 (ヘッダ選択日)、T:387 (選択セルの表示と確定操作の有効状態) | ✅ 一致 |

### ADDED / Requirement: 代替表示状態からの今日への移動

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 年選択の表示中から今日へ移動する | 実装 A:168-172 (年選択の可視判定とモードトグルの `performClick`)、A:183-189 (引き戻しスクロールの後に移動を走らせる `post`) | T:340 (年選択を開いた状態から → 日グリッド復帰・表示月・選択日・callback 0 回) | ✅ 一致 |
| テキスト入力の表示中から今日へ移動する | 実装 A:174-179 (月ページャ不在で経路 D へ)、A:233-238 (`requestRebuild`)、実装 B:155-165 (`onRebuildRequired` = dismiss → `setSelection`/`setOpenAt` で作り直し) | T:367 (カレンダー表示への切替・表示月・選択日・picker 数 1・今日ボタンの再提示・callback 0 回)、T:413 (確定で 1 回) | ✅ 一致 |
| 不完全なテキスト入力で無効化された確定操作が今日への移動で有効に戻る | 実装 同上 (作り直しで選択日 = 今日の新しい picker になる) | T:387 (確定操作が無効 → 移動後に選択セル表示と有効状態が復帰) | ✅ 一致 |
| (Requirement 本文) 範囲外の扱いは同じ | 実装 A:164-165 (範囲判定が表示状態の分岐より前) | T:237 (共通経路のため代替表示状態でも同じ早期 return を通る) | ✅ 一致 |

### MODIFIED / Requirement: 今日へのジャンプ (todayText)

変更後の全文のうち、Spinner 側の記述は変更前と同一で、追加されたのは「`uiStyle = Material` の選択 UI での提示と挙動は ADDED Requirements に定める」の 1 文。したがって Spinner 側は既存実装・既存テストの維持、Material 側は上記 ADDED 3 Requirement で充足する。

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 今日へジャンプする | `DateSelectionSheet.kt:369-371` / `:650-652` (既存、無変更) | `DateSelectionSheetTest.kt:365`、`:386` (慣性中) | ✅ 一致 (回帰なし) |
| todayText が null または空文字なら操作を提示しない | `DateSelectionSheet.kt:370-371` (既存、無変更) | `DateSelectionSheetTest.kt:409`、`:414` | ✅ 一致 (回帰なし) |
| 今日が範囲外なら何も変更しない | `DateSelectionSheet.kt:650-652` (既存、無変更) | `DateSelectionSheetTest.kt:419` | ✅ 一致 (回帰なし) |
| Compose DSL overload から todayText を指定できる | `InputCellDsl.kt:288` / `:307` (既存、無変更) | `InputCellDslTest.kt:227`、`:239` | ✅ 一致 (回帰なし) |
| (追加文) Material の提示と挙動は ADDED に従う | 実装 B:147-165 | T (22 件) | ✅ 一致 |

**旧挙動テストの残存**: `DateSelectionSheetTest.kt:437 Material モードでは todayText は選択 UI に影響しない` は、Material モードで **3 連ホイールの選択面が提示されないこと** だけを assert しており、ADDED Requirement と矛盾する assertion は無い。ただしテスト名と付随コメントの文言が本変更で陳腐化しているため、review-002 で Minor として指摘した (検証上は ❌ ではない)。

---

## 追加検査

### tasks.md の虚偽チェック

15 タスクすべてが `[x]`。対応表と突き合わせた結果、虚偽のチェックは無い。

| タスク | 裏付け |
|---|---|
| 1.1 スパイク (待ち合わせの実測) | 実装 A:200-230 の `post` + 上限リトライが T の Robolectric テスト 22 件で成立。上限を使い切った場合の倒れ先 (経路 D) も T:161 / T:367 で駆動を確認 |
| 2.1 ボタン注入 (行が無い構成のフォールバック込み) | 実装 A:86-106 / T:88・T:110・T:137 |
| 2.2 MaterialIds 追加 + 契約テスト | 実装 C:574-575 (`MONTHS` / `DATE_PICKER_ACTIONS`)、`DatePickerMaterialContractTest.kt:70-90`・`:116-126` |
| 2.3 承認 mock との視覚照合 | `ui/brief.md` の「照合結果」節 (静的 5 観点 + 動的 6 条件、証跡 9 枚が `ui/verification/` に実在) |
| 3.1 押下時の範囲チェック | 実装 A:151-155・164-165 / T:237・256・271・286 |
| 3.2 経路 A | 実装 A:200-230・269-295 / T:192 |
| 3.3 年選択からの復帰 | 実装 A:168-172・183-189 / T:340 |
| 3.4 フォールバック D | 実装 A:233-238、実装 B:155-165 / T:367・T:161 |
| 3.5 single-flight とキャンセル・tag 世代分離 | 実装 A:59-72・166・208、実装 B:143-146 (`baseTag` + `.r<n>`) / T:303・T:319 |
| 4.1〜4.3 Scenario 対応テスト | 上記対応表のとおり網羅 |
| 4.4 Spinner 系の回帰確認 | `DateSelectionSheetTest` / `InputCellDslTest` を含む全件成功 |
| 4.5 ViewHolder 公開経路の統合テスト | T:431-445 (`openPicker` が `DatePickerCellViewHolder.create` → `todayProvider` 注入 → `bind` → 行タップの実経路)。経路 A は T:210、経路 D は T:413 で確定 callback 回数を検証 |
| 5.1 iOS コメント修正 | `ios/Sources/KsSettingsViewUI/DatePickerCalendarSheetController.swift:11-13` (コメントのみ、コード変更 0 行) |

### 逆流検査 (足場アーティファクトの書き換え)

`git diff HEAD -- kasane/` の結果、本変更ディレクトリで変更されているのは以下の 2 ファイルのみ。

- `tasks.md`: 全 15 行の `[ ]` → `[x]` のみ。本文の追加・削除・改変なし
- `ui/brief.md`: 末尾に「照合結果」節を追記 (削除行 0)。承認モックの記述・検証条件は無変更

**正である `proposal.md` と `specs/settings-view-android-ui/spec.md` は 89bac6a から無変更** (`git diff HEAD --stat` に現れない)。逆流なし。

`kasane/changes/fix-picker-dialog-recreation/exploration.md` への追記 (申し送り 2 点) は本変更の足場ではなく、review-001 のアクションプラン 1 に対応する別 change への引き継ぎ記録。逆流には当たらない。

### 未記録乖離

対応表に ❌ は無く、`deviation.md` は存在しない (spec からの乖離なし) — 整合している。

なお、**Fragment 復元 (画面回転) 後に今日ボタン・OK リスナー・着色が失われる**点は、`fix-picker-dialog-recreation` へ切り出し済みの既知の構造問題としてオーナー決定済み (2026-08-02、2026-08-03 に本変更分を同 change の exploration.md へ申し送り済み)。本変更のスコープ外であり、未記録乖離としては扱わない。

### UI 変更の検査

- `ui/brief.md` に承認モックの記録あり (`mock/variant-a-today-text-button.html` を採用、`approved.png`、2026-08-03 オーナー承認)
- 合意済み妥協の記録: 「乖離なし・実装側の修正なし・プラットフォーム制約による妥協なし・トークン候補なし」と明記
- フルスクリーン構成のフォールバック配置は brief.md が明示的に mock 照合の対象外としており、提示と挙動のみの検証で足りる

### テスト実行

`android/` で `./gradlew test lintDebug` → **BUILD SUCCESSFUL**。
テスト結果 XML の集計: **1676 件 (debug + release の 2 variant) / failures 0 / errors 0 / skipped 0**。
本変更の新規テストクラス `DatePickerTodayShortcutTest` は 22 件・failures 0。lint (`lintDebug`) も違反なし。

---

## 判定

全 Requirement / Scenario が「✅ 一致」。虚偽チェックなし、逆流なし、未記録乖離なし、テスト全件成功。

**VALID**
