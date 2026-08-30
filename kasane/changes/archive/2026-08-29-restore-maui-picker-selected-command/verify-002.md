# 一致検証結果: restore-maui-picker-selected-command (002 回目)

**日付**: 2026-08-29
**判定**: VALID

## 対応表

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Requirement: PickerCell の選択完了 Command | `maui/KsSettingsView.Maui/PickerCell.cs:106`、`maui/KsSettingsView.Maui/PickerCell.cs:248`、`maui/KsSettingsView.Maui/PickerCell.cs:304`、`maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1867`、`maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1887` | `maui/KsSettingsView.Maui.Tests/PickerSelectedCommandTests.cs:18` ほか同クラス 8 tests | ✅ 一致 |
| 単一選択の完了後に選択項目を通知 | `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1867` で値を書き戻した後に通知し、`maui/KsSettingsView.Maui/PickerCell.cs:304` で `SelectedItem` を渡す | `maui/KsSettingsView.Maui.Tests/PickerSelectedCommandTests.cs:30` | ✅ 一致 |
| 複数選択の完了後に選択項目列を通知 | `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1887` で正規化した値を書き戻した後に通知し、`maui/KsSettingsView.Maui/PickerCell.cs:304` で `SelectedItems` を渡す | `maui/KsSettingsView.Maui.Tests/PickerSelectedCommandTests.cs:59` | ✅ 一致 |
| 同じ選択の再確定も完了として通知 | `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1874`、`maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1894`。同値時は書き戻しを省きつつ、その後の通知は省かない | `maui/KsSettingsView.Maui.Tests/PickerSelectedCommandTests.cs:90` (単一・複数の 2 cases) | ✅ 一致 |
| CanExecute が false でも完了を通知 | `maui/KsSettingsView.Maui/PickerCell.cs:304` は `CanExecute` を呼ばず `Execute` を直接呼ぶ | `maui/KsSettingsView.Maui.Tests/PickerSelectedCommandTests.cs:119` | ✅ 一致 |
| 公開選択値の直接設定では実行しない | `maui/KsSettingsView.Maui/PickerCell.cs:199`、`:206`、`:223`、`:242` の setter は選択同期のみで、完了通知は controller の native 通知口からだけ呼ばれる | `maui/KsSettingsView.Maui.Tests/PickerSelectedCommandTests.cs:138` (4 公開選択値を直接設定) | ✅ 一致 |
| 選択を確定しなければ実行しない | facade の実行入口は `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1867` / `:1887` の確定 callback だけ。Android は `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/PickerSelectionSheet.kt:230` の cancel が callback を呼ばず、単一確定は `:490`、複数確定は `:231` に限定される。iOS は `ios/Sources/KsSettingsViewUI/PickerListViewController.swift:301` の cancel が dismiss のみで、単一確定は `:266`、複数確定は `:305` に限定される | facade: `maui/KsSettingsView.Maui.Tests/PickerSelectedCommandTests.cs:138`。Android 実選択面: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/PickerSelectionSheetTest.kt:142` (取消)、`:166` (下方向スワイプ)、`:197` (外側タップ・Back・dismiss)。iOS 実選択面: `ios/Tests/KsSettingsViewUITests/PickerSelectionScreenTests.swift:477` (単一 cancel)、`:488` (複数 cancel)。いずれも native 選択 callback 非発火を確認 | ✅ 一致 |
| 未知の Cell 通知を無視 | `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1869`、`:1889` で対象を解決できなければ値更新・完了通知の前に return する | `maui/KsSettingsView.Maui.Tests/PickerSelectedCommandTests.cs:158` (単一・複数の未知 ID) | ✅ 一致 |

## キャンセル経路の境界確認

Native 選択面から facade までの境界は次のとおりであり、cancel / dismiss は選択 callback 自体を生成しない。

- Android: 選択面の callback は `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/PickerCellViewHolder.kt:59` で Cell の選択 callback へ接続される。実選択面テスト 68 件のうち、取消・外側タップ・Back・スワイプ相当 dismiss の各テストが callback 非発火を直接検証している。
- iOS: 選択面の callback は `ios/Sources/KsSettingsViewUI/PickerCellView.swift:74` で Cell の選択 callback へ接続される。実選択面テスト 29 件のうち、単一・複数それぞれの cancel テストが callback 非発火を直接検証している。
- facade: Android / iOS gateway は native callback を `maui/KsSettingsView.Maui/Platforms/Android/KsBridgeGateway.cs:611` / `maui/KsSettingsView.Maui/Platforms/iOS/KsBridgeGateway.cs:613` から interaction sink へ中継するだけである。Native 側で callback が発生しない cancel / dismiss は controller の通知口へ到達せず、`SelectedCommand` も実行されない。

## 追加検査

- tasks.md: 全 6 タスクが完了しており、対応表の実装・テストと一致する。未実装を完了扱いした虚偽チェックなし。
- 逆流検査: git 操作禁止の制約に従い、隣接する基準 checkout との read-only `diff` と更新時刻で確認した。proposal / spec は実装ファイルより前に作成され、実装差分は `PickerCell.cs`、`KsSettingsController.cs`、新規 `PickerSelectedCommandTests.cs` のみ。tasks.md の実装後更新は完了チェックであり、proposal / spec の逆流は検出されなかった。
- 未記録乖離: deviation.md は存在せず、全 Requirement / Scenario に未記録の欠落・乖離はない。
- 付随修正: なし。基準 checkout との read-only `diff` で Android / iOS Native、Android / iOS Binding、MAUI の platform gateway に差分がないことを確認した。
- UI 変更: なし。ui/brief.md の対象外。
- テスト:
  - MAUI facade 全件: `dotnet test KsSettingsView.Maui.Tests/KsSettingsView.Maui.Tests.csproj --no-restore` — **513 tests / 0 failures / 0 skipped**。
  - 追加対象の名指し実行: 同コマンド + `--filter FullyQualifiedName~PickerSelectedCommandTests` — **8 tests / 0 failures / 0 skipped**。
  - Android 実選択面: `:ks-settingsview-ui:testDebugUnitTest --tests jp.kamusoft.kssettingsview.ui.PickerSelectionSheetTest` — **68 tests / 0 failures / 0 errors / 0 skipped**。
  - iOS 実選択面: iOS Simulator の `xcodebuild test` + `-only-testing:KsSettingsViewUITests/PickerSelectionScreenTests` — **29 tests / 0 failures / TEST SUCCEEDED**。

## 判定

全 Requirement / 7 Scenario が実装とテストに対応する。特に「選択を確定しなければ実行しない」は facade の通知不在だけでなく、Android / iOS の実選択面で cancel / dismiss 時に native 選択 callback が発生しないことまで確認した。虚偽チェック・逆流・未記録乖離・テスト失敗はないため **VALID**。
