# Styling

Recipes for the look of the screen: screen-wide defaults, per-cell overrides, list appearance, section decoration, headers and footers, and where to place the control. XAML fragments assume the `ks` namespace declaration from the minimal example in [SKILL.md](../SKILL.md).

Each drawn value is resolved in this order: a value the cell type owns by meaning (such as a button title color), then the override on the cell, then the screen-wide default on `SettingsView`, then the platform default. Leaving a property unset means "inherit the next level", not "use nothing".

## Set the defaults for the whole screen

The screen-wide values live on `SettingsView` as individual properties.

```xml
<ks:SettingsView BackgroundColor="#F2EFE6"
                 CellBackgroundColor="#FFFFFF"
                 SeparatorColor="#E6DAB9"
                 SelectedColor="#50FFBF00"
                 CellAccentColor="#FFBF00"
                 DisabledTextColor="#999999"
                 CellTitleColor="#555555"
                 CellPlaceholderColor="#B0A98F"
                 HeaderTextColor="#CC9900"
                 HeaderBackgroundColor="#FBF3DA"
                 FooterTextColor="#999999"
                 FooterBackgroundColor="#FBF3DA"
                 ScrollIndicatorVisible="True">
  <ks:Section HeaderText="General">
    <ks:LabelCell Title="Version" ValueText="1.0.0" />
  </ks:Section>
</ks:SettingsView>
```

`BackgroundColor` is the backdrop of the list, `CellBackgroundColor` is the default background of a cell - one is never derived from the other. `HeaderBackgroundColor` and `FooterBackgroundColor` fill the section header and footer areas, and they are the one pair that does not reach both platforms: on iOS those areas keep the platform background and only `HeaderTextColor` and `FooterTextColor` take effect. `CellPlaceholderColor` is the default placeholder text color of every `EntryCell`; a cell overrides it with `PlaceholderColor`, and leaving both unset keeps the OS default placeholder color, which adapts to dark mode on its own.

## Override the look of one cell

The same values exist per cell, and only the cells that set them are drawn differently.

```xml
<ks:LabelCell Title="Danger zone"
              ValueText="enabled"
              TitleColor="#CC3333"
              ValueTextColor="#CC3333"
              BackgroundColor="#FFF3F3" />
```

Interactive cells add `AccentColor` for the color of their control - the switch knob, the checkmark, the picker highlight.

```xml
<ks:SwitchCell Title="Push notifications" On="True" AccentColor="#34C759" />
```

## Style property list

The screen-wide defaults live on `SettingsView` and the per-cell overrides on `CellBase` (the base shared by the cells), as the properties below; the recipes in this file show them in use. Each is a bindable property with a matching `BindableProperty` field named `FooProperty` (for example `CellTitleColorProperty`).

`SettingsView` (screen-wide defaults):

| Category | Properties |
|---|---|
| Colors and behavior | `SeparatorColor`, `SelectedColor`, `CellBackgroundColor`, `CellAccentColor`, `DisabledTextColor`, `CellPlaceholderColor`, `ScrollIndicatorVisible`, `RowHeight`, `HasUnevenRows` |
| Header format | `HeaderTextColor`, `HeaderBackgroundColor`, `HeaderFontFamily`, `HeaderFontSize`, `HeaderFontAttributes`, `HeaderHeight` |
| Footer format | `FooterTextColor`, `FooterBackgroundColor`, `FooterFontFamily`, `FooterFontSize`, `FooterFontAttributes` |
| Cell title defaults | `CellTitleColor`, `CellTitleFontFamily`, `CellTitleFontSize`, `CellTitleFontAttributes` |
| Cell value text defaults | `CellValueTextColor`, `CellValueTextFontFamily`, `CellValueTextFontSize`, `CellValueTextFontAttributes` |
| Cell description defaults | `CellDescriptionColor`, `CellDescriptionFontFamily`, `CellDescriptionFontSize`, `CellDescriptionFontAttributes` |
| Cell hint defaults | `CellHintTextColor`, `CellHintFontFamily`, `CellHintFontSize`, `CellHintFontAttributes` |
| Icons | `CellIconSize`, `CellIconRadius` |
| Section decoration | `SectionMargin`, `SectionCornerRadius`, `SectionBorderWidth`, `SectionBorderColor` |

`CellBase` (per-cell overrides):

| Category | Properties |
|---|---|
| Title | `TitleColor`, `TitleFontFamily`, `TitleFontSize`, `TitleFontAttributes` |
| Value text | `ValueTextColor`, `ValueTextFontFamily`, `ValueTextFontSize`, `ValueTextFontAttributes` |
| Description | `DescriptionColor`, `DescriptionFontFamily`, `DescriptionFontSize`, `DescriptionFontAttributes` |
| Hint | `HintTextColor`, `HintFontFamily`, `HintFontSize`, `HintFontAttributes` |
| Cell and icon | `BackgroundColor`, `IconSize`, `IconRadius`, `Height` |

## Change fonts

Fonts are exposed as three separate properties per text slot, screen-wide and per cell.

```xml
<ks:SettingsView CellTitleFontFamily="OpenSansRegular"
                 CellTitleFontSize="16"
                 CellDescriptionFontSize="12"
                 HeaderFontAttributes="Bold">
  <ks:Section HeaderText="General">
    <ks:LabelCell Title="Highlighted" TitleFontAttributes="Bold" TitleFontSize="18" />
  </ks:Section>
</ks:SettingsView>
```

## Control cell height

`RowHeight` is the screen-wide baseline and `Height` overrides it for one cell. With `HasUnevenRows="True"` the height acts as a minimum and each cell grows to fit its content; with `False` the height is fixed. Only a positive `RowHeight` counts, so there is no number that means "measure it": leave the property unset and let `HasUnevenRows="True"` give you cells that fit their content.

```xml
<ks:SettingsView HasUnevenRows="True">
  <ks:Section HeaderText="Account">
    <ks:CommandCell Title="Tanaka Taro" Description="tanaka.taro@example.com" Height="80" />
  </ks:Section>
</ks:SettingsView>
```

## Choose how sections are separated (Classic separators / Modern rounded boxes)

`ListStyle` (of type `SettingsViewStyle`) chooses how sections are separated. `Classic` only draws separator lines between cells and sections, and cells span the full width of the screen. `Modern` wraps just the cells of each section in a rounded box, with the section header and footer outside the box. Switching does not change the content or the identity of anything.

```xml
<ks:SettingsView ListStyle="Modern">
  <ks:Section HeaderText="General">
    <ks:LabelCell Title="Version" ValueText="1.0.0" />
  </ks:Section>
</ks:SettingsView>
```

## Tune the Modern section box

Four properties describe the box and the spacing around each section. They apply to every section on the screen. Left and right of `SectionMargin` mean leading and trailing, so they follow the reading direction; in `Classic` only the vertical parts apply.

```xml
<ks:SettingsView ListStyle="Modern"
                 SectionMargin="16,22,16,0"
                 SectionCornerRadius="12"
                 SectionBorderWidth="1"
                 SectionBorderColor="#C7C7CC">
  <ks:Section HeaderText="General">
    <ks:LabelCell Title="Version" ValueText="1.0.0" />
  </ks:Section>
</ks:SettingsView>
```

Leaving one unset falls back to the platform default: the default margin and corner radius are shared by both platforms, and no border is drawn.

## Add headers and footers

Sections take `HeaderText` and `FooterText`; the screen takes `RootHeaderText` and `RootFooterText`. `HeaderHeight` fixes the height of one section header, and content that does not fit is clipped.

```xml
<ks:SettingsView RootHeaderText="Settings"
                 RootFooterText="Version 1.0.0">
  <ks:Section HeaderText="General"
              FooterText="Applies to this device only."
              HeaderHeight="60">
    <ks:LabelCell Title="Version" ValueText="1.0.0" />
  </ks:Section>
</ks:SettingsView>
```

## Put a view in a header or footer

Any MAUI view can take the place of the text. There are four slots - `RootHeaderView` and `RootFooterView` on the screen, `HeaderView` and `FooterView` on a section. While a view is set it wins over the text, and setting the view back to null falls back to the text.

```xml
<ks:SettingsView>
  <ks:SettingsView.RootHeaderView>
    <Border BackgroundColor="#DDEBFF" StrokeThickness="0" Padding="16,12">
      <Label TextColor="#1F4E9C" FontAttributes="Bold" Text="Account" />
    </Border>
  </ks:SettingsView.RootHeaderView>

  <ks:Section>
    <ks:Section.HeaderView>
      <Border BackgroundColor="#E4F3E6" StrokeThickness="0" Padding="16,12">
        <Label TextColor="#2E6B33" Text="{Binding SectionCaption}" />
      </Border>
    </ks:Section.HeaderView>

    <ks:LabelCell Title="Version" ValueText="1.0.0" />
  </ks:Section>
</ks:SettingsView>
```

These views join the page's logical tree and inherit the `BindingContext` of their owner, so bindings inside them work without extra wiring. Changing what a view shows updates it in place, and the area grows with it unless `HeaderHeight` fixes the height.

## Size the icons

Icon size and corner radius are resolved per screen and per cell.

```xml
<ks:SettingsView CellIconSize="32" CellIconRadius="8">
  <ks:Section HeaderText="General">
    <ks:CommandCell Title="Wi-Fi" IconSource="ic_wifi.png" IconSize="24" IconRadius="0" />
  </ks:Section>
</ks:SettingsView>
```

## Place the control on a page

Give `SettingsView` a placement whose size the layout decides: directly in a page, in a `*` grid row, or with an explicit size. In those placements the control is never asked how tall its content would be.

```xml
<Grid RowDefinitions="Auto,*">
  <Label Margin="16" Text="Settings" />
  <ks:SettingsView Grid.Row="1">
    <ks:Section HeaderText="General">
      <ks:LabelCell Title="Version" ValueText="1.0.0" />
    </ks:Section>
  </ks:SettingsView>
</Grid>
```

Avoid placements that ask the control for its content size - inside a `VerticalStackLayout`, as the content of a vertical `ScrollView`, or in an `Auto` grid row. The screen still renders, but on Android a field being edited can lose focus while the list is measured.
