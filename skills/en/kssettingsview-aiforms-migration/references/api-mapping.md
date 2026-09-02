# API mapping: AiForms.SettingsView to KsSettingsView

Old-to-new tables grouped by what you are migrating. In every table the first column is the AiForms member and the second is the KsSettingsView member. "Not provided" means the member has no counterpart; the notes column then gives the replacement approach. "Not provided yet" means it is planned for a later phase rather than dropped on purpose. Names given without a type are unchanged in type as well as in name; a default is shown only where it changes what unannotated code does.

## Change the namespace and the startup registration

Everything you reference from XAML or C# moves to one namespace, and registration collapses to a single call.

| AiForms | KsSettingsView | Notes |
|---|---|---|
| `AiForms.Settings` namespace | `KsSettingsView` | XAML: `clr-namespace:KsSettingsView;assembly=KsSettingsView.Maui` (was `clr-namespace:AiForms.Settings;assembly=SettingsView`). The namespace is `KsSettingsView`; the assembly and the package are `KsSettingsView.Maui` - the asymmetry is deliberate, so do not write the package ID into `using` or `clr-namespace` |
| `AiForms.Maui.SettingsView` NuGet package | `KsSettingsView.Maui` NuGet package | The package name is the whole change; the binding packages come in transitively. Not on NuGet.org yet - until it is published, use a `ProjectReference` to the facade project in a repository checkout or a feed holding a locally packed build |
| `MauiAppBuilder.UseSettingsView(bool)` | `MauiAppBuilder.AddKsSettingsView()` | The `bool` selected the leak workaround, which no longer exists |
| `IMauiHandlersCollection.AddSettingsViewHandler()` | Not provided | `AddKsSettingsView()` registers the one handler there is |
| Per-cell handler registrations | Not provided | Cells are data converted to native rows, not handler-backed views |

One thing the new namespace brings that `AiForms.Settings` did not: `KsSettingsView.SwitchCell` and `KsSettingsView.EntryCell` share their names with types in `Microsoft.Maui.Controls`. XAML is unaffected, because the `ks:` prefix names the namespace. In C# - the place where an AiForms port builds cells by hand - a file with `using KsSettingsView;` next to the implicit MAUI usings cannot resolve the bare names and the compiler reports CS0104 (ambiguous reference). Write `KsSettingsView.SwitchCell` / `KsSettingsView.EntryCell` in full, or declare a using alias such as `using SwitchCell = KsSettingsView.SwitchCell;` in that file. The kssettingsview-maui Skill covers this under "Name collisions with MAUI's own cells" in its SKILL.md.

## Rebuild the screen skeleton

The container types keep their names. The section header string is the one rename to watch for.

| AiForms | KsSettingsView | Notes |
|---|---|---|
| `SettingsView` | `SettingsView` | Content property is `Root`; sections are written directly inside it |
| `SettingsRoot` | `SettingsRoot` | `ObservableCollection<Section>`, the default value of `SettingsView.Root` |
| `Section` | `Section` | Content property is `Cells` (`IList<CellBase>`). The old `Section` derived from `SectionBase : Element, IList<CellBase>`, so it *was* the collection: C# such as `section.Add(cell)`, `section[0]`, `section.Count` and `section.CollectionChanged` becomes `section.Cells.Add(cell)` and so on. XAML is unaffected |
| `SettingsModel` | Not provided | Bind `ItemsSource` / `ItemTemplate` on `SettingsView` or `Section` directly |
| `Section.Title` (`string`) | `Section.HeaderText` (`string?`) | Declared on `SectionBase` in AiForms |
| `Section.TextColor` (`Color`, `Colors.Black`) | `SettingsView.HeaderTextColor` (`Color?`) | The header text color is a screen-wide setting now; there is no per-section override |
| `Section.FooterText` | `Section.FooterText` | |
| `Section.HeaderView` / `FooterView` | `Section.HeaderView` / `FooterView` (`View?`) | A view wins over the text while both are set; clearing the view falls back to the text |
| `Section.HeaderHeight` (`double`, -1) | `Section.HeaderHeight` (`double?`) | null defers to the platform default |
| `Section.IsVisible` | `Section.IsVisible` | |
| `Section.FooterVisible` | `Section.IsFooterVisible` | `Section.IsHeaderVisible` is the new counterpart for the header |
| `Section.UseDragSort` | Not provided yet | Reordering by drag is on the roadmap, not in this release |
| The tree-change events on `Section`, `SettingsRoot` and `SettingsView`: `SectionCollectionChanged`, `SectionPropertyChanged`, `CellPropertyChanged`, `CollectionChanged`, `ModelChanged` | Not provided | They reported tree changes to the renderer. Observe your own collections and cell properties instead |
| `Section.MoveCellWithoutNotify()` and the other `*WithoutNotify` methods | Not provided | They existed to support drag reordering |
| `CellBase.Section` | Not provided | A cell holds no back-reference to its section |
| `CellBase.Reload()` | Not provided | Property changes reach the screen on their own; there is no forced redraw |
| `CellBase.SetEnabledAppearance(bool)` | `CellBase.IsEnabled` | Appearance follows the property |
| `CellBase.Tapped` (public event on every cell) | `Tapped` event on `CommandCell`, `ButtonCell` and `CustomCell` only | Narrower than before: fires before `Command`. A `LabelCell`, `SwitchCell` or other cell whose `Tapped` you subscribed to has to become a `CommandCell` or a `CustomCell` |
| `CellBase.OnTapped()` (internal) | Not provided | Only reachable if you derived from `CellBase` and called or overrode it; raise taps through `Command` or `Tapped` |

Sections and cells are not part of the logical tree, so `{Binding}` resolves but `x:Reference` and `DynamicResource` do not reach them. Header / footer views and `CustomCell.Content` are the exception: they are connected and inherit their owner's `BindingContext`.

## Translate the fields every cell shares

The 22 shared `CellBase` properties all survive. The systematic change is the sentinel: AiForms used `-1.0` and `KnownColor.Default` to mean "fall back to the screen default", KsSettingsView uses a nullable type with `null`.

| AiForms `CellBase` | KsSettingsView `CellBase` | Notes |
|---|---|---|
| `Title` (`string`) | `Title` (`string`) | Non-nullable, defaults to the empty string |
| `TitleColor` (`Color`) | `TitleColor` (`Color?`) | null inherits the screen default |
| `TitleFontSize` (`double`, -1) | `TitleFontSize` (`double?`) | null instead of -1 |
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
| `IconSource` (`ImageSource`) | `IconSource` (`ImageSource?`) | Still resolved through the MAUI image source service, asynchronously |
| `IconSize` (`Size`) | `IconSize` (`double?`) | One number, used as the side of a square |
| `IconRadius` (`double`, -1) | `IconRadius` (`double?`) | |
| `IsVisible` | `IsVisible` | |
| `Height` (`double`, -1) | `Height` (`double?`) | |
| `IsEnabled` | `IsEnabled` | |

`ValueTextColor`, `ValueTextFontSize`, `ValueTextFontFamily` and `ValueTextFontAttributes` were declared on `LabelCell` (and again on `EntryCell`) in AiForms. They now sit on `CellBase`, so they apply to every cell that shows a value string.

## Show a read-only value (LabelCell)

| AiForms | KsSettingsView | Notes |
|---|---|---|
| `LabelCell.ValueText` | `LabelCell.ValueText` | Each cell type now declares its own `ValueText` instead of inheriting it. `CommandCell` and `PickerCell` already had a usable one; `NumberPickerCell`, `TimePickerCell` and `DatePickerCell` inherited one but hid it with `private new`, and now expose it again; `ButtonCell`, `SwitchCell`, `CheckboxCell`, `SimpleCheckCell` and `RadioCell` gain one they never had. `CustomCell` is the only cell without a `ValueText` |
| `LabelCell.ValueTextColor` and the value-text font properties | `CellBase.ValueTextColor` and the same font properties | Moved up to the base type |
| `LabelCell.IgnoreUseDescriptionAsValue` | Not provided | `UseDescriptionAsValue` is gone, so there is nothing to opt out of; set `Description` and `ValueText` separately |

## Run an action from a row (CommandCell / ButtonCell)

| AiForms | KsSettingsView | Notes |
|---|---|---|
| `CommandCell.Command` / `CommandParameter` | `CommandCell.Command` / `CommandParameter` | Effective enabled state is `IsEnabled && Command.CanExecute(CommandParameter)` and follows `CanExecuteChanged`; `Tapped` fires before `Command` |
| `CommandCell.HideArrowIndicator` (`bool`) | `CommandCell.HideArrow` (`bool`) | |
| `CommandCell.KeepSelectedUntilBack` | Not provided | The selection highlight follows the platform default |
| `ButtonCell.TitleAlignment` (`TextAlignment`, `Center`) | `ButtonCell.TitleAlignment` (`TextAlignment?`) | null takes the platform default. Visible on rows without a `ValueText`, since a value string leaves the title only the width it needs |
| `ButtonCell.Command` / `CommandParameter` | `ButtonCell.Command` / `CommandParameter` | |
| `SettingsView.ShowArrowIndicatorForAndroid` | Not provided | The indicator behaves the same on both platforms; hide it per row with `HideArrow` |

AiForms hid `Description` and its font properties on `ButtonCell` with `private new`, and KsSettingsView keeps `Description` out of that row for the same reason: it is accepted and silently ignored. `HintText` is the difference - a `ButtonCell` does show it.

## Toggle a value (SwitchCell / CheckboxCell / SimpleCheckCell)

| AiForms | KsSettingsView | Notes |
|---|---|---|
| `SwitchCell.On` | `SwitchCell.On` | Two-way by default, as before. In C# write the type as `KsSettingsView.SwitchCell` or through a using alias - the bare name collides with `Microsoft.Maui.Controls.SwitchCell` (see the namespace section) |
| `SwitchCell.AccentColor` (`Color`) | `SwitchCell.AccentColor` (`Color?`) | |
| `CheckboxCell.Checked` | `CheckboxCell.Checked` | Two-way by default, as before |
| `CheckboxCell.AccentColor` (`Color`) | `CheckboxCell.AccentColor` (`Color?`) | |
| `SimpleCheckCell.Checked` (one-way) | `SimpleCheckCell.Checked` (two-way) | The binding mode changed; a one-way binding in your XAML keeps working, a plain `{Binding}` now also writes back |
| `SimpleCheckCell.Value` (`object`) | Not provided | Keep the payload in the view model; `ValueText` covers the text shown on the right |
| `SimpleCheckCell.AccentColor` (`Color`) | `SimpleCheckCell.AccentColor` (`Color?`) | |

## Pick one option in a group (RadioCell)

The selected value keeps its name but changes shape: it was an attached property you set once on the `Section`, and it is now an ordinary property that every member of the group carries. Group membership, which the section used to imply, is stated per cell as a string.

| AiForms | KsSettingsView | Notes |
|---|---|---|
| `RadioCell.SelectedValue` (attached, `object`, two-way, set on the `Section` or another parent) | `RadioCell.SelectedValue` (`string`, two-way, set on each cell) | Same name, different shape. Accessors were `RadioCell.GetSelectedValue` / `SetSelectedValue`; there is no attached property now. Picking a row writes the value on every cell with the same `GroupId` |
| (implied by the section the cells sat in) | `RadioCell.GroupId` (`string`) | Identifies the group; rows in different sections may share one |
| `RadioCell.Value` (`object`) | `RadioCell.Value` (`string`) | Strings only; map your enum or id to a string |
| `RadioCell.AccentColor` (`Color`) | `RadioCell.AccentColor` (`Color?`) | |

Before, in AiForms:

```xml
<sv:Section sv:RadioCell.SelectedValue="{Binding SelectedTheme}">
  <sv:RadioCell Title="Light" Value="light" />
  <sv:RadioCell Title="Dark" Value="dark" />
</sv:Section>
```

After, in KsSettingsView:

```xml
<ks:Section>
  <ks:RadioCell Title="Light" GroupId="theme" Value="light" SelectedValue="{Binding SelectedTheme}" />
  <ks:RadioCell Title="Dark" GroupId="theme" Value="dark" SelectedValue="{Binding SelectedTheme}" />
</ks:Section>
```

Rows generated from a collection need one more step. Each generated cell takes the item as its `BindingContext`, and `x:Reference` does not reach a cell, so the group's selected value has to be reachable from the item: give the item view model a property that reads and writes the value on its owner, and bind `SelectedValue` to that.

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

## Take text input (EntryCell)

| AiForms | KsSettingsView | Notes |
|---|---|---|
| `EntryCell.ValueText` | `EntryCell.ValueText` (`string`) | Two-way by default; non-nullable. In C# write the type as `KsSettingsView.EntryCell` or through a using alias - the bare name collides with `Microsoft.Maui.Controls.EntryCell` (see the namespace section) |
| `EntryCell.MaxLength` (`int`, -1) | `EntryCell.MaxLength` (`int?`) | -1 meant no limit; null means no limit |
| `EntryCell.Keyboard` | `EntryCell.Keyboard` (`Keyboard?`) | Still `Microsoft.Maui.Keyboard` |
| `EntryCell.Placeholder` | `EntryCell.Placeholder` | |
| `EntryCell.PlaceholderColor` | `EntryCell.PlaceholderColor` (`Color?`) | Resolved as this value, then `SettingsView.CellPlaceholderColor`, then the OS default - which follows dark mode on its own |
| `EntryCell.TextAlignment` (`TextAlignment`, `End`) | `EntryCell.TextAlignment` (`TextAlignment?`) | The old default was `End`; null now takes the platform default |
| `EntryCell.AccentColor` (`Color`) | `EntryCell.AccentColor` (`Color?`) | |
| `EntryCell.IsPassword` | `EntryCell.IsPassword` | |
| `EntryCell.Completed` (public event) and `CompletedCommand` | Not provided | Both are gone, so delete the handler as well as the command. The two-way `ValueText` binding is the only path out; react in the property setter behind it |
| `EntryCell.SendCompleted()` | Not provided | It raised `Completed` and ran `CompletedCommand` |
| `EntryCell.SetFocus()` | Not provided | Focus cannot be driven from MAUI; the row takes focus when the user taps it |
| `EntryCell.ShowDoneButtonOnIOS` (`bool`) | Not provided | The iOS keyboard accessory is the platform default |
| `EntryCell.ValueTextColor` and the value-text font properties | `CellBase.ValueTextColor` and the same font properties | |

## Pick from a list (PickerCell)

The shape survives: an untyped list of objects, a display property named as a string, and a two-way `SelectedItem`. What changed is underneath - the authoritative selection is now an index, the items are snapshotted when you set them, and the multi-select default flipped.

| AiForms | KsSettingsView | Notes |
|---|---|---|
| `PickerCell.ItemsSource` (`IList`) | `PickerCell.ItemsSource` (`IList?`) | Still any objects. Null elements are rejected with `ArgumentException` - allowing them would make `SelectedItem == null` ambiguous with "no selection". Setting the property snapshots the elements and their display strings; in-place changes to the collection are not observed, so replace the list to update it |
| `PickerCell.DisplayMember` (property name) | `PickerCell.DisplayMember` (`string?`) | Same idea: a public instance parameterless readable property of the element's runtime type, resolved by reflection - a dot-separated path is not supported. Unset or unresolved falls back to `ToString()`. Under trimming, a property referenced only by this string has to be preserved by you, or the `ToString()` fallback kicks in |
| `PickerCell.SubDisplayMember` | `PickerCell.SubDisplayMember` (`string?`) | Secondary line on the selection surface. Null, an unresolved name, a null value or an empty string all mean no secondary text; it never enters the row's `ValueText` |
| `PickerCell.SelectedItem` (`object`, two-way) | `PickerCell.SelectedItem` (`object?`, two-way) | Still two-way, but derived: the authoritative single selection is `SelectedIndex` (`int?`, two-way). Setting `SelectedItem` reverse-looks-up the first value-equal element; a value not among the items means no selection. A value set before `ItemsSource` arrives is held and resolved once the items arrive, so XAML attribute order does not lose the initial selection |
| `PickerCell.SelectedItems` (`IList`, two-way) | `PickerCell.SelectedItems` (`IList?`, two-way) | Derived from `SelectedIndices` (`IList<int>?`, two-way; ascending and de-duplicated). Elements not among the items are dropped, duplicates collapse to one index, and null means no selection - the list you set is not guaranteed to come back as-is |
| `PickerCell.SelectionMode` (`Microsoft.Maui.Controls.SelectionMode`, `Multiple`) | `PickerCell.SelectionMode` (`PickerSelectionMode`, `Single`) | **The default flipped**: a row that never set `SelectionMode` becomes single-select. The old type was the MAUI one, whose `None` member has no counterpart; `Single` and `Multiple` carry over by name |
| `PickerCell.MaxSelectedNumber` | `PickerCell.MaxSelectedNumber` | 0 still means no limit |
| `PickerCell.PageTitle` | `PickerCell.PageTitle` (`string?`) | |
| `PickerCell.AccentColor` (`Color`) | `PickerCell.AccentColor` (`Color?`) | |
| `PickerCell.SelectedCommand` | `PickerCell.SelectedCommand` (`ICommand?`, one-way, null) | Restored under the same name. Fires only on the native selection-confirmation notification - not when you set a selection property from code, and not on cancel or a non-confirming dismiss. It runs after the selection values are written back and cross-derived, so the command observes the new selection. The argument is `SelectedItem` when a single selection is confirmed and `SelectedItems` when a multiple selection is confirmed, as in AiForms. Re-confirming the same selection executes it again. `Execute` is called without checking `CanExecute` (AiForms-compatible); there is no `CommandParameter` |
| `PickerCell.SelectedItemsOrderKey` | Not provided | Order `ItemsSource` yourself before binding |
| `PickerCell.UseNaturalSort` | Not provided | Sort the strings yourself before binding. The public `NaturalComparer` / `NaturalSortOrder` / `NaturalComparerOptions` types that backed it are not carried over either |
| `PickerCell.UseAutoValueText` | Not provided | While `ValueText` is null the row shows the current selection; set `ValueText` to take over |
| `PickerCell.UsePickToClose` (`bool`) | Not provided | The selection surface closes on the platform's own rule |
| `PickerCell.Padding` (`Thickness`) | Not provided | The selection surface uses the native layout |
| `PickerCell.ShowCommand` (`Command`, get only) | Not provided | It opened the selection surface from code; the row opens it when tapped |

Before, in AiForms:

```xml
<sv:PickerCell Title="Country"
               ItemsSource="{Binding Countries}"
               DisplayMember="Name"
               SelectedItem="{Binding SelectedCountry}" />
```

After, in KsSettingsView:

```xml
<ks:PickerCell Title="Country"
               ItemsSource="{Binding Countries}"
               DisplayMember="Name"
               SelectedItem="{Binding SelectedCountry}" />
```

Only the namespace prefix changes. One behavioral nuance: when the user picks a row, the instance written back to your view model is the element from the items snapshot, which is value-equal to - but not necessarily the same instance as - what you once set.

## Replace TextPickerCell

There is no `TextPickerCell`. A `PickerCell` in `Single` mode does the same job - a plain string list is just the simplest thing to put in its `ItemsSource`.

| AiForms `TextPickerCell` | KsSettingsView | Notes |
|---|---|---|
| `Items` (`IList`) | `PickerCell.ItemsSource` (`IList?`) | Bind the same string list; null elements are rejected |
| `SelectedItem` (`object`, two-way) | `PickerCell.SelectedItem` (`object?`, two-way) or `PickerCell.SelectedIndex` (`int?`, two-way) | The index is authoritative; bind whichever your view model holds |
| `PageTitle` / `PickerTitle` | `PickerCell.PageTitle` | One title property instead of two |
| `AccentColor` (`Color`) | `PickerCell.AccentColor` (`Color?`) | |
| `SelectedCommand` | `PickerCell.SelectedCommand` (`ICommand?`, one-way) | Fires when the user confirms a selection; in `Single` mode the argument is `SelectedItem` |
| `IsCircularPicker` | Not provided | The selection surface does not wrap around |

## Pick a number, a time, or a date

| AiForms | KsSettingsView | Notes |
|---|---|---|
| `NumberPickerCell.Number` (`int?`, null) | `NumberPickerCell.Number` (`int`, 0) | Two-way on both sides, but nullable no longer. A `int?` view model property no longer binds; decide what "unset" should show and seed the number with it |
| `NumberPickerCell.Min` (`int`, 0) / `Max` (`int`, 9999) | `NumberPickerCell.Min` (`int`, 0) / `Max` (`int`, 100) | **The `Max` default changed**: a row that never set `Max` is capped at 100 instead of 9999 |
| `NumberPickerCell.Unit` (`string`, empty) | `NumberPickerCell.Unit` (`string`) | Suffix shown with the value; unchanged |
| `NumberPickerCell.PickerTitle` | `NumberPickerCell.PickerTitle` (`string?`) | |
| `NumberPickerCell.SelectedCommand` | Not provided | React in the setter behind the two-way `Number` binding |
| (new) | `NumberPickerCell.Step` (`int`, 1) | Step width between selectable numbers |
| `TimePickerCell.Time` (`TimeSpan`) | `TimePickerCell.Time` (`TimeSpan`) | Two-way by default |
| `TimePickerCell.Format` | `TimePickerCell.Format` (`string?`) | Display only: it formats the value shown on the row and has no effect on the selection surface |
| (the device settings decided the hour cycle) | `TimePickerCell.Is24Hour` (`bool`, true) | The sole decider of the selection surface's 12/24-hour cycle; neither `Format` nor the device's region and 24-hour settings participate. **The default is 24-hour**, so a row that used to follow the device needs `Is24Hour="False"` to offer 12-hour selection |
| `TimePickerCell.PickerTitle` | `TimePickerCell.PickerTitle` (`string?`) | |
| `DatePickerCell.Date` (`DateTime?`, null) | `DatePickerCell.Date` (`DateTime`, 1970-01-01) | Two-way on both sides, but nullable no longer. A `DateTime?` view model property no longer binds; pick the date an unset row should show and seed it. Only the date part carries meaning |
| `DatePickerCell.MinimumDate` (`DateTime`, 1900-01-01) / `MaximumDate` (`DateTime`, 2100-12-31) | `DatePickerCell.MinimumDate` / `MaximumDate` (`DateTime?`) | null means unbounded, replacing the two fixed sentinel dates |
| `DatePickerCell.Format` | `DatePickerCell.Format` (`string?`) | |
| `DatePickerCell.TodayText` | `DatePickerCell.TodayText` (`string?`) | |
| `DatePickerCell.InitialDate` (`DateTime`) | Not provided | It seeded the surface while `Date` was null, which cannot happen now; seed `Date` itself |
| `DatePickerCell.IsAndroidSpinnerStyle` (`bool`, Android only) | `DatePickerCell.UIStyle` (`DatePickerUIStyle?`, both platforms) | `Wheels` is the old spinner, `Calendar` the calendar surface, null the platform default. On Android `Calendar` brings the Material date picker behaviors with it, including its text-entry mode; it puts no requirement on the host activity type or theme |
| `DatePickerCell.AndroidButtonColor` (`Color`) | `DatePickerCell.AndroidButtonColor` (`Color?`) | |

## Put your own view in a row (CustomCell)

| AiForms | KsSettingsView | Notes |
|---|---|---|
| `CustomCell.Content` (`View`) | `CustomCell.Content` (`View?`) | Still the content property, so the view is written directly inside the cell in XAML |
| `CustomCell.ShowArrowIndicator` | `CustomCell.ShowArrowIndicator` | |
| `CustomCell.Command` / `CommandParameter` | `CustomCell.Command` / `CommandParameter` | A row with neither these nor a `Tapped` handler has no tap behavior at all, so controls inside the content keep their gestures |
| `CustomCell.IsSelectable` | Not provided | Tappability follows from having a `Command` or a `Tapped` handler |
| `CustomCell.IsMeasureOnce` | Not provided | Row height follows the content, including size changes while it is on screen |
| `CustomCell.UseFullSize` | Not provided | The content area is already the whole row except the disclosure indicator |
| `CustomCell.LongCommand` | Not provided | There is no long-press hook. Its argument came from `SendLongCommand()`, which passed the `BindingContext`; there was never a `LongCommandParameter` |
| (inherited from `CommandCell` / `LabelCell` in AiForms) | No `ValueText` on `CustomCell` | The old `CustomCell` derived from `CommandCell` and so had `ValueText`; the new one derives straight from `CellBase` and has none |

On a `CustomCell`, `Title`, `Description`, `HintText`, `IconSource` and the text style properties inherited from `CellBase` are accepted and silently ignored. The properties that do have an effect are `Content`, `Command`, `CommandParameter`, `Tapped` and `ShowArrowIndicator`, plus the row-level `IsEnabled`, `IsVisible`, `BackgroundColor` and `Height`. Registering a cell type of your own, as the native SDKs allow, is not exposed in MAUI: build reusable rows as a `CustomCell` subclass or a factory method.

## Move the screen-wide styling

The `Cell*` defaults keep their names one for one. As on the cells, the sentinel values became nullable types.

| AiForms `SettingsView` | KsSettingsView `SettingsView` | Notes |
|---|---|---|
| `BackgroundColor` | `BackgroundColor` | The standard `VisualElement` property |
| `SeparatorColor` (`Color`) | `SeparatorColor` (`Color?`) | |
| `SelectedColor` (`Color`) | `SelectedColor` (`Color?`) | |
| `CellTitleColor` / `CellTitleFontSize` / `CellTitleFontFamily` / `CellTitleFontAttributes` | same names, nullable | The old `Cell*FontAttributes` were non-nullable `FontAttributes`; the new ones are `FontAttributes?`, where null inherits |
| `CellDescriptionColor` / `CellDescriptionFontSize` / `CellDescriptionFontFamily` / `CellDescriptionFontAttributes` | same names, nullable | |
| `CellValueTextColor` / `CellValueTextFontSize` / `CellValueTextFontFamily` / `CellValueTextFontAttributes` | same names, nullable | |
| `CellHintTextColor` / `CellHintFontSize` / `CellHintFontFamily` / `CellHintFontAttributes` | same names, nullable | |
| (new) | `CellPlaceholderColor` (`Color?`) | Screen-wide default for the entry placeholder color; `EntryCell.PlaceholderColor` overrides it per row, and null falls through to the OS default |
| `CellBackgroundColor` (`Color`) | `CellBackgroundColor` (`Color?`) | |
| `CellAccentColor` (`Color`) | `CellAccentColor` (`Color?`) | |
| `CellIconSize` (`Size`) | `CellIconSize` (`double?`) | One number, as on the cell |
| `CellIconRadius` (`double`) | `CellIconRadius` (`double?`) | |
| `RowHeight` (`int`, -1) | `RowHeight` (`int?`) | -1 meant automatic; null means automatic |
| `HasUnevenRows` (`bool`) | `HasUnevenRows` (`bool?`) | |
| `ShowSectionTopBottomBorder` (`bool`, true, Android only) | `ListStyle` (`SettingsViewStyle`) plus `SectionMargin` / `SectionCornerRadius` / `SectionBorderWidth` / `SectionBorderColor` | Section decoration is now part of the list style and applies on both platforms: `Classic` is the flat grouped list, `Modern` draws sections as inset boxes you can shape with the four properties |
| `SettingsView.ClearCache()` (public static) | Not provided | It emptied the renderer's icon cache. Icons go through the MAUI image source service, and there is no library-level cache to clear |
| (new) | `DisabledTextColor` (`Color?`) | Text color for disabled rows |
| (new) | `ScrollIndicatorVisible` (`bool?`) | |

`SectionMargin` is a `Thickness?` whose `Left` and `Right` are read as leading and trailing, so right-to-left layouts resolve on the native side. In `Classic` only its vertical components apply.

## Move the header and footer settings

| AiForms `SettingsView` | KsSettingsView | Notes |
|---|---|---|
| `HeaderTextColor` / `HeaderFontSize` / `HeaderFontFamily` / `HeaderFontAttributes` | same names, nullable | `HeaderFontAttributes` was a non-nullable `FontAttributes`; it is `FontAttributes?` now |
| `HeaderBackgroundColor` (`Color`) | `HeaderBackgroundColor` (`Color?`) | |
| `HeaderHeight` (`double`, -1) | `HeaderHeight` (`double?`) | `Section.HeaderHeight` overrides it for one section |
| `HeaderPadding` (`Thickness`) | Not provided | Use a `HeaderView` when you need your own spacing |
| `HeaderTextVerticalAlign` (`LayoutAlignment`) | Not provided | The platform's own header alignment applies |
| `FooterTextColor` / `FooterFontSize` / `FooterFontFamily` / `FooterFontAttributes` | same names, nullable | Same nullability change on `FooterFontAttributes` |
| `FooterBackgroundColor` (`Color`) | `FooterBackgroundColor` (`Color?`) | |
| `FooterPadding` (`Thickness`) | Not provided | Use a `FooterView` |
| (new) | `RootHeaderText` / `RootFooterText` | Text above the first section and below the last |
| (new) | `RootHeaderView` / `RootFooterView` (`View?`) | Arbitrary views in the same two places; a view wins over the text while both are set |

## Generate rows from a collection

| AiForms | KsSettingsView | Notes |
|---|---|---|
| `SettingsView.ItemsSource` (`IEnumerable`) / `ItemTemplate` / `TemplateStartIndex` | same names (`IEnumerable?` / `DataTemplate?` / `int`) | On `SettingsView` the template produces sections |
| `Section.ItemsSource` (`IList`) / `ItemTemplate` / `TemplateStartIndex` | same names, but `ItemsSource` is `IEnumerable?` | On a `Section` it produces cells. The accepted type widened, so an `IList` you already bind keeps working |
| `DataTemplateSelector` in `ItemTemplate` | Supported | Resolved just before the row is built; returning null, a nested selector, or a type that cannot be templated raises `InvalidOperationException` |

An observable source mirrors add, remove, replace and move. A reset, or setting the source to null, removes only the generated part and leaves hand-written sections and cells in place.

## Delete the Handler and PropertyMapper customizations

AiForms exposed a handler per cell type, which was the seam for customizing rendering. KsSettingsView draws rows natively from cell data, so none of this survives and none of it is needed.

| AiForms | KsSettingsView | Notes |
|---|---|---|
| `SettingsViewHandler` and its platform partials | `SettingsViewHandler` (public, in `KsSettingsView.Handlers`) | Registered by `AddKsSettingsView()`. It creates and releases the native host only; the settings tree reaches the screen through the conversion path inside the `KsSettingsView.Maui` assembly, so this is not the seam for cell rendering. Its `Mapper` and the `SettingsViewHandler(IPropertyMapper?)` constructor let you replace the view-level property mapping, nothing cell-level |
| `CellBaseHandler<TCell, TNativeCell>` | Not provided | |
| `LabelCellBaseHandler` / `EntryCellBaseHandler` and the per-cell handlers | Not provided | |
| `BasePropertyMapper` and the per-cell `PropertyMapper` entries | Not provided | Add a property to a cell only if the native row already carries the matching state |
| `handler.IsDisconnect` guards in `MapXxx` | Not provided | There is no per-cell mapper to guard |
| `UpdateXxx()` platform-view extension methods | Not provided | |

If you customized rendering through these, rebuild it as a `CustomCell` (see above) or file it as a gap in the built-in cells at https://github.com/kamusoft/KsSettingsView/issues.

## Delete the memory-leak workaround

| AiForms | KsSettingsView | Notes |
|---|---|---|
| `UseSettingsView(true)` | Not provided | `AddKsSettingsView()` takes no options |
| `HandlerCleanUpHelper` | Not provided | |
| `Page.Unloaded` / `Page.NavigatedFrom` hooks that force a disconnect | Not provided | Delete them |
| `SettingsViewConfiguration.ShouldAutoDisconnect` | Not provided | Both the class and the member were internal, so this never appeared in your own code; the flag it held came from `UseSettingsView(bool)` above |

Leaving the page releases the native host while the cells, their state, and any embedded views stay alive; coming back restores the screen from the current state. References from a cell back to the `SettingsView` are weak, so a view model holding on to cells does not keep the screen alive.

## Members with no replacement

Collected here so a search finds them, with the reason and whatever you can do instead. "Not yet" marks the features that are planned for a later phase rather than dropped.

| AiForms | Reason | What to do instead |
|---|---|---|
| `SettingsView.ItemDroppedCommand`, the `ItemDropped` event and its `DropEventArgs`, `Section.UseDragSort` | Drag-and-drop reordering is not offered yet | Offer reordering outside the settings list, or leave that one screen on the old library until it lands |
| `SettingsView.ScrollToTop` / `ScrollToBottom` | Scroll control is not exposed yet | - |
| `SettingsView.VisibleContentHeight` | The content height is not reported back | Give the control a size the layout decides |
| `SettingsView.UseDescriptionAsValue` | Description and value are always distinct | Set `ValueText` explicitly on the rows that need it |
| `SettingsView.ClearCache()` | No library-level icon cache to clear | - |
| `SettingsView.Model`, `ModelChanged` and `SettingsModel` | The renderer-facing model layer is gone | Bind `ItemsSource` / `ItemTemplate` |
| `LabelCell.IgnoreUseDescriptionAsValue` | Follows from the above | - |
| `CellBase.Tapped` on cells other than `CommandCell` / `ButtonCell` / `CustomCell` | Only those three raise taps now | Change the row to a `CommandCell` or a `CustomCell` |
| `CellBase.Section`, `Reload()`, `SetEnabledAppearance()` | Cells no longer drive the renderer | `IsEnabled` covers the appearance case |
| `CommandCell.KeepSelectedUntilBack` | Selection highlight follows the platform | - |
| `CustomCell.LongCommand` | No long-press hook | Handle the gesture inside the content view |
| `CustomCell.IsMeasureOnce` / `UseFullSize` / `IsSelectable` | Height, content area and tappability are decided natively | - |
| `EntryCell.Completed` / `CompletedCommand` / `SendCompleted()` / `SetFocus()` / `ShowDoneButtonOnIOS` | See the EntryCell section | React in the setter behind the two-way `ValueText` binding |
| `PickerCell.SelectedItemsOrderKey` / `UseNaturalSort` / `UseAutoValueText` / `UsePickToClose` / `Padding` / `ShowCommand` | See the PickerCell section | - |
| `NaturalComparer` / `NaturalSortOrder` / `NaturalComparerOptions` | The public sorting helpers behind `UseNaturalSort` are not carried over | Sort the display strings yourself, with your own comparer if the natural order matters |
| `TextPickerCell` and `IsCircularPicker` | Type removed | Use `PickerCell` in `Single` mode |
| `SimpleCheckCell.Value` | Not carried on the cell | Keep it in the view model |
| `Section.TextColor` | Per-section header color is not offered | `SettingsView.HeaderTextColor` for the whole screen |
| `CellBase.IsLoading` / `IsAnimationPlaying` / `UpdateIsLoading()`, `SettingsView.OnCollectionChanged()` / `OnSectionCollectionChanged()`, `SettingsModel`'s `GetCell()` and the rest of its query methods | Renderer plumbing that AiForms happened to expose as public | - |
| `Section.HeaderPadding` / `FooterPadding`, `HeaderTextVerticalAlign` | Header and footer layout is native | Use `HeaderView` / `FooterView` |
| Registering your own cell type | Not exposed in MAUI yet | Build the row as a `CustomCell` |
| Mac Catalyst target | Only iOS and Android are supported | - |

## Check the platform requirements

| Item | AiForms | KsSettingsView |
|---|---|---|
| Target frameworks | net9.0-ios, net9.0-android, net9.0-maccatalyst | net10.0-ios, net10.0-android |
| .NET SDK | 9.0.314 | 10.0.300 |
| Microsoft.Maui.Controls | 9.0.120 | 10.0.70 |
| iOS | 14.2 | 16.0 |
| Android | API 27 | API 29 |
| Android host theme | any | any - the library ships its own Material3 theme and draws its UI inside it |
| Placement | any layout | a layout that decides the size: a page, a `*` grid row, or an explicit size |

The `Microsoft.Maui.Controls` floor is enforced at restore: a `MauiVersion` below 10.0.70 fails the restore with NU1605 (package downgrade). The OS floors are enforced at build: the package brings a check into your project that stops the `net10.0-ios` / `net10.0-android` build with error `KSSV0001` when that target framework's `SupportedOSPlatformVersion` is below the floor. On Android the check also fires when the value is unset, because the SDK default lies below API 29.

On Android the rows, headers, and selection surfaces are visually isolated from the host theme: the library wraps them in its bundled Material3 (DayNight) theme, so the host theme's colors - dynamic color included - do not restyle them, and there is no requirement on the host activity type or theme. Where AiForms drew with the host theme, expect the default look to change; restyle through the `SettingsView` properties and per-cell overrides instead. Light or dark follows the device's night mode and the app's own uiMode control, not the host theme's declared parent. Views you embed (`CustomCell.Content`, header and footer views) still resolve against the host theme.

Every call into `SettingsView` and what it holds (sections and cells) must be made from the UI thread; the library does not marshal for you.

The types you bind do not change. iOS and Android represent images, times and dates with their own native value types, but none of those surface in MAUI: `ImageSource`, `TimeSpan` and `DateTime` remain what you bind, exactly as under AiForms.
