# Updating the screen while it is shown

Recipes for changing a settings screen that is already on display, and for keeping rows identified while the declarative tree is re-evaluated.

Two forms appear on this page and they do not mix in one file. Declarative snippets build the tree with the DSL of `jp.kamusoft.kssettingsview.compose`. Store snippets build it from the cell classes of `jp.kamusoft.kssettingsview.ui` and the model types of `jp.kamusoft.kssettingsview.core`. A DSL function and a cell class share each name - `LabelCell` is both - so a single file cannot import both directly. Keep the two forms in separate files, or import one side under an alias such as `import jp.kamusoft.kssettingsview.ui.LabelCell as UiLabelCell`.

Declarative snippets assume these imports.

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import jp.kamusoft.kssettingsview.compose.KsIdentifiable
import jp.kamusoft.kssettingsview.compose.KsSettingsView
import jp.kamusoft.kssettingsview.compose.LabelCell
import jp.kamusoft.kssettingsview.compose.cellID
import jp.kamusoft.kssettingsview.compose.forEach
import jp.kamusoft.kssettingsview.compose.sectionID
```

Store snippets assume these.

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import jp.kamusoft.kssettingsview.compose.KsSettingsView
import jp.kamusoft.kssettingsview.compose.settingsRoot
import jp.kamusoft.kssettingsview.core.AccessoryTarget
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SectionAccessory
import jp.kamusoft.kssettingsview.core.SettingsAccessory
import jp.kamusoft.kssettingsview.core.SettingsRoot
import jp.kamusoft.kssettingsview.ui.LabelCell
import jp.kamusoft.kssettingsview.ui.RadioCell
import jp.kamusoft.kssettingsview.ui.SettingsRootStore
import jp.kamusoft.kssettingsview.ui.Theme
```

## Own the settings tree with a store

Use `SettingsRootStore` when you want to change parts of the screen imperatively: large trees, frequent updates, or edits driven from a view model. Build the initial tree with the `settingsRoot` builder, hold the store across recompositions, and hand it to the store overload of `KsSettingsView`.

```kotlin
@Composable
fun SettingsScreen() {
    val store = remember {
        SettingsRootStore(
            initialRoot = settingsRoot {
                section(id = "general", header = "General") {
                    cell(LabelCell(id = "version", title = "Version", valueText = "1.0.0"))
                }
            },
            initialTheme = Theme(),
        )
    }

    KsSettingsView(store = store)
}
```

The `settingsRoot` builder is a plain function that takes explicit ids; it is a different scope from the `KsSettingsView { ... }` DSL, which resolves ids on its own. The receiver of the builder is `SettingsRootScope`, and its `section` — along with the `cell` calls inside a section block — returns nothing. The scopes of the re-evaluating DSL are `DSLSettingsRootScope` / `DSLSectionScope`, and only their `Section` and cell functions return a `SectionHandle` / `CellHandle`. Changes applied to the store before the screen appears are still reflected, so the order of "build the store, change it, show it" does not matter.

The current values of the store are exposed as read-only `StateFlow`s: `store.state` holds the current `SettingsRoot` and `store.theme` the current `Theme`. Use them to observe from a view model or to inspect the current structure before an update.

## Add or remove a row after display

`insertCell` places a row inside a section, `removeCell` takes the row id. Indices count hidden rows too, because they are positions in the full model rather than on screen.

```kotlin
store.insertCell(
    cell = LabelCell(id = "license", title = "License"),
    sectionId = "general",
    at = 1,
)
store.removeCell(cellId = "license")
```

Operations whose target id does not exist change nothing and notify nothing, and an out-of-range insertion index is clamped into range.

## Add, remove or replace a whole section after display

`insertSection` and `removeSection` do for sections what `insertCell` and `removeCell` do for rows, and `replaceSection` swaps one out while keeping its position. The index is a position in `SettingsRoot.sections`, hidden sections included, and out-of-range values are clamped the same way.

```kotlin
store.insertSection(
    section = Section(
        id = "diagnostics",
        header = SectionAccessory.Text("Diagnostics"),
        cells = listOf(LabelCell(id = "log-level", title = "Log level", valueText = "debug")),
    ),
    at = 1,
)

store.replaceSection(
    sectionId = "diagnostics",
    new = Section(
        id = "diagnostics",
        header = SectionAccessory.Text("Diagnostics"),
        cells = listOf(LabelCell(id = "log-level", title = "Log level", valueText = "verbose")),
    ),
)

store.removeSection(sectionId = "diagnostics")
```

An unknown section id changes nothing and notifies nothing, as with the row operations. Give the replacement the same id as the section it replaces, or the rows underneath it can no longer be addressed by their section.

## Rebuild the whole screen at once

When a change is large enough that patching it row by row makes no sense - the user switched account, or the whole screen is driven by a fresh response - hand the store a new tree with `replaceAll`.

```kotlin
store.replaceAll(
    SettingsRoot(
        sections = listOf(
            Section(
                id = "general",
                header = SectionAccessory.Text("General"),
                cells = listOf(LabelCell(id = "version", title = "Version", valueText = "1.1.0")),
            ),
        ),
    ),
)
```

Ids that appear in both the old and the new tree keep their rows, so reusing them where the row is conceptually the same avoids a needless rebuild of that row.

## Replace the contents of one row

`replaceCell` updates a row in place, keeping its identity and its view holder. Pass a new cell that carries the same id.

```kotlin
store.replaceCell(
    cellId = "version",
    new = LabelCell(id = "version", title = "Version", valueText = "1.1.0"),
)
```

To change the id itself, remove the row and insert a new one instead.

## Update several rows in one batch

When one user action changes several rows - a radio group, for instance - send them together so they land in a single state update and a single notification. Calling `replaceCell` in a loop instead is not equivalent: each call schedules its own redraw, and the later calls discard the redraws the earlier ones were still waiting for, so some rows keep showing their old contents.

```kotlin
store.replaceCells(
    listOf(
        "appearance-light" to RadioCell(
            id = "appearance-light",
            title = "Light",
            groupId = "appearance",
            value = "light",
            selectedValue = "dark",
        ),
        "appearance-dark" to RadioCell(
            id = "appearance-dark",
            title = "Dark",
            groupId = "appearance",
            value = "dark",
            selectedValue = "dark",
        ),
    ),
)
```

Unknown ids are skipped, and an empty list does nothing.

## Move or reorder sections and rows

`moveSection` works on positions in the full section list; `moveCell` finds the row's own section and reorders it there. Both read `to` as the insertion index after the element has been taken out.

```kotlin
store.moveSection(from = 2, to = 0)
store.moveCell(cellId = "version", to = 0)
```

Moving a row to a different section is expressed as a remove plus an insert.

## Change a section header or footer after display

An accessory is a header or a footer, and a screen has four positions for them: the header and the footer of the whole screen, and the header and the footer of one section. `AccessoryTarget` names which one you mean.

```kotlin
AccessoryTarget.RootHeader
AccessoryTarget.RootFooter
AccessoryTarget.SectionHeader(sectionId = "general")
AccessoryTarget.SectionFooter(sectionId = "general")
```

What you put there is a `SettingsAccessory`, which only says which of the two kinds follows: `SettingsAccessory.Root` wraps a `RootAccessory` for the two screen-level positions, `SettingsAccessory.Section` wraps a `SectionAccessory` for the two section-level ones. `RootAccessory` and `SectionAccessory` are separate types with the same two cases - `Text(value)` for a string and `View(view)` for a `KsAnyView`. `KsAnyView` itself is a two-way choice: `KsAnyView.Compose` wraps a `@Composable` lambda and `KsAnyView.AndroidView` wraps a `(Context) -> View` factory.

```kotlin
store.updateAccessory(
    target = AccessoryTarget.SectionHeader(sectionId = "general"),
    accessory = SettingsAccessory.Section(SectionAccessory.Text("General settings")),
)
```

Passing `null` as the accessory removes what is at that position, and an unknown section id is a no-op.

## Remeasure a header whose Composable changed size

A `View` accessory is compared by identity, not by what it draws, so redrawing a Composable header with taller contents does not tell the list its height changed - see [styling.md](styling.md) for the same caveat on the declarative side. `invalidateAccessoryMeasurement` asks for that one position to be measured again.

```kotlin
store.invalidateAccessoryMeasurement(
    target = AccessoryTarget.SectionHeader(sectionId = "general"),
)
```

This is a one-shot notification rather than stored state: if nothing is attached to the store at that moment, it is dropped rather than replayed later.

## Switch the theme at runtime

The theme is not part of the settings tree. `applyTheme` changes colors and fonts without touching ids or structure, and an identical theme is not re-applied.

```kotlin
store.applyTheme(darkTheme)
```

In the declarative form the `theme` parameter of `KsSettingsView` goes through the same path. The store overload has no `theme` parameter: pass the initial value to `SettingsRootStore(initialTheme = ...)` and change it with `applyTheme`.

## Keep rows identified across re-evaluations

A declarative tree is rebuilt on every recomposition, so dynamic collections need a key. Pass one to the DSL `forEach` as a lambda that returns something distinguishing per item.

```kotlin
KsSettingsView {
    Section(header = "Topics") {
        forEach(topics, key = { topic -> topic.name }) { topic ->
            LabelCell(title = topic.name)
        }
    }
}
```

The key lambda can be dropped when the element type implements `KsIdentifiable`, whose single member is `val id: Any`. Any type works there - `Int`, `String`, a value class - because the DSL only ever compares keys with each other.

```kotlin
data class Topic(override val id: Int, val name: String) : KsIdentifiable

KsSettingsView {
    Section(header = "Topics") {
        forEach(topics) { topic ->
            LabelCell(title = topic.name)
        }
    }
}
```

Return exactly one element per item; returning several from one item makes them collide on the same identity.

## Name an element explicitly

For a static element that needs a meaningful identifier, chain `cellID` or `sectionID`. Do not combine them with a `forEach` key on the same element - pick one source of identity. The string you pass is a hint that drives a stable id, not the final id itself.

```kotlin
KsSettingsView {
    Section(header = "General") {
        LabelCell(title = "App version").cellID("app-version")
    }.sectionID("general")
}
```

A stable id also matters for the calendar dialog of `DatePickerCell` (`uiStyle = Material`): it comes back after a rotation with its selection intact, but only when the row keeps the same id across the activity being recreated - otherwise it stays closed and writes nothing. The bottom-sheet pickers (Picker, NumberPicker, TimePicker, the Spinner date picker) close on rotation regardless.

## Tell the two kinds of identifier apart

The identifiers of the declarative side and the identifiers a store takes are not the same thing, and mistaking one for the other is the usual reason an update quietly does nothing.

- In the DSL, what you supply - a `forEach` key, a `KsIdentifiable.id`, the string given to `cellID` - is a hint of type `Any`. The DSL derives a stable id from it and puts that derived value into `Cell.id`. The derivation is internal, so `.cellID("app-version")` does not produce the id `"app-version"` and you cannot reproduce the value it does produce.
- A store addresses rows and sections by the `String` id you wrote yourself when you built the tree with `settingsRoot { }` or the `SettingsRoot` / `Section` / cell classes. That is the id `removeCell`, `replaceCell`, `moveCell`, `removeSection` and the rest expect.

The two also never meet at runtime: the `KsSettingsView { ... }` overload creates and owns its store internally and never exposes it, and the `KsSettingsView(store = ...)` overload takes no DSL block. So a screen written with the DSL cannot be driven from a store at all. If you want store operations, build the tree with explicit ids and use the store overload.

## Show and hide rows from state

Toggling `isVisible` rebuilds the set of displayed rows from the full model, instead of reconfiguring rows in place.

```kotlin
var showAdvanced by remember { mutableStateOf(false) }

KsSettingsView {
    Section(header = "General") {
        LabelCell(title = "Notifications")
        LabelCell(title = "API key", isVisible = showAdvanced)
    }
    Section(header = "Diagnostics", isVisible = showAdvanced) {
        LabelCell(title = "Log level", valueText = "debug")
    }
}
```

## Host the screen from XML

`jp.kamusoft.kssettingsview.ui.KsSettingsView` is a `FrameLayout`, so it goes into a layout like any other view.

```xml
<jp.kamusoft.kssettingsview.ui.KsSettingsView
    android:id="@+id/settings_view"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

Connect it to a store with `bind`. The things that are not part of the settings tree are properties of the view: `style` picks Classic or Modern, `rootHeader` and `rootFooter` are the screen-level accessories, and `theme` holds the current `Theme`.

```kotlin
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import jp.kamusoft.kssettingsview.core.RootAccessory
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SectionAccessory
import jp.kamusoft.kssettingsview.core.SettingsRoot
import jp.kamusoft.kssettingsview.ui.KsSettingsView
import jp.kamusoft.kssettingsview.ui.KsSettingsViewStyle
import jp.kamusoft.kssettingsview.ui.LabelCell
import jp.kamusoft.kssettingsview.ui.SettingsRootStore

class SettingsActivity : AppCompatActivity() {

    private val store = SettingsRootStore(
        initialRoot = SettingsRoot(
            sections = listOf(
                Section(
                    id = "general",
                    header = SectionAccessory.Text("General"),
                    cells = listOf(
                        LabelCell(id = "version", title = "Version", valueText = "1.0.0"),
                    ),
                ),
            ),
        ),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        findViewById<KsSettingsView>(R.id.settings_view).apply {
            style = KsSettingsViewStyle.Classic
            rootHeader = RootAccessory.Text("Profile")
            bind(store)
        }
    }
}
```

`bind` applies the current root and theme immediately, and every later change goes through the store. The view keeps up with the store across detach and reattach - a pager page scrolling off screen, for instance - by re-reading the current state, though the scroll position is not restored. Assigning `view.theme` directly after `bind` only changes the view until the next store notification overwrites it, so once a store is bound the theme belongs to `applyTheme`; `view.theme` is for a view you drive without one.

## Drive the view directly with diffs, without a store

Where you do not want to bring in a store - an external binding, a preview - the view can be driven by handing a `SettingsRootDiff` straight to its `applyDiff`. `SettingsRootDiff` is a sealed interface that names where in the settings tree a change applies and what kind of change it is, and its cases correspond one-to-one to the public store operations.

| Case | Change |
|---|---|
| `Full` | replace the whole tree |
| `InsertSection` | add a section at an index |
| `RemoveSection` | remove a section by id |
| `MoveSection` | reorder sections |
| `ReplaceSection` | replace a whole section by id |
| `InsertCell` | add a cell at an index of a section |
| `RemoveCell` | remove a cell by id |
| `ReplaceCell` | swap the contents of the cell with the same id |
| `MoveCell` | reorder a cell within its section |
| `UpdateAccessory` | add, update or remove a header / footer |

Feed the first frame with `view.applyDiff(SettingsRootDiff.Full(root))`, and use `view.theme` directly only in this setup. The view also has its own `invalidateAccessoryMeasurement(target)`, which requests the same remeasurement as the store operation of the same name in this setup. Do not combine this direct driving with `bind(store)` on the same view - a normal app screen uses a store.
