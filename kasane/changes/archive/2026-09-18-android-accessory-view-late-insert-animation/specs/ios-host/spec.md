# Delta Spec: ios-host (android-accessory-view-late-insert-animation)

## MODIFIED Requirements

変更点: Root の header / footer の扱いを別 Requirement へ移し、Scenario「Root accessory は復元対象外で、所有者の再適用により反映される」を取り除く。他の Scenario は現行のまま。

### Requirement: view load 時の Store 現在状態からの復元

Store 接続済みの iOS Host (`KsSettingsViewController`) は、view load (viewDidLoad) 完了時点で、接続中 Store の現在状態から表示を構築しなければならない (SHALL)。復元の対象は Store が現在状態として保持するもの — 設定ツリーの構造・Cell 内容・Section の accessory・theme — であり、Host 生成から view load までの間に Store へ適用された変更は種類によらず view load 時の表示に反映される。

Root の header / footer は Store の現在状態に含まれないため、この復元の対象外であり、別の Requirement「view load 前に渡された Root の header / footer の保持」が扱う。

Store 接続中の theme は Store を正とする — view load 時の復元は Store の theme を適用し、view load 前に直接 `applyTheme` で適用された Theme は保持されない (Store 接続中の直接適用の併用は非保証)。

Root の header / footer を除き、view load 前に届いた個々の Diff のイベントとしての適用は保証しない — 保証するのは view load 完了時点での Store 現在状態への収束のみである。view load は `loadViewIfNeeded()` 等でも発生し、window への attach とは独立のイベントである。

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

#### Scenario: Store 非接続 init は従来どおり init 時の root で表示する
- **GIVEN** Store を接続せず root を直接渡して生成した Host
- **WHEN** view load する
- **THEN** init 時に渡した root がそのまま表示される (現行挙動の維持)

## ADDED Requirements

### Requirement: view load 前に渡された Root の header / footer の保持

Store 接続済みの iOS Host は、Host 生成から view load までの間に Store の `updateAccessory` で Root の header / footer を対象に渡された値を失ってはならない (SHALL NOT)。渡された値は viewDidLoad 完了時点の表示に反映される (SHALL)。同じ対象へ複数回渡された場合は最後の値が反映される (SHALL)。`nil` (解除) も値として扱う。

値を受け取ること自体が view load を引き起こしてはならない (SHALL NOT)。

Root の header / footer は Store の現在状態に含めない (SHALL NOT)。保持するのは Host であり、Host 生成より前に Store へ渡された値は対象外とする。

Host のプロパティ (`rootHeader` / `rootFooter`) へ直接代入する従来の使い方、および view load 後に `updateAccessory` で適用する従来の使い方は、そのまま有効である (SHALL)。

#### Scenario: view load 前に渡した Root の header が load 時に表示される
- **GIVEN** Store 接続済みで view 未 load の Host handle
- **WHEN** Store の `updateAccessory` で Root の header に view accessory を渡してから `loadViewIfNeeded()` で view load する
- **THEN** viewDidLoad 完了時点で Root の header が表示されている (所有者による再発行を要しない)

#### Scenario: view load 前に渡した Root の footer が load 時に表示される
- **GIVEN** Store 接続済みで view 未 load の Host handle
- **WHEN** Store の `updateAccessory` で Root の footer に text accessory を渡してから view load する
- **THEN** viewDidLoad 完了時点で Root の footer が表示されている

#### Scenario: 値を渡しても view load は起きない
- **GIVEN** Store 接続済みで view 未 load の Host handle
- **WHEN** Store の `updateAccessory` で Root の header を渡す
- **THEN** Host は view 未 load のままである

#### Scenario: 複数回渡した場合は最後の値が反映される
- **GIVEN** Store 接続済みで view 未 load の Host handle
- **WHEN** Root の header に値 A、続けて値 B を渡してから view load する
- **THEN** viewDidLoad 完了時点の Root の header は値 B を表示している

#### Scenario: view load 前の解除が load 時に反映される
- **GIVEN** Store 接続済みで view 未 load の Host handle
- **WHEN** Root の footer に値を渡し、続けて解除 (`nil`) を渡してから view load する
- **THEN** viewDidLoad 完了時点で Root の footer は表示されていない
