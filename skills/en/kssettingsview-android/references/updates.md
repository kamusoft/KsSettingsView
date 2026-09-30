# Updating the screen while it is shown

Recipes for changing a settings screen that is already on display, and for keeping cells identified while the declarative tree is re-evaluated.

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
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import jp.kamusoft.kssettingsview.compose.KsSettingsView
import jp.kamusoft.kssettingsview.compose.settingsRoot
import jp.kamusoft.kssettingsview.core.AccessoryTarget
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SectionAccessory
import jp.kamusoft.kssettingsview.core.SettingsAccessory
import jp.kamusoft.kssettingsview.core.SettingsRoot
import jp.kamusoft.kssettingsview.ui.CellStyle
import jp.kamusoft.kssettingsview.ui.KsSettingsViewDefaults
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

## Add or remove a cell after display

`insertCell` places a cell inside a section, `removeCell` takes the cell id. Indices count hidden cells too, because they are positions in the full model rather than on screen.

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

`insertSection` and `removeSection` do for sections what `insertCell` and `removeCell` do for cells, and `replaceSection` swaps one out while keeping its position. The index is a position in `SettingsRoot.sections`, hidden sections included, and out-of-range values are clamped the same way.

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

An unknown section id changes nothing and notifies nothing, as with the cell operations. Give the replacement the same id as the section it replaces, or the cells underneath it can no longer be addressed by their section.

## Rebuild the whole screen at once

When a change is large enough that patching it cell by cell makes no sense - the user switched account, or the whole screen is driven by a fresh response - hand the store a new tree with `replaceAll`.

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

Ids that appear in both the old and the new tree keep their cells, so reusing them where the cell is conceptually the same avoids a needless rebuild of that cell.

## Replace the contents of one cell

`replaceCell` updates a cell in place, keeping its identity and its view holder. Pass a new cell that carries the same id.

```kotlin
store.replaceCell(
    cellId = "version",
    new = LabelCell(id = "version", title = "Version", valueText = "1.1.0"),
)
```

To change the id itself, remove the cell and insert a new one instead.

## Update several cells in one batch

When one user action changes several cells - a radio group, for instance - send them together so they land in a single state update and a single notification. Calling `replaceCell` in a loop instead is not equivalent: each call schedules its own redraw, and the later calls discard the redraws the earlier ones were still waiting for, so some cells keep showing their old contents.

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

## Move or reorder sections and cells

`moveSection` works on positions in the full section list; `moveCell` finds the cell's own section and reorders it there. Both read `to` as the insertion index after the element has been taken out.

```kotlin
store.moveSection(from = 2, to = 0)
store.moveCell(cellId = "version", to = 0)
```

Moving a cell to a different section is expressed as a remove plus an insert.

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

Root targets are different from section targets: the store does not keep the current root header or footer in `state`. While a view is bound, `updateAccessory` with `RootHeader` or `RootFooter` delivers the value directly to that host, so an update made before first attach or while the view is detached is kept for the next display. Pass `SettingsAccessory.Root(RootAccessory.Text(...))` or the corresponding `View` case. Updates from the old store are ignored after the view is bound to a different store, and one host's `unbind()` does not stop other hosts from receiving the same root update.

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
store.applyTheme(KsSettingsViewDefaults.darkTheme())
```

In the declarative form the `theme` parameter of `KsSettingsView` goes through the same path. The store overload has no `theme` parameter: pass the initial value to `SettingsRootStore(initialTheme = ...)` and change it with `applyTheme`.

This is for themes you choose yourself. Following the device between light and dark needs no call at all: a color left at `Color.Unspecified` - which is what every color of a bare `Theme()` is - is resolved against the appearance when the row is drawn, and the view resolves it again when the night mode changes under it. See [styling.md](styling.md) for what the library fills in and how to name a default set explicitly.

## Switch explicit cell colors when the appearance changes

`applyTheme` covers the theme, but a color stated on a cell - a `CellStyle` field, or a color argument the cell takes for its own meaning, such as `accentColor` - belongs to the settings tree, and is kept as written when the appearance changes. Give it a value per appearance by replacing the cell with one carrying the new color. In an activity that keeps `uiMode` in its own `configChanges` and is therefore not recreated, do that from `onConfigurationChanged`.

```kotlin
override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    val isDark =
        newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    store.replaceCell(
        cellId = "version",
        new = LabelCell(
            id = "version",
            title = "Version",
            valueText = "1.0.0",
            style = CellStyle(titleColor = if (isDark) Color(0xFFFFD54F) else Color(0xFFB26A00)),
        ),
    )
}
```

A replacement that changes nothing but the color reaches the displayed row as a content update, so the row is re-bound rather than rebuilt. When one appearance change touches several cells, send them together with `replaceCells` for the reason given above under batching.

## Keep cells identified across re-evaluations

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

For a static element that needs a meaningful identifier, chain `cellID` or `sectionID`. Do not combine them with a `forEach` key on the same element - pick one source of identity. When both are given, the explicit one wins and the key no longer tracks the element, so a fixed `cellID` inside `forEach` resolves every item to the same id. The string you pass is a hint that drives a stable id, not the final id itself.

```kotlin
KsSettingsView {
    Section(header = "General") {
        LabelCell(title = "App version").cellID("app-version")
    }.sectionID("general")
}
```

A section with neither an explicit id nor a `forEach` key gets an id derived partly from its text header, so rewording the header changes its identity. Give a section whose header text can change a `sectionID`.

A stable id also matters for the calendar dialog of `DatePickerCell` (`uiStyle = Material`): it comes back after a rotation with its selection intact, but only when the cell keeps the same id across the activity being recreated - otherwise it stays closed and writes nothing. The bottom-sheet pickers (Picker, NumberPicker, TimePicker, the Spinner date picker) close on rotation regardless.

## Tell the two kinds of identifier apart

The identifiers of the declarative side and the identifiers a store takes are not the same thing, and mistaking one for the other is the usual reason an update quietly does nothing.

- In the DSL, what you supply - a `forEach` key, a `KsIdentifiable.id`, the string given to `cellID` - is a hint of type `Any`. The DSL derives a stable id from it and puts that derived value into `Cell.id`. The derivation is internal, so `.cellID("app-version")` does not produce the id `"app-version"` and you cannot reproduce the value it does produce.
- A store addresses cells and sections by the `String` id you wrote yourself when you built the tree with `settingsRoot { }` or the `SettingsRoot` / `Section` / cell classes. That is the id `removeCell`, `replaceCell`, `moveCell`, `removeSection` and the rest expect.

The two also never meet at runtime: the `KsSettingsView { ... }` overload creates and owns its store internally and never exposes it, and the `KsSettingsView(store = ...)` overload takes no DSL block. So a screen written with the DSL cannot be driven from a store at all. If you want store operations, build the tree with explicit ids and use the store overload.

## Show and hide cells from state

Toggling `isVisible` rebuilds the set of displayed cells from the full model, instead of reconfiguring cells in place.

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

`bind` applies the current root and theme immediately, and every later change goes through the store. The view keeps up with the store across detach and reattach - a pager page scrolling off screen, for instance - by re-reading the current state, so store changes made while it was detached are not lost. Root header and footer updates are the exception to state replay: the direct root-target delivery described above keeps values sent before attach or during detach. The scroll position comes back too: the view takes an anchor just before it detaches and restores it on the next attach. When the activity is recreated, the view carries the position through its saved state and returns to it, apart from one setup with several views that the scroll recipes below describe; a host you rebuild yourself starts at the top unless you carry the position over. Assigning `view.theme` directly after `bind` only changes the view until the next store notification overwrites it, so once a store is bound the theme belongs to `applyTheme`; `view.theme` is for a view you drive without one.

`unbind()` releases the store: later store changes no longer reach the view, what is displayed stays as it is, and re-attaching the view does not resume the subscription - call `bind` again to follow a store. It is idempotent, so calling it on a view that has no store does nothing.

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

## Scroll to a cell or a section from code

Scrolling from code - taking the user to a section when the screen opens, going back to the top, showing a cell that was just added - goes through a scroll handle, `KsScrollController`, rather than through the store. In Compose, `rememberScrollController()` returns a handle that stays the same across recompositions; pass it as the `scrollController` argument, which both the DSL and the store overload of `KsSettingsView` accept.

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import jp.kamusoft.kssettingsview.compose.ButtonCell
import jp.kamusoft.kssettingsview.compose.KsSettingsView
import jp.kamusoft.kssettingsview.compose.LabelCell
import jp.kamusoft.kssettingsview.compose.cellID
import jp.kamusoft.kssettingsview.compose.rememberScrollController
import jp.kamusoft.kssettingsview.compose.sectionID
import jp.kamusoft.kssettingsview.ui.KsScrollPosition

@Composable
fun TopicsScreen() {
    val scroll = rememberScrollController()
    var topics by remember { mutableStateOf(listOf("News", "Sports")) }

    KsSettingsView(scrollController = scroll) {
        Section(header = "Actions") {
            ButtonCell(title = "Go to topics", onTap = { scroll.scrollToSection("topics") })
            ButtonCell(
                title = "Center the version",
                onTap = { scroll.scrollTo("app-version", position = KsScrollPosition.Center) },
            )
            ButtonCell(
                title = "Add a topic",
                onTap = {
                    val name = "Topic ${topics.size + 1}"
                    topics = topics + name
                    scroll.scrollTo(name)
                },
            )
        }
        Section(header = "Topics") {
            forEach(topics, key = { it }) { topic ->
                LabelCell(title = topic)
            }
        }.sectionID("topics")
        Section(header = "About") {
            LabelCell(title = "App version", valueText = "1.0.0").cellID("app-version")
            ButtonCell(title = "Back to top", onTap = { scroll.scrollToStart(animated = false) })
        }
    }
}
```

The handle has four commands. `scrollTo` targets the row of a cell, `scrollToSection` the range of a section including its header and footer, and `scrollToStart` / `scrollToEnd` the very top and bottom of the content, root header and root footer included. `animated` defaults to `true`; with `false` the list jumps straight to the final position.

`position` (a `KsScrollPosition`, `Start` by default) says where in the visible area the target lands.

| `KsScrollPosition` | Where the target lands |
|---|---|
| `Start` | its top edge at the top of the visible area |
| `Center` | its middle at the middle of the visible area |
| `End` | its bottom edge at the bottom of the visible area |

A position the list cannot reach stops at the end of the scrollable range, and a target taller than the visible area is aligned with `Start` whatever position you pass.

In the DSL overload you point at an element with an identifier you wrote yourself: the string given to `cellID` / `sectionID`, or a `forEach` key (the `id` of a `KsIdentifiable` element included). The derived ids described under "Tell the two kinds of identifier apart" are not what the command takes, and an element that has neither an explicit id nor a key cannot be targeted. When the same value is both an explicit id and a key, the element with the explicit id is chosen. In the store overload you point at the `id` of a cell or section in the store.

A command issued in the same handler right after a state change is resolved against the tree after that change, which is why "Add a topic" above reaches the cell it has just added.

## Scroll a view hosted from XML

The view host has the same entry point as a property. Assign a `KsScrollController` to `scrollController` and the commands of that handle reach the view; the ids are the `id`s of the cells and sections in the bound store. The snippet below runs inside the activity of "Host the screen from XML", as do the activity snippets further down, and the scroll types live in `jp.kamusoft.kssettingsview.ui`.

```kotlin
val controller = KsScrollController()

val settingsView = findViewById<KsSettingsView>(R.id.settings_view)
settingsView.bind(store)
settingsView.scrollController = controller

store.insertCell(
    cell = LabelCell(id = "license", title = "License"),
    sectionId = "general",
    at = 1,
)
controller.scrollTo("license")
```

One handle delivers to the view it was connected to last - connecting it to another view disconnects the previous one. Assigning `null` or another handle disconnects the current one, and commands that have not run yet are dropped. `unbind()` also disconnects and sets `scrollController` back to `null`; a later `bind` does not reconnect it, so assign the handle again. The handle does not keep the view alive, so a view model may hold it for longer than the screen lives: once the view is gone, the handle is simply disconnected.

## Issue scroll commands from a view model

`KsScrollController` implements the interface `KsScrollControlling`, which declares the four commands. Let the code that decides where to scroll depend on the interface, and a test can pass a fake that only records the calls.

```kotlin
import jp.kamusoft.kssettingsview.ui.KsScrollControlling
import jp.kamusoft.kssettingsview.ui.KsScrollPosition

class SettingsActions(private val scroll: KsScrollControlling) {
    fun showDiagnostics() {
        scroll.scrollToSection("diagnostics", position = KsScrollPosition.Center)
    }
}
```

What you connect to the view is the `KsScrollController` itself; `scrollController` takes the class, not the interface.

```kotlin
val controller = KsScrollController()
val actions = SettingsActions(controller)
settingsView.scrollController = controller
```

## Know when a scroll command runs

A command does not run inside the call. It is queued on the view and runs after the store updates made earlier in the same handler have reached the list and the list has been laid out, so "insert a cell, then `scrollToEnd()`" lands on the end that includes the new cell. A command issued before the screen is first shown runs after the first layout, and one issued while the screen is covered by another screen runs after it comes back.

| Situation | What happens |
|---|---|
| Several commands in a row | They run in order; the final position is that of the last command whose target was found |
| The target disappeared before the command ran | That command is skipped and the next ones still run |
| The handle is not connected to any view | Nothing happens, and nothing is thrown |
| The target is hidden, a section has nothing visible, or the id does not exist | Nothing happens, and the position does not change. An unknown id logs a warning while `KsCellRegistry.strictMode` is `true` |

Issue commands from the main thread. While `KsCellRegistry.strictMode` is `true` - its default, which does not follow your build type on its own - a command sent to a connected handle from another thread throws `IllegalStateException`; with `false` it is posted to the main thread instead. Tie the flag to your build type.

```kotlin
KsCellRegistry.strictMode = BuildConfig.DEBUG
```

## Keep the scroll position when the activity is recreated

When the activity is recreated - a rotation, a night-mode change - the view saves the position in its saved state and returns to it after the new view is laid out, with no code on your side. The position is kept by element id, so cells added or removed in between do not send it to a different place. The same applies to the `KsSettingsView` Composable, and returning to a screen inside a Navigation Compose `NavHost` brings back the position it had when you left.

A command issued in `onCreate` of the recreated activity is overridden by that restore, while a command issued later runs after it. To jump somewhere only on the first open, check `savedInstanceState`.

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_settings)

    val settingsView = findViewById<KsSettingsView>(R.id.settings_view)
    settingsView.bind(store)
    settingsView.scrollController = controller
    if (savedInstanceState == null) {
        controller.scrollToSection("diagnostics")
    }
}
```

The saved state is keyed by the view id. When several `KsSettingsView`s in one view hierarchy keep the library's default id - two `KsSettingsView` Composables on one screen, or XML views without an `android:id` - none of them saves or restores the position. Give each XML view an `android:id` of its own.

## Carry the scroll position into a view you rebuild yourself

A view you create again yourself starts at the top. Take the position from the old view with `captureScrollAnchor()` while it is still attached and on screen, and hand it to the new view with `restoreScrollAnchor(anchor)`.

```kotlin
import android.widget.FrameLayout
import jp.kamusoft.kssettingsview.ui.KsSettingsView
import jp.kamusoft.kssettingsview.ui.SettingsRootStore

fun replaceSettingsView(
    container: FrameLayout,
    old: KsSettingsView,
    store: SettingsRootStore,
): KsSettingsView {
    val anchor = old.captureScrollAnchor()
    old.unbind()
    container.removeView(old)

    val replacement = KsSettingsView(container.context)
    container.addView(replacement)
    replacement.bind(store)
    anchor?.let { replacement.restoreScrollAnchor(it) }
    return replacement
}
```

`captureScrollAnchor()` returns `null` when the view has no rows on screen - not attached yet, already detached, or empty. The returned `KsScrollAnchor` names the element at the top of the visible area and its offset; its contents are not readable, and it is `Parcelable`, so it can be put into a `Bundle`. The restore goes through the same queue as the commands, and commands issued after it run after it. If the element is no longer there, the restore does nothing and the view stays where it is.
