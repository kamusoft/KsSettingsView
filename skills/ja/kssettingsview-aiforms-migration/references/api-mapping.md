# API 対応表: AiForms.SettingsView から KsSettingsView へ

移行で何をしたいかで節を分けた、旧 API から新 API への対応表。どの表も 1 列目が AiForms のメンバー、2 列目が KsSettingsView のメンバーである。「提供しない」は対応するメンバーが存在しないことを表し、その場合は備考に代替手段を書いている。「まだ提供しない」は意図的な廃止ではなく後続フェーズ予定であることを表す。型を書いていない行は名前も型も変わっていない。既定値は、書かなかったコードの挙動が変わる箇所にだけ示す。

## namespace と起動時登録を変える

XAML と C# から参照するものはすべて 1 つの namespace に移り、登録は 1 呼び出しにまとまる。

| AiForms | KsSettingsView | 備考 |
|---|---|---|
| `AiForms.Settings` namespace | `KsSettingsView.Maui` | XAML: `clr-namespace:KsSettingsView.Maui;assembly=KsSettingsView.Maui` (旧: `clr-namespace:AiForms.Settings;assembly=SettingsView`) |
| `AiForms.Maui.SettingsView` NuGet パッケージ | `KsSettingsView.Maui` NuGet パッケージ | 変わるのはパッケージ名だけで、他に足すものはない |
| `MauiAppBuilder.UseSettingsView(bool)` | `MauiAppBuilder.AddKsSettingsView()` | 引数の `bool` はリーク回避策の有効化だったが、その仕組み自体が無くなった |
| `IMauiHandlersCollection.AddSettingsViewHandler()` | 提供しない | 登録すべき Handler は 1 件だけで、`AddKsSettingsView()` が行う |
| Cell 種別ごとの Handler 登録 | 提供しない | Cell は Native の行へ変換されるデータであり、Handler を持つ View ではない |

## 画面の骨格を組み直す

入れ物の型は名前が変わらない。注意すべき改名は Section の見出し文字列である。

| AiForms | KsSettingsView | 備考 |
|---|---|---|
| `SettingsView` | `SettingsView` | content property は `Root`。Section を直下に並べて書く |
| `SettingsRoot` | `SettingsRoot` | `ObservableCollection<Section>`。`SettingsView.Root` の既定値 |
| `Section` | `Section` | content property は `Cells` (`IList<CellBase>`)。旧 `Section` は `SectionBase : Element, IList<CellBase>` 派生で、Section 自身がコレクションだった。`section.Add(cell)` / `section[0]` / `section.Count` / `section.CollectionChanged` のような C# は `section.Cells.Add(cell)` などへ書き換える。XAML の書き方は変わらない |
| `SettingsModel` | 提供しない | `SettingsView` または `Section` の `ItemsSource` / `ItemTemplate` へ直接バインドする |
| `Section.Title` (`string`) | `Section.HeaderText` (`string?`) | AiForms では `SectionBase` の宣言 |
| `Section.TextColor` (`Color`、`Colors.Black`) | `SettingsView.HeaderTextColor` (`Color?`) | 見出しの文字色は画面全体の設定になった。Section 単位の上書きは無い |
| `Section.FooterText` | `Section.FooterText` | |
| `Section.HeaderView` / `FooterView` | `Section.HeaderView` / `FooterView` (`View?`) | 両方設定されている間は View が優先され、View を null に戻すとテキストへ戻る |
| `Section.HeaderHeight` (`double`, -1) | `Section.HeaderHeight` (`double?`) | null は platform 既定に委ねる |
| `Section.IsVisible` | `Section.IsVisible` | |
| `Section.FooterVisible` | `Section.IsFooterVisible` | Header 側の対は `Section.IsHeaderVisible` |
| `Section.UseDragSort` | まだ提供しない | ドラッグによる並べ替えはロードマップ上の後続フェーズで、この版には無い |
| `Section` / `SettingsRoot` / `SettingsView` が持つツリー変更イベント群 (`SectionCollectionChanged` / `SectionPropertyChanged` / `CellPropertyChanged` / `CollectionChanged` / `ModelChanged`) | 提供しない | ツリーの変更を描画側へ伝えるための経路だった。監視は自前のコレクションと Cell のプロパティで行う |
| `Section.MoveCellWithoutNotify()` ほかの `*WithoutNotify` メソッド群 | 提供しない | ドラッグ並べ替えを支えるための API だった |
| `CellBase.Section` | 提供しない | Cell は親 Section への参照を持たない |
| `CellBase.Reload()` | 提供しない | プロパティ変更はそのまま画面に届く。強制再描画の呼び出しは無い |
| `CellBase.SetEnabledAppearance(bool)` | `CellBase.IsEnabled` | 見た目はプロパティに追従する |
| `CellBase.Tapped` (全 Cell が公開する public イベント) | `CommandCell` / `ButtonCell` / `CustomCell` の `Tapped` イベントのみ | 範囲が狭まった。発火は `Command` より先。`LabelCell` や `SwitchCell` などで購読していた場合は、その行を `CommandCell` または `CustomCell` に置き換える |
| `CellBase.OnTapped()` (internal) | 提供しない | `CellBase` を継承して呼び出す・override していた場合にのみ関わる。タップは `Command` か `Tapped` で受ける |

Section と Cell は logical tree に載らないため、`{Binding}` は解決するが `x:Reference` と `DynamicResource` は届かない。Header / Footer の View と `CustomCell.Content` だけは例外で、logical tree に接続され所有者の `BindingContext` を継承する。

## 全 Cell 共通のフィールドを読み替える

`CellBase` の共通 22 プロパティはすべて残っている。系統的に変わったのは既定値の表し方で、AiForms が「画面既定にフォールバックする」意味に `-1.0` や `KnownColor.Default` を使っていたところが、nullable 型の `null` になった。

| AiForms `CellBase` | KsSettingsView `CellBase` | 備考 |
|---|---|---|
| `Title` (`string`) | `Title` (`string`) | non-nullable。既定は空文字列 |
| `TitleColor` (`Color`) | `TitleColor` (`Color?`) | null で画面既定を継承 |
| `TitleFontSize` (`double`, -1) | `TitleFontSize` (`double?`) | -1 ではなく null |
| `TitleFontFamily` | `TitleFontFamily` | |
| `TitleFontAttributes` (`FontAttributes?`) | `TitleFontAttributes` (`FontAttributes?`) | |
| `Description` | `Description` | |
| `DescriptionColor` (`Color`) | `DescriptionColor` (`Color?`) | |
| `DescriptionFontSize` (`double`, -1) | `DescriptionFontSize` (`double?`) | |
| `DescriptionFontFamily` | `DescriptionFontFamily` | |
| `DescriptionFontAttributes` | `DescriptionFontAttributes` | |
| `HintText` | `HintText` | |
| `HintTextColor` (`Color`) | `HintTextColor` (`Color?`) | |
| `HintFontSize` (`double`, -1) | `HintFontSize` (`double?`) | |
| `HintFontFamily` | `HintFontFamily` | |
| `HintFontAttributes` | `HintFontAttributes` | |
| `BackgroundColor` (`Color`) | `BackgroundColor` (`Color?`) | |
| `IconSource` (`ImageSource`) | `IconSource` (`ImageSource?`) | MAUI 標準の image source service 経由で非同期に解決される点は同じ |
| `IconSize` (`Size`) | `IconSize` (`double?`) | 正方形の一辺を表す 1 つの数値 |
| `IconRadius` (`double`, -1) | `IconRadius` (`double?`) | |
| `IsVisible` | `IsVisible` | |
| `Height` (`double`, -1) | `Height` (`double?`) | |
| `IsEnabled` | `IsEnabled` | |

AiForms で `LabelCell` (および `EntryCell`) に宣言されていた `ValueTextColor` / `ValueTextFontSize` / `ValueTextFontFamily` / `ValueTextFontAttributes` は `CellBase` へ移った。値文字列を表示するすべての Cell に効く。

## 読み取り専用の値を表示する (LabelCell)

| AiForms | KsSettingsView | 備考 |
|---|---|---|
| `LabelCell.ValueText` | `LabelCell.ValueText` | `ValueText` は継承ではなく Cell 種別ごとの宣言になった。`CommandCell` と `PickerCell` は以前から使えるものを持っていた。`NumberPickerCell` / `TimePickerCell` / `DatePickerCell` は継承しつつ `private new` で隠していたので、あらためて公開されたことになる。`ButtonCell` / `SwitchCell` / `CheckboxCell` / `SimpleCheckCell` / `RadioCell` は新たに得た。`ValueText` を持たない唯一の Cell が `CustomCell` である |
| `LabelCell.ValueTextColor` と値文字列のフォント系プロパティ | `CellBase.ValueTextColor` と同名のフォント系プロパティ | 基底型へ移動した |
| `LabelCell.IgnoreUseDescriptionAsValue` | 提供しない | `UseDescriptionAsValue` 自体が無くなったので除外指定も要らない。`Description` と `ValueText` をそれぞれ設定する |

## 行から操作を起こす (CommandCell / ButtonCell)

| AiForms | KsSettingsView | 備考 |
|---|---|---|
| `CommandCell.Command` / `CommandParameter` | `CommandCell.Command` / `CommandParameter` | 実効有効状態は `IsEnabled && Command.CanExecute(CommandParameter)` で `CanExecuteChanged` に追従する。発火順は `Tapped` → `Command` |
| `CommandCell.HideArrowIndicator` (`bool`) | `CommandCell.HideArrow` (`bool`) | |
| `CommandCell.KeepSelectedUntilBack` | 提供しない | 選択ハイライトは platform 既定に従う |
| `ButtonCell.TitleAlignment` (`TextAlignment`、`Center`) | `ButtonCell.TitleAlignment` (`TextAlignment?`) | null で platform 既定。`ValueText` の無い行で効く。値文字列があるとタイトルには必要な幅しか残らない |
| `ButtonCell.Command` / `CommandParameter` | `ButtonCell.Command` / `CommandParameter` | |
| `SettingsView.ShowArrowIndicatorForAndroid` | 提供しない | 矢印の挙動は両 platform で同じ。行ごとに消すなら `HideArrow` |

AiForms は `ButtonCell` で `Description` とそのフォント系プロパティを `private new` で隠していた。KsSettingsView も同じ理由で `ButtonCell` の `Description` は受け付けたうえで黙って無視する。違うのは `HintText` で、こちらは `ButtonCell` でも表示される。

## 値を切り替える (SwitchCell / CheckboxCell / SimpleCheckCell)

| AiForms | KsSettingsView | 備考 |
|---|---|---|
| `SwitchCell.On` | `SwitchCell.On` | 従来どおり既定で双方向 |
| `SwitchCell.AccentColor` (`Color`) | `SwitchCell.AccentColor` (`Color?`) | |
| `CheckboxCell.Checked` | `CheckboxCell.Checked` | 従来どおり既定で双方向 |
| `CheckboxCell.AccentColor` (`Color`) | `CheckboxCell.AccentColor` (`Color?`) | |
| `SimpleCheckCell.Checked` (単方向) | `SimpleCheckCell.Checked` (双方向) | 既定の binding mode が変わった。単方向を明示した XAML はそのまま動き、素の `{Binding}` は書き戻すようになる |
| `SimpleCheckCell.Value` (`object`) | 提供しない | 値そのものは ViewModel 側で持つ。右側の表示文字列は `ValueText` が受け持つ |
| `SimpleCheckCell.AccentColor` (`Color`) | `SimpleCheckCell.AccentColor` (`Color?`) | |

## グループから 1 つ選ぶ (RadioCell)

選択値は名前が同じまま形が変わった。旧 API では `Section` に 1 度だけ付ける添付プロパティだったが、新 API ではグループの全メンバーが持つ通常のプロパティになっている。Section が暗黙に表していたグループの所属は、Cell ごとの文字列で明示する。

| AiForms | KsSettingsView | 備考 |
|---|---|---|
| `RadioCell.SelectedValue` (添付・`object`・双方向・`Section` などの親に付ける) | `RadioCell.SelectedValue` (`string`・双方向・各 Cell に付ける) | 同名だが形が違う。旧アクセサは `RadioCell.GetSelectedValue` / `SetSelectedValue` で、新 API に添付プロパティは無い。行を選ぶと同じ `GroupId` の全 Cell に書き込まれる |
| (Cell が置かれた Section が暗黙に表していた) | `RadioCell.GroupId` (`string`) | グループの識別子。異なる Section の行が同じ値を共有してもよい |
| `RadioCell.Value` (`object`) | `RadioCell.Value` (`string`) | 文字列のみ。enum や id は文字列へ写像する |
| `RadioCell.AccentColor` (`Color`) | `RadioCell.AccentColor` (`Color?`) | |

移行前 (AiForms):

```xml
<sv:Section sv:RadioCell.SelectedValue="{Binding SelectedTheme}">
  <sv:RadioCell Title="Light" Value="light" />
  <sv:RadioCell Title="Dark" Value="dark" />
</sv:Section>
```

移行後 (KsSettingsView):

```xml
<ks:Section>
  <ks:RadioCell Title="Light" GroupId="theme" Value="light" SelectedValue="{Binding SelectedTheme}" />
  <ks:RadioCell Title="Dark" GroupId="theme" Value="dark" SelectedValue="{Binding SelectedTheme}" />
</ks:Section>
```

コレクションから行を生成する場合はもう一手要る。生成された Cell の `BindingContext` は対応する item であり、Cell には `x:Reference` が届かないため、グループの選択値を item 側から辿れるようにする。item の ViewModel に、所有者側の値を読み書きするプロパティを持たせ、それを `SelectedValue` にバインドする。

```xml
<ks:Section ItemsSource="{Binding ThemeOptions}">
  <ks:Section.ItemTemplate>
    <DataTemplate>
      <ks:RadioCell Title="{Binding Label}"
                    GroupId="theme"
                    Value="{Binding Id}"
                    SelectedValue="{Binding Owner.SelectedTheme}" />
    </DataTemplate>
  </ks:Section.ItemTemplate>
</ks:Section>
```

## テキストを入力する (EntryCell)

| AiForms | KsSettingsView | 備考 |
|---|---|---|
| `EntryCell.ValueText` | `EntryCell.ValueText` (`string`) | 既定で双方向。non-nullable |
| `EntryCell.MaxLength` (`int`, -1) | `EntryCell.MaxLength` (`int?`) | 旧は -1 が無制限、新は null が無制限 |
| `EntryCell.Keyboard` | `EntryCell.Keyboard` (`Keyboard?`) | `Microsoft.Maui.Keyboard` のまま |
| `EntryCell.Placeholder` | `EntryCell.Placeholder` | |
| `EntryCell.PlaceholderColor` | `EntryCell.PlaceholderColor` (`Color?`) | この値 → `SettingsView.CellPlaceholderColor` → OS 既定の順で解決する。OS 既定はダークモードに自動追従する |
| `EntryCell.TextAlignment` (`TextAlignment`、`End`) | `EntryCell.TextAlignment` (`TextAlignment?`) | 旧の既定は `End`。新は null で platform 既定 |
| `EntryCell.AccentColor` (`Color`) | `EntryCell.AccentColor` (`Color?`) | |
| `EntryCell.IsPassword` | `EntryCell.IsPassword` | |
| `EntryCell.Completed` (public イベント) と `CompletedCommand` | 提供しない | どちらも無くなったので、Command だけでなくイベントハンドラーも削除する。値が出ていく経路は `ValueText` の双方向バインドのみで、その裏のプロパティ setter で受ける |
| `EntryCell.SendCompleted()` | 提供しない | `Completed` の発火と `CompletedCommand` の実行を行うメソッドだった |
| `EntryCell.SetFocus()` | 提供しない | MAUI 側からフォーカスを操作する経路は無い。行をタップしたときにフォーカスが移る |
| `EntryCell.ShowDoneButtonOnIOS` (`bool`) | 提供しない | iOS のキーボード上のアクセサリは platform 既定に従う |
| `EntryCell.ValueTextColor` と値文字列のフォント系プロパティ | `CellBase.ValueTextColor` と同名のフォント系プロパティ | |

## リストから選ぶ (PickerCell)

形はそのまま残っている: 型なしの object のリスト、文字列で名指しする表示用プロパティ、双方向の `SelectedItem`。変わったのはその下側で、選択の正が index になり、候補は設定時に snapshot され、複数選択の既定が反転した。

| AiForms | KsSettingsView | 備考 |
|---|---|---|
| `PickerCell.ItemsSource` (`IList`) | `PickerCell.ItemsSource` (`IList?`) | 任意の object のまま。null 要素は `ArgumentException` で拒否される。許すと `SelectedItem == null` が「未選択」と区別できなくなるため。設定時に要素と表示文字列が写し取られ、コレクションの in-place 変更は観測されない。更新はリストの差し替えで行う |
| `PickerCell.DisplayMember` (プロパティ名) | `PickerCell.DisplayMember` (`string?`) | 考え方は同じ: 要素の実行時型が持つ public instance の引数なし readable プロパティをリフレクションで解決する。ドット区切りのパスは非対応。未指定・未解決は `ToString()` へフォールバックする。trimming 時は、この文字列でしか参照されないプロパティを利用者側で保全しないと `ToString()` フォールバックに落ちる |
| `PickerCell.SubDisplayMember` | `PickerCell.SubDisplayMember` (`string?`) | 選択面の候補行の副表示。null・未解決の名前・値が null・空文字列はいずれも副表示なしになる。行の `ValueText` には含まれない |
| `PickerCell.SelectedItem` (`object`、双方向) | `PickerCell.SelectedItem` (`object?`、双方向) | 双方向のまま残ったが導出になった: 単一選択の正は `SelectedIndex` (`int?`、双方向)。`SelectedItem` の設定は値等価で最初に一致した要素へ逆引きされ、候補に無い値は未選択になる。`ItemsSource` 到着前の設定値は捨てずに保持され、候補が届いた時点で解決されるため、XAML の属性順で初期選択は失われない |
| `PickerCell.SelectedItems` (`IList`、双方向) | `PickerCell.SelectedItems` (`IList?`、双方向) | `SelectedIndices` (`IList<int>?`、双方向。昇順・重複除去) からの導出。候補に無い要素は落ち、重複は 1 つの index に畳まれ、null は選択なしを意味する。設定した並びがそのまま返るとは限らない |
| `PickerCell.SelectionMode` (`Microsoft.Maui.Controls.SelectionMode`、`Multiple`) | `PickerCell.SelectionMode` (`PickerSelectionMode`、`Single`) | **既定が反転している**。`SelectionMode` を書いていなかった行は単一選択になる。旧側の型は MAUI 標準のもので、その `None` に対応先は無い。`Single` と `Multiple` は同名で引き継がれる |
| `PickerCell.MaxSelectedNumber` | `PickerCell.MaxSelectedNumber` | 0 が制限なしなのも同じ |
| `PickerCell.PageTitle` | `PickerCell.PageTitle` (`string?`) | |
| `PickerCell.AccentColor` (`Color`) | `PickerCell.AccentColor` (`Color?`) | |
| `PickerCell.SelectedCommand` | `PickerCell.SelectedCommand` (`ICommand?`・単方向・null) | 同名で復元された。発火するのは Native の選択確定通知のみ: コードから選択値のプロパティを設定しても、cancel や非確定の dismiss でも実行されない。実行は選択値の書き戻しと相互導出の完了後なので、Command 内から新しい選択値を観測できる。引数は AiForms と同じく、単一選択の確定で `SelectedItem`、複数選択の確定で `SelectedItems`。同じ選択を確定し直しても実行される。`CanExecute` は確認せず `Execute` を直接呼ぶ (AiForms 互換)。`CommandParameter` は無い |
| `PickerCell.SelectedItemsOrderKey` | 提供しない | バインド前に `ItemsSource` を並べ替える |
| `PickerCell.UseNaturalSort` | 提供しない | 同じく呼び出し側で並べ替える。これを支えていた public な `NaturalComparer` / `NaturalSortOrder` / `NaturalComparerOptions` も引き継いでいない |
| `PickerCell.UseAutoValueText` | 提供しない | `ValueText` が null の間は現在の選択が表示される。明示設定すればそちらが優先される |
| `PickerCell.UsePickToClose` (`bool`) | 提供しない | 選択面を閉じる条件は platform 自身の規則に従う |
| `PickerCell.Padding` (`Thickness`) | 提供しない | 選択面の余白は Native のレイアウトに従う |
| `PickerCell.ShowCommand` (`Command`、get のみ) | 提供しない | コードから選択面を開くための Command だった。行をタップすると開く |

移行前 (AiForms):

```xml
<sv:PickerCell Title="Country"
               ItemsSource="{Binding Countries}"
               DisplayMember="Name"
               SelectedItem="{Binding SelectedCountry}" />
```

移行後 (KsSettingsView):

```xml
<ks:PickerCell Title="Country"
               ItemsSource="{Binding Countries}"
               DisplayMember="Name"
               SelectedItem="{Binding SelectedCountry}" />
```

変わるのは namespace の接頭辞だけである。挙動の注意が 1 つ: ユーザーが行を選んだとき ViewModel に書き戻されるのは候補 snapshot 側の要素で、かつて設定したインスタンスと値等価ではあるが同一インスタンスとは限らない。

## TextPickerCell を置き換える

`TextPickerCell` は存在しない。`Single` モードの `PickerCell` が同じ役目を果たす。素の文字列リストは、その `ItemsSource` に入れる最も単純な形にすぎない。

| AiForms `TextPickerCell` | KsSettingsView | 備考 |
|---|---|---|
| `Items` (`IList`) | `PickerCell.ItemsSource` (`IList?`) | 同じ文字列リストをそのままバインドできる。null 要素は拒否される |
| `SelectedItem` (`object`、双方向) | `PickerCell.SelectedItem` (`object?`、双方向) または `PickerCell.SelectedIndex` (`int?`、双方向) | 正は index。ViewModel が持っている方をバインドする |
| `PageTitle` / `PickerTitle` | `PickerCell.PageTitle` | タイトルのプロパティは 1 つになった |
| `AccentColor` (`Color`) | `PickerCell.AccentColor` (`Color?`) | |
| `SelectedCommand` | `PickerCell.SelectedCommand` (`ICommand?`・単方向) | 利用者が選択を確定したときに発火する。`Single` モードでの引数は `SelectedItem` |
| `IsCircularPicker` | 提供しない | 選択面は循環しない |

## 数値・時刻・日付を選ぶ

| AiForms | KsSettingsView | 備考 |
|---|---|---|
| `NumberPickerCell.Number` (`int?`、null) | `NumberPickerCell.Number` (`int`、0) | 双方向なのは両者同じだが nullable ではなくなった。`int?` の ViewModel プロパティはそのままではバインドできない。「未設定」を何で表すかを決めて初期値を入れる |
| `NumberPickerCell.Min` (`int`、0) / `Max` (`int`、9999) | `NumberPickerCell.Min` (`int`、0) / `Max` (`int`、100) | **`Max` の既定が変わった**。`Max` を書いていなかった行は上限が 9999 から 100 になる |
| `NumberPickerCell.Unit` (`string`、空文字列) | `NumberPickerCell.Unit` (`string`) | 値に添える単位文字列。変更なし |
| `NumberPickerCell.PickerTitle` | `NumberPickerCell.PickerTitle` (`string?`) | |
| `NumberPickerCell.SelectedCommand` | 提供しない | `Number` の双方向バインドの裏の setter で受ける |
| (新規) | `NumberPickerCell.Step` (`int`、1) | 選べる数値の刻み幅 |
| `TimePickerCell.Time` (`TimeSpan`) | `TimePickerCell.Time` (`TimeSpan`) | 既定で双方向 |
| `TimePickerCell.Format` | `TimePickerCell.Format` (`string?`) | 表示専用: 行に見せる値の書式であり、選択面には影響しない |
| (時制は端末設定が決めていた) | `TimePickerCell.Is24Hour` (`bool`、true) | 選択面の 12/24 時間制を決める唯一の値。`Format` も端末の地域・24時間表示設定も関与しない。**既定は 24 時間制**なので、端末設定に追従していた行で 12 時間制の選択を出すには `Is24Hour="False"` を明示する |
| `TimePickerCell.PickerTitle` | `TimePickerCell.PickerTitle` (`string?`) | |
| `DatePickerCell.Date` (`DateTime?`、null) | `DatePickerCell.Date` (`DateTime`、1970-01-01) | 双方向なのは両者同じだが nullable ではなくなった。`DateTime?` の ViewModel プロパティはそのままではバインドできない。未設定の行に見せる日付を決めて初期値を入れる。意味を持つのは日付部分のみ |
| `DatePickerCell.MinimumDate` (`DateTime`、1900-01-01) / `MaximumDate` (`DateTime`、2100-12-31) | `DatePickerCell.MinimumDate` / `MaximumDate` (`DateTime?`) | null が無制限を表し、固定値だった 2 つの番兵日付を置き換える |
| `DatePickerCell.Format` | `DatePickerCell.Format` (`string?`) | |
| `DatePickerCell.TodayText` | `DatePickerCell.TodayText` (`string?`) | |
| `DatePickerCell.InitialDate` (`DateTime`) | 提供しない | `Date` が null の間の初期表示を与えるプロパティで、新 API では null になり得ない。`Date` 自体を初期化する |
| `DatePickerCell.IsAndroidSpinnerStyle` (`bool`、Android のみ) | `DatePickerCell.UIStyle` (`DatePickerUIStyle?`、両 platform) | `Wheels` が旧 spinner、`Calendar` がカレンダー面、null が platform 既定。Android の `Calendar` は Material date picker 固有の挙動 (テキスト入力モードへの切替) を伴うが、ホスト Activity の型・テーマへの要求はない |
| `DatePickerCell.AndroidButtonColor` (`Color`) | `DatePickerCell.AndroidButtonColor` (`Color?`) | |

## 独自の View を行に置く (CustomCell)

| AiForms | KsSettingsView | 備考 |
|---|---|---|
| `CustomCell.Content` (`View`) | `CustomCell.Content` (`View?`) | 従来どおり content property なので、XAML では Cell の直下に View を直書きする |
| `CustomCell.ShowArrowIndicator` | `CustomCell.ShowArrowIndicator` | |
| `CustomCell.Command` / `CommandParameter` | `CustomCell.Command` / `CommandParameter` | これらも `Tapped` も持たない行はタップ動作そのものを持たないため、content 内のコントロールの操作を妨げない |
| `CustomCell.IsSelectable` | 提供しない | タップ可否は `Command` または `Tapped` の有無で決まる |
| `CustomCell.IsMeasureOnce` | 提供しない | 行高さは content に追従し、表示中のサイズ変化も追う |
| `CustomCell.UseFullSize` | 提供しない | content 領域はもともと Disclosure Indicator を除く行の全域 |
| `CustomCell.LongCommand` | 提供しない | 長押しの hook は無い。引数は `SendLongCommand()` が渡す `BindingContext` であり、`LongCommandParameter` というメンバーは旧 API にも無い |
| (AiForms では `CommandCell` / `LabelCell` から継承) | `CustomCell` に `ValueText` は無い | 旧 `CustomCell` は `CommandCell` 派生で `ValueText` を持っていたが、新 `CustomCell` は `CellBase` の直接の派生で持たない |

`CustomCell` では、`CellBase` から継承する `Title` / `Description` / `HintText` / `IconSource` とテキスト系のスタイルプロパティは受け付けたうえで黙って無視される。効くのは `Content` / `Command` / `CommandParameter` / `Tapped` / `ShowArrowIndicator` と、行単位の `IsEnabled` / `IsVisible` / `BackgroundColor` / `Height` である。Native SDK にある独自 Cell 型の登録機構は MAUI では公開していない。再利用する行は `CustomCell` の派生クラスかファクトリメソッドとして組み立てる。

## 画面全体のスタイルを移す

`Cell*` の既定値は 1 対 1 で名前が残っている。Cell 側と同様、既定値を表す番兵が nullable 型になった。

| AiForms `SettingsView` | KsSettingsView `SettingsView` | 備考 |
|---|---|---|
| `BackgroundColor` | `BackgroundColor` | `VisualElement` の標準プロパティ |
| `SeparatorColor` (`Color`) | `SeparatorColor` (`Color?`) | |
| `SelectedColor` (`Color`) | `SelectedColor` (`Color?`) | |
| `CellTitleColor` / `CellTitleFontSize` / `CellTitleFontFamily` / `CellTitleFontAttributes` | 同名・nullable | 旧の `Cell*FontAttributes` は non-nullable な `FontAttributes` だった。新は `FontAttributes?` で、null が継承を表す |
| `CellDescriptionColor` / `CellDescriptionFontSize` / `CellDescriptionFontFamily` / `CellDescriptionFontAttributes` | 同名・nullable | |
| `CellValueTextColor` / `CellValueTextFontSize` / `CellValueTextFontFamily` / `CellValueTextFontAttributes` | 同名・nullable | |
| `CellHintTextColor` / `CellHintFontSize` / `CellHintFontFamily` / `CellHintFontAttributes` | 同名・nullable | |
| (新規) | `CellPlaceholderColor` (`Color?`) | Entry の placeholder 色の画面全体既定。行単位の `EntryCell.PlaceholderColor` が優先し、null は OS 既定へ抜ける |
| `CellBackgroundColor` (`Color`) | `CellBackgroundColor` (`Color?`) | |
| `CellAccentColor` (`Color`) | `CellAccentColor` (`Color?`) | |
| `CellIconSize` (`Size`) | `CellIconSize` (`double?`) | Cell 側と同じく 1 つの数値 |
| `CellIconRadius` (`double`) | `CellIconRadius` (`double?`) | |
| `RowHeight` (`int`, -1) | `RowHeight` (`int?`) | 旧は -1 が自動、新は null が自動 |
| `HasUnevenRows` (`bool`) | `HasUnevenRows` (`bool?`) | |
| `ShowSectionTopBottomBorder` (`bool`、true、Android のみ) | `ListStyle` (`SettingsViewStyle`) と `SectionMargin` / `SectionCornerRadius` / `SectionBorderWidth` / `SectionBorderColor` | Section の装飾は list の style の一部になり、両 platform で効く。`Classic` は平坦なグループ list、`Modern` は Section を内側に寄せた箱として描き、4 プロパティで形を整える |
| `SettingsView.ClearCache()` (public static) | 提供しない | 描画側のアイコンキャッシュを空にするメソッドだった。アイコンは MAUI の image source service 経由で解決され、ライブラリ側に消すべきキャッシュは無い |
| (新規) | `DisabledTextColor` (`Color?`) | 無効な行のテキスト色 |
| (新規) | `ScrollIndicatorVisible` (`bool?`) | |

`SectionMargin` は `Thickness?` だが、`Left` / `Right` は leading / trailing として読まれ、RTL の左右解決は Native 側に委ねられる。`Classic` では上下成分だけが効く。

## Header / Footer の設定を移す

| AiForms `SettingsView` | KsSettingsView | 備考 |
|---|---|---|
| `HeaderTextColor` / `HeaderFontSize` / `HeaderFontFamily` / `HeaderFontAttributes` | 同名・nullable | `HeaderFontAttributes` は non-nullable な `FontAttributes` だったが `FontAttributes?` になった |
| `HeaderBackgroundColor` (`Color`) | `HeaderBackgroundColor` (`Color?`) | |
| `HeaderHeight` (`double`, -1) | `HeaderHeight` (`double?`) | Section 単位の上書きは `Section.HeaderHeight` |
| `HeaderPadding` (`Thickness`) | 提供しない | 余白を自分で決めたい場合は `HeaderView` を使う |
| `HeaderTextVerticalAlign` (`LayoutAlignment`) | 提供しない | platform 自身の見出し配置に従う |
| `FooterTextColor` / `FooterFontSize` / `FooterFontFamily` / `FooterFontAttributes` | 同名・nullable | `FooterFontAttributes` も同じ nullability の変化 |
| `FooterBackgroundColor` (`Color`) | `FooterBackgroundColor` (`Color?`) | |
| `FooterPadding` (`Thickness`) | 提供しない | `FooterView` を使う |
| (新規) | `RootHeaderText` / `RootFooterText` | 最初の Section の上、最後の Section の下に置くテキスト |
| (新規) | `RootHeaderView` / `RootFooterView` (`View?`) | 同じ 2 箇所に任意の View を置く。テキストと併設された間は View が優先される |

## コレクションから行を生成する

| AiForms | KsSettingsView | 備考 |
|---|---|---|
| `SettingsView.ItemsSource` (`IEnumerable`) / `ItemTemplate` / `TemplateStartIndex` | 同名 (`IEnumerable?` / `DataTemplate?` / `int`) | `SettingsView` 直下のテンプレートは Section を生成する |
| `Section.ItemsSource` (`IList`) / `ItemTemplate` / `TemplateStartIndex` | 同名。ただし `ItemsSource` は `IEnumerable?` | `Section` 配下は Cell を生成する。受け取る型が広がったので、既にバインドしている `IList` はそのまま動く |
| `ItemTemplate` への `DataTemplateSelector` | 利用できる | 行の生成直前に解決される。null・入れ子の selector・テンプレート化できない型を返すと `InvalidOperationException` |

observable なソースの Add / Remove / Replace / Move はミラーされる。Reset と null 化はテンプレート生成分だけを取り除き、手で書いた Section と Cell は残す。

## Handler と PropertyMapper のカスタマイズを削除する

AiForms は Cell 種別ごとに Handler を公開しており、そこが描画をカスタマイズする継ぎ目だった。KsSettingsView は Cell のデータから Native 側が行を描くため、この層は残らず、必要でもない。

| AiForms | KsSettingsView | 備考 |
|---|---|---|
| `SettingsViewHandler` とその platform 別 partial | `SettingsViewHandler` (public、`KsSettingsView.Maui.Handlers`) | `AddKsSettingsView()` が登録する。担うのは Native Host の生成と解放だけで、設定ツリーの反映は `KsSettingsView.Maui` 内部の変換経路が受け持つため、Cell 描画のカスタマイズ点にはならない。`Mapper` と `SettingsViewHandler(IPropertyMapper?)` コンストラクタで差し替えられるのは View 共通の対応付けであり、Cell 単位ではない |
| `CellBaseHandler<TCell, TNativeCell>` | 提供しない | |
| `LabelCellBaseHandler` / `EntryCellBaseHandler` と Cell ごとの Handler | 提供しない | |
| `BasePropertyMapper` と Cell ごとの `PropertyMapper` エントリ | 提供しない | Cell へプロパティを足せるのは、対応する状態を Native の行が既に持っている場合に限る |
| `MapXxx` 内の `handler.IsDisconnect` ガード | 提供しない | 守るべき Cell 単位の mapper が存在しない |
| `UpdateXxx()` の PlatformView 拡張メソッド | 提供しない | |

これらで描画をカスタマイズしていた場合は、`CustomCell` (上記) として組み直すか、組み込み Cell の不足として https://github.com/kamusoft/KsSettingsView/issues に起票する。

## メモリリーク回避策を削除する

| AiForms | KsSettingsView | 備考 |
|---|---|---|
| `UseSettingsView(true)` | 提供しない | `AddKsSettingsView()` はオプションを取らない |
| `HandlerCleanUpHelper` | 提供しない | |
| 強制 disconnect のための `Page.Unloaded` / `Page.NavigatedFrom` の hook | 提供しない | 削除する |
| `SettingsViewConfiguration.ShouldAutoDisconnect` | 提供しない | クラスもメンバーも internal だったので利用者のコードには現れない。ここへ入る値は上の `UseSettingsView(bool)` の引数だった |

ページを離れると Native Host は解放されるが、Cell とその状態、埋め込んだ View は生き続け、戻ったときに現在状態から画面が復元される。Cell から `SettingsView` へ向かう参照はすべて weak なので、ViewModel が Cell を保持し続けても画面の回収は妨げられない。

## 代替のないメンバー

検索で見つかるようにここへ集めた。理由と、代わりにできることを併記する。「まだ」と書いたものは意図的な廃止ではなく後続フェーズ予定である。

| AiForms | 理由 | 代わりにできること |
|---|---|---|
| `SettingsView.ItemDroppedCommand`、`ItemDropped` イベントとその `DropEventArgs`、`Section.UseDragSort` | ドラッグによる並べ替えはまだ提供していない | 並べ替えは設定 list の外で提供する。提供までその画面だけ旧ライブラリに残す判断もあり得る |
| `SettingsView.ScrollToTop` / `ScrollToBottom` | スクロール制御はまだ公開していない | - |
| `SettingsView.VisibleContentHeight` | 内容の高さを返す経路が無い | 大きさがレイアウト側で決まる配置にする |
| `SettingsView.UseDescriptionAsValue` | Description と値表示は常に別物 | 必要な行に `ValueText` を明示的に設定する |
| `SettingsView.ClearCache()` | 消すべきライブラリ側のアイコンキャッシュが無い | - |
| `SettingsView.Model` / `ModelChanged` と `SettingsModel` | 描画側へ渡すモデル層が無くなった | `ItemsSource` / `ItemTemplate` へバインドする |
| `LabelCell.IgnoreUseDescriptionAsValue` | 上記に伴う | - |
| `CommandCell` / `ButtonCell` / `CustomCell` 以外の `CellBase.Tapped` | タップを起こすのはこの 3 型のみになった | その行を `CommandCell` または `CustomCell` に置き換える |
| `CellBase.Section` / `Reload()` / `SetEnabledAppearance()` | Cell が描画側を動かす経路が無くなった | 見た目の無効化は `IsEnabled` が担う |
| `CommandCell.KeepSelectedUntilBack` | 選択ハイライトは platform に従う | - |
| `CustomCell.LongCommand` | 長押しの hook が無い | content の View 側でジェスチャーを扱う |
| `CustomCell.IsMeasureOnce` / `UseFullSize` / `IsSelectable` | 高さ・content 領域・タップ可否は Native 側が決める | - |
| `EntryCell.Completed` / `CompletedCommand` / `SendCompleted()` / `SetFocus()` / `ShowDoneButtonOnIOS` | EntryCell の節を参照 | `ValueText` の双方向バインドの裏の setter で受ける |
| `PickerCell.SelectedItemsOrderKey` / `UseNaturalSort` / `UseAutoValueText` / `UsePickToClose` / `Padding` / `ShowCommand` | PickerCell の節を参照 | - |
| `NaturalComparer` / `NaturalSortOrder` / `NaturalComparerOptions` | `UseNaturalSort` を支えていた public な並べ替え補助型は引き継いでいない | 表示文字列を自分で並べ替える。自然順が要るなら自前の comparer を用意する |
| `TextPickerCell` と `IsCircularPicker` | 型ごと廃止 | `Single` モードの `PickerCell` を使う |
| `SimpleCheckCell.Value` | Cell 側では保持しない | ViewModel 側で持つ |
| `Section.TextColor` | Section 単位の見出し色は提供しない | 画面全体の `SettingsView.HeaderTextColor` を使う |
| `CellBase.IsLoading` / `IsAnimationPlaying` / `UpdateIsLoading()`、`SettingsView.OnCollectionChanged()` / `OnSectionCollectionChanged()`、`SettingsModel` の `GetCell()` ほかの問い合わせメソッド群 | AiForms が public にしていた描画側の内部配線 | - |
| `Section.HeaderPadding` / `FooterPadding`、`HeaderTextVerticalAlign` | Header / Footer のレイアウトは Native 側が決める | `HeaderView` / `FooterView` を使う |
| 独自 Cell 型の登録 | MAUI ではまだ公開していない | その行を `CustomCell` として組み立てる |
| Mac Catalyst ターゲット | 対象は iOS と Android のみ | - |

## platform の要件を確認する

| 項目 | AiForms | KsSettingsView |
|---|---|---|
| ターゲットフレームワーク | net9.0-ios, net9.0-android, net9.0-maccatalyst | net10.0-ios, net10.0-android |
| .NET SDK | 9.0.314 | 10.0.300 |
| Microsoft.Maui.Controls | 9.0.120 | 10.0.70 |
| iOS | 14.2 | 16.0 |
| Android | API 27 | API 29 |
| Android のホストテーマ | 任意 | 任意 — ライブラリが自前の Material3 テーマを同梱し、その中で UI を描く |
| 配置 | 任意のレイアウト | 大きさが決まる配置 (ページ直下・Grid の `*` 行・明示サイズ) |

Android では行・Header・選択面はホストテーマから視覚的に隔離されている: ライブラリは同梱の Material3 (DayNight) テーマでそれらをラップするため、ホストテーマの色 (dynamic color を含む) では変わらず、ホスト Activity の型・テーマへの要求もない。AiForms がホストテーマで描いていた画面は既定の見た目が変わり得るので、調整は `SettingsView` のプロパティと Cell 単位の上書きで行う。ライト / ダークは端末の夜間モードとアプリ自身の uiMode 制御で決まり、ホストテーマの親宣言では決まらない。埋め込む View (`CustomCell.Content`・Header / Footer の View) は従来どおりホストテーマで解決される。

`SettingsView` とその配下 (Section・Cell) への操作はすべて UI スレッドから行う (ライブラリ側で marshal しない)。

バインドする型は変わらない。iOS / Android は画像・時刻・日付をそれぞれ Native 独自の値型で表すが、それらは MAUI 側には現れない。バインドするのは AiForms のときと同じく `ImageSource` / `TimeSpan` / `DateTime` である。
