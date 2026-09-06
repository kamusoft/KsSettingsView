---
type: concept
title: Android ビルドツールチェーンの契約
description: android/ と samples/android/ の Gradle ビルドにおける JDK の役割分担 (Gradle を動かす JDK と成果物ターゲット Java 17)・ビルド関連バージョンと GAV の宣言の単一元 (libs.versions.toml とルート build.gradle.kts)・Maven 発行の入口 (vanniktech maven publish)・MAUI binding など消費側が依存する前提と、ツールチェーン更新時に揃えるもの
tags: [android, build, gradle, toolchain, version-catalog, jdk, maven-publish]
timestamp: 2026-09-04
---

# Android ビルドツールチェーンの契約

この文書を読むと、Android のライブラリと Sample をビルドするときに JDK が 2 つの役割 (Gradle を動かす JDK / 成果物が対象とする Java 版) に分かれていること、Gradle・AGP・Kotlin などのビルド関連バージョンをどこで宣言しているか、ツールチェーンを更新するときに何を同時に揃え何を壊してはいけないかが分かる。`android/` と `samples/android/` が別々の Gradle build である理由は [リポジトリとビルドの責務境界](../../cross/architecture/repository-boundaries.md) を先に読むと分かりやすい。

用語: **build root** = Gradle build の入口 (`settings.gradle.kts` のあるディレクトリ)。**GAV** = Maven 座標 `group:artifact:version`。**宣言の単一元** = その値を書いてよい唯一の場所。

## 責務境界

- `android/` と `samples/android/` (Sample app) は独立した Gradle build で、それぞれ Gradle wrapper (`gradle/wrapper/` と `gradlew`) を持つ。2 つの wrapper は同じ Gradle 版と `distributionSha256Sum` を指す
- module は公開本体 `kssettingsview` と非公開 interop の `kssettingsview-bridge` の 2 つ ([ADR-0016](../../../decisions/android/0016-single-module-single-maven-artifact.md))
- 層 (core / ui / compose) は module 境界ではなく Kotlin パッケージ `.core` / `.ui` / `.compose` で表す
- **Gradle plugin の版 (AGP / Kotlin / Compose Compiler)・Compose BOM・ライブラリ自身の version** の宣言の単一元は `android/gradle/libs.versions.toml` (バージョンカタログ)。`samples/android/settings.gradle.kts` は `versionCatalogs { create("libs") { from(files("../../android/gradle/libs.versions.toml")) } }` で同じファイルを読む。composite build (`includeBuild`) はソース参照を接続するだけで plugin 版を継承しないため、共有は catalog で明示する。これ以外のライブラリ依存 (AndroidX 各種・coroutines・テスト依存) は各 module の `build.gradle.kts` に直書きしてよい
- Android SDK の場所は各 build root の `local.properties` (`sdk.dir`、git 管理外) で解決する。`compileSdk` / `minSdk` は catalog ではなく各 module の `build.gradle.kts` の `android { }` ブロックで宣言する (配布物の互換性を決める値であり、ツールチェーン更新の対象ではない)
- toml の `[versions]` の現行値はこの文書に転記しない (正は toml と、組み合わせの根拠 URL を記したそのコメント)。ただし互換上の既知制約 (「ツールチェーンを更新するとき」の節) は版を伴って記し、timestamp で鮮度を管理する

## 2 つの JDK の役割

「Gradle を動かす JDK」と「成果物が対象とする Java 版」は別物で、前者を変えても後者は変わらない。

| 役割 | 決め方 | 要件 |
|---|---|---|
| Gradle を実行する JVM (Gradle JVM。CLI では `JAVA_HOME`、Android Studio では Gradle JDK 設定) | 開発者環境・IDE・CI が選ぶ | 使用中の Gradle 版がサポートする範囲内であること。JDK 17 / JDK 21 / JDK 25 (JDK 25 は Android Studio 同梱 JBR) のそれぞれで sync・ビルド・全テストが通ることを実測済み (検証時の Gradle / AGP / Kotlin 版は toml のコメント参照) |
| 成果物のターゲット | 各 module の `kotlin { jvmToolchain(17) }` と `compileOptions` (`VERSION_17`) | Java 17 固定。compileSdk / minSdk と同じく配布物の互換性を決める値であり、Gradle JVM の更新では変えない |

`jvmToolchain(17)` は Gradle の toolchain 機構で **ローカルにインストールされた JDK 17 を探す**。toolchain resolver plugin (自動ダウンロード) は入れていないため、Gradle JVM に JDK 21 や 25 を使う場合でも JDK 17 の実体が別途必要で、無いマシンや CI ランナーでは `No matching toolchains found` で失敗する。ランナーを用意するときは JDK 17 を同梱するか、resolver を追加するかを決める必要がある。

## バージョンカタログの中身

| セクション | キー | 使われ方 |
|---|---|---|
| `[versions]` | `agp` | `com.android.library` / `com.android.application` の版 |
| | `kotlin` | Kotlin Android プラグインと Compose Compiler プラグインの共通版 (2 つは同じ版でなければならない) |
| | `compose-bom` | Jetpack Compose BOM |
| | `kssettingsview` | ライブラリ自身の GAV の version の単一宣言元。ルート `android/build.gradle.kts` の subprojects 一括設定 (`group = "jp.kamusoft"` と version) がこのキーを読み、Sample が `includeBuild` の dependency substitution で本体 project へ置換する GAV (`jp.kamusoft:kssettingsview:<version>`) も同じキーを読む |
| | `maven-publish` | vanniktech maven publish plugin の版 |
| `[plugins]` | `android-library` / `android-application` / `kotlin-android` / `kotlin-compose` / `maven-publish` | 各 `build.gradle.kts` の `plugins { alias(libs.plugins.…) }`。`maven-publish` を適用するのは本体 module のみ |
| `[libraries]` | `compose-bom` | `implementation(platform(libs.compose.bom))` |

## Maven 発行の入口

発行は本体 module (`android/kssettingsview/build.gradle.kts`) の `com.vanniktech.maven.publish` plugin が担う。release 単一 variant + sources jar + 空 javadoc jar で、IDE の KDoc 表示は sources jar が担い、利用者向けドキュメントは skills/ と README の責務。bridge に発行タスクは存在しない。

依存スコープは「公開 ABI に露出する外部型の依存は `api`、内部利用は `implementation`、テスト専用はユニットテスト用 configuration」で仕分ける。利用者が依存 1 行で公開 API をコンパイルできることの成立条件であり (android/ADR-0016)、公開宣言に外部型を足す変更では発行メタデータ (POM / `.module`) のスコープ追随を確認する。

version はルート `android/build.gradle.kts` が全モジュールへ配る。`-Pversion=` の注入があればそれを使い、無いときだけカタログの開発用既定値 (`0.1.0-SNAPSHOT`) を使う ([cross/ADR-0020](../../../decisions/cross/0020-release-dispatch-tag-last-version-injection.md))。version が `-SNAPSHOT` の間は Central 向け発行タスクが build.gradle.kts 内のガードで失敗する (ローカル検証は `publishToMavenLocal`)。

署名は署名鍵 (`signingInMemoryKey` プロパティ。release CI は `ORG_GRADLE_PROJECT_*` 環境変数で渡す) がある発行でだけ必須になり、鍵の無い発行はリリース版の version でも Sign タスクを skip して未署名で成功する。鍵を持たない消費者検証の dry-run がリリース版を mavenLocal に発行するための条件で、鍵の渡し忘れは release CI が upload の前に成果物ごとの `.asc` の存在を検査して止める (`scripts/release/check-signatures.sh`)。Central Portal の認証も同じく環境変数渡しで release CI が注入する。Central 向けの発行は `publishToMavenCentral` で upload して保留し (自動 release なし)、NuGet の push が済んでから Portal API で release する 2 段になっている ([cross/ADR-0020](../../../decisions/cross/0020-release-dispatch-tag-last-version-injection.md))。

mavenLocal 経由の解決と Release ビルドは `verification/android` の消費者が `main` 宛て pull request の CI とリリースの dry-run で確かめ、公開後の smoke が Maven Central からの解決を確かめる (初回リリース `0.1.0-beta.1` で実証済み) ([リポジトリとビルドの責務境界](../../cross/architecture/repository-boundaries.md))。

## 消費側が依存する前提

- **MAUI binding** (`maui/android/KsSettingsView.Binding.Android/KsSettingsView.Binding.Android.csproj`) は `android/gradlew … assembleRelease` を Exec で直接呼んで aar を作る ([maui/ADR-0006](../../../decisions/maui/0006-android-binding-gradlew-exec.md))。対象は aar 2 本 — `kssettingsview-bridge` を束縛 (Bind=true) し、本体 `kssettingsview` は同梱のみ (Bind=false)。統合により `.compose` 層のクラスも本体 aar に同梱されるが、MAUI は Bridge 経由で Android View の Host を使うため実行時依存は増えない。Exec の `JAVA_HOME` は .NET Android SDK が解決する `JavaSdkDirectory` (現状 JDK 21) で、その JDK が Gradle JVM の要件を満たし、かつ前節のとおり JDK 17 の実体も別途ある必要がある
- binding csproj 内の MSBuild Target `_BuildKsSettingsViewAars` は Item `KsAndroidModuleSource` を Inputs として aar を作り直すかを判定する。Inputs には module ソースと `build.gradle.kts` のほか `android/gradle/libs.versions.toml`・`android/gradle/wrapper/gradle-wrapper.properties`・`android/gradle.properties` が入っている。catalog だけを変える更新でも aar が作り直されるのはこのため
- **Android Studio** は `samples/android` を開く (composite build で `android/` の 4 module も同じ sync に含まれる)。Gradle JDK は Studio 同梱 JBR のままでよく、別の JDK へ手動固定する必要はない。`android/gradle.properties` と `samples/android/gradle.properties` の `org.gradle.tooling.parallel=true` は IDE sync のモデル取得を並列化する設定で、CLI ビルドには影響しない

## ツールチェーンを更新するとき

互換の制約は 3 方向から来る: (1) 使いたい Gradle JVM の JDK 版 → Gradle の最低版、(2) Kotlin Gradle Plugin がテスト済みとする Gradle の上限と AGP の範囲、(3) AGP が要求する Gradle の最低版。この 3 つの交差で Gradle / AGP / Kotlin の組を決め、公式互換表 (Gradle compatibility matrix・Kotlin の Gradle 設定ページ・AGP リリースノート) で裏を取る。公式表に明記のない組み合わせ (AGP の古い系列と新しい Gradle メジャー等) は、最小プロジェクトで実測してから本体へ適用する。

更新で同時に変えるもの: toml の `[versions]` と wrapper 2 か所。wrapper は手で編集せず、各 build root で `./gradlew wrapper --gradle-version <版>` を実行して `gradle-wrapper.properties` / `gradlew` / jar を再生成し、`distributionSha256Sum` を公式チェックサムで設定する。

完了条件: `android/` の `./gradlew test` が全件実行で失敗 0 ([テスト実行規約](../../../handbook/cross/test-execution.md) の件数確認を含む)、`samples/android` の `:app:assembleDebug`、MAUI binding の `dotnet build`、Android Studio の sync がすべて通ること。Gradle JVM の JDK 版を変える更新では、新しい JDK と従来の JDK の両方でこれを確認する (後方互換を壊さないため)。

既知の制約 (2026-08-21 時点):

- AGP 8.13 系は Gradle 10 で削除予定の API (multi-string dependency notation) を内部で使っており、Gradle 10 系へ上げるには AGP の更新が先に要る
- Kotlin 版を上げると生成物が要求する `kotlin-stdlib` の下限も上がり、ライブラリ利用側の Kotlin 要件が変わる。配布時の互換情報に反映する

Compose 版の整合 (2026-08-28 時点):

- ui module は Compose (CustomCell ホスティング・カレンダーダイアログ) に依存するため、MAUI binding は Xamarin.AndroidX.Compose.* (Runtime / UI / Foundation / Material3 等) を配達する。**Gradle 側の Compose BOM 版と Xamarin.AndroidX.Compose.* の実行時版は整合させる** — 整合が崩れると D8 二重定義や実行時のメソッド欠落クラッシュになる
- 整合の方向は「Gradle の BOM を NuGet 実行時版へ上げる」: MAUI 本体の依存連鎖 (Essentials → AndroidX.Activity → Compose.Runtime 系) が NuGet 側の下限を押し上げるため、NuGet を Gradle コンパイル版へ下げる方向は物理的に成立しない (relax-android-host-prerequisites で実測確定。BOM 2024.10.01 → 2025.11.01 への引き上げ例)。帰結として aar 利用者の Compose 推移依存の下限も BOM に追随して上がる (利用者可視の影響)
- Compose Material3 `DatePicker` は experimental API のため、BOM 更新時はカレンダーダイアログのシグネチャ・描画の追随確認を行う ([DatePickerCell の選択面](../../core/cells/date-picker-selection-surface.md))

## してはいけないこと

- `build.gradle.kts` への plugin 版 `version "…"`・`compose-bom:` のリテラル版・`version = "…"` の書き戻し: Sample と本体で版がずれ、catalog が宣言の単一元でなくなる。追加・変更は toml で行う
- Gradle JVM の更新を理由にした `jvmToolchain(17)` / `compileOptions` / compileSdk / minSdk の変更: 配布物の互換性が変わるため、別の変更として扱う
- 片方の build root だけの wrapper 更新: Studio の composite sync と MAUI の Exec が異なる Gradle 版で走り、症状が再現しなくなる

## 関連

- [リポジトリとビルドの責務境界](../../cross/architecture/repository-boundaries.md) — build root の独立と Sample の consumer 境界
- [テスト実行規約](../../../handbook/cross/test-execution.md) — Android テストの全件実行と件数確認
- [公開識別子と配布座標](../../../handbook/cross/public-identifiers.md) — GAV と `group` の現在地
- [MAUI binding の Native artifact 統合](../../maui/architecture/binding-build-integration.md) — Android binding の gradlew 直接実行と aar の増分ビルド入力
- [maui/ADR-0006](../../../decisions/maui/0006-android-binding-gradlew-exec.md) — gradlew 直接実行方式の採用理由
