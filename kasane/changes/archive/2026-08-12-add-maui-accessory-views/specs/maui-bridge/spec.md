# Delta Spec: maui-bridge — accessory の view 輸送拡張

## ADDED Requirements

### Requirement: updateAccessoryView で view accessory を更新できる

Bridge は `updateAccessoryView(target, sectionID, view)` (iOS は `UIView?`、Android は `android.view.View?`) を公開する (SHALL)。非 null の view は当該 accessory (root または指定 Section の header / footer) の view accessory として Store の既存 accessory 更新経路に乗り、null は accessory のクリアを意味する。既存の `updateAccessory` (text) と同じ更新契約 — Section 系 target の未知 sectionID は no-op、Root 系 target は Host 単位のプロパティで Store 状態に含まれない — に従う (SHALL)。

#### Scenario: section header へ view を設定すると表示される

- **GIVEN** Bridge に Store 上有効な sectionID の Section があり Host が表示中
- **WHEN** `updateAccessoryView(sectionHeader, sectionID, view)` を呼ぶ
- **THEN** その view が当該 Section の header として表示される

#### Scenario: null で view accessory がクリアされる

- **GIVEN** view accessory が表示されている Section
- **WHEN** `updateAccessoryView(sectionHeader, sectionID, null)` を呼ぶ
- **THEN** 当該 header の accessory はクリアされる

#### Scenario: 未知の sectionID は no-op

- **GIVEN** Store 上に存在しない sectionID
- **WHEN** `updateAccessoryView(sectionHeader, 未知のID, view)` を呼ぶ
- **THEN** state 更新も更新通知も発生しない (text 版 `updateAccessory` と同一の契約)

### Requirement: KsBridgeSection は headerView / footerView を輸送する

`KsBridgeSection` は `headerView` / `footerView` (platform view 型、null 許容) を持ち、`setRoot` / `replaceSection` の構築経路で view accessory を text と対称に輸送する (SHALL)。text と view の両方が指定された場合は view が優先される (SHALL — facade の View 優先解決と同一)。

#### Scenario: setRoot で view accessory 付き Section が表示される

- **GIVEN** `headerView` に platform view を設定した `KsBridgeSection` を含む root
- **WHEN** `setRoot` を呼び Host を表示する
- **THEN** 当該 Section の header にその view が表示される

#### Scenario: replaceSection でも view accessory が輸送される

- **GIVEN** 表示中の Section
- **WHEN** `headerView` を設定した置換 DTO で `replaceSection` を呼ぶ
- **THEN** 置換後の Section の header にその view が表示される

### Requirement: 同一 view インスタンスの再バインドが安全である

Bridge が輸送した view は、native 側の accessory 再バインド (リサイクル等) で同一インスタンスが再び取り付けられても失敗しない (SHALL)。Bridge は view を native へ渡す際、取り付け前に既存の親から切り離される形で包む。

#### Scenario: リサイクルを挟んだ再表示が失敗しない

- **GIVEN** view accessory を持つ Section を含む長い list が表示されている
- **WHEN** スクロール等で当該 header が画面外へ出て再び画面内へ戻る (accessory の再バインドが発生する)
- **THEN** 同一の view が例外なく再表示される
