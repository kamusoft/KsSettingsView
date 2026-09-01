// kssettingsview: KsSettingsView Android 本体
//
// 公開ライブラリの全層を単一モジュールに収める（android/ADR-0016）。層の区別は
// Kotlin パッケージ名が担う:
//   - `jp.kamusoft.kssettingsview.core`    SettingsRoot / Section / Cell 抽象 / KsAnyView /
//                                          RootAccessory / SectionAccessory のドメインモデル
//   - `jp.kamusoft.kssettingsview.ui`      KsSettingsView（FrameLayout）/ RecyclerView ベースの
//                                          Adapter / Cell レジストリ / ItemDecoration /
//                                          スタイル型（Theme / CellStyle）
//   - `jp.kamusoft.kssettingsview.compose` Compose ラッパ `KsSettingsView` と宣言 DSL
//                                          `settingsRoot { ... }`
//
// Maven 座標は `jp.kamusoft:kssettingsview`。group / version はルート build.gradle.kts が
// subprojects 一括で設定する。

plugins {
    // Android Library プラグイン
    alias(libs.plugins.android.library)
    // Kotlin Android プラグイン（Compose を使うため JVM 用ではなく Android 用）
    alias(libs.plugins.kotlin.android)
    // Compose Compiler プラグイン（Kotlin 2.0+ で必須）
    alias(libs.plugins.kotlin.compose)
    // Maven 発行（Sonatype Central Portal）
    alias(libs.plugins.maven.publish)
}

android {
    namespace = "jp.kamusoft.kssettingsview"
    compileSdk = 35

    defaultConfig {
        // 本ライブラリの最低サポート Android バージョン
        minSdk = 29
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        // ComposeView / `KsAnyView.Compose` の `@Composable` ラムダ / Compose ラッパのため有効化
        compose = true
        // BuildConfig は不要
        buildConfig = false
    }

    // Kotlin ソースルートは Android Library 既定の `src/main/java` ではなく
    // `src/main/kotlin` / `src/test/kotlin` とする。
    sourceSets {
        named("main") {
            java.srcDirs("src/main/kotlin")
            manifest.srcFile("src/main/AndroidManifest.xml")
        }
        named("test") {
            java.srcDirs("src/test/kotlin")
        }
    }

    testOptions {
        unitTests {
            // Robolectric は Android リソース・Resources 系 API を要求するため有効化
            isIncludeAndroidResources = true

            all {
                // JUnit Platform 上で Jupiter（`.core` 由来の JUnit 5）と
                // junit-vintage-engine（`.ui` / `.compose` 由来の JUnit 4 + Robolectric）を
                // 同居させる。Compose のテスト Rule（createComposeRule）が JUnit 4 前提のため、
                // 両エンジンを並べる構成を採る。
                it.useJUnitPlatform()

                // Robolectric は生成した Resources.Theme を実行環境の生存期間だけ保持する。
                // ライブラリ UI は同梱テーマをかぶせた Context から生成されるため (android/ADR-0020)、
                // テストが作る Context ごとに Theme が 1 つ増える。Gradle 既定の 512m では
                // クラスをまたぐ累積で足りなくなるため、テスト JVM のヒープを明示する。
                it.maxHeapSize = "2g"
            }
        }
    }
}

// JDK 17 を採用（リポジトリ全体の Android ビルド共通）
kotlin {
    // Maven 公開面は visibility と型を明示した宣言だけで構成し、意図しない API の追加を
    // コンパイル時に拒否する (android/ADR-0022)。
    explicitApi()
    jvmToolchain(17)
}

mavenPublishing {
    // release variant のみを発行し、sources jar を同梱する。javadoc jar は Maven Central の
    // 必須要件を満たすためだけの空 jar とする。IDE の KDoc 表示は sources jar が担うため
    // javadoc の実体は要らず、利用者向けドキュメントは skills/ と README が担う。
    configure(
        com.vanniktech.maven.publish.AndroidSingleVariantLibrary(
            javadocJar = com.vanniktech.maven.publish.JavadocJar.Empty(),
            sourcesJar = com.vanniktech.maven.publish.SourcesJar.Sources(),
            variant = "release",
        ),
    )

    // Sonatype Central Portal へ発行する。認証は
    // `ORG_GRADLE_PROJECT_mavenCentralUsername` / `mavenCentralPassword` で渡す。
    publishToMavenCentral()

    // Central の必須要件。署名鍵は `ORG_GRADLE_PROJECT_signingInMemoryKey` 系で渡す。
    // 鍵が未設定のローカル発行 (`publishToMavenLocal`) は未署名のまま成功する。
    signAllPublications()

    pom {
        name.set("KsSettingsView")
        description.set(
            "A settings screen UI library for Android, providing list-style settings screens " +
                "with built-in cell types for both Android View and Jetpack Compose.",
        )
        inceptionYear.set("2026")
        url.set("https://github.com/kamusoft/KsSettingsView")

        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
                distribution.set("repo")
            }
        }

        developers {
            developer {
                id.set("kamusoft")
                name.set("kamusoft")
                url.set("https://github.com/kamusoft")
            }
        }

        scm {
            url.set("https://github.com/kamusoft/KsSettingsView")
            connection.set("scm:git:https://github.com/kamusoft/KsSettingsView.git")
            developerConnection.set("scm:git:ssh://git@github.com/kamusoft/KsSettingsView.git")
        }
    }
}

// SNAPSHOT を Sonatype Central へ発行しない。
//
// 発行プラグインは version が `-SNAPSHOT` で終わるとき、mavenCentral リポジトリの URL を
// Central の snapshot リポジトリへ向ける。そのため認証情報がある環境で Central 向けタスクを
// 実行すると、開発版がそのまま公開される。SNAPSHOT のあいだは Central 向けタスクを失敗させ、
// リリース版の version を与えたときだけ通す。ローカル発行 (`publishToMavenLocal` /
// `publishMavenPublicationToMavenLocal`) は開発と発行物の検証に使うため妨げない。
//
// 対象はタスク名で判定する。個々の名前を列挙すると、プラグインが Central 向けタスクを
// 増やしたときに素通りし、名前を変えたときに列挙が死に名になって、どちらも無音で穴が開く。
// 名前に `MavenCentral` を含むタスクを一律で対象とし、除外は 1 つだけ挙げる —
// `dropMavenCentralDeployment` は誤って作った deployment を取り下げる後始末用で、
// 発行の経路ではないため止めない。
val isSnapshotVersion = version.toString().endsWith("-SNAPSHOT")

if (isSnapshotVersion) {
    val snapshotVersion = version.toString()
    tasks.configureEach {
        if (name.contains("MavenCentral") && name != "dropMavenCentralDeployment") {
            doFirst {
                throw GradleException(
                    "SNAPSHOT ($snapshotVersion) は Maven Central へ発行しない。" +
                        "リリース版の version を gradle/libs.versions.toml の kssettingsview キーへ" +
                        "設定してから実行する。ローカルでの発行物確認には publishToMavenLocal を使う。",
                )
            }
        }
    }
}

dependencies {
    // ---- 公開 API に型が現れる依存（利用者の compile classpath へ届くよう api で公開する）----

    // Compose BOM。api 側に置き、下の versionless な Compose 依存の版を
    // 発行メタデータでも解決できるようにする。
    api(platform(libs.compose.bom))

    // Compose Runtime。`KsAnyView.Compose` が `@Composable () -> Unit` を保持する。
    api("androidx.compose.runtime:runtime")

    // Compose UI。`Theme` の各項目が Color / TextStyle / Dp を値型に使う。
    api("androidx.compose.ui:ui")

    // Compose Foundation Layout。`Theme` の `sectionMargin` が PaddingValues を値型に使う。
    // `foundation` ではなく宣言元 artifact を直接指定する。`foundation` の
    // androidApiElements には foundation-layout が含まれず、間接的な成立になるため
    // (リソース参照と同じく、宣言元を経由するほうが壊れにくい)。
    api("androidx.compose.foundation:foundation-layout")

    // Coroutines Core。`SettingsRootStore` が `StateFlow` を公開する。
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    // androidx.annotation。`KsImage.Resource(@DrawableRes resId: Int)` 等が使う。
    api("androidx.annotation:annotation:1.9.1")

    // RecyclerView。内部の ListAdapter / DiffUtil / ConcatAdapter に使うだけでなく、
    // 独自 Cell を登録する利用者が継承する `CellViewHolder` が
    // `RecyclerView.ViewHolder` を継承するため、公開 API に型が現れる。
    api("androidx.recyclerview:recyclerview:1.3.2")

    // ---- 実装内部でのみ使う依存 ----

    // AppCompat（AppCompatImageView / AppCompatRadioButton / AppCompatCheckBox を使うため）
    implementation("androidx.appcompat:appcompat:1.7.0")

    // ConstraintLayout（CellBaseViews のルート ViewGroup として使用）
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Material Components（MaterialSwitch を使うため、および同梱テーマ
    // res/values/themes.xml の親 Theme.Material3.DayNight.NoActionBar のため）。
    // 利用者アプリのテーマに要件はない。ライブラリ UI は同梱テーマをかぶせた Context から
    // 生成され、Material ウィジェットが要求する属性はその同梱テーマが供給する（android/ADR-0020）。
    implementation("com.google.android.material:material:1.12.0")

    // Activity（カレンダー選択面の器に使う `ComponentDialog`。ViewTree の lifecycle /
    //           savedStateRegistry の所有者を自前で供給するため、Fragment を必要としない）
    implementation("androidx.activity:activity:1.9.3")

    // Coroutines Android（main dispatcher）
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Lifecycle Runtime（findViewTreeLifecycleOwner / lifecycleScope での collect 用）
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")

    // Compose の内部利用分（Text / Image / painterResource / AndroidView / ComposeView）
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-viewbinding")

    // ---- テスト専用依存（release の発行物には現れない）----

    // JUnit 5（Jupiter）: `.core` 由来のテストが使う
    testImplementation(platform("org.junit:junit-bom:5.11.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // JUnit 4 + vintage engine: `.ui` / `.compose` 由来の Robolectric テストが使う
    testImplementation("junit:junit:4.13.2")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine")

    // Robolectric: Android Framework のシャドウ実装で JVM 上で UI テストを行う
    testImplementation("org.robolectric:robolectric:4.13")

    // AndroidX Test Core（ApplicationProvider 等）
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("androidx.test.ext:junit:1.2.1")

    // Coroutines テスト（runTest / TestScope）
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")

    // Pooling Container（recyclerview の推移的依存）。プール滞在＝「pooling container の内側で
    // window から外れた状態」をテストから直接作るために、コンパイル時参照を明示する。
    testImplementation("androidx.customview:customview-poolingcontainer:1.0.0")

    // Compose UI Test（Robolectric バックエンドで動かす：createComposeRule）
    testImplementation("androidx.compose.ui:ui-test-junit4")

    // ui-test-manifest はテスト時に必要な Activity（ComponentActivity）の AndroidManifest を
    // 提供する。テスト専用の configuration に置き、release の発行物へ混入させない。
    testImplementation("androidx.compose.ui:ui-test-manifest")
}
