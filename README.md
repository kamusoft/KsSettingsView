> **Release status:** Public distribution is being prepared.

# KsSettingsView

[日本語](https://github.com/kamusoft/KsSettingsView/blob/develop/README_ja.md)

## Overview and key features

KsSettingsView is a cross-platform UI library for building list-style settings screens on iOS, Android, and .NET MAUI. Native iOS and Android implementations provide the rendering and interaction model, while the MAUI layer exposes the same screens through XAML and C#.

- Declarative APIs for SwiftUI and Jetpack Compose, plus UIKit and Android View hosts
- Twelve built-in cell types and `CustomCell` for application-defined content
- Live structural and content updates through a store, with two-way values on editable cells
- `Theme` and `CellStyle` customization with Classic and Modern list styles
- Native rendering on both platforms, including when used through .NET MAUI

The public API may introduce breaking changes while the project remains on `0.x` versions.

## Screenshots

| Modern | Classic |
| --- | --- |
| **iOS — Modern**<br>![iOS Modern style](https://raw.githubusercontent.com/kamusoft/KsSettingsView/develop/assets/ios-modern.png) | **iOS — Classic**<br>![iOS Classic style](https://raw.githubusercontent.com/kamusoft/KsSettingsView/develop/assets/ios-classic.png) |
| **Android — Modern**<br>![Android Modern style](https://raw.githubusercontent.com/kamusoft/KsSettingsView/develop/assets/android-modern.png) | **Android — Classic**<br>![Android Classic style](https://raw.githubusercontent.com/kamusoft/KsSettingsView/develop/assets/android-classic.png) |

.NET MAUI wraps the Native implementations, so it renders the same screens shown above.

## Supported platforms

| Platform | Minimum platform | Toolchain used by the library |
| --- | --- | --- |
| iOS Native | iOS 16.0 | Swift tools 5.10 |
| Android Native | Android API 29; compileSdk 35 | Kotlin 2.4.10; AGP 8.13.2; Gradle 9.5.0; JDK 17 |
| .NET MAUI | iOS 16.0; Android API 29 | .NET SDK 10.0.300; `net10.0-ios` / `net10.0-android`; Microsoft.Maui.Controls 10.0.70 |

## Installation

The platform [Agent Skills](https://github.com/kamusoft/KsSettingsView/blob/develop/skills/README.md) contain the detailed setup guidance. This section contains only dependency declarations and prerelease version selection.

### iOS — Swift Package Manager

```swift
dependencies: [
    .package(url: "https://github.com/kamusoft/KsSettingsView-SPM", from: "0.1.0")
]
```

Reference the product as `.product(name: "KsSettingsView", package: "KsSettingsView-SPM")`. To select a prerelease, use its semantic version tag explicitly with `from: "X.Y.Z-beta.N"`, or pin it with `exact: "X.Y.Z-beta.N"`.

### Android — Maven

```kotlin
dependencies {
    implementation("jp.kamusoft:kssettingsview:0.1.0")
}
```

To select a prerelease, use a version such as `X.Y.Z-alpha.N`, `X.Y.Z-beta.N`, or `X.Y.Z-rc.N` in the dependency declaration.

### .NET MAUI — NuGet

```xml
<ItemGroup>
  <PackageReference Include="KsSettingsView.Maui" Version="0.1.0" />
</ItemGroup>
```

To select a prerelease, set `Version` to a value such as `X.Y.Z-beta.N`. When searching without an exact version, enable prerelease results (`--prerelease` with the .NET CLI).

## Minimal code examples

### iOS

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

### Android

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

### .NET MAUI

```xml
<ContentPage xmlns="http://schemas.microsoft.com/dotnet/2021/maui"
             xmlns:x="http://schemas.microsoft.com/winfx/2009/xaml"
             xmlns:ks="clr-namespace:KsSettingsView;assembly=KsSettingsView.Maui"
             x:Class="MyApp.SettingsPage">
  <ks:SettingsView>
    <ks:Section HeaderText="General">
      <ks:LabelCell Title="Version" ValueText="1.0.0" />
      <ks:SwitchCell Title="Push notifications" On="True" />
    </ks:Section>
  </ks:SettingsView>
</ContentPage>
```

Register the handler once in `MauiProgram` before use.

```csharp
using KsSettingsView;
using Microsoft.Maui.Hosting;

namespace MyApp;

public static class MauiProgram
{
    public static MauiApp CreateMauiApp()
    {
        MauiAppBuilder builder = MauiApp.CreateBuilder();

        builder
            .UseMauiApp<App>()
            .AddKsSettingsView();

        return builder.Build();
    }
}
```

See the [.NET MAUI Skill](https://github.com/kamusoft/KsSettingsView/blob/develop/skills/en/kssettingsview-maui/SKILL.md) for the setup.

## Skills

Agent Skills provide task-oriented guidance and API recipes for [iOS](https://github.com/kamusoft/KsSettingsView/blob/develop/skills/en/kssettingsview-ios/SKILL.md), [Android](https://github.com/kamusoft/KsSettingsView/blob/develop/skills/en/kssettingsview-android/SKILL.md), [.NET MAUI](https://github.com/kamusoft/KsSettingsView/blob/develop/skills/en/kssettingsview-maui/SKILL.md), and [migration from AiForms.Maui.SettingsView](https://github.com/kamusoft/KsSettingsView/blob/develop/skills/en/kssettingsview-aiforms-migration/SKILL.md). See the [Skills index](https://github.com/kamusoft/KsSettingsView/blob/develop/skills/README.md) for English and Japanese editions.

## Repository structure

| Directory | Purpose |
| --- | --- |
| `ios/` | iOS Native library and Swift package |
| `android/` | Android Native library |
| `maui/` | .NET MAUI facade and Native bindings |
| `samples/` | Sample applications for the supported platforms |
| `skills/` | User-facing Agent Skills in English and Japanese |
| `assets/` | Images used by the root documentation |
| `kasane/` | Kasane change artifacts, decisions, and concepts |
| `openspec/` | Frozen historical artifacts from the previous OpenSpec workflow; not the current specification |

[Agent development rules](https://github.com/kamusoft/KsSettingsView/blob/develop/AGENTS.md) · [Kasane concepts](https://github.com/kamusoft/KsSettingsView/blob/develop/kasane/concepts/index.md)

## Contributing

We do not accept external pull requests. Please report bugs and propose improvements through GitHub Issues.
Use the provided Issue template so the report contains the information needed for review.
See the [contribution guidelines](https://github.com/kamusoft/KsSettingsView/blob/develop/.github/CONTRIBUTING.md) before submitting an Issue.

## License

KsSettingsView is available under the [MIT License](https://github.com/kamusoft/KsSettingsView/blob/develop/LICENSE).

### Third-party notices

The sample applications use vector drawable icons derived from Google's **Material Symbols Outlined** icon set (Copyright Google LLC), distributed under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0). The icon source is [Google Fonts Icons](https://fonts.google.com/icons).

This notice applies only to icons used by the sample applications; it does not describe a dependency of the KsSettingsView library itself.
