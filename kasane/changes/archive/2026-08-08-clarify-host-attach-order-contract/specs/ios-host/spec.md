# Delta Spec: ios-host (clarify-host-attach-order-contract)

## ADDED Requirements

### Requirement: view load 時の Store 現在状態からの復元

Store 接続済みの iOS Host (`KsSettingsViewController`) は、view load (viewDidLoad) 完了時点で、接続中 Store の現在状態から表示を構築しなければならない (SHALL)。復元の対象は Store が現在状態として保持するもの — 設定ツリーの構造・Cell 内容・Section の accessory・theme — であり、Host 生成から view load までの間に Store へ適用された変更は種類によらず view load 時の表示に反映される。

Root の header / footer は Store の現在状態に含まれないため復元の対象外とする。その反映は所有者 (呼び出し側) が view load 後に適用する責務とする。

Store 接続中の theme は Store を正とする — view load 時の復元は Store の theme を適用し、view load 前に直接 `applyTheme` で適用された Theme は保持されない (Store 接続中の直接適用の併用は非保証)。

view load 前に届いた個々の Diff のイベントとしての適用は保証しない — 保証するのは view load 完了時点での Store 現在状態への収束のみである。view load は `loadViewIfNeeded()` 等でも発生し、window への attach とは独立のイベントである。

#### Scenario: view load 前の構造操作が load 時に反映される
- **GIVEN** Store 接続済みで view 未 load の Host handle
- **WHEN** Store へ `replaceSection` / `insertCell` / `removeCell` を適用してから `loadViewIfNeeded()` で view load する
- **THEN** viewDidLoad 完了時点の表示は操作適用後の Store 現在状態と一致する

#### Scenario: view load 前の Cell 内容更新が load 時に反映される
- **GIVEN** Store 接続済みで view 未 load の Host handle
- **WHEN** Store へ `replaceCell` (単発) と `replaceCells` (バッチ) をそれぞれ適用してから `loadViewIfNeeded()` で view load する
- **THEN** viewDidLoad 完了時点の表示は更新後の Cell 内容と一致する (両経路で検証する)

#### Scenario: view load 前の Section accessory / theme 変更が load 時に反映される
- **GIVEN** Store 接続済みで view 未 load の Host handle
- **WHEN** Store へ Section の accessory 更新と theme 変更を適用してから `loadViewIfNeeded()` で view load する
- **THEN** viewDidLoad 完了時点の表示は更新後の Section accessory と theme を反映する

#### Scenario: Store 接続中の直接 applyTheme は view load 時に Store theme で上書きされる
- **GIVEN** Store 接続済みで view 未 load の Host handle
- **WHEN** 公開 API `applyTheme` で Store と異なる Theme を直接適用してから view load する
- **THEN** viewDidLoad 完了時点の表示は Store の theme を反映する

#### Scenario: Root accessory は復元対象外で、所有者の再適用により反映される
- **GIVEN** Store 接続済みで view 未 load の Host handle
- **WHEN** view load 前に root header の `updateAccessory` を適用し、view load 後に所有者が同じ `updateAccessory` を再発行する
- **THEN** view load 直後の root header 表示は保証されず、再発行後に root header が表示される

#### Scenario: Store 非接続 init は従来どおり init 時の root で表示する
- **GIVEN** Store を接続せず root を直接渡して生成した Host
- **WHEN** view load する
- **THEN** init 時に渡した root がそのまま表示される (現行挙動の維持)
