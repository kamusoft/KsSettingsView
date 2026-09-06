# Styling

Recipes for colors, fonts, sizes, list appearance and the supplementary areas around the cells. Every example on this page assumes the imports below. The style types and the modifiers are split across two packages: `Theme`, `CellStyle`, `KsSettingsViewDefaults`, `KsImage` and `KsSettingsViewStyle` come from `jp.kamusoft.kssettingsview.ui`, while the modifiers you chain onto a handle come from `jp.kamusoft.kssettingsview.compose`.

```kotlin
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.kamusoft.kssettingsview.compose.KsSettingsView
import jp.kamusoft.kssettingsview.compose.LabelCell
import jp.kamusoft.kssettingsview.compose.SwitchCell
import jp.kamusoft.kssettingsview.compose.backgroundColor
import jp.kamusoft.kssettingsview.compose.cellHeight
import jp.kamusoft.kssettingsview.compose.font
import jp.kamusoft.kssettingsview.compose.icon
import jp.kamusoft.kssettingsview.compose.sectionFooter
import jp.kamusoft.kssettingsview.compose.sectionHeader
import jp.kamusoft.kssettingsview.compose.titleColor
import jp.kamusoft.kssettingsview.ui.CellStyle
import jp.kamusoft.kssettingsview.ui.KsImage
import jp.kamusoft.kssettingsview.ui.KsSettingsViewDefaults
import jp.kamusoft.kssettingsview.ui.KsSettingsViewStyle
import jp.kamusoft.kssettingsview.ui.Theme
```

`dp` and `sp` are the extension properties that turn a number into a size, so `80.dp` needs `androidx.compose.ui.unit.dp` imported even though the type it produces is `Dp`.

Values resolve in this order: the meaning-specific value of the cell, then `CellStyle`, then `Theme`, then the library default for the current appearance, then the platform default. Compose types are used directly - `androidx.compose.ui.graphics.Color`, `androidx.compose.ui.text.TextStyle`, `androidx.compose.ui.unit.Dp`.

A color you did not set is `Color.Unspecified`. That is the one spelling of "unset" for colors: it is the default of every color field of `Theme` and `CellStyle`, and also of every color argument a cell takes for its own meaning, none of which is a nullable `Color?` (fonts, sizes and paddings do mark the unset state with `null`). The library fills unspecified colors in when it draws, from its own light or dark default set, so a screen that passes no `Theme` is legible in dark mode with nothing extra on your side. A color you did pass is never replaced by another value, in either appearance - the recipes below cover how to give such a color an appearance of its own.

Where the resolution does reach a platform default it lands on a Material3-derived theme the library bundles, not on the theme of your app: neither the XML theme nor a Compose `MaterialTheme` recolors the library UI, so everything on this page is the way to restyle it. Light and dark come from the device night mode and the uiMode APIs, since the bundled theme is a DayNight one.

## Apply a theme to the whole screen

`Theme` holds the screen-wide defaults. Every parameter has a default, so specify only what you change.

```kotlin
val warmTheme = Theme(
    separatorColor = Color(0xFFE6D9BA),
    backgroundColor = Color(0xFFF2F0E6),
    cellAccentColor = Color(0xFFFFBF00),
    cellTitleColor = Color(0xFFCC9900),
)

KsSettingsView(theme = warmTheme) {
    Section(header = "General") {
        LabelCell(title = "Version", valueText = "1.0.0")
    }
}
```

`backgroundColor` paints the canvas behind the list and `cellBackgroundColor` paints the cells; they are separate areas, so setting one does not imply the other.

These are all the fields of `Theme`, in declaration order. `Theme` is a data class, so pass them as named arguments in any order and leave out what you do not change.

| Group | Field | Type | Left unspecified |
|---|---|---|---|
| List | `separatorColor` | `Color` | `Color.Unspecified` - appearance default |
| List | `backgroundColor` | `Color` | `Color.Unspecified` - appearance default |
| List | `cellBackgroundColor` | `Color` | `Color.Unspecified` - appearance default |
| List | `selectedColor` | `Color` | `Color.Unspecified` - appearance default |
| List | `cellAccentColor` | `Color` | `Color.Unspecified` - appearance default |
| List | `disabledTextColor` | `Color` | `Color.Unspecified` - appearance default |
| List | `scrollIndicatorVisible` | `Boolean` | `true` |
| Height | `rowHeight` | `Int` | `-1` (automatic) |
| Height | `hasUnevenRows` | `Boolean` | `true` |
| Header | `headerTextColor` | `Color` | `Color.Unspecified` - appearance default |
| Header | `headerBackgroundColor` | `Color` | `Color.Unspecified` - appearance default |
| Header | `headerFontSize` | `Double` | `-1.0` |
| Header | `headerFont` | `TextStyle?` | `null` |
| Header | `headerHeight` | `Double` | `-1.0` (automatic) |
| Footer | `footerTextColor` | `Color` | `Color.Unspecified` - appearance default |
| Footer | `footerBackgroundColor` | `Color` | `Color.Unspecified` - appearance default |
| Footer | `footerFontSize` | `Double` | `-1.0` |
| Footer | `footerFont` | `TextStyle?` | `null` |
| Cell defaults | `cellTitleColor` | `Color` | `Color.Unspecified` - appearance default, per cell kind |
| Cell defaults | `cellTitleFont` | `TextStyle?` | `null` |
| Cell defaults | `cellTitleFontSize` | `Double` | `-1.0` |
| Cell defaults | `cellValueTextColor` | `Color` | `Color.Unspecified` - follows the title color |
| Cell defaults | `cellValueTextFont` | `TextStyle?` | `null` |
| Cell defaults | `cellDescriptionColor` | `Color` | `Color.Unspecified` - appearance default |
| Cell defaults | `cellDescriptionFont` | `TextStyle?` | `null` |
| Cell defaults | `cellHintTextColor` | `Color` | `Color.Unspecified` - follows `cellAccentColor` |
| Cell defaults | `cellHintFont` | `TextStyle?` | `null` |
| Cell defaults | `cellIconSize` | `Dp?` | `null` (24dp) |
| Cell defaults | `cellIconRadius` | `Dp?` | `null` (0dp) |
| Section box | `sectionMargin` | `PaddingValues?` | `null` |
| Section box | `sectionCornerRadius` | `Dp?` | `null` |
| Section box | `sectionBorderWidth` | `Dp?` | `null` (no border) |
| Section box | `sectionBorderColor` | `Color` | `Color.Unspecified` (transparent) |
| Cell defaults | `cellPlaceholderColor` | `Color` | `Color.Unspecified` (OS default) |

`cellTitleFontSize` is an independent size that overrides the point size of whichever title font was resolved, and `headerFontSize` / `footerFontSize` do the same for headers and footers. Any of the three is only applied when it is positive.

The title color marked "per cell kind" is why `ButtonCell` still gets a tappable-looking title while every other cell gets the plain text color: the default is chosen per cell when the row is drawn rather than stored on the theme. Setting `cellTitleColor` yourself takes that distinction away and colors `ButtonCell` like the rest.

## Start from the library default colors

`KsSettingsViewDefaults` returns the library defaults as a `Theme` with role names on it. Use it to derive a color from a default, or to hold on to the defaults while changing a few of them. `theme()` is the Composable form that picks the set matching the current appearance, and it is also the default value of the `theme` parameter of `KsSettingsView`.

```kotlin
val brandedTheme = KsSettingsViewDefaults.theme().copy(
    cellAccentColor = Color(0xFFFFBF00),
)

KsSettingsView(theme = brandedTheme) {
    Section(header = "General") {
        LabelCell(title = "Version", valueText = "1.0.0")
    }
}
```

Outside a Composable - a view holder, an activity driving the XML view host - use `KsSettingsViewDefaults.theme(darkTheme = ...)`, or `lightTheme()` / `darkTheme()` to name a set outright.

The `Theme` returned by these factories carries the ten colors of the list, the cells, the separator, the selection, the accent, the disabled text and the header / footer text and background. The rest stay `Color.Unspecified` in it, because they are decided when the row is drawn: title and description from the appearance, value text from the title, hint text from the accent, the placeholder from the bundled theme, and the section border from nothing at all. Pass a factory set that disagrees with the device - `darkTheme()` on a light device - and those late-resolved colors follow the device instead; `copy` the returned theme and state them if you need them pinned.

The `Theme` companion no longer publishes color constants; only `DEFAULT_CELL_ICON_SIZE_DP_VALUE` and `DEFAULT_CELL_ICON_RADIUS_DP_VALUE`, the two `Float` dp values behind the icon frame, still live there.

## Give light and dark their own colors

When you want to choose both appearances yourself, pick the theme where you build it. In Compose that is `isSystemInDarkTheme()`; a view host reads the night mode out of `resources.configuration`.

```kotlin
val theme = if (isSystemInDarkTheme()) {
    KsSettingsViewDefaults.darkTheme().copy(cellAccentColor = Color(0xFFFFD54F))
} else {
    KsSettingsViewDefaults.lightTheme().copy(cellAccentColor = Color(0xFFFFBF00))
}

KsSettingsView(theme = theme) {
    Section(header = "General") {
        LabelCell(title = "Version", valueText = "1.0.0")
    }
}
```

Colors you leave unspecified keep following the appearance on their own: the view host re-resolves them when the night mode changes under it, even in an activity that handles `uiMode` itself instead of being recreated. Colors you state explicitly, like the accent above, are yours to switch - which is what the branch does here.

## Give an explicit cell color its own light and dark values

A color stated on a cell - a `CellStyle` field, or a color argument the cell takes for its own meaning - is kept as written when the appearance changes, just like an explicit `Theme` color. In the declarative DSL, branch on the appearance where the cell is built: the recomposition that follows a night-mode change reaches the row as a content update, so the row is re-bound with the new colors rather than rebuilt, and ids and scroll position stay.

```kotlin
val notifications = remember { mutableStateOf(true) }
val accent = if (isSystemInDarkTheme()) Color(0xFFFFD54F) else Color(0xFFFFBF00)

KsSettingsView {
    Section(header = "General") {
        LabelCell(
            title = "Version",
            valueText = "1.0.0",
            style = CellStyle(titleColor = accent),
        )
        SwitchCell(
            title = "Push notifications",
            isOn = notifications,
            accentColor = accent,
        )
    }
}
```

The cells with a color of their own are `ButtonCell` (`titleColor`), `EntryCell` (`placeholderColor`, `accentColor`), `DatePickerCell` (`androidButtonColor`, `accentColor`) and the switch, checkbox, simple check, radio, picker, number picker and time picker cells (`accentColor`). Left at `Color.Unspecified`, such a color falls through to `CellStyle`, then the theme, then the default of the current appearance.

Outside the declarative DSL nothing recomposes to carry the branch. An activity that keeps `uiMode` in its own `configChanges` is not recreated either, so it hands the store cells carrying the new colors when the appearance changes - see [updates.md](updates.md).

## Override the look of one cell

`CellStyle` overrides the theme for a single cell. Fields you leave out are inherited from the theme.

```kotlin
LabelCell(
    title = "Highlighted",
    style = CellStyle(
        titleColor = Color(0xFFFF9500),
        backgroundColor = Color(0xFFFFF6E5),
        cellHeight = 80.dp,
    ),
)
```

These are all the fields of `CellStyle`, in declaration order. Every one of them can be left unspecified, which means "inherit from the theme" - `Color.Unspecified` for the colors, `null` for the fonts and the sizes.

| Field | Type | Unspecified |
|---|---|---|
| `titleColor` | `Color` | `Color.Unspecified` |
| `titleFont` | `TextStyle?` | `null` |
| `descriptionColor` | `Color` | `Color.Unspecified` |
| `descriptionFont` | `TextStyle?` | `null` |
| `valueTextColor` | `Color` | `Color.Unspecified` |
| `valueTextFont` | `TextStyle?` | `null` |
| `iconSize` | `Dp?` | `null` |
| `iconRadius` | `Dp?` | `null` |
| `cellHeight` | `Dp?` | `null` |
| `hintTextColor` | `Color` | `Color.Unspecified` |
| `hintTextFont` | `TextStyle?` | `null` |
| `backgroundColor` | `Color` | `Color.Unspecified` |
| `accentColor` | `Color` | `Color.Unspecified` |
| `placeholderColor` | `Color` | `Color.Unspecified` |

`placeholderColor` only means something on an `EntryCell`, where it sits between the `placeholderColor` argument of the cell and `Theme.cellPlaceholderColor` in the resolution order.

## Chain style modifiers on a cell

The same overrides are available as modifiers on the `CellHandle` each cell function returns. Chaining preserves the values set earlier and the cell identity.

```kotlin
LabelCell(title = "Name")
    .titleColor(Color(0xFFFF9500))
    .backgroundColor(Color(0xFFFFF6E5))
    .font(TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold))
    .icon(KsImage.Resource(R.drawable.ic_person))
    .cellHeight(60.dp)
```

Available modifiers on a `CellHandle` are `font`, `cellHeight`, `titleColor`, `backgroundColor`, `icon`, `cellID` and `disabled`. `font` changes the title font only, not the hint text. `disabled` is a no-op that returns the cell unchanged - to disable a cell, pass `isEnabled = false` to the cell function. A `SectionHandle` takes `sectionHeader`, `sectionFooter` and `sectionID` instead.

## Choose how sections are separated (Classic separators / Modern rounded boxes)

`KsSettingsViewStyle` chooses how sections are separated. `Classic` only draws hairlines between cells and sections, and cells span the full width of the screen. `Modern` wraps just the cells of each section in a rounded box, with the section header and footer outside the box. Switching styles keeps the contents and ids untouched.

```kotlin
KsSettingsView(style = KsSettingsViewStyle.Modern) {
    Section(header = "General") {
        LabelCell(title = "Version", valueText = "1.0.0")
    }
}
```

## Tune the Modern section box

Four theme attributes describe the box. Left unspecified they fall back to the built-in defaults, and by default no border is drawn.

```kotlin
val boxedTheme = Theme(
    sectionMargin = PaddingValues(start = 16.dp, top = 22.dp, end = 16.dp, bottom = 0.dp),
    sectionCornerRadius = 12.dp,
    sectionBorderWidth = 1.dp,
    sectionBorderColor = Color(0xFFD0D0D0),
)
```

The box covers only the cells of a section: section headers and footers sit outside it, and the screen header and footer are never boxed. In `Classic` only the vertical parts of `sectionMargin` apply, because a classic section spans the full width.

## Control cell height

Height resolves from `CellStyle.cellHeight`, then `Theme.rowHeight`, then the platform minimum of 60dp. The two are written differently: `CellStyle.cellHeight` is a `Dp?` and takes `80.dp`, while `Theme.rowHeight` is a plain `Int` counted in dp, with `-1` meaning unspecified, so it takes `64` and rejects `64.dp`.

With `hasUnevenRows` left at `true` the resolved height is a minimum and cells grow with their content; set it to `false` to pin every cell.

```kotlin
val compactTheme = Theme(rowHeight = 64, hasUnevenRows = false)
```

With a fixed height, content that does not fit is not allowed to grow the cell, so pick a height that fits multi-line text. 60dp is also a floor rather than only a fallback: a smaller value resolved from either source is raised back to 60dp, so `Theme(rowHeight = 40)` still gives 60dp cells.

## Size the icon of a cell

`CellStyle.iconSize` and `iconRadius` set the edge length of the icon frame and the rounding of its corners for one cell; `Theme.cellIconSize` and `cellIconRadius` do the same for the screen. All four are `Dp?`, and the defaults are 24dp square with square corners.

```kotlin
val avatarTheme = Theme(cellIconSize = 32.dp, cellIconRadius = 16.dp)
```

## Put a header and footer on a section

Strings passed to `Section` become the header and footer. The same values can be attached afterwards with modifiers on the handle.

```kotlin
Section(header = "Notifications", footer = "Also check the system settings.") {
    LabelCell(title = "Sound")
}
```

## Put arbitrary Compose in a section header

`Section` takes `headerContent` and `footerContent` for a Composable, next to `header` and `footer` for a string.

```kotlin
Section(
    headerContent = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Notifications, contentDescription = null)
            Text(text = "Notifications", style = MaterialTheme.typography.titleMedium)
        }
    },
    footer = "Also check the system settings.",
) {
    LabelCell(title = "Sound")
}
```

The `sectionHeader` and `sectionFooter` modifiers offer the same choice on the `SectionHandle`, each with a string overload and a Composable one.

```kotlin
Section {
    LabelCell(title = "Sound")
}.sectionHeader {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Notifications, contentDescription = null)
        Text(text = "Notifications", style = MaterialTheme.typography.titleMedium)
    }
}.sectionFooter("Also check the system settings.")
```

A string and a Composable cannot be given for the same position: doing so throws at build time.

A Composable header is not compared by its contents, so changing what is inside the lambda is not detected as a model change on its own. When the new contents also need a different height, drive the screen from a store and call `invalidateAccessoryMeasurement` for that position - see [updates.md](updates.md).

## Put a header and footer on the whole screen

The screen-level header and footer belong to the view, not to the settings tree, and are never covered by the Modern box.

```kotlin
KsSettingsView(
    rootHeader = { Text(text = "Welcome back") },
    rootFooter = { Text(text = "© 2026 MyApp", style = MaterialTheme.typography.bodySmall) },
) {
    Section(header = "General") {
        LabelCell(title = "Version", valueText = "1.0.0")
    }
}
```

## Hide a section header without clearing it

`isHeaderVisible` and `isFooterVisible` hide the accessory while keeping its content, so updates applied while hidden appear when it is shown again. They cannot make an empty header appear.

```kotlin
var showHeaders by remember { mutableStateOf(true) }

Section(header = "General", isHeaderVisible = showHeaders) {
    LabelCell(title = "Version", valueText = "1.0.0")
}
```

## Set a fixed height for a section header

`headerHeight` is a `Double` counted in dp - a third spelling next to `Dp` and `Int` - where `-1.0` means automatic height and a positive value fixes it. It applies to headers only, whether they hold text or a Composable, and content that overflows a fixed height is clipped.

```kotlin
Section(header = "General", headerHeight = 44.0) {
    LabelCell(title = "Version", valueText = "1.0.0")
}
```
