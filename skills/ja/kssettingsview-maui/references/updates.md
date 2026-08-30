# 表示中の画面の更新

表示中の設定画面を変える、ユーザーの操作を ViewModel へ戻す、データから行を生成する、ためのレシピ。XAML の断片は [SKILL.md](../SKILL.md) の最小動作コードにある `ks` 名前空間宣言を前提とし、C# の断片は `using KsSettingsView.Maui;` と、ページ内に `Settings` という名前の `SettingsView` があることを前提とする。

## ユーザーが変えた値を受け取る

ユーザー操作で Native の行から書き戻されるプロパティは下の表のとおりで、いずれも既定が TwoWay なので普通にバインドするだけでよい。`PickerCell.SelectedItem` と `SelectedItems` も既定 TwoWay だが、これは書き戻しではなく導出 — `SelectedIndex` / `SelectedIndices` と `ItemsSource` に対して相互に同期されるので、index ではなく項目そのもので扱いたいときにバインドする。

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

これ以外のプロパティは既定が OneWay。書き戻しがユーザーの確定操作によるものだと知りたいときは、`PickerCell` に限り `SelectedCommand` がある ([cells.md](cells.md))。

```xml
<ks:SwitchCell Title="Push notifications" On="{Binding NotificationsEnabled}" />
```

## 表示中に行を足す・外す

`Section.Cells` は既定で observable なコレクションなので、行の追加・削除はそのまま表示に出る。

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

## 行の表示内容を変える

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

## コレクションから行を生成する

Section の `ItemsSource` をバインドして `ItemTemplate` を与える。生成された Cell の `BindingContext` は対応する item になり、observable な items なら行が追従する。

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

## 生成した行と手書きの行を混ぜる

XAML に書いた行はそのまま残り、生成分をどこから差し込むかは `TemplateStartIndex` が決める。`ItemsSource` を外すと生成分だけが取り除かれる。

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

`SettingsView` も同じ 3 プロパティを持ち、そちらでは行ではなく Section が生成される。

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

## ページを離れて戻っても画面を保つ

ページを離れても、`SettingsView` に渡した設定ツリー — Section と Cell、その値、Header / Footer の View — はそのまま保持される。ページに戻ると、保持された内容がそのまま表示される。離れている間に加えた変更も反映されるので、自前で保存・復元する処理は要らない。したがって、再訪のたびにツリーを作り直してはいけない。作り直すと、生きている Section と Cell を捨てることになり、ユーザーがそこで変更した値も一緒に失われる。

## 更新にかかる決まり

- ツリーの操作は UI スレッドから行う。ライブラリ側でスレッドの marshal は行わない。
- `Section` / `CellBase` / Header・Footer・`CustomCell.Content` に置く View は、同時に 1 箇所にしか置けない。同じインスタンスを 2 箇所へ置くと `InvalidOperationException` になり、検査は反映前に行われるので画面が中途半端に更新されることはない。復旧は `Root` の組み直しで行う。
- observable でないコレクション (素の `List<T>`) は接続時点の内容が描かれるだけで、以後の編集は表示に出ない。
