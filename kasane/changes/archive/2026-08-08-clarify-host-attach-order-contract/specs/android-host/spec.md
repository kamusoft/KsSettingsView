# Delta Spec: android-host (clarify-host-attach-order-contract)

## ADDED Requirements

### Requirement: window attach 時の Store 現在状態からの復元

Store bind 済みの Android Host (`KsSettingsView`) は、window attach (onAttachedToWindow) の後、bind 中 Store の現在状態へ表示を収束させなければならない (SHALL)。復元の対象は Store が現在状態として保持するもの — 設定ツリーの構造・Cell 内容・Section の accessory・theme — である。Root の header / footer は Store の現在状態に含まれないため対象外とし、その反映は所有者 (呼び出し側) が attach 後に適用する責務とする。

attach 前・detach 中の Store 変更を通知として受信できたかにかかわらず、attach 後に**メインスレッドのキューが空になった時点**までに表示は Store 現在状態へ収束する (収束の観測境界)。

本 Requirement は現行実装が既に満たす挙動の契約化であり、実装変更を伴わない (回帰テストで固定する)。

#### Scenario: attach 前の更新が attach 後に反映される
- **GIVEN** `bind(store)` 済みで view 階層に未 attach の Host
- **WHEN** Store へ構造操作・Cell 内容更新 (`replaceCells` バッチ含む)・theme 変更を適用してから view 階層へ attach する
- **THEN** attach 後、メインスレッドのキューが空になった時点で表示は Store 現在状態と一致する

#### Scenario: detach 中の更新が再 attach で反映される
- **GIVEN** 一度表示した後 view 階層から detach した Host
- **WHEN** detach 中に Store へ Cell 内容更新を適用し、再度 attach する
- **THEN** attach 後、メインスレッドのキューが空になった時点で表示は Store 現在状態と一致する
