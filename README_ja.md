# KsSettingsView

[English](https://github.com/kamusoft/KsSettingsView/blob/main/README.md)

## 概要と主な特徴

KsSettingsViewは、iOS、Android、.NET MAUIでリスト形式の設定画面を構築するためのクロスプラットフォームUIライブラリです。iOSとAndroidのNative実装が描画と操作モデルを担い、MAUI層は同じ画面をXAMLとC#から利用できる形で公開します。

- SwiftUIとJetpack Composeの宣言的API、およびUIKitとAndroid Viewのhost
- ラベル・スイッチ・テキスト入力・Pickerなどの組み込みCellと、アプリケーション独自の内容を置ける`CustomCell`
- Storeによる構造・内容の動的更新と、編集可能なCellの双方向値反映
- `Theme`と`CellStyle`によるカスタマイズ、およびClassicとModernのリストstyle
- .NET MAUIから利用する場合も含めた、両platformでのNative描画

バージョンが`0.x`の間は、公開APIに破壊的変更が入る可能性があります。

## スクリーンショット

| Modern | Classic |
| --- | --- |
| **iOS — Modern**<br>![iOS Modernスタイル](https://raw.githubusercontent.com/kamusoft/KsSettingsView/develop/assets/ios-modern.png) | **iOS — Classic**<br>![iOS Classicスタイル](https://raw.githubusercontent.com/kamusoft/KsSettingsView/develop/assets/ios-classic.png) |
| **Android — Modern**<br>![Android Modernスタイル](https://raw.githubusercontent.com/kamusoft/KsSettingsView/develop/assets/android-modern.png) | **Android — Classic**<br>![Android Classicスタイル](https://raw.githubusercontent.com/kamusoft/KsSettingsView/develop/assets/android-classic.png) |

.NET MAUIはNative実装をラップするため、上記と同じ画面を描画します。

## 対応プラットフォーム

| プラットフォーム | 最低platform | ライブラリが使用するtoolchain |
| --- | --- | --- |
| iOS Native | iOS 16.0 | Swift tools 5.10 |
| Android Native | Android API 29、compileSdk 35 | Kotlin 2.4.10、AGP 8.13.2、Gradle 9.5.0、JDK 17 |
| .NET MAUI | iOS 16.0、Android API 29 | .NET SDK 10.0.300、`net10.0-ios` / `net10.0-android`、Microsoft.Maui.Controls 10.0.70 |

Androidは単一のMaven artifact `jp.kamusoft:kssettingsview`として配布します。Core、UI、Composeの各層は、Kotlin package `jp.kamusoft.kssettingsview.core`、`.ui`、`.compose`で分かれています。Androidの利用側にはKotlin 2.3以上、minSdk 29、compileSdk 35が必要です。表のKotlin 2.4.10はライブラリのビルドに使用するtoolchainであり、利用側Kotlinの最低版ではありません。

## インストール

詳しい導入方法はplatform別の[Agent Skills](https://github.com/kamusoft/KsSettingsView/blob/main/skills/README_ja.md)を参照してください。この節には依存宣言とprerelease版の指定方法だけを示します。

### iOS — Swift Package Manager

```swift
dependencies: [
    .package(url: "https://github.com/kamusoft/KsSettingsView-SPM", from: "0.1.0")
]
```

productは`.product(name: "KsSettingsView", package: "KsSettingsView-SPM")`として参照します。prerelease版は、そのsemantic version tagを`from: "X.Y.Z-beta.N"`で明示するか、`exact: "X.Y.Z-beta.N"`で固定します。

公開状況: packageは配信リポジトリ`KsSettingsView-SPM`から配布され、各リリースはsemantic version tagとして公開されます。

### Android — Maven

```kotlin
dependencies {
    implementation("jp.kamusoft:kssettingsview:0.1.0")
}
```

prerelease版は、依存宣言のversionに`X.Y.Z-alpha.N`、`X.Y.Z-beta.N`、`X.Y.Z-rc.N`のような値を指定します。Maven Centralではprerelease版も正式版と同列に表示されるため、使用するversionを明示的に選んでください。

### .NET MAUI — NuGet

```xml
<ItemGroup>
  <PackageReference Include="KsSettingsView.Maui" Version="0.1.0" />
</ItemGroup>
```

prerelease版は、`Version`に`X.Y.Z-beta.N`のような値を指定します。versionを固定せず検索する場合は、prereleaseを含める設定を有効にします（.NET CLIでは`--prerelease`）。

公開.NET namespaceは`KsSettingsView`です。XAMLでは、後述の例のように`clr-namespace:KsSettingsView;assembly=KsSettingsView.Maui`を使用します。

互換要件: Microsoft.Maui.Controls（`MauiVersion`）10.0.70以上、最低OS版はiOS 16.0 / Android API 29です。Microsoft.Maui.Controlsを10.0.70未満に固定するとpackage downgradeエラー（`NU1605`）になるため、`MauiVersion`を10.0.70以上へ更新してください。packageにはビルド時のガードが同梱されており、利用側プロジェクトの`SupportedOSPlatformVersion`がこれらの値を下回るとエラー`KSSV0001`でビルドが失敗します。次の断片はSampleアプリケーションと同じ形です。

```xml
<PropertyGroup>
  <MauiVersion>10.0.70</MauiVersion>
</PropertyGroup>

<PropertyGroup Condition=" $([MSBuild]::GetTargetPlatformIdentifier('$(TargetFramework)')) == 'ios' ">
  <SupportedOSPlatformVersion>16.0</SupportedOSPlatformVersion>
</PropertyGroup>

<PropertyGroup Condition=" $([MSBuild]::GetTargetPlatformIdentifier('$(TargetFramework)')) == 'android' ">
  <SupportedOSPlatformVersion>29</SupportedOSPlatformVersion>
</PropertyGroup>
```

通常はAPI版なしのTFM `net10.0-android`と`net10.0-ios`を使用します。platform API版を明示的に固定する場合は、`net10.0-android36.0`と`net10.0-ios26.0`以上を使用してください。これらを下回る版に固定すると、restoreが警告なく成功しても`lib/net10.0`へ静かにフォールバックし、Native binding package 2件が入りません。この解決挙動は.NET SDK 10.0.300で検証済みです。

名前衝突: `KsSettingsView.SwitchCell`と`KsSettingsView.EntryCell`は`Microsoft.Maui.Controls`の同名型と名前が重なります。C#で`using KsSettingsView;`とMAUIの暗黙usingを併用すると、この2つの名前はあいまい参照（CS0104）になります。XAMLの`ks:` prefixでは起きません。C#では完全修飾（`KsSettingsView.SwitchCell`）またはusing alias（`using SwitchCell = KsSettingsView.SwitchCell;`）を使用してください。

## 最小コード例

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

利用前に `MauiProgram` でハンドラを一度登録します。

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

手順は [.NET MAUI Skill](https://github.com/kamusoft/KsSettingsView/blob/main/skills/ja/kssettingsview-maui/SKILL.md) を参照してください。

## Skills

[iOS](https://github.com/kamusoft/KsSettingsView/blob/main/skills/ja/kssettingsview-ios/SKILL.md)、[Android](https://github.com/kamusoft/KsSettingsView/blob/main/skills/ja/kssettingsview-android/SKILL.md)、[.NET MAUI](https://github.com/kamusoft/KsSettingsView/blob/main/skills/ja/kssettingsview-maui/SKILL.md)、[AiForms.Maui.SettingsViewからの移行](https://github.com/kamusoft/KsSettingsView/blob/main/skills/ja/kssettingsview-aiforms-migration/SKILL.md)について、Agent Skillsが用途別の案内とAPIレシピを提供します。英語版・日本語版の一覧は[Skills索引](https://github.com/kamusoft/KsSettingsView/blob/main/skills/README_ja.md)を参照してください。

## リポジトリ構成

| ディレクトリ | 用途 |
| --- | --- |
| `ios/` | iOS NativeライブラリとSwift package |
| `android/` | Android Nativeライブラリ |
| `maui/` | .NET MAUI facadeとNative binding |
| `samples/` | 対応platformのSampleアプリケーション |
| `skills/` | 英語・日本語の利用者向けAgent Skills |
| `scripts/` | リポジトリのlintスクリプトとSwiftPM snapshotツール |
| `assets/` | ルートドキュメントで使用する画像 |
| `kasane/` | Kasaneの変更成果物、決定、concepts |
| `openspec/` | 旧運用 (OpenSpec) の歴史資料。凍結済みで現行の仕様ではありません |

[エージェント向け開発規約](https://github.com/kamusoft/KsSettingsView/blob/main/AGENTS.md) · [Kasane concepts](https://github.com/kamusoft/KsSettingsView/blob/main/kasane/concepts/index.md)

## 貢献

外部からのPull Requestは受け付けていません。不具合報告と改善提案はGitHub Issuesで受け付けます。
レビューに必要な情報が揃うよう、用意されたIssueテンプレートを使用してください。
Issueを投稿する前に[貢献ガイドライン](https://github.com/kamusoft/KsSettingsView/blob/main/.github/CONTRIBUTING_ja.md)を確認してください。

## ライセンス

KsSettingsViewは[MIT License](https://github.com/kamusoft/KsSettingsView/blob/main/LICENSE)で提供されます。

### サードパーティ通知

Sampleアプリケーションでは、Googleの**Material Symbols Outlined**アイコンセット（Copyright Google LLC）から派生したvector drawableを使用しています。これらは[Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)で配布され、アイコンの出典は[Google Fonts Icons](https://fonts.google.com/icons)です。

この通知はSampleアプリケーションで使用するアイコンだけを対象とし、KsSettingsViewライブラリ本体の依存関係を示すものではありません。
