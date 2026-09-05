# Cell

設定画面に Cell を置くためのレシピ。XAML の例はいずれも [SKILL.md](../SKILL.md) の最小動作コードにある `ks` 名前空間宣言を前提とする。`<ks:Section>` から始まる断片は `<ks:SettingsView>` の直下に、Cell 単体の断片は `<ks:Section>` の中に貼る — `SettingsView` の content property である `Root` が持つのは Section であって Cell ではない。バインドはページの `BindingContext` に対して解決されるので、参照しているプロパティは ViewModel 側に用意する。

## Cell を Section にまとめる

Cell は必ず Section の中に置く。Section は Header / Footer のテキストを任意で持てる。

```xml
<ks:Section HeaderText="Account" FooterText="Signing out keeps local data.">
  <ks:LabelCell Title="Signed in as" ValueText="taro" />
</ks:Section>
<ks:Section>
  <ks:LabelCell Title="App information" />
</ks:Section>
```

## 読み取り専用の値を表示する

`LabelCell` はテキストを表示するだけで、タップには反応しない。

```xml
<ks:LabelCell Title="Storage" ValueText="256 GB" />
```

## Cell から処理を実行する・画面へ遷移する

`CommandCell` は Disclosure Indicator を表示し、タップで Command を実行する。`HideArrow="True"` で矢印を消せる。

```xml
<ks:CommandCell Title="License"
                ValueText="MIT"
                Command="{Binding ShowLicenseCommand}"
                CommandParameter="license" />
```

Cell がタップに反応するのは `IsEnabled` が true かつ `Command.CanExecute(CommandParameter)` が true のときで、`CanExecuteChanged` にも追従する。発火順は `Tapped` イベントが先で、その後に Command が実行される。

通知だけ受け取りたいときは `Tapped` を購読する。属性にはページの code-behind のメソッド名を書く。同じイベントは `ButtonCell` と `CustomCell` も持つ。

```xml
<ks:CommandCell Title="Open the log" Tapped="OnOpenLogTapped" />
```

```csharp
private void OnOpenLogTapped(object? sender, EventArgs e)
{
    _logCount++;
}
```

## ボタンの Cell を置く

`ButtonCell` は Disclosure Indicator を表示しない。`TitleAlignment` が視覚に出るのは `ValueText` を持たない Cell に限られる — 値テキストがある Cell では title に配る余白が残らないため。

```xml
<ks:ButtonCell Title="Sign out"
               TitleColor="#CC3333"
               TitleAlignment="Center"
               Command="{Binding SignOutCommand}" />
```

## 二値を切り替える

`SwitchCell.On` は既定が TwoWay なので、ユーザーがスイッチを操作するとバインド先へそのまま書き戻される。C# からは `KsSettingsView.SwitchCell` と完全修飾するか using alias を使う — 型名だけでは MAUI の同名型と衝突する ([SKILL.md](../SKILL.md))。

```xml
<ks:SwitchCell Title="Push notifications"
               Description="Delivery may be delayed on metered networks."
               On="{Binding NotificationsEnabled}" />
```

## 独立したチェック項目を置く

`CheckboxCell` はチェックボックスを、`SimpleCheckCell` は Cell 末尾の簡易チェック印を描く。どちらも独立した二値を持つもので、`RadioCell` の代わりにはならない。

```xml
<ks:CheckboxCell Title="Agree to the terms" Checked="{Binding AgreedTerms}" />
<ks:SimpleCheckCell Title="Send crash reports" Checked="{Binding SendReports}" />
```

## グループから Cell 1 つだけ選ばせる

同じ `GroupId` を持つ Cell が 1 つの選択グループになる。各 Cell は自分の `Value` を持ち、グループ内の全 Cell が同じプロパティへ `SelectedValue` をバインドする。

```xml
<ks:Section HeaderText="Theme">
  <ks:RadioCell Title="Light" GroupId="theme" Value="light" SelectedValue="{Binding Theme}" />
  <ks:RadioCell Title="Dark" GroupId="theme" Value="dark" SelectedValue="{Binding Theme}" />
</ks:Section>
```

## Cell の中でテキストを編集する

`EntryCell` は Cell そのものが入力欄になるため、`ValueText` はここでは表示専用の枠ではない。`ValueText` が編集対象の文字列そのもので、既定が TwoWay。C# からは `KsSettingsView.EntryCell` と完全修飾するか using alias を使う — 型名だけでは MAUI の同名型と衝突する ([SKILL.md](../SKILL.md))。

```xml
<ks:EntryCell Title="Name"
              ValueText="{Binding UserName}"
              Placeholder="Taro Yamada"
              Keyboard="Default"
              MaxLength="20" />
<ks:EntryCell Title="Password"
              ValueText="{Binding Password}"
              Placeholder="8 characters or more"
              IsPassword="True" />
```

`Keyboard` には MAUI 標準のキーボード (`Default` / `Plain` / `Text` / `Chat` / `Url` / `Email` / `Numeric` / `Telephone`) を指定する。`TextAlignment` は入力テキストの揃え位置で、未指定 (null) なら Native 既定の末尾寄せになる。値変更イベントは公開していない — 値が戻る経路は TwoWay バインドだけ。`PlaceholderColor` は Cell ごとのプレースホルダ文字色で、未指定なら `SettingsView.CellPlaceholderColor`、それも未指定なら OS 既定色に落ち、ダークモードにも自動で追従する。

## リストから 1 項目を選ばせる

`PickerCell` をタップすると選択面が開く。選ばせる数は `SelectionMode` (`PickerSelectionMode` 型) で決め、単一選択の `Single` が既定、複数選択が `Multiple`。`Single` では `SelectedIndex` が正で、`SelectedItem` は `SelectedIndex` と `ItemsSource` から導出される。

`ItemsSource` に置けるのは文字列に限らず任意の型のオブジェクト。null 要素は `ArgumentException` で拒否される。コレクションは代入時に 1 度だけ読み取られる — 同じコレクションの中身をいじっても観測されないので、候補を変えるときは新しいコレクションを代入する。

```xml
<ks:PickerCell Title="Theme"
               ItemsSource="{Binding Themes}"
               SelectedIndex="{Binding ThemeIndex}"
               PageTitle="Select a theme" />
```

## リストから複数項目を選ばせる

モードを `Multiple` にして `SelectedIndices` をバインドする。`MaxSelectedNumber` が選択数の上限で、`0` は上限なし。

```xml
<ks:PickerCell Title="Notification types"
               SelectionMode="Multiple"
               ItemsSource="{Binding NotificationTypes}"
               SelectedIndices="{Binding NotificationSelection}"
               MaxSelectedNumber="3"
               PageTitle="Select notification types" />
```

## オブジェクトの候補を読みやすい文字列で表示する

`DisplayMember` に指定した名前のプロパティの値が、Cell と選択面の項目テキストになる。未指定 (または名前が解決できない) なら項目の `ToString()` が表示される。`SubDisplayMember` は選択面の候補行に限って 2 行目の副表示を足す。どちらもリフレクションで public インスタンスプロパティを名前解決するので、trimming ではそれらのプロパティを保全しておく。旧 `DisplayFormatter` デリゲートは廃止済み — `DisplayMember` を使う。

```xml
<ks:PickerCell Title="Plan"
               ItemsSource="{Binding Plans}"
               DisplayMember="Name"
               SubDisplayMember="Detail"
               SelectedItem="{Binding SelectedPlan}"
               PageTitle="Select a plan" />
```

## index ではなく項目そのもので扱う

`SelectedItem` (単一選択) と `SelectedItems` (複数選択) は既定 TwoWay で、`SelectedIndex` / `SelectedIndices` と `ItemsSource` に対して相互に同期される — 正はあくまで index 側。候補に無い項目を設定すると未選択になる (複数選択では見つからない要素だけが落とされる)。逆引きは値等価で、最初に一致した位置に解決される。`ItemsSource` が届く前にバインドされた項目は捨てられずに保持され、候補が届いた時点で解決されるので、XAML の属性順・バインドの適用順は問わない。

```xml
<ks:PickerCell Title="Theme"
               ItemsSource="{Binding Themes}"
               SelectedItem="{Binding Theme}" />
```

## 選択の確定を Command で受け取る

`SelectedCommand` は、ユーザーが選択面で選択を確定した瞬間を通知する。値の TwoWay バインドだけではユーザーの確定操作を初期化・プログラムからの更新と区別できないときに使う。実行されるのはユーザーが選択を確定したときだけで、公開プロパティ (`SelectedIndex` など) を直接設定しても、キャンセル・確定なしの dismiss でも実行されない。同じ選択をそのまま確定し直しても実行される — 値の変化ではなく確定操作の通知だから。

実行は選択値の書き戻しの後なので、Command の中からは確定後の新しい選択値が見える。引数は確定の種類で決まり、単一選択の確定では `SelectedItem`、複数選択の確定では `SelectedItems` が渡される。`CanExecute` は確認されず、`CommandParameter` プロパティは無い。

```xml
<ks:PickerCell Title="Notification recipients"
               SelectionMode="Multiple"
               ItemsSource="{Binding Members}"
               DisplayMember="Name"
               SelectedIndices="{Binding MemberSelection}"
               SelectedCommand="{Binding MemberSelectionCompletedCommand}"
               PageTitle="Select recipients" />
```

```csharp
public ICommand MemberSelectionCompletedCommand { get; }

public SettingsViewModel()
{
    MemberSelectionCompletedCommand = new Command(parameter =>
    {
        IList members = parameter as IList ?? Array.Empty<object>();
        SaveRecipients(members);
    });
}
```

## 数値を選ばせる

`NumberPickerCell` は `Min` から `Max` まで `Step` 刻みの候補を出す。`Unit` は表示値へ付け足される。

```xml
<ks:NumberPickerCell Title="Font size"
                     Min="10"
                     Max="30"
                     Step="1"
                     Number="{Binding FontSize}"
                     Unit="px"
                     PickerTitle="Select a size" />
```

## 時刻を選ばせる

`Time` は `TimeSpan`。選択面の時制は `Is24Hour` だけで決まる — `True` (既定) なら 24 時間制、`False` なら午前/午後の列を持つ 12 時間制で、端末の 24 時間表示設定も `Format` も関与しないため、どの端末でも同じ時制で開く。`Format` が効くのは Cell の値テキストの整形だけで、Cell を描く platform の日時フォーマッタ (iOS は `DateFormatter`、Android は `DateTimeFormatter`) へそのまま渡されるので、.NET の書式指定子ではなくそれらが解釈するパターンを書き、`Is24Hour` との整合は自分で保つ (組み合わせの検証は行われない)。

```xml
<ks:TimePickerCell Title="Alarm"
                   Time="{Binding AlarmTime}"
                   Format="h:mm a"
                   Is24Hour="False"
                   PickerTitle="Alarm time" />
```

Android の選択面はホストによらず時・分ホイールのボトムシートで、時刻のキーボード入力モードはない。

## 日付を選ばせる

`Date` は `DateTime` で、意味を持つのは日付部分。`Format` の渡り先は `TimePickerCell` と同じ platform のフォーマッタ。`UIStyle` (`DatePickerUIStyle?` 型) は選択面の形式で `Calendar` か `Wheels`、未指定なら platform の既定に従う。`TodayText` は「今日」へ飛ぶ操作のオプトイン。

```xml
<ks:DatePickerCell Title="Birthday"
                   Date="{Binding Birthday}"
                   MinimumDate="{Binding BirthdayMinimum}"
                   MaximumDate="{Binding BirthdayMaximum}"
                   Format="yyyy/MM/dd"
                   UIStyle="Wheels"
                   TodayText="Today"
                   PickerTitle="Birthday" />
```

Android の `Calendar` は Material 3 のカレンダーダイアログを開く。ユーザーが切り替えられるテキスト入力モードも付いていて、ホストの Activity 型・テーマを問わず動く。`AndroidButtonColor` は Android の `Wheels` 選択面の OK / CANCEL 操作の色で、未指定なら `AccentColor` 系の解決に従う — Android 専用の指定で、他の platform では表示に影響しない。

## 選択系の Cell に共通する決まり

`PickerCell` / `NumberPickerCell` / `TimePickerCell` / `DatePickerCell` が値を書き戻すのは、ユーザーが確定したときだけ — iOS の Done、Android の OK、単一選択の `PickerCell` なら候補行のタップ。キャンセル・外側タップ・Back・シートの下スワイプは作業中の状態を破棄し、バインド先は元のまま変わらない。複数選択の `PickerCell` も同じで、確定するまで `SelectedIndices` には触れない。確定の瞬間そのものを受け取る経路は `PickerCell` だけが持つ — 上の `SelectedCommand`。

4 つとも `ValueText` を持つ。未指定なら Cell は現在の選択を自動で表示し、指定するとその文字列が代わりに表示される。

## Cell にアイコンを付ける

`IconSource` は通常の MAUI の `ImageSource` なので、ファイル名・`MauiImage` の資産・URI・埋め込みリソースがそのまま使える。画像は非同期に解決され、解決できなかった場合はアイコンなしの Cell になる。

```xml
<ks:CommandCell Title="Profile"
                Description="tanaka.taro@example.com"
                IconSource="ic_account_circle.png"
                Command="{Binding OpenProfileCommand}" />
```

## Cell に補足のテキストを添える

タイトルの下に置く `Description` を持つのは `ButtonCell` と `CustomCell` を除く全 Cell。補足用の `HintText` の除外はもっと狭く、`CustomCell` だけが持たない — `ButtonCell` は持つ。どちらも null の間は表示されない。

```xml
<ks:LabelCell Title="Storage"
              Description="Long descriptions wrap over several lines."
              HintText="Updated a moment ago"
              ValueText="256 GB" />
```

## Cell を無効化する・非表示にする

`IsEnabled="False"` は Cell を表示したまま操作を止め、無効時の文字色で描く。`IsVisible="False"` は値を model に残したまま画面から外すので、バインドは効き続け、戻すと元の位置に復帰する。

```xml
<ks:CommandCell Title="Sync now" IsEnabled="{Binding IsOnline}" />
<ks:LabelCell Title="Debug build" IsVisible="{Binding IsDebug}" />
```
