# maui-core デルタスペック: align-maui-accessory-placement-guard

## MODIFIED Requirements

### Requirement: 同一 View インスタンスの多重配置は例外になる

同一 SettingsView (同一 controller) 配下で、同じ View インスタンスを複数の accessory (Root / Section × Header / Footer の4対象) へ、または accessory と `CustomCell.Content` へ同時に設定することはできず、検出時に `InvalidOperationException` を送出する (SHALL)。本 Requirement の失敗時契約は4対象すべてに適用される (SHALL — テストは4対象をマトリクスで張る)。

**プロパティ設定の失敗時契約**: 配置済みの所有者 (変換経路に載っている Section / SettingsView) の accessory プロパティへの設定では、検査は**値が確定する前**に行われる (SHALL)。検査に失敗した場合、当該プロパティの公開値・それまでの View の論理所有 (論理親と継承 BindingContext)・platform 実体 (lease)・native の表示状態のいずれも変更されない (SHALL)。

**構造変更バッチの失敗時契約**: Section の追加・差し替えのバッチに含まれる accessory View の重複 — バッチ内の相互重複 (`CustomCell.Content` との交差を含む)、およびバッチ後も残る既存配置との重複 — は、native への更新を 1 件も行う前に全件検査される (SHALL)。バッチ内のどの位置の要素が衝突しても、先行要素を含めて 1 件も native・対応表へ入れない (SHALL)。Root の再構築 (コレクション差し替え・Reset) では、新しいツリー内部の相互重複と、再構築をまたいで残る配置 (root accessory) との重複を、現在のツリーへ触れる前に全件検査する (SHALL)。再構築で保証されるのは、同一 Section の継続配置 (同じ Section・同じ slot・同じ View) が例外にならず成立することである (SHALL)。旧所有者から新所有者へ null を経ずに View を移す再構築は本 Requirement の保証対象外で、現行挙動に委ねる — 別の所有者へ移す場合の保証経路は null 解除後の再設定である。

**失敗時に変更されないもの**: 検査失敗時は native・対応表・実体・論理所有のいずれも変更されず、既存の配置はそのまま残る (SHALL)。失敗した操作に由来する native gateway の呼び出し (構造・accessory・内容の更新) は発生しない (SHALL)。公開コレクション (`Root` / `Section.Cells`) への呼び出し元の変更はロールバックされない — コレクションの内容は呼び出し元の操作後の状態のまま残り、native と対応表には反映されない (既存の Section / CellBase / Content 多重配置検出と同一の契約)。

**失敗後の再収束**: バッチ検査の失敗で公開コレクションと表示が分離した後、呼び出し元は衝突要素を取り除いたうえで `Root` の全体再構築 (新しいコレクションの再代入、または Reset を伴う操作) を行うことで再収束できる (SHALL) — 再構築は対応表に載っていない要素も含めて表示を作り直す。

設定ツリーに未参加の所有者 (XAML 構築中等) の accessory プロパティには検査を行う相手がいないため、既配置の View を設定しても既存配置を奪わず、その所有者が変換経路に加わった時点 (Native Host 未接続のまま設定ツリーへ入った場合は Host 接続時) で例外になる (SHALL)。null 解除後の再設定は重複にならない (SHALL)。別 SettingsView や通常 Layout 配下との重複は本検出の対象外で、MAUI platform 層の既存挙動に委ねる。

#### Scenario: 同一インスタンスを2箇所へ設定すると例外

- **GIVEN** ある View が `RootHeaderView` に設定されている
- **WHEN** 同じ SettingsView 配下の別の Section の `HeaderView` にも同じインスタンスを設定する
- **THEN** `InvalidOperationException` が送出される

#### Scenario: 失敗した差し替えでは公開値と旧状態が一切動かない

- **GIVEN** 配置済み所有者の accessory (4対象のいずれか) に View A が表示されており、同じ SettingsView 配下の別の場所に View B が置かれている
- **WHEN** その accessory プロパティへ View B を設定する
- **THEN** `InvalidOperationException` が送出され、プロパティの公開値は View A のまま、View A の論理親・実体・表示も変更されず、失敗した操作に由来する native gateway の呼び出しは発生しない。View B 側の配置も無傷で残る

#### Scenario: Root accessory の失敗した差し替えでも同様に旧状態が残る

- **GIVEN** `RootHeaderView` に View A が表示されており、同じ SettingsView 配下の CustomCell の `Content` に View B が置かれている
- **WHEN** `RootHeaderView` へ View B を設定する
- **THEN** `InvalidOperationException` が送出され、`RootHeaderView` の公開値は View A のまま、表示・実体・View B 側の配置も変更されず、失敗した操作に由来する native gateway の呼び出しは発生しない

#### Scenario: 追加バッチ内で accessory View が重複すると 1 件も入れないまま例外

- **GIVEN** SettingsView が表示されている
- **WHEN** 同じ View インスタンスを `HeaderView` に持つ 2 つの Section を 1 回の追加でまとめて `Root` へ入れる
- **THEN** `InvalidOperationException` が送出され、失敗した操作に由来する native gateway の呼び出しは発生せず、どちらの Section も対応表に載らず、既存の配置はそのまま残る (`Root` コレクションに追加された要素はロールバックされない)

#### Scenario: 追加バッチの後続要素だけが既存配置と衝突しても先頭要素は入らない

- **GIVEN** ある View が配置済み Section の `HeaderView` に表示されている
- **WHEN** 衝突しない Section と、同じ View を `HeaderView` に持つ Section を、この順で 1 回の追加でまとめて `Root` へ入れる
- **THEN** `InvalidOperationException` が送出され、先頭の (衝突しない) Section も native・対応表へ入らず、既存の配置はそのまま残る

#### Scenario: 差し替えバッチの重複でも同様に 1 件も適用されない

- **GIVEN** 2 つの Section が表示されており、別の場所に配置済みの View がある
- **WHEN** 2 つの Section を、衝突しない新しい Section と、その View を `HeaderView` に持つ新しい Section へ、この順で 1 回の差し替え (Replace) でまとめて置き換える
- **THEN** `InvalidOperationException` が送出され、先頭の (衝突しない) 差し替えも適用されず、表示・対応表は差し替え前のまま残り、失敗した操作に由来する native gateway の呼び出しは発生しない

#### Scenario: 失敗したバッチは Root の全体再構築で再収束できる

- **GIVEN** 追加バッチの重複失敗により、公開 `Root` には対応表に載らなかった Section が残っている
- **WHEN** 衝突する Section を除いた新しいコレクションを `Root` へ再代入する
- **THEN** 例外は発生せず、残した Section が表示・対応表に登録され、公開 `Root` と表示が一致する

#### Scenario: Root 再構築内の重複は現在の木に触れないまま例外

- **GIVEN** SettingsView が表示されている
- **WHEN** 同じ View インスタンスを `HeaderView` に持つ 2 つの Section を含む新しいコレクションを `Root` へ設定する
- **THEN** `InvalidOperationException` が送出され、現在の表示・対応表・既存配置の実体はそのまま残る

#### Scenario: 同一 Section を含む Root 再構築は引き続き成立する

- **GIVEN** 配置済み Section A の `HeaderView` に View が表示されている
- **WHEN** Section A と新しい Section を含む新しいコレクションを `Root` へ設定する
- **THEN** 例外は発生せず、View は引き続き Section A の header として表示される

#### Scenario: Root 再構築の新ツリーが root accessory と衝突すると現在の木に触れないまま例外

- **GIVEN** ある View が `RootHeaderView` に表示されている
- **WHEN** 同じ View を `HeaderView` に持つ Section を含む新しいコレクションを `Root` へ設定する
- **THEN** `InvalidOperationException` が送出され、現在の表示・対応表・既存配置の実体はそのまま残る

#### Scenario: 未参加の所有者に持ち越された重複は参加時点で弾かれる

- **GIVEN** 設定ツリーに未参加の Section の `HeaderView` に、既に他所へ配置済みの View が設定されている (この時点では例外にならない)
- **WHEN** その Section を `Root` へ追加する
- **THEN** `InvalidOperationException` が送出され、失敗した操作に由来する native gateway の呼び出しは発生せず、既存の配置はそのまま残る

#### Scenario: 未接続のまま構築された重複は Host 接続時に弾かれる

- **GIVEN** Native Host 未接続の SettingsView の設定ツリーに、同じ View を accessory に持つ 2 つの所有者が入っている (この時点では例外にならない)
- **WHEN** Native Host を接続する
- **THEN** `InvalidOperationException` が送出される

#### Scenario: null 解除後の再利用は許容される

- **GIVEN** ある View が `RootHeaderView` に設定され、その後 null で解除された
- **WHEN** 同じインスタンスを別の Section の `HeaderView` に設定する
- **THEN** 例外は発生せず、その Section の header として表示される
