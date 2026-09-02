---
name: kssettingsview-ios
description: Build iOS settings screens with KsSettingsView - a SwiftUI declarative DSL (KsSettingsView) or a UIKit host (KsSettingsViewController) over the built-in cells (Label, Command, Button, Switch, Checkbox, Radio, SimpleCheck, Entry, Picker, NumberPicker, TimePicker, DatePicker), CustomCell rows holding any SwiftUI view, live updates through SettingsRootStore, and Theme / CellStyle styling. Use when adding, changing, or reviewing a settings screen in a Swift app that depends on KsSettingsViewCore, KsSettingsViewUI, or KsSettingsViewSwiftUI.
license: MIT
metadata:
  language: en
  source: https://github.com/kamusoft/KsSettingsView
---

# KsSettingsView for iOS

KsSettingsView is a UI library for building settings screens - the list-style screens the iOS Settings app is made of. You declare the screen as a tree of rows (cells) grouped into sections, and that tree is the screen. This Skill covers the iOS build, which comes in two forms: a SwiftUI declarative DSL and a UIKit host (`KsSettingsViewController`). The tree can be written declaratively or driven imperatively from a store.

## What you can do

| What you want to do | Where to look |
|---|---|
| Place a row: label, action, button, switch, checkbox, radio, text field, list picker, number, time, date | [references/cells.md](references/cells.md) |
| Group rows into sections, add icons, descriptions, hints; disable or hide a row | [references/cells.md](references/cells.md) |
| Change the screen after it is on display: insert, remove, move, replace rows, batch updates | [references/updates.md](references/updates.md) |
| Keep rows identified across re-evaluations, drive visibility from state, host the screen from UIKit | [references/updates.md](references/updates.md) |
| Express a change as a `SettingsRootDiff`, apply a diff or theme to the controller directly | [references/updates.md](references/updates.md) |
| Colors, fonts, row height, Classic / Modern list appearance, section boxes | [references/styling.md](references/styling.md) |
| Section and screen headers / footers, including arbitrary SwiftUI in them | [references/styling.md](references/styling.md) |
| Put any SwiftUI view into a row of the list, or define your own cell type with its own renderer | [references/custom-cells.md](references/custom-cells.md) |

## Setup

The library is distributed as a Swift package from `https://github.com/kamusoft/KsSettingsView-SPM`. In an Xcode app project, use *File > Add Package Dependencies...*, enter that URL, and add the `KsSettingsView` product. In a SwiftPM package, declare the dependency in the manifest:

```swift
// swift-tools-version: 5.10
import PackageDescription

let package = Package(
    name: "MyApp",
    platforms: [.iOS(.v16)],
    dependencies: [
        .package(url: "https://github.com/kamusoft/KsSettingsView-SPM", from: "0.1.0")
    ],
    targets: [
        .target(
            name: "MyApp",
            dependencies: [
                .product(name: "KsSettingsView", package: "KsSettingsView-SPM")
            ]
        )
    ]
)
```

You link that one product, but you `import` by module name: it bundles three modules, `KsSettingsViewCore` (settings tree), `KsSettingsViewUI` (cells, `Theme`, `CellStyle`, UIKit host), and `KsSettingsViewSwiftUI` (SwiftUI view and declarative DSL). The built-in cell types are covered one by one in [references/cells.md](references/cells.md); `CustomCell` and cell types of your own are in [references/custom-cells.md](references/custom-cells.md).

| Requirement | Minimum |
|---|---|
| Swift tools version | 5.10 |
| iOS deployment target | 16.0 |

## Minimal working example

```swift
import SwiftUI
import KsSettingsViewCore
import KsSettingsViewUI
import KsSettingsViewSwiftUI

struct SettingsScreen: View {
    @State private var notifications = true

    var body: some View {
        KsSettingsView {
            ksSection("General") {
                LabelCell(title: "Version", valueText: "1.0.0")
                SwitchCell(
                    title: "Push notifications",
                    isOn: notifications,
                    onValueChanged: { notifications = $0 }
                )
            }
        }
    }
}
```

`ksSection` is used instead of `Section` so the row builder never collides with `SwiftUI.Section`.

## Reference files

- [references/cells.md](references/cells.md) - one recipe per built-in cell, plus sections, icons, and the fields every row shares.
- [references/updates.md](references/updates.md) - changing a screen that is already on display, row identity, visibility, and UIKit hosting.
- [references/styling.md](references/styling.md) - `Theme`, `CellStyle`, style modifiers, list appearance, headers and footers.
- [references/custom-cells.md](references/custom-cells.md) - `CustomCell`, reusable wrappers, and your own cell type with a renderer.
