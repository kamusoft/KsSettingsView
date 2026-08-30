# Verify 001: relax-android-host-prerequisites

- 検証日: 2026-08-27
- 対象: コミット `68ac115` 以降の作業ツリー全差分 (untracked 新規ファイルを含む)
- 対象デルタスペック: `specs/android-theming/spec.md` / `specs/android-timepicker/spec.md` / `specs/android-datepicker/spec.md`
- 判定: **VALID**

パスは、コードはリポジトリ相対、証跡は change 相対で書く。テスト名は Kotlin のバッククォート関数名。

---

## 1. android-theming

### Requirement: ホストテーマ前提の撤廃

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 非 Material3 テーマでの表示 | `android/ks-settingsview-ui/src/main/res/values/themes.xml` (`Theme.KsSettingsView.Internal` = `Theme.Material3.DayNight.NoActionBar`)、`android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsThemedContext.kt:77` (`ksThemedContext`)、適用点 `CellBaseLayout.kt:119` / `KsCellRegistry.kt:237` / `SectionAccessoryViewHolders.kt:111,244` | `HostThemeIndependenceTest`: `全 Cell 種はフレームワーク標準テーマのホストで例外なく表示される` / `全 Cell 種は素の AppCompat テーマのホストで例外なく表示される` / `テーマを被せない素の Context でも全 Cell 種の ViewHolder を生成できる` | ✅ 一致 |
| MAUI テンプレート既定テーマでの動作 | `samples/maui/KsSettingsView.Sample.Maui/Platforms/Android/MainActivity.cs` (`Theme = "@style/Maui.SplashTheme"`)、同 `AndroidManifest.xml` (application のテーマ指定を削除) | 実機証跡: `evidence/verify-maui-01-basic-cells.png` / `-02-basic-cells-scrolled.png` / `-03-timepicker-sheet.png` / `-04-timepicker-confirmed.png` / `-05-datepicker-calendar.png` 〜 `-15-compose-header-footer.png` | ✅ 一致 (実機証跡) |

- 「ライブラリ既定の配色で描画される」の観測は、下の「ホストテーマからの視覚隔離」の2テスト (タイトル既定色・SwitchCell 配色) が担う。
- `全 Cell 種...` の2テストは、`SwitchCell` の `MaterialSwitch` が生成できていること (旧実装で例外になっていた箇所) を明示的に assert している。

### Requirement: ホストテーマからの視覚隔離

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 利用者所有コンテンツはホストテーマのまま | `KsThemedContext.kt:102` (`ksHostContext`)、適用点 `KsCellRegistryCustomCell.kt:29` / `SectionAccessoryViewHolders.kt:199,274,429` | `HostThemeIndependenceTest`: `KsAnyView の利用者 View はホストの Context で解決される` / `CustomCell の content はホストの Context で解決される` (いずれもライブラリ所有の行が別の値を解くことを併せて assert し、空振りを防いでいる) | ✅ 一致 |
| ホストテーマ色からの隔離 | 常時ラップ (上記) + `EffectiveStyle.kt:resolveDefaultTitleColor` がラップ済み Context で解決 | `HostThemeIndependenceTest`: `ホストのテーマが変わっても SwitchCell の配色は変わらない` / `ホストのテーマが変わってもタイトル既定色は変わらない` | ✅ 一致 |
| ButtonCell タイトル既定色の固定化 | `EffectiveStyle.kt:effectiveButtonTitleColorArgb` (`MaterialColors.getColor(view, colorPrimary, …)` を撤去し `SYSTEM_BLUE_ARGB` 固定へ)、呼び出し側 `ButtonCellViewHolder.kt` から `view` 引数が消えている | `HostThemeIndependenceTest`: `ButtonCell のタイトル既定色はホストの colorPrimary に追従しない` | ✅ 一致 |

- Scenario「ホストテーマ色からの隔離」の THEN は「SwitchCell / **シート等**の配色」を含む。シート単体のホストテーマ A/B テストは無いが、シートの生成 Context は常時ラップ (`TimeSelectionSheet.kt:278` / `NumberSelectionSheet.kt:47` / `PickerSelectionSheet.kt:189` / `DateSelectionSheet.kt:309` がいずれも `hostContext.ksThemedContext()`) であり、配色値は `PickerSheetStyle` = `Theme` / `CellStyle` 由来 (`TimeSelectionSheetTest`: `強調色は Cell 指定を最優先に段階解決される` 等) なので、ホストテーマの入る経路が構造的に存在しない。加えて実機証跡 `ui/verification/datepicker-m3-140-outofrange-nightmode.png` が「端末の明暗を変えても配色が変わらない」を示す。乖離としない。

### Requirement: 選択面のホストテーマ非依存

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 非 Material3 テーマでの選択面提示 | `NumberSelectionSheet.kt:47` ほか4シートの `ksThemedContext()` ラップ | `HostThemeIndependenceTest`: `非 Material3 テーマのホストでも選択面を提示して確定できる` / `非 Material3 テーマのホストで選択面を取消しても通知しない` (ホストは `android.R.style.Theme_Material_Light_NoActionBar`) | ✅ 一致 |
| (補強) 非 M3 ホストでの各シートの実機表示 | — | 実機証跡: `evidence/verify-samples-android-05-pickercell-sheet.png` / `-06-numberpicker-sheet.png` / `-09-datepicker-wheel-sheet.png` | ✅ 一致 (実機証跡) |

---

## 2. android-timepicker

### Requirement: ホスト前提に依存しない時刻選択面の提示

| Scenario / 契約項 | 実装 | テスト | 状態 |
|---|---|---|---|
| ComponentActivity ホストでの提示 | `TimePickerCellViewHolder.kt:showSelectionSheet` → `TimeSelectionSheet.kt:271`。`showAnchoredTo` (`HostAnchoredDialog.kt:24`) で提示 | `TimeSelectionSheetTest`: `ComponentActivity ホストの行タップで選択面が提示される` (ホストは `ComponentActivity` + フレームワーク標準テーマ) | ✅ 一致 |
| 無効 Cell のタップ | `TimePickerCellViewHolder.bind` の `isEnabled` 分岐 | `TimeSelectionSheetTest`: `無効 Cell の行タップでは選択面を提示しない` | ✅ 一致 |
| タイトル解決 (`pickerTitle` → `title`) | `TimePickerCellViewHolder.kt:showSelectionSheet` の `cell.pickerTitle ?: cell.title` | `TimeSelectionSheetTest`: `タイトルは pickerTitle を優先して解決する` / `pickerTitle が null のときタイトルは title を使う` | ✅ 一致 |
| Material 時計ダイヤルを提示しない (SHALL NOT) | `MaterialTimePicker` の参照ゼロ (`android/` `samples/` `maui/` の全ソースを grep。ヒットは gitignore 対象の JVM replay ログと MAUI の生成 obj のみ) | 経路そのものが存在しない | ✅ 一致 |

### Requirement: 時制の決定と候補系列

| Scenario / 契約項 | 実装 | テスト | 状態 |
|---|---|---|---|
| 既定 format は 24 時間制 | `TimeSelectionSheet.kt:40` `timeFormatUsesAmPm` / `TimeCandidates` | `TimeSelectionSheetTest`: `既定 format は 24 時間制の2系列で初期選択は cell の時刻` | ✅ 一致 |
| AM/PM format は 12 時間制 | 同上 (`hourCount` 12 + `periodWheel`) | `AM PM format は 12 時間制の3系列で初期選択は cell の時刻` | ✅ 一致 |
| 引用リテラル内の a は判定に影響しない | `timeFormatUsesAmPm` の引用符スキャン (`''` エスケープ込み) | `引用リテラル内の a は 12 時間制の判定に影響しない` / `大文字 A は 12 時間制の判定に影響しない` / `format 判定は引用符の外の小文字 a だけを見る` | ✅ 一致 |
| 12 時間制の深夜と正午の境界 | `TimeCandidates.timeOf` / `displayHourOf` | `12 時間制の深夜は 12 午前として提示され確定で 0 時になる` / `12 時間制の正午は 12 午後として提示され確定で 12 時になる` / `12 時間制は午前午後の切替で 12 時間ずれた時刻になる` | ✅ 一致 |
| 午前/午後は Locale 由来・自前翻訳を持たない (SHALL NOT) | `TimeWheelLabels.resolvePeriodTexts` (`DateTimeFormatter` → `DateFormatSymbols` → `Locale.ROOT`)。`strings.xml` に AM/PM 文字列を追加していない | `午前午後のラベルは端末 Locale の表記から導出される`、`操作ラベルは OS の公開文字列リソースから解決される` | ✅ 一致 |
| 初期選択は開いた時点の `cell.time` | `TimeSelectionSheet.selectedTime` の初期化 (`truncatedTo(MINUTES)`) | 上記2件 + `秒以下を持つ時刻でも分単位の選択として開ける` | ✅ 一致 |

### Requirement: 確定のみ反映

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 確定で1回発火 | `TimeSelectionSheet.confirmSelection` | `TimeSelectionSheetTest`: `確定で選択中の時刻を1回だけ通知して閉じる` | ✅ 一致 |
| 非確定 dismiss は無発火 | `cancel()` / `setCanceledOnTouchOutside(true)` / `BottomSheetBehavior` の下スワイプいずれも callback を通らない | `取消では通知しない` / `外側タップ相当の cancel では通知しない` / `下スワイプ相当の dismiss では通知しない` | ✅ 一致 |

### Requirement: 構成変更で閉じる

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 回転で閉じて無発火 | `HostAnchoredDialog.kt:24` の `ON_DESTROY` 購読 (シートは保存対象に含めない) | `TimeSelectionSheetTest`: `構成変更で Activity が再生成されると選択面は閉じられ再提示も通知もしない` | ✅ 一致 |

### REMOVED Requirement: Material 時計ダイヤルダイアログの配色契約

| 対象 | 削除の確認 | 状態 |
|---|---|---|
| `TimePickerColorizer.kt` (400 行) / `TimePickerColorizerTest.kt` (369 行) | 削除済み。`PickerDialogColors` の時刻ダイアログ専用派生色 (`intermediateSurface` / `accentTint` と両透過率定数) も撤去 | ✅ 一致 (残骸なし) |

- 派生色4件とそのテスト4件の削除は `deviation.md` (19) にオーナー承認込みで記録済み。

---

## 3. android-datepicker

### Requirement: ホスト前提に依存しないカレンダー選択面の提示

| Scenario / 契約項 | 実装 | テスト | 状態 |
|---|---|---|---|
| ComponentActivity ホストでの提示 | `DateCalendarDialog.kt:189` (`ComponentDialog` + `ComposeView` + M3 `DatePicker`)、`DatePickerCellViewHolder.kt:showCalendarDialog` | `DateCalendarDialogTest`: `ComponentActivity ホストの行タップで選択面が提示される` / `無効 Cell の行タップでは選択面を提示しない` | ✅ 一致 |
| 入力モードの切替 | `DateCalendarDialog.kt:318` `showModeToggle = true`、`DatePickerState.displayMode` | `DateCalendarDialogTest`: `カレンダー表示で開き入力モードへ切り替えられる` (Picker → Input → Picker の往復)。テキスト入力の実描画は実機証跡 `ui/verification/datepicker-m3-140-textinput-light.png` / `-textinput-dark.png` / `-textinput-light-keyboard-hidden.png` | ✅ 一致 |
| タイトル解決 | `cell.pickerTitle ?: cell.title` | `タイトルは pickerTitle を優先して解決する` / `pickerTitle が null のときタイトルは title を使う` | ✅ 一致 |
| ランドスケープの高さ制約 (tasks 4.6) | `DateCalendarDialog.kt:59` `dateCalendarSurfaceMaxHeightDp` + `heightIn` + `verticalScroll` | `面の高さの上限は画面の高さから影の余白を差し引いた値になる` / `画面が低いほど上限も下がり操作行の余地が残る` / `画面が余白より低くても上限は負にならない`。実機証跡 `ui/verification/datepicker-landscape-fixed-*.png` (6点) / `datepicker-portrait-after-height-cap.png` | ✅ 一致 |

### Requirement: 確定のみ反映 (共通契約の維持)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 確定で1回発火 | `DateCalendarDialog.confirmSelection` | `DateCalendarDialogTest`: `確定で選択日を1回だけ通知して閉じる` / `選択日が定まらない状態では確定しても通知しない` | ✅ 一致 |
| 非確定 dismiss は無発火 | `cancel()` / `setCanceledOnTouchOutside(true)` / `ComponentDialog` の Back | `取消では通知しない` / `外側タップや Back での dismiss では通知しない`。ホスト破棄経路は `ホストが破棄されると選択面を閉じる` | ✅ 一致 |
| (補強) MAUI 実機での非確定破棄 | — | 実機証跡: `evidence/verify-maui-10-datepicker-before-cancel.png` → `-11-datepicker-cancelled-nofire.png` | ✅ 一致 (実機証跡) |

### Requirement: 範囲制限 (共通契約の維持)

| Scenario / 契約項 | 実装 | テスト | 状態 |
|---|---|---|---|
| 範囲外日付の選択不可 (両モード) | `DateCalendarRange.selectableDates` (`isSelectableDate` / `isSelectableYear`) を `DatePickerState` へ渡す = カレンダー・テキスト入力の両モードが同じ判定を参照 | `DateCalendarDialogTest`: `範囲外の日付は選択できず範囲内の日付は選択できる` / `年候補は範囲の年に限られる` | ✅ 一致 |
| 範囲外の初期値は範囲端へ丸めて提示 | `DatePickerCellViewHolder`: `initialDate = range.clamp(cell.date)` | `範囲より前の初期値は minDate へ丸めて提示する` / `範囲より後の初期値は maxDate へ丸めて提示する` | ✅ 一致 |
| 年の候補範囲の既定 1900 / 2100 | `DateCalendarRange.of` が `DateCandidates.DEFAULT_MIN_YEAR` / `DEFAULT_MAX_YEAR` を適用 (ホイール型と共有) | `年候補の既定は 1900 年から 2100 年まで` / `minDate だけの指定では上限側に既定が入る` | ✅ 一致 |
| (spec 沈黙) 不正範囲の防御 | `DateCalendarRange.of` が `minDate > maxDate` で `null` を返し提示しない | `minDate が maxDate より後なら選択面を提示しない` | ⚠️ deviation 記録済み (28) |

### Requirement: タイムゾーンに依存しない日付の往復

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 非 UTC タイムゾーンでの日付一致 | `LocalDate.toEpochMilliUtc()` / `Long.toLocalDateUtc()` (`ZoneOffset.UTC` 固定)。端末 TZ 参照は `todayProvider` = `LocalDate.now()` のみ | `DateCalendarDialogTest`: `UTC から東へ離れたタイムゾーンでも確定日は選択日と一致する` (Pacific/Kiritimati) / `UTC から西へ離れたタイムゾーンでも確定日は選択日と一致する` (Pacific/Midway) | ✅ 一致 |

### Requirement: 今日ジャンプ (共通契約の維持)

| Scenario / 契約項 | 実装 | テスト | 状態 |
|---|---|---|---|
| ジャンプは発火しない | `DateCalendarDialog.jumpToToday` (状態の書き換えのみ) | `今日ジャンプは選択日と表示月を今日へ移し通知しない` / `今日ジャンプ後の確定は今日を通知する` | ✅ 一致 |
| 範囲外セーフガード | `if (!range.contains(target)) return` | `今日が範囲外なら今日ジャンプで選択状態は変化しない` / `境界日の今日は範囲内として今日ジャンプが成立する` | ✅ 一致 |
| `todayText` の提示条件 (非 null かつ非空) | `cell.todayText?.takeIf { it.isNotEmpty() }` + `ActionRow` の `todayText?.let` | `todayText 未指定では今日操作を提示しない` / `todayText が空文字なら今日操作を提示しない` / `todayText 指定時は今日操作を提示する` | ✅ 一致 |
| 冪等・入力モードからの復帰 | `jumpToToday` が `displayMode = Picker` を含む | `今日ジャンプの連続実行は冪等` / `入力モード表示中の今日ジャンプはカレンダー表示へ戻して成立する` | ✅ 一致 |
| (補強) 実機での非発火 | — | 実機証跡: `evidence/verify-samples-android-10-datepicker-today-jump-nofire.png` / `-11-datepicker-confirmed.png` | ✅ 一致 (実機証跡) |

### Requirement: 構成変更をまたぐ表示継続

| Scenario / 契約項 | 実装 | テスト | 状態 |
|---|---|---|---|
| Activity 再生成をまたぐ状態維持 | `KsSettingsView.kt:866` `onSaveInstanceState` / `:877` `onRestoreInstanceState` / `:922` `restoreCalendarDialogIfPending`、`SavedState` (`:1044`)、`DateCalendarDisplayState` (`DateCalendarDialog.kt:143`)、既定 ID 付与 `KsSettingsView.kt:234` + `res/values/ids.xml` | `DateCalendarRecreationTest`: `再生成をまたいで選択日と表示月と表示モードを維持して提示し直す` / `再生成後の選択面の確定は維持した選択日を1回だけ通知する` / `再生成後の選択面は現 Cell の範囲制限を反映する` / `選択面を開いていなければ再生成後に提示されない`。実機証跡 `evidence/datepicker-rotation-01-before-row.png` 〜 `-07-after-confirmed.png` (連番7点) | ✅ 一致 |
| in-place 構成変更では開いたまま生存 | 保存時に dismiss しない設計 (`onSaveInstanceState` は控えるだけ)。閉じるのは `showAnchoredTo` の `ON_DESTROY` と detach 経路 | `DateCalendarRecreationTest`: `状態保存だけで破棄が続かなければ選択面は開いたまま選択も保つ` (背面遷移相当) + `ホストの破棄をまたぐときは再生成前の選択面が閉じられ通知しない`。in-place 回転そのものは実機証跡 `evidence/verify-maui-06-datepicker-selected.png` (前) → `-07-datepicker-rotate-landscape.png` (横・生存) → `-08-datepicker-rotate-back-portrait.png` (縦復帰・選択維持) → `-09-datepicker-confirmed.png` | ✅ 一致 (テスト + 実機証跡) |
| 対応 Cell 不在時は復元しない | `findCalendarRestoreTarget` (`id` 一致 + `uiStyle == Material` + `singleOrNull`) | `該当 id が現 root に無ければ再提示せず誤発火しない` / `uiStyle が変更されていたら再提示しない` / `同一 id の候補が複数なら再提示しない` | ✅ 一致 |
| 復元後も配色・今日ジャンプ・確定/破棄が有効 | 復元経路が通常提示と同じ `DateCalendarDialog` を組む (`restoredState` 付き)。色は `resolveDatePickerDialogColors` を提示時と共有し、`EffectiveStyle` はラップ済み Context で解決 (`KsSettingsView.kt:928`) | `再生成後の選択面は提示時と同じ色ロールで組み立てられる` / `再生成後の選択面でも今日ジャンプが成立し通知しない` / `再生成後の選択面でも取消では通知しない` | ✅ 一致 |
| 本文 3 (状態保存に参加できない形態の注記) | スパイクで該当形態は出ず。代わりに「ライブラリ既定 ID のインスタンスが同一階層に複数」を縮退条件として実装 (`hasAmbiguousLibraryDefaultId`) | `ID 未設定の KsSettingsView はライブラリ既定の ID を自分へ付ける` / `ホストが与えた ID は上書きしない` / `ホストが個別の ID を与えた複数インスタンスでは対象の View だけが復元する` / `ID 未設定のインスタンスが複数あるときは復元しない` | ⚠️ deviation 記録済み (14, 15) |

### REMOVED Requirements

| 対象 | 削除の確認 | 状態 |
|---|---|---|
| Material ダイアログの View 走査配色とヘッダ補正 | `DatePickerColorizer.kt` (654 行) / `MaterialDatePickerPresenter.kt` (133 行) / `PickerDialogTag.kt` / `PickerRestoreRegistry.kt` を削除。対応テスト `DatePickerColorizerTest` / `DatePickerDialogIntegrationTest` / `DatePickerMaterialContractTest` / `PickerDialogTagTest` / `PickerRestoreRegistryTest` / `PickerDialogRecreationTest` も削除。新配色は `DateCalendarDialog.datePickerColors()` の4色ロール正面指定 | ✅ 一致 (残骸なし) |
| 今日ジャンプの View 階層駆動 | `DatePickerTodayShortcut.kt` (309 行) / `DatePickerTodayShortcutTest.kt` を削除。`res/values/ids.xml` の `ks_date_picker_today_button` も撤去 | ✅ 一致 (残骸なし) |

- 旧テストの削除タイミング (タスク 6.1 の部分前倒し) は `deviation.md` (9, 10, 13) に記録済み。

---

## 4. 追加検査

### tasks.md の突き合わせ

- 全 21 タスク中、未チェックは 2 件のみ。いずれも意図的な未チェックで虚偽ではない。
  - `1.3 エスカレーション条件` — 1.1 / 1.2 が成立したため発動しない条件付きタスク (`evidence/spike-findings.md`)。
  - `8.4 concepts の追随は distill で実施` — 対象メモを残すこと自体が内容で、実施は蒸留フェーズ。
- チェック済みタスクはいずれも対応表の実装・テスト・証跡と裏が取れる。**未実装のチェックは見つからなかった。**
  - 3.4 / 4.5 / 4.6 の視覚照合 → `ui/brief.md` の照合結果節 + `ui/verification/` の証跡。
  - 5.3 の回転連番 → `evidence/datepicker-rotation-01..07`。
  - 6.2 の依存撤去 → `android/ks-settingsview-ui/build.gradle.kts` (`fragment-ktx` → `androidx.activity:activity`) / binding csproj (`Xamarin.AndroidX.Fragment.Ktx` 削除)。
  - 7.1〜7.4 → 各 MainActivity / AndroidManifest / csproj の差分で確認。
  - 8.2 → `evidence/ab-visual-map.md` (未取得: なし)。

### 逆流検査

- `git diff 68ac115 -- kasane/` の変更は `tasks.md` と `ui/brief.md` の2ファイルのみ。`proposal.md` / `design.md` / `specs/*` は実装期間中に**書き換えられていない**。足場凍結を満たす。

### 未記録乖離

- **なし。** diff の全ファイルを走査し、Scenario に対応しない変更 (JVM ヒープ設定・KDoc 追随・テストのダーク駆動変更・シート3種へのホスト破棄追随・`showAnchoredTo` 共有ヘルパ化に伴う internal API 変更・samples の 12h デモ追加と依存整理・compose BOM 引き上げ) はすべて `deviation.md` に `[付随修正]` または合意記録として存在する。

### UI 変更の確認

- `ui/brief.md` に承認モックの記録あり (`mock/approved-timepicker.png` = 案A / `mock/approved-datepicker.png` = 案A、案Bはいずれも不採用)。
- 合意済み妥協・トークン候補の記録あり (12h の mock フィラー差、分の2桁ゼロ詰め、`yMMMEd` 骨格、面の寸法、ランドスケープの高さ制約)。
- material3 1.4.0 への引き上げ後の再照合結果も記録あり (判定: 一致)。

### テスト実行

- `./gradlew test` を実行: **BUILD SUCCESSFUL**。
- 集計 (`*/build/test-results/test*UnitTest/TEST-*.xml`): **2560 件 / failures 0 / errors 0 / skipped 0**。
  - 内訳: ks-settingsview-ui 1838 / ks-settingsview-bridge 322 / ks-settingsview-compose 240 / ks-settingsview-core 160。
  - 本変更の主要テスト: `HostThemeIndependenceTest` 10 / `TimeSelectionSheetTest` 22 / `DateCalendarDialogTest` 37 / `DateCalendarRecreationTest` 16 / `SheetHostDestructionTest` 3 / `PickerDialogColorRolesTest` 18 — いずれも失敗 0。

---

## 5. 判定

**VALID** — 全 Requirement / Scenario が「✅ 一致」または「⚠️ deviation 記録済み」。虚偽チェックなし、逆流なし、テスト全件成功。❌ は 0 件。

### 参考所見 (判定には影響しない)

判定の根拠 (Scenario 一致・虚偽・逆流・テスト) には当たらないが、蒸留前に直しておくと良い点を1件だけ挙げる。

- `ui/brief.md` の「照合結果 — カレンダー選択ダイアログ (2026-08-27)」節と「ランドスケープ」節が、削除済みの material3 1.3.1 期の証跡ファイル名を参照したままになっている (参照切れ 9 件: `datepicker-calendar-light.png` / `datepicker-calendar-outofrange-dark.png` / `datepicker-textinput-light.png` / `datepicker-textinput-focused-light.png` / `datepicker-textinput-dark.png` / `datepicker-yearselect-light.png` / `datepicker-yearselect-dark.png` / `datepicker-outofrange-light.png` / `datepicker-outofrange-nightmode.png`)。`deviation.md` (27) では `evidence/ab-visual-map.md` の B 側参照だけを m3-140 系へ張り替えており、`ui/brief.md` 側は追随していない。実体としては同じ状態を `datepicker-m3-140-*` が覆っているため証跡の欠落ではなく、記述の参照切れ。
