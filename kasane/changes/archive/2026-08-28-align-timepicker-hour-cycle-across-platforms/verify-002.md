# 一致検証: align-timepicker-hour-cycle-across-platforms (002 回目)

**日付**: 2026-08-28
**判定**: VALID

検証対象: HEAD (1d999dc) からの未コミット差分すべて (untracked 含む、45 パス)。deviation.md に記録された 12時間制の系列順の変更 (spec の後置き固定 → 端末 Locale の時刻パターン由来) は**合意済みの差分**として扱い、違反にしない。

## テスト・ビルド実行結果 (検証者が自分で実行)

| platform | コマンド | 結果 |
|---|---|---|
| Android | `cd android && ANDROID_HOME=<sdk> ./gradlew test --rerun-tasks` | BUILD SUCCESSFUL / **2626 tests / 0 failures / 0 errors** (debug+release、`*/build/test-results/*/TEST-*.xml` 集計) |
| iOS | `cd ios && xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,id=<iPhone 17 / iOS 26>'` | ** TEST SUCCEEDED ** / **619 tests / 0 failures** |
| MAUI | `cd maui && dotnet test KsSettingsView.Maui.Tests/KsSettingsView.Maui.Tests.csproj` | 成功 / **475 tests / 0 failures** |
| MAUI (Android TFM) | `cd maui && ANDROID_HOME=<sdk> dotnet build KsSettingsView.Maui/KsSettingsView.Maui.csproj -f net10.0-android` | ビルド成功 / 0 警告 0 エラー |
| MAUI (iOS TFM) | `cd maui && dotnet build KsSettingsView.Maui/KsSettingsView.Maui.csproj -f net10.0-ios` | ビルド成功 / 0 警告 0 エラー |

新設テストが実際に実行されていることを名指しでも確認した (`-only-testing:KsSettingsViewSwiftUITests/DSLTimePickerHourCycleTests` = 3 tests passed / `-only-testing:KsSettingsViewUITests/TimePickerHourCycleStoreUpdateTests` = 2 tests passed)。Android の新設 2 クラスは `TEST-*.xml` の存在で実行を確認済み。

## 対応表

### cell-types-input — Requirement: TimePickerCell の時制指定 (is24Hour) [ADDED]

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 既定は 24時間制 | `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/TimePickerCell.kt:35` / `ios/Sources/KsSettingsViewUI/TimePickerCell.swift:31,50` / `maui/KsSettingsView.Maui/TimePickerCell.cs:38-42` | `InputCellsTest.kt`「TimePickerCell 既定 is24Hour は true」/ `TimeSelectionSheetTest.kt`「既定は 24 時間制の2系列で初期選択は cell の時刻」/ `InputCellsTests.swift` `test_TimePickerCell_既定is24Hourは24時間制` `test_TimePickerCellView_既定は24時間制のpicker` / `CellShapeTests.cs:174` / `ConversionPathTests.cs` `TimePickerCellCarriesDefaultHourCycleAs24Hour` | ✅ 一致 |
| is24Hour = false で 12時間制 | `TimeSelectionSheet.kt` (`TimeCandidates.of` が `cell.is24Hour` を直読み) / `TimePickerCellView.swift:69` + `HourCycleLocale.swift` / `Internals/KsCellSnapshots.cs:212` | `TimeSelectionSheetTest.kt`「is24Hour false は 12 時間制の3系列で初期選択は cell の時刻」/ `test_TimePickerCellView_is24Hourfalseは12時間制のpickerで初期値はcellのtime` / `TimePickerCellCarriesExplicitTwelveHourCycle` | ✅ 一致 |
| format は時制に影響しない | `TimeSelectionSheet.kt` (`timeFormatUsesAmPm` を関数ごと削除) / `TimePickerCellView.swift:69` | `TimeSelectionSheetTest.kt`「format の a は時制に影響しない」「12 時間制の指定は format に依らず 3 系列になる」/ `test_TimePickerCellView_formatは時制に関与しない` (valueText の AM/PM 表記と picker の 24時間制を同時に検証) | ✅ 一致 |
| 表示済み Cell の is24Hour 変更が反映される | `TimePickerCell.kt:58,75` (equals/hashCode) / `TimePickerCell.swift:121,138` (==/hash) / `TimePickerCell.cs:119` (`AffectsSnapshot`) | Android: `TimePickerHourCycleStoreUpdateTest`「Store の内容更新で is24Hour の変更が次に開く選択面へ届く」+ `DSLTimePickerHourCycleRenderingTest` 2 件 / iOS: `TimePickerHourCycleStoreUpdateTests` 2 件 + `DSLTimePickerHourCycleTests` 3 件 / MAUI: `TimePickerCellHourCycleChangeIsDeliveredToGateway` | ✅ 一致 |

### android-timepicker — Requirement: 時制の決定と候補系列 [MODIFIED]

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 既定は 24 時間制 | `TimeSelectionSheet.kt` (`TimeCandidates.of`) | 「既定は 24 時間制の2系列で初期選択は cell の時刻」「24 時間制の系列順は時 分で Locale に依らない」 | ✅ 一致 |
| is24Hour = false は 12 時間制 | 同上 | 「is24Hour false は 12 時間制の3系列で初期選択は cell の時刻」(系列数・選択中の 時2 / 分30 / 午後 を検証) | ✅ 一致 (系列**順**は deviation。下記参照) |
| format の a は時制に影響しない | 同上 | 「format の a は時制に影響しない」 | ✅ 一致 |
| 12 時間制の深夜と正午の境界 | `TimeSelectionSheet.kt` の `TimeCandidates` 変換 | 「12 時間制の深夜は 12 午前として提示され確定で 0 時になる」「12 時間制の正午は 12 午後として提示され確定で 12 時になる」(いずれも `is24Hour = false` 前提) | ✅ 一致 |

**deviation 該当 (合意済み差分)**: Requirement 本文の「時 1–12 / 分 0–59 / 午前・午後 の3系列」という**並び**は、`TimeSelectionSheet.orderedWheels` と `TimeWheelLabels.isPeriodLeading` により端末 Locale の 12時間表記パターン由来へ変わっている。deviation.md に記録済みのため違反としない。系列の**構成** (3 系列・候補範囲・選択中の値・確定往復) は spec のまま満たされている。この差分は「12 時間制の系列順は端末 Locale の時刻表記に従う（午前午後が先）」(qualifiers=ja) と「（午前午後が後）」(`@Config(qualifiers = "en")`) の対テストで固定され、行の並び (`seriesRow` の子順) まで観測している。

MODIFIED の残骸検査: `timeFormatUsesAmPm` は実装・テストとも消滅 (`git grep` で `android/` 配下ヒットなし)。`DateFormat.is24HourFormat` の参照も `android/` 配下に存在しない。

### android-timepicker — Requirement: Compose DSL の is24Hour 指定 [ADDED]

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| DSL の指定が native cell へ透過する | `android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/InputCellDsl.kt:246,260` | `InputCellDslTest.kt`「TimePickerCell DSL の is24Hour が native cell へ透過する」+ 既定の `assertTrue("既定は 24 時間制", cell.is24Hour)`。加えて `DSLTimePickerHourCycleRenderingTest` が DSL → 表示までを実観測 | ✅ 一致 |

### ios-timepicker — Requirement: 時制の決定 (端末設定非依存) [ADDED]

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 既定は 24 時間制 | `ios/Sources/KsSettingsViewUI/TimePickerCellView.swift:69` + `ios/Sources/KsSettingsViewUI/HourCycleLocale.swift` | `test_TimePickerCellView_既定は24時間制のpicker` (picker の実 locale を `j` テンプレート解決で判定)。証跡 `evidence/ios-input-cells-demo-alarm-picker-24h.png` | ✅ 一致 |
| is24Hour = false は 12 時間制 | 同上 | `test_TimePickerCellView_is24Hourfalseは12時間制のpickerで初期値はcellのtime` + 証跡 `evidence/ios-input-cells-demo-bedtime-picker-12h.png` (AM/PM 系列あり・10/15/PM 選択中) | ✅ 一致 |
| 表記の言語は端末 Locale を保つ | `HourCycleLocale.forcing` が `Locale.Components(locale: base)` の hourCycle だけを差し替える | `test_HourCycleLocale_時制の強制でも表記の言語は基準Localeを保つ` (ja_JP 基準で `amSymbol == "午前"`)、`test_HourCycleLocale_時制は基準Localeの既定時制に依存しない` (en_US / ja_JP 双方向)、`test_HourCycleLocale_端末Locale基準でも両方の時制が得られる` (キャッシュ経路の反復と言語保持) | ✅ 一致 |
| 12 時間制でも確定値の往復が保たれる | `TimePickerCellView.swift` の `handleDone` (既存経路) | `test_TimePickerCellView_12時間制でも確定値の往復が保たれる` (年月日保持 + 14:45) + 証跡 `evidence/ios-input-cells-demo-bedtime-committed-12h.png` | ✅ 一致 |

### maui-bridge — Requirement: 時制フラグの輸送 [ADDED]

| Scenario | 実装 | テスト・証跡 | 状態 |
|---|---|---|---|
| 指定値が native cell へ写る (両 OS) | `android/ks-settingsview-bridge/src/main/kotlin/jp/kamusoft/kssettingsview/bridge/KsBridgeTimePickerCell.kt:35,54` / `ios/Sources/KsSettingsViewBridge/KsBridgeTimePickerCell.swift:25,59` | `KsBridgeCellConversionTest.kt`「TimePickerCell DTO の is24Hour が Native へ写る」/ `KsBridgeCellConversionTests.swift` `test_TimePickerCellDTOのis24HourがNativeへ写る` | ✅ 一致 |
| 未指定は native 既定に落ちる (両 OS) | `KsBridgeTimePickerCell.kt:76` (`DEFAULT_IS_24_HOUR`) / `KsBridgeTimePickerCell.swift:45` (`defaultIs24Hour`)。いずれも native 既定から引く | 既存 DTO 変換テストへ `assertEquals("未指定の時制は Native 既定になる", true, cell?.is24Hour)` / `XCTAssertEqual(cell?.is24Hour, true, ...)` を追加 | ✅ 一致 |
| MAUI facade の値が gateway を透過する (両 OS) | iOS: `maui/KsSettingsView.Maui/Platforms/iOS/KsBridgeGateway.cs:412` + `maui/macios/KsSettingsView.Binding.iOS/ApiDefinition.cs:512-516` / Android: `maui/KsSettingsView.Maui/Platforms/Android/KsBridgeGateway.cs:414-416` (`dto.Set24Hour(...)`) | 自動テストは host 側テストから到達不能。**iOS**: `evidence/maui-input-cells-demo-bedtime-picker-12h.png` (ja_JP・端末既定 24時間制の Simulator で 12時間制 picker)。**Android**: `evidence/maui-android-input-cells-demo-bedtime-picker-12h.png` (システム Locale en-rUS・`time_12_24 = 24` のエミュレータで 時/分/AM・PM の 3 系列・10/15/PM 選択中) — `Set24Hour` 経路を実行時に通した証跡 | ✅ 一致 (001 の ❌-1 は解消) |
| (Requirement 本文) iOS binding assembly が is24Hour を露出する | `ApiDefinition.cs:512-516` | `dotnet build -f net10.0-ios` 成功 + MAUI iOS 証跡が実経路を通す | ✅ 一致 |

### maui-cells — Requirement: TimePickerCell の Is24Hour 指定 [ADDED]

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 既定 true が snapshot に透過する | `maui/KsSettingsView.Maui/TimePickerCell.cs:38-42,112` / `maui/KsSettingsView.Maui/Internals/KsCellSnapshots.cs:212` | `TimePickerCellCarriesDefaultHourCycleAs24Hour` / `CellShapeTests.cs:174` | ✅ 一致 |
| false 指定が snapshot に透過する | 同上 | `TimePickerCellCarriesExplicitTwelveHourCycle` (`Format` が独立して保たれることも同時に検証) | ✅ 一致 |
| 表示済み Cell の Is24Hour 変更が再送出される | `TimePickerCell.cs:119` (`AffectsSnapshot` に `Is24Hour` 参加) | `TimePickerCellHourCycleChangeIsDeliveredToGateway` (`ReplaceCell` の CellId / NewCell / Snapshot.Is24Hour) | ✅ 一致 |

### samples-android / samples-ios / samples-maui

| Scenario | 実装 | 証跡 | 状態 |
|---|---|---|---|
| [samples-android] デモ行で 12時間制の選択面が提示される | `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/InputCellsDemoScreen.kt:227-236` (`is24Hour = false` 明示・`format = "h:mm a"` 維持) | `evidence/android-input-cells-demo-bedtime-picker-12h.png` (午前/午後・時・分 の 3 系列、午後/10/15 選択中、行 "10:15 午後") | ✅ 一致 |
| [samples-ios] デモ行で 12時間制の picker が提示される | `samples/ios/KsSettingsViewSample/InputCellsDemoView.swift:215-221` (title「就寝」/ 22:15 / pickerTitle「就寝時刻」/ format `"h:mm a"` / `is24Hour: false`) | `evidence/ios-input-cells-demo-bedtime-picker-12h.png` (AM/PM 系列あり、行 "10:15 PM") | ✅ 一致 |
| [samples-maui] デモ行で 12時間制の選択面が提示される | `samples/maui/KsSettingsView.Sample.Maui/Pages/InputCellsDemoPage.xaml:86-94` + `ViewModels/InputCellsDemoViewModel.cs:30,190-200` | `evidence/maui-input-cells-demo-bedtime-picker-12h.png` (iOS ホスト・ja_JP)、`evidence/maui-android-input-cells-demo-bedtime-picker-12h.png` (Android ホスト・en-rUS) | ✅ 一致 |

3 platform の文言・初期値・構成 (Section header「TimePickerCell」/ Cell 2 行 / title「アラーム」「就寝」/ 初期値 07:30・22:15 / format `"HH:mm"`・`"h:mm a"` / pickerTitle「アラーム時刻」「就寝時刻」) の一致を再確認した。

## 追加検査

- **tasks.md の虚偽チェック**: 1.1〜5.4 の全 16 タスクを対応表と突き合わせた。虚偽チェックなし。前回残っていた 4.3「gateway 変換」の未検証分は Android ホスト証跡の追加で埋まった (自動テストではなく実行時証跡での担保であることは上表に明記)。
- **逆流検査**: `git diff HEAD -- kasane/` は tasks.md のチェックボックス 16 行のみ。proposal.md / specs/ ×8 / exploration.md / second-opinion-spec-001.md は無変更。**足場の逆流なし**。
- **未記録乖離**: deviation.md 記載の 1 件 (12時間制の系列順) 以外に、diff の中で Scenario に対応しない挙動変更は見つからなかった。付随修正の記載はなし。
- **証跡の実在と提出コードの対応** (lessons/process L-003 (4)): evidence/ の 8 枚をすべて開いて確認した。Android native (アプリ単位 Locale ja-JP) は日本語 UI と午前/午後前置き、MAUI/Android (アプリ単位 Locale 未設定 → システム en-rUS) は英語 UI と AM/PM 後置きで、`capture-environment.txt` の記載と画像が整合する (001 の 🟡-2 は解消)。ja では 3 系列が「午前/午後・時・分」になっており、deviation の locale 由来の並びが実物で成立している。
- **回帰検出力の実測** (lessons/code-review L-001): `TimePickerCell` の等価判定から `is24Hour` を外すミューテーションを両 OS に入れて実測した。Android は `DSLTimePickerHourCycleRenderingTest` の 2 件が両脚 (Store 側「Store 経路で 12 時間制の 3 系列にならない」/ DSL 側) で失敗し、`InputCellsTest` `TimeSelectionSheetTest` の該当 2 件も失敗。iOS は `DSLTimePickerHourCycleTests` の 3 件が失敗。**DSL 検出層の取りこぼし (無音の失敗) を実際に検出できる**ことを確認した。Store 経路テストはこのミューテーションでは落ちない (Store の `replaceCell` は明示操作であり等価判定に依存しないため、想定どおり)。ミューテーションは backup からの復元と shasum 一致で原状復帰済み (Android `f539719b…` / iOS `fe4b2808…`)。
- **UI 変更**: 本 change は ui/ を作らない方針 (proposal「Impact」)。承認モックのゲートは適用外。

## 判定理由

デルタスペック 8 本の全 Requirement / Scenario に実装とテスト (または動作証跡) の対応があり、001 の ❌-1 (maui-bridge の Android 側検証欠落) は Android ホストの実行時証跡で埋まった。deviation.md 記載の系列順の差分は合意済み差分として除外し、それ以外に未記録の乖離はない。したがって **VALID**。

品質面の指摘 (サンプルコメントの系列順記述が実挙動と食い違う) は [review-002.md](review-002.md) を参照。一致検証の判定には影響しない。
