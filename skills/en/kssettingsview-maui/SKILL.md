---
name: kssettingsview-maui
description: Build .NET MAUI settings screens with KsSettingsView - a public XAML / C# API (SettingsView, Section, CellBase) over the native iOS and Android settings list, with 12 built-in cells (Label, Command, Button, Switch, Checkbox, Radio, SimpleCheck, Entry, Picker, NumberPicker, TimePicker, DatePicker) plus CustomCell rows holding any MAUI view, two-way bindings for user edits, ItemsSource / ItemTemplate, header and footer views, and Classic / Modern list styling. Use when adding, changing, or reviewing a settings page in a .NET MAUI app that references KsSettingsView.Maui.
license: MIT
metadata:
  language: en
  source: https://github.com/kamusoft/KsSettingsView
---

# KsSettingsView for .NET MAUI

KsSettingsView is a UI library for building settings screens - the list-style screens the iOS Settings app is made of. You declare the screen as a tree of rows (cells) grouped into sections, and that tree is the screen. This Skill covers the .NET MAUI build, which comes as a set of controls you use from XAML and C# (`SettingsView`, `Section`, and the cells). The rows are drawn by the native settings list on each platform; the MAUI types are the data you bind to.

## What you can do

| What you want to do | Where to look |
|---|---|
| Place a row: label, action, button, switch, checkbox, radio, text field, list picker, number, time, date | [references/cells.md](references/cells.md) |
| Group rows into sections, add icons, descriptions, hints; disable or hide a row | [references/cells.md](references/cells.md) |
| Receive the confirmation of a list selection as a command (`PickerCell.SelectedCommand`) | [references/cells.md](references/cells.md) |
| Change the screen after it is on display: add, remove, move, replace rows and sections | [references/updates.md](references/updates.md) |
| Receive user edits in a view model, generate rows from a collection, keep state across page visits | [references/updates.md](references/updates.md) |
| Colors, fonts, row height, Classic / Modern list appearance, section boxes | [references/styling.md](references/styling.md) |
| Look up the style property list (screen-wide defaults and per-row overrides) | [references/styling.md](references/styling.md) |
| Section and screen headers / footers, including arbitrary views in them, and where to place the control on a page | [references/styling.md](references/styling.md) |
| Put any MAUI view into a row of the list, or package one as a reusable cell type of your own | [references/custom-cells.md](references/custom-cells.md) |

## Setup

### Take the library into your build

Add the package reference in the `.csproj` of your app.

```xml
<ItemGroup>
  <PackageReference Include="KsSettingsView.Maui" Version="0.1.0" />
</ItemGroup>
```

That one reference is all you add; the binding layer underneath comes in transitively. Then register the library once during startup. A single handler is registered; there is no per-cell handler to add.

```csharp
using KsSettingsView.Maui;
using Microsoft.Maui.Hosting;

public static class MauiProgram
{
    public static MauiApp CreateMauiApp()
    {
        MauiAppBuilder builder = MauiApp.CreateBuilder();

        builder
            .UseMauiApp<App>()
            .AddKsSettingsView();

        return builder.Build();
    }
}
```

### Versions

| Requirement | Minimum |
|---|---|
| .NET SDK | 10.0.300 |
| Target frameworks | net10.0-ios, net10.0-android |
| Microsoft.Maui.Controls | 10.0.70 |
| iOS | 16.0 |
| Android | API 29 |

### Android theming

On Android the rows are drawn inside a Material3 theme the library ships with, so the host app has nothing to provide: any activity type and any XML theme work, a plain `ComponentActivity` on a minimal theme included. The flip side is isolation - the host theme's colors (dynamic color included) do not reach the library's rows, so looks are adjusted through the styling properties of `SettingsView` ([references/styling.md](references/styling.md)). Light and dark follow the device night mode and the app's own uiMode control, not the host theme.

## Minimal working example

```xml
<ContentPage xmlns="http://schemas.microsoft.com/dotnet/2021/maui"
             xmlns:x="http://schemas.microsoft.com/winfx/2009/xaml"
             xmlns:ks="clr-namespace:KsSettingsView.Maui;assembly=KsSettingsView.Maui"
             x:Class="MyApp.SettingsPage">
  <ks:SettingsView>
    <ks:Section HeaderText="General">
      <ks:LabelCell Title="Version" ValueText="1.0.0" />
      <ks:SwitchCell Title="Push notifications" On="True" />
    </ks:Section>
  </ks:SettingsView>
</ContentPage>
```

`Root` is the content property of `SettingsView`, so sections are written directly inside it, and `Cells` is the content property of `Section`. Place `SettingsView` where its size is decided by the layout - directly in a page, in a `*` grid row, or with an explicit size.

## Reference files

- [references/cells.md](references/cells.md) - one recipe per built-in cell, plus sections, icons, and the fields every row shares.
- [references/updates.md](references/updates.md) - changing a screen that is already on display, two-way bindings, `ItemsSource`, and what survives leaving the page.
- [references/styling.md](references/styling.md) - screen-wide defaults, per-row overrides, list style, section decoration, headers and footers, placement.
- [references/custom-cells.md](references/custom-cells.md) - `CustomCell`, reusable cell subclasses, and the properties that do not apply to it.
