# Styling

Recipes for colors, fonts, sizes, list appearance and the supplementary areas around the cells. Every example on this page assumes the imports below. The style types and the modifiers are split across two packages: `Theme`, `CellStyle`, `KsImage` and `KsSettingsViewStyle` come from `jp.kamusoft.kssettingsview.ui`, while the modifiers you chain onto a handle come from `jp.kamusoft.kssettingsview.compose`.

```kotlin
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
import jp.kamusoft.kssettingsview.compose.backgroundColor
import jp.kamusoft.kssettingsview.compose.cellHeight
import jp.kamusoft.kssettingsview.compose.font
import jp.kamusoft.kssettingsview.compose.icon
import jp.kamusoft.kssettingsview.compose.sectionFooter
import jp.kamusoft.kssettingsview.compose.sectionHeader
import jp.kamusoft.kssettingsview.compose.titleColor
import jp.kamusoft.kssettingsview.ui.CellStyle
import jp.kamusoft.kssettingsview.ui.KsImage
import jp.kamusoft.kssettingsview.ui.KsSettingsViewStyle
import jp.kamusoft.kssettingsview.ui.Theme
```

`dp` and `sp` are the extension properties that turn a number into a size, so `80.dp` needs `androidx.compose.ui.unit.dp` imported even though the type it produces is `Dp`.

Values resolve in this order: the meaning-specific value of the cell, then `CellStyle`, then `Theme`, then the platform default. Compose types are used directly - `androidx.compose.ui.graphics.Color`, `androidx.compose.ui.text.TextStyle`, `androidx.compose.ui.unit.Dp`. The last step resolves against a Material3-derived theme the library bundles, not against the theme of your app: neither the XML theme nor a Compose `MaterialTheme` recolors the library UI, so everything on this page is the way to restyle it. Light and dark come from the device night mode and the uiMode APIs, since the bundled theme is a DayNight one.

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

| Group | Field | Type | Unspecified |
|---|---|---|---|
| List | `separatorColor` | `Color` | built-in default |
| List | `backgroundColor` | `Color` | built-in default |
| List | `cellBackgroundColor` | `Color` | `Color.White` |
| List | `selectedColor` | `Color` | built-in default |
| List | `cellAccentColor` | `Color` | built-in default |
| List | `disabledTextColor` | `Color` | built-in default |
| List | `scrollIndicatorVisible` | `Boolean` | `true` |
| Height | `rowHeight` | `Int` | `-1` (automatic) |
| Height | `hasUnevenRows` | `Boolean` | `true` |
| Header | `headerTextColor` | `Color` | built-in default |
| Header | `headerBackgroundColor` | `Color` | built-in default |
| Header | `headerFontSize` | `Double` | `-1.0` |
| Header | `headerFont` | `TextStyle?` | `null` |
| Header | `headerHeight` | `Double` | `-1.0` (automatic) |
| Footer | `footerTextColor` | `Color` | built-in default |
| Footer | `footerBackgroundColor` | `Color` | built-in default |
| Footer | `footerFontSize` | `Double` | `-1.0` |
| Footer | `footerFont` | `TextStyle?` | `null` |
| Cell defaults | `cellTitleColor` | `Color?` | `null` |
| Cell defaults | `cellTitleFont` | `TextStyle?` | `null` |
| Cell defaults | `cellTitleFontSize` | `Double` | `-1.0` |
| Cell defaults | `cellValueTextColor` | `Color?` | `null` |
| Cell defaults | `cellValueTextFont` | `TextStyle?` | `null` |
| Cell defaults | `cellDescriptionColor` | `Color?` | `null` |
| Cell defaults | `cellDescriptionFont` | `TextStyle?` | `null` |
| Cell defaults | `cellHintTextColor` | `Color?` | `null` |
| Cell defaults | `cellHintFont` | `TextStyle?` | `null` |
| Cell defaults | `cellIconSize` | `Dp?` | `null` (24dp) |
| Cell defaults | `cellIconRadius` | `Dp?` | `null` (0dp) |
| Section box | `sectionMargin` | `PaddingValues?` | `null` |
| Section box | `sectionCornerRadius` | `Dp?` | `null` |
| Section box | `sectionBorderWidth` | `Dp?` | `null` (no border) |
| Section box | `sectionBorderColor` | `Color?` | `null` |
| Cell defaults | `cellPlaceholderColor` | `Color?` | `null` (OS default) |

`cellTitleFontSize` is an independent size that overrides the point size of whichever title font was resolved, and `headerFontSize` / `footerFontSize` do the same for headers and footers. Any of the three is only applied when it is positive.

## Read the built-in theme defaults

The built-in defaults of the table are published as public constants on the `Theme` companion. Refer to them to go back to a default, or to derive a color from one. Only the two icon constants are `Float` dp values rather than colors; the rest are `Color`s.

| Constant | Default of |
|---|---|
| `DEFAULT_SEPARATOR_COLOR` | separator color |
| `DEFAULT_SELECTED_COLOR` | selected-cell background |
| `DEFAULT_ACCENT_COLOR` | accent color |
| `DEFAULT_BACKGROUND_COLOR` | list background |
| `DEFAULT_DISABLED_TEXT_COLOR` | disabled text color |
| `DEFAULT_HEADER_BACKGROUND_COLOR` | header background |
| `DEFAULT_FOOTER_BACKGROUND_COLOR` | footer background |
| `DEFAULT_HEADER_TEXT_COLOR` | header text color |
| `DEFAULT_FOOTER_TEXT_COLOR` | footer text color |
| `DEFAULT_CELL_TITLE_COLOR` | cell title color |
| `DEFAULT_CELL_DESCRIPTION_COLOR` | cell description color |
| `DEFAULT_BUTTON_TITLE_COLOR` | ButtonCell title color |
| `DEFAULT_CELL_ICON_SIZE_DP_VALUE` | icon size (dp value) |
| `DEFAULT_CELL_ICON_RADIUS_DP_VALUE` | icon corner radius (dp value) |

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

These are all the fields of `CellStyle`, in declaration order. Every one of them is nullable, and `null` means "inherit from the theme".

| Field | Type |
|---|---|
| `titleColor` | `Color?` |
| `titleFont` | `TextStyle?` |
| `descriptionColor` | `Color?` |
| `descriptionFont` | `TextStyle?` |
| `valueTextColor` | `Color?` |
| `valueTextFont` | `TextStyle?` |
| `iconSize` | `Dp?` |
| `iconRadius` | `Dp?` |
| `cellHeight` | `Dp?` |
| `hintTextColor` | `Color?` |
| `hintTextFont` | `TextStyle?` |
| `backgroundColor` | `Color?` |
| `accentColor` | `Color?` |
| `placeholderColor` | `Color?` |

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
