# maui-core デルタスペック

`KsSettingsView.Maui` (MAUI facade 層) の挙動契約。Bridge 以下の保証 (Store の収束・復元・バッチ配信) は既存契約に依拠し、ここでは facade が観察可能に提供する挙動のみを規定する。

## ADDED Requirements

### Requirement: 公開コンテナ形状

SettingsView は `Root` (`IList<Section>`、既定値は observable な `SettingsRoot` インスタンス) を content property として公開し、XAML で Section を直接並べられなければならない (SHALL)。Section は `Cells` (`IList<CellBase>`、既定値は observable なコレクション) を content property として公開し、XAML で Cell を直接並べられなければならない (SHALL)。`Root` / `Cells` の差し替え時は、旧コレクションの購読を解除し、新コレクションの内容で表示を再構築しなければならない (SHALL)。

#### Scenario: XAML 直置き

- **GIVEN** XAML で SettingsView 直下に Section、Section 直下に LabelCell を並べたページ
- **WHEN** ページを表示する
- **THEN** 記述どおりの構造で表示に反映される

#### Scenario: Root の差し替え

- **GIVEN** 表示中の SettingsView
- **WHEN** `Root` に別のコレクションを代入する
- **THEN** 新コレクションの内容で表示が再構築され、旧コレクションへの操作は表示に反映されなくなる

### Requirement: UI スレッド契約

SettingsView / Section / CellBase への操作 (コレクション操作・プロパティ変更) は UI スレッドで行う呼び出し側契約とし、facade はスレッド marshal を行わない (SHALL NOT)。

#### Scenario: UI スレッド上の操作

- **GIVEN** 表示中の SettingsView
- **WHEN** UI スレッド上でコレクション操作とプロパティ変更を行う
- **THEN** すべて表示に反映される

### Requirement: Handler 接続時の表示反映

SettingsView は Handler 接続 (ページ表示) 時に native Host を生成し、その時点の Root 構造 (Section / Cell の内容と accessory テキスト) を表示に反映させなければならない (SHALL)。

#### Scenario: 初回表示

- **GIVEN** Section 1つと LabelCell 2つを構成した SettingsView
- **WHEN** ページ表示で Handler が接続される
- **THEN** 構成どおりの Section / Cell が native 表示に反映される

### Requirement: Handler 切断と再接続の復元

SettingsView は Handler 切断時に native Host を解放し、再接続時には切断中の変更を含む最新状態を表示に復元しなければならない (SHALL)。`RootHeaderText` / `RootFooterText` の所有値も再接続後に再適用されなければならない (SHALL)。iOS では Host (ViewController) は子 ViewController として親子関係を成立させ、切断時に親子関係を解消しなければならない (SHALL)。

#### Scenario: 再接続で最新状態を復元

- **GIVEN** 表示済みの SettingsView がページ離脱で Handler 切断された
- **WHEN** 切断中に LabelCell の `ValueText` を変更し、ページ再訪問で Handler が再接続される
- **THEN** 変更後の `ValueText` で表示が復元される

#### Scenario: root accessory の再適用

- **GIVEN** `RootHeaderText` を設定済みの SettingsView が Handler 切断された
- **WHEN** ページ再訪問で Handler が再接続される
- **THEN** 両 OS で `RootHeaderText` が表示に反映されている

#### Scenario: iOS の親子関係

- **GIVEN** iOS でページ表示された SettingsView
- **WHEN** Handler が接続され、その後ページ離脱で切断される
- **THEN** 接続中は Host ViewController が親 ViewController の子であり、切断後は親子関係が解消され旧 controller への参照が facade / Handler に残らない

### Requirement: 静的コレクションの描画

`Root` / `Cells` に `INotifyCollectionChanged` を実装しないコレクションが渡された場合、SettingsView は Handler 接続時点の内容で描画し、以後のコレクション操作を表示に反映してはならない (SHALL NOT)。

#### Scenario: List への追加は反映されない

- **GIVEN** `List<Section>` を `Root` に設定して表示中の SettingsView
- **WHEN** コードで `List` に Section を追加する
- **THEN** 表示は接続時点の内容のまま変化しない

### Requirement: 構造変更の反映

`INotifyCollectionChanged` を実装するコレクションの構造イベント (Add / Remove / Move / Replace) は、対応する構造更新として native へ配信されなければならない (SHALL)。`Reset` は Root 全体の再構築として配信されなければならない (SHALL)。

#### Scenario: Section の追加

- **GIVEN** observable な `Root` で表示中の SettingsView
- **WHEN** `Root` に Section を追加する
- **THEN** 追加された Section が表示に反映される

#### Scenario: Cell の移動

- **GIVEN** observable な `Cells` に 3つの Cell を持つ表示中の Section
- **WHEN** `Move` で Cell の順序を入れ替える
- **THEN** 入れ替え後の順序で表示に反映される

#### Scenario: Clear で再構築

- **GIVEN** 表示中の SettingsView
- **WHEN** `Root.Clear()` を呼ぶ
- **THEN** 表示が空の状態へ再構築される

### Requirement: 同一インスタンスの重複配置の禁止

同一の Section インスタンスを複数箇所へ、または同一の CellBase インスタンスを複数箇所へ配置することは禁止し、配置が表示へ変換される時点で `InvalidOperationException` を送出しなければならない (SHALL)。ItemsSource のテンプレートが既配置のインスタンスを返した場合も同様とする (SHALL)。

#### Scenario: 同一 Cell の二重追加

- **GIVEN** 表示中の SettingsView と、既にある Section に配置済みの LabelCell
- **WHEN** 同じ LabelCell インスタンスを別の Section の `Cells` に追加する
- **THEN** `InvalidOperationException` が送出され、表示は変化しない

### Requirement: Cell 内容更新のバッチ配信

Cell の内容更新 (`CellBase` プロパティ変更) は即時に配信せず、最初の変更で 1 回だけ予約される flush までの変更を 1 バッチとして配信しなければならない (SHALL)。バッチのうち可視性 (`IsVisible`) が変化した Cell は単発の内容更新として、それ以外の複数 Cell の変更は 1 回の一括更新として配信しなければならない (SHALL)。

#### Scenario: 複数 Cell の変更が 1 バッチになる

- **GIVEN** 表示中の SettingsView と 2つの LabelCell
- **WHEN** 同一イベントハンドラ内で両方の `Title` を変更する
- **THEN** flush 実行時に両変更が 1 回の一括更新として配信され、表示に反映される

#### Scenario: 複数 Cell の可視性変更

- **GIVEN** 表示中の SettingsView と 2つの LabelCell
- **WHEN** 同一イベントハンドラ内で両方の `IsVisible` を false に変更する
- **THEN** 各 Cell が単発の内容更新として配信され、両方とも表示から除かれた状態になる

#### Scenario: 保留中に削除された Cell の更新は安全に破棄される

- **GIVEN** 表示中の SettingsView
- **WHEN** LabelCell の `Title` を変更した直後、flush 実行前にその Cell をコレクションから削除する
- **THEN** 例外なく処理され、削除された Cell の保留更新は配信されない (残りの保留更新のみ配信される)

### Requirement: 削除済み要素からの通知遮断

コレクションから除去・置換された Section / Cell への購読は除去時点で同期的に解除されなければならない (SHALL)。除去済み Section / Cell のプロパティをその後変更しても、native への配信・例外のいずれも発生してはならない (SHALL NOT)。

#### Scenario: 削除済み Section の HeaderText 変更

- **GIVEN** 表示中の SettingsView から Remove 済みの Section
- **WHEN** その Section の `HeaderText` を変更する
- **THEN** native への配信は発生せず、例外も発生しない

### Requirement: Root header / footer テキスト

SettingsView は `RootHeaderText` / `RootFooterText` (string) を公開し、設定・変更を root accessory として表示に反映させなければならない (SHALL)。null の設定は表示のクリアとして反映されなければならない (SHALL)。

#### Scenario: 設定と反映

- **GIVEN** 表示中の SettingsView
- **WHEN** `RootHeaderText` に文字列を設定する
- **THEN** root header として表示に反映される

#### Scenario: null でクリア

- **GIVEN** `RootFooterText` 設定済みの表示中の SettingsView
- **WHEN** `RootFooterText` に null を設定する
- **THEN** root footer の表示がクリアされる

### Requirement: Section header / footer テキスト

Section は `HeaderText` / `FooterText` (string) の対称対を公開し、設定・変更を該当 Section の accessory として反映させなければならない (SHALL)。

#### Scenario: 表示中の変更

- **GIVEN** `HeaderText` 設定済みの表示中の Section
- **WHEN** `FooterText` に文字列を設定する
- **THEN** 該当 Section の footer として表示に反映される

### Requirement: CellBase / LabelCell の公開プロパティ

CellBase は `Title` / `Description` / `HintText` / `IsEnabled` / `IsVisible` を公開し、LabelCell はこれに `ValueText` を追加公開しなければならない (SHALL)。各プロパティの変更は表示へ反映されなければならない (SHALL)。本変更で公開するプロパティは Bridge interop が輸送できるこの範囲に限る (SHALL)。

#### Scenario: ValueText の反映

- **GIVEN** 表示中の LabelCell
- **WHEN** `ValueText` を変更する
- **THEN** 変更後の値が表示に反映される

#### Scenario: IsVisible の反映

- **GIVEN** 表示中の LabelCell
- **WHEN** `IsVisible` を false に変更する
- **THEN** 該当 Cell が表示から除かれた状態になる

### Requirement: ItemsSource / ItemTemplate による生成

SettingsView は `ItemsSource` + `ItemTemplate` (+ `TemplateStartIndex`) による Section 生成を、Section は同名プロパティによる Cell 生成を提供しなければならない (SHALL)。生成された要素の `BindingContext` は対応する item でなければならない (SHALL)。挙動は次のとおり (SHALL):

- `ItemTemplate` 未設定の間は生成しない (後から設定された時点で生成する)
- `ItemTemplate` / `TemplateStartIndex` の表示中の変更は、既存のテンプレ生成分を除去して再生成する
- `ItemsSource` が `INotifyCollectionChanged` を実装する場合、Add / Remove / Replace / Move / Reset を生成先コレクションへミラーする
- `ItemsSource = null` および `Reset` では、生成区間への手動挿入があってもテンプレ生成分のみを除去し、手動追加分を温存する
- テンプレートが期待型 (Section 生成なら Section、Cell 生成なら CellBase) 以外を生成した場合は `InvalidOperationException` を送出する

生成物は通常の構造変更と同じ経路で表示に反映されなければならない (SHALL)。

#### Scenario: items から Cell を生成

- **GIVEN** 3件の item を持つ observable なコレクションと CellBase を生成する `ItemTemplate` を設定した Section
- **WHEN** SettingsView を表示する
- **THEN** 各 item を `BindingContext` に持つ 3つの Cell が生成され表示に反映される

#### Scenario: Template の後付け

- **GIVEN** `ItemsSource` のみ設定済み (Cell 未生成) の表示中の Section
- **WHEN** `ItemTemplate` を設定する
- **THEN** その時点で items から Cell が生成され表示に反映される

#### Scenario: item 追加のミラー

- **GIVEN** テンプレ生成済みの表示中の Section
- **WHEN** items に 1件追加する
- **THEN** 対応する Cell が生成され表示に反映される

#### Scenario: 生成区間へ手動挿入後の Reset

- **GIVEN** テンプレ生成の Cell 3つの間へ手動で Cell 1つを挿入した表示中の Section
- **WHEN** items をクリア (Reset) する
- **THEN** テンプレ生成分のみ除去され、手動挿入した Cell は表示に残る

#### Scenario: ItemsSource の null 化

- **GIVEN** 手動追加の Cell 1つとテンプレ生成の Cell 3つを持つ表示中の Section
- **WHEN** `ItemsSource` に null を設定する
- **THEN** テンプレ生成分のみ除去され、手動追加の Cell は表示に残る

### Requirement: Handler 登録

`AddKsSettingsView()` は `SettingsViewHandler` 1件のみを登録し、これにより XAML / C# から SettingsView が利用可能にならなければならない (SHALL)。Cell 種別ごとの Handler は存在してはならない (SHALL NOT)。

#### Scenario: 登録して利用

- **GIVEN** `MauiAppBuilder` で `AddKsSettingsView()` を呼んだアプリ
- **WHEN** XAML で SettingsView を配置したページを表示する
- **THEN** SettingsView が表示される

### Requirement: 切断後の資源回収

Handler 切断後、facade / Handler 側に Handler / platform view / 解放済み Host への強参照が残ってはならず (SHALL NOT)、Handler と platform view は GC で回収可能でなければならない (SHALL)。SettingsView (facade) 自身への参照がなくなった場合、外部がコレクションや Cell を保持し続けていても、SettingsView と gateway (Bridge 参照) は回収可能でなければならない (SHALL)。native Host 実体の解放は Bridge の `releaseHost()` 既存契約に依拠する。

#### Scenario: 切断後の回収

- **GIVEN** 表示済みの SettingsView が Handler 切断された
- **WHEN** Handler / platform view への外部参照を捨てて GC を実行する
- **THEN** `WeakReference` 検証で両者が回収済みになる

#### Scenario: 外部保持があっても facade は回収される

- **GIVEN** Handler 切断済みの SettingsView と、外部 (ViewModel 相当) が保持し続ける observable コレクションおよび Cell
- **WHEN** SettingsView への参照のみを捨てて GC を実行する
- **THEN** `WeakReference` 検証で SettingsView と gateway が回収済みになる
