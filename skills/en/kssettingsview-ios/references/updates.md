# Updating the screen while it is shown

Recipes for changing a settings screen that is already on display, and for keeping cells identified while the declarative tree is re-evaluated. Unless a snippet carries its own imports, it assumes the imports from the minimal example in [SKILL.md](../SKILL.md).

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

## Switch the theme at runtime

The theme is not part of the settings tree. `applyTheme` changes colors and fonts without touching identifiers or structure, and an identical theme is not re-applied.

```swift
store.applyTheme(darkTheme)
```

In the declarative form, the `.theme(_:)` modifier goes through the same path.

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

For a static element that needs a meaningful identifier, use `cellID` or `sectionID`. Do not combine them with a `ForEach` key on the same element - pick one source of identity.

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

For tests or hosting of your own, `KsSettingsView` (the SwiftUI view) in its store form offers `makeController()`, which builds the backing `KsSettingsViewController` outside a SwiftUI hierarchy. It is for the store form only - calling it on a DSL-built view is a `fatalError` - and a normal SwiftUI screen never needs it.

## Express a change as a SettingsRootDiff

Each store operation above is backed by a `SettingsRootDiff`, a `Hashable` enum with one case per kind of change: `.full`, `.insertSection`, `.removeSection`, `.moveSection`, `.replaceSection`, `.insertCell`, `.removeCell`, `.replaceCell`, `.moveCell` and `.updateAccessory`. `KsSettingsViewController` also exposes `applyDiff(_:)` and `applyTheme(_:)` for applying such a value - or a theme - to the controller directly, with no store involved.

```swift
controller.applyDiff(.removeCell(cellID: KsCellID(cell: cell)))
controller.applyTheme(darkTheme)
```

The direct APIs bypass the store the controller was built with. While a store is connected the store is the source of truth, and combining store operations with direct application is not guaranteed - prefer the store operations, and reach for the direct APIs when you already hold a diff value from elsewhere.
