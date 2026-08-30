# maui-cells デルタスペック

`KsSettingsView.Maui` (facade 層) の Cell 公開面の拡張。コンテナ形状・lifecycle・バッチ配信・UI スレッド契約は phase-2 で確立した既存挙動 (コードとテストが正) に依拠し、ここでは本変更で追加される観察可能な挙動のみを規定する。

## ADDED Requirements

### Requirement: 基本 Cell 6種の公開

facade は `CommandCell` / `ButtonCell` / `SwitchCell` / `CheckboxCell` / `RadioCell` / `SimpleCheckCell` を `CellBase` 派生として公開しなければならない (SHALL)。各 Cell は native の対応 Cell と同じ状態フィールドを AiForms 互換命名の BindableProperty で公開し (`SwitchCell.On`、`CheckboxCell.Checked`、`SimpleCheckCell.Checked`、`RadioCell.Value` / `SelectedValue` グループ対応、`CommandCell.HideArrow` 等)、`ButtonCell` は `Description` を自型から公開せず、基底 (`CellBase`) 経由で設定された場合も輸送・表示してはならない (SHALL NOT — 継承により基底プロパティ自体は除去できないため、契約は「輸送・表示しない」で定める)。プロパティ変更は既存のバッチ配信契約で表示に反映されなければならない (SHALL)。

#### Scenario: XAML 直置きと表示反映

- **GIVEN** XAML で Section 配下に基本 Cell 6種を並べたページ
- **WHEN** ページを表示する
- **THEN** 各 Cell が native の対応 Cell として記述どおりの内容で表示される

#### Scenario: プロパティ変更の反映

- **GIVEN** 表示中の `SwitchCell`
- **WHEN** コードで `On` を反転する
- **THEN** native のスイッチ表示が追随する

### Requirement: 入力 Cell 5種の公開

facade は `EntryCell` / `PickerCell` / `NumberPickerCell` / `TimePickerCell` / `DatePickerCell` を `CellBase` 派生として公開しなければならない (SHALL)。状態フィールドは native の対応 Cell と同じ意味論で公開する: `EntryCell` は `ValueText` / `Placeholder` / `Keyboard` (`Microsoft.Maui.Keyboard`) / `IsPassword` / `TextAlignment` / `MaxLength`、`NumberPickerCell` は `Min` / `Max` / `Step` / `Number` / `Unit` / `PickerTitle`、`TimePickerCell` は `Time` (`TimeSpan`) / `Format` / `PickerTitle`、`DatePickerCell` は `Date` (`DateTime`) / `MinimumDate` / `MaximumDate` / `Format` / `TodayText` / `PickerTitle`。`EntryCell` に `ValueText` 以外の入力値プロパティを追加してはならない (SHALL NOT)。

#### Scenario: 入力 Cell の表示反映

- **GIVEN** XAML で Section 配下に入力 Cell 5種を構成したページ
- **WHEN** ページを表示する
- **THEN** 各 Cell が native の対応 Cell として記述どおりの内容 (現在値・タイトル・補助表示) で表示される

#### Scenario: 選択面の挙動は native 契約に従う

- **GIVEN** 表示中の `PickerCell` / `NumberPickerCell` / `DatePickerCell`
- **WHEN** 行をタップして選択面を開き、確定または非確定で閉じる
- **THEN** 確定のみが値変更として facade に通知され、非確定 dismiss では通知されない (native の選択面契約が透過する)

### Requirement: タップ通知 (CommandCell / ButtonCell)

`CommandCell` / `ButtonCell` の実効有効状態は `IsEnabled` かつ `Command.CanExecute(CommandParameter)` (Command 未設定時は `IsEnabled` のみ) でなければならない (SHALL)。実効有効な Cell のタップは `Tapped` イベント発火 → `Command.Execute(CommandParameter)` の順で通知しなければならない (SHALL)。実効無効な Cell はタップ通知・Command 実行とも発生してはならない (SHALL NOT)。`Command.CanExecuteChanged` の発火で実効有効状態は表示に追随しなければならない (SHALL)。タップ通知を `CellBase` の共通イベントとして公開してはならない (SHALL NOT)。

#### Scenario: タップで Command 実行

- **GIVEN** `Command` を設定した表示中の `CommandCell`
- **WHEN** ユーザーが行をタップする
- **THEN** `Tapped` が発火し `Command.Execute(CommandParameter)` が呼ばれる

#### Scenario: CanExecute = false で無効化

- **GIVEN** `CanExecute` が false を返す `Command` を設定した `ButtonCell`
- **WHEN** ページを表示しタップを試みる
- **THEN** Cell は無効表示で、`Tapped` も `Execute` も発生しない

#### Scenario: CanExecuteChanged で復帰

- **GIVEN** 無効状態 (CanExecute = false) で表示中の `ButtonCell`
- **WHEN** CanExecute が true に変わり `CanExecuteChanged` が発火する
- **THEN** Cell は有効表示に戻りタップが機能する

#### Scenario: Command 差し替え後は旧 Command の通知を無視

- **GIVEN** `Command` A を設定済みの `CommandCell` を `Command` B へ差し替えた
- **WHEN** A の `CanExecuteChanged` が発火し、その後行をタップする
- **THEN** A の通知は実効有効状態に影響せず、タップは B に対して実行される

### Requirement: 双方向バインドの書き戻し

ユーザー操作による native の値変更は、対応する facade プロパティ (`SwitchCell.On` / `CheckboxCell.Checked` / `SimpleCheckCell.Checked` / `RadioCell.SelectedValue` / `EntryCell.ValueText` / `PickerCell.SelectedIndex`・`SelectedIndices` / `NumberPickerCell.Number` / `TimePickerCell.Time` / `DatePickerCell.Date`) へ書き戻され、`PropertyChanged` が発火して TwoWay バインドの ViewModel へ伝播しなければならない (SHALL)。この10プロパティの BindableProperty は既定 binding mode を `TwoWay` としなければならない (SHALL)。書き戻しは Store への値コミットを兼ねるため、抑止・間引き (debounce) を行ってはならない (SHALL NOT)。radio の選択通知は、通知元 Cell と同一 `GroupId` を持つ全 `RadioCell` の `SelectedValue` へ適用されなければならない (SHALL)。

#### Scenario: スイッチ操作が ViewModel へ届く

- **GIVEN** ViewModel のプロパティと TwoWay バインドされた表示中の `SwitchCell`
- **WHEN** ユーザーがスイッチをトグルする
- **THEN** `On` が更新され ViewModel のプロパティも新値になる

#### Scenario: radio 選択がグループ全体へ反映される

- **GIVEN** 同一 `GroupId` の `RadioCell` 3つが表示され、1つ目が選択中
- **WHEN** ユーザーが3つ目をタップする
- **THEN** 3つの `SelectedValue` がすべて新値になり、選択表示は3つ目だけになる

#### Scenario: 再訪問で操作結果が復元される

- **GIVEN** ユーザー操作で値を変更した SettingsView がページ離脱で Handler 切断された
- **WHEN** ページ再訪問で Handler が再接続される
- **THEN** 操作後の値で表示が復元される (書き戻しが Store へコミットされている)

### Requirement: エコー抑止 (入口同値チェック)

native からの値変更通知が対象プロパティの現値と同値の場合、facade は書き戻しを行わず、`PropertyChanged` も配信も発生してはならない (SHALL NOT)。双方向の折り返し (書き戻し → 配信 → native 再描画 → 再通知) は同値チェックで必ず収束しなければならない (SHALL)。

#### Scenario: 同値通知は無視される

- **GIVEN** `On = true` の表示中の `SwitchCell`
- **WHEN** native から `isOn = true` の変更通知が届く
- **THEN** `PropertyChanged` は発火せず、native への配信も発生しない

### Requirement: PickerCell の SelectedItem 相互導出

`PickerCell.SelectedItem` は `ItemsSource` と `SelectedIndex` から導出され、`SelectedItem` への設定は `ItemsSource` 内の位置で `SelectedIndex` を更新しなければならない (SHALL)。`ItemsSource` 未設定または `SelectedIndex` が範囲外のとき `SelectedItem` は null でなければならない (SHALL)。ユーザーの選択確定では `SelectedIndex` と `SelectedItem` の両方が更新されなければならない (SHALL)。

#### Scenario: SelectedItem 設定が index に反映される

- **GIVEN** `ItemsSource = ["ライト", "ダーク"]` の `PickerCell`
- **WHEN** `SelectedItem = "ダーク"` を設定する
- **THEN** `SelectedIndex` が 1 になり表示に反映される

#### Scenario: ItemsSource 未設定時は null

- **GIVEN** `ItemsSource` 未設定で `SelectedIndex = 0` の `PickerCell`
- **WHEN** `SelectedItem` を読み取る
- **THEN** null が返る

### Requirement: DatePickerUIStyle の統一 enum

`DatePickerCell.UIStyle` (`DatePickerUIStyle? { Calendar, Wheels }`、既定 null) は、`Calendar` が iOS のカレンダー形式 / Android の Material 形式、`Wheels` が iOS のホイール形式 / Android の Spinner 形式として native へ適用されなければならない (SHALL)。null のときは各 native の既定に従わなければならない (SHALL)。

Android の `Calendar` (= Material 形式) には native 既存の固有前提 (`FragmentActivity` ホスト要求等) が付随する — 前提を満たさない場合の挙動は native 契約に従い、facade は追加の保証をしない。

#### Scenario: Wheels 指定の両OS 適用

- **GIVEN** `UIStyle = Wheels` の `DatePickerCell`
- **WHEN** 行をタップする
- **THEN** iOS はホイール形式、Android は Spinner 形式の選択面が開く

### Requirement: platform 固有プロパティの無視

対象外プラットフォームでは、platform 固有プロパティ (`DatePickerCell.AndroidButtonColor` 等の接頭辞付きプロパティ) は表示・挙動に影響してはならない (SHALL NOT)。

#### Scenario: iOS での AndroidButtonColor

- **GIVEN** `AndroidButtonColor` を設定した `DatePickerCell`
- **WHEN** iOS で表示・操作する
- **THEN** 表示・挙動に影響しない

### Requirement: IconSource の実体化と反映

`CellBase.IconSource` (`ImageSource?`) に設定された画像は platform 画像へ解決され、解決完了時に該当 Cell の icon として表示に反映されなければならない (SHALL)。Handler 未接続の間に設定された `IconSource` は接続後に解決・反映されなければならない (SHALL)。`IconSource` の変更は再解決を、null 設定は icon なし表示を引き起こさなければならない (SHALL)。解決失敗時は icon なしとして安全に表示しなければならない (SHALL)。

#### Scenario: 接続前設定の反映

- **GIVEN** Handler 接続前に `IconSource` を設定した Cell
- **WHEN** ページ表示で Handler が接続される
- **THEN** 解決された画像が icon として表示される

#### Scenario: null 化で icon なし

- **GIVEN** icon 表示中の Cell
- **WHEN** `IconSource = null` を設定する
- **THEN** icon なしの表示になる

#### Scenario: 解決競合は最後の設定が勝つ

- **GIVEN** `IconSource` に画像 A を設定し解決が未完了の Cell
- **WHEN** 解決完了前に `IconSource` を画像 B へ変更し、その後 A・B の解決が任意の順で完了する
- **THEN** 最終表示は常に B であり、遅れて完了した A が表示を上書きしない

### Requirement: Theme 系プロパティの公開と適用

SettingsView は native `Theme` に対応する平置きプロパティ群 (背景色・separator 色・Cell 既定の文字色/背景色/強調色・フォント系の分割プロパティ等、対応概念がある項目のみ) を公開し、設定・変更は `setTheme` 経路で表示全体に反映されなければならない (SHALL)。native に対応概念のない項目を公開してはならない (SHALL NOT)。

#### Scenario: Theme 色の適用

- **GIVEN** SettingsView の Theme 系プロパティで強調色を設定したページ
- **WHEN** ページを表示する
- **THEN** 全 Cell の強調表示 (スイッチ・チェック・選択印等) に設定色が反映される

#### Scenario: 表示中の Theme 変更

- **GIVEN** 表示中の SettingsView
- **WHEN** Theme 系プロパティを変更する
- **THEN** 表示が新しい Theme で更新される (更新範囲は native の Theme 更新契約に従う — iOS で表示済みの Header / Footer への即時再適用は既存契約どおり保証されない)

### Requirement: Cell 単位スタイルの公開と適用

CellBase は CellStyle に対応するスタイルプロパティ (`TitleColor` / `IconSize` / `IconRadius` 等、native `CellStyle` の対応フィールド) を、対話・選択系 Cell は `AccentColor` を公開し、native の実効値解決 (Cell → CellStyle → Theme) に乗って表示に反映されなければならない (SHALL)。

#### Scenario: Cell の AccentColor 上書き

- **GIVEN** Theme の強調色があるページで `AccentColor` を個別設定した `SwitchCell`
- **WHEN** ページを表示する
- **THEN** その Cell だけ個別設定の強調色で表示される

### Requirement: Section.IsVisible

`Section.IsVisible` (既定 true) が false の Section は、header / footer / 配下の全 Cell ごと表示から除外されなければならない (SHALL)。true へ戻すと元の位置に復帰し、非表示中の内容変更も復帰後の表示に反映されなければならない (SHALL)。可視性の切替をまたいでも配下 Cell の双方向バインド (ユーザー操作の書き戻し) は機能し続けなければならない (SHALL)。

#### Scenario: 非表示と復帰

- **GIVEN** 表示中の SettingsView の Section
- **WHEN** `IsVisible = false` にし、その後 true へ戻す
- **THEN** Section は表示から除外され、復帰後は元の位置に表示される

#### Scenario: 非表示中の内容変更が復帰後に反映される

- **GIVEN** `IsVisible = false` にした Section 配下の `LabelCell`
- **WHEN** 非表示中に `ValueText` を変更し、Section を `IsVisible = true` へ戻す
- **THEN** 復帰後の表示は変更後の `ValueText` になっている

#### Scenario: 切替後も双方向バインドが機能する

- **GIVEN** `IsVisible` を false → true と切り替えた Section 配下の `SwitchCell`
- **WHEN** ユーザーがスイッチをトグルする
- **THEN** `On` への書き戻しと ViewModel への伝播が機能する

### Requirement: DataTemplateSelector の解決

`ItemTemplate` に `DataTemplateSelector` が設定された場合、facade は SettingsView 直下 (Section 生成) と Section 配下 (Cell 生成) の両階層で、item ごとに `SelectTemplate(item, container)` (container はテンプレートが設定されている BindableObject — SettingsView 直下なら SettingsView、Section 配下なら当該 Section) で実テンプレートへ解決してから実体化しなければならない (SHALL)。selector の null 返却・selector 返却・生成物の型不一致は、既存 DataTemplate 経路と同じ例外契約で扱わなければならない (SHALL)。

#### Scenario: Section 生成の出し分け

- **GIVEN** SettingsView 直下の `ItemsSource` と、item に応じて異なる Section テンプレートを返す `DataTemplateSelector`
- **WHEN** 複数の item を設定して表示する
- **THEN** 各 item が selector の返したテンプレートの Section として表示される

#### Scenario: item ごとのテンプレート出し分け

- **GIVEN** item の型・値に応じて異なる Cell 種のテンプレートを返す `DataTemplateSelector` を `ItemTemplate` に設定した Section
- **WHEN** `ItemsSource` に複数の item を設定して表示する
- **THEN** 各 item が selector の返したテンプレートの Cell 種で表示される
