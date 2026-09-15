## ADDED Requirements

### Requirement: Section / Cell は SettingsView の論理子である
`Section` は所属する `SettingsView` の、`CellBase` は所属する `Section` の論理子 (`Parent`) であること。論理子であることは Handler (Native Host) の有無に依らず、`Root` / `Cells` への所属が始まった時点で成立し、所属が終わった時点 (除去・差し替え・Reset・ItemsSource 再生成による除去) で解除される (`Parent` が null に戻る)。所属の解除で `Parent` が旧所属先を指し続けてはならない (SHALL NOT)。

#### Scenario: XAML 構築時 (Host 未接続) に Section と Cell が論理子になる
- **GIVEN** Handler の無い `SettingsView`
- **WHEN** `Root` に `Section` を追加し、その `Cells` に `CellBase` を追加する
- **THEN** `Section.Parent` は `SettingsView`、`CellBase.Parent` は `Section` である

#### Scenario: Root から Section を除去すると Section と配下 Cell の論理子が解除される
- **GIVEN** `Root` に所属する `Section` (配下に Cell あり)
- **WHEN** `Root.Remove(section)` する
- **THEN** `section.Parent` は null になり、配下 Cell の `Parent` は `section` のまま (Section → Cell の所属は変わらない)

#### Scenario: Section から Cell を除去すると論理子が解除される
- **GIVEN** `Section.Cells` に所属する `CellBase`
- **WHEN** `Cells.Remove(cell)` する
- **THEN** `cell.Parent` は null

#### Scenario: Root コレクションの差し替えで旧 Section の論理子が解除され新 Section が論理子になる
- **GIVEN** `Root` に Section A が所属する `SettingsView`
- **WHEN** `Root` を Section B だけを含む別コレクションに再代入する
- **THEN** A の `Parent` は null、B の `Parent` は `SettingsView`

#### Scenario: Reset で所属から外れた要素の論理子が解除され、再追加で再び論理子になる
- **GIVEN** `Cells` (ObservableCollection) に Cell X / Y が所属する `Section`
- **WHEN** `Clear()` する (Reset 通知の時点でコレクションは空)
- **THEN** X / Y の `Parent` はともに null
- **WHEN** 続けて X を `Add` する
- **THEN** X の `Parent` は `Section`、Y の `Parent` は null のまま

#### Scenario: Cells コレクションの差し替えで旧 Cell の論理子が解除され新 Cell が論理子になる
- **GIVEN** `Cells` に Cell X が所属する `Section`
- **WHEN** `Cells` を Cell Y だけを含む別コレクションに再代入する
- **THEN** X の `Parent` は null、Y の `Parent` は `Section`。旧コレクションへその後 Cell を追加しても `Section` の論理子にはならない

#### Scenario: Host 未接続で同じ Cell を 2 回追加して 1 件除去しても所属が残る間は論理子のまま
- **GIVEN** Handler の無い `SettingsView` の `Cells` に同じ Cell インスタンスを 2 回 `Add` した Section
- **WHEN** 1 件だけ `Remove` する
- **THEN** Cell の `Parent` は `Section` のまま (もう 1 件除去すると null)

#### Scenario: ItemsSource の再生成で除去された Cell の論理子が解除される
- **GIVEN** `ItemsSource` / `ItemTemplate` から生成された Cell を持つ `Section`
- **WHEN** `ItemsSource` を別の items に差し替える
- **THEN** 旧 items から生成された Cell の `Parent` は null、新 items から生成された Cell の `Parent` は `Section` で、その `BindingContext` は対応する item のまま

#### Scenario: Host 接続中の除去でも論理子が解除される
- **GIVEN** Handler の接続された `SettingsView` に表示中の Section と Cell
- **WHEN** `Cells.Remove(cell)` する
- **THEN** `cell.Parent` は null で、native への構造更新は現行どおり配信される

### Requirement: 論理子化は既存の BindingContext 継承契約を変えない
論理子化の前後で、`Root` / `Cells` に直接並べた Section / Cell に `SettingsView.BindingContext` が継承されること、ItemsSource / ItemTemplate から生成した要素が対応する item を `BindingContext` として保つこと (継承で上書きされない) は現行のまま成立すること。

#### Scenario: 直接並べた Cell に BindingContext が継承される
- **GIVEN** `Root` に Section / Cell を並べた `SettingsView`
- **WHEN** `SettingsView.BindingContext` を設定する
- **THEN** Section と Cell の `BindingContext` は同じ値で、Cell の `{Binding}` が解決される

#### Scenario: ItemsSource 生成 Cell は item を BindingContext として保つ
- **GIVEN** `ItemsSource` から生成された Cell を持つ Section (論理子)
- **WHEN** `SettingsView.BindingContext` を別の値に変える
- **THEN** 生成 Cell の `BindingContext` は対応する item のまま

### Requirement: Section / Cell に設定した binding は外観変更と Resources の変更で再評価される
`Section` / `CellBase` (派生 Cell を含む) の BindableProperty に設定した `AppThemeBinding` は、初期解決に加えてアプリの外観 (`Application.UserAppTheme` の変更、または端末の外観の変更) で再評価され、`DynamicResource` は所属先の祖先 (ページ / アプリ) の Resources の変更で再評価されること。再評価はプロパティ値の変更として観測できること (対象プロパティは限定しない: MAUI の binding 機構が扱う全 BindableProperty)。所属を解除された要素 (`Parent` が null) は旧所属先の Resources の変更に追随しないこと。

#### Scenario: Cell の色プロパティの AppThemeBinding が外観変更で再評価される
- **GIVEN** ページに載った `SettingsView` の `ButtonCell.TitleColor` に light / dark で異なる色の `AppThemeBinding` を設定し、`Application.UserAppTheme` が light
- **WHEN** `Application.UserAppTheme` を dark に変える
- **THEN** `TitleColor` は dark の値になる

#### Scenario: Section のプロパティの AppThemeBinding が外観変更で再評価される
- **GIVEN** `Section.HeaderText` に light / dark で異なる文字列の `AppThemeBinding` を設定し、`Application.UserAppTheme` が light
- **WHEN** `Application.UserAppTheme` を dark に変える
- **THEN** `HeaderText` は dark の値になる

#### Scenario: Cell の DynamicResource がページ Resources の差し替えで再評価される
- **GIVEN** ページの Resources にキー K の色があり、`CellBase.TitleColor` に `DynamicResource K` を設定した Cell
- **WHEN** ページの Resources のキー K の値を別の色に置き換える
- **THEN** `TitleColor` は新しい色になる

#### Scenario: Cell の DynamicResource がアプリ Resources の差し替えで再評価される
- **GIVEN** `Application.Current.Resources` にキー K の色があり、`CellBase.TitleColor` に `DynamicResource K` を設定した Cell (ページ Resources にキー K は無い)
- **WHEN** アプリ Resources のキー K の値を別の色に置き換える
- **THEN** `TitleColor` は新しい色になる

#### Scenario: 所属を解除された Cell は旧所属先の Resources 変更に追随しない
- **GIVEN** ページ Resources のキー K に `DynamicResource` を設定した Cell を `Cells` から除去した後
- **WHEN** ページ Resources のキー K の値を置き換える
- **THEN** Cell の `TitleColor` は変わらない

### Requirement: 再評価されたプロパティ値は通常のプロパティ変更と同じ経路で反映される
binding の再評価によるプロパティ値の変更は、同じプロパティを直接代入したときと同じ反映経路を通ること。Cell の内容プロパティ (色・文字列など snapshot に載る値) なら内容更新 (gateway の `ReplaceCell` / `ReplaceCells`) として表示中の行に届き、行は作り直されない (構造更新 `RemoveCell` / `InsertCell` を伴わない) こと。Section の header / footer text なら accessory 更新として届くこと。構造プロパティ (`Cells` / `ItemsSource` / `ItemTemplate`) や表示値でないプロパティ (command 等) は、それぞれの通常の経路 (構造更新 / 反映なし) のままであること。

#### Scenario: Cell の色の再評価が内容更新として配信される
- **GIVEN** Handler の接続された `SettingsView` に表示中の `ButtonCell` (`TitleColor` に `AppThemeBinding`)
- **WHEN** `Application.UserAppTheme` を切り替える
- **THEN** gateway へその Cell の `ReplaceCell` / `ReplaceCells` が配信され、`RemoveCell` / `InsertCell` は配信されない

#### Scenario: Section の HeaderText の再評価が accessory 更新として配信される
- **GIVEN** Handler の接続された `SettingsView` に表示中の Section (`HeaderText` に `AppThemeBinding`)
- **WHEN** `Application.UserAppTheme` を切り替える
- **THEN** gateway へその Section の `UpdateAccessory` が配信される

### Requirement: 論理子化は SettingsView の回収を妨げない
外部 (ViewModel 等) が `Root` コレクション・Section・Cell を保持し続けても、`SettingsView` と gateway が回収されること (現行の leak 契約) は論理子化の後も成立すること。

#### Scenario: 外部が root を保持したまま SettingsView が回収される
- **GIVEN** 外部が `Root` コレクション (Section / Cell を含む) を強く保持し、Section / Cell は論理子として `Parent` を持つ
- **WHEN** `SettingsView` への参照を手放して GC する
- **THEN** `SettingsView` と gateway は回収される

## MODIFIED Requirements

### Requirement: 同一インスタンスの重複配置の禁止

同一の Section インスタンスを複数箇所へ、または同一の CellBase インスタンスを複数箇所へ配置することは禁止する (SHALL)。複数箇所には、同じ SettingsView 内の別の Section、別の SettingsView、および SettingsView から既に外れた Section が所有したままの場合を含む。

- 既に他所 (期待する所有者以外の facade 所有者) の論理子である Section / Cell を `Root` / `Cells` へ追加した場合は、**追加の時点で** `InvalidOperationException` を送出しなければならない (SHALL)。Handler の有無に依らない。既存の配置 (`Parent` / `BindingContext` / 表示) は変えない (SHALL NOT)
- 同じコレクションへ同一インスタンスを二重に入れた場合、および ItemsSource のテンプレートが既配置のインスタンスを返した場合は、配置が表示へ変換される時点で `InvalidOperationException` を送出しなければならない (SHALL)。構造変更バッチ内の重複は native へ構造・snapshot を配信する前に全件検査され、部分更新を残してはならない (SHALL NOT)
- 失敗後も公開コレクションはロールバックされず、回復は Root の全体再構築で行う (現行どおり)

#### Scenario: 同一 Cell の二重追加

- **GIVEN** 表示中の SettingsView と、既にある Section に配置済みの LabelCell
- **WHEN** 同じ LabelCell インスタンスを別の Section の `Cells` に追加する
- **THEN** 追加の時点で `InvalidOperationException` が送出され、表示は変化しない

#### Scenario: 別の SettingsView に所有された Cell の追加は Host 未接続でも追加時に例外
- **GIVEN** 表示中の SettingsView の Section A に所属する Cell と、Handler の無い別の SettingsView の Section B
- **WHEN** 同じ Cell を B の `Cells` に追加する
- **THEN** `InvalidOperationException` が送出され、Cell の `Parent` と `BindingContext` は A 側のまま変わらない

#### Scenario: 別の SettingsView に所有された Section の追加は追加時に例外
- **GIVEN** SettingsView X の `Root` に所属する Section と、別の SettingsView Y
- **WHEN** 同じ Section を Y の `Root` に追加する
- **THEN** `InvalidOperationException` が送出され、Section の `Parent` は X のまま

#### Scenario: SettingsView から外れた Section が所有したままの Cell の追加は例外
- **GIVEN** `Root` から除去された Section A (その `Cells` に Cell が残っている。Cell の `Parent` は A)
- **WHEN** 同じ Cell を別の Section B の `Cells` に追加する
- **THEN** `InvalidOperationException` が送出され、Cell の `Parent` は A のまま

#### Scenario: 除去してから別の Section へ追加した Cell は新しい所属先の論理子になる
- **GIVEN** Section A に所属する Cell
- **WHEN** A の `Cells` から除去した後、Section B の `Cells` に追加する
- **THEN** Cell の `Parent` は B

#### Scenario: 同じコレクションへの二重追加は変換時に例外
- **GIVEN** Handler の無い SettingsView の Section に同じ Cell インスタンスを 2 回 `Add` した状態 (追加時は例外にならない)
- **WHEN** SettingsView に Handler を接続する
- **THEN** `InvalidOperationException` が送出され、構造・snapshot は native へ配信されない (Theme / Style の先行配信は許容)

### Requirement: 同一 View インスタンスの多重配置は例外になる

同一の View インスタンスを複数の accessory View (Root / Section の Header・Footer) / `CustomCell.Content` へ置くことは `InvalidOperationException` であること。複数箇所には**期待する所有者以外の facade 所有者の論理子である場合を含む** (SHALL): 別の SettingsView、その配下の Section / CustomCell、および SettingsView から既に外れた Section / CustomCell が所有したままの View を置いた場合も例外とする。View 配置プロパティの検査は値が確定する前に行われ、失敗しても公開値・論理所有・表示はいずれも動かないこと。構造変更バッチ内の重複は native へ触れる前に全件検査され、部分更新を残さないこと。設定ツリーに未参加の所有者へ既配置の View を設定した場合は既存配置を奪わず、その所有者が変換経路に加わった時点で例外になること。

#### Scenario: 別の SettingsView に置かれた View を accessory に置くと変換時に例外
- **GIVEN** 表示中の SettingsView X の Section header に置かれた View と、Handler の無い別の SettingsView Y の Section
- **WHEN** 同じ View を Y の Section の `HeaderView` に設定し、Y に Handler を接続する
- **THEN** 設定の時点では例外にならず View の `Parent` は X 側のまま。Handler 接続時に `InvalidOperationException` が送出され、Y の構造・snapshot は配信されず (Theme / Style の先行配信は許容)、X 側の配置は変わらない

#### Scenario: 別の SettingsView の CustomCell に置かれた View を Content に置くと変換時に例外
- **GIVEN** 表示中の SettingsView X の `CustomCell.Content` に置かれた View と、Handler の無い別の SettingsView Y の CustomCell
- **WHEN** 同じ View を Y の CustomCell の `Content` に設定し、Y に Handler を接続する
- **THEN** 設定の時点では例外にならず View の `Parent` は X 側のまま。Handler 接続時に `InvalidOperationException` が送出され、X 側の配置は変わらない

#### Scenario: SettingsView から外れた Section が所有したままの View を accessory に置くと例外
- **GIVEN** `Root` から除去された Section A の `HeaderView` に置かれたままの View と、表示中の SettingsView の Section B
- **WHEN** 同じ View を B の `HeaderView` に設定する
- **THEN** `InvalidOperationException` が送出され、A 側の配置は変わらない
