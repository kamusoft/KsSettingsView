# Updating the screen while it is shown

Recipes for changing a settings screen that is already on display, for keeping cells identified while the declarative tree is re-evaluated, and for scrolling the list from code. Unless a snippet carries its own imports, it assumes the imports from the minimal example in [SKILL.md](../SKILL.md).

## Own the settings tree with a store

Use `SettingsRootStore` when you want to change parts of the screen imperatively: large trees, frequent updates, or edits driven from a view model. Hold the store yourself and hand it to `KsSettingsView` together with a `KsSettingsViewStyle` (`.classic` or `.modern`). The store and every operation on it are main actor isolated, so the type that owns it is marked `@MainActor`.

```swift
@MainActor
final class SettingsModel: ObservableObject {
    let generalSectionID: UUID
    let store: SettingsRootStore

    init() {
        let sectionID = UUID()
        let section = KsSettingsViewCore.Section(
            id: sectionID,
            header: .text("General"),
            cells: [LabelCell(title: "Version", valueText: "1.0.0")]
        )
        generalSectionID = sectionID
        store = SettingsRootStore(
            initialRoot: SettingsRoot(sections: [section]),
            initialTheme: Theme()
        )
    }
}

struct SettingsScreen: View {
    @StateObject private var model = SettingsModel()

    var body: some View {
        KsSettingsView(store: model.store, style: .classic)
    }
}
```

Changes applied to the store before the screen appears are still reflected, so the order of "build the store, change it, show it" does not matter.

These are the main public operations of `SettingsRootStore`. The recipes below walk through the everyday ones; the rest follow the same pattern.

| Target | Operations |
|---|---|
| Whole root | `replaceAll(_:)` |
| Section | `insertSection(_:at:)`, `removeSection(sectionID:)`, `moveSection(from:to:)`, `replaceSection(sectionID:new:)` |
| Cell | `insertCell(_:in:at:)`, `removeCell(cellID:)`, `replaceCell(cellID:new:)`, `replaceCells(_:)`, `moveCell(cellID:to:)` |
| Header / footer | `updateAccessory(target:accessory:)`, `invalidateAccessoryMeasurement(target:)` |
| Theme | `applyTheme(_:)` |

The store recipes below are written as members of that `SettingsModel`, so `store` and `generalSectionID` refer to its two properties.

The store keeps hidden sections and cells in its model; an update while hidden is visible when the element returns. A section `updateAccessory` whose `sectionID` is unknown is a no-op and emits no notification. Root header and footer targets are different: they are emitted without a Store-side current value, and the controller owns the displayed `rootHeader` / `rootFooter`.

## Add or remove a cell after display

`insertCell` places a cell inside a section, `removeCell` takes the cell identifier. That identifier is a `KsCellID`, a wrapper built from the cell's `id` (`UUID`) and nothing else: cells with the same `id` are the same cell no matter how their contents changed. Indices count hidden cells too, because they are positions in the full model rather than on screen.

```swift
func appendUser(_ name: String) {
    guard let section = store.root.sections.first else { return }
    store.insertCell(LabelCell(title: name), in: section.id, at: section.cells.count)
}

func removeLastUser() {
    guard let cell = store.root.sections.first?.cells.last else { return }
    store.removeCell(cellID: KsCellID(cell: cell))
}
```

Operations whose target identifier does not exist change nothing and notify nothing.

## Replace the contents of one cell

`replaceCell` updates a cell in place: the cell keeps its identity and its position, and is reconfigured rather than removed and re-inserted. Pass a new cell that carries the same identifier.

```swift
let updated = LabelCell(id: cell.id, title: "Version", valueText: "1.1.0")
store.replaceCell(cellID: KsCellID(cell: cell), new: updated)
```

The new cell may even be of a different type - a `LabelCell` replaced by a `SwitchCell`, say: the cell keeps its identity and position, and the native cell behind it is swapped. To change the identifier itself, remove the cell and insert a new one instead.

`replaceSection` replaces the whole section and may change its header, footer, fixed header height, visibility or cells. Treat it as a full update rather than a lightweight cell edit; use the narrower Cell or accessory operation when you already know the local change.

## Update several cells in one batch

When one user action changes several cells - a radio group, for instance - send them together so they land in a single state update and a single notification.

```swift
store.replaceCells([
    (
        cellID: KsCellID(cell: lightRow),
        new: RadioCell(
            id: lightRow.id,
            title: "Light",
            groupId: "appearance",
            value: "light",
            selectedValue: "dark"
        )
    ),
    (
        cellID: KsCellID(cell: darkRow),
        new: RadioCell(
            id: darkRow.id,
            title: "Dark",
            groupId: "appearance",
            value: "dark",
            selectedValue: "dark"
        )
    )
])
```

Unknown identifiers are skipped, and an empty list does nothing.

## Move or reorder sections and cells

`moveSection` works on positions in the full section list; `moveCell` finds the cell's own section and reorders it there. Both read `to` as the insertion index after the element has been taken out.

```swift
store.moveSection(from: 2, to: 0)
store.moveCell(cellID: KsCellID(cell: cell), to: 0)
```

Moving a cell to a different section is expressed as a remove plus an insert.

## Change a section header or footer after display

`updateAccessory` targets one of the four positions of `AccessoryTarget`: `.rootHeader`, `.rootFooter`, `.sectionHeader(sectionID:)` and `.sectionFooter(sectionID:)`. The value is a `SettingsAccessory`, whose `.section(_:)` case carries a `SectionAccessory` and whose `.root(_:)` case carries a `RootAccessory`; both of those are either `.text(_:)` or `.view(_:)`. Passing `nil` removes the accessory at that position.

```swift
store.updateAccessory(
    target: .sectionHeader(sectionID: generalSectionID),
    accessory: .section(.text("General settings"))
)
```

Root accessories are not part of `SettingsRoot`. Set `rootHeader` / `rootFooter` on the UIKit controller, or target them through the Store update path shown in [styling.md](styling.md); a value delivered before the controller's view loads is retained for that initial display, but it is not restored from a later Store snapshot.

## Switch the theme at runtime

The theme is not part of the settings tree. `applyTheme` changes colors and fonts without touching identifiers or structure, and an identical theme is not re-applied.

```swift
store.applyTheme(darkTheme)
```

In the declarative form, the `.theme(_:)` modifier goes through the same path.

The new theme reaches the cells on display and the text headers and footers, which are recolored in place. Headers and footers holding a view are deliberately left alone - re-binding them would run the view factory again and lose whatever state the hosted view held - so a view accessory that should follow the theme has to be updated by you, with `store.updateAccessory(target:accessory:)`. One exception concerns the screen header and footer: when a theme change alters the resolved section margin (`sectionMargin`), or the number of visible sections goes from zero to non-zero or back, they are rebuilt to re-apply the margin whatever their kind, and a view held there loses its internal state.

`scrollIndicatorVisible` is applied to the main settings list at initial display and on a theme change without rebuilding rows or moving the scroll position. A Picker list captures that value when it opens; wheel-based picker surfaces and lists owned by `CustomCell` content are outside this setting.

`applyTheme` moves the screen-wide defaults only. Colors set explicitly on a cell - through `CellStyle`, or through a color field the cell type owns - are not part of the theme and are left untouched, in this path and when the light / dark appearance changes. To have those follow the appearance, pass a dynamic `UIColor` instead of pushing a new cell (see [styling.md](styling.md)). If you do compute a color yourself, a re-evaluation that changes only that color, on a cell keeping its identifier, is delivered as an in-place content update rather than as a removal plus an insertion - as is `replaceCell` with the same identifier.

## Keep cells identified across re-evaluations

A declarative tree is rebuilt on every evaluation, so dynamic collections need a key. Use the DSL `ForEach`, which takes `Identifiable` elements or an `id:` key path.

```swift
struct Topic: Identifiable {
    let id: UUID
    let name: String
}

KsSettingsView {
    ksSection("Topics") {
        ForEach(topics) { topic in
            LabelCell(title: topic.name)
        }
    }
}
```

Return exactly one element per item; returning several from one item makes them collide on the same identity.

## Name an element explicitly

For a static element that needs a meaningful identifier, use `cellID` or `sectionID`. Do not combine them with a `ForEach` key on the same element - pick one source of identity. If you do combine them, the explicit identifier wins and the key no longer tracks the item; an explicit identifier that does not change per item then resolves every item to the same identity.

A section with neither an explicit identifier nor a key is identified partly by its text header, so changing that header text changes its identity. Give a section whose header text can change a `sectionID`.

```swift
ksSection("General") {
    LabelCell(title: "App version").cellID("app-version")
}
.sectionID("general")
```

## Show and hide cells from state

Toggling `isVisible` rebuilds the set of displayed cells, so the change lands as cells being added and removed rather than as an in-place update of an existing cell.

```swift
@State private var showAdvanced = false

KsSettingsView {
    ksSection("General") {
        LabelCell(title: "Notifications")
        LabelCell(title: "API key", isVisible: showAdvanced)
    }
    ksSection("Diagnostics", isVisible: showAdvanced) {
        LabelCell(title: "Log level", valueText: "debug")
    }
}
```

## Drive the screen from UIKit

`KsSettingsViewController` is a plain `UIViewController`, so it can be pushed, presented, or embedded as a child.

```swift
import UIKit
import KsSettingsViewCore
import KsSettingsViewUI

final class SettingsContainerViewController: UIViewController {
    private let store = SettingsRootStore(
        initialRoot: SettingsRoot(sections: [
            Section(
                header: .text("General"),
                cells: [LabelCell(title: "Version", valueText: "1.0.0")]
            )
        ]),
        initialTheme: Theme()
    )

    override func viewDidLoad() {
        super.viewDidLoad()

        let settings = KsSettingsViewController(store: store, style: .classic)
        settings.rootHeader = .text("Profile")

        addChild(settings)
        settings.view.frame = view.bounds
        settings.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        view.addSubview(settings.view)
        settings.didMove(toParent: self)
    }
}
```

The controller has no public setter for the settings tree: every change goes through the store it was built with.

The controller converges on the Store's current root, accessories and theme when its view loads, including changes made after the controller was created but before `viewDidLoad` completes. `rootHeader` and `rootFooter` are host-owned properties rather than Store state, so reapplying them when a controller is recreated remains the caller's responsibility.

For tests or hosting of your own, `KsSettingsView` (the SwiftUI view) in its store form offers `makeController()`, which builds the backing `KsSettingsViewController` outside a SwiftUI hierarchy. It is for the store form only - calling it on a DSL-built view is a `fatalError` - and a normal SwiftUI screen never needs it.

## Scroll to a cell, a section, or either end

A `KsScrollController` is a handle for one-off scroll commands. Hold it in `@State`, keep passing the same instance, and connect it with the `.scrollController(_:)` root modifier, which works in both the DSL and the store form. In the DSL form a command names an element by the identifier you gave it with `cellID` / `sectionID` or by its `ForEach` key; a static element identified only by its position cannot be targeted, so give the elements you want to reach one of the two. When the same value matches both, the element with the explicit identifier wins.

```swift
struct SettingsScreen: View {
    @State private var scroll = KsScrollController()
    @State private var topics = ["News"]

    var body: some View {
        KsSettingsView {
            ksSection("Actions") {
                ButtonCell(title: "Go to About", onTap: {
                    MainActor.assumeIsolated {
                        scroll.scrollToSection(id: "about")
                    }
                })
                ButtonCell(title: "Add a topic", onTap: {
                    MainActor.assumeIsolated {
                        let topic = "Topic \(topics.count + 1)"
                        topics.append(topic)
                        scroll.scrollTo(id: topic, position: .center)
                    }
                })
            }
            ksSection("Topics") {
                ForEach(topics, id: \.self) { topic in
                    LabelCell(title: topic)
                }
            }
            ksSection("About") {
                LabelCell(title: "Version", valueText: "1.0.0").cellID("version")
            }
            .sectionID("about")
        }
        .scrollController(scroll)
    }
}
```

The command runs after the updates made in the same handler have reached the screen, so appending an item and scrolling to it in one closure lands on the new item.

| Command | Target |
|---|---|
| `scrollTo(id:position:animated:)` | The row of a cell |
| `scrollToSection(id:position:animated:)` | The span of a section: header, visible cells and footer |
| `scrollToStart(animated:)` | The very top of the content, screen header included |
| `scrollToEnd(animated:)` | The very bottom of the content, screen footer included |

`position` is a `KsScrollPosition` and defaults to `.start`; `animated` defaults to `true`.

| `KsScrollPosition` | Aligns |
|---|---|
| `.start` | The top of the target with the top of the visible area |
| `.center` | The middle of the target with the middle of the visible area |
| `.end` | The bottom of the target with the bottom of the visible area |

The visible area excludes the safe area and the list's content insets. A position that cannot be reached stops at the edge of the scrollable range, and a target taller than the visible area is aligned at `.start` whatever you asked for. Commands issued in a row run in the order they were called, and the last one whose target exists decides where the list ends up. A command aimed at a hidden element, a section with nothing visible, or an identifier that does not exist does nothing (an unknown identifier logs a warning in debug builds). An animated command stops as soon as the user starts dragging.

Cell callbacks are `@Sendable` but run on the main thread, which is why the commands above sit inside `MainActor.assumeIsolated`. A view model can hold the handle as `any KsScrollControlling`, the protocol `KsScrollController` conforms to, and swap in a recording implementation in tests; the connection points themselves take a `KsScrollController`.

## Scroll the UIKit host from code

Assign the handle to `scrollController` of `KsSettingsViewController`. Commands then name a cell by its `KsCellID` and a section by its `id`.

```swift
import UIKit
import KsSettingsViewCore
import KsSettingsViewUI

final class TopicsViewController: UIViewController {
    private let topicsSectionID = UUID()
    private let scroll = KsScrollController()
    private lazy var store = SettingsRootStore(
        initialRoot: SettingsRoot(sections: [
            Section(id: topicsSectionID, header: .text("Topics"), cells: [])
        ]),
        initialTheme: Theme()
    )

    override func viewDidLoad() {
        super.viewDidLoad()

        let settings = KsSettingsViewController(store: store, style: .classic)
        settings.scrollController = scroll

        addChild(settings)
        settings.view.frame = view.bounds
        settings.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        view.addSubview(settings.view)
        settings.didMove(toParent: self)
    }

    func addTopic(_ title: String) {
        let cell = LabelCell(title: title)
        let count = store.root.sections.first?.cells.count ?? 0
        store.insertCell(cell, in: topicsSectionID, at: count)
        scroll.scrollTo(id: KsCellID(cell: cell), position: .end)
    }

    func backToTop() {
        scroll.scrollToStart(animated: false)
    }
}
```

A command issued before the view has loaded, or while the controller is not in a window, runs after the next layout. When a controller stays alive underneath a pushed screen, the command is carried out when it comes back into view.

One handle delivers to the screen it was connected to last; connecting it elsewhere cuts the earlier screen off. The handle does not keep the controller alive. Assigning `nil`, calling `disconnectStore()`, or the controller going away disconnects it, after which commands do nothing; `disconnectStore()` also resets `scrollController` to `nil`. Replacing or removing the handle discards the commands still waiting on that screen - in the SwiftUI modifier as well.

## Keep the scroll position when you recreate the UIKit host

The UIKit host does not carry its scroll position over when you build a new controller for the same store. Take it yourself with `captureScrollAnchor()`, which returns an opaque `KsScrollAnchor`, and hand it to the new controller with `restoreScrollAnchor(_:)`. The anchor records which element sits at the top of the visible area, not a pixel offset, so it comes back to the same element even if items were added or removed above it in between.

```swift
let anchor = oldController.captureScrollAnchor()
oldController.disconnectStore()

let newController = KsSettingsViewController(store: store, style: .classic)
if let anchor {
    newController.restoreScrollAnchor(anchor)
}
newController.scrollController = scroll
```

Capture while the old controller is still in its window: once it has been removed, `captureScrollAnchor()` returns `nil`, as it does when there is nothing to record. The restore runs after the new controller's first layout without animation, and a command issued after it runs after the restore, so it decides the final position. If the recorded element no longer exists, the restore does nothing. The SwiftUI `KsSettingsView` keeps its host for as long as the view identity lasts, so it needs none of this.

## Express a change as a SettingsRootDiff

Each store operation above is backed by a `SettingsRootDiff`, a `Hashable` enum with one case per kind of change: `.full`, `.insertSection`, `.removeSection`, `.moveSection`, `.replaceSection`, `.insertCell`, `.removeCell`, `.replaceCell`, `.moveCell` and `.updateAccessory`. `KsSettingsViewController` also exposes `applyDiff(_:)` and `applyTheme(_:)` for applying such a value - or a theme - to the controller directly, with no store involved.

```swift
controller.applyDiff(.removeCell(cellID: KsCellID(cell: cell)))
controller.applyTheme(darkTheme)
```

The direct APIs bypass the store the controller was built with. While a store is connected the store is the source of truth, and combining store operations with direct application is not guaranteed - prefer the store operations, and reach for the direct APIs when you already hold a diff value from elsewhere.
