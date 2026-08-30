# セカンドオピニオン: align-timepicker-hour-cycle-across-platforms (code-001)

**相方**: codex / **label**: so-code-align-timepicker-hour-cycle-across-platforms / **日付**: 2026-08-28 / **対象**: HEAD (1d999dc) からの未コミット差分すべて (android / ios / maui / samples の is24Hour 実装一式)

---

## 総合判定: CHANGES_REQUESTED

Critical 0件、Major 1件、Minor 1件です。実装本体に明白な動作不良は見つかりませんでしたが、更新経路に対する必須テストが不足しています。

### Major

1. Store／DSL 更新経路の対称テストがない

該当箇所:

- `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/TimeSelectionSheetTest.kt:221`
- `android/ks-settingsview-compose/src/test/kotlin/jp/kamusoft/kssettingsview/compose/InputCellDslTest.kt:223`
- `ios/Tests/KsSettingsViewUITests/InputCellsTests.swift:841`

問題点:

`is24Hour` は生成後に変更可能な動的反映プロパティですが、追加された更新テストは Android では `ViewHolder.bind()`、iOS では `TimePickerCellView.render()` を直接呼んでいます。Android Compose のテストも DSL から初期 Cell を生成するだけです。

このため、実際の次の経路を通していません。

- Store の公開更新操作 → Host → Native View
- Compose／SwiftUI DSL 再評価 → `DSLDiffCalculator` → Store → Host

`kasane/concepts/core/architecture/declarative-ui-bridge.md:48,78` は、動的反映プロパティについて Store と DSL の両方の対称テストを必須としています。現在のテストでは、等価判定や renderer が正しくても、DSL 差分生成または Store 通知で `is24Hour` が取りこぼされる無音の失敗を検出できません。

推奨修正:

Android・iOSのそれぞれで、同一 ID の `TimePickerCell` を次の両経路から更新し、最終的な選択面が12時間制へ変わることを確認してください。

- Store の公開操作による更新
- Compose／SwiftUI DSL の再評価による更新

少なくとも `DSLDiffCalculator` が更新を生成し、Host の既存 Cell が再描画されるところまで通す必要があります。

### Minor

1. MAUIの「gateway変換」タスクが実際にはテストされていない

該当箇所:

- `kasane/changes/align-timepicker-hour-cycle-across-platforms/tasks.md:27`
- `maui/KsSettingsView.Maui.Tests/ConversionPathTests.cs:746`
- `maui/KsSettingsView.Maui/Platforms/Android/KsBridgeGateway.cs:416`
- `maui/KsSettingsView.Maui/Platforms/iOS/KsBridgeGateway.cs:412`

問題点:

タスク4.3は「gateway変換」を完了済みとしていますが、`TimePickerCellHourCycleChangeIsDeliveredToGateway` が検証しているのは、偽の gateway に渡された `KsTimePickerCellSnapshot.Is24Hour` までです。

Android の `Set24Hour(...)` および iOS の `dto.Is24Hour = ...` に値が正しく写ることは検証されていません。コンパイル成功は setter／property の存在を保証しますが、`false` が正しく輸送されることまでは保証しません。

推奨修正:

プラットフォーム固有 DTO 変換をテスト可能な mapper に分離するか、各プラットフォーム TFM のテストで、生成された bridge DTO の `is24Hour` が既定 `true`／明示 `false` の双方を保持することを確認してください。それまではタスク4.3の「gateway変換」を完了扱いにしないのが適切です。

なお、指定されたビルド・テスト結果は成功済みの前提として扱い、こちらでは実行していません。ファイルへの書き込みも行っていません。

---

## 突き合わせ結果 (ホスト review-001.md との照合、2026-08-28)

| 相方の指摘 | ホスト側 | 採否 | 根拠 |
|---|---|---|---|
| Major: Store／DSL 更新経路の対称テストがない | 指摘なし (見逃し) | **採用** (Major) | `kasane/concepts/core/architecture/declarative-ui-bridge.md` の対称テスト契約 (core/ADR-0018) に実在を確認。追加テストは Android が ViewHolder 直 bind・Compose DSL は初期生成のみ・iOS が render 直呼びで、Store 公開操作経由 / DSL 再評価経由のいずれの反映テストも欠けている |
| Minor: MAUI「gateway 変換」が実際にはテストされていない | review-001.md 🟡-1 / verify-001.md ❌-1 と同一指摘 | **確定** (Minor / 優先度 高) | 双方一致。実装の正しさはホスト側が生成 binding の JNI シグネチャまで確認済み。不足は Android ホストでの実行証跡 — ホスト推奨の証跡追加で対処 |

- 未解決 (両者矛盾) の指摘: なし
- 相方が見ておらずホストのみの指摘 (capture-environment 記録齟齬・MAUI サンプルのイベント表示パリティ・Suggestion 2 件) はホスト側レビューの指摘として通常処理
