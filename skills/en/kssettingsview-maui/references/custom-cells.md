# Custom cells

Recipes for rows the built-in cells cannot express. `CustomCell` shows an arbitrary MAUI view as one row in the list; the content fills the row except for the disclosure indicator, and none of the shared row slots (title, description, icon) are drawn. XAML fragments assume the `ks` namespace declaration from the minimal example in [SKILL.md](../SKILL.md).

## Put a view into a row of the list

`Content` is the content property, so the view is written directly inside the cell.

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

## Make the row react to a tap

`Command`, `CommandParameter`, and the `Tapped` event behave exactly as they do on `CommandCell`, and `ShowArrowIndicator` adds the same disclosure indicator. A row with neither a command nor a `Tapped` handler has no tap behavior at all, so controls inside the content keep working.

```xml
<ks:CustomCell ShowArrowIndicator="True" Command="{Binding OpenDetailCommand}">
  <Label Text="Advanced settings" Padding="16,14" />
</ks:CustomCell>
```

When a control inside the content consumes the tap, the row command does not also fire.

## Keep controls inside the row usable

Leave the row without a command and the content owns every gesture. The value comes back through the control's own events or bindings.

```xml
<ks:CustomCell>
  <Grid ColumnDefinitions="Auto,*,Auto" ColumnSpacing="12" Padding="16,10">
    <Label Text="Brightness" VerticalOptions="Center" />
    <Slider Grid.Column="1" Minimum="0" Maximum="100" Value="{Binding Brightness}" />
    <Label Grid.Column="2" Text="{Binding Brightness, StringFormat='{0:F0}'}" VerticalOptions="Center" />
  </Grid>
</ks:CustomCell>
```

`IsEnabled="False"` suppresses both the row tap and the controls inside the content, and dims the whole content.

## Package a row as a reusable cell type

Derive from `CustomCell`, build the content in the constructor, and expose the parts callers set as bindable properties. In XAML it is placed like any other cell and needs no registration. Declare the namespace that holds it on the page root next to `ks` - `xmlns:local="clr-namespace:MyApp.Cells"` for a type in the same assembly as the page, or `xmlns:local="clr-namespace:MyApp.Cells;assembly=MyApp.Cells"` for one in another assembly.

```csharp
using KsSettingsView.Maui;
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

## Generate custom rows from a collection

A `CustomCell` works inside a `DataTemplate` like any other cell. Each generated row gets its own view instance with the item as `BindingContext`.

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

## Let the row height follow the content

The row is sized by the content, including while the screen is on display, so an expander or a wrapping label changes the row height on its own. Nothing has to be remeasured by hand.

## Update what the row shows

Changes inside the same view instance - a bound value, a label text, a child added - reach the screen without touching `Content`. The row is only rebuilt when `Content` is set to a different instance.

```csharp
cell.Content = BuildRow(newState);
```

## What does not apply to a custom cell

`Title`, `Description`, `HintText`, `IconSource`, and the text style properties inherited from `CellBase` do not affect the row. Setting them is ignored silently rather than throwing, so one shared style can be applied to a mixed set of cells. What does apply is the row itself - `IsEnabled`, `IsVisible`, `BackgroundColor`, `Height` - and what `CustomCell` adds of its own: `Content`, `Command`, `CommandParameter`, the `Tapped` event, and `ShowArrowIndicator`.

A single view instance belongs to one place: using it as the `Content` of two cells, or as both a content and a header or footer view, throws `InvalidOperationException`. Registering a cell type of your own with its own renderer is not offered in MAUI - a `CustomCell` subclass is the reusable unit.
