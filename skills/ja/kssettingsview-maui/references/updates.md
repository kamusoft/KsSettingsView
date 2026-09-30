# 表示中の画面の更新

表示中の設定画面を変える、ユーザーの操作を ViewModel へ戻す、データから Cell を生成する、コードからスクロールさせる、ためのレシピ。XAML の断片は [SKILL.md](../SKILL.md) の最小動作コードにある `ks` 名前空間宣言を前提とし、C# の断片は `using KsSettingsView;` と、ページ内に `Settings` という名前の `SettingsView` があることを前提とする。操作は UI スレッドから行い、Native Host が再接続すると現在のツリーから表示が復元される。

## ユーザーが変えた値を受け取る

ユーザー操作で Native の Cell から書き戻されるプロパティは下の表のとおりで、いずれも既定が TwoWay なので普通にバインドするだけでよい。`PickerCell.SelectedItem` と `SelectedItems` も既定 TwoWay だが、これは書き戻しではなく導出 — `SelectedIndex` / `SelectedIndices` と `ItemsSource` に対して相互に同期されるので、index ではなく項目そのもので扱いたいときにバインドする。

| Cell | プロパティ |
|---|---|
| `SwitchCell` | `On` |
| `CheckboxCell` | `Checked` |
| `SimpleCheckCell` | `Checked` |
| `RadioCell` | `SelectedValue` |
| `EntryCell` | `ValueText` |
| `PickerCell` | `SelectedIndex`, `SelectedIndices` |
| `NumberPickerCell` | `Number` |
| `TimePickerCell` | `Time` |
| `DatePickerCell` | `Date` |
| `PickerCell` (導出) | `SelectedItem`, `SelectedItems` |

これ以外のプロパティは既定が OneWay。書き戻しがユーザーの確定操作によるものだと知りたいときは、`PickerCell` に限り `SelectedCommand` がある。選択面が閉じ切った後に実行される ([cells.md](cells.md))。

```xml
<ks:SwitchCell Title="Push notifications" On="{Binding NotificationsEnabled}" />
```

## 表示中に Cell を足す・外す

`Section.Cells` は既定で observable なコレクションなので、Cell の追加・削除はそのまま表示に出る。

```csharp
Section section = Settings.Root[0];

section.Cells.Add(new LabelCell { Title = "Cache", ValueText = "0 MB" });
section.Cells.Insert(0, new LabelCell { Title = "Version", ValueText = "1.0.0" });
section.Cells.RemoveAt(section.Cells.Count - 1);
```

要素の移動と、その場での差し替えも同じように表示へ直接届き、コレクションをクリアすると Section が組み直される。

## 表示中に Section を足す・外す

`SettingsView.Root` も同じ振る舞いをする。

```csharp
Section storage = new() { HeaderText = "Storage" };
storage.Cells.Add(new LabelCell { Title = "Used", ValueText = "12.4 GB" });

Settings.Root.Add(storage);
Settings.Root.Remove(storage);
```

## 画面全体を組み直す

`Root` へ新しいコレクションを代入すると全体が入れ替わる。以後も編集し続けるなら `SettingsRoot` (または他の observable なリスト) を渡す。素の `List<Section>` は接続時点の内容が 1 度描かれるだけで、以後の操作は反映されない。

```csharp
SettingsRoot root = [];
root.Add(new Section { HeaderText = "General" });

Settings.Root = root;
```

## Cell の表示内容を変える

すでに渡してある Cell のプロパティを設定する。同一 UI サイクル内の内容変更は 1 回にまとまって画面へ届く。

```csharp
LabelCell version = (LabelCell)Settings.Root[0].Cells[0];

version.ValueText = "1.0.1";
version.IsEnabled = false;
```

## 画面の一部を出し入れする

Cell と Section の `IsVisible` は、内容とバインドを保ったまま表示から外す。`IsHeaderVisible` / `IsFooterVisible` は Section の Header / Footer をテキストを消さずに隠すもので、内容が無い Header をこれで出すことはできない。

```xml
<ks:Section HeaderText="Developer"
            FooterText="Only shown in debug builds."
            IsVisible="{Binding IsDebug}"
            IsHeaderVisible="{Binding ShowHeader}"
            IsFooterVisible="{Binding ShowFooter}">
  <ks:LabelCell Title="Build" ValueText="{Binding BuildNumber}" />
  <ks:LabelCell Title="Commit" IsVisible="{Binding HasCommit}" />
</ks:Section>
```

## コレクションから Cell を生成する

Section の `ItemsSource` をバインドして `ItemTemplate` を与える。生成された Cell の `BindingContext` は対応する item になり、observable な items なら Cell が追従する。

```xml
<ks:Section HeaderText="Devices" ItemsSource="{Binding Devices}">
  <ks:Section.ItemTemplate>
    <DataTemplate>
      <ks:CommandCell Title="{Binding Name}"
                      ValueText="{Binding Status}"
                      Command="{Binding OpenCommand}" />
    </DataTemplate>
  </ks:Section.ItemTemplate>
</ks:Section>
```

## 生成した Cell と手書きの Cell を混ぜる

XAML に書いた Cell はそのまま残り、生成分をどこから差し込むかは `TemplateStartIndex` が決める。`ItemsSource` を外すと生成分だけが取り除かれる。

```xml
<ks:Section HeaderText="Devices"
            ItemsSource="{Binding Devices}"
            TemplateStartIndex="1">
  <ks:LabelCell Title="Paired devices" />
  <ks:Section.ItemTemplate>
    <DataTemplate>
      <ks:LabelCell Title="{Binding Name}" />
    </DataTemplate>
  </ks:Section.ItemTemplate>
</ks:Section>
```

## コレクションから Section ごと生成する

`SettingsView` も同じ 3 プロパティを持ち、そちらでは Cell ではなく Section が生成される。

```xml
<ks:SettingsView ItemsSource="{Binding Groups}">
  <ks:SettingsView.ItemTemplate>
    <DataTemplate>
      <ks:Section HeaderText="{Binding Title}" ItemsSource="{Binding Items}">
        <ks:Section.ItemTemplate>
          <DataTemplate>
            <ks:LabelCell Title="{Binding Name}" />
          </DataTemplate>
        </ks:Section.ItemTemplate>
      </ks:Section>
    </DataTemplate>
  </ks:SettingsView.ItemTemplate>
</ks:SettingsView>
```

## item ごとにテンプレートを切り替える

`ItemTemplate` には `DataTemplateSelector` も渡せる。実体化の直前に解決され、null・別の selector・テンプレートとして使えない型を返した場合は `InvalidOperationException` になる。

```csharp
public class CellTemplateSelector : DataTemplateSelector
{
    public DataTemplate? LabelTemplate { get; set; }

    public DataTemplate? SwitchTemplate { get; set; }

    protected override DataTemplate OnSelectTemplate(object item, BindableObject container)
        => item is ToggleItem ? SwitchTemplate! : LabelTemplate!;
}
```

## コードから Cell や Section へスクロールする

`SettingsView.ScrollController` はスクロール命令のハンドル (`IScrollController` 型) を持つ。ハンドルは `SettingsView` 自身が作り、このプロパティの既定のバインド方向は `OneWayToSource` なので、ViewModel のプロパティへバインドすればハンドルが ViewModel に渡る。別の値を代入してもハンドルは差し替わらない。命令の対象には、Cell なら `CellId`、Section なら `SectionId` で明示 ID を付ける。ID は対象を指すためだけのもので、表示は何も変わらない。

```xml
<ks:SettingsView ScrollController="{Binding Scroll}">
  <ks:Section HeaderText="Notifications" SectionId="notifications">
    <ks:SwitchCell Title="Push notifications" CellId="push" />
    <ks:SwitchCell Title="Sound" CellId="sound" />
  </ks:Section>
</ks:SettingsView>
```

```csharp
public class SettingsViewModel
{
    public IScrollController? Scroll { get; set; }

    public void ShowSound() => Scroll?.ScrollTo("sound", ScrollPosition.Center);

    public void ShowNotifications() => Scroll?.ScrollToSection("notifications");

    public void BackToTop() => Scroll?.ScrollToStart(animated: false);
}
```

命令は 4 種ある。`ScrollTo` は Cell の行を、`ScrollToSection` は Section を見出しごと表示範囲へ入れ、`ScrollToStart` / `ScrollToEnd` は Root Header / Root Footer を含む内容の先頭・末尾へ送る。`position` はライブラリ独自の enum `ScrollPosition` — `Start` (既定)・`Center`・`End` — で、対象を表示範囲のどこへ合わせるかを表す。`animated` の既定は `true`。MAUI 標準の `ScrollToPosition` は、その `MakeVisible` に Native の対応が無いため使わない。内容の末尾付近の対象は行き過ぎずにスクロールの端で止まり、表示範囲より高い対象は指定した位置によらず上端で合わせる。

コードビハインドからは同じハンドルを `Settings.ScrollController` で使える。ViewModel が `IScrollController` だけに依存していれば、テストでは呼び出しを記録するだけの実装に差し替えられる。

## 生成した Cell や Section へスクロールする

`target` には `ItemsSource` の項目も渡せる。`ScrollTo` は Section がその項目から生成した Cell を、`ScrollToSection` は `SettingsView.ItemsSource` がその項目から生成した Section を探す。明示 ID が対象と等しい要素は生成物より優先され、複数当たる場合は表示順で最初のものを採る。項目との対応は、ItemsSource 内での移動・置換や、生成された Cell の `BindingContext` の差し替えの後も保たれる。

```csharp
public void ShowDevice(Device device) => Scroll?.ScrollTo(device);
```

## 画面を開いた直後にスクロールする

Native の一覧が作られる前に出した命令は何もせず、後から実行し直されることもない。命令が効くようになった時点は `ScrollControllerReadyCommand` で分かる。Native の一覧が作られて画面に取り付けられるたびに、`CanExecute(null)` が真なら `Execute(null)` が呼ばれる。一覧が作り直されるたび — たとえば Pop したページをもう一度 Push したときや、Android が Activity を作り直したとき — にも改めて呼ばれるので、最初に開いたときだけスクロールしたいなら自分でフラグを持つ。

```xml
<ks:SettingsView ScrollController="{Binding Scroll}"
                 ScrollControllerReadyCommand="{Binding ScrollReadyCommand}">
  <ks:Section HeaderText="Notifications" SectionId="notifications">
    <ks:SwitchCell Title="Push notifications" />
  </ks:Section>
</ks:SettingsView>
```

```csharp
public class SettingsViewModel
{
    private bool _scrolledOnOpen;

    public SettingsViewModel()
    {
        ScrollReadyCommand = new Command(() =>
        {
            if (_scrolledOnOpen)
            {
                return;
            }

            _scrolledOnOpen = true;
            Scroll?.ScrollToSection("notifications");
        });
    }

    public IScrollController? Scroll { get; set; }

    public ICommand ScrollReadyCommand { get; }
}
```

## 項目を足して末尾を見せる

命令は、同じ UI サイクルの中で先に行ったツリーの変更が表示に反映された後に実行される。そのため項目を足した直後に `ScrollToEnd` を呼べば、足した項目を含む末尾へ届く。続けて出した命令は呼んだ順に処理され、最終位置は対象が見つかった最後の命令のものになる。

```csharp
public ObservableCollection<string> AddedItems { get; } = [];

public void AddAndShow(string name)
{
    AddedItems.Add(name);
    Scroll?.ScrollToEnd();
}
```

## ページを離れて戻っても画面を保つ

ページを離れても、`SettingsView` に渡した設定ツリー — Section と Cell、その値、Header / Footer の View — はそのまま保持される。ページに戻ると、保持された内容がそのまま表示され、accessory View と `CustomCell.Content` も最初の表示から含まれる。離れている間に加えた変更も反映されるので、自前で保存・復元する処理は要らない。スクロール位置も戻る。Native の一覧が作り直されたときは、表示範囲の上端にあった要素が同じずれで上端に来る位置へ戻るので、離れている間にその上で行が増減しても戻り先はずれない。`ScrollControllerReadyCommand` の中で出した命令はこの復元の後に実行され、最終位置を決める。したがって、再訪のたびにツリーを作り直してはいけない。作り直すと、生きている Section と Cell を捨てることになり、ユーザーがそこで変更した値も一緒に失われる。

## 更新にかかる決まり

- ツリーの操作とスクロール命令は UI スレッドから行う。ライブラリ側でスレッドの marshal は行わない。
- `Section` / `CellBase` / Header・Footer・`CustomCell.Content` に置く View は、同時に 1 箇所にしか置けない。同じインスタンスを 2 箇所へ置くと `InvalidOperationException` になる — 他の Section / `SettingsView` が所有したままのインスタンスは追加した時点で、同じコレクションへの二重の追加は表示へ反映する時点で送出される。検査は反映前に行われるので、先に置かれていた方は動かず、画面が中途半端に更新されることもない。復旧は `Root` の組み直しで行う。
- observable でないコレクション (素の `List<T>`) は接続時点の内容が描かれるだけで、以後の編集は表示に出ない。所属の始まりと終わりもその時点で数えられるので、そこから取り除いた要素を別の場所へ置き直せるのは、`Root` / `Cells` へ新しいコレクションを代入した後になる。
- Host が再接続すると、Section の Header / Footer と `CustomCell.Content` に置いた View は最初の表示前に実体化されて届く。View インスタンスを差し替えると内容も差し替わり、既存 View のバインド値を変えると同じインスタンスのまま追従する。
- 非表示の要素への命令は何もしない。明示 ID にも項目にも当たらない対象への命令も何もせず、こちらは Debug 出力に警告を書く。どちらも例外にはならない。設定ページの上に別のページを Push している間も Native の一覧は生きており、その間に出した命令は戻ってきたときに実行される。
- ハンドルは持ち主の `SettingsView` を弱参照で持つ。ViewModel がハンドルをページより長く保持しても、`SettingsView` と Native の一覧は生かし続けない。それらが回収された後のハンドルへの命令は何もしない。
