---
name: kssettingsview-aiforms-migration
description: Move a .NET MAUI settings screen off AiForms.Maui.SettingsView onto KsSettingsView. Maps the old public API - the AiForms.Settings namespace, UseSettingsView / AddSettingsViewHandler, SettingsView / SettingsRoot / Section / CellBase, every AiForms cell type (Label, Command, Button, Switch, Checkbox, Radio, SimpleCheck, Entry, Picker, TextPicker, NumberPicker, TimePicker, DatePicker, CustomCell), the screen-wide style properties, the per-cell Handler and PropertyMapper customizations, and the HandlerCleanUpHelper leak workaround - onto KsSettingsView.Maui, and names what is gone together with the replacement approach. Use when porting, reviewing, or debugging a settings page that came from AiForms.
license: MIT
metadata:
  language: en
  source: https://github.com/kamusoft/KsSettingsView
---

# Migrating from AiForms.SettingsView to KsSettingsView

KsSettingsView is a UI library for building settings screens - the list-style screens the iOS Settings app is made of. You declare the screen as a tree of rows (cells) grouped into sections, and that tree is the screen. This Skill covers moving a .NET MAUI app off AiForms.Maui.SettingsView onto KsSettingsView, member by member of the old public API.

KsSettingsView keeps the shape of AiForms.SettingsView - a `SettingsView` holding `Section`s holding cells, bound from a view model - but the rows are drawn by a native settings list instead of MAUI handlers. Most properties survive the move under the same name; a smaller set is renamed, retyped, or gone. This Skill tells you which is which for every AiForms member you may have in your XAML.

The mapping is against AiForms.Maui.SettingsView, the .NET MAUI release. Coming from the older Xamarin.Forms AiForms.SettingsView you can still read across, since the member names largely carried over into the MAUI release, but the tables here are not checked against the Xamarin.Forms API.

## What you can do

| What you want to do | Where to look |
|---|---|
| Swap the package reference, XAML namespace, and startup registration | Setup and Minimal migration below |
| Translate a cell property that survives under the same or a new name | [references/api-mapping.md](references/api-mapping.md) |
| Find what replaced `TextPickerCell`, the attached `RadioCell.SelectedValue`, `EntryCell.CompletedCommand`, or `IsAndroidSpinnerStyle` | [references/api-mapping.md](references/api-mapping.md) |
| Carry screen-wide styling over: `Cell*` defaults, header and footer, row height, section borders | [references/api-mapping.md](references/api-mapping.md) |
| Decide what to do about a dropped feature: drag sort, `ScrollToTop`, `UseDescriptionAsValue`, `LongCommand` | [references/api-mapping.md](references/api-mapping.md) |
| Delete the per-cell Handler / PropertyMapper code and the `HandlerCleanUpHelper` leak workaround | [references/api-mapping.md](references/api-mapping.md) |
| Fix a CS0104 on `SwitchCell` / `EntryCell` in C# that migrated cell-building code now hits | [references/api-mapping.md](references/api-mapping.md), then the kssettingsview-maui Skill |
| Look up the KsSettingsView API itself | the kssettingsview-maui Skill |

## Setup

Remove the `AiForms.Maui.SettingsView` package reference and reference `KsSettingsView.Maui` instead. The two libraries share no types, so their XAML namespaces stay separate and screens can move one at a time. Referencing both at once is not a configuration this project verifies, so treat any overlap as temporary.

In place of the `PackageReference` you removed, add this one in the `.csproj` of your app.

```xml
<ItemGroup>
  <PackageReference Include="KsSettingsView.Maui" Version="0.1.0" />
</ItemGroup>
```

That one line is the whole reference; the binding packages underneath arrive transitively. The package is not on NuGet.org yet - public distribution is being prepared. Until it is published, reference the facade project `maui/KsSettingsView.Maui/KsSettingsView.Maui.csproj` from a checkout of the repository with a `ProjectReference`, or point restore at a feed that holds a locally packed build; the rest of this Skill reads the same either way.

| Requirement | AiForms | KsSettingsView |
|---|---|---|
| .NET SDK | 9.0.314 | 10.0.300 |
| Target frameworks | net9.0-ios, net9.0-android, net9.0-maccatalyst | net10.0-ios, net10.0-android |
| Microsoft.Maui.Controls | 9.0.120 | 10.0.70 |
| iOS | 14.2 | 16.0 |
| Android | API 27 | API 29 |

The `Microsoft.Maui.Controls` floor is checked at restore: an AiForms project carries a `MauiVersion` below 10.0.70, and leaving it there fails the restore with NU1605 (package downgrade), so raise `MauiVersion` to 10.0.70 or later. The OS floors are checked at build: the package brings a check into your project that stops the `net10.0-ios` / `net10.0-android` build with error `KSSV0001` when that target framework's `SupportedOSPlatformVersion` is below iOS 16.0 / Android API 29 - the AiForms values (14.2 / 27) trip it, so raise both.

Mac Catalyst is not a target. Android puts no requirement on the host activity type or theme: the library ships its own Material3 theme and draws its rows inside it, so the host theme does not restyle them - AiForms screens that relied on the host theme may look different until you restyle through the `SettingsView` properties - and light or dark follows the device's night mode.

## Minimal migration

Startup registration collapses to one call. AiForms registered one handler per cell type behind `AddSettingsViewHandler()` and took a leak-workaround flag on `UseSettingsView(true)`; KsSettingsView registers a single `SettingsViewHandler` and needs no workaround.

Before, in AiForms:

```csharp
builder
    .UseMauiApp<App>()
    .UseSettingsView(true);
```

After, in KsSettingsView:

```csharp
builder
    .UseMauiApp<App>()
    .AddKsSettingsView();
```

In XAML, the namespace declaration changes and section header text moves from `Section.Title` to `Section.HeaderText`. The CLR namespace is `KsSettingsView` while the assembly (and the package) is `KsSettingsView.Maui`, so the two halves of the `xmlns` differ on purpose. Cell names and the properties in this example are unchanged.

Before, in AiForms:

```xml
<ContentPage xmlns="http://schemas.microsoft.com/dotnet/2021/maui"
             xmlns:x="http://schemas.microsoft.com/winfx/2009/xaml"
             xmlns:sv="clr-namespace:AiForms.Settings;assembly=SettingsView"
             x:Class="MyApp.SettingsPage">
  <sv:SettingsView>
    <sv:Section Title="General">
      <sv:LabelCell Title="Version" ValueText="1.0.0" />
      <sv:SwitchCell Title="Push notifications" On="{Binding NotificationsEnabled}" />
    </sv:Section>
  </sv:SettingsView>
</ContentPage>
```

After, in KsSettingsView:

```xml
<ContentPage xmlns="http://schemas.microsoft.com/dotnet/2021/maui"
             xmlns:x="http://schemas.microsoft.com/winfx/2009/xaml"
             xmlns:ks="clr-namespace:KsSettingsView;assembly=KsSettingsView.Maui"
             x:Class="MyApp.SettingsPage">
  <ks:SettingsView>
    <ks:Section HeaderText="General">
      <ks:LabelCell Title="Version" ValueText="1.0.0" />
      <ks:SwitchCell Title="Push notifications" On="{Binding NotificationsEnabled}" />
    </ks:Section>
  </ks:SettingsView>
</ContentPage>
```

Place `SettingsView` where the layout decides its size - directly in a page, in a `*` grid row, or with an explicit size. AiForms tolerated an `Auto` row or a `VerticalStackLayout`. Here it does not: in a container that sizes itself to its content, an editable row on Android loses focus while the user is typing.

## Reference files

- [references/api-mapping.md](references/api-mapping.md) - the full old-to-new table, grouped by what you are trying to migrate: namespace and registration, screen skeleton, shared cell fields, each cell type, screen-wide styling, headers and footers, templated rows, handler customizations, and the members with no replacement.
