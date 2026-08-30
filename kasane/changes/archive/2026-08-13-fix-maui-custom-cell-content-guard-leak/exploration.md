# Exploration: fix-maui-custom-cell-content-guard-leak

## 経緯

- align-maui-accessory-placement-guard の実装中に発見された既存問題 (同 change の deviation.md 2件目に記録済み)。デルタスペック Non-Goals「Content 側の挙動変更なし」に従い当該 change では手を入れず、本 S 級変更として切り出した。
- 変更級: **S** (提案不要・直接実装+テスト+独立レビュー)。作業ドメイン: **maui**。

## 課題

`maui/KsSettingsView.Maui/CustomCell.cs` の `internal IKsCellContentGuard? ContentGuard` (add-maui-custom-cell 由来) が auto-property の強参照で `KsSettingsController` を保持する。外部 (ViewModel 等) が CustomCell を保持したまま SettingsView を破棄すると、CustomCell → ContentGuard (controller) → SettingsView の連鎖で SettingsView が回収されない実測リークがある。

## 合意済みスコープ

1. `CustomCell.ContentGuard` の backing store を `WeakReference<IKsCellContentGuard>` 化する。形は `Section.AccessoryGuard` (align-maui-accessory-placement-guard で採った実装、`maui/KsSettingsView.Maui/Section.cs`) と同一にする。model→controller 参照は弱参照がこのリポジトリの規律 (KsWeakPropertySubscription と同軸)。
2. 回帰テスト: `maui/KsSettingsView.Maui.Tests/LeakTests.cs` の流儀で「外部が CustomCell (Content 設定済み) を保持したまま SettingsView を落とすと SettingsView が回収される」テストを追加する。
3. 既存の guard 動作テスト (CustomCellContentTests の AFailedContentReplacement 系) が退行しないこと。

## Non-Goals

- guard の配線の形 (`?.Ensure…` / `= this` / `= null`) の変更 — 参照強度のみ変える。
- Section.AccessoryGuard 側・他プラットフォームへの変更。

## テスト実行

```
dotnet test maui/KsSettingsView.Maui.Tests/KsSettingsView.Maui.Tests.csproj
```
