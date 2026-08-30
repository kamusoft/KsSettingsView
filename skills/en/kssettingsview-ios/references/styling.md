# Styling

Recipes for colors, fonts, sizes, list appearance and the supplementary areas around the rows. Every example assumes the imports from the minimal example in [SKILL.md](../SKILL.md).

Values resolve in this order: the meaning-specific value of the cell, then `CellStyle`, then `Theme`, then the UIKit default. A meaning-specific value is a field the cell type owns because of what it means on that cell - `ButtonCell.titleColor`, or the `accentColor` of the selection and input cells - and it wins over the same attribute coming from `CellStyle`. UIKit types are used directly - `UIColor`, `UIFont`, `CGFloat` - which needs `import UIKit` in files that do not already get it from `import SwiftUI`.

## Apply a theme to the whole screen

`Theme` holds the screen-wide defaults. Every parameter has a default, so specify only what you change.

```swift
let warmTheme = Theme(
    separatorColor: UIColor(red: 0.90, green: 0.85, blue: 0.73, alpha: 1.0),
    backgroundColor: UIColor(red: 0.95, green: 0.94, blue: 0.90, alpha: 1.0),
    cellAccentColor: UIColor(red: 1.0, green: 0.75, blue: 0.0, alpha: 1.0),
    cellTitleColor: UIColor(red: 0.8, green: 0.6, blue: 0.0, alpha: 1.0)
)

KsSettingsView {
    ksSection("General") {
        LabelCell(title: "Version", valueText: "1.0.0")
    }
}
.theme(warmTheme)
```

`backgroundColor` paints the canvas behind the list and `cellBackgroundColor` paints the rows; they are separate areas, so setting one does not imply the other.

These are all the fields of `Theme`, in declaration order. Arguments must be passed in this order.

| Group | Field | Type | Unspecified |
|---|---|---|---|
| List | `separatorColor` | `UIColor` | built-in default |
| List | `backgroundColor` | `UIColor` | built-in default |
| List | `cellBackgroundColor` | `UIColor` | `.white` |
| List | `selectedColor` | `UIColor` | built-in default |
| List | `cellAccentColor` | `UIColor` | built-in default |
| List | `disabledTextColor` | `UIColor` | built-in default |
| List | `scrollIndicatorVisible` | `Bool` | `true` |
| Height | `rowHeight` | `Int` | `-1` (automatic) |
| Height | `hasUnevenRows` | `Bool` | `true` |
| Header | `headerTextColor` | `UIColor` | built-in default |
| Header | `headerBackgroundColor` | `UIColor` | built-in default |
| Header | `headerFontSize` | `Double` | `-1` |
| Header | `headerFont` | `UIFont?` | `nil` |
| Header | `headerHeight` | `Double` | `-1` (automatic) |
| Footer | `footerTextColor` | `UIColor` | built-in default |
| Footer | `footerBackgroundColor` | `UIColor` | built-in default |
| Footer | `footerFontSize` | `Double` | `-1` |
| Footer | `footerFont` | `UIFont?` | `nil` |
| Cell defaults | `cellTitleColor` | `UIColor?` | `nil` |
| Cell defaults | `cellTitleFont` | `UIFont?` | `nil` |
| Cell defaults | `cellTitleFontSize` | `Double` | `-1` |
| Cell defaults | `cellValueTextColor` | `UIColor?` | `nil` |
| Cell defaults | `cellValueTextFont` | `UIFont?` | `nil` |
| Cell defaults | `cellDescriptionColor` | `UIColor?` | `nil` |
| Cell defaults | `cellDescriptionFont` | `UIFont?` | `nil` |
| Cell defaults | `cellHintTextColor` | `UIColor?` | `nil` |
| Cell defaults | `cellHintFont` | `UIFont?` | `nil` |
| Cell defaults | `cellPlaceholderColor` | `UIColor?` | `nil` (OS default) |
| Cell defaults | `cellIconSize` | `CGFloat?` | `nil` (24pt) |
| Cell defaults | `cellIconRadius` | `CGFloat?` | `nil` (0pt) |
| Section box | `sectionMargin` | `NSDirectionalEdgeInsets?` | `nil` |
| Section box | `sectionCornerRadius` | `CGFloat?` | `nil` |
| Section box | `sectionBorderWidth` | `CGFloat?` | `nil` (no border) |
| Section box | `sectionBorderColor` | `UIColor?` | `nil` |

`cellTitleFontSize` is an independent size that overrides the point size of whichever title font was resolved, and `headerFontSize` / `footerFontSize` do the same for headers and footers. Any of the three is only applied when it is positive.

## Start from the library defaults

The built-in defaults behind the "unspecified" values above are published as `public static` constants on `Theme`, for putting an attribute back to its default or deriving a new value from one.

| Constant | Default for |
|---|---|
| `defaultSeparatorColor` | separator color |
| `defaultSelectedColor` | selected-row background |
| `defaultAccentColor` | accent color |
| `defaultBackgroundColor` | list background |
| `defaultDisabledTextColor` | disabled text color |
| `defaultHeaderBackgroundColor` | header background |
| `defaultFooterBackgroundColor` | footer background |
| `defaultHeaderTextColor` | header text color |
| `defaultFooterTextColor` | footer text color |
| `defaultHeaderFooterFont` | header / footer font |
| `defaultCellTitleColor` | cell title color |
| `defaultCellTitleFont` | cell title font |
| `defaultCellDescriptionColor` | cell description color |
| `defaultCellDescriptionFont` | cell description font |
| `defaultCellHintFont` | cell hint font |
| `defaultButtonTitleColor` | ButtonCell title color |
| `defaultCellIconSize` | icon size |
| `defaultCellIconRadius` | icon corner radius |

## Override the look of one row

`CellStyle` overrides the theme for a single row. Fields you leave out are `nil` and are inherited from the theme.

```swift
LabelCell(
    style: CellStyle(
        titleColor: .systemOrange,
        cellHeight: 80,
        backgroundColor: .secondarySystemGroupedBackground
    ),
    title: "Highlighted"
)
```

`style` comes before `title` and the rest of the cell's fields, and inside `CellStyle` the arguments follow the declaration order below.

| Field | Type |
|---|---|
| `titleColor` | `UIColor?` |
| `titleFont` | `UIFont?` |
| `descriptionColor` | `UIColor?` |
| `descriptionFont` | `UIFont?` |
| `valueTextColor` | `UIColor?` |
| `valueTextFont` | `UIFont?` |
| `iconSize` | `CGFloat?` |
| `iconRadius` | `CGFloat?` |
| `cellHeight` | `CGFloat?` |
| `hintTextColor` | `UIColor?` |
| `hintTextFont` | `UIFont?` |
| `backgroundColor` | `UIColor?` |
| `accentColor` | `UIColor?` |
| `placeholderColor` | `UIColor?` |

## Chain style modifiers on a cell

In the declarative tree the same overrides are available as modifiers returning a copy of the cell. Chaining preserves the values set earlier and the cell identity.

```swift
LabelCell(title: "Name")
    .titleColor(.systemOrange)
    .backgroundColor(.secondarySystemGroupedBackground)
    .font(.preferredFont(forTextStyle: .headline))
    .icon(.systemName("person"))
    .cellHeight(60)
```

Available modifiers are `font`, `descriptionFont`, `iconSize`, `cellHeight`, `titleColor`, `backgroundColor`, `icon`, `disabled` and `cellID`. `descriptionFont` and `iconSize` are iOS only - the Android DSL has no equivalent - and `disabled` is a no-op on every cell, as noted in [cells.md](cells.md).

## Color the placeholder of entry rows

The placeholder text color of `EntryCell` resolves from `EntryCell.placeholderColor`, then `CellStyle.placeholderColor`, then `Theme.cellPlaceholderColor`, then the OS placeholder color, which adapts to dark mode on its own.

```swift
@State private var nickname = ""

EntryCell(
    title: "Nickname",
    text: $nickname,
    placeholder: "Up to 20 characters",
    placeholderColor: .systemTeal
)
```

## Switch between Classic and Modern list appearance

The style is a `KsSettingsViewStyle`: `.classic` separates rows with flat rules, `.modern` groups each section into a rounded box. Switching styles keeps the contents and identifiers untouched.

```swift
KsSettingsView {
    ksSection("General") {
        LabelCell(title: "Version", valueText: "1.0.0")
    }
}
.style(.modern)
```

## Tune the Modern section box

Four theme attributes describe the box. Left unspecified they fall back to the built-in defaults, and by default no border is drawn.

```swift
let boxedTheme = Theme(
    sectionMargin: NSDirectionalEdgeInsets(top: 22, leading: 16, bottom: 0, trailing: 16),
    sectionCornerRadius: 12,
    sectionBorderWidth: 1,
    sectionBorderColor: .separator
)
```

The box covers only the rows of a section: section headers and footers sit outside it, and the screen header and footer are never boxed. In `.classic` only the vertical parts of `sectionMargin` apply, because a classic section spans the full width.

## Control row height

Height resolves from `CellStyle.cellHeight`, then `Theme.rowHeight`, then the platform minimum of 48pt. With `hasUnevenRows` left at `true` the resolved height is a minimum and rows grow with their content; set it to `false` to pin every row.

```swift
let compactTheme = Theme(rowHeight: 52, hasUnevenRows: false)
```

With a fixed height, content that does not fit is not allowed to grow the row, so pick a height that fits multi-line text.

## Put a header and footer on a section

Strings passed to `ksSection` become the header and footer. The same values can be attached afterwards with modifiers.

```swift
ksSection("Notifications", footer: "Also check the system settings.") {
    LabelCell(title: "Sound")
}
```

## Put arbitrary SwiftUI in a section header

`sectionHeader` and `sectionFooter` also take a view builder.

```swift
ksSection {
    LabelCell(title: "Sound")
}
.sectionHeader {
    HStack {
        Image(systemName: "bell.fill")
        Text("Notifications").font(.headline)
    }
}
.sectionFooter("Also check the system settings.")
```

A view header is not compared by its contents, so changing what is inside the closure is not detected as a model change on its own. To push a new one anyway, own the tree with a store and send it explicitly with `store.updateAccessory(target:accessory:)`, which always emits its update. When only the measured height of the header changed, call `store.invalidateAccessoryMeasurement(target:)` to have that area measured again.

## Put a view into a header from the store or UIKit

Outside the DSL - `store.updateAccessory`, or the `rootHeader` / `rootFooter` of the UIKit controller - a view goes in as a `KsAnyView`, the payload behind the `.view(_:)` accessory case. `KsAnyView.swiftUI { ... }` wraps a SwiftUI view builder, and `KsAnyView.uiKit { ... }` wraps a factory returning a `UIView`.

```swift
store.updateAccessory(
    target: .rootFooter,
    accessory: .root(.view(.swiftUI {
        Text("Signed in as taro").font(.caption)
    }))
)
```

## Put a header and footer on the whole screen

The screen-level header and footer belong to the view, not to the settings tree, and are never covered by the Modern box.

```swift
KsSettingsView {
    ksSection("General") {
        LabelCell(title: "Version", valueText: "1.0.0")
    }
}
.rootHeader("Welcome back")
.rootFooter {
    Text("© 2026 MyApp").font(.caption)
}
```

## Hide a section header without clearing it

`isHeaderVisible` and `isFooterVisible` hide the accessory while keeping its content, so updates applied while hidden appear when it is shown again. They cannot make an empty header appear.

```swift
@State private var showHeaders = true

ksSection("General", isHeaderVisible: showHeaders) {
    LabelCell(title: "Version", valueText: "1.0.0")
}
```

## Set a fixed height for a section header

`headerHeight` takes `-1` for automatic height and a positive value for a fixed one. It applies to headers only, whether they hold text or a view, and content that overflows a fixed height is clipped.

```swift
ksSection("General", headerHeight: 44) {
    LabelCell(title: "Version", valueText: "1.0.0")
}
```
