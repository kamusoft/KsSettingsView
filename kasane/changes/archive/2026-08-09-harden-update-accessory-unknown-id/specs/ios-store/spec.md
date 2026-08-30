# Delta Spec: ios-store (harden-update-accessory-unknown-id)

## ADDED Requirements

### Requirement: updateAccessory の未知 sectionID no-op

iOS の `SettingsRootStore.updateAccessory` は、section 系 target (section header / section footer) に Store の現在状態に存在しない sectionID が指定された場合、状態を変更せず構造 Diff も発行しない no-op としなければならない (SHALL)。`moveCell` / Cell / Section 操作と同じ「状態更新が成立しなかった構造 Diff は発行しない」契約に従う (core/ADR-0020)。

no-op の観測基準は「現在状態の値が不変」かつ「現在状態ストリーム・Diff ストリームの両方が無発行」である — 同値の再代入による状態ストリームへの通知も発生させてはならない。

Root 系 target (root header / root footer) は `SettingsRoot` 値型に状態を持たないため sectionID 検証の対象外であり、従来どおり Diff を発行する。

#### Scenario: 未知 sectionID の section header 更新は no-op
- **GIVEN** Section を含む root を保持する Store と、その状態・Diff 両ストリームの購読者
- **WHEN** 存在しない sectionID の section header target で `updateAccessory` を呼ぶ
- **THEN** Store の現在状態は変化せず、状態ストリーム・Diff ストリームのいずれにも発行が発生しない

#### Scenario: 未知 sectionID の section footer 更新は no-op
- **GIVEN** Section を含む root を保持する Store と、その状態・Diff 両ストリームの購読者
- **WHEN** 存在しない sectionID の section footer target で `updateAccessory` を呼ぶ
- **THEN** Store の現在状態は変化せず、状態ストリーム・Diff ストリームのいずれにも発行が発生しない

#### Scenario: 既知 sectionID は header / footer とも従来どおり反映される
- **GIVEN** Section を含む root を保持する Store と、その Diff 購読者
- **WHEN** 既知の sectionID の section header target と section footer target のそれぞれで `updateAccessory` を呼ぶ
- **THEN** それぞれの accessory が Store の現在状態に反映され、それぞれに対応する Diff が発行される

#### Scenario: Root 系 target は header / footer とも従来どおり Diff を発行する
- **GIVEN** root を保持する Store と、その Diff 購読者
- **WHEN** root header target と root footer target のそれぞれで `updateAccessory` を呼ぶ
- **THEN** それぞれに対応する Diff が発行される

#### Scenario: Host 表示中の未知 sectionID 呼び出しは表示に影響しない
- **GIVEN** Host に接続され表示中の設定 list
- **WHEN** 存在しない sectionID の section 系 target で Store の `updateAccessory` を呼ぶ
- **THEN** エラーやクラッシュ (DEBUG ビルドの assertion を含む) は発生せず、表示は変化しない
