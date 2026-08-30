# Delta Spec: maui-bridge — CustomCell の輸送と native 埋め込み

## ADDED Requirements

### Requirement: KsBridgeCustomCell で platform view と content トークンを輸送する

両 OS の Bridge は `KsBridgeCustomCell` (`KsBridgeCell` 派生の per-type DTO) を受け付け、`view` (platform view) / `contentToken` (文字列) / `showArrowIndicator` / タップ購読有無を輸送する (SHALL)。Bridge は native の CustomCell を構築し、輸送された platform view が行の内容として full-bleed で描画される (SHALL)。`view` は null を許容し、null の場合は空内容の行として描画される (SHALL)。native CustomCell の content にはトークンを格納する — 等価性はトークンの値等価で決まる (SHALL)。

#### Scenario: 構造更新で custom cell が表示される

- **GIVEN** 生成済みの Bridge と Host
- **WHEN** `KsBridgeCustomCell` を含む Section を構造更新 (setRoot / replaceCells 等) で渡す
- **THEN** 輸送した platform view が当該行の内容として表示される

### Requirement: 埋め込み platform view はトークンの変更でのみ差し替わる

同一 `contentToken` の `KsBridgeCustomCell` を再発行 (他プロパティの変更を含む) しても、埋め込み済みの platform view は**同一インスタンスのまま維持され**、破棄・再生成されない (SHALL — native の再バインド自体は native CustomCell の等価性契約に従って発火してよく、その場合も builder は同一インスタンスを返す)。`contentToken` が異なる再発行では行の内容が新しい platform view に置き換わり、旧 view はその後に破棄される (SHALL)。

#### Scenario: 同一トークンの再発行では view インスタンスが維持される

- **GIVEN** custom cell が表示されている
- **WHEN** 同一 `contentToken` ・同一 view の DTO で当該 Cell を更新する (isEnabled 等の他プロパティ変更)
- **THEN** 埋め込み view は同一インスタンスのまま表示され続け、破棄・再生成は起きない

#### Scenario: トークン変更で view が置き換わる

- **GIVEN** custom cell が表示されている
- **WHEN** 新しい `contentToken` と別の view を持つ DTO で当該 Cell を更新する
- **THEN** 行の内容が新しい view に置き換わる

### Requirement: platform view は返す前に既存の親から切り離される

Bridge が native CustomCell の builder に埋め込む platform view は、描画側へ返す前に既存の親 view から切り離される (SHALL — 行のリサイクルや再バインドで同じ view が別の描画先に取り付けられても失敗しない)。行のリサイクル後の再 bind でも view は正しく再取り付けされ、前の行に表示が残らない (SHALL)。

#### Scenario: スクロールによるリサイクルで表示が壊れない

- **GIVEN** custom cell を含む多数の行があるリストが表示されている
- **WHEN** custom cell が画面外へ出るまでスクロールし、再び画面内へ戻す
- **THEN** custom cell の行に輸送した view が表示され、他の行に content の残骸が現れない

### Requirement: 行タップは単一 delegate / listener へ通知される

タップ購読ありの custom cell の行タップは、既存の単一 interaction delegate / listener に追加された custom cell 用メソッドで Cell ID と共に通知される (SHALL)。タップ購読なしで構築された custom cell は行タップ動作を持たず、content 内部の操作を妨げない (SHALL — native の `onTap` を nil / null で構築する)。書き戻しは伴わない (SHALL)。

#### Scenario: タップ通知が Cell ID 付きで届く

- **GIVEN** タップ購読ありの `KsBridgeCustomCell` が表示されている
- **WHEN** その行をタップする
- **THEN** delegate / listener の custom cell 用メソッドが当該 Cell ID を引数に呼ばれる
