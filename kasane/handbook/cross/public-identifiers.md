---
kind: rule
applies-when:
  always: false
  paths: ["**/build.gradle.kts", "ios/Package.swift", "**/*.csproj", "maui/Directory.*.props"]
  tasks: [公開識別子・配布座標の決定]
title: 公開識別子と配布座標
description: 所有主体・製品・成果物の役割を ecosystem ごとの識別子へ写像する規約
timestamp: 2026-09-02
---

# 公開識別子と配布座標

この文書は、KsSettingsView の product 名、namespace、application ID、配布座標の命名規約を説明する。読むと、識別子を全 ecosystem で同じ文字列にせず、所有主体・製品・用途をそれぞれの慣例へどう写像するかが分かる。

## 命名方針

公開識別子は、所有主体、KsSettingsView 製品、成果物または application の用途を区別する。Apple / Android は lowercase の reverse-DNS、Swift product と .NET namespace は PascalCase、Android の artifact / project 名は lowercase を使う。Android の artifact / project 名でハイフンを使うのはブランド名とサブモジュールの境目だけで、ブランド名 `kssettingsview` の内部には入れない ([android/ADR-0016](../../decisions/android/0016-single-module-single-maven-artifact.md))。

| 対象 | 規則または現行値 | 表すもの |
|---|---|---|
| SwiftPM package | `KsSettingsView` | 製品 |
| SwiftPM product | `KsSettingsView` (umbrella 1 本) | 製品 |
| SwiftPM module | `KsSettingsViewCore` / `KsSettingsViewUI` / `KsSettingsViewSwiftUI` | 製品内の公開層 |
| SwiftPM 配信リポジトリ | `KsSettingsView-SPM` / `https://github.com/kamusoft/KsSettingsView-SPM` | 製品の SwiftPM 配布経路 |
| Android library namespace | `jp.kamusoft.kssettingsview` (公開ライブラリ本体) / `.bridge` (interop Bridge) | 所有主体・製品・成果物 |
| Apple bundle ID / Android application ID | `jp.kamusoft.kssettingsview.*` | 所有主体・製品・application 用途 |
| Maven artifactId | `kssettingsview` (単一) | 製品 |
| .NET namespace | `KsSettingsView.*` | 製品・用途。実装時にこの規則から導く |
| NuGet Package ID | `KsSettingsView.Maui` (facade) / `KsSettingsView.Binding.iOS` / `KsSettingsView.Binding.Android` | 製品・対象 platform stack・成果物の役割 |

Android の公開層 (Core / UI / Compose) は namespace ではなく Kotlin パッケージ名
(`jp.kamusoft.kssettingsview.core` / `.ui` / `.compose`) が表す。

iOS / Android の Sample application は `jp.kamusoft.kssettingsview.samples.ios` / `.android`、配布物の消費者検証 (`verification/`) の application は `jp.kamusoft.kssettingsview.verification.android` / `.maui` を使う。後続の module や Sample も独自の体系を作らず、上表の接頭辞と各 ecosystem の表記規則から用途を導く。

## SwiftPM の配布座標

`ios/Package.swift` が公開する product は umbrella の `KsSettingsView` 1 本だけである。利用者はこれ 1 つを依存に追加し、必要な module (`KsSettingsViewCore` / `KsSettingsViewUI` / `KsSettingsViewSwiftUI`) を import する。module 名は product 名とは別の識別子であり、公開層の区別はこちらが担う。

配布は monorepo からではなく、専用の配信リポジトリ `KsSettingsView-SPM` から行う ([ADR-0018](../../decisions/cross/0018-distribution-public-channels-root-swiftpm-manifest.md))。利用者が書く Package URL は `https://github.com/kamusoft/KsSettingsView-SPM` であり、product 参照は `.product(name: "KsSettingsView", package: "KsSettingsView-SPM")` になる。

## NuGet の配布座標

.NET MAUI 向けは 3 パッケージで配る。利用者が書くのは facade の `KsSettingsView.Maui` 1 行だけで、
binding 2 件 (`KsSettingsView.Binding.iOS` / `KsSettingsView.Binding.Android`) は platform TFM の
依存として推移的に届く ([maui/ADR-0025](../../decisions/maui/0025-nuget-three-package-root-namespace.md))。
Package ID はいずれもアセンブリ名と同じ文字列で、facade だけが `PackageId` を明示し、
binding はアセンブリ名の既定に任せる。

facade の Package ID `KsSettingsView.Maui` と .NET namespace `KsSettingsView` は意図的に非対称である。
Package ID は「どの platform stack 向けの配布物か」を利用者が検索・識別するための座標であり、
`.Maui` はその区別を担う。一方 namespace は利用者のソースに毎行現れる記述であり、
1 つのアプリが iOS / Android / MAUI の実装を同時に import することはないため、
platform を表す語を持たせる価値がない。この非対称は Swift の product 名と module 名の関係
(product は配布単位、module は import 単位) と同じ考え方に立つ。

binding の namespace は `KsSettingsView.Bridge` で、Package ID とも非対称である。
binding は interop の輸送層であり利用者向けの公開契約ではないため、
namespace は役割 (Bridge) を表し、Package ID は配布上の対象 platform を表す。

## Maven 座標の現在地

accepted [ADR-0002](../../decisions/cross/0002-public-identifier-namespace.md) は Maven Central の `groupId` を `jp.kamusoft` と定め、組織と成果物を分ける。Android の公開単位は単一 artifact
`jp.kamusoft:kssettingsview` である ([android/ADR-0016](../../decisions/android/0016-single-module-single-maven-artifact.md))。
artifactId はブランド名 `kssettingsview` を 1 トークンとして扱い、内部にハイフンを入れない。

```text
jp.kamusoft:kssettingsview:0.1.0-SNAPSHOT
```

Gradle `group` は `android/build.gradle.kts` の subprojects 一括設定が `jp.kamusoft` を与え、ADR-0002 と一致している。version の値は `android/gradle/libs.versions.toml` の `kssettingsview` キーが単一の宣言元で、subprojects 一括設定と Sample の GAV 参照がそれを読む ([Android ビルドツールチェーンの契約](../../concepts/android/architecture/build-toolchain.md))。

GAV は Maven 系の `groupId:artifactId:version` 形式の座標を指す。`:kssettingsview` は
`com.vanniktech.maven.publish` を適用して Sonatype Central Portal へ発行する構成を持ち、Android Sample はこの GAV を composite build の明示 dependencySubstitution で本体 project へ置換する。interop Bridge (`:kssettingsview-bridge`) は発行対象に含めない。

## 保証すること

- 完全な識別子から、kamusoft、KsSettingsView、成果物または application の用途を判別できる。
- Swift product は PascalCase、Android namespace は lowercase reverse-DNS、artifact / project 名は lowercase を使い、ハイフンはブランド名とサブモジュールの境目にだけ置く。
- Sample application は `jp.kamusoft.kssettingsview.samples.*`、消費者検証の application は `jp.kamusoft.kssettingsview.verification.*` の下で platform を区別する。
- Android の公開ライブラリは単一 artifact `kssettingsview` として配る。Core、UI、Compose の区別は Kotlin パッケージ名が担い、artifactId には現れない。
- SwiftPM で公開する product は umbrella 1 本 (`KsSettingsView`) に保ち、公開層の区別は module 名で表す。
- NuGet は facade 1 件 + binding 2 件で配り、利用者が書く座標は `KsSettingsView.Maui` だけに保つ。

## してはいけないこと

- 各 ecosystem の大小文字や区切りを無視して、識別子の文字列表現を一律にしない。
- Maven の `groupId` と `artifactId` の責務を混同しない。
- SwiftPM の Package URL に monorepo (`.../KsSettingsView`) を書かない。利用者が指すのは配信リポジトリだけである。
- NuGet の Package ID と .NET namespace を同じ文字列に揃えない。座標と import 単位は別の役割を担う。

## 関連

- [リポジトリとビルドの責務境界](../../concepts/cross/architecture/repository-boundaries.md)
- [ADR-0002: 公開識別子の名前空間](../../decisions/cross/0002-public-identifier-namespace.md)
- [android/ADR-0016: Android の単一 module / 単一 Maven artifact](../../decisions/android/0016-single-module-single-maven-artifact.md)
- [maui/ADR-0025: MAUI の 3 パッケージ構成と root namespace](../../decisions/maui/0025-nuget-three-package-root-namespace.md)
