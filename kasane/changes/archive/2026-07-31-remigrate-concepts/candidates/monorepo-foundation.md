# Candidate: monorepo-foundation

## 概念候補

### リポジトリとビルドの責務境界 (提案カテゴリ: architecture/)

KsSettingsView は、iOS Native、Android Native、将来の .NET MAUI binding、各 platform の Sample、長命な設計知識を同じ変更単位で扱う monorepo である。一方、全 platform を一つの build graph へ統合せず、各 ecosystem の標準入口を独立した build root とする。この境界により、横断的な API 変更は一つの repository で調整しつつ、platform ごとの build、test、IDE の working set は分離できる。

#### 責務境界

| build root | 公開単位 | 依存方向 | 現在の役割 |
|---|---|---|---|
| iOS (`Package.swift`) | SwiftPM products `KsSettingsViewCore` / `KsSettingsViewUI` / `KsSettingsViewSwiftUI` | `SwiftUI → UI → Core`（`SwiftUI` は `Core` も直接参照） | iOS Native library と test の入口 |
| Android (`settings.gradle.kts`) | Gradle modules `ks-settingsview-core` / `ks-settingsview-ui` / `ks-settingsview-compose` | `compose → ui → core`（`compose` は `core` も直接参照） | Android Native library と Unit Test の入口 |
| MAUI (`KsSettingsView.slnx`) | まだなし | まだなし | 空の solution による将来の binding 入口 |

repository root は platform 横断の変更調整と知識の入口を担うが、共通 build file や全 platform 一括 build は提供しない。`maui/KsSettingsView.slnx` と `samples/maui/` は foundation placeholder であり、現在利用できる library product ではない。

Sample は library 本体とは別の利用側 application である。iOS Sample は `../../ios` を `XCLocalSwiftPackageReference` として参照し、3 products を link する。Android Sample は `../../android` を Gradle composite build として取り込み、開発用 GAV を各 included project へ `dependencySubstitution` する。どちらも本体 source に step-in できる local source reference であり、配布 repository から取得する利用形態を証明するものではない。

#### 保証すること

- iOS と Android は、他方の toolchain を build graph に含めず、それぞれの build root から library test を開始できる。
- Native library は Core、UI host、declarative wrapper の3層を公開単位として分け、依存は wrapper から UI、UI から Core の方向に限定する。
- Sample は公開 product/module を利用者側の境界から参照し、本体の内部 source set を Sample module へ混在させない。
- iOS Sample の local Swift package build と Android Sample の composite build は、開発中の本体変更を直接取り込む。

#### してはいけないこと

- repository root に統合 build がある、または一つの platform build が別 platform の成立も検証すると見なしてはいけない。
- Sample を library の配布物、挙動契約の SSoT、自動 test の代替として扱ってはいけない。
- `Core → UI`、`UI → declarative wrapper` の逆方向依存を作ってはいけない。
- 空の MAUI solution を実装済みの MAUI product と見なしてはいけない。

#### 代表的な利用側参照

```swift
import KsSettingsViewCore
import KsSettingsViewUI
import KsSettingsViewSwiftUI
```

```kotlin
implementation("jp.kamusoft.kssettingsview:ks-settingsview-core:0.1.0-SNAPSHOT")
implementation("jp.kamusoft.kssettingsview:ks-settingsview-ui:0.1.0-SNAPSHOT")
implementation("jp.kamusoft.kssettingsview:ks-settingsview-compose:0.1.0-SNAPSHOT")
```

後者は現行 Sample の composite build 用座標であり、Maven repository への公開を保証しない。

出典: `ios/Package.swift`、`android/settings.gradle.kts`、`android/ks-settingsview-*/build.gradle.kts`、`samples/ios/KsSettingsViewSample.xcodeproj/project.pbxproj`、`samples/android/settings.gradle.kts`、`samples/android/app/build.gradle.kts`、`maui/KsSettingsView.slnx`、`openspec/specs/monorepo-foundation/spec.md` Purpose、`openspec/changes/archive/2026-05-06-add-monorepo-foundation/design.md` Decisions 1–2、旧 `kasane/concepts/architecture/repository-boundaries.md`、`docs/overview.md`

### 公開識別子と開発用座標 (提案カテゴリ: conventions/)

公開識別子は ecosystem の慣例に合わせて、所有主体、製品、成果物の役割を表す。SwiftPM は product 名、Android は namespace と GAV、application は bundle/application ID、.NET は namespace をそれぞれの識別子とする。同じ文字列へ統一するのではなく、完全な識別子から KsSettingsView と用途を判別できることが目的である。

#### 現行コードで確認できる識別子

| 対象 | 現行値 |
|---|---|
| SwiftPM package | `KsSettingsView` |
| SwiftPM products | `KsSettingsViewCore` / `KsSettingsViewUI` / `KsSettingsViewSwiftUI` |
| Android library namespace | `jp.kamusoft.kssettingsview.core` / `.ui` / `.compose` |
| Android project group | `jp.kamusoft.kssettingsview` |
| Android artifact 相当の project 名 | `ks-settingsview-core` / `ks-settingsview-ui` / `ks-settingsview-compose` |
| Android Sample application ID | `jp.kamusoft.kssettingsview.samples.android` |
| iOS Sample bundle ID | `jp.kamusoft.kssettingsview.samples.ios` |

Android には `maven-publish` / `MavenPublication` の設定がなく、上表の GAV は現時点では Sample の composite build が置換する開発用座標である。公開 repository、release version、配布 metadata はこの capability からは保証しない。.NET namespace は実装が存在しないため、現行コードから確認できる識別子はまだない。

#### 保証すること

- Swift module/product 名は PascalCase、Android project/artifact 名は kebab-case、Kotlin package/namespace は lowercase の reverse-DNS 形式を使う。
- iOS / Android の Sample application は `jp.kamusoft.kssettingsview.samples.*` の下で用途を区別する。
- Android の3 module は product 名を `ks-settingsview-*` で対応付ける。

#### してはいけないこと

- composite build で解決できる GAV を、Maven repository に公開済みの配布座標と説明してはいけない。
- 実装のない .NET namespace や package ID を、現在利用可能な公開契約として列挙してはいけない。
- 現行 Android project group と ADR/spec が規定する Maven Central `groupId` の差を、統合時に暗黙にどちらかへ合わせてはいけない。

出典: `ios/Package.swift`、Android 各 module の `build.gradle.kts`、`samples/android/app/build.gradle.kts`、`samples/ios/KsSettingsViewSample.xcodeproj/project.pbxproj`、`openspec/changes/archive/2026-05-06-add-monorepo-foundation/design.md` Decision 3、旧 `kasane/concepts/conventions/public-identifiers.md`、`docs/overview.md`

### 対応 platform と build 検証入口 (提案カテゴリ: conventions/)

platform の最低要件は、利用者が library を組み込める範囲と、開発者が同じ build を再現するための基準を定める。現在コードで強制される基準と、各 build root / Sample から実行する検証入口を対にして扱う。

#### 現行 baseline

| 対象 | コードで強制される基準 |
|---|---|
| iOS library | Swift tools 5.10、iOS 16+ |
| SwiftPM host test | macOS 13+（test host のための指定であり、macOS product support ではない） |
| Android library | minSdk 29、compileSdk 35、Java/JVM toolchain 17 |
| Android build tools | Gradle Wrapper 8.10.2、AGP 8.7.3、Kotlin 2.0.21 |
| MAUI | 実装 project がなく、code で強制される SDK baseline はまだない |

#### 検証入口

| 対象 | 主な入口 | 検証する境界 |
|---|---|---|
| iOS package | `cd ios && swift test` | SwiftPM target graph と host で実行可能な Unit Test |
| iOS Sample | `xcodebuild -project ... -scheme KsSettingsViewSample -sdk iphonesimulator ... build` | local package 解決、UIKit/SwiftUI products、利用 application の link |
| Android libraries | `cd android && ./gradlew test` | 3 library modules の Debug/Release Unit Test |
| Android Sample | `cd samples/android && ./gradlew :app:assembleDebug` | composite build、GAV 置換、利用 application の assemble |
| MAUI placeholder | `cd maui && dotnet sln KsSettingsView.slnx list` | solution file を .NET CLI が読み込めること（実装 project の build ではない） |

2026-07-19 の抽出時には上記5入口が成功した。repository に CI workflow や root-level build script はなく、この成功は継続的な自動 enforcement を意味しない。

#### 保証すること

- library の deployment/min SDK baseline は build metadata 自体で強制される。
- library test と Sample build を分け、module 内契約と利用側 integration を別の入口で検証できる。
- Android library と Android Sample は独立した Gradle build であり、composite build 時は両方が Android SDK location を解決できなければならない。

#### してはいけないこと

- macOS test host の指定を、macOS 向け UI library の提供保証と解釈してはいけない。
- `swift test` だけで UIKit 上の全 test・Sample integration まで検証できると見なしてはいけない。
- 空の MAUI solution に対して .NET 9 / MAUI 9 が code で強制済みと説明してはいけない。
- 現在存在しない repository-wide CI や `build-all` 入口を保証してはいけない。

出典: `ios/Package.swift`、`ios/Tests/`、Android 各 module の `build.gradle.kts`、`android/gradle/wrapper/gradle-wrapper.properties`、`samples/ios/KsSettingsViewSample.xcodeproj`、`samples/android/`、`README.md`、各 Sample README、`openspec/changes/archive/2026-05-06-add-monorepo-foundation/design.md` Decision 4

## ADR 候補

- **monorepo と platform 別の独立 build root を併用する** — 横断変更は単一 repository にまとめる一方、`ios/Package.swift`、`android/settings.gradle.kts`、`maui/KsSettingsView.slnx` を独立入口とし、root に統合 build file を置かない。出典: archive design Decisions 1–2。選別基準: 覆すコストが高い、platform 境界を越える、将来の build/CI/KMP 構成を制約する。既存 accepted ADR-0001 が包含する。
- **ecosystem ごとの公開識別子を所有 domain と product 名から導く** — Apple/Android は `jp.kamusoft.kssettingsview.*`、Maven は組織 group と `ks-settingsview-*` artifact、.NET は `KsSettingsView.*` とする。出典: archive design Decision 3。選別基準: 公開配布後に覆すコストが高い、全 platform 境界を越える、後続 module の命名を制約する。既存 accepted ADR-0002 が包含するが、Android の現行 `group` との drift 解消が必要。
- **modern UI API を前提に最低 toolchain / OS baseline を固定する** — iOS 16 / Swift 5.10 / Xcode 16、Android API 29 / compileSdk 35 / JDK 17 / Gradle 8.10 / AGP 8.7、将来の MAUI は .NET 9 を選ぶ。出典: archive design Decision 4。選別基準: 対応利用者と全 module に影響して覆すコストが高い、将来採用できる API と build tool を制約する。現行 code で確認できない MAUI baseline は ADR 化時に「将来方針」と「現行保証」を分ける必要がある。

## drift 所見

- 旧 spec、archive design Decision 3、ADR-0002、旧 `public-identifiers.md` は Maven Central `groupId = jp.kamusoft` を規定するが、現行 Android 3 modules は `group = jp.kamusoft.kssettingsview` であり、Sample の GAV と `dependencySubstitution` も後者を使う (`openspec/specs/monorepo-foundation/spec.md` / `kasane/decisions/0002-public-identifier-namespace.md` / `android/ks-settingsview-*/build.gradle.kts` / `samples/android/settings.gradle.kts`)。
- spec が命名規約と最低 toolchain の所在として要求する `docs/conventions.md` と `docs/development.md` は現存せず、内容は `docs/overview.md` へ統合されている。Requirement のファイル path と現行 docs 構成が一致しない (`openspec/specs/monorepo-foundation/spec.md` / `docs/overview.md`)。
- `docs/overview.md` は `Gradle / AGP 8.7 以上` と一括表記するが、archive Decision 4 は Gradle 8.10+ / AGP 8.7+ を選択し、現行 Wrapper は Gradle 8.10.2 である。記載どおり Gradle 8.7 を最低要件とは確認できない (`docs/overview.md` / `openspec/changes/archive/2026-05-06-add-monorepo-foundation/design.md` / `android/gradle/wrapper/gradle-wrapper.properties`)。
- `README.md`、`docs/overview.md`、`docs/architecture.md` は Core を platform 型非依存と説明するが、現行 iOS `KsSettingsViewCore.KsAnyView` は SwiftUI/UIKit、Android `ks-settingsview-core` は Android View/Compose Runtime に依存し、Android Core 自体も `com.android.library` である。module の依存方向は維持されているが、Core の platform 非依存という責務説明は現実と一致しない (`ios/Sources/KsSettingsViewCore/KsAnyView.swift` / `android/ks-settingsview-core/build.gradle.kts` / `android/ks-settingsview-core/src/main/kotlin/jp/kamusoft/kssettingsview/core/KsAnyView.kt`)。
- `README.md` と `docs/README.md` は `openspec/specs/` を仕様の SSoT と説明するが、現行 project harness は `openspec/` を歴史資料として凍結し、新しい長命知識を `kasane/concepts/` へ移す。monorepo 内の知識入口に関する説明が現行運用と一致しない (`README.md` / `docs/README.md` / `AGENTS.md` / `kasane/config.yaml`)。

## 用語

- **build root**: ecosystem の build graph と toolchain 解決を所有し、その platform の build/test を開始する独立入口。
- **SwiftPM product**: `Package.swift` が利用者へ公開し、import/link の単位となる library product。
- **Gradle module**: Android build 内の project 単位。現行は `core`、`ui`、`compose` の3 module。
- **GAV**: Maven 系座標の `groupId:artifactId:version`。
- **composite build**: 独立した Gradle build を `includeBuild` で組み合わせ、外部 module dependency を included project へ置換する仕組み。
- **local source reference**: package repository の配布物ではなく、同じ checkout 内の library source を Sample が直接 build する参照方式。
- **Sample**: 公開 product/module を利用者側から組み込む実行可能 application。library 本体や自動 test の代替ではない。
- **foundation placeholder**: 後続実装の入口だけを予約し、利用可能な product をまだ含まない最小構成。現行の MAUI solution と MAUI Sample が該当する。

## 抽出メモ

- 「リポジトリとビルドの責務境界」は旧 `architecture/repository-boundaries.md` を、現行の3 module/product、Sample wiring、MAUI placeholder、検証入口まで具体化した候補。単なる directory 一覧ではなく、再導出コストの高い build/consumer 境界だけを残した。
- 「公開識別子と開発用座標」は旧 `conventions/public-identifiers.md` の後継候補だが、Maven group drift をオーナーが解消方向を決めるまで、`jp.kamusoft` を現行保証として確定できない。
- 「対応 platform と build 検証入口」は `docs/overview.md` の記述水準を基準にした。tool version は腐りやすいが、利用可否と再現性に直結して探索コストも高いため、timestamp 付き concept/policy として保持する価値がある。
- Android には version catalog がなく plugin/dependency version は module build files に直接記載される。repository-wide CI/build script もない。どちらも現行構成の事実だが、独立した長命概念へ昇格させず、build root 境界と drift 評価の補助情報に留めた。
- Android の `implementation(project(...))` と public API の型露出を配布 metadata 上でどう表現するかは、現時点で publishing 設定がないため未確定。将来 Maven publication を導入する capability で `api` / `implementation` と consumer dependency の契約を再評価する必要がある。
- Core の platform 依存 drift は `settings-view-core` の統合候補と重なる。ここでは build/module 境界への影響だけを記録し、Core API の責務定義は隣接 capability 側へ委ねる。
