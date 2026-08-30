# Delta Spec: maui-bridge (perf-android-customcell-composition-reuse)

Android ホストの CustomCell が deactivate+reuse 方式 (settings-view-android-ui デルタ参照) になることに伴う、Bridge 埋め込みの保全契約。

## ADDED Requirements

### Requirement: deactivate+reuse 下での Bridge 埋め込み platform view の保全

Bridge 経由で CustomCell に埋め込まれた platform view は、ホスト側の deactivate (リサイクル時の reset) と再 bind を経ても破棄されない SHALL。同じ Cell が再表示されたとき、同一の platform view インスタンスが再親付けされて表示される SHALL。deactivate による取り外しは自行の階層に閉じ、表示中の他の行が保持する platform view に影響しない SHALL。

#### Scenario: リサイクルを挟んだ再表示で同一 platform view が再親付けされる

- **GIVEN** Bridge の CustomCell を含む長いリストが表示されている
- **WHEN** 行が画面外へ出て ViewHolder がリサイクルプールに入り (deactivate 経路を通過)、のちに同じ Cell が再表示される
- **THEN** 同一の platform view インスタンスが再親付けされて表示され、view は破棄されていない

#### Scenario: deactivate が他の行の埋め込みを奪わない

- **GIVEN** Bridge の CustomCell の行が複数表示されている
- **WHEN** ある行がリサイクルされ deactivate される
- **THEN** 表示中の他の行の platform view は取り外されない
