# Updating the screen while it is shown

Recipes for changing a settings screen that is already on display, for getting user edits back into a view model, for generating cells from data, and for scrolling from code. XAML fragments assume the `ks` namespace declaration from the minimal example in [SKILL.md](../SKILL.md); C# snippets assume `using KsSettingsView;` and a `SettingsView` named `Settings` in the page. Changes are made on the UI thread; the native host restores the current tree when it reconnects.

## Receive what the user changed

The table below lists the properties written back from the native cell when the user operates it, and each of them is two-way by default, so a plain binding is enough. `PickerCell.SelectedItem` and `SelectedItems` are two-way as well, but they are derived rather than written back: they are kept in step with `SelectedIndex` / `SelectedIndices` and `ItemsSource`, so binding them is a way of working in items instead of indices.

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

Every other property binds one-way by default. When you need to know that a write-back came from a user confirmation, `PickerCell` alone offers `SelectedCommand`; it runs after the selection surface has finished closing ([cells.md](cells.md)).

```xml
<ks:SwitchCell Title="Push notifications" On="{Binding NotificationsEnabled}" />
```

## Add or remove a cell after display

`Section.Cells` is an observable collection by default, so adding and removing cells shows up immediately.

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

## Change what a cell shows

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

## Generate cells from a collection

Bind `ItemsSource` on a section and give it an `ItemTemplate`. Each generated cell gets its item as `BindingContext`, and an observable source keeps the cells in sync.

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

## Mix generated cells with hand-written ones

Cells written in XAML stay where they are; `TemplateStartIndex` decides where the generated block is inserted among them. Clearing `ItemsSource` removes only the generated cells.

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

`SettingsView` carries the same three properties, and there they generate sections instead of cells.

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

## Scroll to a cell or a section from code

`SettingsView.ScrollController` holds a scroll handle of type `IScrollController`. The `SettingsView` creates the handle itself, and the property binds `OneWayToSource` by default, so binding it to a view model property is how the handle reaches the view model; assigning another value does not replace it. Give the targets an explicit ID with `CellId` on a cell and `SectionId` on a section. The IDs only name the target - they change nothing on screen.

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

The handle has four commands. `ScrollTo` brings a cell row into view, `ScrollToSection` brings a section in with its header, and `ScrollToStart` / `ScrollToEnd` go to the top and bottom of the content, root header and root footer included. `position` is the library's own `ScrollPosition` enum - `Start` (the default), `Center`, or `End` - and says where in the visible area the target lines up; `animated` defaults to `true`. MAUI's own `ScrollToPosition` is not used here, because its `MakeVisible` has no native counterpart. A target near the end of the content stops at the scroll limit instead of overshooting, and a target taller than the visible area is aligned at its start whatever position you pass.

In code-behind the same handle is available as `Settings.ScrollController`. A view model that only depends on `IScrollController` can be tested with an implementation that records the calls.

## Scroll to a generated cell or section

`target` also accepts an item of `ItemsSource`: `ScrollTo` finds the cell a section generated from that item, and `ScrollToSection` finds the section `SettingsView.ItemsSource` generated from it. An element whose explicit ID equals the target is preferred over a generated one, and among several matches the first in display order is taken. The link to the item survives moves and replacements in the source and a changed `BindingContext` on the generated cell.

```csharp
public void ShowDevice(Device device) => Scroll?.ScrollTo(device);
```

## Scroll as soon as the screen opens

A command issued before the native list has been created does nothing and is not replayed later. `ScrollControllerReadyCommand` tells you when commands start to work: it runs each time the native list is created and attached, calling `Execute(null)` when `CanExecute(null)` is true. It runs again whenever the list is recreated - for example when a page that was popped is pushed again, or when Android recreates the activity - so keep a flag if you want to scroll only on the first opening.

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

## Add an item and show the end

A command runs after the tree changes made earlier in the same UI cycle have reached the screen, so `ScrollToEnd` right after adding an item lands on the end that includes the new item. Commands issued one after another run in order, and the final position is set by the last one whose target was found.

```csharp
public ObservableCollection<string> AddedItems { get; } = [];

public void AddAndShow(string name)
{
    AddedItems.Add(name);
    Scroll?.ScrollToEnd();
}
```

## Keep the screen across page visits

Leaving the page keeps the settings tree you handed to the `SettingsView` - the sections and cells, their values, and the header and footer views - exactly as it was. Coming back to the page shows that kept content as it is, with accessory views and `CustomCell.Content` included in the first rendered screen rather than inserted later. Changes applied while the page was away are shown too, so there is nothing to save and restore by hand. The scroll position comes back as well: when the native list is recreated, it returns to the element that was at the top of the visible area, with the same offset, so rows added or removed above it in the meantime do not shift where it lands. A command issued in `ScrollControllerReadyCommand` runs after that restore and sets the final position. So do not rebuild the tree on every visit: rebuilding throws away the live sections and cells, and the values the user changed in them go with them.

## Rules the updates follow

- Change the tree and issue scroll commands from the UI thread. The library does not marshal calls for you.
- A `Section`, a `CellBase`, or a view used as a header, footer, or `CustomCell.Content` belongs to one place at a time. Placing the same instance twice throws `InvalidOperationException`: an instance another section or `SettingsView` still owns throws as you add it, and a duplicate inside one collection throws when the placement is drawn. The check runs before anything is applied, so the placement that was already there is untouched and the visible screen never ends up half updated. Recovery is to rebuild `Root`.
- A collection that is not observable (a plain `List<T>`) is drawn once at the moment it is connected; later edits to it are not shown. Joining and leaving such a collection also counts at that moment, so an element you removed from it can be placed elsewhere only after a new collection is assigned to `Root` or `Cells`.
- When a host reconnects, the views used by section headers, footers, and `CustomCell.Content` are materialized and delivered before the first screen is shown. Replacing a view instance creates a new content instance; changing the existing view through bindings keeps that instance live.
- A scroll command whose target is hidden does nothing, and so does one whose target matches no explicit ID or item; the latter writes a warning to the Debug output. Neither throws. While another page is pushed on top of the settings page, its native list stays alive, and a command issued meanwhile is carried out when you come back.
- The handle refers to its `SettingsView` weakly. A view model that keeps the handle longer than the page does not keep the `SettingsView` and its native list alive; once they are collected, commands on the handle do nothing.
