# Cells

Recipes for placing rows in a settings screen. Every XAML example assumes the `ks` namespace declaration from the minimal example in [SKILL.md](../SKILL.md). A fragment that starts with `<ks:Section>` goes directly inside a `<ks:SettingsView>` element; a fragment that is a bare cell goes inside a `<ks:Section>`, because `Root` - the content property of `SettingsView` - holds sections, not cells. Bindings resolve against the page `BindingContext`; the properties they name are yours to declare on a view model.

## Group rows into a section

Rows always live inside a section. A section takes optional header and footer text.

```xml
<ks:Section HeaderText="Account" FooterText="Signing out keeps local data.">
  <ks:LabelCell Title="Signed in as" ValueText="taro" />
</ks:Section>
<ks:Section>
  <ks:LabelCell Title="App information" />
</ks:Section>
```

## Show a read-only value

`LabelCell` displays text and never reacts to taps.

```xml
<ks:LabelCell Title="Storage" ValueText="256 GB" />
```

## Run an action or navigate from a row

`CommandCell` shows a disclosure indicator and runs a command when tapped. Set `HideArrow="True"` to drop the indicator.

```xml
<ks:CommandCell Title="License"
                ValueText="MIT"
                Command="{Binding ShowLicenseCommand}"
                CommandParameter="license" />
```

The row is tappable while `IsEnabled` is true and `Command.CanExecute(CommandParameter)` returns true, and it follows `CanExecuteChanged`. The `Tapped` event fires first, then the command executes.

Subscribe to `Tapped` when the notification alone is what you want. The attribute names a method in the code-behind of the page; `ButtonCell` and `CustomCell` expose the same event.

```xml
<ks:CommandCell Title="Open the log" Tapped="OnOpenLogTapped" />
```

```csharp
private void OnOpenLogTapped(object? sender, EventArgs e)
{
    _logCount++;
}
```

## Put a button in a row

`ButtonCell` never shows a disclosure indicator. `TitleAlignment` only shows visually on rows that carry no `ValueText`, because a row with a value text gives the title only the width it needs.

```xml
<ks:ButtonCell Title="Sign out"
               TitleColor="#CC3333"
               TitleAlignment="Center"
               Command="{Binding SignOutCommand}" />
```

## Toggle a boolean value

`SwitchCell.On` is two-way by default, so a user flipping the switch writes straight back to the bound property.

```xml
<ks:SwitchCell Title="Push notifications"
               Description="Delivery may be delayed on metered networks."
               On="{Binding NotificationsEnabled}" />
```

## Check an independent option

`CheckboxCell` draws a checkbox, `SimpleCheckCell` draws a plain checkmark at the trailing edge. Both own an independent boolean; neither is a substitute for `RadioCell`.

```xml
<ks:CheckboxCell Title="Agree to the terms" Checked="{Binding AgreedTerms}" />
<ks:SimpleCheckCell Title="Send crash reports" Checked="{Binding SendReports}" />
```

## Pick one row out of a group

Rows that share a `GroupId` form one selection. Each row carries its own `Value`, and every row in the group binds `SelectedValue` to the same property.

```xml
<ks:Section HeaderText="Theme">
  <ks:RadioCell Title="Light" GroupId="theme" Value="light" SelectedValue="{Binding Theme}" />
  <ks:RadioCell Title="Dark" GroupId="theme" Value="dark" SelectedValue="{Binding Theme}" />
</ks:Section>
```

## Edit text in a row

`EntryCell` turns the row itself into the editor, so `ValueText` is not a separate display slot here: it is the edited string, and it is two-way by default.

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

`Keyboard` takes the standard MAUI keyboards (`Default`, `Plain`, `Text`, `Chat`, `Url`, `Email`, `Numeric`, `Telephone`). `TextAlignment` sets the alignment of the input text; unset (null) it keeps the native default, trailing. There is no value-changed event: the two-way binding is the only way values come back. `PlaceholderColor` colors the placeholder of one row; unset, it inherits `SettingsView.CellPlaceholderColor` and then the OS default, which adapts to dark mode on its own.

## Choose one item from a list

Tapping a `PickerCell` opens a selection surface. `SelectionMode` (of type `PickerSelectionMode`) decides how many items it takes: `Single` (the default) or `Multiple`. In `Single` mode `SelectedIndex` is the source of truth, and `SelectedItem` is derived from it and `ItemsSource`.

`ItemsSource` holds objects of any type, not just strings. A null element is rejected with `ArgumentException`, and the collection is read once when assigned - edits inside the same collection are not observed, so assign a new collection to change the candidates.

```xml
<ks:PickerCell Title="Theme"
               ItemsSource="{Binding Themes}"
               SelectedIndex="{Binding ThemeIndex}"
               PageTitle="Select a theme" />
```

## Choose several items from a list

Switch the mode to `Multiple` and bind `SelectedIndices` instead. `MaxSelectedNumber` caps the selection; `0` means no cap.

```xml
<ks:PickerCell Title="Notification types"
               SelectionMode="Multiple"
               ItemsSource="{Binding NotificationTypes}"
               SelectedIndices="{Binding NotificationSelection}"
               MaxSelectedNumber="3"
               PageTitle="Select notification types" />
```

## Show object items with a readable text

`DisplayMember` names the property whose value becomes the item text, on the row and in the selection surface; leave it unset (or name a property that does not resolve) and the item's `ToString()` is shown instead. `SubDisplayMember` adds a second line under each candidate, in the selection surface only. Both resolve public instance properties by name through reflection, so keep those properties preserved when trimming. The former `DisplayFormatter` delegate is gone - use `DisplayMember` instead.

```xml
<ks:PickerCell Title="Plan"
               ItemsSource="{Binding Plans}"
               DisplayMember="Name"
               SubDisplayMember="Detail"
               SelectedItem="{Binding SelectedPlan}"
               PageTitle="Select a plan" />
```

## Work in items instead of indices

`SelectedItem` (single selection) and `SelectedItems` (multiple selection) are two-way and kept in step with `SelectedIndex` / `SelectedIndices` and `ItemsSource`; the index side stays the source of truth. Setting an item that is not among the candidates leaves the row unselected (in a multiple selection, elements that are not found are simply dropped) - the lookup uses value equality and takes the first match. An item bound before `ItemsSource` arrives is held and resolved once the candidates are set, so the order of XAML attributes and bindings does not matter.

```xml
<ks:PickerCell Title="Theme"
               ItemsSource="{Binding Themes}"
               SelectedItem="{Binding Theme}" />
```

## Receive the moment a selection is confirmed

`SelectedCommand` notifies you of the moment the user confirms a selection on the selection surface. Reach for it when the two-way value binding alone cannot tell a user confirmation apart from initialization or a programmatic update. It executes only when the user confirms a selection: setting the public properties (`SelectedIndex` and the rest) directly does not run it, and neither does cancelling or dismissing without confirming. Reconfirming the same selection does run it - it reports the confirmation, not a change of value.

Execution comes after the selection is written back, so the command sees the new, confirmed selection. The argument follows the kind of confirmation: `SelectedItem` for a single-selection confirmation, `SelectedItems` for a multiple-selection one. `CanExecute` is never consulted, and there is no `CommandParameter` property.

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

## Choose a number

`NumberPickerCell` offers `Min` to `Max` in `Step` increments. `Unit` is appended to the displayed value.

```xml
<ks:NumberPickerCell Title="Font size"
                     Min="10"
                     Max="30"
                     Step="1"
                     Number="{Binding FontSize}"
                     Unit="px"
                     PickerTitle="Select a size" />
```

## Choose a time

`Time` is a `TimeSpan`. `Is24Hour` alone decides the hour cycle of the picker: `True` (the default) opens a 24-hour picker, `False` a 12-hour one with an AM/PM column - on every device, because neither the device's 24-hour setting nor `Format` takes part. `Format` only shapes the value text on the row, and it is carried through to the platform date and time formatter that draws it - `DateFormatter` on iOS, `DateTimeFormatter` on Android - so write a pattern those accept, not a .NET format specifier, and keep it consistent with `Is24Hour` yourself (nothing validates the pair).

```xml
<ks:TimePickerCell Title="Alarm"
                   Time="{Binding AlarmTime}"
                   Format="h:mm a"
                   Is24Hour="False"
                   PickerTitle="Alarm time" />
```

On Android the picker is a bottom sheet with hour and minute wheels on every host; there is no keyboard input mode for the time.

## Choose a date

`Date` is a `DateTime` whose date part is meaningful, and `Format` goes to the same platform formatter as on `TimePickerCell`. `UIStyle` (of type `DatePickerUIStyle?`) picks the selection surface: `Calendar` or `Wheels`, and leaving it unset follows the platform default. `TodayText` opts into a jump-to-today action.

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

`Calendar` on Android opens a Material 3 calendar dialog that also offers a text input mode the user can switch to; it works on any host activity and theme. `AndroidButtonColor` colors the OK and CANCEL actions of the `Wheels` surface on Android and falls back to the `AccentColor` resolution when unset - it is an Android-only setting and does not affect the display on other platforms.

## Rules the picker rows share

`PickerCell`, `NumberPickerCell`, `TimePickerCell`, and `DatePickerCell` write back only when the user confirms: the Done button on iOS, the OK button on Android, or the tap on a candidate row in a single-selection `PickerCell`. Cancelling, tapping outside, going Back, and swiping a sheet away throw the work in progress away and leave the bound property as it was - a multiple-selection `PickerCell` holds its working set the same way and does not touch `SelectedIndices` until the confirmation. Only `PickerCell` carries a path that reports the confirmation itself - `SelectedCommand`, above.

All four also carry `ValueText`. Leave it unset and the row shows the current selection on its own; set it and your string is shown instead.

## Add an icon to a row

`IconSource` is a normal MAUI `ImageSource`, so file names, `MauiImage` assets, URIs, and embedded resources all work. The image is resolved asynchronously; a source that fails to resolve leaves the row without an icon.

```xml
<ks:CommandCell Title="Profile"
                Description="tanaka.taro@example.com"
                IconSource="ic_account_circle.png"
                Command="{Binding OpenProfileCommand}" />
```

## Add supporting text to a row

`Description` sits under the title on every cell except `ButtonCell` and `CustomCell`. `HintText`, the secondary note, has a narrower exception: every cell except `CustomCell` carries it, `ButtonCell` included. Both are hidden while null.

```xml
<ks:LabelCell Title="Storage"
              Description="Long descriptions wrap over several lines."
              HintText="Updated a moment ago"
              ValueText="256 GB" />
```

## Disable or hide a row

`IsEnabled="False"` keeps the row visible but stops it reacting and draws it in the disabled text color. `IsVisible="False"` drops it from the screen while keeping its value in the model, so the binding keeps working and the row comes back in place.

```xml
<ks:CommandCell Title="Sync now" IsEnabled="{Binding IsOnline}" />
<ks:LabelCell Title="Debug build" IsVisible="{Binding IsDebug}" />
```
