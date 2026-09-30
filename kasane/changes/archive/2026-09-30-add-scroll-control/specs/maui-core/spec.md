# Delta: maui-core (add-scroll-control)

## ADDED Requirements

### Requirement: スクロール命令のインターフェースと位置の公開

facade は、名前空間 `KsSettingsView` にインターフェース `IScrollController` と enum `ScrollPosition` (`Start` / `Center` / `End`) を公開する (SHALL)。`IScrollController` の命令は次の 4 種で、`position` の既定は `Start`、`animated` の既定は `true` とする (SHALL)。

- Cell へ: `ScrollTo(object target, ScrollPosition position, bool animated)`
- Section へ: `ScrollToSection(object target, ScrollPosition position, bool animated)`
- 先頭へ・末尾へ: `ScrollToStart(bool animated)` / `ScrollToEnd(bool animated)`

位置と先頭・末尾の意味は Native の Host の契約と同じとする。MAUI 標準の `ScrollToPosition` は使わない。

#### Scenario: fake のインターフェース実装で ViewModel をテストできる
- **GIVEN** `IScrollController` 型のプロパティを持つ ViewModel と、呼び出しを記録する fake 実装
- **WHEN** ViewModel の処理が `ScrollToEnd()` を呼ぶ
- **THEN** fake 実装に `ScrollToEnd(animated: true)` の呼び出しが記録される (SettingsView を生成しなくてよい)

### Requirement: SettingsView が作った命令ハンドルを ViewModel へ渡す

`SettingsView` は BindableProperty `ScrollController` (型 `IScrollController`、既定の向き OneWayToSource) を公開する (SHALL)。値は `SettingsView` ごとに 1 つ、`SettingsView` 自身が生成した実体とする (SHALL)。外から別の値を代入されても、自身の実体に戻す (SHALL)。実装クラスは公開しない。

#### Scenario: Binding で ViewModel に実体が届く
- **GIVEN** `IScrollController` 型のプロパティ `Scroll` を持つ ViewModel を `BindingContext` にした SettingsView
- **WHEN** XAML で `ScrollController="{Binding Scroll}"` と書く
- **THEN** ViewModel の `Scroll` に、その SettingsView の実体が代入される

#### Scenario: SettingsView ごとに別の実体を持つ
- **GIVEN** 2 つの SettingsView
- **WHEN** それぞれの `ScrollController` を読む
- **THEN** 別々の実体が返る

#### Scenario: 外から代入した値は採用されない
- **GIVEN** SettingsView と、別に用意した `IScrollController` の実装
- **WHEN** その実装を `ScrollController` に代入する
- **THEN** `ScrollController` は SettingsView 自身の実体のまま変わらない

### Requirement: Section と Cell の明示 ID

`Section` は BindableProperty `SectionId` (型 `string?`、既定 `null`) を、`CellBase` は BindableProperty `CellId` (型 `string?`、既定 `null`) を公開する (SHALL)。明示 ID はスクロール命令の対象を指すためだけに使い、表示・Bridge の ID・値の書き戻しには影響しない (SHALL)。

#### Scenario: 明示 ID を付けても表示は変わらない
- **GIVEN** `CellId` を付けた Cell と付けない Cell を並べた SettingsView
- **WHEN** 表示する
- **THEN** 両者の表示と Bridge への配信内容は `CellId` 以外同じになる

### Requirement: 命令の対象の解決

`ScrollTo` は Cell を、`ScrollToSection` は Section を対象として、`target` を次の順で解決する (SHALL)。

1. 明示 ID (`CellId` / `SectionId`) が `target` と等しい要素のうち、表示順で最初のもの
2. 無ければ、`ItemsSource` から生成した要素のうち、生成元の項目が `target` と等しいもの。表示順で最初のものを採る

`ScrollTo` の項目は Section の `ItemsSource` から生成した Cell を、`ScrollToSection` の項目は SettingsView の `ItemsSource` から生成した Section を対象とする。生成元の項目との対応は、生成物の `BindingContext` ではなく、生成したときの項目と生成物の並びで取る (SHALL)。この対応は ItemsSource の追加・削除・置換・移動・作り直しの後も保たれる。命令のたびに `ItemsSource` を列挙し直しては取らない。解決できた要素は、その Bridge の ID で gateway へ命令を渡す。解決できないときは gateway を呼ばない (SHALL)。Debug 出力に英語の警告を出す。非表示の要素は解決の対象に含め、表示しないことによる何もしない扱いは Native に委ねる。

#### Scenario: 手で並べた Cell を明示 ID で指せる
- **GIVEN** fake gateway を注入し、XAML で手で並べた Cell に `CellId="wifi"` を付けた SettingsView
- **WHEN** `ScrollController.ScrollTo("wifi", ScrollPosition.Center)` を呼ぶ
- **THEN** gateway はその Cell の Bridge の ID で、位置 center の Cell への命令を 1 回受ける

#### Scenario: ItemsSource の項目で生成した Cell を指せる
- **GIVEN** fake gateway を注入し、Section の `ItemsSource` に項目 A・B・C を渡して Cell を生成した SettingsView
- **WHEN** `ScrollController.ScrollTo(B)` を呼ぶ
- **THEN** gateway は B から生成した Cell の Bridge の ID で Cell への命令を受ける

#### Scenario: ItemsSource の項目で生成した Section を指せる
- **GIVEN** fake gateway を注入し、SettingsView の `ItemsSource` に項目 X・Y を渡して Section を生成した SettingsView
- **WHEN** `ScrollController.ScrollToSection(Y)` を呼ぶ
- **THEN** gateway は Y から生成した Section の Bridge の ID で Section への命令を受ける

#### Scenario: 明示 ID と項目の両方に当たるときは明示 ID の要素を採る
- **GIVEN** 手で並べた Cell P に `CellId="a"`、別の Section の `ItemsSource` に文字列の項目 `"a"` から生成した Cell Q を持つ SettingsView
- **WHEN** `ScrollController.ScrollTo("a")` を呼ぶ
- **THEN** gateway は Cell P の Bridge の ID で命令を受ける

#### Scenario: 生成物の BindingContext を差し替えても元の項目で指せる
- **GIVEN** 項目 B から生成した Cell の `BindingContext` を別のオブジェクトに差し替えた SettingsView
- **WHEN** `ScrollController.ScrollTo(B)` を呼ぶ
- **THEN** gateway は B から生成した Cell の Bridge の ID で命令を受ける

#### Scenario: 項目の移動と置換の後も元の項目で指せる
- **GIVEN** Section の `ItemsSource` (`ObservableCollection`) に項目 A・B・C を渡して Cell を生成した SettingsView
- **WHEN** B を末尾へ移動し、C を新しい項目 D に置き換えてから、`ScrollController.ScrollTo(B)` と `ScrollTo(D)` を呼ぶ
- **THEN** gateway はそれぞれ B から生成した Cell、D から生成した Cell の Bridge の ID で命令を受け、置き換えで消えた C では命令を受けない

#### Scenario: 解決できない対象は gateway を呼ばない
- **GIVEN** fake gateway を注入した SettingsView
- **WHEN** どの明示 ID にも項目にも当たらない値で `ScrollTo` を呼ぶ
- **THEN** gateway はスクロール命令を受けない

### Requirement: 命令の gateway 経路と Host が無い間の扱い

命令は `KsSettingsController` から gateway へ渡し、gateway は Bridge のスクロール命令 API を呼ぶ (SHALL)。gateway は Cell への命令・Section への命令・先頭へ・末尾への 4 メソッドを持つ。`position` は `Start` = 0、`Center` = 1、`End` = 2 の整数で運ぶ。gateway が未接続 (Host の初回生成前) のときの命令は何もしない (SHALL)。Handler の切断で Host が解放されている間の命令は gateway に渡し、Bridge が何もしない (maui-bridge の契約)。

#### Scenario: 先頭・末尾への命令が gateway に届く
- **GIVEN** fake gateway を注入して接続した SettingsView
- **WHEN** `ScrollController.ScrollToEnd(false)` を呼ぶ
- **THEN** gateway は末尾への命令を `animated = false` で 1 回受ける

#### Scenario: Host の初回生成前の命令は何もしない
- **GIVEN** gateway を接続する前 (Handler が Host を作る前) の SettingsView
- **WHEN** `ScrollController.ScrollToEnd()` を呼ぶ
- **THEN** 例外は起きず、その後に接続した gateway もこの命令を受けない

### Requirement: 準備完了のコマンド

`SettingsView` は BindableProperty `ScrollControllerReadyCommand` (型 `ICommand`) を公開する (SHALL)。Host が生成されて取り付けが済むたびに、`CanExecute(null)` が真なら `Execute(null)` を呼ぶ (SHALL)。これは Handler の再接続による Host の作り直しでも起きる。Host の作り直しで Bridge が戻すスクロール位置は、この合図の中で出した命令より先に処理される (SHALL)。したがって合図の中の命令が最終位置を決める。

#### Scenario: Host の生成と取り付けの後に合図が来る
- **GIVEN** `ScrollControllerReadyCommand` を設定した SettingsView
- **WHEN** Handler が接続され、Host が生成されて取り付けが済む
- **THEN** コマンドの `Execute` が 1 回呼ばれる

#### Scenario: Host を作り直すたびに合図が来る
- **GIVEN** 一度合図を受けた SettingsView
- **WHEN** Handler の切断と再接続で Host が作り直される
- **THEN** コマンドの `Execute` がもう 1 回呼ばれる

#### Scenario: 実行できないコマンドは呼ばない
- **GIVEN** `CanExecute` が偽を返すコマンドを設定した SettingsView
- **WHEN** Host が生成されて取り付けが済む
- **THEN** `Execute` は呼ばれない

#### Scenario: 合図の中で出した命令は戻した位置より後に実行される
- **GIVEN** 途中までスクロールした状態で Handler を切断した SettingsView と、合図の中で `ScrollToStart(false)` を呼ぶコマンド
- **WHEN** Handler を再接続して Host が作り直される
- **THEN** 最終位置は内容の先頭になる
