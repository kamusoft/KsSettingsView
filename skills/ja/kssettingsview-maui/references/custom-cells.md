# CustomCell

組み込み Cell では表せない内容のためのレシピ。`CustomCell` は任意の MAUI View を設定リストの 1 つの Cell として表示する。内容は Disclosure Indicator を除く Cell の全域を占め、共通 Cell レイアウトのスロット (title / description / icon) は描かれない。XAML の断片は [SKILL.md](../SKILL.md) の最小動作コードにある `ks` 名前空間宣言を前提とする。

## View を Cell として表示する

`Content` が content property なので、View は Cell の直下に書く。

```xml
<ks:Section HeaderText="Status">
  <ks:CustomCell>
    <HorizontalStackLayout Padding="16,10" Spacing="12">
      <BoxView WidthRequest="10" HeightRequest="10" CornerRadius="5" Color="#34C759" />
      <Label Text="Sync" VerticalOptions="Center" />
      <Label Text="{Binding SyncState}" TextColor="#999999" VerticalOptions="Center" />
    </HorizontalStackLayout>
  </ks:CustomCell>
</ks:Section>
```

## Cell のタップに反応させる

`Command` / `CommandParameter` と `Tapped` イベントの挙動は `CommandCell` と同じで、`ShowArrowIndicator` は同じ Disclosure Indicator を出す。Command も `Tapped` も持たない Cell はタップ動作そのものを持たないので、内容の中のコントロールの操作を妨げない。

```xml
<ks:CustomCell ShowArrowIndicator="True" Command="{Binding OpenDetailCommand}">
  <Label Text="Advanced settings" Padding="16,14" />
</ks:CustomCell>
```

内容の中の要素がタップを消費したときは、Cell の Command は発火しない。

## Cell の中のコントロールを操作させる

Command を持たせなければ、ジェスチャは内容側が受け取る。値はそのコントロール自身のイベントやバインドで戻ってくる。

```xml
<ks:CustomCell>
  <Grid ColumnDefinitions="Auto,*,Auto" ColumnSpacing="12" Padding="16,10">
    <Label Text="Brightness" VerticalOptions="Center" />
    <Slider Grid.Column="1" Minimum="0" Maximum="100" Value="{Binding Brightness}" />
    <Label Grid.Column="2" Text="{Binding Brightness, StringFormat='{0:F0}'}" VerticalOptions="Center" />
  </Grid>
</ks:CustomCell>
```

`IsEnabled="False"` は Cell のタップと内容の中の操作の両方を抑止し、内容全体を淡色化する。

## 再利用できる Cell 型にまとめる

`CustomCell` を継承し、コンストラクタで内容を組み立て、呼び出し側が設定する部分を bindable property として公開する。XAML では他の Cell と同じように置け、登録は要らない。そのクラスがある名前空間はページのルートで `ks` と並べて宣言する — ページと同じアセンブリの型なら `xmlns:local="clr-namespace:MyApp.Cells"`、別アセンブリの型なら `xmlns:local="clr-namespace:MyApp.Cells;assembly=MyApp.Cells"`。

```csharp
using KsSettingsView;
using Microsoft.Maui.Controls;

public class SliderCell : CustomCell
{
    public static readonly BindableProperty ValueProperty = BindableProperty.Create(
        nameof(Value),
        typeof(double),
        typeof(SliderCell),
        0d,
        BindingMode.TwoWay,
        propertyChanged: static (bindable, _, newValue) => ((SliderCell)bindable)._slider.Value = (double)newValue);

    private readonly Slider _slider = new() { Minimum = 0, Maximum = 100 };

    public SliderCell()
    {
        _slider.ValueChanged += (_, e) => Value = e.NewValue;
        Content = new Grid { Padding = new Thickness(16, 10), Children = { _slider } };
    }

    public double Value
    {
        get => (double)GetValue(ValueProperty);
        set => SetValue(ValueProperty, value);
    }
}
```

```xml
<ks:Section HeaderText="Display">
  <local:SliderCell Value="{Binding Brightness}" />
</ks:Section>
```

## コレクションから CustomCell を生成する

`CustomCell` も他の Cell と同じように `DataTemplate` の中で使える。生成された Cell はそれぞれ独立した View 実体を持ち、`BindingContext` は対応する item になる。

```xml
<ks:Section HeaderText="Devices" ItemsSource="{Binding Devices}">
  <ks:Section.ItemTemplate>
    <DataTemplate>
      <ks:CustomCell Command="{Binding OpenCommand}">
        <VerticalStackLayout Padding="16,10" Spacing="2">
          <Label Text="{Binding Name}" />
          <Label Text="{Binding Status}" FontSize="12" TextColor="#999999" />
        </VerticalStackLayout>
      </ks:CustomCell>
    </DataTemplate>
  </ks:Section.ItemTemplate>
</ks:Section>
```

## Cell の高さを内容に追従させる

Cell の高さは内容が決める。表示中の内容のサイズ変化にも追従するので、開閉するブロックや折り返すラベルを置いても高さは自動で変わる。手動での再計測は要らない。

## Cell の表示を更新する

同じ View インスタンスの内部の変化 — バインド値の更新、ラベル文言の変更、子要素の追加 — は `Content` に触れずに表示へ届く。Cell が作り直されるのは `Content` を別インスタンスに差し替えたときだけ。

```csharp
cell.Content = BuildRow(newState);
```

## CustomCell に効かないもの

`CellBase` から継承する `Title` / `Description` / `HintText` / `IconSource` とテキスト系のスタイルプロパティは Cell に影響しない。設定しても例外にはならず黙って無視される — 同じスタイル指定を種類の違う Cell へまとめて当てられるようにするため。効くのは Cell そのものに掛かる `IsEnabled` / `IsVisible` / `BackgroundColor` / `Height` と、`CustomCell` 固有の `Content` / `Command` / `CommandParameter` / `Tapped` イベント / `ShowArrowIndicator`。

View インスタンスは同時に 1 箇所にしか置けない。同じインスタンスを 2 つの Cell の `Content` にしたり、`Content` と Header / Footer の View に同時に使ったりすると `InvalidOperationException` になる。独自の Cell 型と描画を登録する機構は MAUI では公開していない — 再利用の単位は `CustomCell` の派生クラスになる。
