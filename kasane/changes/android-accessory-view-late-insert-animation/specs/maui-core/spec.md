# Delta Spec: maui-core (android-accessory-view-late-insert-animation)

## ADDED Requirements

### Requirement: 配置済みの view accessory は Native Host の最初の表示に含まれる

SettingsView が表示される時点で Root / Section の Header・Footer に配置されている MAUI View は、Native Host が view 階層へ取り付けられた後の最初の表示に含まれなければならない (SHALL)。取り付け後に view accessory が後から追加される形で現れてはならない (SHALL NOT)。text を持たず View だけを配置した Header・Footer も同じである。

この保証は、初回の表示と、Handler の切断・再接続 (ページの再訪問・タブの切り替え) の後の表示の両方で成り立つ (SHALL)。

Section の view accessory は、Native Host を生成する時点で native の設定ツリーの現在状態に含まれている (SHALL)。Root の view accessory は、Native Host の生成後・view 階層への取り付けより前に適用されている (SHALL)。取り付けの通知 (`Loaded`) を待って view accessory を適用してはならない (SHALL NOT)。

表示中の SettingsView に対して実行時に View を配置・差し替え・解除した場合の反映は、既存の Requirement「View の差し替えと内容変化が表示に反映される」のとおりであり、本 Requirement の対象外とする。

#### Scenario: 初回の表示で Section の View だけの Footer が最初から表示される
- **GIVEN** `FooterText` を持たず `FooterView` に View を配置した Section を含む SettingsView
- **WHEN** Handler が接続され Native Host が生成される
- **THEN** Native Host の生成時点で、native の設定ツリーの現在状態はその Section の footer に view accessory を持つ

#### Scenario: 初回の表示で Section の HeaderView が最初から表示される
- **GIVEN** `HeaderView` に View を配置した Section を含む SettingsView
- **WHEN** Handler が接続され Native Host が生成される
- **THEN** Native Host の生成時点で、native の設定ツリーの現在状態はその Section の header に view accessory を持つ

#### Scenario: Root の view accessory は取り付けを待たずに適用される
- **GIVEN** `RootHeaderView` と `RootFooterView` に View を配置した SettingsView
- **WHEN** Handler が接続され Native Host が生成される (view 階層への取り付けの通知はまだ来ていない)
- **THEN** Root の header / footer の view accessory は native へ適用済みである

#### Scenario: 再接続でも view accessory は Native Host の生成までに届く
- **GIVEN** Root と Section の view accessory を表示した後に Handler が切断された SettingsView
- **WHEN** Handler が再接続され Native Host が生成される
- **THEN** Section の view accessory は Native Host の生成時点で native の設定ツリーの現在状態に含まれ、Root の view accessory は取り付けの通知より前に適用済みである

#### Scenario: 切断中に配置した View も再接続時の最初の表示に含まれる
- **GIVEN** Handler 切断中の SettingsView
- **WHEN** Section の `FooterView` に新しい View を設定し、その後 Handler が再接続される
- **THEN** Native Host の生成時点で、native の設定ツリーの現在状態はその Section の footer に新しい View の view accessory を持つ

#### Scenario: 取り付けの通知では view accessory を適用し直さない
- **GIVEN** view accessory を配置した SettingsView の Handler が接続され、Native Host が生成された状態
- **WHEN** view 階層への取り付けの通知 (`Loaded`) が届く
- **THEN** view accessory の native への配信は新たに発生しない
