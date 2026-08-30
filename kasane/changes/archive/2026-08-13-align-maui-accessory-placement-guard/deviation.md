# Deviation: align-maui-accessory-placement-guard

- guard の参照強度 (spec 沈黙領域): tasks 1.4 では「custom.ContentGuard の配線と対称」→ 実装では `Section.AccessoryGuard` の backing store のみ `WeakReference<IKsAccessoryViewGuard>`。理由: 強参照だと外部保持の Section → controller → SettingsView が繋がり既存 leak テスト 2 件 (LeakTests / InteractionLifetimeTests) が赤になる。model→controller 参照は弱参照がこのリポジトリの規律 (KsWeakPropertySubscription と同軸)。配線の形 (`?.Ensure…` / `= this` / `= null`) は content 側と同一 (2026-08-13)
- 発見した既存問題 (本変更スコープ外・未修正): `CustomCell.ContentGuard` (add-maui-custom-cell 由来) は強参照のままで、外部が CustomCell を保持したまま SettingsView を落とすと SettingsView が回収されない実測リークがある。デルタスペック Non-Goals「Content 側の挙動変更なし」に従い手を入れず、別 S 級変更として切り出す (2026-08-13)
