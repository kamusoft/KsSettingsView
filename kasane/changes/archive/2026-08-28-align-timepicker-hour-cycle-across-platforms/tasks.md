# Tasks: align-timepicker-hour-cycle-across-platforms

## 1. Android native

- [x] 1.1 `TimePickerCell` (android/ks-settingsview-ui) に `is24Hour: Boolean = true` を追加 (→ Requirement: TimePickerCell の時制指定 (is24Hour))
- [x] 1.2 `TimeSelectionSheet` の時制決定を `is24Hour` へ置換し、`timeFormatUsesAmPm` を決定源から撤去する (→ Requirement: 時制の決定と候補系列)
- [x] 1.3 時制テストの改修: format 判定ケースを is24Hour ケースへ移行、「format の `a` は時制に影響しない」テストを追加、12h 境界往復は `is24Hour = false` 前提へ (→ Requirement: 時制の決定と候補系列 全 Scenario)
- [x] 1.4 `TimePickerCell` の手書き equals/hashCode に `is24Hour` を参加させ、同一 ID 更新の反映テストを追加 (→ Requirement: TimePickerCell の時制指定 Scenario「表示済み Cell の is24Hour 変更が反映される」)
- [x] 1.5 Compose TwoWay DSL 拡張関数 (`InputCellDsl`) に `is24Hour` 引数を追加して native へ透過し、DSL テストを追加 (→ Requirement: Compose DSL の is24Hour 指定)

## 2. iOS native

- [x] 2.1 `TimePickerCell` (ios/Sources/KsSettingsViewUI) に `is24Hour: Bool = true` を追加し、手書き Equatable/hash と再構築 helper (`withDSLID` / `withStyle` 等) にも参加させる (→ Requirement: TimePickerCell の時制指定 (is24Hour) — 更新反映 Scenario 含む)
- [x] 2.2 `TimePickerCellView` の picker 時制を `is24Hour` で決定 (端末設定非依存化)。上書きは hour cycle のみに作用させ、表記の言語は端末 Locale を保つ (→ Requirement: 時制の決定 (端末設定非依存))
- [x] 2.3 iOS テスト新設: 時制の決定・format 非関与・表記言語の維持・同一 ID 更新の反映・12時間制での確定往復 (→ Requirement: 時制の決定 (端末設定非依存) 全 Scenario)

## 3. Bridge (Compose / SwiftUI)

- [x] 3.1 `KsBridgeTimePickerCell` (両 OS) に nullable な `is24Hour` を追加し、resolve で null → native 既定へ落とす (→ Requirement: 時制フラグの輸送)
- [x] 3.2 bridge resolve テスト: 指定値の透過・null の既定落ち (両 OS) (→ Requirement: 時制フラグの輸送 Scenario 1・2)
- [x] 3.3 iOS binding assembly (`maui/macios/KsSettingsView.Binding.iOS/ApiDefinition.cs` の `KsBridgeTimePickerCell`) に `is24Hour` の binding を追加する (→ Requirement: 時制フラグの輸送)

## 4. MAUI facade

- [x] 4.1 facade `TimePickerCell` に `Is24Hour` bindable property (既定 true) を追加し、`KsTimePickerCellSnapshot` へ透過・`AffectsSnapshot` に参加させる (→ Requirement: TimePickerCell の Is24Hour 指定)
- [x] 4.2 両 OS の gateway で snapshot の `Is24Hour` を bridge DTO へ写す (→ Requirement: 時制フラグの輸送 Scenario「MAUI facade の値が gateway を透過する」)
- [x] 4.3 ConversionPathTests 拡張: snapshot 透過 (既定 true / 明示 false)・gateway 変換・表示済み Cell の `Is24Hour` 変更の再送出 (→ Requirement: TimePickerCell の Is24Hour 指定 全 Scenario)

## 5. サンプル (sample-parity 解消)

- [x] 5.1 samples/android: 12時間制デモ (就寝時刻) に `is24Hour = false` を明示し、format 依存コメントを更新 (→ Requirement: 12時間制デモの時制明示)
- [x] 5.2 samples/ios: 12時間制デモセルを常設 (文言は android とパリティ) (→ Requirement: 12時間制デモの常設 [samples-ios])
- [x] 5.3 samples/maui: 12時間制デモセルを常設 (同上) (→ Requirement: 12時間制デモの常設 [samples-maui])
- [x] 5.4 3面で 12時間制デモの選択面提示を視覚確認する (スクリーンショット) (→ samples 各 Scenario)
