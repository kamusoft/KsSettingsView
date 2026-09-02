# Verify 001: add-android-maven-distribution

**判定: VALID**

検証日: 2026-09-01 / 対象: HEAD (93a79d0) に対する作業ツリーの全変更 (untracked 含む)
デルタスペック: `kasane/changes/add-android-maven-distribution/specs/android-maven-distribution/spec.md` (Requirement 5 件 / Scenario 9 件)

> 注: コンテキストパッケージは「Scenario 8」としていたが、spec.md の実数は 9 件 (下表のとおり)。9 件すべてを対象に突き合わせた。

## 対応表

| Requirement / Scenario | 実装 | 検証 (テスト・証跡) | 状態 |
|---|---|---|---|
| **R1 単一 library module のビルド構成** | | | |
| Scenario: 統合 aar の生成 | `android/settings.gradle.kts:22`/`:28`/`:33` (`rootProject.name` と `:kssettingsview` / `:kssettingsview-bridge` の 2 project)、`android/kssettingsview/build.gradle.kts:28` (`namespace = "jp.kamusoft.kssettingsview"`)、`android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/{core,ui,compose}/` (旧 3 module のソースをパッケージ維持のまま収容)、`android/kssettingsview/src/main/res/` (旧 ui の res を統合) | 本検証で `./gradlew :kssettingsview:assembleRelease` を実行 → BUILD SUCCESSFUL。生成物 `android/kssettingsview/build/outputs/aar/kssettingsview-release.aar` を展開し `classes.jar` のパッケージ別クラス数を実測 (`.core` 36 / `.ui` 202 / `.compose` 35)。`evidence/publish-to-maven-local.txt`「aar (classes.jar) のパッケージ別クラス数」 | ✅ 一致 |
| Scenario: bridge module の独立維持 | `android/kssettingsview-bridge/build.gradle.kts:65` (`implementation(project(":kssettingsview"))`)、同 `:8-16` (発行プラグインを持たない plugins ブロック)、`android/settings.gradle.kts:33` | 本検証で `./gradlew :kssettingsview-bridge:assembleRelease` を実行 → BUILD SUCCESSFUL。`kssettingsview-bridge-release.aar` の `classes.jar` は `jp/kamusoft/kssettingsview/bridge` のみ 39 クラス (本体クラスの同梱なし = project 依存で解決) | ✅ 一致 |
| **R2 既存テストの無改変実行** | | | |
| Scenario: 全既存テストの実行 | `android/kssettingsview/build.gradle.kts:63`/`:70`/`:76` (`isIncludeAndroidResources = true` / `useJUnitPlatform()` / `maxHeapSize = "2g"`)、同 `:232-238` (junit-bom + jupiter + `junit:junit:4.13.2` + `junit-vintage-engine`) | 本検証で `./gradlew test --rerun-tasks` を実行 (124 tasks executed / BUILD SUCCESSFUL in 2m 44s)。`build/test-results/**/TEST-*.xml` 実測: `kssettingsview` debug 1183 / release 1183、`kssettingsview-bridge` debug 167 / release 167 = **合計 2700 件・failures 0・errors 0・skipped 0**。統合前の旧 4 module 合計 2700 件と一致 (`evidence/android-test-counts.txt`)。テストコードは無改変 (diff は R 明示 import 1 行とコメント文言のみ) | ✅ 一致 |
| **R3 Maven 発行物の座標と内容** | | | |
| Scenario: ローカル発行での発行物検証 | `android/kssettingsview/build.gradle.kts:24` (`alias(libs.plugins.maven.publish)`)、同 `:90-105` (`AndroidSingleVariantLibrary("release")` + `SourcesJar.Sources()` + `JavadocJar.Empty()` + `publishToMavenCentral()` + `signAllPublications()`)、同 `:107-137` (POM: name / description / url / MIT license / developers / scm)、`android/build.gradle.kts:11-17` (`group = "jp.kamusoft"` / version は catalog `kssettingsview` キー)、`android/gradle/libs.versions.toml` (`kssettingsview = "0.1.0-SNAPSHOT"` / `maven-publish = "0.37.0"`) | `evidence/publish-to-maven-local.txt`。本検証で `~/.m2/repository/jp/kamusoft/kssettingsview/0.1.0-SNAPSHOT/` を独立に再検査 — aar / sources jar / javadoc jar / `.pom` / `.module` が揃い、javadoc jar は `META-INF/` + `MANIFEST.MF` の 2 エントリのみ (実質空)、sources jar は `.core` / `.ui` / `.compose` の 3 パッケージを含み、POM に MIT license / scm / developers と recyclerview・material 等の依存が記載されていることを確認 | ✅ 一致 |
| Scenario: 発行メタデータの依存スコープ | `android/kssettingsview/build.gradle.kts:175-198` (`api`: compose-bom / compose runtime / compose-ui / **foundation-layout** / kotlinx-coroutines-core / androidx.annotation / **recyclerview**)、同 `:203-227` (`implementation`: appcompat / constraintlayout / material / activity / coroutines-android / lifecycle / foundation / material3 / ui-viewbinding)、同 `:232-259` (テスト依存を `testImplementation` / `testRuntimeOnly` へ隔離。`ui-test-manifest` は `:259`) | `evidence/publish-to-maven-local.txt`。本検証で `.pom` / `.module` を独立にパースして再確認 — spec 列挙の 4 件 (compose runtime / compose-ui / kotlinx-coroutines-core / androidx.annotation) はすべて `compile` / api variant に載る。テスト専用ライブラリ (junit / robolectric / androidx.test / ui-test-manifest / ui-test-junit4 / customview-poolingcontainer / coroutines-test) は `.pom` `.module` のいずれにも 0 件 | ⚠️ deviation 記録済み (recyclerview / foundation-layout の `api` 追加 = deviation.md 1・2 行目。spec 列挙 4 件はすべて充足しており、追加は列挙の拡張のみ) |
| Scenario: bridge の非公開 | `android/kssettingsview-bridge/build.gradle.kts:8-16` (発行プラグイン非適用) | 本検証で `./gradlew :kssettingsview-bridge:tasks --group publishing` → `No tasks`。`--all \| grep -i publish` → `prepareLintJarForPublish` のみ (AGP の lint jar 準備タスクで Maven 発行経路ではない)。`evidence/publish-to-maven-local.txt` 末尾と一致 | ✅ 一致 |
| **R4 Sample のソース参照の維持** | | | |
| Scenario: Sample のビルドとソース参照 | `samples/android/settings.gradle.kts:39-44` (明示 `substitute(module("jp.kamusoft:kssettingsview")).using(project(":kssettingsview"))`、理由コメントに design Decision 4 と同旨を明記)、`samples/android/app/build.gradle.kts:64` (依存 1 行) | 本検証で `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL、`:app:dependencies --configuration debugCompileClasspath` → `jp.kamusoft:kssettingsview:0.1.0-SNAPSHOT -> project :android:kssettingsview`。同出力で recyclerview 1.3.2 と foundation-layout が本体の `api` から Sample の compile classpath へ推移していることも確認 (Sample 側の明示 recyclerview 宣言は削除済み)。本体ソース変更が再ビルドへ反映されることは `evidence/sample-substitution.txt` | ⚠️ deviation 記録済み (Sample の明示 recyclerview 依存削除 = deviation.md 1 行目) |
| **R5 MAUI binding のビルド入力の追随** | | | |
| Scenario: binding のビルド | `maui/android/KsSettingsView.Binding.Android/KsSettingsView.Binding.Android.csproj:31-32` (`AndroidLibrary` 2 本: `kssettingsview-bridge-release.aar` Bind=true / `kssettingsview-release.aar` Bind=false)、同 `:111-119` (ソース glob 2 module)、同 `:133` (`./gradlew ... :kssettingsview:assembleRelease :kssettingsview-bridge:assembleRelease`) | `evidence/maui-binding-and-integrationhost.txt`「binding のビルド」 — `dotnet build ...Binding.Android.csproj -c Debug` が 0 エラー、警告 20 件は BG8605 / BG8606 / BG8A00 の既知分のみ。本検証では再実行していない (下記「未検証」参照) | ✅ 一致 (証跡による) |
| Scenario: IntegrationHost での実行確認 | 上記 csproj の追随に依存 (IntegrationHost 側は `Properties/AndroidManifest.xml` のコメント文言のみ変更) | `evidence/maui-binding-and-integrationhost.txt`「IntegrationHost のビルドと起動」+ スクショ 2 枚 (`evidence/android-integrationhost-initial.png` / `-recreated.png`)。本検証でスクショ 2 枚を実視し、`kasane/handbook/maui/integration-host-verification.md`「期待される表示」の両表 (初期: root header / 3 Section / root footer / 緑 header、再生成後: root header・footer 非表示 / テーマ「解放中に更新」/ 言語 Français / 「通知設定 (解放中に更新)」/ オレンジ header) と全項目一致することを確認 | ✅ 一致 (証跡による) |

## 追加検査

| 検査 | 結果 |
|---|---|
| tasks.md の完了状態 | 全 17 タスクが `[x]`。対応表・実物と突き合わせ、**虚偽チェックなし**。1.3 (R 明示 import) は `jp.kamusoft.kssettingsview.R` を import する 10 ファイル (main 9 + test 1) で実在を確認。1.5 (catalog キー改名) は `libs.versions.toml` の diff で確認 |
| 逆流検査 (足場凍結) | `git status` で `kasane/changes/add-android-maven-distribution/` 配下の tracked ファイルのうち変更があるのは `tasks.md` のみ。`proposal.md` / `design.md` / `specs/android-maven-distribution/spec.md` はいずれも HEAD から無変更。**逆流なし** |
| テスト全件成功 | `./gradlew test --rerun-tasks` を本検証で実行し 2700 件 / 失敗 0 / エラー 0 / skip 0。**再実行して確認済み** |
| 旧 module 名の残骸 | `android/ks-settingsview-*` ディレクトリは存在しない。ソース・ビルドスクリプト・csproj・handbook 内の `ks-settingsview` 参照は 0 件 (残存はすべて `build/` `obj/` `bin/` 配下のビルド生成物 = git 管理外) |
| UI 変更 | 本変更に `ui/` アーティファクトはなく、承認モックのゲートは適用外 (IntegrationHost スクショは動作証跡であり、モック照合ではない) |

## 未記録乖離・所見

判定を覆すものではないが、記録の完全性として 2 点。

### 1. `.agents/skills/docs-refresh/SKILL.md` の 2 か所の更新が deviation.md に載っていない

作業ツリーの diff に含まれるが、どの Scenario にも対応せず、deviation.md の `[付随修正]` 列挙 (①ソースコメント ②ホスト側コメント ③handbook 3 本 ④samples の rootProject.name ⑤ビルド生成物) のどれにも該当しない。

- `.agents/skills/docs-refresh/SKILL.md:180` — 本変更で消滅した `android/ks-settingsview-ui/build.gradle.kts` への参照を `android/kssettingsview/build.gradle.kts` へ更新
- 同 `:709` — 本変更で偽になった「ADR-0002 と現行 Gradle `group` の食い違い」の記述を現行値に更新

いずれも review-001.md アクションプラン 2 / review-002.md 指摘 (4) として起票され、review-002.md の「再確認」で解消が確認されている。**内容として合意されていない変更ではなく、記録先が deviation.md ではなく review 記録である**という帳簿上の欠けなので、⚠️ 相当と扱い ❌ にはしなかった。

**見立て**: 実装側の修正は不要。deviation.md の `[付随修正]` に「⑥ docs-refresh スキルの失効参照・失効記述の更新 (2 か所)」を 1 行追記して記録を閉じるのが正。蒸留 (ksn-distill) が deviation.md を実装乖離メモの原料に使うため、review 記録だけに残ると落ちる可能性がある。

### 2. deviation.md ②の件数が実数と 1 件ずれている

`[付随修正]` ② は「ホスト側コメント 5 ファイル」とあるが、該当する diff は 6 ファイル (`maui/tests/KsSettingsView.IntegrationHost.Android/Properties/AndroidManifest.xml`、`maui/tests/KsSettingsView.MauiHost/Platforms/Android/{AndroidManifest.xml,MainActivity.cs}`、`samples/maui/KsSettingsView.Sample.Maui/Platforms/Android/{AndroidManifest.xml,MainActivity.cs}`、`samples/android/app/src/main/AndroidManifest.xml`)。列挙したディレクトリ・ファイル種別の範囲からは外れていないため、数字の書き誤りとみなせる。

**見立て**: deviation.md の「5 ファイル」を「6 ファイル」に直すだけ。

## 未検証・留保

- **Scenario: binding のビルド / IntegrationHost での実行確認** — 本検証では再実行していない。binding の `dotnet build` は Android workload と長時間ビルドを要し、IntegrationHost は実機接続が前提のため。いずれも `evidence/maui-binding-and-integrationhost.txt` とスクショ 2 枚で確認した。スクショについては handbook の期待表と全項目を実視照合済みで、証跡の内容自体に不整合はない。
- **Central への実発行・署名付き成果物** — design Decision 3 が明示的に保証範囲外 (phase-7 / phase-8 の責務) としており、本変更の Scenario にも含まれない。`evidence/snapshot-central-publish-guard.txt` の SNAPSHOT ガードは spec の Scenario に対応しない追加の安全策で、本検証では実装 (`android/kssettingsview/build.gradle.kts:153-168`) の存在のみ確認した (ガード発火の再実行はしていない)。
