# Delta Spec: settings-view-android-ui (perf-android-customcell-composition-reuse)

既存の `cell-types-custom`「content 駆動の描画と再利用」の契約 (再利用時に前の content 由来の表示・listener・購読を残さない) は変更しない。本デルタは Android Host における CustomCell のリサイクル挙動の契約を追加する。

## ADDED Requirements

### Requirement: CustomCell Composition のプール生存と破棄境界

CustomCell 行の ViewHolder が行単位のスクロールアウト (detach) やリサイクルプール滞在に入っても、Composition を破棄しない SHALL。Composition の破棄は、プールからの放逐、またはホスト (KsSettingsView / RecyclerView) 自体の解放に伴う pooling container の解放通知に従う SHALL。

行が itemViewCache に滞在する間 (onViewRecycled が通知される前) は、content を活性のまま維持する SHALL — 行は bind を経ずに再表示され得るため。この間、content の内部状態は保持され、effect・購読は継続する。

#### Scenario: 行のスクロールアウトでは破棄されない

- **GIVEN** CustomCell を含む長いリストが表示されている
- **WHEN** CustomCell の行が画面外へスクロールされ、ViewHolder がリサイクルプールに入る
- **THEN** その ComposeView の Composition は破棄されない

#### Scenario: itemViewCache 経由の再表示で content が継続する

- **GIVEN** 既定の itemViewCache 設定で、builder 内の remember 状態が初期値から変更済みの CustomCell 行
- **WHEN** 行が画面外へ出て itemViewCache に滞在し、bind を経ずに再表示される
- **THEN** content の状態は維持されており、DisposableEffect の dispose は実行されていない

#### Scenario: プールからの放逐で破棄される

- **GIVEN** リサイクルプール滞在中の CustomCell 用 ViewHolder
- **WHEN** プールがクリアされる
- **THEN** Composition は破棄される

#### Scenario: ホストの解放で破棄される

- **GIVEN** CustomCell を含むリストを表示中の KsSettingsView
- **WHEN** KsSettingsView が window から取り外され保持中の ViewHolder が解放される
- **THEN** Composition は破棄される (リーク防止の既存挙動を維持)

### Requirement: content ノードツリーの再利用

同一の builder (同一の composable 呼び出し構造。ラップ関数による再利用形態が該当する) を持つ CustomCell 間で ViewHolder が再 bind されたとき、reusable なノード — Compose が reusable node として生成するノード、および onReset を指定した AndroidView — は破棄されずに再利用される SHALL。異なる builder 間の再 bind、および reusable でないノード (onReset を指定しない AndroidView 等) の再利用は保証しない。再利用が成立しない再 bind でも、新しい builder の出力が正しく表示される SHALL。

#### Scenario: 同一ラップ関数 builder 間でノードが再利用される

- **GIVEN** 同一のラップ関数 builder を使う CustomCell A と B があり、builder の content には onReset を指定した AndroidView が含まれる
- **WHEN** A を表示していた ViewHolder が B の行として再 bind される
- **THEN** AndroidView の View インスタンスは同一のまま onReset で再構成され、factory は再実行されない

#### Scenario: 構造が異なる builder でも表示が壊れない

- **GIVEN** 互いに異なる builder を持つ CustomCell A (プローブ要素 a を表示) と B (プローブ要素 b を表示)
- **WHEN** A を表示していた ViewHolder が B の行として再 bind される
- **THEN** プローブ b が表示され、プローブ a は表示ツリーに存在しない

### Requirement: content 状態の行間隔離

Composition とノードツリーがリサイクルを跨いで生存する場合でも、content の内部状態は Cell の同一性の単位で隔離される SHALL。別の Cell の行として再 bind されたとき、前の Cell の content に由来する remember 状態は新しい content に現れず、初期化式が再実行される SHALL。前の content の DisposableEffect の後始末 (dispose) は、遅くとも新しい content の表示までに実行される SHALL。同一 Cell が生存中の Composition へ再 bind された場合に内部状態が維持されるかどうかは契約しない (維持されることがある)。

#### Scenario: 同一フレーム内の再 bind でも remember 状態が持ち越されない

- **GIVEN** builder 内に remember による内部状態を持つ CustomCell A の行が表示され、その状態が初期値から変更されている
- **WHEN** 間に recomposition を挟まず、同じ ViewHolder が別の CustomCell B の行として直接再 bind される
- **THEN** B の content は初期状態で表示され、A で変更した状態は現れない

#### Scenario: DisposableEffect の後始末が実行される

- **GIVEN** builder 内に DisposableEffect を持つ CustomCell A の行が表示されている
- **WHEN** 同じ ViewHolder が別の CustomCell B の行として再 bind され表示される
- **THEN** A の DisposableEffect の dispose が実行済みである

### Requirement: reset による状態破棄と参照切断

CustomCell 用 ViewHolder の reset は、ノードツリーを保持したまま content を非活性化し、前の Cell の content に由来する remember 状態と effect を破棄する SHALL。reset 後の ViewHolder は、前の builder の出力を表示せず、タップ listener を保持せず、content state 経由の builder への参照を保持しない SHALL。保持されたノードが持つ旧 content 由来の参照 (Modifier・パラメータ slot 等) は、次の再利用または Composition の破棄まで残り得る — これは契約違反ではない。プールからの放逐等で Composition が破棄された後は、builder とその参照対象はガベージコレクション可能である SHALL。

#### Scenario: reset 後に前の content と listener が残らない

- **GIVEN** bind 済みの CustomCell 行
- **WHEN** 行がリサイクルされ reset が実行される
- **THEN** 前の builder の出力は表示されず、タップ listener は解除されている

#### Scenario: Composition 破棄後に builder が解放可能になる

- **GIVEN** bind 済みの CustomCell 行と、その builder だけが保持する参照対象
- **WHEN** 行がリサイクルされてプールに入り、プールがクリアされ、Cell 側の参照も手放される
- **THEN** builder とその参照対象はガベージコレクション可能である
