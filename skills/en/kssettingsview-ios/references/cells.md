# Cells

Recipes for placing rows in a settings screen. Every example assumes the imports from the minimal example in [SKILL.md](../SKILL.md). UIKit types appear in these examples directly (`UIColor` in `titleColor`, `UIKeyboardType` in `keyboardType`), which needs `import UIKit` in files that do not already get it from `import SwiftUI`.

## Group rows into a section

Rows always live inside a section. `ksSection` takes an optional string header and footer.

```swift
KsSettingsView {
    ksSection("Account", footer: "Signing out keeps local data.") {
        LabelCell(title: "Signed in as", valueText: "taro")
    }
    ksSection {
        LabelCell(title: "App information")
    }
}
```

## Show a read-only value

`LabelCell` displays text and never reacts to taps.

```swift
LabelCell(title: "Storage", valueText: "256 GB")
```

## Run an action or navigate from a row

`CommandCell` reports taps and shows a disclosure indicator unless you pass `hideArrow: true`.

```swift
CommandCell(
    title: "License",
    onTap: { showLicense = true }
)
```

## Put a button in a row

`ButtonCell` never shows a disclosure indicator and centers its title by default. `titleAlignment` takes a `CellTitleAlignment` - `.start`, `.center` or `.end` - and only shows visually on rows that have no `valueText`.

```swift
ButtonCell(
    title: "Sign out",
    titleColor: .systemRed,
    onTap: { signOut() },
    titleAlignment: .start
)
```

## Toggle a boolean value

`SwitchCell` takes the current value and reports the flipped one. You own the state and feed the new value back on the next evaluation.

```swift
@State private var notifications = false

SwitchCell(
    title: "Push notifications",
    isOn: notifications,
    onValueChanged: { notifications = $0 }
)
```

## Check an independent option

`CheckboxCell` is an independent boolean drawn as a checkbox.

```swift
@State private var acceptedTerms = false

CheckboxCell(
    title: "I accept the terms",
    isChecked: acceptedTerms,
    onValueChanged: { acceptedTerms = $0 }
)
```

## Show a lightweight checkmark

`SimpleCheckCell` is also an independent boolean, drawn as a plain checkmark instead of a checkbox.

```swift
@State private var weeklyReport = false

SimpleCheckCell(
    title: "Weekly report",
    isChecked: weeklyReport,
    onValueChanged: { weeklyReport = $0 }
)
```

## Choose one option among rows

`RadioCell` rows that share a `groupId` form one selection. The row is drawn as selected when `value == selectedValue`, and you own `selectedValue`.

```swift
@State private var appearance = "light"

ksSection("Appearance") {
    RadioCell(
        title: "Light",
        groupId: "appearance",
        value: "light",
        selectedValue: appearance,
        onSelected: { appearance = $0 }
    )
    RadioCell(
        title: "Dark",
        groupId: "appearance",
        value: "dark",
        selectedValue: appearance,
        onSelected: { appearance = $0 }
    )
}
```

## Let the user type text

`EntryCell` has a `Binding<String>` initializer that writes back for you. `keyboardType` takes a `UIKeyboardType` directly.

```swift
@State private var nickname = ""

EntryCell(
    title: "Nickname",
    text: $nickname,
    placeholder: "Up to 20 characters",
    maxLength: 20
)
```

For a password field, set `isPassword: true`; for a numeric field, pass `keyboardType: .numberPad`. `placeholderColor` overrides the placeholder text color - left out, the OS placeholder color is used and adapts to dark mode on its own. The screen-wide default is `Theme.cellPlaceholderColor` - see [styling.md](styling.md). `textAlignment` (a `CellTitleAlignment`, default `.end`) aligns the text inside the field. Instead of the binding there is also a callback form, which takes the current `text` as a plain value plus `onTextChanged` - use it when you own the state yourself or drive the tree from a store. In either form the field keeps what the user has typed: a row that does not feed the callback back into `text` is not rolled back by a later redraw, because the row is drawn from the latest cell value at draw time.

## Choose one item from a list

`PickerCell` opens a selection page when the row is tapped. The single-selection form uses `selectedIndex`: tapping a candidate fires the callback once and closes the page - there is no separate confirmation step. Closing the page with Cancel fires nothing, in either selection form.

```swift
@State private var themeIndex: Int? = 0

PickerCell(
    title: "Theme",
    items: ["Light", "Dark", "System"],
    selectedIndex: $themeIndex
)
```

`items` is a list of `PickerItem` - a main `text` plus an optional `subText` second line - and a string array like the one above is a shorthand for it. `pageTitle` overrides the title of the selection page, which otherwise reuses `title`. Instead of the binding there is also a callback form, which takes `selectedIndex` as a plain value plus `onSelectionChanged`. Which shape a cell is in is exposed as `selectionMode` (`PickerSelectionMode`, `.single` or `.multiple`), fixed by the initializer you used.

## Choose several items with an upper limit

The multiple-selection form uses `selectedIndices` and `maxSelectedNumber`. `0` means no limit, and the callback fires once when the user taps the Done button that closes the page. The callback form of this shape is `onMultiSelectionChanged`, which receives the confirmed `Set<Int>`.

```swift
@State private var topics: Set<Int> = [0]

PickerCell(
    title: "Topics",
    items: ["News", "Sports", "Music", "Travel"],
    selectedIndices: $topics,
    maxSelectedNumber: 2
)
```

## Use your own objects as picker candidates

`items` also accepts an array of your own element type (`Sendable`) together with a `displayText` projection. An optional `subText` projection adds a second line to each row of the selection page. The array is snapshotted when the cell is built, selection stays index-based, and `onItemSelected` delivers the original element after the index write-back.

```swift
struct Plan: Sendable { let name: String; let detail: String }

@State private var planIndex: Int? = 0

PickerCell(
    title: "Plan",
    items: plans,
    displayText: { $0.name },
    subText: { $0.detail },
    selectedIndex: $planIndex,
    onItemSelected: { plan in savePlan(plan) }
)
```

The multiple-selection form works the same way with `selectedIndices`, and `onItemsSelected` delivers the confirmed elements in ascending index order.

## Bind the selected object directly

When the element type is also `Equatable`, `selectedItem` binds the selection as the element itself. The initial index is resolved by equality - the first matching index wins, and an element not present in `items` means no selection. Multiple selection has no object binding: bind `selectedIndices` and receive the elements through `onItemsSelected`.

```swift
struct Plan: Sendable, Equatable { let name: String; let detail: String }

@State private var plan: Plan?

PickerCell(
    title: "Plan",
    items: plans,
    displayText: { $0.name },
    selectedItem: $plan
)
```

## Choose a number with a unit

`NumberPickerCell` builds its candidates from `min` to `max` in `step` increments and appends `unit` to each of them.

```swift
@State private var fontSize = 14

NumberPickerCell(
    title: "Font size",
    min: 10,
    max: 30,
    step: 1,
    value: $fontSize,
    unit: "pt"
)
```

`pickerTitle` overrides the title of the number picker; left out, `title` is used.

## Choose a time

`TimePickerCell` edits the time part of a `Foundation.Date`, and `format` controls the text shown on the row.

```swift
@State private var alarm = Date()

TimePickerCell(
    title: "Alarm",
    time: $alarm,
    format: "HH:mm"
)
```

The hour cycle of the picker is decided by `is24Hour` alone (default `true` = 24-hour): `format` only shapes the text on the row, and the device's 24-hour setting is never consulted. For a 12-hour picker pass `is24Hour: false` together with a matching format such as `"h:mm a"` - the library does not validate that the two agree. `pickerTitle` overrides the title of the time picker; left out, `title` is used.

## Choose a date

`DatePickerCell` edits the date part of a `Date`. `uiStyle` takes a `DatePickerUIStyle` and picks the surface (`.wheels` or `.calendar`), and a non-empty `todayText` adds a jump-to-today control.

```swift
@State private var birthday = Date()

DatePickerCell(
    title: "Birthday",
    date: $birthday,
    format: "yyyy/MM/dd",
    uiStyle: .calendar,
    todayText: "Today"
)
```

`minDate` / `maxDate` limit the selectable range, and `pickerTitle` overrides the title of the date picker; left out, `title` is used.

## Add an icon to a row

`icon` takes a `KsImage`: an SF Symbol name (`.systemName(_:)`) or a `UIImage` (`.uiImage(_:)`).

```swift
LabelCell(title: "Storage", icon: .systemName("externaldrive"))
LabelCell(title: "Avatar", icon: .uiImage(avatarImage))
```

The icon is drawn inside a square frame, so rows keep their titles aligned regardless of the glyph width.

## Add description, value and hint to the same row

Every built-in cell accepts `description` (below the title), `valueText` (trailing on the title row), and `hintText` (top right). There are two exceptions: `ButtonCell` has no `description`, and `EntryCell` has no `valueText` because its text field shows the value itself - use `text` there.

Arguments must appear in declaration order, which for the shared fields is `id`, `style`, `title`, `description`, `valueText`, `icon`, `hintText`, then the cell's own fields, then `isEnabled` and `isVisible`.

```swift
LabelCell(
    title: "Storage",
    description: "Internal storage of this device",
    valueText: "256 GB",
    icon: .systemName("externaldrive"),
    hintText: "Updated today"
)
```

When the row is too narrow, the title is kept and `valueText` is truncated.

## Disable a row

Pass `isEnabled: false` at construction. It blocks taps and the embedded control, and swaps the text color for the disabled color. Cells also offer a `.disabled(_:)` modifier of their own - not SwiftUI's `View.disabled(_:)` - but it is a no-op, so it will not disable a row.

```swift
CommandCell(title: "Advanced settings", isEnabled: false)
```

## Hide a row without dropping its value

`isVisible: false` removes the row from the display while the value stays in the model, so an update applied while hidden is visible again when the row returns.

```swift
@State private var showAdvanced = false

ksSection("General") {
    LabelCell(title: "Notifications")
    LabelCell(title: "API key", isVisible: showAdvanced)
}
```

`isVisible` also exists on `ksSection`, where it hides the header, footer and every row of that section.
