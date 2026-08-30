---
kind: rule
applies-when:
  always: false
  paths: ["**/build.gradle.kts", "ios/Package.swift", "**/*.csproj"]
  tasks: [公開識別子・配布座標の決定]
title: 公開識別子と配布座標
description: 所有主体・製品・成果物の役割を ecosystem ごとの識別子へ写像する規約
timestamp: 2026-08-29
---

# 公開識別子と配布座標

この文書は、KsSettingsView の product 名、namespace、application ID、配布座標の命名規約を説明する。読むと、識別子を全 ecosystem で同じ文字列にせず、所有主体・製品・用途をそれぞれの慣例へどう写像するかが分かる。

## 命名方針

公開識別子は、所有主体、KsSettingsView 製品、成果物または application の用途を区別する。Apple / Android は lowercase の reverse-DNS、Swift product と .NET namespace は PascalCase、Android artifact / project 名は kebab-case を使う。

| 対象 | 規則または現行値 | 表すもの |
|---|---|---|
| SwiftPM package | `KsSettingsView` | 製品 |
| SwiftPM product | `KsSettingsViewCore` / `KsSettingsViewUI` / `KsSettingsViewSwiftUI` | 製品内の公開層 |
| Android library namespace | `jp.kamusoft.kssettingsview.core` / `.ui` / `.compose` | 所有主体・製品・公開層 |
| Apple bundle ID / Android application ID | `jp.kamusoft.kssettingsview.*` | 所有主体・製品・application 用途 |
| Maven artifactId | `ks-settingsview-*` | 製品内の個別成果物 |
| .NET namespace | `KsSettingsView.*` | 製品・用途。実装時にこの規則から導く |

iOS / Android の Sample application は `jp.kamusoft.kssettingsview.samples.ios` / `.android` を使う。後続の module や Sample も独自の体系を作らず、上表の接頭辞と各 ecosystem の表記規則から用途を導く。

## Maven 座標の現在地

accepted [ADR-0002](../../decisions/cross/0002-public-identifier-namespace.md) は Maven Central の `groupId` を `jp.kamusoft` と定め、`jp.kamusoft:ks-settingsview-core` のように組織と成果物を分ける。

この ADR を明示的に置き換える変更が accepted になるまでは、将来の公開 Maven `groupId` の規範は `jp.kamusoft` である。現行 Gradle `group` は未追従の実装 drift であり、それ自体で公開規範を変更しない。

一方、現行 Android 4 module (bridge を含む) の Gradle `group` は `jp.kamusoft.kssettingsview` であり、Android Sample は次の開発用 GAV を composite build で本体 project へ置換する。version の値は `android/gradle/libs.versions.toml` の `ks-settingsview` キーが単一の宣言元で、各 module の `version` と Sample の GAV 参照がそれを読む ([Android ビルドツールチェーンの契約](../../concepts/android/architecture/build-toolchain.md))。

```text
jp.kamusoft.kssettingsview:ks-settingsview-core:0.1.0-SNAPSHOT
jp.kamusoft.kssettingsview:ks-settingsview-ui:0.1.0-SNAPSHOT
jp.kamusoft.kssettingsview:ks-settingsview-compose:0.1.0-SNAPSHOT
```

GAV は Maven 系の `groupId:artifactId:version` 形式の座標を指す。Android module には現在 `maven-publish` / `MavenPublication` の設定がない。このため、上記を公開済みの Maven 配布座標として確定せず、現行 Sample 内の開発用座標として扱う。Maven 公開を導入する変更では、ADR の `jp.kamusoft` と現行 Gradle `group` の食い違いを先に解消する。

## 保証すること

- 完全な識別子から、kamusoft、KsSettingsView、成果物または application の用途を判別できる。
- Swift product は PascalCase、Android namespace は lowercase reverse-DNS、artifact / project 名は kebab-case を使う。
- Sample application は `jp.kamusoft.kssettingsview.samples.*` の下で platform を区別する。
- Android の Core、UI、Compose は `ks-settingsview-*` の suffix で対応付ける。

## してはいけないこと

- 各 ecosystem の大小文字や区切りを無視して、識別子の文字列表現を一律にしない。
- Maven の `groupId` と `artifactId` の責務を混同しない。
- ADR と現行 Gradle `group` の食い違いを、判断なしにどちらかへ合わせない。

## 関連

- [リポジトリとビルドの責務境界](../../concepts/cross/architecture/repository-boundaries.md)
- [ADR-0002: 公開識別子の名前空間](../../decisions/cross/0002-public-identifier-namespace.md)
