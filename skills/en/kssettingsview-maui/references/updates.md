# Updating the screen while it is shown

Recipes for changing a settings screen that is already on display, for getting user edits back into a view model, and for generating rows from data. XAML fragments assume the `ks` namespace declaration from the minimal example in [SKILL.md](../SKILL.md); C# snippets assume `using KsSettingsView.Maui;` and a `SettingsView` named `Settings` in the page.

## Receive what the user changed

The table below lists the properties written back from the native row when the user operates it, and each of them is two-way by default, so a plain binding is enough. `PickerCell.SelectedItem` and `SelectedItems` are two-way as well, but they are derived rather than written back: they are kept in step with `SelectedIndex` / `SelectedIndices` and `ItemsSource`, so binding them is a way of working in items instead of indices.

| Cell | Property |
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
| `PickerCell` (derived) | `SelectedItem`, `SelectedItems` |

Every other property binds one-way by default. When you need to know that a write-back came from the user confirming a selection, `PickerCell` alone offers `SelectedCommand` ([cells.md](cells.md)).

```xml
<ks:SwitchCell Title="Push notifications" On="{Binding NotificationsEnabled}" />
```

## Add or remove a row after display

`Section.Cells` is an observable collection by default, so adding and removing rows shows up immediately.

```csharp
Section section = Settings.Root[0];

section.Cells.Add(new LabelCell { Title = "Cache", ValueText = "0 MB" });
section.Cells.Insert(0, new LabelCell { Title = "Version", ValueText = "1.0.0" });
section.Cells.RemoveAt(section.Cells.Count - 1);
```

Moving an element and replacing one in place reach the screen just as directly, and clearing the collection rebuilds the section.

## Add or remove a section after display

`SettingsView.Root` behaves the same way.

```csharp
Section storage = new() { HeaderText = "Storage" };
storage.Cells.Add(new LabelCell { Title = "Used", ValueText = "12.4 GB" });

Settings.Root.Add(storage);
Settings.Root.Remove(storage);
```

## Rebuild the whole screen at once

Assigning a new collection to `Root` replaces everything. Assign a `SettingsRoot` (or any other observable list) if you want to keep editing it afterwards; a plain `List<Section>` is drawn once and never observed again.

```csharp
SettingsRoot root = [];
root.Add(new Section { HeaderText = "General" });

Settings.Root = root;
```

## Change what a row shows

Set the property on the cell you already handed over. Content changes made in the same UI cycle reach the screen together as one update.

```csharp
LabelCell version = (LabelCell)Settings.Root[0].Cells[0];

version.ValueText = "1.0.1";
version.IsEnabled = false;
```

## Show and hide parts of the screen

`IsVisible` on a cell or a section removes it from the screen while keeping its content and its bindings alive. `IsHeaderVisible` and `IsFooterVisible` hide a section header or footer without clearing its text - they cannot make an empty header appear.

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

## Generate rows from a collection

Bind `ItemsSource` on a section and give it an `ItemTemplate`. Each generated cell gets its item as `BindingContext`, and an observable source keeps the rows in sync.

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

## Mix generated rows with hand-written ones

Rows written in XAML stay where they are; `TemplateStartIndex` decides where the generated block is inserted among them. Clearing `ItemsSource` removes only the generated rows.

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

## Generate whole sections from a collection

`SettingsView` carries the same three properties, and there they generate sections instead of rows.

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

## Pick a different template per item

`ItemTemplate` also accepts a `DataTemplateSelector`, which is resolved just before each item is materialized. Returning null, another selector, or something that is not a usable template throws `InvalidOperationException`.

```csharp
public class CellTemplateSelector : DataTemplateSelector
{
    public DataTemplate? LabelTemplate { get; set; }

    public DataTemplate? SwitchTemplate { get; set; }

    protected override DataTemplate OnSelectTemplate(object item, BindableObject container)
        => item is ToggleItem ? SwitchTemplate! : LabelTemplate!;
}
```

## Keep the screen across page visits

Leaving the page keeps the settings tree you handed to the `SettingsView` - the sections and cells, their values, and the header and footer views - exactly as it was. Coming back to the page shows that kept content as it is. Changes applied while the page was away are shown too, so there is nothing to save and restore by hand. So do not rebuild the tree on every visit: rebuilding throws away the live sections and cells, and the values the user changed in them go with them.

## Rules the updates follow

- Change the tree from the UI thread. The library does not marshal calls for you.
- A `Section`, a `CellBase`, or a view used as a header, footer, or `CustomCell.Content` belongs to one place at a time. Placing the same instance twice throws `InvalidOperationException`, and the check runs before anything is applied, so the visible screen never ends up half updated. Recovery is to rebuild `Root`.
- A collection that is not observable (a plain `List<T>`) is drawn once at the moment it is connected; later edits to it are not shown.
