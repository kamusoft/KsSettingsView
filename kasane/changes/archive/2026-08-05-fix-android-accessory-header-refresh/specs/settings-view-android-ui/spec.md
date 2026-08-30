# Delta: settings-view-android-ui (fix-android-accessory-header-refresh)

## ADDED Requirements

### Requirement: Section accessory の内容更新の表示反映

Android host は、section identity (sectionID) が同一のまま Section header / footer の accessory が**非 null から非 null へ**変わる更新 (`updateAccessory`、および同一 id の Section を置換する `replaceSection`) を、同一行の内容変更として表示へ反映する SHALL。

- accessory の view type が変わらない場合 (Text → Text / View → View)、反映は行の identity を維持した payload 付き変更通知で行われ、構造変更通知 (挿入・削除・移動) としては扱わない SHALL。View accessory 同士は、保持する View インスタンスが別のものへ差し替えられた場合を内容変更とみなす
- accessory の型が切り替わる場合 (Text ↔ View)、行の stable identity は維持したまま表示を新しい accessory の内容にする SHALL。この場合の Native 行 View の差し替えは許容される (同一 ViewHolder の保証は view type 不変時に限る)
- 構造・内容の更新において accessory 内容が変わらない場合、当該 Section header / footer 行へ変更通知を発行しない SHALL (Theme 更新など全行の再評価を契約とする更新はこの限りではない)

#### Scenario: updateAccessory による header text 変更が表示へ反映される
- **GIVEN** 既知の sectionID を持つ Section が Text accessory の header と共に表示されている
- **WHEN** `updateAccessory(SectionHeader, その sectionID, 別の text)` を適用する
- **THEN** 表示中の header は新しい text になり、当該行へ payload 付き変更通知が発行される

#### Scenario: updateAccessory による footer text 変更が表示へ反映される
- **GIVEN** 既知の sectionID を持つ Section が Text accessory の footer と共に表示されている
- **WHEN** `updateAccessory(SectionFooter, その sectionID, 別の text)` を適用する
- **THEN** 表示中の footer は新しい text になり、当該行へ payload 付き変更通知が発行される

#### Scenario: replaceSection による header text 変更が表示へ反映される
- **GIVEN** 既知の sectionID を持つ Section が Text accessory の header と共に表示されている
- **WHEN** 同一 id で header text だけが異なる Section へ `replaceSection` する
- **THEN** 表示中の header は新しい text になる

#### Scenario: accessory の型の切替も表示へ反映される
- **GIVEN** 既知の sectionID を持つ Section が Text accessory の header と共に表示されている
- **WHEN** 同一 sectionID の header を View accessory へ変える更新を適用する
- **THEN** 表示中の header は新しい accessory の内容になる (行 View の差し替えは許容される)

#### Scenario: View accessory の差し替えが表示へ反映される
- **GIVEN** 既知の sectionID を持つ Section が View accessory の header と共に表示されている
- **WHEN** 同一 sectionID の header を別の View インスタンスを保持する View accessory へ変える更新を適用する
- **THEN** 表示中の header は新しい View の内容になり、当該行へ payload 付き変更通知が発行される

#### Scenario: 内容が同一なら Section H/F へ変更通知を発行しない
- **GIVEN** Section header / footer を含む root が表示されている
- **WHEN** Section header / footer の内容が同一のまま構造だけが変わる更新 (別 Section への Cell 追加等) を適用する
- **THEN** 内容が同一の Section header / footer 行へは変更通知が発行されない

### Requirement: Section accessory の追加と削除は構造変更として反映する

Android host は、section identity が同一のまま Section header / footer の accessory が **null から非 null へ**変わる更新を行の挿入として、**非 null から null へ**変わる更新を行の削除として表示へ反映する SHALL (内容変更としては扱わない)。

#### Scenario: header の追加が行の挿入として反映される
- **GIVEN** header accessory を持たない Section が表示されている
- **WHEN** `updateAccessory(SectionHeader, その sectionID, text)` で header を追加する
- **THEN** 当該 Section の header 行が挿入され、表示される

#### Scenario: footer の解除が行の削除として反映される
- **GIVEN** Text accessory の footer を持つ Section が表示されている
- **WHEN** `updateAccessory(SectionFooter, その sectionID, null)` で footer を解除する
- **THEN** 当該 Section の footer 行が削除される

### Requirement: full 更新経路での同一 id の Cell 内容反映

Android host は、full 更新経路 (`replaceSection`・`SettingsRootDiff.Full`・root 全体の再設定) において、**更新前後の表示リスト双方に存在する同一 id の Cell** の内容が変わった場合、その内容を行 identity を維持した payload 付き変更通知で表示へ反映する SHALL。

- 新規に挿入される Cell および hidden から表示へ復帰する Cell は、構造変更 (挿入) として表示され、内容の変更通知を重ねて発行しない SHALL
- 削除される Cell および hidden になる Cell へは、内容の変更通知を発行しない SHALL
- 内容通知の対象となる Cell が存在しない場合 (空 root への更新・Section header / footer のみの root への更新を含む) でも、構造の反映は必ず実行される SHALL

#### Scenario: replaceSection で同一 id の Cell 内容変更が表示へ反映される
- **GIVEN** 既知の id を持つ Cell を含む Section が表示されている
- **WHEN** 同一 Section id・同一 Cell id のまま Cell の内容 (title 等) だけが異なる Section へ `replaceSection` する
- **THEN** 表示中の当該 Cell は新しい内容になり、当該行へ payload 付き変更通知が発行される

#### Scenario: Full diff で同一 id の Cell 内容変更が表示へ反映される
- **GIVEN** 既知の id を持つ Cell を含む root が表示されている
- **WHEN** 同一構造・同一 id のまま一部 Cell の内容だけが異なる root で `SettingsRootDiff.Full` を適用する
- **THEN** 表示中の当該 Cell は新しい内容になる

#### Scenario: root の再設定でも同一 id の Cell 内容変更が反映される
- **GIVEN** 既知の id を持つ Cell を含む root が表示されている
- **WHEN** 同一構造・同一 id のまま一部 Cell の内容だけが異なる root を再設定する
- **THEN** 表示中の当該 Cell は新しい内容になり、当該行へ payload 付き変更通知が発行される

#### Scenario: 空 root への更新で表示が空になる
- **GIVEN** Cell を含む root が表示されている
- **WHEN** Section を持たない空の root で full 更新を適用する
- **THEN** 表示は空になる (古い行が残らない)

#### Scenario: Section header / footer のみの root への更新が反映される
- **GIVEN** Cell を含む root が表示されている
- **WHEN** Cell を持たず Section header / footer だけを持つ root で full 更新を適用する
- **THEN** 表示は新しい root の内容になる (古い Cell 行が残らない)

#### Scenario: 新規に挿入される Cell へは内容通知を重ねない
- **GIVEN** root が表示されている
- **WHEN** 新しい id の Cell を追加した root で full 更新を適用する
- **THEN** 追加された Cell は挿入の構造通知で表示され、当該 id への内容変更通知は発行されない

#### Scenario: 削除された Cell へは内容通知を発行しない
- **GIVEN** 複数の Cell を含む Section が表示されている
- **WHEN** 一部の Cell を削除した Section へ `replaceSection` する
- **THEN** 削除は構造変更として反映され、削除された Cell の id への内容変更通知は発行されない

#### Scenario: 初回の root 反映では内容変更通知を発行しない
- **GIVEN** まだ root が設定されていない KsSettingsView がある
- **WHEN** 最初の root を設定する
- **THEN** 全行が表示され、内容の変更通知は発行されない (挿入の構造通知のみ)
