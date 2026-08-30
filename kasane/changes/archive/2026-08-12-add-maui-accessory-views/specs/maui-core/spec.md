# Delta Spec: maui-core — Root / Section Header・Footer の任意 View 対応

## ADDED Requirements

### Requirement: Root / Section の Header・Footer に任意 View を設定できる

`SettingsView.RootHeaderView` / `RootFooterView` と `Section.HeaderView` / `FooterView` (いずれも `View?`、既定 null) を公開する (SHALL)。非 null の View を設定すると、その View が対応する header / footer 領域に表示される。null に戻すと view accessory はクリアされる (対応する text プロパティが設定されていれば text 表示へ、無ければ accessory なしへ)。

本 spec の全 Requirement (設定・クリア・View 優先・差し替え・内容変化・復元) は **Root / Section × Header / Footer の4対象すべて**に適用される (SHALL)。以下の Scenario は代表対象で記述するが、契約は4対象で同一である (Root は Host 単位・Section は Store 状態と、内部の輸送経路が異なるため、テストは4対象をマトリクスで張る)。

#### Scenario: Section.HeaderView の設定で View が表示される

- **GIVEN** Handler 接続済みの SettingsView 配下の Section
- **WHEN** `Section.HeaderView` に View を設定する
- **THEN** その View が当該 Section の header として表示される

#### Scenario: RootHeaderView の設定で View が表示される

- **GIVEN** Handler 接続済みの SettingsView
- **WHEN** `RootHeaderView` に View を設定する
- **THEN** その View が list 全体の root header として表示される

#### Scenario: null 設定でクリアされる

- **GIVEN** `Section.HeaderView` に View が設定され表示されている (対応する `HeaderText` は未設定)
- **WHEN** `Section.HeaderView` を null にする
- **THEN** header 領域から View が消え、accessory なしの表示になる

### Requirement: text と view の併存は View 優先で解決される

同じ対象 (Root header 等) に text プロパティと View プロパティの両方が設定されている場合、View が優先して表示される (SHALL)。View 非 null の間、text の値は保持されるが表示に使われない。View を null に戻すと保持されていた text へフォールバックして表示される。

#### Scenario: 両方設定時は View が表示される

- **GIVEN** `Section.HeaderText` に文字列、`Section.HeaderView` に View の両方が設定されている
- **WHEN** その Section が表示される
- **THEN** header には View が表示され、text は表示されない

#### Scenario: View を null に戻すと text へフォールバックする

- **GIVEN** text と View の両方が設定され View が表示されている
- **WHEN** View プロパティを null にする
- **THEN** header の表示は保持されていた text に切り替わる

#### Scenario: View 表示中の text 変更は保持され解除後に反映される

- **GIVEN** View が表示されている (text も設定済み)
- **WHEN** text プロパティを別の文字列に変更し、その後 View を null にする
- **THEN** View 解除後の header には変更後の text が表示される

### Requirement: accessory View は所有者の BindingContext を継承する

accessory View は論理ツリーに接続され、**所有者**の `BindingContext` を継承する (SHALL) — `RootHeaderView` / `RootFooterView` は SettingsView から、`Section.HeaderView` / `FooterView` は所有 Section から継承する (既存の facade 意味論 SettingsView → Section → Cell と同じ階層)。所有者の `BindingContext` が変わると accessory View にも伝播する (SHALL)。View 自身に明示的な `BindingContext` が設定されている場合は継承で上書きしない (SHALL — MAUI 標準の継承規則)。

#### Scenario: Root accessory は SettingsView の BindingContext を継承する

- **GIVEN** `BindingContext` に ViewModel が設定された SettingsView
- **WHEN** バインディングを持つ View を `RootHeaderView` に設定する
- **THEN** View のバインディングは SettingsView の ViewModel を参照して解決される

#### Scenario: Section accessory は所有 Section の BindingContext を継承する

- **GIVEN** `ItemsSource` から生成され item を `BindingContext` として持つ Section
- **WHEN** バインディングを持つ View がその Section の header に設定される (ItemTemplate 経由の Section 生成物)
- **THEN** View のバインディングは SettingsView 全体の ViewModel ではなく、当該 Section の item を参照して解決される

#### Scenario: BindingContext の変更が accessory View へ伝播する

- **GIVEN** accessory View が表示されている SettingsView (View に明示的な BindingContext はない)
- **WHEN** 所有者 (SettingsView または Section) の `BindingContext` を別の値に変更する
- **THEN** accessory View のバインディングが新しい値で再解決される

#### Scenario: View の明示的な BindingContext は継承で上書きされない

- **GIVEN** 明示的に `BindingContext` を設定した View
- **WHEN** その View を accessory に設定する
- **THEN** View のバインディングは明示設定された値のまま解決される

### Requirement: View の差し替えと内容変化が表示に反映される

View プロパティに別のインスタンスを設定すると表示が新しい View に置き換わる (SHALL)。同一インスタンスの内部内容の変化 (バインド値の更新等) は、プロパティの再設定なしに表示へ反映される (SHALL)。内容変化でサイズが変わる場合、自動高さの accessory 領域は新しいサイズに追従する (SHALL)。

#### Scenario: 新しいインスタンスへの差し替え

- **GIVEN** accessory View A が表示されている
- **WHEN** 同じプロパティに別の View B を設定する
- **THEN** 表示が B に置き換わる

#### Scenario: 同一インスタンスの内容変化が反映される

- **GIVEN** バインディングを持つ accessory View が表示されている
- **WHEN** バインド元の値を変更する (プロパティの再設定はしない)
- **THEN** accessory View の表示内容が更新される

#### Scenario: サイズが変わる内容変化に領域高さが追従する

- **GIVEN** 自動高さ (HeaderHeight 未指定) の accessory View が表示されている
- **WHEN** 内容変化で View の必要サイズが変わる
- **THEN** accessory 領域の高さが新しいサイズに追従する

### Requirement: Handler 切断・再接続をまたいで view accessory は保持される

ページ離脱 (Handler 切断) 後の再訪問 (再接続) で、Root / Section の view accessory は再び表示される (SHALL)。切断中に行われた View プロパティの変更は、再接続後の表示に反映される (SHALL)。

#### Scenario: 再訪問で view accessory が復元される

- **GIVEN** Root と Section の view accessory を表示した状態でページを離脱する
- **WHEN** 同じページへ再訪問する
- **THEN** Root / Section の view accessory がいずれも再表示される

#### Scenario: 切断中の変更が再接続後に反映される

- **GIVEN** ページ離脱中 (Handler 切断中) の SettingsView
- **WHEN** `RootHeaderView` に新しい View を設定し、その後ページへ再訪問する
- **THEN** 再接続後の表示には新しい View が表示される

### Requirement: 同一 View インスタンスの多重配置は例外になる

同一 SettingsView (同一 controller) 配下で、同じ View インスタンスを複数の accessory へ同時に設定することはできず、検出時に `InvalidOperationException` を送出する (SHALL)。検出範囲・例外のタイミング・例外後のプロパティ状態は、既存の Section / CellBase 多重配置検出と同一の契約に従う (SHALL)。別 SettingsView や通常 Layout 配下との重複は本検出の対象外で、MAUI platform 層の既存挙動 (Handler 1:1 制約等) に委ねる。null 解除後の再設定は重複にならない。

#### Scenario: 同一インスタンスを2箇所へ設定すると例外

- **GIVEN** ある View が `RootHeaderView` に設定されている
- **WHEN** 同じ SettingsView 配下の別の Section の `HeaderView` にも同じインスタンスを設定する
- **THEN** `InvalidOperationException` が送出される (例外後の状態は既存の多重配置検出と同一)

#### Scenario: null 解除後の再利用は許容される

- **GIVEN** ある View が `RootHeaderView` に設定され、その後 null で解除された
- **WHEN** 同じインスタンスを別の Section の `HeaderView` に設定する
- **THEN** 例外は発生せず、その Section の header として表示される

### Requirement: HeaderHeight と view accessory の相互作用

`Section.HeaderHeight` (または Theme の header 高さ) が正値のとき、view accessory の header 領域も固定高さになり、内容がはみ出す場合は表示されない (SHALL)。未指定 (自動) のときは View の内容に応じた自動高さになる (SHALL)。優先順位は Section 指定 > Theme > 自動 (native 契約と同一 — 先行 change align-view-accessory-header-height で OS 対称化済み)。

#### Scenario: HeaderHeight 正値で固定高さになる

- **GIVEN** `Section.HeaderHeight` に正値が設定され、`HeaderView` に指定より大きい内容の View が設定されている
- **WHEN** その Section が表示される
- **THEN** header 領域は指定の固定高さになり、はみ出し分は表示されない

#### Scenario: 未指定なら内容の自動高さになる

- **GIVEN** `Section.HeaderHeight` 未指定 (自動) で `HeaderView` が設定されている
- **WHEN** その Section が表示される
- **THEN** header 領域は View の内容に応じた自動高さになる
