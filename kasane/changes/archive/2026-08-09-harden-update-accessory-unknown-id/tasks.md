# Tasks: harden-update-accessory-unknown-id

## 1. iOS Store

- [x] 1.1 `SettingsRootStore.updateAccessory` の section 系 target に「state 更新が成立しなければ Diff を発行しない」ガードを追加し、実装箇所に `core/ADR-0020` コメントを付記する (→ Requirement: updateAccessory の未知 sectionID no-op [ios-store])
- [x] 1.2 Store テスト: 未知 sectionID の header / footer no-op (状態・Diff 両ストリーム無発行)・既知 sectionID の header / footer 反映・Root 系 target の header / footer 従来どおり発行 (→ Scenario: 未知 sectionID の section header 更新は no-op / section footer 更新は no-op / 既知 sectionID は header / footer とも従来どおり反映される / Root 系 target は header / footer とも従来どおり Diff を発行する)
- [x] 1.3 Host 接続テスト: 表示中の未知 sectionID 呼び出しで表示不変・assert 非到達 (→ Scenario: Host 表示中の未知 sectionID 呼び出しは表示に影響しない)

## 2. Android Store

- [x] 2.1 `SettingsRootStore.updateAccessory` の section 系 target に同ガードを追加し、実装箇所に `core/ADR-0020` コメントを付記する (→ Requirement: updateAccessory の未知 sectionId no-op [android-store])
- [x] 2.2 Store テスト: 未知 sectionId の header / footer no-op (状態・Diff 両ストリーム無 emit)・既知 sectionId の header / footer 反映・Root 系 target の header / footer 従来どおり emit — iOS のテストケースと対称構成にする (core/ADR-0018) (→ Scenario: 未知 sectionId の section header 更新は no-op / section footer 更新は no-op / 既知 sectionId は header / footer とも従来どおり反映される / Root 系 target は header / footer とも従来どおり Diff を emit する)
- [x] 2.3 Host 接続テスト (Robolectric): `strictMode = true` のまま未知 sectionId 呼び出しで例外なし・表示不変・後続の既知 cellId `replaceCell` が表示へ届く (→ Scenario: strictMode 既定のまま未知 sectionId 呼び出しでも Host は沈黙しない)

## 3. MAUI Bridge (実装変更なし)

- [x] 3.1 Bridge テスト: 未知 sectionID の `updateAccessory` が no-op であることを既存 KsBridge テストの流儀で両 OS に追加。Bridge 実装は素通しのまま変更しないことを確認する (→ Scenario: 未知 sectionID の updateAccessory は no-op [maui-bridge])
