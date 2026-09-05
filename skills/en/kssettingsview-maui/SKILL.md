---
name: kssettingsview-maui
description: Build .NET MAUI settings screens with KsSettingsView - a public XAML / C# API (SettingsView, Section, CellBase) over the native iOS and Android settings list, with built-in cells (Label, Command, Button, Switch, Checkbox, Radio, SimpleCheck, Entry, Picker, NumberPicker, TimePicker, DatePicker) plus CustomCell holding any MAUI view, two-way bindings for user edits, ItemsSource / ItemTemplate, header and footer views, and Classic / Modern list styling. Use when adding, changing, or reviewing a settings page in a .NET MAUI app that references KsSettingsView.Maui.
license: MIT
metadata:
  language: en
  source: https://github.com/kamusoft/KsSettingsView
---

# KsSettingsView for .NET MAUI

KsSettingsView is a UI library for building settings screens - the list-style screens the iOS Settings app is made of. You declare the screen as a tree of cells grouped into sections, and that tree is the screen. This Skill covers the .NET MAUI build, which comes as a set of controls you use from XAML and C# (`SettingsView`, `Section`, and the cells). The cells are drawn by the native settings list on each platform; the MAUI types are the data you bind to.

## What you can do

| What you want to do | Where to look |
|---|---|
| Place a cell: label, action, button, switch, checkbox, radio, text field, list picker, number, time, date | [references/cells.md](references/cells.md) |
| Group cells into sections, add icons, descriptions, hints; disable or hide a cell | [references/cells.md](references/cells.md) |
| Receive the confirmation of a list selection as a command (`PickerCell.SelectedCommand`) | [references/cells.md](references/cells.md) |
| Change the screen after it is on display: add, remove, move, replace cells and sections | [references/updates.md](references/updates.md) |
| Receive user edits in a view model, generate cells from a collection, keep state across page visits | [references/updates.md](references/updates.md) |
| Colors, fonts, cell height, Classic / Modern list appearance, section boxes | [references/styling.md](references/styling.md) |
| Look up the style property list (screen-wide defaults and per-cell overrides) | [references/styling.md](references/styling.md) |
| Section and screen headers / footers, including arbitrary views in them, and where to place the control on a page | [references/styling.md](references/styling.md) |
| Put any MAUI view into a cell of the list, or package one as a reusable cell type of your own | [references/custom-cells.md](references/custom-cells.md) |

## Setup

### Take the library into your build

Add the package reference in the `.csproj` of your app.

```xml
<ItemGroup>
  <PackageReference Include="KsSettingsView.Maui" Version="0.1.0" />
</ItemGroup>
```

That one reference is all you add; NuGet brings in the platform binding layer transitively. Then register the library once during startup. A single handler is registered; there is no per-cell handler to add.

```csharp
using KsSettingsView;
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

The `Microsoft.Maui.Controls` floor is enforced at restore time: the version the .NET 10 project template writes into `MauiVersion` is lower than 10.0.70 (10.0.20 at SDK 10.0.300), and leaving it there fails the restore with NU1605 (package downgrade), so set `MauiVersion` to 10.0.70 or later. The OS floors are enforced at build time: the package carries a check into your project that stops the `net10.0-ios` / `net10.0-android` build with error `KSSV0001` when the `SupportedOSPlatformVersion` of that target framework is below iOS 16.0 / Android API 29. On Android the check also fires when the value is unset, because the SDK default lies below the floor, so declare both values explicitly.

Prefer the API-versionless target frameworks shown above. They select the platform asset and its transitive binding. If you pin an API version in the TFM, use `net10.0-android36.0` / `net10.0-ios26.0` or later. Older pins such as `net10.0-android35.0` / `net10.0-ios18.0` can restore without a warning but silently fall back to the platform-neutral `lib/net10.0` asset, so neither native binding enters the dependency graph. This package selection behavior was verified with .NET SDK 10.0.300.

```xml
<PropertyGroup>
  <MauiVersion>10.0.70</MauiVersion>
</PropertyGroup>

<PropertyGroup Condition=" $([MSBuild]::GetTargetPlatformIdentifier('$(TargetFramework)')) == 'ios' ">
  <SupportedOSPlatformVersion>16.0</SupportedOSPlatformVersion>
</PropertyGroup>

<PropertyGroup Condition=" $([MSBuild]::GetTargetPlatformIdentifier('$(TargetFramework)')) == 'android' ">
  <SupportedOSPlatformVersion>29</SupportedOSPlatformVersion>
</PropertyGroup>
```

### Name collisions with MAUI's own cells

`SwitchCell` and `EntryCell` exist under the same names in `Microsoft.Maui.Controls`, and they are the only two public types of the library that do. XAML is unaffected, because the `ks:` prefix names the namespace. In C#, a file that has `using KsSettingsView;` next to the implicit MAUI usings cannot resolve the bare names - the compiler reports CS0104 (ambiguous reference). Write the full name (`KsSettingsView.SwitchCell`) or declare a using alias in that file.

```csharp
using KsSettingsView;
using SwitchCell = KsSettingsView.SwitchCell;

Section account = new() { HeaderText = "Account" };
account.Cells.Add(new SwitchCell { Title = "Push notifications", On = true });
account.Cells.Add(new KsSettingsView.EntryCell { Title = "Name", Placeholder = "Taro Yamada" });
```

### Android theming

On Android the cells are drawn inside a Material3 theme the library ships with, so the host app has nothing to provide: any activity type and any XML theme work, a plain `ComponentActivity` on a minimal theme included. The flip side is isolation - the host theme's colors (dynamic color included) do not reach the library's cells, so looks are adjusted through the styling properties of `SettingsView` ([references/styling.md](references/styling.md)). Light and dark follow the device night mode and the app's own uiMode control, not the host theme.

## Minimal working example

```xml
<ContentPage xmlns="http://schemas.microsoft.com/dotnet/2021/maui"
             xmlns:x="http://schemas.microsoft.com/winfx/2009/xaml"
             xmlns:ks="clr-namespace:KsSettingsView;assembly=KsSettingsView.Maui"
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

- [references/cells.md](references/cells.md) - one recipe per built-in cell, plus sections, icons, and the fields every cell shares.
- [references/updates.md](references/updates.md) - changing a screen that is already on display, two-way bindings, `ItemsSource`, and what survives leaving the page.
- [references/styling.md](references/styling.md) - screen-wide defaults, per-cell overrides, list style, section decoration, headers and footers, placement.
- [references/custom-cells.md](references/custom-cells.md) - `CustomCell`, reusable cell subclasses, and the properties that do not apply to it.
