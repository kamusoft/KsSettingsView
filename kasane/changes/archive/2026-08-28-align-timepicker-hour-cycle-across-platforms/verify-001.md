# 一致検証: align-timepicker-hour-cycle-across-platforms (001 回目)

**日付**: 2026-08-28
**判定**: INVALID (❌ 1 件)

検証対象: HEAD (1d999dc) からの未コミット差分すべて。deviation.md は存在しない (合意済み乖離の記録なし)。

## テスト実行結果 (レビュアーが自分で実行)

| platform | コマンド | 結果 |
|---|---|---|
| Android | `cd android && ANDROID_HOME=<sdk> ./gradlew test --rerun-tasks` | BUILD SUCCESSFUL / **2614 tests / 0 failures / 0 errors** (debug+release、`build/test-results/*/TEST-*.xml` 集計) |
| iOS | `cd ios && xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,id=<iPhone 17 / iOS 26.0.1>'` | ** TEST SUCCEEDED ** / **616 tests / 0 failures** |
| MAUI | `cd maui && dotnet test KsSettingsView.Maui.Tests/KsSettingsView.Maui.Tests.csproj` | 成功 / **475 tests / 0 failures** |
| MAUI (Android TFM ビルド) | `cd maui && ANDROID_HOME=<sdk> dotnet build KsSettingsView.Maui/KsSettingsView.Maui.csproj -f net10.0-android` | ビルド成功 / 0 警告 0 エラー |

## 対応表

### cell-types-input — Requirement: TimePickerCell の時制指定 (is24Hour) [ADDED]

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 既定は 24時間制 | `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/TimePickerCell.kt:35` / `ios/Sources/KsSettingsViewUI/TimePickerCell.swift:50` / `maui/KsSettingsView.Maui/TimePickerCell.cs:38` | `InputCellsTest.kt`「TimePickerCell 既定 is24Hour は true」/ `TimeSelectionSheetTest.kt`「既定は 24 時間制の2系列で初期選択は cell の時刻」/ `InputCellsTests.swift` `test_TimePickerCell_既定is24Hourは24時間制` `test_TimePickerCellView_既定は24時間制のpicker` / `CellShapeTests.cs:174` `ConversionPathTests.cs` `TimePickerCellCarriesDefaultHourCycleAs24Hour` | ✅ 一致 |
| is24Hour = false で 12時間制 | `TimeSelectionSheet.kt:209` / `TimePickerCellView.swift:69` / `KsCellSnapshots.cs:212` | `TimeSelectionSheetTest.kt`「is24Hour false は 12 時間制の3系列で初期選択は cell の時刻」/ `test_TimePickerCellView_is24Hourfalseは12時間制のpickerで初期値はcellのtime` / `TimePickerCellCarriesExplicitTwelveHourCycle` | ✅ 一致 |
| format は時制に影響しない | `TimeSelectionSheet.kt:209` (`format` 参照を撤去) / `TimePickerCellView.swift:69` | `TimeSelectionSheetTest.kt`「format の a は時制に影響しない」「12 時間制の指定は format に依らず 3 系列になる」/ `test_TimePickerCellView_formatは時制に関与しない` (valueText の AM/PM 表記と picker 24時間制を同時に検証) | ✅ 一致 |
| 表示済み Cell の is24Hour 変更が反映される | `TimePickerCell.kt:58,75` (equals/hashCode) / `TimePickerCell.swift:121,138` (Equatable/hash) / `TimePickerCell.cs:119` (`AffectsSnapshot`) | `TimeSelectionSheetTest.kt`「表示済み Cell の is24Hour 変更が次の選択面に反映される」(ViewHolder 実 bind → 実タップ → 実シートで往復) / `test_TimePickerCellView_表示済みCellのis24Hour変更が次のpickerに反映される` / `TimePickerCellHourCycleChangeIsDeliveredToGateway` | ✅ 一致 |

### android-timepicker — Requirement: 時制の決定と候補系列 [MODIFIED]

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 既定は 24 時間制 | `TimeSelectionSheet.kt:209` | `TimeSelectionSheetTest.kt`「既定は 24 時間制の2系列で初期選択は cell の時刻」 | ✅ 一致 |
| is24Hour = false は 12 時間制 | 同上 | 「is24Hour false は 12 時間制の3系列で初期選択は cell の時刻」 | ✅ 一致 |
| format の a は時制に影響しない | 同上 (`timeFormatUsesAmPm` を関数ごと削除) | 「format の a は時制に影響しない」 | ✅ 一致 |
| 12 時間制の深夜と正午の境界 | `TimeSelectionSheet.kt:135-180` | 「12 時間制の深夜は 12 午前として提示され確定で 0 時になる」「12 時間制の正午は 12 午後として提示され確定で 12 時になる」(いずれも `is24Hour = false` 前提へ移行済み) | ✅ 一致 |

MODIFIED の残骸検査: `timeFormatUsesAmPm` は実装・テストとも完全に削除済み (リポジトリ全体 grep で `openspec/` と `kasane/concepts/` の歴史・未追随記述以外にヒットなし)。端末設定参照 (`DateFormat.is24HourFormat`) は `android/` 配下に存在しない。Requirement 本文の「午前/午後は端末 Locale の表記から導出」は既存テスト「午前午後のラベルは端末 Locale の表記から導出される」(`@Config(qualifiers = "en")`) が継続担保。

### android-timepicker — Requirement: Compose DSL の is24Hour 指定 [ADDED]

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| DSL の指定が native cell へ透過する | `android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/InputCellDsl.kt:246,260` | `InputCellDslTest.kt`「TimePickerCell DSL の is24Hour が native cell へ透過する」+ 既定の `assertTrue("既定は 24 時間制", cell.is24Hour)` | ✅ 一致 |

### ios-timepicker — Requirement: 時制の決定 (端末設定非依存) [ADDED]

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 既定は 24 時間制 | `ios/Sources/KsSettingsViewUI/TimePickerCellView.swift:69` + `ios/Sources/KsSettingsViewUI/HourCycleLocale.swift:22-26` | `test_TimePickerCellView_既定は24時間制のpicker` (picker の実 locale を `j` テンプレート解決で判定)。証跡 `evidence/ios-input-cells-demo-alarm-picker-24h.png` は 12時間制既定の en 端末上で 2 系列 (0–23) を示す | ✅ 一致 |
| is24Hour = false は 12 時間制 | 同上 | `test_TimePickerCellView_is24Hourfalseは12時間制のpickerで初期値はcellのtime` + 証跡 `evidence/ios-input-cells-demo-bedtime-picker-12h.png` (10 / 15 / PM 選択中) | ✅ 一致 |
| 表記の言語は端末 Locale を保つ | `HourCycleLocale.forcing` が `Locale.Components(locale: base)` の hourCycle だけを差し替える | `test_TimePickerCellView_時制の強制でも表記の言語は端末Localeを保つ` (ja_JP 基準で `amSymbol == "午前"`)、`test_HourCycleLocale_時制は基準Localeの既定時制に依存しない` (en_US / ja_JP の双方向) | ✅ 一致 |
| 12 時間制でも確定値の往復が保たれる | `TimePickerCellView.swift` の `handleDone` (既存経路、変更なし) | `test_TimePickerCellView_12時間制でも確定値の往復が保たれる` (年月日保持 + 14:45) + 証跡 `evidence/ios-input-cells-demo-bedtime-committed-12h.png` | ✅ 一致 |

### maui-bridge — Requirement: 時制フラグの輸送 [ADDED]

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 指定値が native cell へ写る (両 OS) | `android/ks-settingsview-bridge/.../KsBridgeTimePickerCell.kt:35,54` / `ios/Sources/KsSettingsViewBridge/KsBridgeTimePickerCell.swift:25,59` | `KsBridgeCellConversionTest.kt`「TimePickerCell DTO の is24Hour が Native へ写る」/ `KsBridgeCellConversionTests.swift` `test_TimePickerCellDTOのis24HourがNativeへ写る` | ✅ 一致 |
| 未指定は native 既定に落ちる (両 OS) | `KsBridgeTimePickerCell.kt:76` / `KsBridgeTimePickerCell.swift:45` (いずれも native 既定から引く) | 既存 DTO 変換テストへ `assertEquals("未指定の時制は Native 既定になる", true, cell?.is24Hour)` / `XCTAssertEqual(cell?.is24Hour, true, ...)` を追加 | ✅ 一致 |
| MAUI facade の値が gateway を透過する (両 OS) | iOS: `maui/KsSettingsView.Maui/Platforms/iOS/KsBridgeGateway.cs:412` + `maui/macios/KsSettingsView.Binding.iOS/ApiDefinition.cs:512-515` / Android: `maui/KsSettingsView.Maui/Platforms/Android/KsBridgeGateway.cs:416` | **iOS**: 自動テストなし。ただし `evidence/maui-input-cells-demo-bedtime-picker-12h.png` が facade→snapshot→iOS gateway→DTO→native の end-to-end を示す (ja_JP / 端末既定 24時間制の Simulator で 12時間制の picker)。**Android**: 自動テストなし・証跡なし | ❌ 欠落 (Android 側) |
| (Requirement 本文) iOS binding assembly が is24Hour を C# 側へ露出する | `ApiDefinition.cs:512-515` | 自動テストなし。MAUI iOS 証跡が実経路を通していることで担保 | ✅ 一致 |

### maui-cells — Requirement: TimePickerCell の Is24Hour 指定 [ADDED]

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 既定 true が snapshot に透過する | `maui/KsSettingsView.Maui/TimePickerCell.cs:38-43,112` / `Internals/KsCellSnapshots.cs:212` | `ConversionPathTests.cs` `TimePickerCellCarriesDefaultHourCycleAs24Hour` / `CellShapeTests.cs:174` | ✅ 一致 |
| false 指定が snapshot に透過する | 同上 | `TimePickerCellCarriesExplicitTwelveHourCycle` (`Format` が独立して保たれることも同時に検証) | ✅ 一致 |
| 表示済み Cell の Is24Hour 変更が再送出される | `TimePickerCell.cs:119` (`AffectsSnapshot`) | `TimePickerCellHourCycleChangeIsDeliveredToGateway` (`ReplaceCell` の CellId / NewCell / Snapshot.Is24Hour を検証) | ✅ 一致 |

### samples-android / samples-ios / samples-maui

| Scenario | 実装 | テスト・証跡 | 状態 |
|---|---|---|---|
| [samples-android] デモ行で 12時間制の選択面が提示される | `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/InputCellsDemoScreen.kt:235` (`is24Hour = false` 明示、`format = "h:mm a"` 維持) | `evidence/android-input-cells-demo-bedtime-picker-12h.png` (時 / 分 / 午前・午後 の 3 系列、10 / 15 / 午後 選択中) | ✅ 一致 |
| [samples-ios] デモ行で 12時間制の picker が提示される | `samples/ios/KsSettingsViewSample/InputCellsDemoView.swift:214-220` (title「就寝」/ 初期値 22:15 / pickerTitle「就寝時刻」/ format `"h:mm a"`) | `evidence/ios-input-cells-demo-bedtime-picker-12h.png` (AM/PM 系列あり、行 valueText「10:15 PM」) | ✅ 一致 |
| [samples-maui] デモ行で 12時間制の選択面が提示される | `samples/maui/KsSettingsView.Sample.Maui/Pages/InputCellsDemoPage.xaml:90-94` + `ViewModels/InputCellsDemoViewModel.cs:30,191-199` | `evidence/maui-input-cells-demo-bedtime-picker-12h.png` (午前/午後 系列あり、行 valueText「10:15 午後」) | ✅ 一致 |

3 platform の文言・初期値・構成 (Section header「TimePickerCell」/ footer なし / Cell 2 行 / title「アラーム」「就寝」/ 初期値 07:30・22:15 / format `"HH:mm"`・`"h:mm a"` / pickerTitle「アラーム時刻」「就寝時刻") は一致を確認した。

## 追加検査

- **tasks.md の虚偽チェック**: 1.1〜5.4 の全 16 タスクについて、対応表の実装・テストと突き合わせて実体を確認した。虚偽チェックは検出されなかった。ただし 4.3 の「gateway 変換」は、追加された 3 テストのうち `TimePickerCellHourCycleChangeIsDeliveredToGateway` (fake gateway への配信検証) を指す読みでのみ充足する。platform 別の `Platforms/{Android,iOS}/KsBridgeGateway.cs` の DTO 変換そのものには自動テストがない (本リポジトリの構造上、host 側テストからは到達できない)。
- **逆流検査 (足場アーティファクトの書き換え)**: `git diff HEAD -- kasane/` は `tasks.md` のチェックボックス 16 行のみ。proposal.md / specs/ ×8 / exploration.md / second-opinion-spec-001.md はいずれも無変更。**逆流なし**。
- **未記録乖離**: deviation.md は不在。対応表の ❌ 1 件 (下記) 以外に、diff の中で Scenario に対応しない変更は見つからなかった (samples の 12時間制デモ追加・コメント更新はいずれも samples-* の Requirement に対応)。
- **UI 変更**: 本 change は ui/ を作らない方針 (proposal「Impact」で明示。新規の視覚要素・レイアウト判断がないため)。承認モックのゲートは適用外。
- **テスト全件成功**: 上表のとおり 3 platform とも実行して確認済み。

## ❌ の詳細と見立て

### ❌-1 maui-bridge「MAUI facade の値が gateway を透過する」— Android 側の検証が存在しない

**該当箇所**: `maui/KsSettingsView.Maui/Platforms/Android/KsBridgeGateway.cs:414-416`

Scenario の THEN は「**両 OS** の TimePicker bridge DTO の `is24Hour` は `false` である」と両 OS を名指ししている。iOS 側は MAUI サンプルの実機証跡 (`evidence/maui-input-cells-demo-bedtime-picker-12h.png`) が facade から native までを通しで示すが、Android 側は自動テストも視覚証跡も存在しない。加えてこの 1 行は、Kotlin の `is` 接頭辞プロパティが binding 生成器でプロパティにまとまらないことを前提とした、本リポジトリで唯一の手書き setter 直呼びである。

レビュアー側で以下まで確認済み (実装の誤りは見つからなかった):

- `dotnet build KsSettingsView.Maui.csproj -f net10.0-android` が 0 警告 0 エラーで成功する
- 生成された binding (`maui/android/KsSettingsView.Binding.Android/obj/Debug/net10.0-android/generated/src/KsSettingsView.Bridge.KsBridgeTimePickerCell.cs:317-341`) は実際にプロパティではなくメソッド `Is24Hour()` / `Set24Hour(Java.Lang.Boolean?)` を出しており、JNI シグネチャは `set24Hour.(Ljava/lang/Boolean;)V` で Kotlin 側 setter と一致する

**見立て**: 実装を直す必要はない。不足しているのは Android ホストでの検証であり、**証跡側で埋めるのが妥当** — MAUI サンプルを Android エミュレータで起動し、12時間制デモ行の選択面 1 枚を `evidence/` に追加する (Android は端末既定が 24時間制でも 12時間制シートが出ることを示せる)。証跡を足さない判断をするなら「MAUI の視覚確認は iOS ホストのみで代表させる」を deviation として記録する必要がある (実装の誤りではないため、deviation で合意する選択肢も成立する)。決定はオーナーの判断。
