# Cells

Recipes for placing cells in a settings screen. Every example on this page assumes the imports below. Cell functions live in `jp.kamusoft.kssettingsview.compose` and have to be imported one by one; the value types they take (`KsImage`, `DatePickerUIStyle`) live in `jp.kamusoft.kssettingsview.ui`, not in `compose`.

```kotlin
import android.text.InputType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import java.time.LocalDate
import java.time.LocalTime
import jp.kamusoft.kssettingsview.compose.ButtonCell
import jp.kamusoft.kssettingsview.compose.CheckboxCell
import jp.kamusoft.kssettingsview.compose.CommandCell
import jp.kamusoft.kssettingsview.compose.DatePickerCell
import jp.kamusoft.kssettingsview.compose.EntryCell
import jp.kamusoft.kssettingsview.compose.KsSettingsView
import jp.kamusoft.kssettingsview.compose.LabelCell
import jp.kamusoft.kssettingsview.compose.NumberPickerCell
import jp.kamusoft.kssettingsview.compose.PickerCell
import jp.kamusoft.kssettingsview.compose.RadioCell
import jp.kamusoft.kssettingsview.compose.SimpleCheckCell
import jp.kamusoft.kssettingsview.compose.SwitchCell
import jp.kamusoft.kssettingsview.compose.TimePickerCell
import jp.kamusoft.kssettingsview.ui.DatePickerUIStyle
import jp.kamusoft.kssettingsview.ui.KsImage
```

`getValue` and `setValue` are what make `var x by remember { mutableStateOf(...) }` compile; without them the delegate form fails to resolve. Snippets that place cells are fragments of a `KsSettingsView { Section { ... } }` body inside a `@Composable` function, unless they show that frame themselves.

## Group cells into a section

Cells always live inside a section. `Section` takes an optional string header and footer.

```kotlin
KsSettingsView {
    Section(header = "Account", footer = "Signing out keeps local data.") {
        LabelCell(title = "Signed in as", valueText = "taro")
    }
    Section {
        LabelCell(title = "App information")
    }
}
```

## Show a read-only value

`LabelCell` displays text and never reacts to taps.

```kotlin
LabelCell(title = "Storage", valueText = "256 GB")
```

## Run an action or navigate from a cell

`CommandCell` reports taps and shows a disclosure indicator - the chevron at the trailing edge of the cell that marks it as leading somewhere - unless you pass `hideArrow = true`.

```kotlin
CommandCell(
    title = "License",
    onTap = { showLicense() },
)
```

## Put a button in a cell

`ButtonCell` never shows a disclosure indicator and centers its title by default. `titleAlignment` takes a `CellTitleAlignment` (`START` / `CENTER` / `END`) from `jp.kamusoft.kssettingsview.core`, and only shows visually on cells that have no `valueText`.

```kotlin
ButtonCell(
    title = "Sign out",
    titleColor = Color.Red,
    onTap = { signOut() },
)
```

## Toggle a boolean value

`SwitchCell` has a two-way overload: hand it a `MutableState<Boolean>` and it reads the current value and writes the flipped one back.

```kotlin
val notifications = remember { mutableStateOf(false) }

SwitchCell(title = "Push notifications", isOn = notifications)
```

The other overload takes a plain `Boolean` and an `onValueChanged` callback, for state you hold as a plain value.

```kotlin
var pushEnabled by remember { mutableStateOf(false) }

SwitchCell(
    title = "Push notifications",
    isOn = pushEnabled,
    onValueChanged = { pushEnabled = it },
)
```

## Check an independent option

`CheckboxCell` is an independent boolean drawn as a checkbox. It takes the current value and reports the flipped one, so you own the state.

```kotlin
var acceptedTerms by remember { mutableStateOf(false) }

CheckboxCell(
    title = "I accept the terms",
    isChecked = acceptedTerms,
    onValueChanged = { acceptedTerms = it },
)
```

## Show a lightweight checkmark

`SimpleCheckCell` is also an independent boolean, drawn as a plain checkmark instead of a checkbox.

```kotlin
var weeklyReport by remember { mutableStateOf(false) }

SimpleCheckCell(
    title = "Weekly report",
    isChecked = weeklyReport,
    onValueChanged = { weeklyReport = it },
)
```

## Choose one option among cells

`RadioCell` cells that share a `groupId` form one selection. The cell is drawn as selected when `value == selectedValue`, and you own `selectedValue`.

```kotlin
var appearance by remember { mutableStateOf("light") }

Section(header = "Appearance") {
    RadioCell(
        title = "Light",
        groupId = "appearance",
        value = "light",
        selectedValue = appearance,
        onSelected = { appearance = it },
    )
    RadioCell(
        title = "Dark",
        groupId = "appearance",
        value = "dark",
        selectedValue = appearance,
        onSelected = { appearance = it },
    )
}
```

Tapping the cell that is already selected does not fire `onSelected` again.

## Let the user type text

`EntryCell` has a `MutableState<String>` overload that writes back for you. `keyboardType` takes an `android.text.InputType` constant.

```kotlin
val nickname = remember { mutableStateOf("") }

EntryCell(
    title = "Nickname",
    text = nickname,
    placeholder = "Up to 20 characters",
    maxLength = 20,
)
```

For a password field pass `isPassword = true`; for a numeric field pass `keyboardType = InputType.TYPE_CLASS_NUMBER`. `textAlignment` aligns the entered text with a `CellTitleAlignment` (`START` / `CENTER` / `END`) and defaults to `END`. `placeholderColor` colors the placeholder text; left unspecified it resolves through `CellStyle.placeholderColor`, then `Theme.cellPlaceholderColor`, and finally the OS default, which follows dark mode on its own.

While the field has focus it owns its own text: content updates for the same cell do not replace what is being typed, and the cell re-syncs with the last supplied value when focus is lost. So if you use the callback overload - a `String` for `text` and `onTextChanged` for the change - feed the new value back into the cell, or the field snaps back on blur. Enter on a single-line field closes the keyboard and keeps the focus; include `InputType.TYPE_TEXT_FLAG_MULTI_LINE` in `keyboardType` if you want Enter to insert a line break instead.

## Choose one item from a list

`PickerCell` opens a bottom sheet when the cell is tapped. The single-selection overload takes a `MutableState<Int?>`; there is no confirm button, and tapping a candidate writes the value back and closes the sheet right away.

```kotlin
val themeIndex = remember { mutableStateOf<Int?>(0) }

PickerCell(
    title = "Theme",
    items = listOf("Light", "Dark", "System"),
    selectedIndex = themeIndex,
)
```

`pageTitle` sets the title of the sheet; left unspecified, the `title` of the cell is used.

## Choose one object from a list

The candidates do not have to be strings. A generic overload takes any element list plus a `displayText` projection, and `subText` adds a second line under each candidate in the sheet - candidates without one stay single-line. `onItemSelected` hands back the chosen element itself. The element list is copied when the cell is built, so later changes to the original collection are picked up by supplying a new list, not by mutating in place.

```kotlin
data class Plan(val name: String, val detail: String)

val plans = listOf(
    Plan(name = "Free", detail = "Up to 1 device"),
    Plan(name = "Pro", detail = "Unlimited devices"),
)
val planIndex = remember { mutableStateOf<Int?>(0) }

PickerCell(
    title = "Plan",
    items = plans,
    displayText = { it.name },
    subText = { it.detail },
    selectedIndex = planIndex,
    onItemSelected = { plan -> applyPlan(plan) },
)
```

The cell itself shows only the `displayText` of the selection, never the `subText`. Instead of an index state, single selection can also bind the element directly: `selectedItem` takes a `MutableState<T?>`, resolves it to the first equal candidate when the cell is built (an element not in the list means no selection), and writes the chosen element back.

```kotlin
val plan = remember { mutableStateOf<Plan?>(null) }

PickerCell(
    title = "Plan",
    items = plans,
    displayText = { it.name },
    selectedItem = plan,
)
```

## Choose several items with an upper limit

The multiple-selection overload takes a `MutableState<Set<Int>>` and `maxSelectedNumber`. `0` means no limit. Unlike the single-selection sheet, this one keeps a pending selection and shows an OK button, and it writes back once, on confirmation.

```kotlin
val topics = remember { mutableStateOf(setOf(0)) }

PickerCell(
    title = "Topics",
    items = listOf("News", "Sports", "Music", "Travel"),
    selectedIndices = topics,
    maxSelectedNumber = 2,
)
```

Dismissing the sheet without confirming - cancel, back, outside tap, swipe down on the handle - discards the pending selection.

The object overload exists here too: pass any element list with `displayText` (and `subText` if wanted), and `onItemsSelected` receives the confirmed elements in ascending index order. The written-back state stays a `Set<Int>` - there is no element-typed state for multiple selection.

When you build a tree for a store with the `PickerCell` class of `jp.kamusoft.kssettingsview.ui` (see [updates.md](updates.md)), the candidates are a list of `PickerItem` (primary `text` plus an optional `subText`), and single versus multiple is switched with `selectionMode` (`PickerSelectionMode.Single` / `Multiple`). The callbacks are `onSelectionChanged` for single and `onMultiSelectionChanged` for multiple selection. Each DSL overload sets these from its argument combination, so the declarative side never specifies them directly.

## Choose a number with a unit

`NumberPickerCell` builds its candidates from `min` to `max` in `step` increments and appends `unit` to each of them.

```kotlin
val fontSize = remember { mutableStateOf(14) }

NumberPickerCell(
    title = "Font size",
    value = fontSize,
    min = 10,
    max = 30,
    step = 1,
    unit = "pt",
)
```

`pickerTitle` sets the title of the sheet; left unspecified, the `title` of the cell is used. `TimePickerCell` and `DatePickerCell` take the same parameter.

## Choose a time

`TimePickerCell` edits a `java.time.LocalTime`. Tapping the cell opens a bottom sheet with hour and minute wheels, and the value is written back once, on confirmation - closing the sheet any other way discards the change. `format` only controls the text shown on the cell.

```kotlin
val alarm = remember { mutableStateOf(LocalTime.of(7, 0)) }

TimePickerCell(
    title = "Alarm",
    time = alarm,
    format = "HH:mm",
)
```

Whether the sheet counts hours as 0-23 or as 1-12 with an AM/PM wheel is decided by `is24Hour` alone; `true`, the default, is 24-hour. Neither `format` nor the 24-hour setting of the device takes part, so the same cell opens the same sheet on every device - and keeping `format` consistent with `is24Hour` is on you. In the 12-hour sheet the AM/PM labels and the wheel order follow the device locale.

```kotlin
TimePickerCell(
    title = "Bedtime",
    time = alarm,
    format = "h:mm a",
    is24Hour = false,
)
```

## Choose a date

`DatePickerCell` edits a `java.time.LocalDate`. `uiStyle` picks the surface - `DatePickerUIStyle.Material` for a calendar dialog that also offers a text-input mode, `DatePickerUIStyle.Spinner` for a bottom sheet with three wheels - and a non-empty `todayText` adds a jump-to-today control to either surface. Jumping moves the selection without confirming it, and does nothing when today is outside the allowed range.

```kotlin
val birthday = remember { mutableStateOf(LocalDate.of(1990, 1, 1)) }

DatePickerCell(
    title = "Birthday",
    date = birthday,
    format = "yyyy/MM/dd",
    uiStyle = DatePickerUIStyle.Material,
    todayText = "Today",
)
```

Here too the value is written back only on confirmation; every other way of closing discards the change. The calendar dialog survives a rotation with its selection intact as long as the cell keeps a stable id across the activity being recreated - see [updates.md](updates.md); the Spinner sheet, like the other bottom sheets, closes on rotation without reporting anything. `androidButtonColor` recolors only the header controls (confirm and cancel) of the `Spinner` sheet and has no effect on the `Material` dialog.

`minDate` and `maxDate` restrict what can be chosen; either may be given on its own. A current value outside the range is presented clamped to the nearest bound.

```kotlin
DatePickerCell(
    title = "Appointment",
    date = birthday,
    minDate = LocalDate.of(2026, 1, 1),
    maxDate = LocalDate.of(2026, 12, 31),
)
```

## Add an icon to a cell

`icon` takes a `KsImage`: a drawable resource id or a `Drawable` instance.

```kotlin
LabelCell(title = "Storage", icon = KsImage.Resource(R.drawable.ic_storage))
LabelCell(title = "Avatar", icon = KsImage.Drawable(avatarDrawable))
```

The icon is drawn inside a square frame, so cells keep their titles aligned regardless of the glyph width. `KsImage.SystemName` exists for symmetry with iOS and resolves to no icon here.

## Add description, value and hint to the same cell

Every built-in cell accepts `description` (below the title), `valueText` (trailing on the title row), and `hintText` (top right). There are two exceptions: `ButtonCell` has no `description`, and `EntryCell` has no `valueText` because the text field itself shows the value - use `text` there.

```kotlin
LabelCell(
    title = "Storage",
    description = "Internal storage of this device",
    valueText = "256 GB",
    hintText = "Updated today",
    icon = KsImage.Resource(R.drawable.ic_storage),
)
```

When the cell is too narrow, the title is kept and `valueText` is truncated.

## Disable a cell

Pass `isEnabled = false` at construction. It blocks taps and the embedded control, and swaps the text color for the disabled color. There is also a `disabled(...)` modifier on the handle, but it is deliberately a no-op that returns the cell unchanged, so it is not an alternative to the constructor argument.

```kotlin
CommandCell(title = "Advanced settings", isEnabled = false)
```

## Hide a cell without dropping its value

`isVisible = false` removes the cell from the display while the value stays in the model, so an update applied while hidden is visible again when the cell returns.

```kotlin
var showAdvanced by remember { mutableStateOf(false) }

Section(header = "General") {
    LabelCell(title = "Notifications")
    LabelCell(title = "API key", isVisible = showAdvanced)
}
```

`isVisible` also exists on `Section`, where it hides the header, footer and every cell of that section.
