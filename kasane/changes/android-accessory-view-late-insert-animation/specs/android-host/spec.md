# Delta Spec: android-host (android-accessory-view-late-insert-animation)

## MODIFIED Requirements

変更点: Root の header / footer を「所有者が attach 後に適用する責務」とする記述を、別 Requirement への参照に改める。実装変更を伴わない旨の注記は役目を終えたため取り除く。Scenario は現行のまま。

### Requirement: window attach 時の Store 現在状態からの復元

Store bind 済みの Android Host (`KsSettingsView`) は、window attach (onAttachedToWindow) の後、bind 中 Store の現在状態へ表示を収束させなければならない (SHALL)。復元の対象は Store が現在状態として保持するもの — 設定ツリーの構造・Cell 内容・Section の accessory・theme — である。Root の header / footer は Store の現在状態に含まれないため、この復元の対象外であり、別の Requirement「購読できていない間に渡された Root の header / footer の保持」が扱う。

attach 前・detach 中の Store 変更を通知として受信できたかにかかわらず、attach 後に**メインスレッドのキューが空になった時点**までに表示は Store 現在状態へ収束する (収束の観測境界)。

#### Scenario: attach 前の更新が attach 後に反映される
- **GIVEN** `bind(store)` 済みで view 階層に未 attach の Host
- **WHEN** Store へ構造操作・Cell 内容更新 (`replaceCells` バッチ含む)・theme 変更を適用してから view 階層へ attach する
- **THEN** attach 後、メインスレッドのキューが空になった時点で表示は Store 現在状態と一致する

#### Scenario: detach 中の更新が再 attach で反映される
- **GIVEN** 一度表示した後 view 階層から detach した Host
- **WHEN** detach 中に Store へ Cell 内容更新を適用し、再度 attach する
- **THEN** attach 後、メインスレッドのキューが空になった時点で表示は Store 現在状態と一致する

## ADDED Requirements

### Requirement: 購読できていない間に渡された Root の header / footer の保持

Store bind 済みの Android Host は、`bind(store)` から `unbind()` までの間、view 階層への attach 状態にかかわらず、Store の `updateAccessory` で Root の header / footer を対象に渡された値を失ってはならない (SHALL NOT)。attach 前および detach 中に渡された値は、次の attach 後、メインスレッドのキューが空になった時点の表示に反映される (SHALL)。同じ対象へ複数回渡された場合は最後の値が反映される (SHALL)。`nil` (解除) も値として扱う。

Root の header / footer は Store の現在状態に含めない (SHALL NOT)。保持するのは Host であり、`bind` より前に Store へ渡された値、および `unbind` 後に渡された値は対象外とする。

保持は、値が渡された時点で成立する (SHALL)。`bind(store)` から戻った直後にメインスレッドのキューを流さず渡された値、およびメインスレッドのキューを流さず多数回続けて渡されたときの最後の値も失われない。

attach 中に渡された値は従来どおり即時に表示へ反映され、1 回の `updateAccessory` につき Host への適用は 1 回だけ行われる (SHALL)。

Store の `updateAccessory` は従来どおりどのスレッドから呼んでもよく、メインスレッド以外から渡された値も同じく保持され、表示への反映はメインスレッドで行われる (SHALL)。ある Host での反映の失敗は `updateAccessory` の呼び出し元へ伝わらず、他の Host の受け取りと Store の通知の発行を妨げない (SHALL NOT)。

同じ Store に複数の Host が bind している場合は、すべての Host が値を受け取る (SHALL)。同じ Host を同じ Store へ `bind` し直しても、1 回の `updateAccessory` につき適用は 1 回のままである (SHALL)。ある Host の `unbind()` は、同じ Store に bind している他の Host の受け取りを妨げない (SHALL NOT)。

別の Store へ `bind` し直した後は、以前の Store へ渡された値は反映されない (SHALL NOT)。

Store に bind したまま `unbind()` されずに手放された Host は、Store が生きていても回収可能でなければならない (SHALL)。

Host のプロパティ (`rootHeader` / `rootFooter`) へ直接代入する従来の使い方、および attach 後に `updateAccessory` で適用する従来の使い方は、そのまま有効である (SHALL)。

#### Scenario: attach 前に渡した Root の header が attach 後に表示される
- **GIVEN** `bind(store)` 済みで view 階層に未 attach の Host
- **WHEN** Store の `updateAccessory` で Root の header に view accessory を渡してから view 階層へ attach する
- **THEN** attach 後、メインスレッドのキューが空になった時点で Root の header が表示されている

#### Scenario: attach 前に渡した Root の footer が attach 後に表示される
- **GIVEN** `bind(store)` 済みで view 階層に未 attach の Host
- **WHEN** Store の `updateAccessory` で Root の footer に text accessory を渡してから view 階層へ attach する
- **THEN** attach 後、メインスレッドのキューが空になった時点で Root の footer が表示されている

#### Scenario: detach 中に渡した値が再 attach で反映される
- **GIVEN** Root の header を表示した後 view 階層から detach した Host
- **WHEN** detach 中に Store の `updateAccessory` で Root の header を別の値へ更新し、再度 attach する
- **THEN** attach 後、メインスレッドのキューが空になった時点で Root の header は更新後の値を表示している

#### Scenario: 複数回渡した場合は最後の値が反映される
- **GIVEN** `bind(store)` 済みで view 階層に未 attach の Host
- **WHEN** Root の header に値 A、続けて値 B を渡し、Root の footer には値 C を渡した後に解除 (`nil`) を渡してから attach する
- **THEN** attach 後の Root の header は値 B を表示し、Root の footer は表示されない

#### Scenario: attach 中の更新は一度だけ適用される
- **GIVEN** view 階層に attach 済みで、Root の header に view accessory を表示している Host
- **WHEN** Store の `updateAccessory` で Root の header を別の view accessory へ 1 回更新する
- **THEN** Root の header の行に対する変更通知は 1 回だけ発行される

#### Scenario: bind 直後にキューを流さず渡した値が表示される
- **GIVEN** view 階層に未 attach の Host
- **WHEN** `bind(store)` を呼び、メインスレッドのキューを流さずに続けて Store の `updateAccessory` で Root の header を渡し、その後 attach する
- **THEN** attach 後、メインスレッドのキューが空になった時点で Root の header が表示されている

#### Scenario: キューを流さない多数回の連続更新でも最後の値が表示される
- **GIVEN** `bind(store)` 済みで view 階層に未 attach の Host
- **WHEN** メインスレッドのキューを流さずに Root の header を多数回続けて更新し、その後 attach する
- **THEN** attach 後の Root の header は最後に渡した値を表示している

#### Scenario: 別 Store へ bind し直した後は以前の Store の値が反映されない
- **GIVEN** Store A に `bind` した後、Store B へ `bind` し直した未 attach の Host
- **WHEN** Store A の `updateAccessory` で Root の header を渡してから attach する
- **THEN** Root の header は表示されない

#### Scenario: unbind されずに手放された Host は回収できる
- **GIVEN** 長命の Store に `bind` した Host
- **WHEN** `unbind()` を呼ばずに Host への参照をすべて手放し、その後 Store の `updateAccessory` で Root の header を渡す
- **THEN** Host は回収可能であり、Store に残っていた Host 向けの登録は取り除かれている

#### Scenario: unbind 後に渡した値は反映されない
- **GIVEN** `bind(store)` の後に `unbind()` した Host
- **WHEN** Store の `updateAccessory` で Root の header を渡してから view 階層へ attach する
- **THEN** Root の header は表示されない

#### Scenario: 同じ Store に bind した 2 つの Host の両方が更新される
- **GIVEN** 同じ Store に `bind` した、view 階層に未 attach の Host A と Host B
- **WHEN** Store の `updateAccessory` で Root の header を渡してから両方を attach する
- **THEN** Host A と Host B の両方で Root の header が表示されている

#### Scenario: 同じ Store へ bind し直しても適用は 1 回である
- **GIVEN** 同じ Store へ `bind` を 2 回呼んだ、attach 済みで Root の header を表示している Host
- **WHEN** Store の `updateAccessory` で Root の header を別の view accessory へ 1 回更新する
- **THEN** Root の header の行に対する変更通知は 1 回だけ発行される

#### Scenario: 一方の unbind は他方の受け取りを妨げない
- **GIVEN** 同じ Store に `bind` した Host A と Host B
- **WHEN** Host A を `unbind()` してから Store の `updateAccessory` で Root の header を渡し、Host B を attach する
- **THEN** Host B で Root の header が表示されている

#### Scenario: メインスレッド以外から渡した値が attach 後に表示される
- **GIVEN** `bind(store)` 済みで view 階層に未 attach の Host
- **WHEN** メインスレッド以外のスレッドから Store の `updateAccessory` で Root の header を渡し、その呼び出しが戻った後に view 階層へ attach する
- **THEN** attach 後、メインスレッドのキューが空になった時点で Root の header が表示されている

#### Scenario: attach 中にメインスレッド以外から渡した値はメインスレッドで反映される
- **GIVEN** view 階層に attach 済みの Host
- **WHEN** メインスレッド以外のスレッドから Store の `updateAccessory` で Root の header を渡す
- **THEN** メインスレッドのキューが空になった時点で Root の header が表示されており、表示への反映はメインスレッドで行われている
