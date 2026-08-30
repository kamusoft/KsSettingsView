---
name: kssettingsview-android
description: Build Android settings screens with KsSettingsView - a Jetpack Compose declarative DSL or an XML View host (the KsSettingsView view), with 12 built-in cells (Label, Command, Button, Switch, Checkbox, Radio, SimpleCheck, Entry, Picker, NumberPicker, TimePicker, DatePicker) plus CustomCell rows holding any Composable, live updates through SettingsRootStore, and Theme / CellStyle styling. Use when adding, changing, or reviewing a settings screen in a Kotlin app that depends on jp.kamusoft:kssettingsview or imports jp.kamusoft.kssettingsview.core, .ui, or .compose.
license: MIT
metadata:
  language: en
  source: https://github.com/kamusoft/KsSettingsView
---

# KsSettingsView for Android

KsSettingsView is a UI library for building settings screens - the list-style screens the iOS Settings app is made of. You declare the screen as a tree of rows (cells) grouped into sections, and that tree is the screen. This Skill covers the Android build, which comes in two forms: a Jetpack Compose declarative DSL and a View host placed in XML (the `KsSettingsView` view). The tree can be written declaratively or driven imperatively from a store.

## What you can do

| What you want to do | Where to look |
|---|---|
| Place a row: label, action, button, switch, checkbox, radio, text field, list picker, number, time, date | [references/cells.md](references/cells.md) |
| Group rows into sections, add icons, descriptions, hints; disable or hide a row | [references/cells.md](references/cells.md) |
| Change the screen after it is on display: insert, remove, move, replace rows, batch updates, direct driving with `SettingsRootDiff` | [references/updates.md](references/updates.md) |
| Keep rows identified across re-evaluations, drive visibility from state, host the screen from XML | [references/updates.md](references/updates.md) |
| Colors, fonts, row height, Classic / Modern list appearance, section boxes, the `Theme` default constants | [references/styling.md](references/styling.md) |
| Section and screen headers / footers, including arbitrary Composables in them | [references/styling.md](references/styling.md) |
| Put any Composable into a row of the list, or define your own cell type with its own view holder | [references/custom-cells.md](references/custom-cells.md) |

## Setup

The library ships as a single artifact that already contains the Compose DSL. Its layers are expressed by Kotlin package name - `jp.kamusoft.kssettingsview.core` (settings tree), `.ui` (cells, `Theme`, `CellStyle`, the View host), and `.compose` (the Composable and the declarative DSL) - so that is what you `import`.

### Take the library into your build

Declare the dependency in the `build.gradle.kts` of your app module.

```kotlin
dependencies {
    implementation("jp.kamusoft:kssettingsview:0.1.0")
}
```

### Versions

These are what your own application module has to satisfy.

| Requirement of your app | Minimum |
|---|---|
| minSdk | 29 |
| compileSdk | 35 |
| Java / Kotlin JVM target | 17 |
| Kotlin | 2.4.10 |
| Compose BOM | 2024.10.01 |

The library itself is built with Gradle 9.5.0 and Android Gradle Plugin 8.13.2. Those are its own toolchain rather than a requirement on your build.

The library puts no prerequisites on the host application. It draws everything inside a context wrapped in its own bundled Material3-derived theme, so any XML theme works - a minimal theme, AppCompat, or a MAUI template default - and any activity works, `ComponentActivity` included; the time and date pickers open everywhere. Two consequences of that self-containment are worth knowing:

- The colors of your app theme (custom colors and dynamic color included) do not reach the library UI. Restyling is done with the library's own `Theme` / `CellStyle` - see [references/styling.md](references/styling.md). Only content you own - a `CustomCell` body, a view passed through `KsAnyView` - still renders with the theme of the host.
- Light and dark switch with the device night mode and the app's uiMode APIs (`AppCompatDelegate.setDefaultNightMode` / `UiModeManager.setApplicationNightMode`). Merely declaring a dark XML theme in the app does not switch the library UI.

## Minimal working example

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import jp.kamusoft.kssettingsview.compose.KsSettingsView
import jp.kamusoft.kssettingsview.compose.LabelCell
import jp.kamusoft.kssettingsview.compose.SwitchCell

@Composable
fun SettingsScreen() {
    val notifications = remember { mutableStateOf(true) }

    KsSettingsView {
        Section(header = "General") {
            LabelCell(title = "Version", valueText = "1.0.0")
            SwitchCell(title = "Push notifications", isOn = notifications)
        }
    }
}
```

`Section` is a member of the DSL scope, so it needs no import. The cell functions are extensions on the section scope and have to be imported one by one. Both overloads of the `KsSettingsView` Composable - DSL and store - accept a Compose `modifier` parameter.

In this re-evaluating `KsSettingsView { ... }` DSL, the cell functions return a `CellHandle` and `Section` returns a `SectionHandle` (the `section` / `cell` of the `settingsRoot` builder that appears in [references/updates.md](references/updates.md) return no handle). A handle is an opaque reference to the row or section that was just placed - you cannot construct or read one - and it exists so that the modifiers described in [references/styling.md](references/styling.md) can be chained onto the call, as in `LabelCell(title = "Name").titleColor(Color.Red)`. Ignoring the return value is normal.

## Reference files

- [references/cells.md](references/cells.md) - one recipe per built-in cell, plus sections, icons, and the fields every row shares.
- [references/updates.md](references/updates.md) - changing a screen that is already on display, row identity, visibility, and hosting from XML.
- [references/styling.md](references/styling.md) - `Theme`, `CellStyle`, style modifiers, list appearance, headers and footers.
- [references/custom-cells.md](references/custom-cells.md) - `CustomCell`, reusable wrappers, and your own cell type with a view holder.
