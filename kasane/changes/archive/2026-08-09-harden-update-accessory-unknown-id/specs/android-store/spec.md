# Delta Spec: android-store (harden-update-accessory-unknown-id)

## ADDED Requirements

### Requirement: updateAccessory の未知 sectionId no-op

Android の `SettingsRootStore.updateAccessory` は、section 系 target (`SectionHeader` / `SectionFooter`) に Store の現在状態に存在しない sectionId が指定された場合、状態を変更せず構造 Diff も emit しない no-op としなければならない (SHALL)。`moveCell` / Cell / Section 操作と同じ「状態更新が成立しなかった構造 Diff は発行しない」契約に従う (core/ADR-0020)。観察可能挙動は iOS の同契約と対称とする。

no-op の観測基準は「現在状態の値が不変」かつ「現在状態ストリーム・Diff ストリームの両方が無発行」である — 同値の再代入による状態ストリームへの通知も発生させてはならない。

Root 系 target (`RootHeader` / `RootFooter`) は `SettingsRoot` 値型に状態を持たないため sectionId 検証の対象外であり、従来どおり Diff を emit する。

#### Scenario: 未知 sectionId の section header 更新は no-op
- **GIVEN** Section を含む root を保持する Store と、その状態・Diff 両ストリームの購読者
- **WHEN** 存在しない sectionId の `SectionHeader` target で `updateAccessory` を呼ぶ
- **THEN** Store の現在状態は変化せず、状態ストリーム・Diff ストリームのいずれにも emit が発生しない

#### Scenario: 未知 sectionId の section footer 更新は no-op
- **GIVEN** Section を含む root を保持する Store と、その状態・Diff 両ストリームの購読者
- **WHEN** 存在しない sectionId の `SectionFooter` target で `updateAccessory` を呼ぶ
- **THEN** Store の現在状態は変化せず、状態ストリーム・Diff ストリームのいずれにも emit が発生しない

#### Scenario: 既知 sectionId は header / footer とも従来どおり反映される
- **GIVEN** Section を含む root を保持する Store と、その Diff 購読者
- **WHEN** 既知の sectionId の `SectionHeader` target と `SectionFooter` target のそれぞれで `updateAccessory` を呼ぶ
- **THEN** それぞれの accessory が Store の現在状態に反映され、それぞれに対応する Diff が emit される

#### Scenario: Root 系 target は header / footer とも従来どおり Diff を emit する
- **GIVEN** root を保持する Store と、その Diff 購読者
- **WHEN** `RootHeader` target と `RootFooter` target のそれぞれで `updateAccessory` を呼ぶ
- **THEN** それぞれに対応する Diff が emit される

#### Scenario: strictMode 既定のまま未知 sectionId 呼び出しでも Host は沈黙しない
- **GIVEN** `strictMode = true` (既定) の Host に接続され表示中の設定 list
- **WHEN** 存在しない sectionId の section 系 target で Store の `updateAccessory` を呼び、続けて既知 cellId の `replaceCell` で表示中 Cell の内容を更新する
- **THEN** 例外は発生せず、未知 sectionId の呼び出しで表示は変化せず、後続の `replaceCell` の更新内容が表示へ反映される (Diff 購読は生き続ける)
