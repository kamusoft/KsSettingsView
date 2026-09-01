---
kind: rule
applies-when:
  always: false
  paths: ["**/build.gradle.kts", "ios/Package.swift", "**/*.csproj"]
  tasks: [公開識別子・配布座標の決定]
title: 公開識別子と配布座標
description: 所有主体・製品・成果物の役割を ecosystem ごとの識別子へ写像する規約
timestamp: 2026-09-01
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

Android の公開層 (Core / UI / Compose) は namespace ではなく Kotlin パッケージ名
(`jp.kamusoft.kssettingsview.core` / `.ui` / `.compose`) が表す。

iOS / Android の Sample application は `jp.kamusoft.kssettingsview.samples.ios` / `.android` を使う。後続の module や Sample も独自の体系を作らず、上表の接頭辞と各 ecosystem の表記規則から用途を導く。

## SwiftPM の配布座標

`ios/Package.swift` が公開する product は umbrella の `KsSettingsView` 1 本だけである。利用者はこれ 1 つを依存に追加し、必要な module (`KsSettingsViewCore` / `KsSettingsViewUI` / `KsSettingsViewSwiftUI`) を import する。module 名は product 名とは別の識別子であり、公開層の区別はこちらが担う。

配布は monorepo からではなく、専用の配信リポジトリ `KsSettingsView-SPM` から行う ([ADR-0018](../../decisions/cross/0018-distribution-public-channels-root-swiftpm-manifest.md))。利用者が書く Package URL は `https://github.com/kamusoft/KsSettingsView-SPM` であり、product 参照は `.product(name: "KsSettingsView", package: "KsSettingsView-SPM")` になる。

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
- Sample application は `jp.kamusoft.kssettingsview.samples.*` の下で platform を区別する。
- Android の公開ライブラリは単一 artifact `kssettingsview` として配る。Core、UI、Compose の区別は Kotlin パッケージ名が担い、artifactId には現れない。
- SwiftPM で公開する product は umbrella 1 本 (`KsSettingsView`) に保ち、公開層の区別は module 名で表す。

## してはいけないこと

- 各 ecosystem の大小文字や区切りを無視して、識別子の文字列表現を一律にしない。
- Maven の `groupId` と `artifactId` の責務を混同しない。
- SwiftPM の Package URL に monorepo (`.../KsSettingsView`) を書かない。利用者が指すのは配信リポジトリだけである。

## 関連

- [リポジトリとビルドの責務境界](../../concepts/cross/architecture/repository-boundaries.md)
- [ADR-0002: 公開識別子の名前空間](../../decisions/cross/0002-public-identifier-namespace.md)
- [android/ADR-0016: Android の単一 module / 単一 Maven artifact](../../decisions/android/0016-single-module-single-maven-artifact.md)
