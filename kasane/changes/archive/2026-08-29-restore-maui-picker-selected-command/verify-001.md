# 一致検証結果: restore-maui-picker-selected-command (001 回目)

**日付**: 2026-08-28
**判定**: VALID

## 対応表

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Requirement: PickerCell の選択完了 Command | `maui/KsSettingsView.Maui/PickerCell.cs:106`、`maui/KsSettingsView.Maui/PickerCell.cs:248`、`maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1867` | `maui/KsSettingsView.Maui.Tests/PickerSelectedCommandTests.cs:18` | ✅ 一致 |
| 単一選択の完了後に選択項目を通知 | `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1867`、`maui/KsSettingsView.Maui/PickerCell.cs:304` | `maui/KsSettingsView.Maui.Tests/PickerSelectedCommandTests.cs:30` | ✅ 一致 |
| 複数選択の完了後に選択項目列を通知 | `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1887`、`maui/KsSettingsView.Maui/PickerCell.cs:304` | `maui/KsSettingsView.Maui.Tests/PickerSelectedCommandTests.cs:59` | ✅ 一致 |
| 同じ選択の再確定も完了として通知 | `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1874`、`maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1894` | `maui/KsSettingsView.Maui.Tests/PickerSelectedCommandTests.cs:90` | ✅ 一致 |
| CanExecute が false でも完了を通知 | `maui/KsSettingsView.Maui/PickerCell.cs:304` | `maui/KsSettingsView.Maui.Tests/PickerSelectedCommandTests.cs:119` | ✅ 一致 |
| 公開選択値の直接設定では実行しない | `maui/KsSettingsView.Maui/PickerCell.cs:199`、`maui/KsSettingsView.Maui/PickerCell.cs:304` | `maui/KsSettingsView.Maui.Tests/PickerSelectedCommandTests.cs:138` | ✅ 一致 |
| 選択を確定しなければ実行しない | `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1867`、`maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1887` | `maui/KsSettingsView.Maui.Tests/PickerSelectedCommandTests.cs:138` | ✅ 一致 |
| 未知の Cell 通知を無視 | `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1869`、`maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:1889` | `maui/KsSettingsView.Maui.Tests/PickerSelectedCommandTests.cs:158` | ✅ 一致 |

## 追加検査

- tasks.md: 全 6 タスクが完了し、対応する実装・テストを確認した。虚偽チェックなし。
- 逆流検査: git 操作禁止の制約に従い、隣接する基準 checkout との read-only `diff` と更新時刻を確認した。proposal / spec は実装ファイルより前に作成され、実装都合による足場の逆流は検出されなかった。tasks.md の実装後更新は完了チェックのみ。
- 未記録乖離: deviation.md は存在せず、未記録の欠落・乖離もなし。
- 付随修正: なし。差分は `PickerCell.cs`、`KsSettingsController.cs`、新規テストに限定され、Native / Bridge の変更なし。
- UI 変更: なし。ui/brief.md の対象外。
- テスト: `dotnet test KsSettingsView.Maui.Tests/KsSettingsView.Maui.Tests.csproj --no-restore` は 513 tests / 0 failures / 0 skipped。追加対象は 8 tests / 0 failures。

## 判定

全 Requirement / Scenario が実装とテストに対応し、虚偽チェック・逆流・未記録乖離・テスト失敗はないため VALID。
